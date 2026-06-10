package org.showen.livecoverageplugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import org.showen.livecoverageplugin.settings.LiveCoverageSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import static org.showen.livecoverageplugin.constants.CoverageConstants.*;

/**
 * Startup activity that initializes Live Coverage when a project is opened.
 * Automatically starts polling and notifies user if configuration is needed.
 */
public class PluginStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        LiveCoverageComponent component = LiveCoverageComponent.getInstance(project);
        LiveCoverageSettings settings = LiveCoverageSettings.getInstance();

        if (component != null && settings != null) {
            // Start polling automatically
            // Note: We don't reset highlighters on startup to avoid clearing user's existing coverage
            component.startPolling();

            // Check if configuration is complete
            if (!isConfigurationComplete(settings)) {
                showConfigurationNotification(project);
            }
        }

        return Unit.INSTANCE;
    }

    /**
     * Checks if plugin configuration is complete.
     */
    private boolean isConfigurationComplete(@NotNull LiveCoverageSettings settings) {
        return !settings.getClassOutputPaths().isEmpty()
                && !settings.getSourceRootPaths().isEmpty()
                && settings.getClassOutputPaths().size() == settings.getSourceRootPaths().size();
    }

    /**
     * Shows a notification prompting user to configure the plugin.
     */
    private void showConfigurationNotification(@NotNull Project project) {
        Notification notification = new Notification(
                "LiveCoveragePlugin.NotificationGroup",
                NOTIFICATION_TITLE_SETUP,
                NOTIFICATION_CONTENT_CONFIG_REQUIRED,
                NotificationType.INFORMATION
        );

        notification.addAction(new AnAction(NOTIFICATION_ACTION_OPEN_SETTINGS) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ShowSettingsUtil.getInstance().showSettingsDialog(
                        project,
                        org.showen.livecoverageplugin.ui.LiveCoverageConfigurable.class);
                notification.expire();
            }
        });

        Notifications.Bus.notify(notification, project);
    }
}
