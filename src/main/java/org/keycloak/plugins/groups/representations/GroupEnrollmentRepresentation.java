package org.keycloak.plugins.groups.representations;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collection;

public class GroupEnrollmentRepresentation {

    protected String id;
    protected UserRepresentation user;
    protected GroupRepresentation group;
    protected Collection<GroupEnrollmentStateRepresentation> enrollmentStates;

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

    public GroupRepresentation getGroup() {
        return group;
    }

    public void setGroup(GroupRepresentation group) {
        this.group = group;
    }

    public Collection<GroupEnrollmentStateRepresentation> getEnrollmentStates() {
        return enrollmentStates;
    }

    public void setEnrollmentStates(Collection<GroupEnrollmentStateRepresentation> enrollmentStates) {
        this.enrollmentStates = enrollmentStates;
    }
}
