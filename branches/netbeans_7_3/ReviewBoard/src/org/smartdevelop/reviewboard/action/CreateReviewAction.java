package org.smartdevelop.reviewboard.action;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.MissingResourceException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.versioning.util.TableSorter;
import org.netbeans.modules.versioning.util.Utils;
import org.netbeans.spi.diff.DiffProvider;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataShadow;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.NodeAction;
import org.openide.windows.TopComponent;
import org.smartdevelop.reviewboard.diff.DiffManager;
import org.smartdevelop.reviewboard.diff.DiffManagerFactory;
import org.smartdevelop.reviewboard.diff.VcsFile;

/**
 * Action class for creating ReviewBoard request.
 *
 * @author Pragalathan M
 */
@ActionID(category = "Team",
id = "org.smartdevelop.reviewboard.CreateReviewAction")
@ActionRegistration(iconBase = "org/smartdevelop/reviewboard/images/logo.png",
displayName = "#CTL_CreateReviewAction")
@ActionReferences({
    @ActionReference(path = "Menu/Versioning/ReviewBoard", position = -10, separatorAfter = 40),
    @ActionReference(path = "Toolbars/File", position = 500)
})
@Messages({"CTL_CreateReviewAction_Context=New Review Request...",
    "CTL_CreateReviewAction=New Review Request...",
    "CTL_CreateReviewActionRunning_Context=creating review request",
    "CTL_CreateReviewActionRunning_Context_Multiple=creating review request",
    "CTL_CreateReviewAction_Context_Multiple=Create Review Request"})
public final class CreateReviewAction extends NodeAction {

    private DiffManager diffManager;

    public CreateReviewAction() {
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }

    @Override
    protected String iconResource() {
        return "org/smartdevelop/reviewboard/images/logo.png";
    }

    /**
     * Enables or disables the action
     *
     * @param nodes the file nodes based on which this action has to be enabled
     * or disabled.
     * @return true, if the action can be enabled, false otherwise.
     */
    @Override
    public boolean enable(Node[] nodes) {
        if (Lookup.getDefault().lookup(DiffProvider.class) == null) {
            return false;
        }
        diffManager = DiffManagerFactory.getInsance().getDiffManager(nodes);
        return diffManager != null && diffManager.enableAction(nodes);
    }

