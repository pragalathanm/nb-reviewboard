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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

import java.io.File;
import java.util.*;
import javax.swing.table.AbstractTableModel;
import org.openide.util.NbBundle;
import org.smartdevelop.reviewboard.diff.VcsFile;

/**
 * Table model for the review request dialog table. Copied and modified from {@link org.netbeans.modules.subversion.ui.commit.ReviewRequestTableModel}.
 *
 */
public class ReviewRequestTableModel extends AbstractTableModel {

    public static final String COLUMN_NAME_COMMIT = "commit"; // NOI18N
    public static final String COLUMN_NAME_NAME = "name"; // NOI18N
    public static final String COLUMN_NAME_STATUS = "status"; // NOI18N
    public static final String COLUMN_NAME_PATH = "path"; // NOI18N

    private class RootFile {

        String repositoryPath;
        String rootLocalPath;
    }
    private RootFile rootFile;
    /**
     * Defines labels for Versioning view table columns.
     */
    private static final Map<String, String[]> columnLabels = new HashMap<String, String[]>(4);

    {
        ResourceBundle loc = NbBundle.getBundle(ReviewRequestTableModel.class);
        columnLabels.put(COLUMN_NAME_COMMIT, new String[]{
                    loc.getString("CTL_ReviewRequestTable_Column_First"), // NOI18N
                    loc.getString("CTL_ReviewRequestTable_Column_Description")}); // NOI18N
        columnLabels.put(COLUMN_NAME_NAME, new String[]{
                    loc.getString("CTL_ReviewRequestTable_Column_File"),
                    loc.getString("CTL_ReviewRequestTable_Column_File")});
        columnLabels.put(COLUMN_NAME_STATUS, new String[]{
                    loc.getString("CTL_ReviewRequestTable_Column_Status"),
                    loc.getString("CTL_ReviewRequestTable_Column_Status")});
        columnLabels.put(COLUMN_NAME_PATH, new String[]{
                    loc.getString("CTL_ReviewRequestTable_Column_Folder"),
                    loc.getString("CTL_ReviewRequestTable_Column_Folder")});
    }
    private VcsFile[] nodes;
    private Index index;
    private String[] columns;

    /**
     * Create stable with name, status, action and path columns and empty nodes {@link #setNodes model}.
     */
    public ReviewRequestTableModel(String[] columns) {
        setColumns(columns);
        setNodes(new VcsFile[0]);
    }

    void setNodes(VcsFile[] nodes) {
        this.nodes = nodes;
        this.index = new Index();
        fireTableDataChanged();
    }

    void setColumns(String[] cols) {
        if (Arrays.equals(cols, columns)) {
            return;
        }
        columns = cols;
        fireTableStructureChanged();
    }

    /**
     * @return Map&lt;SvnFileNode, CommitOptions>
     */
    public List<VcsFile> getCommitFiles() {
        List<VcsFile> result = new ArrayList<VcsFile>();
        for (VcsFile vcsFile : nodes) {
            if (vcsFile.isSelected()) {
                result.add(vcsFile);
            }
        }
        return result;
    }

    @Override
    public String getColumnName(int column) {
        return columnLabels.get(columns[column])[0];
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public int getRowCount() {
        return nodes.length;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        String col = columns[columnIndex];
        if (col.equals(COLUMN_NAME_COMMIT)) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        String col = columns[columnIndex];
        return col.equals(COLUMN_NAME_COMMIT);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        VcsFile node;
        String col = columns[columnIndex];
        if (col.equals(COLUMN_NAME_COMMIT)) {
            return nodes[rowIndex].isSelected();
        } else if (col.equals(COLUMN_NAME_NAME)) {
            return nodes[rowIndex].getName();
        } else if (col.equals(COLUMN_NAME_STATUS)) {
            node = nodes[rowIndex];
            return node.getStatus();
        } else if (col.equals(COLUMN_NAME_PATH)) {
            String shortPath;
            // XXX this is a mess
            if (rootFile != null) {
                // must convert from native separators to slashes
                String relativePath = nodes[rowIndex].getFile().getAbsolutePath().substring(rootFile.rootLocalPath.length());
                shortPath = rootFile.repositoryPath + relativePath.replace(File.separatorChar, '/');
            } else {
                shortPath = nodes[rowIndex].getLocation();
                if (shortPath == null) {
                    shortPath = org.openide.util.NbBundle.getMessage(ReviewRequestTableModel.class, "CTL_CommitForm_NotInRepository"); // NOI18N
                }
            }
            return shortPath;
        }
        throw new IllegalArgumentException("Column index out of range: " + columnIndex); // NOI18N
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        String col = columns[columnIndex];
        if (col.equals(COLUMN_NAME_COMMIT)) {
            nodes[rowIndex].setSelected((Boolean) aValue);
        }
        fireTableRowsUpdated(0, getRowCount() - 1);
    }

    public VcsFile getNode(int row) {
        return nodes[row];
    }

    void setRootFile(String repositoryPath, String rootLocalPath) {
        rootFile = new RootFile();
        rootFile.repositoryPath = repositoryPath;
        rootFile.rootLocalPath = rootLocalPath;
    }

    void setIncluded(int[] rows, boolean include, boolean recursively) {
        for (int rowIndex : rows) {
            nodes[rowIndex].setSelected(include);
        }
        fireTableRowsUpdated(0, getRowCount() - 1);
    }

    private class Index {

        private HashMap<File, Value> fileToIndex;

        public Index() {
            constructIndex();
        }

        private void constructIndex() {
            fileToIndex = new HashMap<File, Value>(nodes.length);
            for (int i = 0; i < nodes.length; ++i) {
                Value value = new Value(i);
                fileToIndex.put(nodes[i].getFile(), value);
            }
            for (int i = 0; i < nodes.length; ++i) {
                File parentFile = nodes[i].getFile().getParentFile();
                if (parentFile != null) {
                    Value value = fileToIndex.get(parentFile);
                    if (value != null) {
                        value.addChild(i);
                    }
                }
            }
        }

        private Integer getParent(int nodeIndex) {
            File parentFile = nodes[nodeIndex].getFile().getParentFile();
            Value parentValue = parentFile == null ? null : fileToIndex.get(parentFile);
            return parentValue == null ? null : parentValue.nodeIndex;
        }

        private Integer[] getChildren(int nodeIndex) {
            Value value = fileToIndex.get(nodes[nodeIndex].getFile());
            return value == null || value.childrenIndexes == null ? null : value.childrenIndexes.toArray(new Integer[value.childrenIndexes.size()]);
        }

        private class Value {

            private Integer nodeIndex;
            private Set<Integer> childrenIndexes;

            private Value(int index) {
                this.nodeIndex = index;
            }

            private void addChild(int childIndex) {
                if (childrenIndexes == null) {
                    childrenIndexes = new HashSet<Integer>();
                }
                childrenIndexes.add(childIndex);
            }
        }
    }
}
