package org.smartdevelop.reviewboard.action;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.jdesktop.beansbinding.*;
import org.netbeans.modules.versioning.util.AutoResizingPanel;
import org.netbeans.modules.versioning.util.DialogBoundsPreserver;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.TabbedPaneFactory;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.smartdevelop.reviewboard.client.ReviewBoardClient;
import org.smartdevelop.reviewboard.client.response.Repository;
import org.smartdevelop.reviewboard.client.response.ReviewRequest;
import org.smartdevelop.reviewboard.diff.DiffManager;
import org.smartdevelop.reviewboard.diff.VcsFile;

/**
 *
 * @author Pragalathan M
 */
public class FileStatusPanel extends AutoResizingPanel implements PropertyChangeListener {

    private ReviewRequestTable commitTable;
    private JTabbedPane tabbedPane;
    private HashMap<File, JPanel> displayedDiffs = new HashMap<File, JPanel>();
    private java.util.List<BindingGroup> bindingGroups = new ArrayList<BindingGroup>();
    private Future<?> runningTask;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private String runningName;
    private DiffManager diffManager;

    /**
     * Creates new form FileStatusPanel
     */
    public FileStatusPanel(String runningName, DiffManager diffManager) {
        initComponents();
        filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(filesLabel);
        filesPanel.add(panel);
        this.runningName = runningName;
        this.diffManager = diffManager;
    }

    void setCommitTable(ReviewRequestTable commitTable) {
        if (this.commitTable != null) {
            filesPanel.remove(this.commitTable.getComponent());
        }
        this.commitTable = commitTable;
        filesPanel.add(commitTable.getComponent());
        filesPanel.revalidate();
    }

    void openDiff(VcsFile[] nodes) {
        for (VcsFile node : nodes) {
            if (tabbedPane == null) {
                initializeTabs();
            }
            File file = node.getFile();
            JPanel panel = displayedDiffs.get(file);
            if (panel == null) {
                panel = node.createDiffPanel();
                displayedDiffs.put(file, panel);
            }
            if (tabbedPane.indexOfComponent(panel) == -1) {
                tabbedPane.addTab(file.getName(), panel);
            }
            tabbedPane.setSelectedComponent(panel);
            tabbedPane.requestFocusInWindow();
//            panel.requestActive();
        }
        revalidate();
        repaint();
    }

    private void initializeTabs() {
        tabbedPane = TabbedPaneFactory.createCloseButtonTabbedPane();
        tabbedPane.addPropertyChangeListener(this);
        tabbedPane.addTab(NbBundle.getMessage(FileStatusPanel.class, "CTL_ReviewRequest_Tab_Files"), containerPanel); //NOI18N
        tabbedPane.setPreferredSize(containerPanel.getPreferredSize());
        add(tabbedPane);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (TabbedPaneFactory.PROP_CLOSE.equals(evt.getPropertyName())) {
            JComponent comp = (JComponent) evt.getNewValue();
            removeTab(comp);
        }
    }

