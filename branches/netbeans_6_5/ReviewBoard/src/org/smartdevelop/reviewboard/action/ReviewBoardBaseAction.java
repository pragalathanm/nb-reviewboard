/**
 * Created on: Feb 27, 2012
 */
package org.smartdevelop.reviewboard.action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.actions.SystemAction;

/**
 * Menu for reviewboard actions.
 *
 * @author Pragalathan M
 */
public class ReviewBoardBaseAction extends AbstractAction implements Presenter.Popup {

    private PopupPresenter popupPresenter;

    public ReviewBoardBaseAction() {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        popupPresenter = new PopupPresenter();
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return enabled ? popupPresenter : null;
    }

    public void setContext(Lookup context) {
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        assert false;
    }

    /**
     * MenuItem to display the {@code CreateReviewRequest} action.
     */
    private class PopupPresenter extends JMenuItem implements DynamicMenuContent {

        private JMenu brazilMenu;
        private JComponent[] menuItems;

        public PopupPresenter() {
            brazilMenu = new JMenu("Review Board");
            menuItems = new JComponent[]{brazilMenu};
            brazilMenu.add(new JMenuItem(SystemAction.get(CreateReviewAction.class)));
        }

        @Override
        public boolean isEnabled() {
            return ReviewBoardBaseAction.this.isEnabled();
        }

        @Override
        public JComponent[] getMenuPresenters() {
            // Multiple top level entries can be created this way
            return menuItems;
        }

        @Override
        public JComponent[] synchMenuPresenters(JComponent[] items) {
            return items;
        }
    }
}
