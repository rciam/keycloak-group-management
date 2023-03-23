package org.keycloak.plugins.groups.representations;

import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;

public class GroupRepresentation extends org.keycloak.representations.idm.GroupRepresentation {

    private List<GroupRepresentation> subGroups;
    private List<String> groupRoles;
    private List<GroupEnrollmentConfigurationRepresentation> enrollmentConfigurationList;
    private List<UserRepresentation> admins;


    public List<GroupRepresentation> getExtraSubGroups() {
        return this.subGroups;
    }

    public void setExtraSubGroups(List<GroupRepresentation> subGroups) {
        this.subGroups = subGroups;
    }

    public List<String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<String> groupRoles) {
        this.groupRoles = groupRoles;
    }

    public List<GroupEnrollmentConfigurationRepresentation> getEnrollmentConfigurationList() {
        return enrollmentConfigurationList;
    }

    public void setEnrollmentConfigurationList(List<GroupEnrollmentConfigurationRepresentation> enrollmentConfigurationList) {
        this.enrollmentConfigurationList = enrollmentConfigurationList;
    }

    public List<UserRepresentation> getAdmins() {
        return admins;
    }

    public void setAdmins(List<UserRepresentation> admins) {
        this.admins = admins;
    }
}
