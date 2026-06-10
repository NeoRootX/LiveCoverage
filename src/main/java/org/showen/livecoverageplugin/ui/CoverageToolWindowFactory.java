package org.showen.livecoverageplugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.showen.livecoverageplugin.ui.icons.CoverageIcons;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the Coverage Tool Window.
 * Registers the tool window UI in the IDE.
 */
public class CoverageToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CoverageToolWindowService service = project.getService(CoverageToolWindowService.class);
        if (service == null) {
            return;
        }

        // Set the tool window icon using CoverageIcons
        toolWindow.setIcon(CoverageIcons.getToolWindowIcon());

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(service.getMainPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
