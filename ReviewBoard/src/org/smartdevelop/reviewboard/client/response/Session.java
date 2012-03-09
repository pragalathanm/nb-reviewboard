/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

import java.util.Map;

/**
 *
 * @author Pragalathan M
 */
public class Session {

    private boolean authenticated;
    private Map<String, Link> links;

    public Map<String, Link> getLinks() {
        return links;
    }

    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
