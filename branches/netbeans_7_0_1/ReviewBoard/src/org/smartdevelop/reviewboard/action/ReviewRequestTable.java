/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.smartdevelop.reviewboard.action;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.netbeans.modules.versioning.util.FilePathCellRenderer;
import org.netbeans.modules.versioning.util.SortedTable;
import org.netbeans.modules.versioning.util.TableSorter;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.smartdevelop.reviewboard.diff.VcsFile;

/**
 * Copied and modified from {@link org.netbeans.modules.subversion.ui.commit.CommitTable}.
 * Displays nodes in the new review request dialog.
 */
public class ReviewRequestTable implements AncestorListener, TableModelListener, MouseListener {

    public static String[] COMMIT_COLUMNS = new String[]{
        ReviewRequestTableModel.COLUMN_NAME_COMMIT,
        ReviewRequestTableModel.COLUMN_NAME_NAME,
        ReviewRequestTableModel.COLUMN_NAME_STATUS,
        ReviewRequestTableModel.COLUMN_NAME_PATH
    };
    public static String[] IMPORT_COLUMNS = new String[]{
        ReviewRequestTableModel.COLUMN_NAME_COMMIT,
        ReviewRequestTableModel.COLUMN_NAME_NAME,
        ReviewRequestTableModel.COLUMN_NAME_PATH
    };
    private ReviewRequestTableModel tableModel;
    private JTable table;
    private JComponent component;
    private TableSorter sorter;
    private String[] columns;
    private Map<String, Integer> sortByColumns;
    private FileStatusPanel commitPanel;
    private Set<File> modifiedFiles = Collections.<File>emptySet();

    public ReviewRequestTable(JLabel label, String[] columns, Map<String, Integer> sortByColumns) {
        init(label, columns, null);
        this.sortByColumns = sortByColumns;
        setSortingStatus();
    }

    public ReviewRequestTable(JLabel label, String[] columns, TableSorter sorter) {
        init(label, columns, sorter);
    }

