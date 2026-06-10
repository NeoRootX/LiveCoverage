package org.showen.livecoverageplugin.ui.model;

import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree node for displaying class and method hierarchy in the coverage tool window.
 */
public class ClassMethodTreeNode extends DefaultMutableTreeNode {
    
    public enum NodeType {
        CLASS,
        METHOD
    }
    
    private final NodeType nodeType;
    private final String displayName;
    private int coveredInstructions;
    private int totalInstructions;
    
    public ClassMethodTreeNode(@NotNull NodeType nodeType, @NotNull String displayName) {
        super(displayName);
        this.nodeType = nodeType;
        this.displayName = displayName;
        this.coveredInstructions = 0;
        this.totalInstructions = 0;
    }
    
    public ClassMethodTreeNode(@NotNull NodeType nodeType, @NotNull String displayName, 
                               int coveredInstructions, int totalInstructions) {
        super(displayName);
        this.nodeType = nodeType;
        this.displayName = displayName;
        this.coveredInstructions = coveredInstructions;
        this.totalInstructions = totalInstructions;
    }
    
    @NotNull
    public NodeType getNodeType() {
        return nodeType;
    }
    
    @NotNull
    public String getDisplayName() {
        return displayName;
    }
    
    public int getCoveredInstructions() {
        return coveredInstructions;
    }
    
    public int getTotalInstructions() {
        return totalInstructions;
    }
    
    public void setCoveredInstructions(int coveredInstructions) {
        this.coveredInstructions = coveredInstructions;
    }
    
    public void setTotalInstructions(int totalInstructions) {
        this.totalInstructions = totalInstructions;
    }
    
    public double getCoveragePercentage() {
        if (totalInstructions == 0) {
            return 0.0;
        }
        return (100.0 * coveredInstructions) / totalInstructions;
    }
    
    @Override
    public String toString() {
        if (nodeType == NodeType.CLASS) {
            return displayName;
        } else {
            // Method node: show name and coverage info
            if (totalInstructions > 0) {
                return String.format("%s [%d/%d instructions, %.1f%%]", 
                        displayName, coveredInstructions, totalInstructions, getCoveragePercentage());
            } else {
                return displayName;
            }
        }
    }
}
