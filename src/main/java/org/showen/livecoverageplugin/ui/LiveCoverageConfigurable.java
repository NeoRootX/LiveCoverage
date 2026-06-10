package org.showen.livecoverageplugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import org.showen.livecoverageplugin.coverage.service.JaCoCoAgentService;
import org.showen.livecoverageplugin.settings.LiveCoverageSettings;
import org.showen.livecoverageplugin.util.PathValidator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration UI for Live Coverage Plugin settings.
 * Provides a user-friendly interface for configuring connection and paths.
 */
public class LiveCoverageConfigurable implements Configurable {

    private static final int CONTENT_WIDTH = 760;
    private JPanel mainPanel;
    private final JTextField tcpAddressField = new JBTextField();
    private final JTextField tcpPortField = new JBTextField();
    private JPanel pathPairsPanel;
    private final List<PathPairRow> pathPairRows = new ArrayList<>();
    private final JTextField jvmArgumentField = new JBTextField();
    private final JBLabel validationLabel = new JBLabel("", SwingConstants.LEFT);

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Live Coverage";
    }

    @Override
    public @Nullable JComponent createComponent() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JBLabel noteLabel = new JBLabel(
                "Note: Coverage accumulates over time. Use 'Clear Coverage' action to reset.",
                UIUtil.ComponentStyle.SMALL);
        noteLabel.setForeground(UIUtil.getContextHelpForeground());

        // Configure JVM argument field (read-only, for copying)
        tcpAddressField.setColumns(24);
        tcpPortField.setColumns(8);
        jvmArgumentField.setColumns(38);
        jvmArgumentField.setEditable(false);
        jvmArgumentField.setBackground(UIUtil.getTextFieldBackground());
        jvmArgumentField.setFont(jvmArgumentField.getFont().deriveFont(Font.PLAIN, jvmArgumentField.getFont().getSize() - 1f));
        
        // Add copy button for JVM argument
        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            String jvmArg = jvmArgumentField.getText();
            if (jvmArg != null && !jvmArg.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(jvmArg), null);
            }
        });
        
        JPanel jvmArgPanel = new JPanel(new BorderLayout());
        jvmArgPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        jvmArgPanel.add(jvmArgumentField, BorderLayout.CENTER);
        jvmArgPanel.add(copyButton, BorderLayout.EAST);

        // Configure validation label
        validationLabel.setForeground(UIUtil.getErrorForeground());
        validationLabel.setVisible(false);
        validationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add info label about automatic injection
        JBLabel autoInjectLabel = new JBLabel(
                "✓ JVM argument will be automatically added to run configurations (no manual setup needed)",
                UIUtil.ComponentStyle.SMALL);
        autoInjectLabel.setForeground(new Color(0, 150, 0));

        JPanel tcpPanel = new JPanel(new GridBagLayout());
        tcpPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 0, 3, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        tcpPanel.add(labeledField("JaCoCo Agent TCP Address:", tcpAddressField), gbc);
        gbc.gridy = 1;
        tcpPanel.add(labeledField("JaCoCo Agent TCP Port:", tcpPortField), gbc);

        pathPairsPanel = new JPanel();
        pathPairsPanel.setLayout(new BoxLayout(pathPairsPanel, BoxLayout.Y_AXIS));
        pathPairsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addPairButton = new JButton("+ Add Path Pair");
        addPairButton.addActionListener(e -> {
            addPathPairRow("", "");
            validateAndShow();
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        JPanel pathHeaderPanel = new JPanel(new BorderLayout());
        pathHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JBLabel pathHintLabel = new JBLabel("Source output path and Classes output path are configured in pairs.");
        pathHintLabel.setForeground(UIUtil.getContextHelpForeground());
        pathHintLabel.setFont(pathHintLabel.getFont().deriveFont(Font.PLAIN, pathHintLabel.getFont().getSize() - 1f));
        pathHeaderPanel.add(pathHintLabel, BorderLayout.WEST);
        pathHeaderPanel.add(addPairButton, BorderLayout.EAST);

        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoInjectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(noteLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        TitledSeparator jvmSeparator = new TitledSeparator("JVM Argument (automatically added to run configurations)");
        jvmSeparator.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(limitWidth(jvmSeparator));
        mainPanel.add(autoInjectLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(limitWidth(labeledField("JVM argument (for reference):", jvmArgPanel)));
        mainPanel.add(Box.createVerticalStrut(10));
        TitledSeparator connectionSeparator = new TitledSeparator("Connection & Paths");
        connectionSeparator.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(limitWidth(connectionSeparator));
        mainPanel.add(limitWidth(tcpPanel));
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(limitWidth(pathHeaderPanel));
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(limitWidth(pathPairsPanel));
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(limitWidth(validationLabel));
        mainPanel.add(Box.createVerticalGlue());

        // Add listeners for real-time validation
        tcpAddressField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
        });

        tcpPortField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { 
                validateAndShow();
                updateJvmArgument();
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { 
                validateAndShow();
                updateJvmArgument();
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { 
                validateAndShow();
                updateJvmArgument();
            }
        });
        
        tcpAddressField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateJvmArgument(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateJvmArgument(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateJvmArgument(); }
        });

        // Initial JVM argument update
        updateJvmArgument();
        if (pathPairRows.isEmpty()) {
            addPathPairRow("", "");
        }

        return mainPanel;
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.add(new JBLabel(label), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JComponent limitWidth(JComponent component) {
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(CONTENT_WIDTH, Math.max(preferred.height, 1)));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    private void addPathPairRow(String sourcePath, String classesPath) {
        PathPairRow row = new PathPairRow(sourcePath, classesPath);
        pathPairRows.add(row);
        pathPairsPanel.add(limitWidth(row.rowPanel));
    }

    private void removePathPairRow(PathPairRow row) {
        if (pathPairRows.size() <= 1) {
            return;
        }
        pathPairRows.remove(row);
        pathPairsPanel.remove(row.rowPanel);
    }

    private List<String> getSourcePathsFromUi() {
        List<String> paths = new ArrayList<>();
        for (PathPairRow row : pathPairRows) {
            String value = row.sourceField.getText().trim();
            if (!value.isEmpty()) {
                paths.add(value);
            }
        }
        return paths;
    }

    private List<String> getClassPathsFromUi() {
        List<String> paths = new ArrayList<>();
        for (PathPairRow row : pathPairRows) {
            String value = row.classesField.getText().trim();
            if (!value.isEmpty()) {
                paths.add(value);
            }
        }
        return paths;
    }
    
    /**
     * Updates the JVM argument field based on current settings.
     */
    private void updateJvmArgument() {
        try {
            JaCoCoAgentService agentService = JaCoCoAgentService.getInstance();
            if (agentService == null) {
                jvmArgumentField.setText("JaCoCo agent service not available");
                return;
            }
            
            String address = tcpAddressField.getText().trim();
            if (address.isEmpty()) {
                address = "127.0.0.1";
            }
            
            int port = parsePort();
            if (port <= 0) {
                port = 6300;
            }
            
            String jvmArg = agentService.getJvmArgument(address, port);
            if (jvmArg != null) {
                jvmArgumentField.setText(jvmArg);
            } else {
                jvmArgumentField.setText("JaCoCo agent JAR not found - please rebuild plugin");
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            jvmArgumentField.setText("Error generating JVM argument: " + errorMsg);
        }
    }

    /**
     * Validates current input and shows error messages.
     */
    private void validateAndShow() {
        List<String> errors = PathValidator.validateAllSettings(
                tcpAddressField.getText(),
                parsePort(),
                getClassPathsFromUi(),
                getSourcePathsFromUi(),
                false // Don't check file existence during typing
        );

        if (errors.isEmpty()) {
            validationLabel.setText("");
            validationLabel.setVisible(false);
        } else {
            validationLabel.setText("Validation: " + String.join("; ", errors));
            validationLabel.setVisible(true);
        }
    }

    /**
     * Parses port number from text field.
     */
    private int parsePort() {
        try {
            return Integer.parseInt(tcpPortField.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean isModified() {
        LiveCoverageSettings settings = LiveCoverageSettings.getInstance();
        if (settings == null) {
            return false;
        }

        int port = parsePort();
        boolean portModified = port != settings.getTcpServerPort();

        return !tcpAddressField.getText().equals(settings.getTcpServerAddress())
                || portModified
                || !getClassPathsFromUi().equals(settings.getClassOutputPaths())
                || !getSourcePathsFromUi().equals(settings.getSourceRootPaths());
    }

    @Override
    public void apply() throws ConfigurationException {
        LiveCoverageSettings settings = LiveCoverageSettings.getInstance();
        if (settings == null) {
            return;
        }

        // Validate before applying
        int port = parsePort();
        List<String> errors = PathValidator.validateAllSettings(
                tcpAddressField.getText(),
                port,
                getClassPathsFromUi(),
                getSourcePathsFromUi(),
                false // Don't require paths to exist on apply
        );

        if (!errors.isEmpty()) {
            throw new ConfigurationException(String.join("\n", errors), "Validation Error");
        }

        // Apply settings
        settings.setTcpServerAddress(tcpAddressField.getText());
        settings.setTcpServerPort(port);
        settings.setClassOutputPaths(getClassPathsFromUi());
        settings.setSourceRootPaths(getSourcePathsFromUi());

        // Clear validation label
        validationLabel.setText("");
        validationLabel.setVisible(false);
    }

    @Override
    public void reset() {
        LiveCoverageSettings settings = LiveCoverageSettings.getInstance();
        if (settings == null || pathPairsPanel == null || mainPanel == null) {
            return;
        }

        tcpAddressField.setText(settings.getTcpServerAddress());
        tcpPortField.setText(String.valueOf(settings.getTcpServerPort()));
        pathPairsPanel.removeAll();
        pathPairRows.clear();
        List<String> sourcePaths = settings.getSourceRootPaths();
        List<String> classPaths = settings.getClassOutputPaths();
        int rowCount = Math.max(sourcePaths.size(), classPaths.size());
        if (rowCount == 0) {
            addPathPairRow("", "");
        } else {
            for (int i = 0; i < rowCount; i++) {
                String source = i < sourcePaths.size() ? sourcePaths.get(i) : "";
                String classes = i < classPaths.size() ? classPaths.get(i) : "";
                addPathPairRow(source, classes);
            }
        }
        
        updateJvmArgument();
        
        validationLabel.setText("");
        validationLabel.setVisible(false);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private final class PathPairRow {
        private final JPanel rowPanel;
        private final JTextField sourceField;
        private final JTextField classesField;

        private PathPairRow(String sourcePath, String classesPath) {
            sourceField = new JBTextField(sourcePath);
            classesField = new JBTextField(classesPath);
            sourceField.setColumns(36);
            classesField.setColumns(36);

            sourceField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void changedUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
            });
            classesField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void changedUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { validateAndShow(); }
            });

            JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(e -> {
                removePathPairRow(this);
                validateAndShow();
                mainPanel.revalidate();
                mainPanel.repaint();
            });

            rowPanel = new JPanel(new GridBagLayout());
            rowPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(2, 0, 2, 0);
            gbc.weightx = 1.0;
            gbc.gridx = 0;
            gbc.gridy = 0;
            rowPanel.add(labeledField("Source output path:", sourceField), gbc);
            gbc.gridy = 1;
            rowPanel.add(labeledField("Classes output path:", classesField), gbc);
            gbc.gridy = 2;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.EAST;
            rowPanel.add(removeButton, gbc);
            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(6, 0, 2, 0);
            rowPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }
}
