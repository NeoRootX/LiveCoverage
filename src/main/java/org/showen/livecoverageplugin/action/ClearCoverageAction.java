package org.showen.livecoverageplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.showen.livecoverageplugin.LiveCoverageComponent;
import org.showen.livecoverageplugin.ui.CoverageToolWindowService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to clear all coverage highlights and data.
 */
public class ClearCoverageAction extends AnAction {

    public ClearCoverageAction() {
        super("Clear Coverage", "Clear all coverage highlights and reset JaCoCo agent", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        LiveCoverageComponent component = LiveCoverageComponent.getInstance(project);
        if (component != null) {
            component.resetHighlighters();
        }

        CoverageToolWindowService toolService = project.getService(CoverageToolWindowService.class);
        if (toolService != null) {
            toolService.clear();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null && !project.isDisposed());
    }
}
