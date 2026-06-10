package org.showen.livecoverageplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.showen.livecoverageplugin.LiveCoverageComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Action to manually refresh coverage data.
 * Can be triggered from the menu or toolbar.
 */
public class RefreshCoverageAction extends AnAction {

    public RefreshCoverageAction() {
        super("Refresh Coverage", "Manually refresh code coverage data from JaCoCo agent", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        LiveCoverageComponent component = LiveCoverageComponent.getInstance(project);
        if (component != null) {
            component.refreshCoverage();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null && !project.isDisposed());
    }
}
