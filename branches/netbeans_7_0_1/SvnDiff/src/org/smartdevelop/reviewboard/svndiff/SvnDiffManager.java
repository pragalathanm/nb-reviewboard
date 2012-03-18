/**
 * Created on: Feb 27, 2012
 */
package org.smartdevelop.reviewboard.svndiff;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import org.netbeans.api.diff.Difference;
import org.netbeans.modules.diff.builtin.visualizer.TextDiffVisualizer;
import org.netbeans.modules.proxy.Base64Encoder;
import org.netbeans.modules.subversion.FileInformation;
import org.netbeans.modules.subversion.Subversion;
import org.netbeans.modules.subversion.client.SvnClient;
import org.netbeans.modules.subversion.client.SvnClientExceptionHandler;
import org.netbeans.modules.subversion.client.SvnProgressSupport;
import org.netbeans.modules.subversion.ui.diff.DiffSetupSource;
import org.netbeans.modules.subversion.ui.diff.DiffStreamSource;
import org.netbeans.modules.subversion.ui.diff.ExportDiffAction;
import org.netbeans.modules.subversion.ui.diff.Setup;
import org.netbeans.modules.subversion.util.SvnUtils;
import org.netbeans.modules.versioning.util.Utils;
import org.netbeans.spi.diff.DiffProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.smartdevelop.reviewboard.diff.DiffManager;
import org.smartdevelop.reviewboard.diff.VcsFile;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * SVN diff manager to generate ReviewBoard compatible diff of source files.
 *
 * @author Pragalathan M
 */
@ServiceProvider(service = DiffManager.class)
public class SvnDiffManager extends SvnContext implements DiffManager {

