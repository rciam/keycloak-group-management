package org.rciam.plugins.groups.helpers;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.rciam.plugins.groups.jpa.entities.*;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRulesRepresentation;
import org.rciam.plugins.groups.representations.MemberUserAttributeConfigurationRepresentation;
import org.rciam.plugins.groups.representations.GroupAupRepresentation;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestRepresentation;
import org.rciam.plugins.groups.representations.GroupInvitationRepresentation;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EntityToRepresentation {

    private EntityToRepresentation(){}

    public static GroupEnrollmentConfigurationRepresentation toRepresentation(GroupEnrollmentConfigurationEntity entity, boolean fullPath, RealmModel realm) {
        GroupEnrollmentConfigurationRepresentation rep = new GroupEnrollmentConfigurationRepresentation(entity.getId());
        GroupRepresentation group = toBriefRepresentation(entity.getGroup(), true, fullPath, realm);
        rep.setGroup(group);
        rep.setName(entity.getName());
        rep.setActive(entity.isActive());
        rep.setVisibleToNotMembers(entity.isVisibleToNotMembers());
        rep.setRequireApproval(entity.getRequireApproval());
        rep.setRequireApprovalForExtension(entity.getRequireApprovalForExtension());
        rep.setValidFrom(entity.getValidFrom());
        rep.setMembershipExpirationDays(entity.getMembershipExpirationDays());
        rep.setEnrollmentConclusion(entity.getEnrollmentConclusion());
        rep.setEnrollmentIntroduction(entity.getEnrollmentIntroduction());
        rep.setInvitationConclusion(entity.getInvitationConclusion());
        rep.setInvitationIntroduction(entity.getInvitationIntroduction());
        rep.setMultiselectRole(entity.isMultiselectRole());
        if (entity.getAupEntity() != null)
            rep.setAup(toRepresentation(entity.getAupEntity()));
        if (entity.getGroupRoles() != null)
            rep.setGroupRoles(entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));
        rep.setCommentsNeeded(entity.getCommentsNeeded());
        if (rep.getCommentsNeeded()) {
            rep.setCommentsLabel(entity.getCommentsLabel());
            rep.setCommentsDescription(entity.getCommentsDescription());
        }
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

    public static UserGroupMembershipExtensionRepresentation toRepresentation(UserGroupMembershipExtensionEntity entity, RealmModel realm, boolean fullPath) {
        UserGroupMembershipExtensionRepresentation rep = new UserGroupMembershipExtensionRepresentation();
        rep.setId(entity.getId());
        GroupRepresentation group = toBriefRepresentation(entity.getGroup(), true, fullPath, realm);
        rep.setGroup(group);
        rep.setUser(toBriefRepresentation(entity.getUser(), realm));
        rep.setJustification(entity.getJustification());
        rep.setMembershipExpiresAt(entity.getMembershipExpiresAt());
        rep.setValidFrom(entity.getValidFrom());
        rep.setStatus(entity.getStatus());
        if (entity.getGroupRoles() != null)
            rep.setGroupRoles(entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));
        return rep;
    }

    public static UserGroupMembershipExtensionRepresentation toRepresentation(UserGroupMembershipExtensionEntity entity, RealmModel realm, String groupId) {
        UserGroupMembershipExtensionRepresentation rep = toRepresentation(entity, realm, true);
        rep.setDirect(groupId.equals(rep.getGroup().getId()));
        return rep;
    }

    private static Map<String, List<String>> getGroupAttributes(Collection<GroupAttributeEntity> attributesList) {
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<>();
        for (GroupAttributeEntity attr : attributesList) {
            result.add(attr.getName(), attr.getValue());
        }
        return result;
    }

    public static GroupEnrollmentRequestRepresentation toRepresentation(GroupEnrollmentRequestEntity entity, RealmModel realm) {
        GroupEnrollmentRequestRepresentation rep = new GroupEnrollmentRequestRepresentation();
        rep.setId(entity.getId());
        rep.setUser(toBriefRepresentation(entity.getUser(), realm));
        rep.setUserFirstName(entity.getUserFirstName());
        rep.setUserLastName(entity.getUserLastName());
        rep.setUserEmail(entity.getUserEmail());
        rep.setUserIdentifier(entity.getUserIdentifier());
        rep.setUserAssurance(entity.getUserAssurance());
        rep.setUserAuthnAuthorities(entity.getUserAuthnAuthorities());
        if (entity.getCheckAdmin() != null)
            rep.setCheckAdmin(toBriefRepresentation(entity.getCheckAdmin(), realm));
        rep.setGroupEnrollmentConfiguration(toRepresentation(entity.getGroupEnrollmentConfiguration(), true, realm));
        rep.setAdminJustification(entity.getAdminJustification());
        rep.setReviewComments(entity.getReviewComments());
        rep.setStatus(entity.getStatus());
        rep.setComments(entity.getComments());
        rep.setSubmittedDate(entity.getSubmittedDate());
        rep.setApprovedDate(entity.getApprovedDate());
        if (entity.getGroupRoles() != null)
            rep.setGroupRoles(entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));
        return rep;
    }

    public static GroupRepresentation toBriefRepresentation(GroupEntity entity, boolean attributes, boolean fullPath, RealmModel realm) {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setId(entity.getId());
        rep.setName(entity.getName());
        if (attributes && entity.getAttributes() != null)
            rep.setAttributes(getGroupAttributes(entity.getAttributes()));
        if (fullPath) {
            GroupModel group = realm.getGroupById(entity.getId());
            rep.setPath(ModelToRepresentation.buildGroupPath(group));
        }
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
        if (entity.getAttributes() != null && !entity.getAttributes().isEmpty()) {
            MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
            for (UserAttributeEntity attr : entity.getAttributes()) {
                attributes.add(attr.getName(), attr.getValue());
            }
            rep.setAttributes(attributes);
        }
        if (entity.getFederatedIdentities() != null)
            rep.setFederatedIdentities(entity.getFederatedIdentities().stream().map(fed -> Utils.getFederatedIdentityRep(realm, fed.getIdentityProvider())).collect(Collectors.toList()));
        return rep;
    }

    public static GroupInvitationRepresentation toRepresentation(GroupInvitationEntity entity, RealmModel realm) {
        GroupInvitationRepresentation rep = new GroupInvitationRepresentation();
        rep.setId(entity.getId());
        rep.setCreationDate(entity.getCreationDate());
        rep.setForMember(entity.getForMember());
        if (entity.getGroupEnrollmentConfiguration() != null)
            rep.setGroupEnrollmentConfiguration(toRepresentation(entity.getGroupEnrollmentConfiguration(), false,  realm));
        if (entity.getGroupRoles() != null)
            rep.setGroupRoles(entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()));
        if (entity.getGroup() != null) {
            GroupRepresentation group = toBriefRepresentation(entity.getGroup(), true, false, realm);
            rep.setGroup(group);
        }

        return rep;
    }

    public static MemberUserAttributeConfigurationRepresentation toRepresentation(MemberUserAttributeConfigurationEntity entity) {
        MemberUserAttributeConfigurationRepresentation rep = new MemberUserAttributeConfigurationRepresentation(entity.getUserAttribute(), entity.getUrnNamespace(), entity.getAuthority(), entity.getSignatureMessage());
        rep.setId(entity.getId());
        return rep;
    }

    public static GroupEnrollmentConfigurationRulesRepresentation toRepresentation(GroupEnrollmentConfigurationRulesEntity entity) {
        GroupEnrollmentConfigurationRulesRepresentation rep = new GroupEnrollmentConfigurationRulesRepresentation();
        rep.setId(entity.getId());
        rep.setType(entity.getType());
        rep.setField(entity.getField());
        rep.setDefaultValue(entity.getDefaultValue());
        rep.setMax(entity.getMax());
        rep.setRequired(entity.getRequired());
        return rep;
    }


}
