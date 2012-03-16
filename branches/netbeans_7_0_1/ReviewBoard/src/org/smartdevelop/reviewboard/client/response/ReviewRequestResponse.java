/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 * @author Pragalathan M
 */
public class ReviewRequestResponse extends ReviewBoardResponse {

    private ReviewRequest reviewRequest;

    public ReviewRequest getReviewRequest() {
        return reviewRequest;
    }

    @JsonProperty("review_request")
    public void setReviewRequest(ReviewRequest reviewRequest) {
        this.reviewRequest = reviewRequest;
    }
}
