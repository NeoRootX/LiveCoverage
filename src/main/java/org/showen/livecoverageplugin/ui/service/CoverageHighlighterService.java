package org.showen.livecoverageplugin.ui.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.showen.livecoverageplugin.coverage.model.CoverageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.showen.livecoverageplugin.constants.CoverageConstants.FULL_COVERAGE_COLOR;
import static org.showen.livecoverageplugin.constants.CoverageConstants.PARTIAL_COVERAGE_COLOR;
import static org.showen.livecoverageplugin.constants.CoverageConstants.JAVA_EXTENSION;

/**
 * Service responsible for highlighting covered lines in the editor.
 * Manages highlighters lifecycle and provides thread-safe operations.
 */
public final class CoverageHighlighterService {
    
    private static final Logger LOG = Logger.getInstance(CoverageHighlighterService.class);
    
    private final Project project;
    private final List<RangeHighlighter> highlighters = new CopyOnWriteArrayList<>();

    public CoverageHighlighterService(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Highlights coverage information in editors.
     * This method clears existing highlighters and adds new ones atomically.
     * @param coverageInfos List of coverage information to highlight
     * @param sourceRootPath Root path of source files
     */
    public void highlightCoverage(
            @NotNull List<CoverageInfo> coverageInfos,
            @NotNull List<String> sourceRootPaths) {
        
        if (project.isDisposed()) {
            return;
        }

        // Group coverage info by file
        Map<VirtualFile, List<CoverageInfo>> linesPerFile = groupByFile(coverageInfos, sourceRootPaths);
        
        if (linesPerFile.isEmpty()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }

            WriteCommandAction.runWriteCommandAction(project, () -> {
                // Clear existing highlighters first (synchronously in the same action)
                clearHighlightersSync();
                
                // Then add new highlighters
                for (Map.Entry<VirtualFile, List<CoverageInfo>> entry : linesPerFile.entrySet()) {
                    highlightFile(entry.getKey(), entry.getValue());
                }
            });
        });
    }
    
    /**
     * Synchronously clears all highlighters (must be called from EDT within WriteCommandAction).
     * This is an internal method used to ensure atomicity with highlightCoverage.
     */
    private void clearHighlightersSync() {
        for (RangeHighlighter highlighter : highlighters) {
            try {
                if (highlighter.isValid()) {
                    highlighter.dispose();
                }
            } catch (Exception e) {
                LOG.debug("Error disposing highlighter", e);
            }
        }
        highlighters.clear();
    }

    /**
     * Groups coverage information by source file.
     */
    @NotNull
    private Map<VirtualFile, List<CoverageInfo>> groupByFile(
            @NotNull List<CoverageInfo> coverageInfos,
            @NotNull List<String> sourceRootPaths) {
        
        Map<VirtualFile, List<CoverageInfo>> linesPerFile = new HashMap<>();
        
        ApplicationManager.getApplication().runReadAction(() -> {
            if (project.isDisposed()) {
                return;
            }

            for (CoverageInfo info : coverageInfos) {
                if (info == null || info.className() == null || info.className().isEmpty()) {
                    continue;
                }
                
                String relativePath = info.className().replace('.', '/') + JAVA_EXTENSION;
                VirtualFile vf = findSourceFile(relativePath, sourceRootPaths);
                
                if (vf != null && vf.isValid()) {
                    linesPerFile.computeIfAbsent(vf, k -> new ArrayList<>()).add(info);
                }
            }
        });

        return linesPerFile;
    }

    @Nullable
    private VirtualFile findSourceFile(@NotNull String relativePath, @NotNull List<String> sourceRootPaths) {
        for (String sourceRootPath : sourceRootPaths) {
            if (sourceRootPath == null || sourceRootPath.trim().isEmpty()) {
                continue;
            }
            File root = new File(sourceRootPath);
            File file = new File(root, relativePath);
            if (!file.getAbsolutePath().startsWith(root.getAbsolutePath())) {
                LOG.warn("Invalid file path detected: " + relativePath);
                continue;
            }
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            if (vf != null && vf.isValid()) {
                return vf;
            }
        }
        return null;
    }

    /**
     * Highlights coverage lines in a specific file.
     */
    private void highlightFile(@NotNull VirtualFile virtualFile, @NotNull List<CoverageInfo> coverageInfos) {
        Document doc = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (doc == null) {
            return;
        }

        Editor[] editors = EditorFactory.getInstance().getEditors(doc, project);
        if (editors == null || editors.length == 0) {
            return;
        }
        
        int docLineCount = doc.getLineCount();
        if (docLineCount <= 0) {
            return;
        }
        
        for (Editor editor : editors) {
            if (editor == null || editor.isDisposed()) {
                continue;
            }

            for (CoverageInfo info : coverageInfos) {
                if (info == null) {
                    continue;
                }
                
                int lineNumber = info.lineNumber();
                if (lineNumber <= 0 || lineNumber > docLineCount) {
                    continue;
                }

                Color bgColor = info.status() == CoverageInfo.CoverageStatus.FULL
                        ? FULL_COVERAGE_COLOR
                        : PARTIAL_COVERAGE_COLOR;

                TextAttributes attrs = new TextAttributes(null, bgColor, null, null, Font.PLAIN);
                try {
                    // Use HighlighterLayer.ADDITIONAL_SYNTAX value (1000)
                    RangeHighlighter highlighter = editor.getMarkupModel()
                            .addLineHighlighter(lineNumber - 1, 
                                    com.intellij.openapi.editor.markup.HighlighterLayer.ADDITIONAL_SYNTAX, 
                                    attrs);
                    
                    if (highlighter != null) {
                        highlighters.add(highlighter);
                    }
                } catch (Exception e) {
                    LOG.debug("Failed to add highlighter for line " + lineNumber + " in " + virtualFile.getName(), e);
                    // Continue with other lines
                }
            }
        }
    }

    /**
     * Clears all coverage highlighters.
     */
    public void clearHighlighters() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }

            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (RangeHighlighter highlighter : highlighters) {
                    try {
                        if (highlighter.isValid()) {
                            highlighter.dispose();
                        }
                    } catch (Exception e) {
                        LOG.debug("Error disposing highlighter", e);
                    }
                }
                highlighters.clear();
            });
        });
    }

    /**
     * Gets the number of active highlighters.
     * This method synchronously checks and removes invalid highlighters.
     * @return The number of valid highlighters
     */
    public int getHighlighterCount() {
        // Clean up invalid highlighters synchronously for accurate count
        highlighters.removeIf(highlighter -> {
            try {
                return !highlighter.isValid();
            } catch (Exception e) {
                return true; // Remove if check fails
            }
        });
        return highlighters.size();
    }
}
