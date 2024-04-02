package org.rciam.plugins.groups.jpa.repositories;

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
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.ModelToRepresentation;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupAdminEntity;
import org.rciam.plugins.groups.representations.GroupAdminRepresentation;
import org.rciam.plugins.groups.representations.GroupsPager;
import org.keycloak.representations.idm.GroupRepresentation;

public class GroupAdminRepository extends GeneralRepository<GroupAdminEntity> {

    public GroupAdminRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<GroupAdminEntity> getTClass() {
        return GroupAdminEntity.class;
    }

    public boolean isGroupAdmin(String userId, GroupModel group) {
        List<String> groupIds = getThisAndParentGroupIds(group, true);
        Long count = em.createNamedQuery("countByUserAndGroups", Long.class).setParameter("groupIds", groupIds).setParameter("userId", userId).getSingleResult();
        return count > 0;
    }

    private  List<String> getThisAndParentGroupIds(GroupModel group, boolean includeThis) {
        List<String> groupIds =  new ArrayList<>();
        if (includeThis)
            groupIds.add(group.getId());
        while (group.getParent() != null) {
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
        return em.createNamedQuery("getAdminByUserAndGroup", GroupAdminEntity.class).setParameter("groupId", groupId).setParameter("userId", userId).getResultStream().findAny().orElse(null);
    }

    public GroupsPager getAdminGroups(String userId, String search, Integer first, Integer max) {
        if (search == null) {
            List<GroupRepresentation> groups = em.createNamedQuery("getGroupsForAdmin", String.class).setParameter("userId", userId).setFirstResult(first).setMaxResults(max).getResultStream().map(realm::getGroupById).map(g -> ModelToRepresentation.toSimpleGroupHierarchy(g, true)).collect(Collectors.toList());
            return new GroupsPager(groups, em.createNamedQuery("countGroupsForAdmin", Long.class).setParameter("userId", userId).getSingleResult());
        } else {
            List<GroupRepresentation> groups = em.createNamedQuery("searchGroupsForAdmin", String.class).setParameter("userId", userId).setParameter("search", "%" + search.toLowerCase() + "%").setFirstResult(first).setMaxResults(max).getResultStream().map(realm::getGroupById).map(g -> ModelToRepresentation.toSimpleGroupHierarchy(g, true)).collect(Collectors.toList());
            return new GroupsPager(groups, em.createNamedQuery("countSearchGroupsForAdmin", Long.class).setParameter("userId", userId).setParameter("search", "%" + search.toLowerCase() + "%").getSingleResult());
        }
    }

    public GroupsPager getAdminGroups(String userId, Integer first, Integer max) {
        List<GroupRepresentation> groups = em.createNamedQuery("getGroupsForAdmin", String.class).setParameter("userId", userId).setFirstResult(first).setMaxResults(max).getResultStream().map(realm::getGroupById).map(g -> ModelToRepresentation.toSimpleGroupHierarchy(g, true)).collect(Collectors.toList());
        return new GroupsPager(groups, em.createNamedQuery("countGroupsForAdmin", Long.class).setParameter("userId", userId).getSingleResult());
    }
    public List<GroupAdminRepresentation> getAdminsForGroup(GroupModel group) {
        //prota to arxiko kai to vazo me true
        //meta ti lista xoris to id mono parent - an parent != null kai an den yparxei idi to vazo me false
        List<UserEntity> admins = em.createNamedQuery("getAdminsForGroup", GroupAdminEntity.class).setParameter("groupId", group.getId()).getResultStream().map(GroupAdminEntity::getUser).collect(Collectors.toList());
        List<GroupAdminRepresentation> adminsRep = admins.stream().map(x -> new GroupAdminRepresentation(EntityToRepresentation.toBriefRepresentation(x, realm), true)).collect(Collectors.toList());
        List<String> groupIds = getThisAndParentGroupIds(group, false);
        if (!groupIds.isEmpty()) {
            Stream<UserEntity> parentAdmins = em.createNamedQuery("getAdminsForGroupIds", UserEntity.class).setParameter("groupIds", groupIds).getResultStream().filter(x -> !admins.contains(x));
            adminsRep.addAll(parentAdmins.map(x -> new GroupAdminRepresentation(EntityToRepresentation.toBriefRepresentation(x, realm), false)).collect(Collectors.toList()));
        }
        return adminsRep;
    }

    public List<String> getAllAdminGroupIds(String userId) {
        return em.createNamedQuery("getGroupsForAdmin", String.class).setParameter("userId", userId).getResultStream().map(realm::getGroupById).flatMap(g -> this.getLeafGroupsIds(g).stream()).collect(Collectors.toList());
    }

    public List<String> getAdminGroupIdsByName(String userId, String groupName) {
        return em.createNamedQuery("getGroupsForAdmin", String.class).setParameter("userId", userId).getResultStream().map(realm::getGroupById).flatMap(Utils::getGroupWithSubgroups).filter(x->groupName == null || x.getName().toLowerCase().contains(groupName.toLowerCase())).flatMap(this::getGroupIdsWithSubgroups).distinct().collect(Collectors.toList());
    }
    private Stream<String> getGroupIdsWithSubgroups(GroupModel group){
        Set<String> groups = group.getSubGroupsStream().map(GroupModel::getId).collect(Collectors.toSet());
        groups.add(group.getId());
        return groups.stream();
    }

    public boolean hasAdminRights(String userId) {
        return em.createNamedQuery("countGroupsForAdmin", Long.class).setParameter("userId", userId).getSingleResult() > 0;
    }

    public Stream<String> getAllAdminIdsGroupUsers(GroupModel group){
        List<String> groupIds = getThisAndParentGroupIds(group, true);
        return em.createNamedQuery("getAdminsIdsForGroupIds", String.class).setParameter("groupIds", groupIds).getResultStream();
    }

    public Stream<String> getAllAdminIdsGroupUsers(String groupId){
        return getAllAdminIdsGroupUsers(realm.getGroupById(groupId));
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
