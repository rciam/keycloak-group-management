package org.keycloak.plugins.groups.jpa.repositories;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.representations.GroupInvitationInitialRepresentation;

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

    public String create(GroupInvitationInitialRepresentation rep, String adminId, GroupEnrollmentConfigurationEntity conf){
        GroupInvitationEntity entity = new GroupInvitationEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setCreationDate(LocalDateTime.now());
        UserEntity checkAdmin = new UserEntity();
        checkAdmin.setId(adminId);
        entity.setCheckAdmin(checkAdmin);
        entity.setGroupEnrollmentConfiguration(conf);
        entity.setRealmId(realm.getId());
        if (rep.getGroupRoles() != null)
            entity.setGroupRoles(rep.getGroupRoles().stream().map(x -> groupRolesRepository.getGroupRolesByNameAndGroup(x,conf.getGroup().getId())).filter(Objects::nonNull).collect(Collectors.toList()));
        create(entity);
        return entity.getId();
    }

    public Stream<GroupInvitationEntity> getAllByRealm(){
        return em.createNamedQuery("getAllGroupInvitations").setParameter("realmId", realm.getId()).getResultStream();
    }

}
