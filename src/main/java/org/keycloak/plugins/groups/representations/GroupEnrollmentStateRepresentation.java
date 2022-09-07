package org.keycloak.plugins.groups.representations;

import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.persistence.Column;
import java.util.Collection;
import java.util.Date;

public class GroupEnrollmentStateRepresentation {

    protected String id;
    protected String enrollmentId;
    protected String state;
    protected Date timestamp;
    protected String justification;

    public GroupEnrollmentStateRepresentation() {
    }

    public GroupEnrollmentStateRepresentation(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}
