package org.keycloak.plugins.groups.representations;

import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.persistence.Column;
import java.util.Collection;

public class GroupEnrollmentStateRepresentation {

    protected String enrollmentId;
    protected String state;
    protected Long timestamp;
    protected String justification;


    public String getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(String enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}
