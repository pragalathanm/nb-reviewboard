package org.smartdevelop.reviewboard.action;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.*;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.*;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;
import org.smartdevelop.reviewboard.client.ReviewBoardClient;
import org.smartdevelop.reviewboard.client.response.Repository;
import org.smartdevelop.reviewboard.client.response.ReviewRequest;
import org.smartdevelop.reviewboard.diff.DiffManager;
import org.smartdevelop.reviewboard.diff.VcsFile;

/**
 * A dialog that displays the selected -modified files. The user then can select
 * what file to submit for review.
 *
 * @author Pragalathan M
 */
public class FileStatusDialog extends JDialog implements ExplorerManager.Provider {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ExplorerManager em;
    private AbstractNode root;
    private boolean cancelled = true;
    private Future<?> runningTask;
    private List<BindingGroup> bindingGroups = new ArrayList<BindingGroup>();
    private String runningName;

    /**
     * Creates new form FileStatusDialog
     */
    public FileStatusDialog(final VcsFile[] files, String runningName) {
        super(WindowManager.getDefault().getMainWindow(), true);
        em = new ExplorerManager();
        initComponents();
        publishLabel.setVisible(false);
        root = new AbstractNode(Children.create(new FileStatusNodeChildFactory(files), true));
        em.setRootContext(root);
        outlineView1.setPropertyColumns(
                "select", "...",
                "path", "Location");
        outlineView1.getOutline().setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        outlineView1.getOutline().setRootVisible(false);
        outlineView1.getOutline().getColumnModel().getColumn(1).setPreferredWidth(10);
        outlineView1.getOutline().getColumnModel().getColumn(1).setWidth(10);
        setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        this.runningName = runningName;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public ExplorerManager getExplorerManager() {
        return em;
    }

    /**
     * Returns the user selected files.
     *
     * @return the selected files.
     */
    public File[] getSelectedFiles() {
        if (cancelled) {
            return new File[0];
        }
        List<VcsFile> files = new ArrayList<VcsFile>();
        for (Node node : root.getChildren().getNodes()) {
            FileStatusNode fileNode = (FileStatusNode) node;
            if (fileNode.isSelected()) {
                files.add(fileNode.getDelegate());
            }
        }
        return files.toArray(new File[files.size()]);
    }

    /**
     * Binds all the {@code comps} using beans binding. This enables the user to
     * type only once and to replicate the details across all the reviews
     * without manually copying and pasting.
     *
     * @param comps the {@JTextComponent}s to bind.
     * @return the {@code BindingGroup}
     */
    private BindingGroup bind(List<JTextComponent> comps) {
        BindingGroup bindingGroup = new BindingGroup();
        if (comps.size() > 1) {
            for (int i = 0; i < comps.size() - 1; i++) {
                JTextComponent comp1 = comps.get(i);
                JTextComponent comp2 = comps.get(i + 1);
                Binding binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, comp2, ELProperty.create("${text}"), comp1, BeanProperty.create("text"), "test" + i);
                bindingGroup.addBinding(binding);
            }
//            bindingGroup.bind();
        }
        return bindingGroup;
    }

