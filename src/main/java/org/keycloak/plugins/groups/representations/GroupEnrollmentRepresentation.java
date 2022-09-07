package org.keycloak.plugins.groups.representations;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collection;
import java.util.List;

public class GroupEnrollmentRepresentation {

    protected String id;
    protected UserRepresentation user;
    protected List<GroupRepresentation> groups;
    protected Collection<GroupEnrollmentStateRepresentation> enrollmentStates;

    public GroupEnrollmentRepresentation() {
    }

    public GroupEnrollmentRepresentation(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    public List<GroupRepresentation> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupRepresentation> groups) {
        this.groups = groups;
    }

    public Collection<GroupEnrollmentStateRepresentation> getEnrollmentStates() {
        return enrollmentStates;
    }

    public void setEnrollmentStates(Collection<GroupEnrollmentStateRepresentation> enrollmentStates) {
        this.enrollmentStates = enrollmentStates;
    }
}
