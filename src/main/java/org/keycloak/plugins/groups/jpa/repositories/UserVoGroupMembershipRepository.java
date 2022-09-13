package org.keycloak.plugins.groups.jpa.repositories;

import java.util.List;

import javax.persistence.TypedQuery;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;
import org.keycloak.plugins.groups.representations.UserVoGroupMembershipRepresentation;

public class UserVoGroupMembershipRepository extends GeneralRepository<UserVoGroupMembershipEntity> {

    public UserVoGroupMembershipRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<UserVoGroupMembershipEntity> getTClass() {
        return UserVoGroupMembershipEntity.class;
    }

    public UserVoGroupMembershipEntity getByUserAndGroup(String groupId, String userId){
        List<UserVoGroupMembershipEntity> results = em.createNamedQuery("getByUserAndGroup").setParameter("groupId",groupId).setParameter("userId",userId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean isVoAdmin(String groupId, String userId){
       Long result = em.createNamedQuery("countVoAdmin", Long.class).setParameter("groupId",groupId).setParameter("userId",userId).getSingleResult();
       return  result > 0;
    }

    public void create(UserVoGroupMembershipRepresentation rep, String editor){
        UserVoGroupMembershipEntity entity = new UserVoGroupMembershipEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAupExpiresAt(rep.getAupExpiresAt());
        entity.setMembershipExpiresAt(rep.getMembershipExpiresAt());
        GroupEntity group = new GroupEntity();
        group.setId(rep.getGroupId());
        entity.setGroup(group);
        UserEntity user = new UserEntity();
        user.setId(rep.getUserId());
        entity.setUser(user);
        UserEntity editorUser = new UserEntity();
        editorUser.setId(editor);
        entity.setChangedBy(editorUser);
        entity.setIsAdmin(rep.getAdmin());
        entity.setJustification(rep.getJustification());
        entity.setStatus(rep.getStatus());
        create(entity);
    }

}
