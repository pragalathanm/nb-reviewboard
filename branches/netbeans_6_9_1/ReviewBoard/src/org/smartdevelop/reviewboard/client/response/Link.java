/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

/**
 *
 * @author Pragalathan M
 */
public class Link {

    String href;
    String method;
    String title;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
