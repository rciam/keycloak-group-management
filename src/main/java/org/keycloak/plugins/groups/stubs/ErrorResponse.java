package org.keycloak.plugins.groups.stubs;

import java.io.Serializable;

public class ErrorResponse implements Serializable {

    private String reason;

    public ErrorResponse(){}

    public ErrorResponse(String reason){
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
