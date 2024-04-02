package org.rciam.plugins.groups.jpa.repositories;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.rciam.plugins.groups.representations.GroupInvitationInitialRepresentation;

public class GroupInvitationRepository extends GeneralRepository<GroupInvitationEntity> {

    private GroupRolesRepository groupRolesRepository;

    public GroupInvitationRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    public GroupInvitationRepository(KeycloakSession session, RealmModel realm, GroupRolesRepository groupRolesRepository) {
        super(session, realm);
        this.groupRolesRepository = groupRolesRepository;
    }

    @Override
    protected Class<GroupInvitationEntity> getTClass() {
        return GroupInvitationEntity.class;
    }

    public String createForMember(GroupInvitationInitialRepresentation rep, String adminId, GroupEnrollmentConfigurationEntity conf){
        GroupInvitationEntity entity = new GroupInvitationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setCreationDate(LocalDateTime.now());
        UserEntity checkAdmin = new UserEntity();
        checkAdmin.setId(adminId);
        entity.setCheckAdmin(checkAdmin);
        entity.setGroupEnrollmentConfiguration(conf);
        entity.setRealmId(realm.getId());
        entity.setForMember(true);
        if (rep.getGroupRoles() != null)
            entity.setGroupRoles(rep.getGroupRoles().stream().map(x -> groupRolesRepository.getGroupRolesByNameAndGroup(x,conf.getGroup().getId())).filter(Objects::nonNull).collect(Collectors.toList()));
        create(entity);
        return entity.getId();
    }

    public String createForAdmin(String groupId, String adminId){
        GroupInvitationEntity entity = new GroupInvitationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setCreationDate(LocalDateTime.now());
        entity.setRealmId(realm.getId());
        entity.setForMember(false);
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        entity.setGroup(group);
        UserEntity checkAdmin = new UserEntity();
        checkAdmin.setId(adminId);
        entity.setCheckAdmin(checkAdmin);
        create(entity);
        return entity.getId();
    }

    public Stream<GroupInvitationEntity> getAllByRealm(){
        return em.createNamedQuery("getAllGroupInvitations").setParameter("realmId", realm.getId()).getResultStream();
    }

    public void deleteByGroup(String groupId){
        em.createNamedQuery("deleteInvitationByGroup").setParameter("groupId", groupId).executeUpdate();
    }


}
