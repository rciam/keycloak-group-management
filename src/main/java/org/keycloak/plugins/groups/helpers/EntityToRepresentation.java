package org.keycloak.plugins.groups.helpers;

import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.FederatedIdentityEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupAupEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentStateEntity;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;
import org.keycloak.plugins.groups.representations.GroupAupRepresentation;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentStateRepresentation;
import org.keycloak.plugins.groups.representations.UserVoGroupMembershipRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.stream.Collectors;

public class EntityToRepresentation {

    public static GroupConfigurationRepresentation toRepresentation(GroupConfigurationEntity entity, RealmModel realm) {
        GroupConfigurationRepresentation rep = new GroupConfigurationRepresentation(entity.getId());
        rep.setDescription(entity.getDescription());
        rep.setRequireApproval(entity.getRequireApproval());
        rep.setRequireAupAcceptance(entity.getRequireAupAcceptance());
        rep.setAupExpirySec(entity.getAupExpirySec());
        rep.setMembershipExpirationSec(entity.getMembershipExpirationSec());
        if ( entity.getAupEntity() != null)
            rep.setAup(toRepresentation(entity.getAupEntity(), realm));
        return rep;
    }

    private static GroupAupRepresentation toRepresentation(GroupAupEntity entity, RealmModel realm) {
        GroupAupRepresentation rep = new GroupAupRepresentation();
        rep.setId(entity.getId());
        rep.setType(entity.getType());
        rep.setContent(entity.getContent());
        rep.setMimeType(entity.getMimeType());
        rep.setUrl(entity.getUrl());
        rep.setEditor(toBriefRepresentation(entity.getEditor(), realm));
        return rep;
    }

    public static UserVoGroupMembershipRepresentation toRepresentation(UserVoGroupMembershipEntity entity, RealmModel realm) {
        UserVoGroupMembershipRepresentation rep = new UserVoGroupMembershipRepresentation();
        rep.setId(entity.getId());
        rep.setGroupId(entity.getGroup().getId());
        rep.setUser(toBriefRepresentation(entity.getUser(), realm));
        rep.setAdmin(entity.getIsAdmin());
        rep.setJustification(entity.getJustification());
        rep.setAupExpiresAt(entity.getAupExpiresAt());
        rep.setMembershipExpiresAt(entity.getMembershipExpiresAt());
        rep.setStatus(entity.getStatus());
        return rep;
    }

    public static GroupEnrollmentRepresentation toRepresentation(GroupEnrollmentEntity entity, RealmModel realm) {
        GroupEnrollmentRepresentation rep = new GroupEnrollmentRepresentation();
        rep.setId(entity.getId());
        rep.setGroup(toBriefRepresentation(entity.getGroup()));
        rep.setUser(toBriefRepresentation(entity.getUser(), realm));
        rep.setEnrollmentStates(entity.getEnrollmentStates().stream().map(es->toRepresentation(es)).collect(Collectors.toList()));
        return rep;
    }

    public static GroupEnrollmentStateRepresentation toRepresentation(GroupEnrollmentStateEntity entity) {
        GroupEnrollmentStateRepresentation rep = new GroupEnrollmentStateRepresentation();
        rep.setId(entity.getId());
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

    public static UserRepresentation toBriefRepresentation(UserEntity entity, RealmModel realm) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(entity.getId());
        rep.setFirstName(entity.getFirstName());
        rep.setLastName(entity.getLastName());
        rep.setEmail(entity.getEmail());
        rep.setEmailVerified(entity.isEmailVerified());
        rep.setUsername(entity.getUsername());
        if (entity.getFederatedIdentities() != null)
            rep.setFederatedIdentities(entity.getFederatedIdentities().stream().map(fed -> getFederatedIdentityRep(realm, fed.getIdentityProvider())).collect(Collectors.toList()));
        return rep;
    }

    private static FederatedIdentityRepresentation getFederatedIdentityRep(RealmModel realm, String idPAlias){
        FederatedIdentityRepresentation rep = new FederatedIdentityRepresentation();
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(idPAlias);
        rep.setIdentityProvider(idp.getDisplayName() != null ? idp.getDisplayName() : idPAlias);
        return rep;
    }




}
