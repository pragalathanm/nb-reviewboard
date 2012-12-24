/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

import java.util.List;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 * @author Pragalathan M
 */
public class ReviewBoardResponse {

    private Map<String, List<String>> failedFields;
    private Error err;
    private String stat;

    public ReviewBoardResponse() {
    }

    public Error getErr() {
        return err;
    }

    public void setErr(Error err) {
        this.err = err;
    }

    public Map<String, List<String>> getFailedFields() {
        return failedFields;
    }

    @JsonProperty("fields")
    public void setFailedFields(Map<String, List<String>> failedFields) {
        this.failedFields = failedFields;
    }

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public boolean isSuccessful() {
        return "ok".equals(stat);
    }
}
