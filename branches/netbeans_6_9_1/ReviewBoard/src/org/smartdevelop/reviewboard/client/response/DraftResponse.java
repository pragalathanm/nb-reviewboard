/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

/**
 *
 * @author Pragalathan M
 */
public class DraftResponse extends ReviewBoardResponse {

    private ReviewRequest draft;

    public ReviewRequest getDraft() {
        return draft;
    }

    public void setDraft(ReviewRequest draft) {
        this.draft = draft;
    }
}
