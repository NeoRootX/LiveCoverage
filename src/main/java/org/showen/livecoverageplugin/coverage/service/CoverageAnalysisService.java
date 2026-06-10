package org.showen.livecoverageplugin.coverage.service;

import com.intellij.openapi.diagnostic.Logger;
import org.showen.livecoverageplugin.coverage.model.CoverageInfo;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for analyzing JaCoCo coverage data.
 * Handles the conversion between JaCoCo coverage data and application models.
 */
public final class CoverageAnalysisService {
    
    private static final Logger LOG = Logger.getInstance(CoverageAnalysisService.class);

    /**
     * Converts a CoverageBuilder to a list of CoverageInfo objects.
     * @param builder The coverage builder containing analysis results
     * @return List of coverage information for source code lines
     */
    @NotNull
    public List<CoverageInfo> convertToCoverageInfos(@Nullable CoverageBuilder builder) {
        List<CoverageInfo> infos = new ArrayList<>();
        
        if (builder == null || builder.getClasses().isEmpty()) {
            return infos;
        }

        for (IClassCoverage classCoverage : builder.getClasses()) {
            if (classCoverage == null) {
                continue;
            }
            
            String className = classCoverage.getName();
            if (className == null || className.isEmpty()) {
                continue;
            }
            className = className.replace('/', '.');
            
            int firstLine = classCoverage.getFirstLine();
            int lastLine = classCoverage.getLastLine();
            
            // Validate line numbers
            if (firstLine <= 0 || lastLine < firstLine) {
                LOG.debug("Invalid line range for class " + className + ": " + firstLine + "-" + lastLine);
                continue;
            }
            
            for (int lineNum = firstLine; lineNum <= lastLine; lineNum++) {
                ILine line = classCoverage.getLine(lineNum);
                if (line == null) {
                    continue;
                }
                
                int status = line.getStatus();
                
                if (status == ICounter.FULLY_COVERED) {
                    infos.add(new CoverageInfo(className, lineNum, CoverageInfo.CoverageStatus.FULL));
                } else if (status == ICounter.PARTLY_COVERED) {
                    infos.add(new CoverageInfo(className, lineNum, CoverageInfo.CoverageStatus.PARTIAL));
                }
            }
        }

        return infos;
    }

    /**
     * Analyzes coverage data from an execution data store.
     * @param executionDataStore The execution data store from JaCoCo
     * @param classesDir Directory containing compiled class files
     * @param classBytecodeCache Cache of class bytecode (fallback if classesDir is null)
     * @return CoverageBuilder with analysis results
     */
    @NotNull
    public CoverageBuilder analyzeCoverage(
            @NotNull ExecutionDataStore executionDataStore,
            @Nullable File classesDir,
            @NotNull java.util.Map<String, byte[]> classBytecodeCache) {
        List<File> dirs = new ArrayList<>();
        if (classesDir != null) {
            dirs.add(classesDir);
        }
        return analyzeCoverage(executionDataStore, dirs, classBytecodeCache);
    }

    /**
     * Analyzes coverage data across multiple classes directories.
     */
    @NotNull
    public CoverageBuilder analyzeCoverage(
            @NotNull ExecutionDataStore executionDataStore,
            @NotNull List<File> classesDirs,
            @NotNull java.util.Map<String, byte[]> classBytecodeCache) {
        
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionDataStore, builder);

        try {
            boolean analyzedAtLeastOneDir = false;
            for (File classesDir : classesDirs) {
                if (classesDir != null && classesDir.exists() && classesDir.isDirectory()) {
                    analyzer.analyzeAll(classesDir);
                    analyzedAtLeastOneDir = true;
                }
            }
            if (analyzedAtLeastOneDir) {
                return builder;
            } else if (!classBytecodeCache.isEmpty()) {
                LOG.debug("Using bytecode cache for coverage analysis (classesDirs not available)");
                for (java.util.Map.Entry<String, byte[]> entry : classBytecodeCache.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null && entry.getValue().length > 0) {
                        try {
                            analyzer.analyzeClass(entry.getValue(), entry.getKey());
                        } catch (Exception e) {
                            LOG.debug("Failed to analyze class: " + entry.getKey(), e);
                        }
                    }
                }
            } else {
                LOG.warn("No classes directory or bytecode cache available for coverage analysis");
            }
        } catch (IOException e) {
            LOG.warn("Error analyzing coverage data", e);
        } catch (Exception e) {
            LOG.error("Unexpected error during coverage analysis", e);
        }

        return builder;
    }
}