    private void init(JLabel label, String[] columns, TableSorter sorter) {
        tableModel = new ReviewRequestTableModel(columns);
        tableModel.addTableModelListener(this);
        if (sorter == null) {
            sorter = new TableSorter(tableModel);
        }
        this.sorter = sorter;
        table = new SortedTable(this.sorter);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(String.class, new CommitStringsCellRenderer());
        table.setDefaultRenderer(Boolean.class, new CheckboxCellRenderer());
        table.setDefaultEditor(Boolean.class, new CheckboxCellEditor());
        table.getTableHeader().setReorderingAllowed(true);
        table.setRowHeight(table.getRowHeight() * 6 / 5);
        table.addAncestorListener(this);
        component = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        label.setLabelFor(table);
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F10, KeyEvent.SHIFT_DOWN_MASK), "org.openide.actions.PopupAction"); // NOI18N
        table.getActionMap().put("org.openide.actions.PopupAction", new AbstractAction() { // NOI18N

            @Override
            public void actionPerformed(ActionEvent e) {
                showPopup(org.netbeans.modules.versioning.util.Utils.getPositionForPopup(table));
            }
        });
        table.addMouseListener(this);
        setColumns(columns);
    }

    @Override
    public void ancestorAdded(AncestorEvent event) {
        setDefaultColumnSizes();
    }

    /**
     * Sets sizes of Commit table columns, kind of hard coded.
     */
    private void setDefaultColumnSizes() {
        int width = table.getWidth();
        TableColumnModel columnModel = table.getColumnModel();
        if (columns == null || columnModel == null) {
            return; // unsure when this methed will be called (component realization) 
        }
        if (columnModel.getColumnCount() != columns.length) {
            return;
        }
        if (columns.length == 4) {
            for (int i = 0; i < columns.length; i++) {
                String col = columns[i];
                sorter.setColumnComparator(i, null);
                if (col.equals(ReviewRequestTableModel.COLUMN_NAME_COMMIT)) {
                    columnModel.getColumn(i).setMinWidth(new JCheckBox().getMinimumSize().width);
                    columnModel.getColumn(i).setPreferredWidth(new JCheckBox().getPreferredSize().width);
                } else if (col.equals(ReviewRequestTableModel.COLUMN_NAME_NAME)) {
                    sorter.setColumnComparator(i, new FileNameComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 30 / 100);
                } else {
                    columnModel.getColumn(i).setPreferredWidth(width * 30 / 100);
                }
            }
        } else if (columns.length == 5) {
            for (int i = 0; i < columns.length; i++) {
                String col = columns[i];
                sorter.setColumnComparator(i, null);
                if (col.equals(ReviewRequestTableModel.COLUMN_NAME_COMMIT)) {
                    columnModel.getColumn(i).setMinWidth(new JCheckBox().getMinimumSize().width);
                    columnModel.getColumn(i).setPreferredWidth(new JCheckBox().getPreferredSize().width);
                } else if (col.equals(ReviewRequestTableModel.COLUMN_NAME_NAME)) {
                    sorter.setColumnComparator(i, new FileNameComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 30 / 100);
                } else if (col.equals(ReviewRequestTableModel.COLUMN_NAME_STATUS)) {
                    sorter.setColumnComparator(i, new StatusComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else {
                    columnModel.getColumn(i).setPreferredWidth(width * 40 / 100);
                }
            }
        }
    }

    private void setSortingStatus() {
        for (Map.Entry<String, Integer> e : sortByColumns.entrySet()) {
            String sortByColumn = e.getKey();
            for (int j = 0; j < columns.length; j++) {
                String column = columns[j];
                if (column.equals(sortByColumn)) {
                    sorter.setSortingStatus(j, e.getValue());
                    break;
                }
            }
        }
    }

    public LinkedHashMap<String, Integer> getSortingState() {
        Map<Integer, Integer> sorterState = sorter.getSortingState();
        LinkedHashMap<String, Integer> sortingStatus = new LinkedHashMap<String, Integer>(sorterState.size());
        for (Map.Entry<Integer, Integer> e : sorterState.entrySet()) {
            sortingStatus.put(columns[e.getKey()], e.getValue());
        }
        return sortingStatus;
    }

    @Override
    public void ancestorMoved(AncestorEvent event) {
    }

    @Override
    public void ancestorRemoved(AncestorEvent event) {
    }

    void setColumns(String[] cols) {
        if (Arrays.equals(columns, cols)) {
            return;
        }
        columns = cols;
        tableModel.setColumns(cols);
        setDefaultColumnSizes();
    }

    /**
     * Note that each node should have it's values initialized
     *
     * @param nodes
     */
    public void setNodes(VcsFile[] nodes) {
        tableModel.setNodes(nodes);
    }

    /**
     * @return Map&lt;VcsFile, CommitOptions>
     */
    public List<VcsFile> getCommitFiles() {
        return tableModel.getCommitFiles();
    }

    /**
     * @return table in a scrollpane
     */
    public JComponent getComponent() {
        return component;
    }

    void dataChanged() {
        int idx = table.getSelectedRow();
        tableModel.fireTableDataChanged();
        if (idx != -1) {
            table.getSelectionModel().addSelectionInterval(idx, idx);
        }
    }

    public TableModel getTableModel() {
        return tableModel;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        // change in commit options may alter name rendering (strikethrough)
        table.repaint();
    }

    public void setRootFile(String repositoryPath, String rootLocalPath) {
        tableModel.setRootFile(repositoryPath, rootLocalPath);
    }

    private void showPopup(final MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());
        if (row != -1) {
            boolean makeRowSelected = true;
            int[] selectedrows = table.getSelectedRows();
            for (int i = 0; i < selectedrows.length; i++) {
                if (row == selectedrows[i]) {
                    makeRowSelected = false;
                    break;
                }
            }
            if (makeRowSelected) {
                table.getSelectionModel().setSelectionInterval(row, row);
            }
        }
        if (col != -1) {
            boolean makeColSelected = true;
            int[] selectedcols = table.getSelectedColumns();
            for (int i = 0; i < selectedcols.length; i++) {
                if (col == selectedcols[i]) {
                    makeColSelected = false;
                    break;
                }
            }
            if (makeColSelected) {
                table.getColumnModel().getSelectionModel().setSelectionInterval(col, col);
            }
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // invoke later so the selection on the table will be set first
                if (table.isShowing()) {
                    JPopupMenu menu = getPopup();
                    menu.show(table, e.getX(), e.getY());
                }
            }
        });
    }

    private void showPopup(Point p) {
        JPopupMenu menu = getPopup();
        menu.show(table, p.x, p.y);
    }

    private JPopupMenu getPopup() {

        JPopupMenu menu = new JPopupMenu();
        JMenuItem item;
        boolean onlyIncluded = true;
        for (int rowIndex : table.getSelectedRows()) {
            int row = sorter.modelIndex(rowIndex);
            VcsFile node = tableModel.getNode(row);

            if (!node.isSelected()) {
                onlyIncluded = false;
            }
        }
        final boolean include = !onlyIncluded;
        item = menu.add(new PopupAction(NbBundle.getMessage(ReviewRequestTable.class, include ? "CTL_ReviewRequestTable_IncludeAction" : "CTL_ReviewRequestTable_ExcludeAction")) { // NOI18N

            @Override
            public void performAction(ActionEvent e) {
                int[] rows = getRows();
                tableModel.setIncluded(rows, include, false);
            }
        });
        Mnemonics.setLocalizedText(item, item.getText());
        item = menu.add(new AbstractAction(NbBundle.getMessage(ReviewRequestTable.class, "CTL_ReviewRequestTable_DiffAction")) { // NOI18N

            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = table.getSelectedRows();
                VcsFile[] nodes = new VcsFile[rows.length];
                for (int i = 0; i < rows.length; ++i) {
                    nodes[i] = tableModel.getNode(sorter.modelIndex(rows[i]));
                }
                commitPanel.openDiff(nodes);
            }
        });
        Mnemonics.setLocalizedText(item, item.getText());
        item.setEnabled(commitPanel != null);
        return menu;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // not interested
    }

    /**
     * This action keeps selection of rows in the table
     */
    private abstract class PopupAction extends AbstractAction {

        private int[] rows;

        public PopupAction(String name) {
            super(name);
        }

        @Override
        public final void actionPerformed(ActionEvent e) {
            rows = table.getSelectedRows();
            int rowCount = table.getRowCount();
            for (int i = 0; i < rows.length; ++i) {
                rows[i] = sorter.modelIndex(rows[i]);
            }
            performAction(e);
            if (rowCount == table.getRowCount()) {
                for (int i = 0; i < rows.length; ++i) {
                    table.getSelectionModel().addSelectionInterval(sorter.viewIndex(rows[i]), sorter.viewIndex(rows[i]));
                }
            }
        }

        protected int[] getRows() {
            return rows;
        }

        protected abstract void performAction(ActionEvent e);
    }

    void setCommitPanel(FileStatusPanel panel) {
        this.commitPanel = panel;
    }

    void setModifiedFiles(Set<File> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    private class CommitStringsCellRenderer extends DefaultTableCellRenderer {

        private FilePathCellRenderer pathRenderer = new FilePathCellRenderer();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int col = table.convertColumnIndexToModel(column);
            if (ReviewRequestTableModel.COLUMN_NAME_NAME.equals(columns[col])) {
                TableSorter sorter = (TableSorter) table.getModel();
                ReviewRequestTableModel model = (ReviewRequestTableModel) sorter.getTableModel();
                VcsFile node = model.getNode(sorter.modelIndex(row));
                if (!isSelected) {
                    value = node.getAnnotatedName();
                }
                if (!node.isSelected()) {
                    value = "<s>" + value + "</s>"; // NOI18N
                }
                if (modifiedFiles.contains(node.getFile())) {
                    value = "<strong>" + value + "</strong>"; //NOI18N
                }
                value = "<html>" + value + "</html>"; //NOI18N
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (ReviewRequestTableModel.COLUMN_NAME_PATH.equals(columns[col])) {
                return pathRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else {
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }
    }

    private class CheckboxCellRenderer extends JCheckBox implements TableCellRenderer {

        public CheckboxCellRenderer() {
            setToolTipText(NbBundle.getMessage(ReviewRequestTable.class, "CTL_ReviewRequestTable_Column_Description")); //NOI18N
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setSelected(value == null ? false : (Boolean) value);
            setBackground(hasFocus || isSelected ? table.getSelectionBackground() : table.getBackground());
            setHorizontalAlignment(SwingConstants.LEFT);
            return this;
        }
    }

    private class CheckboxCellEditor extends DefaultCellEditor {

        public CheckboxCellEditor() {
            super(new JCheckBox());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            JCheckBox checkbox = (JCheckBox) editorComponent;
            checkbox.setSelected(value == null ? false : (Boolean) value);
            checkbox.setHorizontalAlignment(SwingConstants.LEFT);
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    private class StatusComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            Integer row1 = (Integer) o1;
            Integer row2 = (Integer) o2;
            return tableModel.getNode(row1.intValue()).getStatus().compareTo(tableModel.getNode(row2.intValue()).getStatus());
        }
    }

    private class FileNameComparator implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            Integer row1 = (Integer) o1;
            Integer row2 = (Integer) o2;
            return tableModel.getNode(row1.intValue()).getName().compareToIgnoreCase(
                    tableModel.getNode(row2.intValue()).getName());
        }
    }
}
