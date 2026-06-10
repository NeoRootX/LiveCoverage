package org.showen.livecoverageplugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.showen.livecoverageplugin.constants.CoverageConstants.*;

/**
 * Persistent settings for Live Coverage Plugin.
 * Stores user configuration that persists across IDE sessions.
 */
@State(name = "LiveCoverageSettings", storages = @Storage("LiveCoverageSettings.xml"))
public final class LiveCoverageSettings implements PersistentStateComponent<LiveCoverageSettings.State> {

    /**
     * Coverage display modes.
     */
    public enum CoverageMode {
        /** Accumulates coverage over time. Shows all executed code since start. */
        CUMULATIVE,
        /** Shows coverage for the latest action only, auto-refreshing. */
        LATEST_REQUEST
    }

    /**
     * Serializable state for settings persistence.
     */
    public static class State {
        public String tcpServerAddress = DEFAULT_TCP_ADDRESS;
        public int tcpServerPort = DEFAULT_TCP_PORT;
        public String classOutputPath = "";
        public String sourceRootPath = "";
        public String classOutputPaths = "";
        public String sourceRootPaths = "";
        public CoverageMode coverageMode = CoverageMode.CUMULATIVE;
    }

    private State myState = new State();

    /**
     * Gets the singleton instance of LiveCoverageSettings.
     */
    @NotNull
    public static LiveCoverageSettings getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(LiveCoverageSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State newState) {
        myState = newState;
    }

    // Getters and Setters

    @NotNull
    public String getTcpServerAddress() {
        return myState.tcpServerAddress != null ? myState.tcpServerAddress : DEFAULT_TCP_ADDRESS;
    }

    public void setTcpServerAddress(@Nullable String address) {
        myState.tcpServerAddress = address != null ? address.trim() : DEFAULT_TCP_ADDRESS;
    }

    public int getTcpServerPort() {
        return myState.tcpServerPort > 0 ? myState.tcpServerPort : DEFAULT_TCP_PORT;
    }

    public void setTcpServerPort(int port) {
        myState.tcpServerPort = port;
    }

    @NotNull
    public String getClassOutputPath() {
        List<String> paths = getClassOutputPaths();
        if (!paths.isEmpty()) {
            return paths.get(0);
        }
        return myState.classOutputPath != null ? myState.classOutputPath : "";
    }

    public void setClassOutputPath(@Nullable String path) {
        String sanitized = path != null ? path.trim() : "";
        myState.classOutputPath = sanitized;
        myState.classOutputPaths = sanitized;
    }

    @NotNull
    public String getSourceRootPath() {
        List<String> paths = getSourceRootPaths();
        if (!paths.isEmpty()) {
            return paths.get(0);
        }
        return myState.sourceRootPath != null ? myState.sourceRootPath : "";
    }

    public void setSourceRootPath(@Nullable String path) {
        String sanitized = path != null ? path.trim() : "";
        myState.sourceRootPath = sanitized;
        myState.sourceRootPaths = sanitized;
    }

    @NotNull
    public List<String> getClassOutputPaths() {
        List<String> paths = parsePaths(myState.classOutputPaths);
        if (!paths.isEmpty()) {
            return paths;
        }
        String legacyPath = myState.classOutputPath;
        if (legacyPath != null && !legacyPath.trim().isEmpty()) {
            return Collections.singletonList(legacyPath.trim());
        }
        return Collections.emptyList();
    }

    public void setClassOutputPaths(@Nullable List<String> paths) {
        List<String> sanitized = sanitizePaths(paths);
        myState.classOutputPaths = String.join("\n", sanitized);
        myState.classOutputPath = sanitized.isEmpty() ? "" : sanitized.get(0);
    }

    @NotNull
    public List<String> getSourceRootPaths() {
        List<String> paths = parsePaths(myState.sourceRootPaths);
        if (!paths.isEmpty()) {
            return paths;
        }
        String legacyPath = myState.sourceRootPath;
        if (legacyPath != null && !legacyPath.trim().isEmpty()) {
            return Collections.singletonList(legacyPath.trim());
        }
        return Collections.emptyList();
    }

    public void setSourceRootPaths(@Nullable List<String> paths) {
        List<String> sanitized = sanitizePaths(paths);
        myState.sourceRootPaths = String.join("\n", sanitized);
        myState.sourceRootPath = sanitized.isEmpty() ? "" : sanitized.get(0);
    }

    @NotNull
    private static List<String> parsePaths(@Nullable String serializedPaths) {
        if (serializedPaths == null || serializedPaths.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String part : serializedPaths.split("\\R")) {
            if (part != null) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    @NotNull
    private static List<String> sanitizePaths(@Nullable List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String path : paths) {
            if (path != null) {
                String trimmed = path.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    @NotNull
    public CoverageMode getCoverageMode() {
        return myState.coverageMode != null ? myState.coverageMode : CoverageMode.CUMULATIVE;
    }

    public void setCoverageMode(@NotNull CoverageMode mode) {
        myState.coverageMode = mode;
    }
}
