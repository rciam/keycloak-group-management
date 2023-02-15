package org.keycloak.plugins.groups.jpa.repositories;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.jpa.entities.GroupAupEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.representations.GroupAupRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationAttributesRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;

public class GroupEnrollmentConfigurationRepository extends GeneralRepository<GroupEnrollmentConfigurationEntity> {

    private GroupRolesRepository groupRolesRepository;

    public GroupEnrollmentConfigurationRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    public void setGroupRolesRepository(GroupRolesRepository groupRolesRepository) {
        this.groupRolesRepository = groupRolesRepository;
    }

    @Override
    protected Class<GroupEnrollmentConfigurationEntity> getTClass() {
        return GroupEnrollmentConfigurationEntity.class;
    }

    public void create(GroupEnrollmentConfigurationRepresentation rep, String groupId){
        GroupEnrollmentConfigurationEntity entity = new GroupEnrollmentConfigurationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        entity.setGroup(group);
        toEntity(entity, rep, groupId);
        create(entity);
    }

    public void createDefault(String groupId, String groupName){
        //default values, hide by default
        GroupEnrollmentConfigurationEntity entity = new GroupEnrollmentConfigurationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        entity.setGroup(group);
        entity.setName(groupName);
        entity.setRequireApproval(true);
        entity.setRequireAupAcceptance(false);
        entity.setActive(true);
        entity.setHideConfiguration(true);
        entity.setMultiselectRole(true);
        entity.setGroupRoles(groupRolesRepository.getGroupRolesByGroup(groupId).map(x -> {
            GroupRolesEntity r = new GroupRolesEntity();
            r.setId(x.getId());
            r.setGroup(x.getGroup());
            r.setName(x.getName());
            return r;
        }).collect(Collectors.toList()));
        create(entity);
    }

    public void update(GroupEnrollmentConfigurationEntity entity, GroupEnrollmentConfigurationRepresentation rep){
        toEntity(entity, rep, entity.getGroup().getId());
        update(entity);
    }

    public Stream<GroupEnrollmentConfigurationEntity> getGroupAdminGroups(String userId) {
        return em.createNamedQuery("getAdminGroups").setParameter("userId",userId).getResultStream();
    }

    public Stream<GroupEnrollmentConfigurationEntity> getByGroup(String groupId) {
        return em.createNamedQuery("getByGroup").setParameter("groupId",groupId).getResultStream();
    }

    private void toEntity(GroupEnrollmentConfigurationEntity entity, GroupEnrollmentConfigurationRepresentation rep, String groupId) {
        entity.setName(rep.getName());
        entity.setActive(rep.isActive());
        entity.setHideConfiguration(rep.isHideConfiguration());
        entity.setRequireApproval(rep.getRequireApproval());
        entity.setRequireAupAcceptance(rep.getRequireAupAcceptance());
        entity.setAupExpiryDays(rep.getAupExpiryDays());
        entity.setMembershipExpirationDays(rep.getMembershipExpirationDays());
        entity.setEnrollmentConclusion(rep.getEnrollmentConclusion());
        entity.setEnrollmentIntroduction(rep.getEnrollmentIntroduction());
        entity.setInvitationConclusion(rep.getInvitationConclusion());
        entity.setInvitationIntroduction(rep.getInvitationIntroduction());
        entity.setMultiselectRole(rep.getMultiselectRole());
        if ( rep.getAup() != null)
            entity.setAupEntity(toEntity(rep.getAup()));
        if (rep.getAttributes() != null) {
            entity.setAttributes(new ArrayList<>());
            entity.getAttributes().addAll(rep.getAttributes().stream().map(attr-> this.toEntity(attr, entity)).collect(Collectors.toList()));
        } else if (entity.getAttributes() != null) {
            entity.getAttributes().clear();
        }
        if (rep.getGroupRoles() != null) {
            entity.setGroupRoles(rep.getGroupRoles().stream().map(x -> groupRolesRepository.getGroupRolesByNameAndGroup(x, groupId)).filter(Objects::nonNull).collect(Collectors.toList()));
        } else {
            entity.setGroupRoles(null);
        }
    }

    private GroupEnrollmentConfigurationAttributesEntity toEntity(GroupEnrollmentConfigurationAttributesRepresentation rep, GroupEnrollmentConfigurationEntity configuration){
        GroupEnrollmentConfigurationAttributesEntity entity = new GroupEnrollmentConfigurationAttributesEntity();
        entity.setId(rep.getId() != null ? rep.getId() : KeycloakModelUtils.generateId());
        entity.setLabel(rep.getLabel());
        entity.setGroupEnrollmentConfiguration(configuration);
        entity.setAttribute(rep.getAttribute());
        entity.setHidden(rep.getHidden());
        entity.setModifiable(rep.getModifiable());
        entity.setOrder(rep.getOrder());
        entity.setDefaultValue(rep.getDefaultValue());
        return entity;
    }

    private GroupAupEntity toEntity(GroupAupRepresentation rep) {
        GroupAupEntity entity = new GroupAupEntity();
        entity.setId(rep.getId() != null ? rep.getId() : KeycloakModelUtils.generateId());
        entity.setType(rep.getType());
        entity.setContent(rep.getContent());
        entity.setMimeType(rep.getMimeType());
        entity.setUrl(rep.getUrl());
        return entity;
    }

    public void deleteByGroup(String groupId){
        em.createNamedQuery("deleteEnrollmentConfigurationAttrByGroup").setParameter("groupId", groupId).executeUpdate();
        em.createNamedQuery("deleteEnrollmentConfigurationByGroup").setParameter("groupId", groupId).executeUpdate();
    }

}
