/**
 * Created on: Mar 11, 2012
 */
package org.smartdevelop.reviewboard.gitdiff;

import java.io.File;
import javax.swing.JPanel;
import org.netbeans.modules.git.ui.diff.MultiDiffPanelController;
import org.smartdevelop.reviewboard.diff.VcsFile;

/**
 * This class wraps {@link File} to provide SVN related additional attributes.
 *
 * @author Pragalathan M
 */
public class GitFile extends VcsFile {

    public GitFile() {
    }

    public GitFile(File file, String repository) {
        super(file, repository);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public JPanel createDiffPanel() {
        return new MultiDiffPanelController(file).getPanel();
    }
}
