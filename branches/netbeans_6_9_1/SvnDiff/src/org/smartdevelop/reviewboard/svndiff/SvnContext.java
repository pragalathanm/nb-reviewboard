/*
 * Created on: Mar 5, 2012
 */
package org.smartdevelop.reviewboard.svndiff;

import org.netbeans.modules.subversion.FileInformation;
import org.netbeans.modules.subversion.Subversion;
import org.netbeans.modules.subversion.ui.actions.ContextAction;
import org.netbeans.modules.subversion.ui.diff.DiffSetupSource;
import org.netbeans.modules.subversion.ui.diff.ExportDiffAction;
import org.netbeans.modules.subversion.util.Context;
import org.netbeans.modules.subversion.util.SvnUtils;
import org.netbeans.spi.diff.DiffProvider;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

/**
 * Base class providing basic SVN related functionalities. Most of the code
 * refers to {@link ExportDiffAction}.
 *
 * @author Pragalathan M
 */
public class SvnContext extends ContextAction {

    private static final int enabledForStatus =
            FileInformation.STATUS_VERSIONED_MERGE
            | FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY
            | FileInformation.STATUS_VERSIONED_DELETEDLOCALLY
            | FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY
            | FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY
            | FileInformation.STATUS_VERSIONED_ADDEDLOCALLY;

    public boolean enableAction(Node[] nodes) {
        Context ctx = getCachedContext(nodes);
        if (!Subversion.getInstance().getStatusCache().containsFiles(ctx, enabledForStatus, true)) {
            return false;
        }
        TopComponent activated = TopComponent.getRegistry().getActivated();
        if (activated instanceof DiffSetupSource) {
            return true;
        }
        return super.enable(nodes) && Lookup.getDefault().lookup(DiffProvider.class) != null;
    }

    @Override
    protected Context getCachedContext(Node[] nodes) {
        return SvnUtils.getCurrentContext(nodes, getFileEnabledStatus(), getDirectoryEnabledStatus(), true);
    }

    @Override
    protected String getBaseName(Node[] activatedNodes) {
        return "CTL_CreateReviewAction";
    }

    @Override
    protected void performContextAction(Node[] nodes) {
    }
}
