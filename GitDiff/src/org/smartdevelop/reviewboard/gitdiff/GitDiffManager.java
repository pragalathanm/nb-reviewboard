/*
 * To change this template, choose Tools | Templates
 * an

 @Override
 protected void perform() {
 throw new UnsupportedOperationException("Not supported yet.");
 }
 } open the template in the editor.
 */
package org.smartdevelop.reviewboard.gitdiff;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.List;
import org.netbeans.libs.git.GitClient.DiffMode;
import org.netbeans.libs.git.GitRemoteConfig;
import org.netbeans.libs.git.progress.ProgressMonitor;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileInformation.Status;
import org.netbeans.modules.git.FileStatusCache;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitVCS;
import org.netbeans.modules.git.client.GitClient;
import org.netbeans.modules.git.client.GitClientExceptionHandler;
import org.netbeans.modules.git.client.GitProgressSupport;
import org.netbeans.modules.git.ui.diff.ExportUncommittedChangesAction;
import org.netbeans.modules.git.ui.output.OutputLogger;
import org.netbeans.modules.git.utils.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.smartdevelop.reviewboard.diff.DiffManager;
import org.smartdevelop.reviewboard.diff.VcsFile;

/**
 *
 * @author Pragalathan M
 */
@ServiceProvider(service = DiffManager.class)
public class GitDiffManager implements DiffManager {

    protected EnumSet<Status> enabledForStatus = FileInformation.STATUS_LOCAL_CHANGES;

    @Override
    public VcsFile[] getModifiedFiles(Node[] nodes) throws Exception {
        VCSContext context = GitUtils.getCurrentContext(nodes);
        FileStatusCache cache = Git.getInstance().getFileStatusCache();
        File[] files = cache.listFiles(context.getRootFiles(), enabledForStatus);
//        File[] files = GitUtils.listFiles(context.getFiles().toArray(new File[0]), enabledForStatus);
//        File[] files = GitUtils.flatten(context.getFiles().toArray(new File[0]), enabledForStatus);
        VcsFile[] modifiedFiles = new VcsFile[files.length];
        Git git = Git.getInstance();
        for (int i = 0; i < modifiedFiles.length; i++) {
            GitRemoteConfig remote = git.getClient(files[i]).getRemote("origin", new ProgressMonitor.DefaultProgressMonitor());
            List<String> uris = remote.getUris();
            VcsFile vcsFile = new GitFile(files[i], uris.get(0));
            FileInformation fileInformation = git.getFileStatusCache().getStatus(files[i]);
            vcsFile.setStatus(fileInformation.getStatusText());
            vcsFile.setAnnotatedName(git.getVCSAnnotator().annotateName(files[i].getName(), context));
            modifiedFiles[i] = vcsFile;
        }
        return modifiedFiles;
    }

    @Override
    public boolean isClientAvailable() {
        return true;
    }

    @Override
    public boolean hasAnyModifiedFile(Node[] nodes) {
        VCSContext context = GitUtils.getCurrentContext(nodes);
        return Git.getInstance().getFileStatusCache().containsFiles(context, enabledForStatus, false);
    }

    @Override
    public void writeDiff(final OutputStream outputStream, final File[] files, String runningName) {
        RequestProcessor rp = Git.getInstance().getRequestProcessor();
        GitProgressSupport ps = new GitProgressSupport() {
            @Override
            protected void perform() {
                OutputStream out;
                OutputLogger logger = getLogger();
                try {
                    GitClient client = getClient();
                    out = new BufferedOutputStream(outputStream);
                    setProgress(NbBundle.getMessage(ExportUncommittedChangesAction.class, "MSG_ExportUncommittedChangesAction.preparingDiff")); //NOI18N
                    client.exportDiff(files, DiffMode.HEAD_VS_WORKINGTREE, out, getProgressMonitor());
                } catch (Exception ex) {
                    logger.outputInRed(NbBundle.getMessage(ExportUncommittedChangesAction.class, "MSG_ExportUncommittedChangesAction.failed")); //NOI18N
                    GitClientExceptionHandler.notifyException(ex, true);
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.flush();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        };
        ps.start(rp, files[0], runningName).waitFinished();
    }

    @Override
    public boolean enableAction(Node[] nodes) {
        return hasAnyModifiedFile(nodes);
    }

    @Override
    public File[] getCachedContextFiles(Node[] nodes) {
        VCSContext context = GitUtils.getCurrentContext(nodes);
        return GitUtils.listFiles(context.getRootFiles().toArray(new File[0]), enabledForStatus);
    }

    @Override
    public boolean owns(Node[] nodes) {
        for (File file : VCSContext.forNodes(nodes).getRootFiles()) {
            if (!(VersioningSupport.getOwner(file) instanceof GitVCS)) {
                return false;
            }
        }
        return true;
    }
}
