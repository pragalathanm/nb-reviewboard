/*
 * Created on: Feb 27, 2012
 */
package org.smartdevelop.reviewboard.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 * Action Suite for hosting reviewboard operations.
 *
 * @author Pragalathan M
 */
@ActionID(id = "org.smartdevelop.reviewboard.ReviewBoardActionSuite", category = "Projects")
@ActionRegistration(iconBase = "org/smartdevelop/reviewboard/images/logo.png", displayName = "#CTL_ReviewBoardActionSuite")
@ActionReferences({
    @ActionReference(path = "Projects/Actions", position = -100, separatorAfter = 40, separatorBefore = -101)
})
@Messages("CTL_ReviewBoardActionSuite=Review Board")
public class ReviewBoardActionSuite extends AbstractAction implements ContextAwareAction {

    ReviewBoardBaseAction contextAction;

    public ReviewBoardActionSuite() {
        super("Review");
        contextAction = new ReviewBoardBaseAction();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        contextAction.setContext(context);
        return contextAction;
    }
}