    /**
     * {@inheritDoc}
     *
     * @param nodes {@inheritDoc}
     * @return {@inheritDoc}
     * @throws Exception
     */
    @Override
    public VcsFile[] getModifiedFiles(org.openide.nodes.Node[] nodes) throws Exception {
        File[] files = SvnUtils.getModifiedFiles(getContext(nodes), FileInformation.STATUS_LOCAL_CHANGE);
        VcsFile[] modifiedFiles = new VcsFile[files.length];
        for (int i = 0; i < modifiedFiles.length; i++) {
            VcsFile vcsFile = new SvnFile(files[i], SvnUtils.getRepositoryRootUrl(files[i]).toString());
            FileInformation fileInformation = Subversion.getInstance().getStatusCache().getStatus(files[i]);
            vcsFile.setStatus(fileInformation.getStatusText());
            vcsFile.setAnnotatedName(Subversion.getInstance().getAnnotator().annotateNameHtml(files[i].getName(), fileInformation, null));
            modifiedFiles[i] = vcsFile;
        }
        return modifiedFiles;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isClientAvailable() {
        return Subversion.getInstance().checkClientAvailable();
    }

    /**
     * {@inheritDoc}
     *
     * @param nodes {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean hasAnyModifiedFile(Node[] nodes) {
        TopComponent activated = TopComponent.getRegistry().getActivated();
        if (activated instanceof DiffSetupSource) {
            return ((DiffSetupSource) activated).getSetups().isEmpty();
        }
        return !Subversion.getInstance().getStatusCache().containsFiles(getContext(nodes), FileInformation.STATUS_LOCAL_CHANGE, true);
    }

    /**
     * {@inheritDoc}
     *
     * @param outputStream {@inheritDoc}
     * @param files {@inheritDoc}
     * @param runningName {@inheritDoc}
     */
    @Override
    public void writeDiff(final OutputStream outputStream, final File[] files, String runningName) {
        RequestProcessor rp = Subversion.getInstance().getRequestProcessor();
        SvnProgressSupport ps = new SvnProgressSupport() {

            @Override
            protected void perform() {
                try {
                    async(this, files, outputStream);
                } catch (SVNClientException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        };
        ps.start(rp, null, runningName).waitFinished();
    }

    private void async(SvnProgressSupport progress, File[] files, OutputStream destination) throws SVNClientException {
        // prepare setups and common parent - root
        File root;
        List<Setup> setups;

        TopComponent activated = TopComponent.getRegistry().getActivated();
        if (activated instanceof DiffSetupSource) {
            setups = new ArrayList<Setup>(((DiffSetupSource) activated).getSetups());

            List<File> setupFiles = new ArrayList<File>(setups.size());
            for (Iterator i = setups.iterator(); i.hasNext();) {
                Setup setup = (Setup) i.next();
                setupFiles.add(setup.getBaseFile());
            }
            root = getCommonParent(setupFiles.toArray(new File[setupFiles.size()]));
        } else {
            root = getCommonParent(files);
            setups = new ArrayList<Setup>(files.length);
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                Setup setup = new Setup(file, null, Setup.DIFFTYPE_LOCAL);
                setups.add(setup);
            }
        }
        exportDiff(setups, destination, root, progress);
    }

    /**
     * Exports the diff
     *
     * @param setups the {@code Setup} instances representing diff sources.
     * @param destination the destination stream to write the diff into.
     * @param root the local file system root corresponding to the repository
     * root
     * @param progress
     */
    public void exportDiff(List<Setup> setups, OutputStream destination, File root, SvnProgressSupport progress) {
        if (root == null) {
            NotifyDescriptor nd = new NotifyDescriptor(
                    NbBundle.getMessage(ExportDiffAction.class, "MSG_BadSelection_Prompt"),
                    NbBundle.getMessage(ExportDiffAction.class, "MSG_BadSelection_Title"),
                    NotifyDescriptor.DEFAULT_OPTION, NotifyDescriptor.ERROR_MESSAGE, null, null);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }
        boolean success = false;
        OutputStream out = null;
        int exportedFiles = 0;

        try {
            String sep = System.getProperty("line.separator"); // NOI18N
            out = new BufferedOutputStream(destination);
            Collections.sort(setups, new Comparator<Setup>() {

                @Override
                public int compare(Setup o1, Setup o2) {
                    return o1.getBaseFile().compareTo(o2.getBaseFile());
                }
            });
            Iterator<Setup> it = setups.iterator();
            int i = 0;
            while (it.hasNext()) {
                Setup setup = it.next();
                File file = setup.getBaseFile();
                if (file.isDirectory()) {
                    continue;
                }
                try {
                    progress.setRepositoryRoot(SvnUtils.getRepositoryRootUrl(file));
                } catch (Exception ex) {
                    SvnClientExceptionHandler.notifyException(ex, true, true);
                    return;
                }
                progress.setDisplayName(file.getName());

                String index = "Index: ";   // NOI18N
                String rootPath = root.getAbsolutePath();
                String filePath = file.getAbsolutePath();
                String relativePath = filePath;
                if (filePath.startsWith(rootPath)) {
                    relativePath = filePath.substring(rootPath.length() + 1).replace(File.separatorChar, '/');
                    index += relativePath + sep;
                    out.write(index.getBytes("utf8")); // NOI18N
                }
                out.write("===================================================================\n".getBytes("utf8")); // NOI18N
                exportDiff(setup, relativePath, out);
                i++;
            }

            exportedFiles = i;
            success = true;
        } catch (Exception ex) {
            Subversion.LOG.log(Level.INFO, NbBundle.getMessage(ExportDiffAction.class, "BK3003"), ex);
        } finally {
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException alreadyClsoed) {
                }
            }
            if (success) {
                StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(ExportDiffAction.class, "BK3004", new Integer(exportedFiles)));
            }

        }
    }

    /**
     * Writes contextual diff into given stream.
     */
    private void exportDiff(Setup setup, String relativePath, OutputStream out) throws IOException, SVNClientException {
        // setup.initSources();
        // hack to call init();
        DiffStreamSource firstSource = (DiffStreamSource) setup.getFirstSource();
        DiffStreamSource secondSource = (DiffStreamSource) setup.getSecondSource();
        if (firstSource != null) {
            firstSource.getMIMEType();
        }
        if (secondSource != null) {
            secondSource.getMIMEType();
        }
        // hack end

        DiffProvider diff = Lookup.getDefault().lookup(DiffProvider.class);

        Reader r1 = null;
        Reader r2 = null;
        Difference[] differences;

        try {
            r1 = setup.getFirstSource().createReader();
            if (r1 == null) {
                r1 = new StringReader("");  // NOI18N
            }
            r2 = setup.getSecondSource().createReader();
            if (r2 == null) {
                r2 = new StringReader("");  // NOI18N
            }
            differences = diff.computeDiff(r1, r2);
        } finally {
            if (r1 != null) {
                try {
                    r1.close();
                } catch (Exception e) {
                }
            }
            if (r2 != null) {
                try {
                    r2.close();
                } catch (Exception e) {
                }
            }
        }

        File file = setup.getBaseFile();
        try {
            InputStream is;
            if (!SvnUtils.getMimeType(file).startsWith("text/") && differences.length == 0) {
                // assume the file is binary 
                is = new ByteArrayInputStream(exportBinaryFile(file).getBytes("utf8"));  // NOI18N
            } else {
                r1 = setup.getFirstSource().createReader();
                if (r1 == null) {
                    r1 = new StringReader(""); // NOI18N
                }
                r2 = setup.getSecondSource().createReader();
                if (r2 == null) {
                    r2 = new StringReader(""); // NOI18N
                }
                String lastRevision = getLastRevision(file);
                String firstTitle = lastRevision.isEmpty() ? "(revision 0)" : "(revision " + lastRevision + ")";
                String secondTitle = lastRevision.isEmpty() ? "(revision 0)" : "(working copy)";
                // ensure more than one space (or a tab) here, for reviewboard to work.
                TextDiffVisualizer.TextDiffInfo info = new TextDiffVisualizer.TextDiffInfo(
                        relativePath + "     " + firstTitle, // NOI18N
                        relativePath + "     " + secondTitle, // NOI18N
                        null,
                        null,
                        r1,
                        r2,
                        differences);
                info.setContextMode(true, 3);
                String diffText = TextDiffVisualizer.differenceToUnifiedDiffText(info);
                is = new ByteArrayInputStream(diffText.getBytes("utf8"));  // NOI18N
            }
            while (true) {
                int i = is.read();
                if (i == -1) {
                    break;
                }
                out.write(i);
            }
        } finally {
            if (r1 != null) {
                try {
                    r1.close();
                } catch (Exception e) {
                }
            }
            if (r2 != null) {
                try {
                    r2.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private String getLastRevision(File file) throws SVNClientException {
        SvnClient client = Subversion.getInstance().getClient(false);
        ISVNInfo info = client.getInfoFromWorkingCopy(file);
        SVNRevision rev = info.getRevision();
        return rev != null && !"-1".equals(rev.toString()) ? rev.toString() : ""; //NOI18N
    }

    /**
     * Returns the local file path corresponding to the repository root. This
     * method assumes that it is the caller's responsibility to ensure that all
     * the files belong to the same repository.
     *
     * @param files
     * @return
     */
    private File getCommonParent(File[] files) throws SVNClientException {
        File root = files[0];
        String repositoryPath = SvnUtils.getRepositoryPath(root);
        String filePath = root.getAbsolutePath();
        if (filePath.endsWith(repositoryPath)) {
            File file = new File(filePath.substring(0, filePath.length() - repositoryPath.length()));
            return file;
        }

        return root;
    }

    private String exportBinaryFile(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder sb = new StringBuilder((int) file.length());
        if (file.canRead()) {
            Utils.copyStreamsCloseAll(baos, new FileInputStream(file));
        }
        sb.append("MIME: application/octet-stream; encoding: Base64; length: ").append(file.canRead() ? file.length() : -1); // NOI18N
        sb.append(System.getProperty("line.separator")); // NOI18N
        sb.append(Base64Encoder.encode(baos.toByteArray(), true));
        sb.append(System.getProperty("line.separator")); // NOI18N
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @param nodes {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public File[] getCachedContextFiles(Node[] nodes) {
        return super.getCachedContext(nodes).getFiles();
    }
}
