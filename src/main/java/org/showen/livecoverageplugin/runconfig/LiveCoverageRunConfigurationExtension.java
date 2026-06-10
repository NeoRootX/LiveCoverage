package org.showen.livecoverageplugin.runconfig;

import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.showen.livecoverageplugin.coverage.service.JaCoCoAgentService;
import org.showen.livecoverageplugin.license.LicenseSupport;
import org.showen.livecoverageplugin.settings.LiveCoverageSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Automatically adds JaCoCo agent JVM arguments to Java run configurations.
 * This eliminates the need for users to manually configure JVM arguments.
 */
public class LiveCoverageRunConfigurationExtension extends RunConfigurationExtension {
    
    private static final Logger LOG = Logger.getInstance(LiveCoverageRunConfigurationExtension.class);
    
    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(
            @NotNull T configuration,
            @NotNull JavaParameters javaParameters,
            @Nullable RunnerSettings runnerSettings) {
        
        if (!LicenseSupport.isFeatureAllowed()) {
            return;
        }

        Project project = configuration.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }
        
        // Get settings
        LiveCoverageSettings settings = LiveCoverageSettings.getInstance();
        if (settings == null) {
            return;
        }
        
        // Check if settings are configured
        String address = settings.getTcpServerAddress();
        int port = settings.getTcpServerPort();
        if (address == null || address.isEmpty() || port <= 0) {
            LOG.debug("Live Coverage settings not configured, skipping JVM argument injection");
            return;
        }
        
        // Get agent service
        JaCoCoAgentService agentService = JaCoCoAgentService.getInstance();
        if (agentService == null) {
            LOG.debug("JaCoCoAgentService not available, skipping JVM argument injection");
            return;
        }
        
        // Get JVM argument
        String jvmArg = agentService.getJvmArgument(address, port);
        if (jvmArg == null || jvmArg.isEmpty()) {
            LOG.debug("Failed to generate JVM argument, skipping injection");
            return;
        }
        
        // Check if already added (avoid duplicates)
        // Get VM parameters list
        ParametersList vmParamsList = javaParameters.getVMParametersList();
        if (vmParamsList == null) {
            LOG.debug("VM parameters list is null, skipping JVM argument injection");
            return;
        }
        
        // Check existing VM parameters string
        String existingVmOptions = vmParamsList.getParametersString();
        boolean alreadyAdded = false;
        if (existingVmOptions != null && !existingVmOptions.isEmpty()) {
            // Check if JaCoCo agent is already configured
            if (existingVmOptions.contains("-javaagent:") && existingVmOptions.contains("org.jacoco.agent")) {
                alreadyAdded = true;
            }
        }
        
        if (alreadyAdded) {
            LOG.debug("JaCoCo agent already configured in VM options, skipping");
            return;
        }
        
        // Add JVM argument as a single parameter
        vmParamsList.add(jvmArg);
        LOG.info("Automatically added JaCoCo agent JVM argument to run configuration: " + configuration.getName());
    }
    
    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
        // Apply to all run configurations that use JavaParameters
        // The updateJavaParameters method will be called for Java-based configurations
        return true;
    }
}
