/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

/**
 *
 * @author Pragalathan M
 */
public class LoginResponse extends ReviewBoardResponse {

    private Session session;

    public LoginResponse() {
        super();
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
