/**
 * Created on: Mar 11, 2012
 */
package org.smartdevelop.reviewboard.svndiff;

import java.io.File;
import javax.swing.JPanel;
import org.netbeans.modules.subversion.ui.diff.MultiDiffPanel;
import org.netbeans.modules.subversion.ui.diff.Setup;
import org.smartdevelop.reviewboard.diff.VcsFile;

/**
 * This class wraps {@link File} to provide SVN related additional attributes.
 *
 * @author Pragalathan M
 */
public class SvnFile extends VcsFile {

    public SvnFile() {
    }

    public SvnFile(File file, String repository) {
        super(file, repository);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public JPanel createDiffPanel() {
        return new MultiDiffPanel(file, Setup.REVISION_BASE, Setup.REVISION_CURRENT, false); // switch the last parameter to true if editable diff works poorly
    }
}
