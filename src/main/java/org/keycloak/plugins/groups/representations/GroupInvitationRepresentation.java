package org.keycloak.plugins.groups.representations;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.representations.idm.UserRepresentation;

public class GroupInvitationRepresentation {

    private String id;
    private LocalDateTime creationDate;
    private UserRepresentation checkAdmin;
    private GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
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
}
