package org.showen.livecoverageplugin.ui.icons;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Custom icons for coverage tree nodes and tool window.
 * Matches the style of pluginIcon.svg with green/teal color scheme.
 */
public final class CoverageIcons {
    
    private CoverageIcons() {
        // Utility class - prevent instantiation
    }

    // Color scheme matching pluginIcon.svg
    private static final Color CLASS_ICON_COLOR = new Color(73, 156, 84);  // #499C54 - green
    private static final Color METHOD_ICON_COLOR = new Color(52, 211, 153); // #34D399 - teal/emerald
    private static final Color METHOD_COVERED_COLOR = new Color(167, 243, 208); // #A7F3D0 - light teal

    // Cached icons
    private static Icon toolWindowIcon;
    private static Icon classIcon;
    private static Icon methodIcon;
    private static Icon methodCoveredIcon;

    /**
     * Icon for the tool window sidebar - loads pluginIcon.svg.
     */
    @NotNull
    public static Icon getToolWindowIcon() {
        if (toolWindowIcon == null) {
            toolWindowIcon = IconLoader.getIcon("/META-INF/toolWindowIcon.svg", CoverageIcons.class);
        }
        return toolWindowIcon;
    }

    /**
     * Icon for class nodes - stylized class icon with green color.
     */
    @NotNull
    public static Icon getClassIcon() {
        if (classIcon == null) {
            classIcon = createClassIcon();
        }
        return classIcon;
    }

    /**
     * Icon for method nodes - stylized method icon with teal color.
     */
    @NotNull
    public static Icon getMethodIcon() {
        if (methodIcon == null) {
            methodIcon = createMethodIcon();
        }
        return methodIcon;
    }

    /**
     * Icon for covered method nodes - brighter teal.
     */
    @NotNull
    public static Icon getMethodCoveredIcon() {
        if (methodCoveredIcon == null) {
            methodCoveredIcon = createMethodCoveredIcon();
        }
        return methodCoveredIcon;
    }

    /**
     * Creates a custom class icon with green color scheme.
     * Inspired by pluginIcon.svg - stylized code braces.
     */
    @NotNull
    private static Icon createClassIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw stylized class representation inspired by pluginIcon.svg
        g.setColor(CLASS_ICON_COLOR);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Draw code braces style (similar to pluginIcon.svg)
        // Left brace
        g.drawLine(3, 4, 3, 6);
        g.drawLine(3, 6, 5, 6);
        g.drawLine(3, 10, 5, 10);
        g.drawLine(3, 10, 3, 12);
        
        // Right brace
        g.drawLine(13, 4, 13, 6);
        g.drawLine(11, 6, 13, 6);
        g.drawLine(11, 10, 13, 10);
        g.drawLine(13, 10, 13, 12);
        
        // Class structure lines (representing code inside)
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(6, 8, 10, 8);
        
        g.dispose();
        return new ImageIcon(image);
    }

    /**
     * Creates a method icon with teal color scheme.
     * Inspired by pluginIcon.svg - code lines representing coverage.
     */
    @NotNull
    private static Icon createMethodIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw method representation (code lines like in pluginIcon.svg)
        g.setColor(METHOD_ICON_COLOR);
        
        // Draw rounded rectangles representing code lines (similar to pluginIcon.svg)
        // Top line (short)
        g.fillRoundRect(3, 5, 6, 2, 1, 1);
        
        // Middle line (medium)
        g.fillRoundRect(3, 8, 8, 2, 1, 1);
        
        // Bottom line (short)
        g.fillRoundRect(3, 11, 5, 2, 1, 1);
        
        g.dispose();
        return new ImageIcon(image);
    }

    /**
     * Creates a covered method icon (brighter teal with pulse indicator).
     * Inspired by pluginIcon.svg - has a pulse indicator circle.
     */
    @NotNull
    private static Icon createMethodCoveredIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw method representation with coverage indicator
        g.setColor(METHOD_COVERED_COLOR);
        
        // Draw rounded rectangles representing covered code lines
        // Top line (covered - longer)
        g.fillRoundRect(3, 5, 7, 2, 1, 1);
        
        // Middle line (covered - longer)
        g.fillRoundRect(3, 8, 9, 2, 1, 1);
        
        // Bottom line (covered - longer)
        g.fillRoundRect(3, 11, 6, 2, 1, 1);
        
        // Add pulse indicator circle (like in pluginIcon.svg)
        g.setColor(new Color(167, 243, 208, 200)); // Slightly transparent
        g.fillOval(12, 6, 3, 3);
        
        g.dispose();
        return new ImageIcon(image);
    }
}
