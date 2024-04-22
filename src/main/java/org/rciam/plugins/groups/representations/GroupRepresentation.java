package org.rciam.plugins.groups.representations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GroupRepresentation extends org.keycloak.representations.idm.GroupRepresentation {

    private List<GroupRepresentation> subGroups;
    private LinkedList<GroupRepresentation> parents;
    private Map<String,String> groupRoles;
    private List<GroupEnrollmentConfigurationRepresentation> enrollmentConfigurationList;
    private List<GroupAdminRepresentation> admins;


    public List<GroupRepresentation> getExtraSubGroups() {
        return this.subGroups;
    }

    public void setExtraSubGroups(List<GroupRepresentation> subGroups) {
        this.subGroups = subGroups;
    }

    public LinkedList<GroupRepresentation> getParents() {
        return parents;
    }

    public void setParents(LinkedList<GroupRepresentation> parents) {
        this.parents = parents;
    }

    public Map<String,String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(Map<String,String> groupRoles) {
        this.groupRoles = groupRoles;
    }

    public List<GroupEnrollmentConfigurationRepresentation> getEnrollmentConfigurationList() {
        return enrollmentConfigurationList;
    }

    public void setEnrollmentConfigurationList(List<GroupEnrollmentConfigurationRepresentation> enrollmentConfigurationList) {
        this.enrollmentConfigurationList = enrollmentConfigurationList;
    }

    public List<GroupAdminRepresentation> getAdmins() {
        return admins;
    }

    public void setAdmins(List<GroupAdminRepresentation> admins) {
        this.admins = admins;
    }
}
