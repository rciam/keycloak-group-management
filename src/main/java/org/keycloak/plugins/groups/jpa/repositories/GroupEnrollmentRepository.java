package org.keycloak.plugins.groups.jpa.repositories;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.representations.GroupEnrollmentAttributesRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;

public class GroupEnrollmentRepository extends GeneralRepository<GroupEnrollmentEntity> {

    public GroupEnrollmentRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
    }

    @Override
    protected Class<GroupEnrollmentEntity> getTClass() {
        return GroupEnrollmentEntity.class;
    }

    public void create(GroupEnrollmentRepresentation rep, String userId){
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
        List<String> statusList = Stream.of(EnrollmentStatusEnum.PENDING_APPROVAL.toString(),EnrollmentStatusEnum.WAITING_FOR_REPLY.toString()).collect(Collectors.toList());
        return em.createNamedQuery("countOngoingByUserAndGroup", Long.class).setParameter("userId",userId).setParameter("groupId",groupId).setParameter("status",statusList).getSingleResult();
    }


}
