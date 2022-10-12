package org.keycloak.plugins.groups.jpa.repositories;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.jpa.entities.GroupAdminEntity;

public class GroupAdminRepository extends GeneralRepository<GroupAdminEntity> {

    public GroupAdminRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<GroupAdminEntity> getTClass() {
        return GroupAdminEntity.class;
    }

    public boolean isVoAdmin(String userId, GroupModel group){
       List<String> groupIds = new ArrayList<>();
       groupIds.add(group.getId());
       while ( group.getParent() != null){
           group = group.getParent();
           groupIds.add(group.getId());
       }
        Long count = em.createNamedQuery("countByUserAndGroups", Long.class).setParameter("groupIds",groupIds).setParameter("userId",userId).getSingleResult();
        return count > 0;
    }

    public void addGroupAdmin(String userId, String groupId) {
        GroupAdminEntity entity = new GroupAdminEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEntity group = new GroupEntity();
        group.setId(groupId);
        entity.setGroup(group);
        UserEntity user = new UserEntity();
        user.setId(userId);
        entity.setUser(user);
        create(entity);
    }

    public GroupAdminEntity getGroupAdminByUserAndGroup(String userId, String groupId) {
        return em.createNamedQuery("getAdminByUserAndGroup", GroupAdminEntity.class).setParameter("groupId",groupId).setParameter("userId",userId).getResultStream().findAny().orElse(null);
    }

}
