package org.keycloak.plugins.groups.representations;

import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collection;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

public class GroupEnrollmentRepresentation {

    private String id;
    private UserRepresentation user;
    private UserEntity checkAdmin;
    private GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration;
    private EnrollmentStatusEnum status;
    private String reason;
    private String adminJustification;
    private String comment;
    private List<GroupEnrollmentAttributesRepresentation> attributes;

    public GroupEnrollmentRepresentation() {
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

    public UserEntity getCheckAdmin() {
        return checkAdmin;
    }

    public void setCheckAdmin(UserEntity checkAdmin) {
        this.checkAdmin = checkAdmin;
    }

    public GroupEnrollmentConfigurationRepresentation getGroupEnrollmentConfiguration() {
        return groupEnrollmentConfiguration;
    }

    public void setGroupEnrollmentConfiguration(GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration) {
        this.groupEnrollmentConfiguration = groupEnrollmentConfiguration;
    }

    public EnrollmentStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatusEnum status) {
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

    public List<GroupEnrollmentAttributesRepresentation> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<GroupEnrollmentAttributesRepresentation> attributes) {
        this.attributes = attributes;
    }
}
