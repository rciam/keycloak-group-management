package org.keycloak.plugins.groups.jpa.repositories;

import java.util.stream.Stream;

import liquibase.pro.packaged.G;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.jpa.entities.GroupAupEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.representations.GroupAupRepresentation;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;

public class GroupConfigurationRepository extends GeneralRepository<GroupConfigurationEntity> {

    public GroupConfigurationRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<GroupConfigurationEntity> getTClass() {
        return GroupConfigurationEntity.class;
    }

    public void create(GroupConfigurationRepresentation rep, String groupId, String editorId){
        GroupConfigurationEntity entity = new GroupConfigurationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        entity.setGroup(group);
        toEntity(entity, rep, editorId);
        create(entity);
    }

    public void createDefault(String groupId, String groupName){
        //default values, hide by default
        GroupConfigurationEntity entity = new GroupConfigurationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        entity.setGroup(group);
        entity.setName(groupName);
        entity.setRequireApproval(true);
        entity.setRequireAupAcceptance(false);
        entity.setActive(true);
        entity.setHideConfiguration(true);
        create(entity);
    }

    public void update( GroupConfigurationEntity entity, GroupConfigurationRepresentation rep, String editorId){
        toEntity(entity, rep, editorId);
        update(entity);
    }

    public Stream<GroupConfigurationEntity> getVoAdminGroups(String userId) {
        return em.createNamedQuery("getVoAdminGroups").setParameter("userId",userId).getResultStream();
    }

    public Stream<GroupConfigurationEntity> getByGroup(String groupId) {
        return em.createNamedQuery("getByGroup").setParameter("groupId",groupId).getResultStream();
    }

    private void toEntity(GroupConfigurationEntity entity, GroupConfigurationRepresentation rep, String editorId) {
        entity.setName(rep.getName());
        entity.setActive(rep.isActive());
        entity.setHideConfiguration(rep.isHideConfiguration());
        entity.setRequireApproval(rep.getRequireApproval());
        entity.setRequireAupAcceptance(rep.getRequireAupAcceptance());
        entity.setAupExpirySec(rep.getAupExpirySec());
        entity.setMembershipExpirationSec(rep.getMembershipExpirationSec());
        entity.setEnrollmentConclusion(rep.getEnrollmentConclusion());
        entity.setEnrollmentIntroduction(rep.getEnrollmentIntroduction());
        entity.setInvitationConclusion(rep.getInvitationConclusion());
        entity.setInvitationIntroduction(rep.getInvitationIntroduction());
        if ( rep.getAup() != null)
            entity.setAupEntity(toEntity(rep.getAup(), editorId));
    }

    private GroupAupEntity toEntity(GroupAupRepresentation rep, String editorId) {
        GroupAupEntity entity = new GroupAupEntity();
        entity.setId(rep.getId() != null ? rep.getId() : KeycloakModelUtils.generateId());
        entity.setType(rep.getType());
        entity.setContent(rep.getContent());
        entity.setMimeType(rep.getMimeType());
        entity.setUrl(rep.getUrl());
        UserEntity editor = new UserEntity();
        editor.setId(editorId);
        entity.setEditor(editor);
        return entity;
    }

}
