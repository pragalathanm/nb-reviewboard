package org.smartdevelop.reviewboard.diff;

import java.io.File;
import java.io.OutputStream;
import org.openide.nodes.Node;

/**
 * This class acts as the base for providing diff based operations. This class
 * is independent of any version control system.
 *
 * @author Pragalathan M
 */
public interface DiffManager {

    /**
     * Returns the modified files.
     *
     * @param nodes the selected {@link Node}s.
     * @return array of modified file among the given {@code nodes}.
     * @throws Exception
     */
    VcsFile[] getModifiedFiles(Node[] nodes) throws Exception;

    /**
     * Returns whether any VCS client available.
     *
     * @return true if any VCS client is available, false otherwise.
     */
    boolean isClientAvailable();

    /**
     * Checked whether the given {@code nodes} have any modified files.
     *
     * @param nodes the selected {@link Node}s.
     * @return true if any file represented by the {@code nodes} is modified.
     */
    boolean hasAnyModifiedFile(Node[] nodes);

    /**
     * Generates and writes the diff to the given stream.
     *
     * @param outputStream the stream to write the diffs into.
     * @param files the files whose diffs need to be generated.
     * @param runningName the text to display in the status-progress bar of the
     * IDE.
     */
    void writeDiff(OutputStream outputStream, File[] files, String runningName);

    /**
     * Checks whether the associated action can be enabled or not. The
     * corresponding action is enabled only when at least one file is modified.
     *
     * @param nodes the nodes wrapping the files to check
     * @return true if any file is modified.
     */
    boolean enableAction(Node[] nodes);

    /**
     * Gets the caches context files associated with the {@code nodes}.
     *
     * @param nodes the nodes wrapping the files.
     * @return the array of cached {@code File}s
     */
    File[] getCachedContextFiles(Node[] nodes);

    /**
     * Determines whether the current VCS DiffManger manages all the files in
     * the context.
     *
     * @param nodes the nodes representing the current context
     * @return true, if the DiffManger manages the context, false otherwise
     */
    boolean owns(Node[] nodes);
}
