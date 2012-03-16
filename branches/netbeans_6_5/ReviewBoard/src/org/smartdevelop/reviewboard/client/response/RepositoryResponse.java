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
public class RepositoryResponse extends ReviewBoardResponse {

    private int totalResults;
    private List<Repository> repositories;
    private Map<String, Link> links;

    public Map<String, Link> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
    }

    public int getTotalResults() {
        return totalResults;
    }

    @JsonProperty("total_results")
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
}
