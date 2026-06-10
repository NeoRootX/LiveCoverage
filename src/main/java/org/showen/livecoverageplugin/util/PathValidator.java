package org.showen.livecoverageplugin.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.showen.livecoverageplugin.constants.CoverageConstants.*;

/**
 * Utility class for validating paths and configuration settings.
 */
public final class PathValidator {
    
    private PathValidator() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates TCP address format.
     * @param address The TCP address to validate
     * @return Validation error message, or null if valid
     */
    @Nullable
    public static String validateTcpAddress(@Nullable String address) {
        if (address == null || address.trim().isEmpty()) {
            return VALIDATION_TCP_ADDRESS_REQUIRED;
        }
        // Basic IP address or hostname validation
        String trimmed = address.trim();
        if (!isValidAddress(trimmed)) {
            return "Invalid TCP address format: " + trimmed;
        }
        return null;
    }

    /**
     * Validates TCP port range.
     * @param port The port number to validate
     * @return Validation error message, or null if valid
     */
    @Nullable
    public static String validateTcpPort(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            return String.format(VALIDATION_TCP_PORT_RANGE, MIN_PORT, MAX_PORT);
        }
        return null;
    }

    /**
     * Validates classes output path.
     * @param path The path to validate
     * @param checkExists Whether to check if the path exists
     * @return Validation error message, or null if valid
     */
    @Nullable
    public static String validateClassesPath(@Nullable String path, boolean checkExists) {
        if (path == null || path.trim().isEmpty()) {
            return VALIDATION_CLASSES_PATH_REQUIRED;
        }
        if (checkExists) {
            File file = new File(path.trim());
            if (!file.exists()) {
                return String.format(VALIDATION_PATH_NOT_EXISTS, path);
            }
            if (!file.isDirectory()) {
                return String.format(VALIDATION_PATH_NOT_DIRECTORY, path);
            }
        }
        return null;
    }

    /**
     * Validates source root path.
     * @param path The path to validate
     * @param checkExists Whether to check if the path exists
     * @return Validation error message, or null if valid
     */
    @Nullable
    public static String validateSourceRootPath(@Nullable String path, boolean checkExists) {
        if (path == null || path.trim().isEmpty()) {
            return VALIDATION_SOURCE_ROOT_REQUIRED;
        }
        if (checkExists) {
            File file = new File(path.trim());
            if (!file.exists()) {
                return String.format(VALIDATION_PATH_NOT_EXISTS, path);
            }
            if (!file.isDirectory()) {
                return String.format(VALIDATION_PATH_NOT_DIRECTORY, path);
            }
        }
        return null;
    }

    /**
     * Validates all settings and returns a list of errors.
     * @param tcpAddress TCP address
     * @param tcpPort TCP port
     * @param classesPath Classes output path
     * @param sourceRootPath Source root path
     * @param checkPaths Whether to check if paths exist
     * @return List of validation error messages (empty if all valid)
     */
    @NotNull
    public static List<String> validateAllSettings(
            @Nullable String tcpAddress,
            int tcpPort,
            @Nullable String classesPath,
            @Nullable String sourceRootPath,
            boolean checkPaths) {
        
        List<String> errors = new ArrayList<>();
        
        String addressError = validateTcpAddress(tcpAddress);
        if (addressError != null) {
            errors.add(addressError);
        }
        
        String portError = validateTcpPort(tcpPort);
        if (portError != null) {
            errors.add(portError);
        }
        
        String classesError = validateClassesPath(classesPath, checkPaths);
        if (classesError != null) {
            errors.add(classesError);
        }
        
        String sourceError = validateSourceRootPath(sourceRootPath, checkPaths);
        if (sourceError != null) {
            errors.add(sourceError);
        }
        
        return errors;
    }

    /**
     * Validates settings with multiple source/classes path pairs.
     */
    @NotNull
    public static List<String> validateAllSettings(
            @Nullable String tcpAddress,
            int tcpPort,
            @NotNull List<String> classesPaths,
            @NotNull List<String> sourceRootPaths,
            boolean checkPaths) {

        List<String> errors = new ArrayList<>();

        String addressError = validateTcpAddress(tcpAddress);
        if (addressError != null) {
            errors.add(addressError);
        }

        String portError = validateTcpPort(tcpPort);
        if (portError != null) {
            errors.add(portError);
        }

        if (classesPaths.isEmpty() || sourceRootPaths.isEmpty()) {
            errors.add("At least one Source output path / Classes output path pair is required");
            return errors;
        }

        if (classesPaths.size() != sourceRootPaths.size()) {
            errors.add("Source output paths and Classes output paths must be configured in pairs");
            return errors;
        }

        for (int i = 0; i < classesPaths.size(); i++) {
            String classesError = validateClassesPath(classesPaths.get(i), checkPaths);
            if (classesError != null) {
                errors.add("Pair " + (i + 1) + " - " + classesError);
            }

            String sourceError = validateSourceRootPath(sourceRootPaths.get(i), checkPaths);
            if (sourceError != null) {
                errors.add("Pair " + (i + 1) + " - " + sourceError);
            }
        }

        return errors;
    }

    /**
     * Simple validation for IP address or hostname.
     */
    private static boolean isValidAddress(@NotNull String address) {
        // IPv4 format: xxx.xxx.xxx.xxx
        if (address.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
            return true;
        }
        // Localhost
        if ("localhost".equalsIgnoreCase(address)) {
            return true;
        }
        // Hostname format (basic check)
        if (address.matches("^[a-zA-Z0-9][a-zA-Z0-9.-]*[a-zA-Z0-9]$|^[a-zA-Z0-9]$")) {
            return true;
        }
        return false;
    }
}
