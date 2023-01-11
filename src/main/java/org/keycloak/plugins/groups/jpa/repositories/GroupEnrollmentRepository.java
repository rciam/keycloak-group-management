package org.keycloak.plugins.groups.jpa.repositories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.TypedQuery;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.representations.GroupEnrollmentAttributesRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentPager;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.representations.GroupsPager;

public class GroupEnrollmentRepository extends GeneralRepository<GroupEnrollmentEntity> {

    public GroupEnrollmentRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<GroupEnrollmentEntity> getTClass() {
        return GroupEnrollmentEntity.class;
    }

    public GroupEnrollmentEntity create(GroupEnrollmentRepresentation rep, String userId){
        GroupEnrollmentEntity entity = new GroupEnrollmentEntity();
        entity.setId(KeycloakModelUtils.generateId());
        UserEntity user = new UserEntity();
        user.setId(userId);
        entity.setUser(user);
        GroupEnrollmentConfigurationEntity configuration = new GroupEnrollmentConfigurationEntity();
        configuration.setId(rep.getGroupEnrollmentConfiguration().getId());
        entity.setGroupEnrollmentConfiguration(configuration);
        entity.setReason(rep.getReason());
        entity.setStatus(EnrollmentStatusEnum.PENDING_APPROVAL);
        if (rep.getAttributes() != null)
            entity.setAttributes(rep.getAttributes().stream().map(x -> toEntity(x, entity)).collect(Collectors.toList()));
        create(entity);
        return  entity;
    }

    private GroupEnrollmentAttributesEntity toEntity(GroupEnrollmentAttributesRepresentation rep, GroupEnrollmentEntity enrollment){
        GroupEnrollmentAttributesEntity entity = new GroupEnrollmentAttributesEntity();
        entity.setId(rep.getId()!= null ? rep.getId() : KeycloakModelUtils.generateId());
        entity.setValue(rep.getValue());
        GroupEnrollmentConfigurationAttributesEntity confAttrEntity = new GroupEnrollmentConfigurationAttributesEntity();
        confAttrEntity.setId(rep.getConfigurationAttribute().getId());
        entity.setConfigurationAttribute(confAttrEntity);
        entity.setEnrollment(enrollment);
        return entity;
    }

    public Long countOngoingByUserAndGroup(String userId, String groupId) {
        List<EnrollmentStatusEnum> statusList = Stream.of(EnrollmentStatusEnum.PENDING_APPROVAL,EnrollmentStatusEnum.WAITING_FOR_REPLY).collect(Collectors.toList());
        return em.createNamedQuery("countOngoingByUserAndGroup", Long.class).setParameter("userId",userId).setParameter("groupId",groupId).setParameter("status",statusList).getSingleResult();
    }

    public GroupEnrollmentPager groupEnrollmentPager(String userId, String groupName, EnrollmentStatusEnum status, Integer first, Integer max){
        StringBuilder sqlQueryMain = new StringBuilder("from GroupEnrollmentEntity f");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId",userId);
        if (groupName == null) {
            sqlQueryMain.append(" where f.user.id = :userId");
        } else {
            sqlQueryMain.append(" join f.groupEnrollmentConfiguration c join c.group g where f.user.id = :userId and g.name like :groupName");
            parameters.put("groupName","%"+groupName+"%");
        }
        if (status != null) {
            sqlQueryMain.append(" and f.status = :status");
            parameters.put("status",status);
        }

        TypedQuery<GroupEnrollmentEntity> query = em.createQuery("select f "+sqlQueryMain.toString(), GroupEnrollmentEntity.class);
        TypedQuery<Long> queryCount = em.createQuery("select count(f) "+ sqlQueryMain.toString(), Long.class);
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
            queryCount.setParameter(e.getKey(), e.getValue());
        }
        List<GroupEnrollmentRepresentation> enrollments = query.setFirstResult(first).setMaxResults(max).getResultStream().map(x -> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList());
        return new GroupEnrollmentPager(enrollments,queryCount.getSingleResult());

    }

    public GroupEnrollmentPager groupAdminEnrollmentPager(List<String> groupIds, String userSearch, EnrollmentStatusEnum status, Integer first, Integer max){
        StringBuilder sqlQueryMain = new StringBuilder("from GroupEnrollmentEntity f join f.groupEnrollmentConfiguration c join c.group g");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("groupIds",groupIds);
        if (userSearch == null) {
            sqlQueryMain.append(" where g.id in (:groupIds)");
        } else {
            sqlQueryMain.append(" join f.user u where g.id in (:groupIds) and (u.firstName like :userSearch or u.lastName like :userSearch) ");
            parameters.put("userSearch","%"+userSearch+"%");
        }
        if (status != null) {
            sqlQueryMain.append(" and f.status = :status");
            parameters.put("status",status);
        }

        TypedQuery<GroupEnrollmentEntity> query = em.createQuery("select f "+sqlQueryMain.toString(), GroupEnrollmentEntity.class);
        TypedQuery<Long> queryCount = em.createQuery("select count(f) "+ sqlQueryMain.toString(), Long.class);
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
            queryCount.setParameter(e.getKey(), e.getValue());
        }
        List<GroupEnrollmentRepresentation> enrollments = query.setFirstResult(first).setMaxResults(max).getResultStream().map(x -> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList());
        return new GroupEnrollmentPager(enrollments,queryCount.getSingleResult());

    }

    public void deleteByGroup(String groupId){
        em.createNamedQuery("deleteEnrollmentAttrByGroup").setParameter("groupId", groupId).executeUpdate();
        em.createNamedQuery("deleteEnrollmentByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public void deleteByUser(String userId){
        em.createNamedQuery("deleteEnrollmentAttrByUser").setParameter("userId", userId).executeUpdate();
        em.createNamedQuery("deleteEnrollmentByUser").setParameter("userId", userId).executeUpdate();
        em.createNamedQuery("updateEnrollmentByAdminUser").setParameter("userId", userId).executeUpdate();
    }


}