    private void removeTab(JComponent comp) {
        if (containerPanel != comp && tabbedPane != null && tabbedPane.getTabCount() > 1) {
            tabbedPane.remove(comp);
            revalidate();
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (tabbedPane.getTabCount() <= 1) {
                    remove(tabbedPane);
                    tabbedPane.removePropertyChangeListener(FileStatusPanel.this);
                    tabbedPane = null;
                    add(containerPanel);
                    revalidate();
                }
            }
        });
    }

    /**
     * Binds all the {@code comps} using beans binding. This enables the user to
     * type only once and to replicate the details across all the reviews
     * without manually copying and pasting.
     *
     * @param comps the {@JTextComponent}s to bind.
     * @return the {@code BindingGroup}
     */
    private BindingGroup bind(java.util.List<JTextComponent> comps) {
        BindingGroup bindingGroup = new BindingGroup();
        if (comps.size() > 1) {
            for (int i = 0; i < comps.size() - 1; i++) {
                JTextComponent comp1 = comps.get(i);
                JTextComponent comp2 = comps.get(i + 1);
                Binding binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, comp2, ELProperty.create("${text}"), comp1, BeanProperty.create("text"), "test" + i);
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

        backButton = new JButton();
        nextButton = new JButton();
        cancelButton = new JButton();
        finishButton = new JButton();
        containerPanel = new AutoResizingPanel();
        filesPanel = new AutoResizingPanel();
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

        filesLabel.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.filesLabel.text")); 
        backButton.setMnemonic('B');
        backButton.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.backButton.text"));         backButton.setEnabled(false);
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                back(evt);
            }
        });

        nextButton.setMnemonic('N');
        nextButton.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.nextButton.text"));         nextButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                next(evt);
            }
        });

        cancelButton.setMnemonic('C');
        cancelButton.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.cancelButton.text"));         cancelButton.setDefaultCapable(false);

        finishButton.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.finishButton.text")); 
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        containerPanel.setLayout(new CardLayout());
        containerPanel.add(filesPanel, "card2");

        jPanel1.setLayout(new GridBagLayout());

        replicateCheckBox.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.replicateCheckBox.text"));         replicateCheckBox.addActionListener(new ActionListener() {
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

        publishCheckBox.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.publishCheckBox.text"));         publishCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                publishCheckBoxenablePublishOption(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(3, 3, 3, 3);
        jPanel1.add(publishCheckBox, gridBagConstraints);

        containerPanel.add(jPanel1, "card4");

        diffLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         diffLabel.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.diffLabel.text")); 
        reviewRequestLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         reviewRequestLabel.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.reviewRequestLabel.text")); 
        uploadingLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         uploadingLabel.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.uploadingLabel.text")); 
        paramsLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         paramsLabel.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.paramsLabel.text")); 
        publishLabel.setIcon(new ImageIcon(getClass().getResource("/org/smartdevelop/reviewboard/images/bullet-yellow.png")));         publishLabel.setText(NbBundle.getMessage(FileStatusPanel.class, "FileStatusPanel.publishLabel.text")); 
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
                        .addGap(0, 138, Short.MAX_VALUE)))
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
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE))
        );

        containerPanel.add(jPanel2, "card4");

        add(containerPanel);
    }// </editor-fold>//GEN-END:initComponents

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

    private void publishCheckBoxenablePublishOption(ActionEvent evt) {//GEN-FIRST:event_publishCheckBoxenablePublishOption
        publishLabel.setVisible(publishCheckBox.isSelected());
    }//GEN-LAST:event_publishCheckBoxenablePublishOption

    private void back(ActionEvent evt) {//GEN-FIRST:event_back
        CardLayout layout = (CardLayout) containerPanel.getLayout();
        layout.first(containerPanel);
        backButton.setEnabled(false);
        bindingGroups.clear();
        tab.removeAll();
        nextButton.setEnabled(true);
    }//GEN-LAST:event_back

    private void next(ActionEvent evt) {//GEN-FIRST:event_next
        try {
            ReviewBoardClient client = ReviewBoardClient.INSTANCE;
            Map<String, List<VcsFile>> filesMap = new HashMap<String, List<VcsFile>>();

            for (VcsFile file : commitTable.getCommitFiles()) {
                List<VcsFile> files = filesMap.get(file.getRepository());
                if (files == null) {
                    files = new ArrayList<VcsFile>();
                    filesMap.put(file.getRepository(), files);
                }
                files.add(file);
            }
            List<JTextComponent> summaries = new ArrayList<JTextComponent>();
            List<JTextComponent> groups = new ArrayList<JTextComponent>();
            List<JTextComponent> reviewers = new ArrayList<JTextComponent>();
            List<JTextComponent> descriptions = new ArrayList<JTextComponent>();
            Map<String, Repository> repositoryMap = client.getRepositories();
            for (String repositoryPath : filesMap.keySet()) {
                ReviewRequestPanel panel = new ReviewRequestPanel(filesMap.get(repositoryPath));
                summaries.add(panel.getSummaryComponent());
                groups.add(panel.getGroupComponent());
                reviewers.add(panel.getReviewerComponent());
                descriptions.add(panel.getDescriptionComponent());
                Repository repository = repositoryMap.get(repositoryPath);
                if (repository == null) {
                    DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("No repository found for path: " + repositoryPath + "\nPlease check the settings in Tools->Options->ReviewBoard", NotifyDescriptor.ERROR_MESSAGE));
                    return;
                }
                tab.add(repository.getName(), panel);
            }
            bindingGroups.add(bind(summaries));
            bindingGroups.add(bind(groups));
            bindingGroups.add(bind(reviewers));
            bindingGroups.add(bind(descriptions));
            CardLayout layout = (CardLayout) containerPanel.getLayout();
            layout.next(containerPanel);
            backButton.setEnabled(true);
            nextButton.setEnabled(false);
            finishButton.setEnabled(true);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_next

    public Object showCreateRequestDialog() {
        JPanel panel = this;
        finishButton.setEnabled(false);
        finishButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                nextButton.setVisible(false);
                backButton.setVisible(false);
                finishButton.setVisible(false);
                CardLayout layout = (CardLayout) containerPanel.getLayout();
                layout.last(containerPanel);
                int tabsCount = tab.getTabCount();
                ReviewRequestPanel[] panels = new ReviewRequestPanel[tabsCount];
                for (int i = 0; i < tabsCount; i++) {
                    panels[i] = (ReviewRequestPanel) tab.getComponentAt(i);
                }
                createReviewRequests(panels);
            }
        });
        String dialogTitle = NbBundle.getMessage(FileStatusPanel.class, "CTL_ReviewRequestDialog_Title"); // NOI18N
        final DialogDescriptor dd = new DialogDescriptor(panel, dialogTitle, 
                true,
                new Object[]{backButton, nextButton, finishButton, cancelButton},
                null,
                DialogDescriptor.DEFAULT_ALIGN,
                null,
//                new HelpCtx(FileStatusPanel.class),
                null);
        ActionListener al;

        dd.setButtonListener(al = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dd.setClosingOptions(new Object[]{finishButton, cancelButton});
                if (finishButton == e.getSource()) {
                    JButton closeButton = (JButton)dd.getClosingOptions()[1];
                    closeButton.setText("Close");
                    dd.setClosingOptions(new Object[]{closeButton});
                }
            }
        });

