package org.smartdevelop.reviewboard.diff;

import java.util.Collection;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 *
 * @author Pragalathan M
 */
public class DiffManagerFactory {

    public static final DiffManagerFactory factory = new DiffManagerFactory();
    private Collection<? extends DiffManager> diffManagers = Lookup.getDefault().lookupAll(DiffManager.class);

    public static DiffManagerFactory getInsance() {
        return factory;
    }

    public DiffManager getDiffManager(Node[] nodes) {
        if (nodes.length == 0) {
            return null;
        }
        for (DiffManager manager : diffManagers) {
            if (manager.owns(nodes)) {
                return manager;
            }
        }
        return null;
    }
}
