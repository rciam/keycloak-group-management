package org.keycloak.plugins.groups.jpa.repositories;

import java.time.LocalDateTime;
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

    public GroupInvitationRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
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
        create(entity);
        return entity.getId();
    }

    public Stream<GroupInvitationEntity> getAllByRealm(){
        return em.createNamedQuery("getAllGroupInvitations").setParameter("realmId", realm.getId()).getResultStream();
    }

    public void acceptInvitation(GroupInvitationEntity entity, KeycloakSession session, UserModel userModel, GroupModel groupModel) {
        UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository= new UserGroupMembershipExtensionRepository(session, realm);

        UserGroupMembershipExtensionEntity memberExtensionEntity = new UserGroupMembershipExtensionEntity();
        memberExtensionEntity.setId(KeycloakModelUtils.generateId());
       // memberExtensionEntity.setAupExpiresAt(rep.getAupExpiresAt());
      //  memberExtensionEntity.setMembershipExpiresAt(rep.getMembershipExpiresAt());
        GroupEntity group = new GroupEntity();
        group.setId(groupModel.getId());
        memberExtensionEntity.setGroup(group);
        UserEntity user = new UserEntity();
        user.setId(userModel.getId());
        memberExtensionEntity.setUser(user);
        memberExtensionEntity.setChangedBy(entity.getCheckAdmin());
        memberExtensionEntity.setStatus(MemberStatusEnum.ENABLED);
        create(entity);
        userModel.joinGroup(groupModel);


    }

}
