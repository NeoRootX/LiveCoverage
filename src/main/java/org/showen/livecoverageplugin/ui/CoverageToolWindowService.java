package org.showen.livecoverageplugin.ui;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import javax.swing.JTree;

import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.showen.livecoverageplugin.LiveCoverageComponent;
import org.showen.livecoverageplugin.ui.icons.CoverageIcons;
import org.showen.livecoverageplugin.ui.model.ClassMethodTreeNode;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for managing the Coverage Tool Window UI.
 * Displays coverage statistics using a tree structure showing classes and methods.
 */
@Service(Service.Level.PROJECT)
public final class CoverageToolWindowService {
    
    private static final Logger LOG = Logger.getInstance(CoverageToolWindowService.class);
    
    private final Project project;
    private final JPanel mainPanel;
    private final JTree coverageTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final JBLabel statusLabel;
    private final JButton clearButton;
    private final JButton pollingToggleButton;
    
    // Cache for tree nodes to maintain expansion state
    private final Map<String, ClassMethodTreeNode> classNodeCache = new HashMap<>();
    
    // Track last coverage data to avoid unnecessary updates
    private CoverageBuilder lastCoverageBuilder = null;

    public CoverageToolWindowService(@NotNull Project project) {
        this.project = project;
        
        // Initialize root node
        this.rootNode = new DefaultMutableTreeNode("Coverage Data");
        this.treeModel = new DefaultTreeModel(rootNode);
        this.coverageTree = new Tree(treeModel);
        
        // Configure tree
        coverageTree.setRootVisible(false);
        coverageTree.setShowsRootHandles(true);
        coverageTree.setCellRenderer(new CoverageTreeCellRenderer());
        
        // Expand all nodes by default for better visibility
        coverageTree.setExpandsSelectedPaths(true);
        
        // Create status label
        statusLabel = new JBLabel("Ready", SwingConstants.LEFT);
        statusLabel.setBorder(JBUI.Borders.empty(5));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, statusLabel.getFont().getSize() - 1f));
        
        // Create clear/reset button
        clearButton = new JButton("Clear Coverage");
        clearButton.setToolTipText("Clear all coverage highlights and reset JaCoCo agent");
        clearButton.addActionListener(e -> clearCoverage());

        pollingToggleButton = new JButton("Pause Coverage");
        pollingToggleButton.setToolTipText("Pause or resume live coverage polling and highlighting");
        pollingToggleButton.addActionListener(e -> togglePolling());
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonPanel.setBorder(JBUI.Borders.empty(5, 5, 0, 5));
        buttonPanel.add(clearButton);
        buttonPanel.add(pollingToggleButton);
        
        // Create status panel with button
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Build UI
        JScrollPane scrollPane = new JScrollPane(coverageTree);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        refreshPollingButtonState();
    }
    
    /**
     * Clears coverage data and resets the JaCoCo agent.
     * Called when the clear button is clicked.
     */
    private void clearCoverage() {
        if (project.isDisposed()) {
            return;
        }
        
        // Clear highlighters and reset agent
        LiveCoverageComponent component = LiveCoverageComponent.getInstance(project);
        if (component != null) {
            component.resetHighlighters();
        }
        
        // Clear tool window
        clear();
    }

    private void togglePolling() {
        if (project.isDisposed()) {
            return;
        }
        LiveCoverageComponent component = LiveCoverageComponent.getInstance(project);
        if (component == null) {
            return;
        }
        if (component.isPolling()) {
            component.stopPolling();
        } else {
            component.startPolling();
        }
        refreshPollingButtonState();
    }

    private void refreshPollingButtonState() {
        LiveCoverageComponent component = LiveCoverageComponent.getInstance(project);
        boolean polling = component != null && component.isPolling();
        pollingToggleButton.setText(polling ? "Pause Coverage" : "Resume Coverage");
    }

    /**
     * Gets the main panel for the tool window.
     */
    @NotNull
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Updates the tool window content with coverage data.
     * @param coverageBuilder The coverage builder containing analysis results
     */
    public void updateContent(@Nullable CoverageBuilder coverageBuilder) {
        SwingUtilities.invokeLater(() -> {
            // Check if data has actually changed to avoid unnecessary updates
            if (coverageBuilder == null || coverageBuilder.getClasses().isEmpty()) {
                // Only update if we had data before (to avoid flickering when data is empty)
                if (lastCoverageBuilder != null && !lastCoverageBuilder.getClasses().isEmpty()) {
                    rootNode.removeAllChildren();
                    classNodeCache.clear();
                    treeModel.reload(rootNode);
                    updateStatus("No coverage data - please run the application and refresh");
                    lastCoverageBuilder = coverageBuilder;
                } else if (lastCoverageBuilder == null) {
                    // First time, show empty state
                    updateStatus("No coverage data - please run the application and refresh");
                }
                return;
            }
            
            // Check if coverage data has changed by comparing class counts and method counts
            // This optimization prevents unnecessary UI updates and flickering
            boolean dataChanged = false;
            if (lastCoverageBuilder == null || lastCoverageBuilder.getClasses().isEmpty()) {
                dataChanged = true;
            } else {
                // Quick check: compare class counts first
                int oldClassCount = lastCoverageBuilder.getClasses().size();
                int newClassCount = coverageBuilder.getClasses().size();
                if (oldClassCount != newClassCount) {
                    dataChanged = true;
                } else {
                    // Deep check: compare method counts and coverage status per class
                    java.util.Iterator<IClassCoverage> oldIt = lastCoverageBuilder.getClasses().iterator();
                    java.util.Iterator<IClassCoverage> newIt = coverageBuilder.getClasses().iterator();
                    while (oldIt.hasNext() && newIt.hasNext() && !dataChanged) {
                        IClassCoverage oldClass = oldIt.next();
                        IClassCoverage newClass = newIt.next();
                        
                        // Compare class names
                        String oldClassName = oldClass.getName();
                        String newClassName = newClass.getName();
                        if (oldClassName == null || newClassName == null || !oldClassName.equals(newClassName)) {
                            dataChanged = true;
                            break;
                        }
                        
                        // Compare method counts
                        java.util.Collection<IMethodCoverage> oldMethods = oldClass.getMethods();
                        java.util.Collection<IMethodCoverage> newMethods = newClass.getMethods();
                        int oldMethodCount = (oldMethods != null) ? oldMethods.size() : 0;
                        int newMethodCount = (newMethods != null) ? newMethods.size() : 0;
                        if (oldMethodCount != newMethodCount) {
                            dataChanged = true;
                            break;
                        }
                        
                        // Compare instruction coverage (more accurate than just method count)
                        try {
                            int oldCovered = oldClass.getInstructionCounter().getCoveredCount();
                            int newCovered = newClass.getInstructionCounter().getCoveredCount();
                            if (oldCovered != newCovered) {
                                dataChanged = true;
                                break;
                            }
                        } catch (Exception e) {
                            // If counter access fails, assume data changed to be safe
                            dataChanged = true;
                            break;
                        }
                    }
                }
            }
            
            // Only update if data has changed - this prevents flickering
            if (!dataChanged && lastCoverageBuilder != null) {
                return; // Skip update to avoid flickering
            }
            
            // Store expanded paths to restore after update
            java.util.Enumeration<TreePath> expandedEnum = coverageTree.getExpandedDescendants(
                    new TreePath(rootNode.getPath()));
            java.util.List<TreePath> expandedPaths = new java.util.ArrayList<>();
            if (expandedEnum != null) {
                while (expandedEnum.hasMoreElements()) {
                    expandedPaths.add(expandedEnum.nextElement());
                }
            }
            
            // Clear existing nodes
            rootNode.removeAllChildren();
            classNodeCache.clear();

            int totalClasses = 0;
            int coveredClasses = 0;
            int totalMethods = 0;
            int coveredMethods = 0;
            
            // Build tree structure
            for (IClassCoverage classCoverage : coverageBuilder.getClasses()) {
                if (classCoverage == null) {
                    continue;
                }
                
                String className = classCoverage.getName();
                if (className == null || className.isEmpty()) {
                    continue;
                }
                className = className.replace('/', '.');
                totalClasses++;
                
                // Create class node
                ClassMethodTreeNode classNode = new ClassMethodTreeNode(
                        ClassMethodTreeNode.NodeType.CLASS, className);
                
                int classCoveredMethods = 0;
                int classTotalMethods = 0;
                int classCoveredInstructions = 0;
                int classTotalInstructions = 0;
                
                // Add method nodes
                java.util.Collection<IMethodCoverage> methods = classCoverage.getMethods();
                if (methods != null) {
                    for (IMethodCoverage methodCoverage : methods) {
                        if (methodCoverage == null) {
                            continue;
                        }
                        
                        classTotalMethods++;
                        totalMethods++;
                        
                        try {
                            int methodCoveredInstructions = methodCoverage.getInstructionCounter().getCoveredCount();
                            int methodTotalInstructions = methodCoverage.getInstructionCounter().getTotalCount();
                            
                            classCoveredInstructions += methodCoveredInstructions;
                            classTotalInstructions += methodTotalInstructions;
                            
                            if (methodCoveredInstructions > 0) {
                                classCoveredMethods++;
                                coveredMethods++;
                                
                                // Format method signature nicely
                                String methodName = formatMethodName(
                                        methodCoverage.getName() != null ? methodCoverage.getName() : "unknown",
                                        methodCoverage.getDesc() != null ? methodCoverage.getDesc() : "");
                                
                                ClassMethodTreeNode methodNode = new ClassMethodTreeNode(
                                        ClassMethodTreeNode.NodeType.METHOD,
                                        methodName,
                                        methodCoveredInstructions,
                                        methodTotalInstructions);
                                
                                classNode.add(methodNode);
                            }
                        } catch (Exception e) {
                            LOG.debug("Error processing method in class " + className, e);
                            // Continue with other methods
                        }
                    }
                }
                
                // Only show classes that have at least one executed method
                if (classCoveredMethods > 0) {
                    coveredClasses++;
                    classNode.setCoveredInstructions(classCoveredInstructions);
                    classNode.setTotalInstructions(classTotalInstructions);
                    
                    // Update class display name with summary
                    classNode.setUserObject(String.format("%s (%d/%d methods executed)", 
                            className, classCoveredMethods, classTotalMethods));
                    
                    rootNode.add(classNode);
                    classNodeCache.put(className, classNode);
                }
            }

            // Reload tree model
            treeModel.reload(rootNode);
            
            // Restore expansion state
            if (!expandedPaths.isEmpty()) {
                for (TreePath path : expandedPaths) {
                    if (path != null) {
                        coverageTree.expandPath(path);
                    }
                }
            } else {
                // Expand all by default on first load
                expandAllNodes();
            }
            
            // Update status
            if (coveredClasses == 0) {
                updateStatus("No executed methods found");
            } else {
                updateStatus(String.format("Covered: %d/%d classes, %d/%d methods (%.1f%%)",
                        coveredClasses, totalClasses,
                        coveredMethods, totalMethods,
                        totalMethods > 0 ? (100.0 * coveredMethods / totalMethods) : 0.0));
            }
            
            // Save current coverage builder for comparison
            lastCoverageBuilder = coverageBuilder;
        });
    }

    /**
     * Formats method name and descriptor into a readable signature.
     */
    @NotNull
    private String formatMethodName(@NotNull String methodName, @NotNull String descriptor) {
        // Simple formatting - just show method name
        // descriptor contains type info but can be complex, so we simplify
        if ("<init>".equals(methodName)) {
            return "Constructor";
        } else if ("<clinit>".equals(methodName)) {
            return "Static initializer";
        } else {
            // Extract return type and parameters from descriptor would be nice,
            // but for simplicity, just show method name
            return methodName;
        }
    }

    /**
     * Expands all nodes in the tree.
     */
    private void expandAllNodes() {
        DefaultMutableTreeNode node = rootNode;
        do {
            TreePath path = new TreePath(node.getPath());
            coverageTree.expandPath(path);
            node = node.getNextNode();
        } while (node != null);
    }

    /**
     * Updates the status label with connection information.
     * @param message Status message
     */
    public void updateStatus(@NotNull String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setToolTipText(message);
            refreshPollingButtonState();
        });
    }

    /**
     * Clears the tool window content.
     */
    public void clear() {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            classNodeCache.clear();
            lastCoverageBuilder = null;
            treeModel.reload(rootNode);
            updateStatus("Cleared - Ready");
        });
    }
    
    /**
     * Custom tree cell renderer for better display of coverage information.
     */
    private static class CoverageTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {
            Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                
                if (userObject instanceof ClassMethodTreeNode) {
                    ClassMethodTreeNode coverageNode = (ClassMethodTreeNode) userObject;
                    
                    // Set icon based on node type - using custom icons matching pluginIcon.svg style
                    if (coverageNode.getNodeType() == ClassMethodTreeNode.NodeType.CLASS) {
                        // Class icon - green color scheme
                        setIcon(CoverageIcons.getClassIcon());
                        // Expand/collapse icon will be shown automatically by JTree
                        if (expanded) {
                            // When expanded, tree handles show the collapse icon
                        }
                    } else {
                        // Method node - use method icon with coverage-based color
                        setIcon(CoverageIcons.getMethodIcon());
                        
                        // Set text color based on coverage percentage
                        double coverage = coverageNode.getCoveragePercentage();
                        if (coverage >= 80) {
                            setForeground(new Color(52, 211, 153)); // Teal green (#34D399)
                        } else if (coverage >= 50) {
                            setForeground(new Color(167, 243, 208)); // Light teal (#A7F3D0)
                        } else {
                            setForeground(new Color(73, 156, 84)); // Green (#499C54)
                        }
                        
                        // For well-covered methods, use the covered icon variant
                        if (coverage >= 80) {
                            setIcon(CoverageIcons.getMethodCoveredIcon());
                        }
                    }
                }
            }
            
            return component;
        }
    }
}
