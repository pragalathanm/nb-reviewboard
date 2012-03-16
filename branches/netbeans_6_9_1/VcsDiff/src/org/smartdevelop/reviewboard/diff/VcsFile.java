/*
 * Created on: Mar 5, 2012
 */
package org.smartdevelop.reviewboard.diff;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JPanel;

/**
 * A wrapper for version-ed file. Along with the file, it also stores the VCS
 * repository the file belongs to.
 *
 * @author Pragalathan M
 */
public abstract class VcsFile {

    private static final Pattern p = Pattern.compile("[(http)|(https)|(svn+ssh)]://([a-zA-Z]*@)?(.*)");
    protected File file;
    protected String repository;
    protected String status;// New/Modified
    protected boolean selected = true;
    protected String annotatedName;

    public VcsFile() {
    }

    /**
     * Creates a version-ed file instance.
     *
     * @param file the actual file that exists in the local file system.
     * @param repository the VCS repository the file belongs to.
     */
    public VcsFile(File file, String repository) {
        this.file = file;
        Matcher matcher = p.matcher(repository);
        if (matcher.find()) {
            String group = matcher.group(1);
            if (group != null) {
                repository = repository.replace(group, "");
            }
        }
        this.repository = repository;
    }

    public String getAnnotatedName() {
        return annotatedName;
    }

    public void setAnnotatedName(String annotatedName) {
        this.annotatedName = annotatedName;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the actual {@link File} instance.
     *
     * @return the actual {@link File} instance.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the VCS repository this version-ed file belongs to.
     *
     * @return the VCS repository this version-ed file belongs to.
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Returns the name of the file.
     *
     * @return the name of the file.
     */
    public String getName() {
        return file.getName();
    }

    /**
     * Returns the parent {@code File}.
     *
     * @return the parent {@code File}.
     */
    public File getParentFile() {
        return file.getParentFile();
    }

    /**
     * Checks whether the file represented by this object is a directory or not.
     *
     * @return true if this object represents a directory, false otherwise.
     */
    public boolean isDirectory() {
        return file.isDirectory();
    }

    public String getLocation() {
        return file.getAbsolutePath();
    }

    /**
     * Facilitates the version diff manager to provide diff panel.
     *
     * @return the diff panel.
     */
    public abstract JPanel createDiffPanel();
}
