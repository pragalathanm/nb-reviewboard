package org.smartdevelop.reviewboard.action;

import java.awt.Component;
import javax.swing.JTable;
import org.netbeans.swing.outline.DefaultOutlineCellRenderer;

/**
 *
 * @author Pragalathan M.
 */
public class OutlineCellRenderer extends DefaultOutlineCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c;
        c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        return c;
    }
}
