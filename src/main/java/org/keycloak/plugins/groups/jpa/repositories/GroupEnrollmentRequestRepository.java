package org.keycloak.plugins.groups.jpa.repositories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.TypedQuery;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestPager;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestRepresentation;

public class GroupEnrollmentRequestRepository extends GeneralRepository<GroupEnrollmentRequestEntity> {

    private final GroupRolesRepository groupRolesRepository;

    public GroupEnrollmentRequestRepository(KeycloakSession session, RealmModel realm, GroupRolesRepository groupRolesRepository) {
        super(session, realm);
        this.groupRolesRepository = groupRolesRepository;
    }

    @Override
    protected Class<GroupEnrollmentRequestEntity> getTClass() {
        return GroupEnrollmentRequestEntity.class;
    }

    public GroupEnrollmentRequestEntity create(GroupEnrollmentRequestRepresentation rep, String userId, GroupEnrollmentConfigurationEntity configuration) {
        GroupEnrollmentRequestEntity entity = new GroupEnrollmentRequestEntity();
        entity.setId(KeycloakModelUtils.generateId());
        UserEntity user = new UserEntity();
        user.setId(userId);
        entity.setUser(user);
        entity.setGroupEnrollmentConfiguration(configuration);
        entity.setComments(rep.getComments());
        entity.setStatus(EnrollmentRequestStatusEnum.PENDING_APPROVAL);
        if (rep.getGroupRoles() != null) {
            entity.setGroupRoles(rep.getGroupRoles().stream().map(x -> groupRolesRepository.getGroupRolesByNameAndGroup(x, configuration.getGroup().getId())).filter(Objects::nonNull).limit(configuration.isMultiselectRole() ? Integer.MAX_VALUE : 1).collect(Collectors.toList()));
        }
        create(entity);
        return entity;
    }

    public Long countOngoingByUserAndGroup(String userId, String groupId) {
        List<EnrollmentRequestStatusEnum> statusList = Stream.of(EnrollmentRequestStatusEnum.PENDING_APPROVAL, EnrollmentRequestStatusEnum.WAITING_FOR_REPLY).collect(Collectors.toList());
        return em.createNamedQuery("countOngoingByUserAndGroup", Long.class).setParameter("userId", userId).setParameter("groupId", groupId).setParameter("status", statusList).getSingleResult();
    }

    public GroupEnrollmentRequestPager groupEnrollmentPager(String userId, String groupId, String groupName, EnrollmentRequestStatusEnum status, Integer first, Integer max) {
        StringBuilder sqlQueryMain = new StringBuilder("from GroupEnrollmentRequestEntity f");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        if (groupId== null && groupName == null) {
            sqlQueryMain.append(" where f.user.id = :userId");
        } else  if (groupId!= null){
            sqlQueryMain.append(" join f.groupEnrollmentConfiguration c where f.user.id = :userId and c.group.id = :groupId");
            parameters.put("groupId", groupId);
        } else {
            sqlQueryMain.append(" join f.groupEnrollmentConfiguration c join c.group g where f.user.id = :userId and g.name like :groupName");
            parameters.put("groupName", "%" + groupName + "%");
        }
        if (status != null) {
            sqlQueryMain.append(" and f.status = :status");
            parameters.put("status", status);
        }

        TypedQuery<GroupEnrollmentRequestEntity> query = em.createQuery("select f " + sqlQueryMain.toString(), GroupEnrollmentRequestEntity.class);
        TypedQuery<Long> queryCount = em.createQuery("select count(f) " + sqlQueryMain.toString(), Long.class);
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
            queryCount.setParameter(e.getKey(), e.getValue());
        }
        List<GroupEnrollmentRequestRepresentation> enrollments = query.setFirstResult(first).setMaxResults(max).getResultStream().map(x -> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList());
        return new GroupEnrollmentRequestPager(enrollments, queryCount.getSingleResult());

    }

    public GroupEnrollmentRequestPager groupAdminEnrollmentPager(List<String> groupIds, String userSearch, EnrollmentRequestStatusEnum status, Integer first, Integer max) {
        StringBuilder sqlQueryMain = new StringBuilder("from GroupEnrollmentRequestEntity f join f.groupEnrollmentConfiguration c join c.group g");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("groupIds", groupIds);
        if (userSearch == null) {
            sqlQueryMain.append(" where g.id in (:groupIds)");
        } else {
            sqlQueryMain.append(" join f.user u where g.id in (:groupIds) and (u.firstName like :userSearch or u.lastName like :userSearch) ");
            parameters.put("userSearch", "%" + userSearch + "%");
        }
        if (status != null) {
            sqlQueryMain.append(" and f.status = :status");
            parameters.put("status", status);
        }

        TypedQuery<GroupEnrollmentRequestEntity> query = em.createQuery("select f " + sqlQueryMain.toString(), GroupEnrollmentRequestEntity.class);
        TypedQuery<Long> queryCount = em.createQuery("select count(f) " + sqlQueryMain.toString(), Long.class);
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
            queryCount.setParameter(e.getKey(), e.getValue());
        }
        List<GroupEnrollmentRequestRepresentation> enrollments = query.setFirstResult(first).setMaxResults(max).getResultStream().map(x -> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList());
        return new GroupEnrollmentRequestPager(enrollments, queryCount.getSingleResult());

    }

    public void deleteByGroup(String groupId) {
        em.createNamedQuery("deleteEnrollmentByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public void deleteByUser(String userId) {
        em.createNamedQuery("deleteEnrollmentByUser").setParameter("userId", userId).executeUpdate();
        em.createNamedQuery("updateEnrollmentByAdminUser").setParameter("userId", userId).executeUpdate();
    }

    public Stream<GroupEnrollmentRequestEntity> getRequestsByConfigurationAndStatus(String configurationId, List<EnrollmentRequestStatusEnum> status) {
        return em.createNamedQuery("getRequestsByConfigurationAndStatus").setParameter("configurationId", configurationId).setParameter("status", status).getResultStream();
    }
}
