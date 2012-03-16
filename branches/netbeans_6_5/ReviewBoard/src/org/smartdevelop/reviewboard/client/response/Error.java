/*
 * Created on: Mar 6, 2012
 */
package org.smartdevelop.reviewboard.client.response;

/**
 *
 * @author Pragalathan M
 */
public class Error {

    private String msg;
    private int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Error{" + "msg=" + msg + ", code=" + code + '}';
    }
}