//        panel.putClientProperty("contentTitle", dialogTitle);  // NOI18N
//        panel.putClientProperty("DialogDescriptor", dd); // NOI18N
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dd);

        dialog.addWindowListener(new DialogBoundsPreserver(Preferences.userNodeForPackage(FileStatusPanel.class), "reviewboard.dialog")); // NOI18N
        dialog.pack();

        dialog.setVisible(true);
        if (dd.getValue() == DialogDescriptor.CLOSED_OPTION) {
            al.actionPerformed(new ActionEvent(cancelButton, ActionEvent.ACTION_PERFORMED, null));
        }

        return dd.getValue();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton backButton;
    private JButton cancelButton;
    private JPanel containerPanel;
    private JLabel diffLabel;
    public final JLabel filesLabel = new JLabel();
    private JPanel filesPanel;
    private JButton finishButton;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JScrollPane jScrollPane1;
    private JButton nextButton;
    private JLabel paramsLabel;
    private JCheckBox publishCheckBox;
    private JLabel publishLabel;
    private JCheckBox replicateCheckBox;
    private JLabel reviewRequestLabel;
    private JTextPane reviewUrlsTextPane;
    private JTabbedPane tab;
    private JLabel uploadingLabel;
    // End of variables declaration//GEN-END:variables
}
