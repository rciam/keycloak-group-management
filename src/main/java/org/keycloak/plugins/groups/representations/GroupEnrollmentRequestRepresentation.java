package org.keycloak.plugins.groups.representations;

import org.keycloak.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public class GroupEnrollmentRequestRepresentation {

    private String id;
    private UserRepresentation user;
    private UserRepresentation checkAdmin;
    private GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration;
    private EnrollmentRequestStatusEnum status;
    private String reason;
    private String adminJustification;
    private String comment;
    private List<GroupEnrollmentRequestAttributesRepresentation> attributes;

    private List<String> groupRoles;

    public GroupEnrollmentRequestRepresentation() {
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

    public UserRepresentation getCheckAdmin() {
        return checkAdmin;
    }

    public void setCheckAdmin(UserRepresentation checkAdmin) {
        this.checkAdmin = checkAdmin;
    }

    public GroupEnrollmentConfigurationRepresentation getGroupEnrollmentConfiguration() {
        return groupEnrollmentConfiguration;
    }

    public void setGroupEnrollmentConfiguration(GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration) {
        this.groupEnrollmentConfiguration = groupEnrollmentConfiguration;
    }

    public EnrollmentRequestStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnrollmentRequestStatusEnum status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAdminJustification() {
        return adminJustification;
    }

    public void setAdminJustification(String adminJustification) {
        this.adminJustification = adminJustification;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<GroupEnrollmentRequestAttributesRepresentation> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<GroupEnrollmentRequestAttributesRepresentation> attributes) {
        this.attributes = attributes;
    }

    public List<String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<String> groupRoles) {
        this.groupRoles = groupRoles;
    }
}
