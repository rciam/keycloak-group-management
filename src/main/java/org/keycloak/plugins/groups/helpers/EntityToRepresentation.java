package org.keycloak.plugins.groups.helpers;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.jpa.entities.*;
import org.keycloak.plugins.groups.representations.GroupAupRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentAttributesRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationAttributesRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.representations.GroupInvitationRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.stream.Collectors;

public class EntityToRepresentation {

    public static GroupEnrollmentConfigurationRepresentation toRepresentation(GroupEnrollmentConfigurationEntity entity, boolean containAttributes) {
        GroupEnrollmentConfigurationRepresentation rep = new GroupEnrollmentConfigurationRepresentation(entity.getId());
        org.keycloak.representations.idm.GroupRepresentation group = new GroupRepresentation();
        group.setId(entity.getGroup().getId());
        group.setName(entity.getGroup().getName());
        rep.setGroup(group);
        rep.setName(entity.getName());
        rep.setActive(entity.isActive());
        rep.setHideConfiguration(entity.isHideConfiguration());
        rep.setRequireApproval(entity.getRequireApproval());
        rep.setRequireAupAcceptance(entity.getRequireAupAcceptance());
        rep.setAupExpiryDays(entity.getAupExpiryDays());
        rep.setMembershipExpirationDays(entity.getMembershipExpirationDays());
        rep.setEnrollmentConclusion(entity.getEnrollmentConclusion());
        rep.setEnrollmentIntroduction(entity.getEnrollmentIntroduction());
        rep.setInvitationConclusion(entity.getInvitationConclusion());
        rep.setInvitationIntroduction(entity.getInvitationIntroduction());
        rep.setConfigurableRole(entity.isConfigurableRole());
        if ( entity.getAupEntity() != null)
            rep.setAup(toRepresentation(entity.getAupEntity()));
        if ( containAttributes && entity.getAttributes() != null)
            rep.setAttributes(entity.getAttributes().stream().map(EntityToRepresentation::toRepresentation).collect(Collectors.toList()));
        if (entity.getGroupRoles() != null)
            rep.setGroupRoles(entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));
        return rep;
    }

    private static GroupEnrollmentConfigurationAttributesRepresentation toRepresentation(GroupEnrollmentConfigurationAttributesEntity entity){
        GroupEnrollmentConfigurationAttributesRepresentation rep = new GroupEnrollmentConfigurationAttributesRepresentation();
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
        rep.setValidFrom(entity.getValidFrom());
        rep.setStatus(entity.getStatus());
        if (entity.getGroupRoles() != null)
            rep.setGroupRoles(entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));
        return rep;
    }

    public static GroupEnrollmentRepresentation toRepresentation(GroupEnrollmentEntity entity, RealmModel realm) {
        GroupEnrollmentRepresentation rep = new GroupEnrollmentRepresentation();
        rep.setId(entity.getId());
        rep.setUser(toBriefRepresentation(entity.getUser(), realm));
        if (entity.getCheckAdmin() != null )
            rep.setCheckAdmin(toBriefRepresentation(entity.getCheckAdmin(), realm));
        rep.setGroupEnrollmentConfiguration(toRepresentation(entity.getGroupEnrollmentConfiguration(), false));
        rep.setAdminJustification(entity.getAdminJustification());
        rep.setComment(entity.getComments());
        rep.setStatus(entity.getStatus());
        rep.setReason(entity.getReason());
        if ( entity.getAttributes()!= null)
            rep.setAttributes(entity.getAttributes().stream().map(attr-> EntityToRepresentation.toRepresentation(attr)).collect(Collectors.toList()));
        if (entity.getGroupRoles() != null)
            rep.setGroupRoles(entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));
        return rep;
    }

    private static GroupEnrollmentAttributesRepresentation toRepresentation(GroupEnrollmentAttributesEntity entity){
        GroupEnrollmentAttributesRepresentation rep = new GroupEnrollmentAttributesRepresentation();
        rep.setId(entity.getId());
        rep.setValue(entity.getValue());
        rep.setConfigurationAttribute(toRepresentation(entity.getConfigurationAttribute()));
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

    public static GroupInvitationRepresentation toRepresentation(GroupInvitationEntity entity) {
        GroupInvitationRepresentation rep = new GroupInvitationRepresentation();
        rep.setId(entity.getId());
        rep.setCreationDate(entity.getCreationDate());
        rep.setGroupEnrollmentConfiguration(toRepresentation(entity.getGroupEnrollmentConfiguration(), true));
        return rep;
    }


}
