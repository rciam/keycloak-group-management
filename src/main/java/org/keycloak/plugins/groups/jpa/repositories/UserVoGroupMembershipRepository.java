package org.keycloak.plugins.groups.jpa.repositories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.enums.StatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;
import org.keycloak.plugins.groups.representations.UserVoGroupMembershipRepresentation;
import org.keycloak.plugins.groups.representations.UserVoGroupMembershipRepresentationPager;

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

    public UserVoGroupMembershipRepresentationPager searchByGroup(String groupId, String search, StatusEnum status, Integer first, Integer max, RealmModel realm) {

        String sqlQuery = "from UserVoGroupMembershipEntity f ";
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        if (search != null) {
            sqlQuery += ", User u where f.group.id = :groupId and f.user.id = u.id and (u.email like :search or u.firstName like :search or u.lastName like :search)";
            params.put("search", "%" + search + "%");
        } else {
            sqlQuery += "where f.group.id = :groupId";
        }
        if (status != null) {
            sqlQuery += " and f.status = :status";
            params.put("status", status);
        }

        Query queryList = em.createQuery(sqlQuery).setFirstResult(first).setMaxResults(max);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryList.setParameter(entry.getKey(), entry.getValue());
        }
        Stream<UserVoGroupMembershipEntity> results = queryList.getResultStream();

        Query queryCount = em.createQuery("select count(f) " + sqlQuery, Long.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryCount.setParameter(entry.getKey(), entry.getValue());
        }
        Long count = (Long) queryCount.getSingleResult();

        return new UserVoGroupMembershipRepresentationPager(results.map(x-> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList()), count);
    }

    @Transactional
    public void suspendUser(UserModel user, UserVoGroupMembershipEntity member, String justification, GroupModel group){
        member.setStatus(StatusEnum.SUSPENDED);
        member.setJustification(justification);
        update(member);
        user.leaveGroup(group);
    }

    @Transactional
    public void activateUser(UserModel user, UserVoGroupMembershipEntity member, String justification, GroupModel group){
        member.setStatus(StatusEnum.ENABLED);
        member.setJustification(justification);
        update(member);
        user.joinGroup(group);
    }


    @Transactional
    public void create(UserVoGroupMembershipRepresentation rep, String editor, UserModel userModel, GroupModel groupModel){
        UserVoGroupMembershipEntity entity = new UserVoGroupMembershipEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAupExpiresAt(rep.getAupExpiresAt());
        entity.setMembershipExpiresAt(rep.getMembershipExpiresAt());
        GroupEntity group = new GroupEntity();
        group.setId(rep.getGroupId());
        entity.setGroup(group);
        UserEntity user = new UserEntity();
        user.setId(rep.getUser().getId());
        entity.setUser(user);
        UserEntity editorUser = new UserEntity();
        editorUser.setId(editor);
        entity.setChangedBy(editorUser);
        entity.setJustification(rep.getJustification());
        entity.setStatus(rep.getStatus());
        create(entity);
        userModel.joinGroup(groupModel);
    }

}