    /**
     * Creates review board requests.
     *
     * @param panels the tab panels containing the information to create review
     * requests.
     */
    private void createReviewRequests(final ReviewRequestPanel[] panels) {
        runningTask = executor.submit(new SwingWorker<Object, JLabel>() {

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    ReviewBoardClient client = ReviewBoardClient.INSTANCE;
                    DiffManager diffManager = Lookup.getDefault().lookup(DiffManager.class);
                    for (ReviewRequestPanel panel : panels) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        List<VcsFile> vcsFiles = panel.getFiles();
                        File[] files = new File[vcsFiles.size()];
                        for (int i = 0; i < files.length; i++) {
                            files[i] = vcsFiles.get(i).getFile();
                        }
                        diffManager.writeDiff(stream, files, runningName);
                        stream.close();
                        panel.setDiff(stream.toString("utf8"));
                    }
                    publish(diffLabel);

                    Document doc = reviewUrlsTextPane.getDocument();
                    for (ReviewRequestPanel panel : panels) {
                        VcsFile file = panel.getFiles().get(0);
                        ReviewRequest request = client.createRequest(file.getRepository());
                        if (request == null) {
                            return null;
                        }

                        doc.insertString(doc.getLength(), request.getLinks().get("self").getHref() + "\n", null);
                        panel.setReviewRequest(request);
                    }

                    publish(reviewRequestLabel);
                    boolean success = true;
                    for (ReviewRequestPanel panel : panels) {
                        success &= client.uploadDiff(panel.getReviewRequest(), panel.getDiff());
                    }
                    if (success) {
                        publish(uploadingLabel);
                    }

                    for (ReviewRequestPanel panel : panels) {
                        Map<String, String> fields = new HashMap<String, String>();
                        fields.put("summary", panel.getSummary());
                        fields.put("description", panel.getDescription());
                        fields.put("target_people", panel.getReviewer());
                        fields.put("target_groups", panel.getGroup());
                        if (publishCheckBox.isSelected()) {
                            fields.put("public", "1");
                        }
                        success &= client.updateField(panel.getReviewRequest(), fields);
                    }
                    if (success) {
                        publish(paramsLabel, publishLabel);
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                return null;
            }

            @Override
            protected void done() {
                cancelButton.setText("Close");
            }

            @Override
            protected void process(List<JLabel> chunks) {
                for (JLabel label : chunks) {
                    label.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/green-tick.png")));
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        reviewPanel = new JPanel();
        outlineView1 = new OutlineView("Files");
        jPanel1 = new JPanel();
        replicateCheckBox = new JCheckBox();
        tab = new JTabbedPane();
        publishCheckBox = new JCheckBox();
        jPanel2 = new JPanel();
        diffLabel = new JLabel();
        reviewRequestLabel = new JLabel();
        uploadingLabel = new JLabel();
        paramsLabel = new JLabel();
        publishLabel = new JLabel();
        jScrollPane1 = new JScrollPane();
        reviewUrlsTextPane = new JTextPane();
        cancelButton = new JButton();
        nextButton = new JButton();
        backButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        reviewPanel.setLayout(new CardLayout());
        reviewPanel.add(outlineView1, "card2");

        jPanel1.setLayout(new GridBagLayout());

        replicateCheckBox.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.replicateCheckBox.text"));         replicateCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                replicateCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        jPanel1.add(replicateCheckBox, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new Insets(3, 0, 3, 0);
        jPanel1.add(tab, gridBagConstraints);

        publishCheckBox.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.publishCheckBox.text"));         publishCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                enablePublishOption(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        jPanel1.add(publishCheckBox, gridBagConstraints);

        reviewPanel.add(jPanel1, "card4");

        diffLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         diffLabel.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.diffLabel.text")); 
        reviewRequestLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         reviewRequestLabel.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.reviewRequestLabel.text")); 
        uploadingLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         uploadingLabel.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.uploadingLabel.text")); 
        paramsLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         paramsLabel.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.paramsLabel.text")); 
        publishLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         publishLabel.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.publishLabel.text")); 
        reviewUrlsTextPane.setOpaque(false);
        jScrollPane1.setViewportView(reviewUrlsTextPane);

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(Alignment.LEADING)
                            .addComponent(diffLabel)
                            .addComponent(reviewRequestLabel)
                            .addComponent(uploadingLabel)
                            .addComponent(paramsLabel)
                            .addComponent(publishLabel))
                        .addGap(0, 521, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(diffLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(reviewRequestLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(uploadingLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(paramsLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(publishLabel)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))
        );

        reviewPanel.add(jPanel2, "card4");

        cancelButton.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.cancelButton.text"));         cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancel(evt);
            }
        });

        nextButton.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.nextButton.text"));         nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                next(evt);
            }
        });

        backButton.setText(NbBundle.getMessage(FileStatusDialog.class, "FileStatusDialog.backButton.text"));         backButton.setEnabled(false);
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                back(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(backButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(nextButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(reviewPanel, GroupLayout.DEFAULT_SIZE, 783, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(reviewPanel, GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(nextButton)
                    .addComponent(backButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancel(ActionEvent evt) {//GEN-FIRST:event_cancel
        if (cancelButton.getText().equals("Cancel")) {
            if (runningTask != null && !runningTask.isDone()) {
                runningTask.cancel(true);
            }
        }
        setVisible(false);
        dispose();
    }//GEN-LAST:event_cancel

    private void next(ActionEvent evt) {//GEN-FIRST:event_next
        try {
            ReviewBoardClient client = ReviewBoardClient.INSTANCE;
            if (nextButton.getText().equals("Finish")) {
                nextButton.setVisible(false);
                backButton.setVisible(false);
                CardLayout layout = (CardLayout) reviewPanel.getLayout();
                layout.last(reviewPanel);
                int tabsCount = tab.getTabCount();
                ReviewRequestPanel[] panels = new ReviewRequestPanel[tabsCount];
                for (int i = 0; i < tabsCount; i++) {
                    panels[i] = (ReviewRequestPanel) tab.getComponentAt(i);
                }
                createReviewRequests(panels);
            } else {
                Map<String, Repository> repositoryMap = client.getRepositories();
                Map<String, List<VcsFile>> filesMap = new HashMap<String, List<VcsFile>>();
                for (Node node : root.getChildren().getNodes()) {
                    FileStatusNode fileNode = (FileStatusNode) node;
                    if (fileNode.isSelected()) {
                        VcsFile file = fileNode.getDelegate();

                        List<VcsFile> files = filesMap.get(file.getRepository());
                        if (files == null) {
                            files = new ArrayList<VcsFile>();
                            filesMap.put(file.getRepository(), files);
                        }

                        files.add(fileNode.getDelegate());
                    }
                }
                List<JTextComponent> summaries = new ArrayList<JTextComponent>();
                List<JTextComponent> groups = new ArrayList<JTextComponent>();
                List<JTextComponent> reviewers = new ArrayList<JTextComponent>();
                List<JTextComponent> descriptions = new ArrayList<JTextComponent>();
                for (String repository : filesMap.keySet()) {
                    ReviewRequestPanel panel = new ReviewRequestPanel(filesMap.get(repository));
                    summaries.add(panel.getSummaryComponent());
                    groups.add(panel.getGroupComponent());
                    reviewers.add(panel.getReviewerComponent());
                    descriptions.add(panel.getDescriptionComponent());
                    tab.add(repositoryMap.get(repository).getName(), panel);
                }
                bindingGroups.add(bind(summaries));
                bindingGroups.add(bind(groups));
                bindingGroups.add(bind(reviewers));
                bindingGroups.add(bind(descriptions));
                CardLayout layout = (CardLayout) reviewPanel.getLayout();
                layout.next(reviewPanel);
                backButton.setEnabled(true);
                nextButton.setText("Finish");
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_next

    private void replicateCheckBoxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_replicateCheckBoxActionPerformed
        if (replicateCheckBox.isSelected()) {
            for (BindingGroup bindingGroup : bindingGroups) {
                bindingGroup.bind();
            }
        } else {
            for (BindingGroup bindingGroup : bindingGroups) {
                bindingGroup.unbind();
            }
        }
    }//GEN-LAST:event_replicateCheckBoxActionPerformed

    private void back(ActionEvent evt) {//GEN-FIRST:event_back
        CardLayout layout = (CardLayout) reviewPanel.getLayout();
        layout.first(reviewPanel);
        backButton.setEnabled(false);
        nextButton.setText("Next");
        bindingGroups.clear();
        tab.removeAll();
    }//GEN-LAST:event_back

    private void enablePublishOption(ActionEvent evt) {//GEN-FIRST:event_enablePublishOption
        publishLabel.setVisible(publishCheckBox.isSelected());
    }//GEN-LAST:event_enablePublishOption

    /**
     * Node to represent modified file.
     */
    class FileStatusNode extends AbstractNode {

        private VcsFile delegate;
        private boolean selected = true;

        public VcsFile getDelegate() {
            return delegate;
        }

        public boolean isSelected() {
            return selected;
        }

        public FileStatusNode(VcsFile node) {
            super(Children.LEAF);
            this.delegate = node;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        protected Sheet createSheet() {
            Sheet s = super.createSheet();
            Sheet.Set ss = s.get(Sheet.PROPERTIES);
            if (ss == null) {
                ss = Sheet.createPropertiesSet();
                s.put(ss);
            }

            ss.put(new PropertySupport<Boolean>("select", Boolean.TYPE, "...", null, true, true) {

                @Override
                public Boolean getValue() throws IllegalAccessException, InvocationTargetException {
                    return selected;
                }

                @Override
                public void setValue(Boolean val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                    selected = val;
                }
            });
            ss.put(new PropertySupport.ReadOnly<String>("path", String.class, "Location", null) {

                @Override
                public String getValue() throws IllegalAccessException, InvocationTargetException {
                    return delegate.getParentFile().getAbsolutePath();
                }
            });

            return s;
        }
    }

    /**
     * Node Child factory for displaying modified files.
     */
    class FileStatusNodeChildFactory extends ChildFactory<VcsFile> {

        private VcsFile[] delegates;

        public FileStatusNodeChildFactory(VcsFile[] delegates) {
            this.delegates = delegates;
        }

        @Override
        protected Node createNodeForKey(VcsFile key) {
            return new FileStatusNode(key);
        }

        @Override
        protected boolean createKeys(List<VcsFile> toPopulate) {
            for (VcsFile file : delegates) {
                if (!file.isDirectory()) {
                    toPopulate.add(file);
                }
            }
            return true;
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton backButton;
    private JButton cancelButton;
    private JLabel diffLabel;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JScrollPane jScrollPane1;
    private JButton nextButton;
    private OutlineView outlineView1;
    private JLabel paramsLabel;
    private JCheckBox publishCheckBox;
    private JLabel publishLabel;
    private JCheckBox replicateCheckBox;
    private JPanel reviewPanel;
    private JLabel reviewRequestLabel;
    private JTextPane reviewUrlsTextPane;
    private JTabbedPane tab;
    private JLabel uploadingLabel;
    // End of variables declaration//GEN-END:variables
}
