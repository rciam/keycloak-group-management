package org.keycloak.plugins.groups.jpa.repositories;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Query;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.GroupManagementEvent;
import org.keycloak.plugins.groups.enums.GroupEnrollmentAttributeEnum;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupManagementEventEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;

public class UserGroupMembershipExtensionRepository extends GeneralRepository<UserGroupMembershipExtensionEntity> {

    private static final Logger logger = Logger.getLogger(UserGroupMembershipExtensionRepository.class);
    public static final String eventId = "1";
    private  GroupManagementEventRepository eventRepository;

    public UserGroupMembershipExtensionRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
        this.eventRepository = new GroupManagementEventRepository(session);
    }

    @Override
    protected Class<UserGroupMembershipExtensionEntity> getTClass() {
        return UserGroupMembershipExtensionEntity.class;
    }

    public UserGroupMembershipExtensionEntity getByUserAndGroup(String groupId, String userId){
        List<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getByUserAndGroup").setParameter("groupId",groupId).setParameter("userId",userId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public void inactivateExpiredMemberships(KeycloakSession session) {
        GroupManagementEventEntity eventEntity = eventRepository.getEntity(eventId);
        if (eventEntity == null || LocalDate.now().isAfter(eventEntity.getDate())) {
            logger.info("group management daily action is executing ...");
            Stream<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getExpiredMemberships").setParameter("status", MemberStatusEnum.ENABLED).setParameter("date", LocalDate.now()).getResultStream();
            results.forEach(entity -> {
                entity.setStatus(MemberStatusEnum.DISABLED);
                update(entity);
                RealmModel realmModel = session.realms().getRealm(entity.getUser().getRealmId());
                UserModel user = session.users().getUserById(realmModel, entity.getUser().getId());
                GroupModel group = realmModel.getGroupById(entity.getGroup().getId());
                logger.info(user.getFirstName()+" "+user.getFirstName()+" is removing from being member of group "+group.getName());
                user.leaveGroup(group);
            });

            if (eventEntity == null) {
                eventEntity = new GroupManagementEventEntity();
                eventEntity.setId(eventId);
                eventEntity.setDate(LocalDate.now());
                eventRepository.create(eventEntity);
            } else {
                eventEntity.setDate(LocalDate.now());
                eventRepository.update(eventEntity);
            }
        }
    }

    public UserGroupMembershipExtensionRepresentationPager searchByGroup(String groupId, String search, MemberStatusEnum status, Integer first, Integer max, RealmModel realm) {

        String sqlQuery = "from UserGroupMembershipExtensionEntity f ";
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
        Stream<UserGroupMembershipExtensionEntity> results = queryList.getResultStream();

        Query queryCount = em.createQuery("select count(f) " + sqlQuery, Long.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryCount.setParameter(entry.getKey(), entry.getValue());
        }
        Long count = (Long) queryCount.getSingleResult();

        return new UserGroupMembershipExtensionRepresentationPager(results.map(x-> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList()), count);
    }

    @Transactional
    public void suspendUser(UserModel user, UserGroupMembershipExtensionEntity member, String justification, GroupModel group){
        member.setStatus(MemberStatusEnum.SUSPENDED);
        member.setJustification(justification);
        update(member);
        user.leaveGroup(group);
    }

    @Transactional
    public void activateUser(UserModel user, UserGroupMembershipExtensionEntity member, String justification, GroupModel group){
        member.setStatus(MemberStatusEnum.ENABLED);
        member.setJustification(justification);
        update(member);
        user.joinGroup(group);
    }


    @Transactional
    @Deprecated
    public void create(UserGroupMembershipExtensionRepresentation rep, String editor, UserModel userModel, GroupModel groupModel){
        UserGroupMembershipExtensionEntity entity = new UserGroupMembershipExtensionEntity();
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

    @Transactional
    public void create(GroupEnrollmentEntity enrollmentEntity, KeycloakSession session, String groupAdminId){
        UserGroupMembershipExtensionEntity entity = new UserGroupMembershipExtensionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEnrollmentConfigurationEntity configuration = enrollmentEntity.getGroupEnrollmentConfiguration();
        if (configuration.getAupExpirySec() != null)
            entity.setAupExpiresAt(LocalDate.ofEpochDay(Duration.ofMillis(Instant.now().toEpochMilli() + configuration.getAupExpirySec()).toDays()));
        String validThroughValue = enrollmentEntity.getAttributes().stream().filter(at -> GroupEnrollmentAttributeEnum.VALID_THROUGH.equals(at.getConfigurationAttribute().getAttribute())).findAny().orElse(new GroupEnrollmentAttributesEntity()).getValue();
        if (validThroughValue != null)
            entity.setMembershipExpiresAt(LocalDate.parse(validThroughValue, Utils.formatter));
        String validFromValue = enrollmentEntity.getAttributes().stream().filter(at -> GroupEnrollmentAttributeEnum.VALID_FROM.equals(at.getConfigurationAttribute().getAttribute())).findAny().orElse(new GroupEnrollmentAttributesEntity()).getValue();
        if (validFromValue != null)
            entity.setValidFrom(LocalDate.parse(validFromValue, Utils.formatter));
        entity.setGroup(configuration.getGroup());
        entity.setUser(enrollmentEntity.getUser());
        UserEntity editorUser = new UserEntity();
        editorUser.setId(groupAdminId);
        entity.setChangedBy(editorUser);
        entity.setJustification(enrollmentEntity.getAdminJustification());
        entity.setStatus(MemberStatusEnum.ENABLED);
        create(entity);

        GroupModel group = realm.getGroupById(configuration.getGroup().getId());
        UserModel userModel = session.users().getUserById(realm,enrollmentEntity.getUser().getId());
        userModel.joinGroup(group);
    }

    public void deleteByGroup(String groupId){
        em.createNamedQuery("deleteMembershipExtensionByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public void deleteByUser(String userId){
        em.createNamedQuery("deleteMembershipExtensionByUser").setParameter("userId", userId).executeUpdate();
    }

}
