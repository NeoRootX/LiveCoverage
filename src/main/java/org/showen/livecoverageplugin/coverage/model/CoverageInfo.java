package org.showen.livecoverageplugin.coverage.model;

import org.jetbrains.annotations.NotNull;

/**
 * Represents coverage information for a specific line of code.
 * Immutable data class for coverage status.
 */
public record CoverageInfo(
        @NotNull String className,
        int lineNumber,
        @NotNull CoverageStatus status
) {
    /**
     * Coverage status for a line of code.
     */
    public enum CoverageStatus {
        /** Line is fully covered by tests/execution */
        FULL,
        /** Line is partially covered */
        PARTIAL,
        /** Line is not covered */
        NONE
    }

    /**
     * Validates that line number is positive.
     */
    public CoverageInfo {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line number must be positive, got: " + lineNumber);
        }
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("Coverage status cannot be null");
        }
    }
}
