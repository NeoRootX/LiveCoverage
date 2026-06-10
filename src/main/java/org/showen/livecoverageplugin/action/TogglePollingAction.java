package org.showen.livecoverageplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.showen.livecoverageplugin.LiveCoverageComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Action to toggle coverage polling on/off.
 */
public class TogglePollingAction extends AnAction {

    public TogglePollingAction() {
        super("Pause Coverage", "Pause or resume automatic coverage polling", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        LiveCoverageComponent component = LiveCoverageComponent.getInstance(project);
        if (component == null) {
            return;
        }

        if (component.isPolling()) {
            component.stopPolling();
            e.getPresentation().setText("Resume Coverage");
        } else {
            component.startPolling();
            e.getPresentation().setText("Pause Coverage");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null && !project.isDisposed();
        e.getPresentation().setEnabled(enabled);

        if (enabled) {
            LiveCoverageComponent component = LiveCoverageComponent.getInstance(project);
            if (component != null && component.isPolling()) {
                e.getPresentation().setText("Pause Coverage");
            } else {
                e.getPresentation().setText("Resume Coverage");
            }
        }
    }
}
