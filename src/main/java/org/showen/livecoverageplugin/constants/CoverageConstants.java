package org.showen.livecoverageplugin.constants;

import java.awt.Color;

/**
 * Constants for Live Coverage Plugin.
 * Centralizes all configuration values for easy maintenance.
 */
public final class CoverageConstants {
    
    private CoverageConstants() {
        // Utility class - prevent instantiation
    }

    // Polling Configuration
    public static final long DEFAULT_POLLING_INTERVAL_MS = 500L;
    public static final long MIN_POLLING_INTERVAL_MS = 100L;
    public static final long MAX_POLLING_INTERVAL_MS = 5000L;

    // Connection Configuration
    public static final String DEFAULT_TCP_ADDRESS = "127.0.0.1";
    public static final int DEFAULT_TCP_PORT = 6300;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;
    public static final int CONNECTION_TIMEOUT_MS = 5000;

    // UI Configuration
    public static final int LIST_VISIBLE_ROW_COUNT = 20;
    public static final int LIST_FIXED_CELL_HEIGHT = 20;

    // Highlighting Colors (with alpha transparency)
    public static final Color FULL_COVERAGE_COLOR = new Color(128, 255, 128, 60); // Light green
    public static final Color PARTIAL_COVERAGE_COLOR = new Color(255, 255, 128, 70); // Light yellow
    
    // Highlighter Layer - Use ADDITIONAL_SYNTAX from HighlighterLayer class
    // Value: 1000 (constant from IntelliJ Platform)
    public static final int HIGHLIGHTER_LAYER = 1000;

    // Default Paths
    public static final String DEFAULT_CLASSES_PATH_RELATIVE_1 = "build/classes/java/main";
    public static final String DEFAULT_CLASSES_PATH_RELATIVE_2 = "target/classes";

    // File Extensions
    public static final String CLASS_EXTENSION = ".class";
    public static final String JAVA_EXTENSION = ".java";
    public static final String EXEC_FILE_PREFIX = "livecov_poll_";
    public static final String EXEC_FILE_SUFFIX = ".exec";

    // Log Messages
    public static final String LOG_PREFIX = "[LiveCoverage] ";
    public static final String MSG_STARTING_POLLING = "Starting coverage polling for project: %s";
    public static final String MSG_STOPPING_POLLING = "Stopping coverage polling for project: %s";
    public static final String MSG_CLASS_FILES_CHANGED = "Class files changed, rebuilding bytecode cache...";
    public static final String MSG_CONNECTION_FAILED = "Failed to connect to JaCoCo agent at %s:%d";

    // UI Messages
    public static final String MSG_NO_COVERAGE_DATA = "No coverage data (run and then dump)";
    public static final String MSG_NO_EXECUTED_METHODS = "No executed methods found.";
    public static final String MSG_CLASS_PREFIX = "Class: ";
    public static final String MSG_METHOD_PREFIX = "  + ";
    public static final String MSG_COVERED_INSTRUCTIONS = "  (covered instructions: %d)";

    // Notification Messages
    public static final String NOTIFICATION_TITLE_SETUP = "Live Coverage Plugin Setup";
    public static final String NOTIFICATION_CONTENT_CONFIG_REQUIRED = 
        "Please configure the classes output path and source root for Live Coverage to work.";
    public static final String NOTIFICATION_ACTION_OPEN_SETTINGS = "Open Settings";
    
    // Validation Messages
    public static final String VALIDATION_TCP_ADDRESS_REQUIRED = "TCP address is required";
    public static final String VALIDATION_TCP_PORT_RANGE = "TCP port must be between %d and %d";
    public static final String VALIDATION_CLASSES_PATH_REQUIRED = "Classes output path is required";
    public static final String VALIDATION_SOURCE_ROOT_REQUIRED = "Source root path is required";
    public static final String VALIDATION_PATH_NOT_EXISTS = "Path does not exist: %s";
    public static final String VALIDATION_PATH_NOT_DIRECTORY = "Path is not a directory: %s";
}
