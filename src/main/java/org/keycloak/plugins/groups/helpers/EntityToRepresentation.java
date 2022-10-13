package org.keycloak.plugins.groups.helpers;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupAupEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentStateEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.representations.GroupAupRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentAttributesRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentStateRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.stream.Collectors;

public class EntityToRepresentation {

    public static GroupEnrollmentConfigurationRepresentation toRepresentation(GroupEnrollmentConfigurationEntity entity) {
        GroupEnrollmentConfigurationRepresentation rep = new GroupEnrollmentConfigurationRepresentation(entity.getId());
        rep.setGroupId(entity.getGroup().getId());
        rep.setName(entity.getName());
        rep.setActive(entity.isActive());
        rep.setHideConfiguration(entity.isHideConfiguration());
        rep.setRequireApproval(entity.getRequireApproval());
        rep.setRequireAupAcceptance(entity.getRequireAupAcceptance());
        rep.setAupExpirySec(entity.getAupExpirySec());
        rep.setMembershipExpirationSec(entity.getMembershipExpirationSec());
        rep.setEnrollmentConclusion(entity.getEnrollmentConclusion());
        rep.setEnrollmentIntroduction(entity.getEnrollmentIntroduction());
        rep.setInvitationConclusion(entity.getInvitationConclusion());
        rep.setInvitationIntroduction(entity.getInvitationIntroduction());
        if ( entity.getAupEntity() != null)
            rep.setAup(toRepresentation(entity.getAupEntity()));
        if ( entity.getAttributes() != null)
            rep.setAttributes(entity.getAttributes().stream().map(EntityToRepresentation::toRepresentation).collect(Collectors.toList()));
        return rep;
    }

    private static GroupEnrollmentAttributesRepresentation toRepresentation(GroupEnrollmentAttributesEntity entity){
        GroupEnrollmentAttributesRepresentation rep = new GroupEnrollmentAttributesRepresentation();
        rep.setId(entity.getId());
        rep.setAttribute(entity.getAttribute());
        rep.setDefaultValue(entity.getDefaultValue());
        rep.setHidden(entity.getHidden());
        rep.setLabel(entity.getLabel());
        rep.setModifiable(entity.getModifiable());
        rep.setOrder(entity.getOrder());
        return rep;
    }

    private static GroupAupRepresentation toRepresentation(GroupAupEntity entity) {
        GroupAupRepresentation rep = new GroupAupRepresentation();
        rep.setId(entity.getId());
        rep.setType(entity.getType());
        rep.setContent(entity.getContent());
        rep.setMimeType(entity.getMimeType());
        rep.setUrl(entity.getUrl());
        return rep;
    }

    public static UserGroupMembershipExtensionRepresentation toRepresentation(UserGroupMembershipExtensionEntity entity, RealmModel realm) {
        UserGroupMembershipExtensionRepresentation rep = new UserGroupMembershipExtensionRepresentation();
        rep.setId(entity.getId());
        rep.setGroupId(entity.getGroup().getId());
        rep.setUser(toBriefRepresentation(entity.getUser(), realm));
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