    /**
     * Creates new review request(s).
     *
     * @param nodes the file nodes for which the new review request needs to be
     * created.
     */
    protected void performContextAction(Node[] nodes) {
        // reevaluate fast enablement logic guess

        if (!diffManager.isClientAvailable()) {
            return;
        }

        boolean noop = !diffManager.hasAnyModifiedFile(nodes);
        if (noop) {
            NotifyDescriptor msg = new NotifyDescriptor.Message(NbBundle.getMessage(CreateReviewAction.class, "BK3001"), NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            return;
        }
        try {
            VcsFile[] files = diffManager.getModifiedFiles(nodes);
            FileStatusPanel panel = new FileStatusPanel(getRunningName(nodes), diffManager);
            Map<String, Integer> sortingStatus = Collections.singletonMap(ReviewRequestTableModel.COLUMN_NAME_PATH, TableSorter.ASCENDING);
            ReviewRequestTable data = new ReviewRequestTable(panel.filesLabel, ReviewRequestTable.COMMIT_COLUMNS, sortingStatus);
            data.setNodes(files);
            panel.setCommitTable(data);
            data.setCommitPanel(panel);
            panel.showCreateRequestDialog();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    protected boolean asynchronous() {
        return false;
    }

    /**
     * Returns the base name for the action.
     *
     * @param activatedNodes the nodes based on which the name of the action
     * needs to be determined.
     * @return the base name for the action
     */
    protected String getBaseName(Node[] activatedNodes) {
        return "CTL_CreateReviewAction";
    }

    //------- svn context class methods copied here
    /**
     * Synchronizes memory modifications with disk and calls
     * {@link  #performContextAction}.
     */
    @Override
    protected void performAction(final Node[] nodes) {
        // TODO try to save files in invocation context only
        // list somehow modified file in the context and save
        // just them.
        // The same (global save) logic is in CVS, no complaint
        LifecycleManager.getDefault().saveAll();
        Utils.logVCSActionEvent("SVN");
        performContextAction(nodes);
    }

    /**
     * Be sure nobody overwrites
     */
    @Override
    public final boolean isEnabled() {
        return super.isEnabled();
    }

    /**
     * Be sure nobody overwrites
     */
    @Override
    public final void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    /**
     * Running action display name, it seeks action class bundle for: <ul> <li><code>getBaseName() + "Running"</code>
     * key <li><code>getBaseName() + "Running_Context"</code> key for one
     * selected file <li><code>getBaseName() + "Running_Context_Multiple"</code>
     * key for multiple selected files <li><code>getBaseName() + "Running_Project"</code>
     * key for one selected project <li><code>getBaseName() + "Running_Projects"</code>
     * key for multiple selected projects </ul>
     */
    public String getRunningName(Node[] activatedNodes) {
        return getName("Running", activatedNodes); // NOI18N
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public String getName() {
        return getName("", TopComponent.getRegistry().getActivatedNodes()); // NOI18N
    }

    /**
     * Display name, it seeks action class bundle for: <ul> <li><code>getBaseName()</code>
     * key <li><code>getBaseName() + "_Context"</code> key for one selected file <li><code>getBaseName() + "_Context_Multiple"</code>
     * key for multiple selected files <li><code>getBaseName() + "_Project"</code>
     * key for one selected project <li><code>getBaseName() + "_Projects"</code>
     * key for multiple selected projects </ul>
     */
    public String getName(String role, Node[] activatedNodes) {
        String baseName = getBaseName(activatedNodes) + role;
        if (!isEnabled()) {
            return NbBundle.getMessage(this.getClass(), baseName);
        }

        File[] nodes = diffManager.getCachedContextFiles(activatedNodes);
        int objectCount = nodes.length;
        // if all nodes represent project node the use plain name
        // It avoids "Show changes 2 files" on project node
        // caused by fact that project contains two source groups.

        boolean projectsOnly = true;
        for (int i = 0; i < activatedNodes.length; i++) {
            Node activatedNode = activatedNodes[i];
            Project project = activatedNode.getLookup().lookup(Project.class);
            if (project == null) {
                projectsOnly = false;
                break;
            }
        }
        if (projectsOnly) {
            objectCount = activatedNodes.length;
        }

        if (objectCount == 0) {
            return NbBundle.getMessage(this.getClass(), baseName);
        } else if (objectCount == 1) {
            if (projectsOnly) {
                String dispName = ProjectUtils.getInformation(activatedNodes[0].getLookup().lookup(Project.class)).getDisplayName();
                return NbBundle.getMessage(this.getClass(), baseName + "_Context", // NOI18N
                        dispName);
            }
            String name;
            FileObject fo = activatedNodes[0].getLookup().lookup(FileObject.class);
            if (fo != null) {
                name = fo.getNameExt();
            } else {
                DataObject dao = activatedNodes[0].getLookup().lookup(DataObject.class);
                if (dao instanceof DataShadow) {
                    dao = ((DataShadow) dao).getOriginal();
                }
                if (dao != null) {
                    name = dao.getPrimaryFile().getNameExt();
                } else {
                    name = activatedNodes[0].getDisplayName();
                }
            }
            return MessageFormat.format(NbBundle.getMessage(this.getClass(), baseName + "_Context"), // NOI18N
                    new Object[]{name});
        } else {
            if (projectsOnly) {
                try {
                    return MessageFormat.format(NbBundle.getMessage(this.getClass(), baseName + "_Projects"), // NOI18N
                            new Object[]{new Integer(objectCount)});
                } catch (MissingResourceException ex) {
                    // ignore use files alternative bellow
                }
            }
            return MessageFormat.format(NbBundle.getMessage(this.getClass(), baseName + "_Context_Multiple"), // NOI18N
                    new Object[]{new Integer(objectCount)});
        }
    }

    /**
     * Computes display name of the context this action will operate.
     *
     * @return String name of this action's context, e.g. "3 files",
     * "MyProject", "2 projects", "Foo.java". Returns null if the context is
     * empty
     */
    public String getContextDisplayName(Node[] activatedNodes) {
        // TODO: reuse this code in getName() 
        File[] nodes = diffManager.getCachedContextFiles(activatedNodes);
        int objectCount = nodes.length;
        // if all nodes represent project node the use plain name
        // It avoids "Show changes 2 files" on project node
        // caused by fact that project contains two source groups.

        boolean projectsOnly = true;
        for (int i = 0; i < activatedNodes.length; i++) {
            Node activatedNode = activatedNodes[i];
            Project project = activatedNode.getLookup().lookup(Project.class);
            if (project == null) {
                projectsOnly = false;
                break;
            }
        }
        if (projectsOnly) {
            objectCount = activatedNodes.length;
        }

        if (objectCount == 0) {
            return null;
        } else if (objectCount == 1) {
            if (projectsOnly) {
                return ProjectUtils.getInformation(activatedNodes[0].getLookup().lookup(Project.class)).getDisplayName();
            }
            FileObject fo = activatedNodes[0].getLookup().lookup(FileObject.class);
            if (fo != null) {
                return fo.getNameExt();
            } else {
                DataObject dao = activatedNodes[0].getLookup().lookup(DataObject.class);
                if (dao instanceof DataShadow) {
                    dao = ((DataShadow) dao).getOriginal();
                }
                if (dao != null) {
                    return dao.getPrimaryFile().getNameExt();
                } else {
                    return activatedNodes[0].getDisplayName();
                }
            }
        } else {
            if (projectsOnly) {
                try {
                    return MessageFormat.format(NbBundle.getMessage(CreateReviewAction.class, "MSG_ActionContext_MultipleProjects"), // NOI18N
                            new Object[]{new Integer(objectCount)});
                } catch (MissingResourceException ex) {
                    // ignore use files alternative bellow
                }
            }
            return MessageFormat.format(NbBundle.getMessage(CreateReviewAction.class, "MSG_ActionContext_MultipleFiles"), // NOI18N
                    new Object[]{new Integer(objectCount)});
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(this.getClass());
    }
}
