package org.showen.livecoverageplugin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.showen.livecoverageplugin.coverage.model.CoverageInfo;
import org.showen.livecoverageplugin.coverage.service.*;
import org.showen.livecoverageplugin.license.LicenseSupport;
import org.showen.livecoverageplugin.settings.LiveCoverageSettings;
import org.showen.livecoverageplugin.ui.CoverageToolWindowService;
import org.showen.livecoverageplugin.ui.service.CoverageHighlighterService;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.tools.ExecFileLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.showen.livecoverageplugin.constants.CoverageConstants.*;

/**
 * Main component for Live Coverage functionality.
 * Orchestrates coverage polling, analysis, and visualization.
 * Uses a modular service-based architecture for better maintainability.
 */
@Service(Service.Level.PROJECT)
public final class LiveCoverageComponent implements Disposable {
    
    private static final Logger LOG = Logger.getInstance(LiveCoverageComponent.class);
    
    private final Project project;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> pollingTask;
    
    // Service dependencies
    private final CoverageConnectionService connectionService;
    private final CoverageAnalysisService analysisService;
    private final BytecodeCacheService bytecodeCacheService;
    private final CoverageHighlighterService highlighterService;
    

    public LiveCoverageComponent(@NotNull Project project) {
        this.project = project;
        // Use final variable for lambda expression
        final String projectName = project.getName() != null ? project.getName() : "Unknown";
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LiveCoveragePoller-" + projectName);
            t.setDaemon(true);
            return t;
        });
        
        // Initialize services
        this.connectionService = new CoverageConnectionService();
        this.analysisService = new CoverageAnalysisService();
        this.bytecodeCacheService = new BytecodeCacheService();
        this.highlighterService = new CoverageHighlighterService(project);
    }

    /**
     * Gets the LiveCoverageComponent instance for a project.
     */
    @NotNull
    public static LiveCoverageComponent getInstance(@NotNull Project project) {
        return project.getService(LiveCoverageComponent.class);
    }

    /**
     * Starts polling for coverage updates.
     */
    public void startPolling() {
        if (!LicenseSupport.requireLicense()) {
            return;
        }
        if (pollingTask != null && !pollingTask.isCancelled()) {
            String projectName = project.getName();
            LOG.warn("Polling already started for project: " + (projectName != null ? projectName : "Unknown"));
            return;
        }

        String projectName = project.getName();
        LOG.info(String.format(MSG_STARTING_POLLING, projectName != null ? projectName : "Unknown"));
        pollingTask = executor.scheduleAtFixedRate(
                this::updateCoverage,
                0,
                DEFAULT_POLLING_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        updateToolWindowStatus("Coverage polling is running");
    }

    /**
     * Stops polling for coverage updates.
     */
    public void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
        }
        String projectName = project.getName();
        LOG.info(String.format(MSG_STOPPING_POLLING, projectName != null ? projectName : "Unknown"));
        updateToolWindowStatus("Coverage polling is paused");
    }

    /**
     * Manually triggers a coverage update.
     * Useful for manual refresh actions.
     */
    public void refreshCoverage() {
        if (!LicenseSupport.requireLicense()) {
            return;
        }
        if (project.isDisposed()) {
            LOG.debug("Cannot refresh coverage: project is disposed");
            return;
        }
        if (executor.isShutdown()) {
            LOG.debug("Cannot refresh coverage: executor is shutdown");
            return;
        }
        executor.submit(() -> {
            if (!project.isDisposed()) {
                updateCoverage();
            }
        });
    }

    /**
     * Resets all highlighters and clears coverage data from JaCoCo agent.
     * This method is thread-safe and can be called from any thread.
     * The reset operation is performed in the correct order to ensure consistency.
     */
    public void resetHighlighters() {
        if (!LicenseSupport.requireLicense()) {
            return;
        }
        if (project.isDisposed()) {
            LOG.debug("Cannot reset highlighters: project is disposed");
            return;
        }
        
        // First, reset the JaCoCo agent to clear accumulated coverage data
        // This should happen before clearing UI to ensure data consistency
        LiveCoverageSettings settings = LiveCoverageSettings.getInstance();
        boolean agentResetSuccess = false;
        if (settings != null) {
            try {
                // Dump and reset the agent to clear its internal state
                ExecFileLoader loader = connectionService.dumpCoverageData(
                        settings.getTcpServerAddress(),
                        settings.getTcpServerPort(),
                        true); // reset=true clears the agent's coverage data
                
                if (loader != null) {
                    LOG.info("JaCoCo agent reset successfully");
                    agentResetSuccess = true;
                } else {
                    LOG.warn("Failed to reset JaCoCo agent - connection may be unavailable");
                }
            } catch (Exception e) {
                LOG.warn("Error resetting JaCoCo agent", e);
            }
        }
        
        // Then clear UI components (highlighters and tool window)
        // This ensures UI reflects the reset state
        highlighterService.clearHighlighters();
        CoverageToolWindowService toolService = project.getService(CoverageToolWindowService.class);
        if (toolService != null) {
            toolService.clear();
        }
        
        // Log final status
        if (agentResetSuccess) {
            LOG.debug("Coverage reset completed successfully");
        } else {
            LOG.debug("Coverage reset completed (agent reset may have failed)");
        }
    }

    /**
     * Checks if polling is currently active.
     */
    public boolean isPolling() {
        return pollingTask != null && !pollingTask.isCancelled();
    }

    /**
     * Updates coverage by fetching data from agent, analyzing it, and updating UI.
     */
    private void updateCoverage() {
        // Check if project is still valid
        if (project.isDisposed()) {
            LOG.debug("Project is disposed, skipping coverage update");
            return;
        }
        
        try {
            LiveCoverageSettings settings = LiveCoverageSettings.getInstance();
            if (settings == null) {
                return;
            }

            // Check if settings are valid
            if (!validateSettings(settings)) {
                String errorMsg = buildValidationErrorMessage(settings);
                updateToolWindowStatus(errorMsg);
                LOG.debug("Settings validation failed: " + errorMsg);
                return;
            }

            // Always use cumulative mode - never reset the agent during polling
            // User can manually reset via "Clear Coverage" action
            boolean resetAgent = false;

            // Dump coverage data from agent
            // Important: In cumulative mode, reset=false so data accumulates
            ExecFileLoader loader = connectionService.dumpCoverageData(
                    settings.getTcpServerAddress(),
                    settings.getTcpServerPort(),
                    resetAgent);

            if (loader == null) {
                // Connection failed
                handleConnectionFailure();
                return;
            }

            // Check again if project is still valid before processing
            if (project.isDisposed()) {
                LOG.debug("Project was disposed during coverage update, aborting");
                return;
            }

            // Ensure bytecode cache is fresh
            List<File> classesDirs = findClassesDirs();
            if (classesDirs.isEmpty()) {
                String errorMsg = "Classes directory not found";
                if (!settings.getClassOutputPaths().isEmpty()) {
                    errorMsg += ": " + String.join(", ", settings.getClassOutputPaths());
                }
                errorMsg += " - check Settings → Live Coverage";
                updateToolWindowStatus(errorMsg);
                String projectName = project.getName();
                LOG.warn("Classes directory not found for project: " + (projectName != null ? projectName : "Unknown") + 
                        ", configured path: " + String.join(", ", settings.getClassOutputPaths()));
                return;
            }

            for (File classesDir : classesDirs) {
                bytecodeCacheService.refreshCacheIfNeeded(classesDir);
            }

            // Analyze coverage
            CoverageBuilder builder = analysisService.analyzeCoverage(
                    loader.getExecutionDataStore(),
                    classesDirs,
                    bytecodeCacheService.getCache());

            // Check project state again before UI updates
            if (project.isDisposed()) {
                return;
            }

            // Update tool window
            CoverageToolWindowService toolService = project.getService(CoverageToolWindowService.class);
            if (toolService != null) {
                toolService.updateContent(builder);
                updateToolWindowStatus("Connected to JaCoCo agent");
            }

            // Get coverage info for highlighting
            List<CoverageInfo> coverageInfos = analysisService.convertToCoverageInfos(builder);

            // Update highlights (always cumulative mode)
            if (!coverageInfos.isEmpty()) {
                updateHighlights(coverageInfos, settings);
            }

        } catch (Exception e) {
            if (!project.isDisposed()) {
                LOG.warn("Error in updateCoverage", e);
                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = e.getClass().getSimpleName();
                }
                // Truncate very long error messages
                if (errorMsg.length() > 100) {
                    errorMsg = errorMsg.substring(0, 97) + "...";
                }
                updateToolWindowStatus("Error: " + errorMsg);
            }
        }
    }

    /**
     * Validates that settings are properly configured.
     */
    private boolean validateSettings(@NotNull LiveCoverageSettings settings) {
        return settings.getTcpServerAddress() != null && !settings.getTcpServerAddress().isEmpty()
                && settings.getTcpServerPort() > 0
                && !settings.getSourceRootPaths().isEmpty()
                && !settings.getClassOutputPaths().isEmpty()
                && settings.getSourceRootPaths().size() == settings.getClassOutputPaths().size();
    }
    
    /**
     * Builds a detailed validation error message.
     */
    @NotNull
    private String buildValidationErrorMessage(@NotNull LiveCoverageSettings settings) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        
        if (settings.getTcpServerAddress() == null || settings.getTcpServerAddress().isEmpty()) {
            missing.add("TCP Address");
        }
        if (settings.getTcpServerPort() <= 0) {
            missing.add("TCP Port");
        }
        if (settings.getSourceRootPaths().isEmpty() || settings.getClassOutputPaths().isEmpty()) {
            missing.add("Source output path / Classes output path pair");
        } else if (settings.getSourceRootPaths().size() != settings.getClassOutputPaths().size()) {
            missing.add("Path pair count mismatch");
        }
        
        if (missing.isEmpty()) {
            return "Configuration incomplete - check settings";
        } else {
            return "Missing configuration: " + String.join(", ", missing) + " - check Settings → Live Coverage";
        }
    }

    /**
     * Handles connection failure scenarios.
     */
    private void handleConnectionFailure() {
        CoverageToolWindowService toolService = project.getService(CoverageToolWindowService.class);
        // Keep existing highlights but show status
        if (toolService != null) {
            toolService.updateStatus("Connection failed - using cached data");
        }
    }

    /**
     * Updates highlights in cumulative mode.
     * Note: highlightCoverage already handles clearing internally for atomicity.
     */
    private void updateHighlights(
            @NotNull List<CoverageInfo> coverageInfos,
            @NotNull LiveCoverageSettings settings) {
        
        List<String> sourceRoots = settings.getSourceRootPaths();
        if (sourceRoots.isEmpty()) {
            return;
        }

        // highlightCoverage internally clears old highlighters before adding new ones
        // This ensures atomicity and prevents race conditions
        highlighterService.highlightCoverage(coverageInfos, sourceRoots);
    }

    /**
     * Updates tool window status message.
     */
    private void updateToolWindowStatus(@NotNull String message) {
        CoverageToolWindowService toolService = project.getService(CoverageToolWindowService.class);
        if (toolService != null) {
            toolService.updateStatus(message);
        }
    }

    /**
     * Finds available classes directories for the project.
     */
    private List<File> findClassesDirs() {
        List<File> classesDirs = new ArrayList<>();
        LiveCoverageSettings settings = LiveCoverageSettings.getInstance();
        if (settings != null && !settings.getClassOutputPaths().isEmpty()) {
            for (String classesPath : settings.getClassOutputPaths()) {
                File f = new File(classesPath);
                if (f.exists() && f.isDirectory()) {
                    classesDirs.add(f);
                }
            }
        }
        if (!classesDirs.isEmpty()) {
            return classesDirs;
        }

        String singlePath = settings != null ? settings.getClassOutputPath() : null;
        if (singlePath != null && !singlePath.isEmpty()) {
            File f = new File(singlePath);
            if (f.exists() && f.isDirectory()) {
                classesDirs.add(f);
                return classesDirs;
            }
        }

        // Fallback to common build directories
        String basePath = project.getBasePath();
        if (basePath != null) {
            File base = new File(basePath);
            File f1 = new File(base, DEFAULT_CLASSES_PATH_RELATIVE_1);
            if (f1.exists() && f1.isDirectory()) {
                classesDirs.add(f1);
            }
            File f2 = new File(base, DEFAULT_CLASSES_PATH_RELATIVE_2);
            if (f2.exists() && f2.isDirectory()) {
                classesDirs.add(f2);
            }
        }

        return classesDirs;
    }

    @Override
    public void dispose() {
        stopPolling();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        highlighterService.clearHighlighters();
        bytecodeCacheService.clear();
    }
}
