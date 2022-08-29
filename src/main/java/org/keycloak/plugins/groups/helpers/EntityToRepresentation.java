package org.keycloak.plugins.groups.helpers;

import org.keycloak.models.GroupModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentStateEntity;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentStateRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.stream.Collectors;

public class EntityToRepresentation {

    public static GroupConfigurationRepresentation toRepresentation(GroupConfigurationEntity entity) {
        GroupConfigurationRepresentation rep = new GroupConfigurationRepresentation(entity.getId());
        rep.setDescription(entity.getDescription());
        rep.setRequireApproval(entity.getRequireApproval());
        rep.setRequireAupAcceptance(entity.getRequireAupAcceptance());
        rep.setAupExpirySec(entity.getAupExpirySec());
        rep.setMembershipExpirationSec(entity.getMembershipExpirationSec());
        return rep;
    }

    public static GroupEnrollmentRepresentation toRepresentation(GroupEnrollmentEntity entity) {
        GroupEnrollmentRepresentation rep = new GroupEnrollmentRepresentation();
        rep.setId(entity.getId());
        rep.setGroup(toBriefRepresentation(entity.getGroup()));
        rep.setUser(toBriefRepresentation(entity.getUser()));
        rep.setEnrollmentStates(entity.getEnrollmentStates().stream().map(es->toRepresentation(es)).collect(Collectors.toList()));
        return rep;
    }

    public static GroupEnrollmentStateRepresentation toRepresentation(GroupEnrollmentStateEntity entity) {
        GroupEnrollmentStateRepresentation rep = new GroupEnrollmentStateRepresentation();
        rep.setEnrollmentId(entity.getEnrollmentEntity().getId());
        rep.setJustification(entity.getJustification());
        rep.setState(entity.getState());
        rep.setTimestamp(entity.getTimestamp());
        return rep;
    }

    public static GroupRepresentation toBriefRepresentation(GroupEntity entity) {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setId(entity.getId());
        rep.setName(entity.getName());
        return rep;
    }

    public static UserRepresentation toBriefRepresentation(UserEntity entity) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(entity.getId());
        rep.setFirstName(entity.getFirstName());
        rep.setLastName(entity.getLastName());
        rep.setEmail(entity.getEmail());
        rep.setEmailVerified(entity.isEmailVerified());
        rep.setUsername(entity.getUsername());
        return rep;
    }




}
