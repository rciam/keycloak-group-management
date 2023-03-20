package org.keycloak.plugins.groups.jpa.repositories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.helpers.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupAdminEntity;
import org.keycloak.plugins.groups.representations.GroupsPager;
import org.keycloak.representations.idm.GroupRepresentation;

public class GroupAdminRepository extends GeneralRepository<GroupAdminEntity> {

    public GroupAdminRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<GroupAdminEntity> getTClass() {
        return GroupAdminEntity.class;
    }

    public boolean isGroupAdmin(String userId, GroupModel group){
        List<String> groupIds = getThisAndParentGroupIds(group);
        Long count = em.createNamedQuery("countByUserAndGroups", Long.class).setParameter("groupIds",groupIds).setParameter("userId",userId).getSingleResult();
        return count > 0;
    }

    private  List<String> getThisAndParentGroupIds(GroupModel group) {
        List<String> groupIds =  new ArrayList<>();
        groupIds.add(group.getId());
        while ( group.getParent() != null){
            group = group.getParent();
            groupIds.add(group.getId());
        }
        return groupIds;
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

    public GroupsPager getAdminGroups(String userId, Integer first, Integer max) {
        List<GroupRepresentation> groups = em.createNamedQuery("getGroupsForAdmin", String.class).setParameter("userId",userId).setFirstResult(first).setMaxResults(max).getResultStream().map(id -> realm.getGroupById(id)) .map(g -> ModelToRepresentation.toSimpleGroupHierarchy(g, true)).collect(Collectors.toList());
        return new GroupsPager(groups,em.createNamedQuery("countGroupsForAdmin", Long.class).setParameter("userId",userId).getSingleResult());

    }

    public List<String> getAllAdminGroupIds(String userId) {
        List<String> groupIds = em.createNamedQuery("getGroupsForAdmin", String.class).setParameter("userId", userId).getResultStream().map(id -> realm.getGroupById(id)).flatMap(g ->this.getLeafGroupsIds(g).stream()).collect(Collectors.toList());
        return groupIds;
    }

    public Stream<String> getAllAdminGroupUsers(String groupId){
        GroupModel group = realm.getGroupById(groupId);
        List<String> groupIds = getThisAndParentGroupIds(group);
        return em.createNamedQuery("getAdminsForGroup", String.class).setParameter("groupIds", groupIds).getResultStream();
    }

    private Set<String> getLeafGroupsIds(GroupModel group) {
        Set<String> groupIds = new HashSet<>();
        groupIds.add(group.getId());
        group.getSubGroupsStream().forEach(g -> groupIds.addAll(getLeafGroupsIds(g)));
        return groupIds;
    }

    public void deleteByGroup(String groupId){
        em.createNamedQuery("deleteAdminByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public void deleteByUser(String userId){
        em.createNamedQuery("deleteAdminByUser").setParameter("userId", userId).executeUpdate();
    }


}
