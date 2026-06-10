package org.showen.livecoverageplugin.coverage.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Service responsible for extracting and providing the JaCoCo agent JAR.
 * Extracts the agent from the plugin's classpath to a temporary location.
 */
public final class JaCoCoAgentService {
    
    private static final Logger LOG = Logger.getInstance(JaCoCoAgentService.class);
    private static final String AGENT_JAR_NAME = "org.jacoco.agent-0.8.12-runtime.jar";
    private static final String TEMP_AGENT_DIR = "livecoverage-agent";
    
    private File extractedAgentJar = null;
    
    /**
     * Gets the path to the JaCoCo agent JAR.
     * Extracts it from the plugin's classpath if not already extracted.
     * @return Path to the agent JAR file, or null if extraction failed
     */
    @Nullable
    public File getAgentJarPath() {
        if (extractedAgentJar != null && extractedAgentJar.exists()) {
            return extractedAgentJar;
        }
        
        return extractAgentJar();
    }
    
    /**
     * Extracts the JaCoCo agent JAR from the classpath to a temporary location.
     * @return Path to the extracted JAR, or null if extraction failed
     */
    @Nullable
    private File extractAgentJar() {
        // First, try to find it from the classpath URL (for development)
        // This doesn't require opening a stream
        String classPath = System.getProperty("java.class.path");
        if (classPath != null) {
            String[] paths = classPath.split(File.pathSeparator);
            for (String path : paths) {
                if (path.contains("org.jacoco.agent") && path.endsWith(".jar")) {
                    File agentFile = new File(path);
                    if (agentFile.exists() && agentFile.isFile()) {
                        extractedAgentJar = agentFile;
                        LOG.info("Using JaCoCo agent from classpath: " + extractedAgentJar.getAbsolutePath());
                        return extractedAgentJar;
                    }
                }
            }
        }
        
        // Try to find the agent jar as a resource in the plugin
        ClassLoader classLoader = JaCoCoAgentService.class.getClassLoader();
        
        // Try resources/lib/ directory first
        String resourcePath = "lib/" + AGENT_JAR_NAME;
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return extractFromStream(inputStream);
            }
        } catch (IOException e) {
            LOG.debug("Failed to extract from lib/ path", e);
        }
        
        // Try alternative path (direct in resources)
        resourcePath = AGENT_JAR_NAME;
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return extractFromStream(inputStream);
            }
        } catch (IOException e) {
            LOG.debug("Failed to extract from root resources path", e);
        }
        
        LOG.warn("JaCoCo agent JAR not found. Please rebuild the plugin to include the agent JAR.");
        return null;
    }
    
    /**
     * Extracts the agent JAR from an input stream to a temporary location.
     * @param inputStream The input stream containing the JAR data
     * @return Path to the extracted JAR, or null if extraction failed
     */
    @Nullable
    private File extractFromStream(@NotNull InputStream inputStream) {
        try {
            // Create temporary directory for agent jar
            Path tempDir = Files.createTempDirectory(TEMP_AGENT_DIR);
            tempDir.toFile().deleteOnExit();
            
            Path agentJarPath = tempDir.resolve(AGENT_JAR_NAME);
            
            // Copy the jar to temporary location
            Files.copy(inputStream, agentJarPath, StandardCopyOption.REPLACE_EXISTING);
            
            extractedAgentJar = agentJarPath.toFile();
            extractedAgentJar.deleteOnExit();
            
            LOG.info("JaCoCo agent JAR extracted to: " + extractedAgentJar.getAbsolutePath());
            return extractedAgentJar;
            
        } catch (IOException e) {
            LOG.warn("Failed to extract JaCoCo agent JAR from stream", e);
            return null;
        } catch (Exception e) {
            LOG.error("Unexpected error extracting JaCoCo agent JAR from stream", e);
            return null;
        }
    }
    
    /**
     * Gets the JVM argument string for attaching the JaCoCo agent.
     * @param address TCP server address (default: 127.0.0.1)
     * @param port TCP server port (default: 6300)
     * @return JVM argument string, or null if agent jar is not available
     */
    @Nullable
    public String getJvmArgument(@NotNull String address, int port) {
        File agentJar = getAgentJarPath();
        if (agentJar == null) {
            return null;
        }
        
        return String.format("-javaagent:%s=output=tcpserver,address=%s,port=%d",
                agentJar.getAbsolutePath(), address, port);
    }
    
    /**
     * Gets the singleton instance of JaCoCoAgentService.
     */
    @NotNull
    public static JaCoCoAgentService getInstance() {
        return ApplicationManager.getApplication().getService(JaCoCoAgentService.class);
    }
}
