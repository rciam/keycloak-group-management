package org.keycloak.plugins.groups.jpa.repositories;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Query;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.GroupEnrollmentAttributeEnum;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupManagementEventEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.keycloak.plugins.groups.scheduled.DeleteExpiredInvitationTask;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.timer.TimerProvider;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;

public class UserGroupMembershipExtensionRepository extends GeneralRepository<UserGroupMembershipExtensionEntity> {

    private static final Logger logger = Logger.getLogger(UserGroupMembershipExtensionRepository.class);
    public static final String eventId = "1";
    private  GroupManagementEventRepository eventRepository;
    private  GroupAdminRepository groupAdminRepository;

    public UserGroupMembershipExtensionRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
        this.eventRepository = new GroupManagementEventRepository(session);
        this.groupAdminRepository = new GroupAdminRepository(session, realm);
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
        CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
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
                groupAdminRepository.getAllAdminGroupUsers(group.getId()).map(id -> session.users().getUserById(realmModel, id)).forEach(admin -> {
                    if ( admin != null) {
                        customFreeMarkerEmailTemplateProvider.setRealm(realmModel);
                        customFreeMarkerEmailTemplateProvider.setUser(admin);
                        try {
                            customFreeMarkerEmailTemplateProvider.sendExpiredGroupMemberEmailToAdmin(user, group.getName());
                        } catch (EmailException e) {
                           logger.info("problem sending email to group admin "+ admin.getFirstName()+ " "+ admin.getLastName());
                        }
                    }
                });
            });

            if (eventEntity == null) {
                //first time, execute also weekly tasks
                weeklyTaskExecution(customFreeMarkerEmailTemplateProvider,session);
                eventEntity = new GroupManagementEventEntity();
                eventEntity.setId(eventId);
                eventEntity.setDate(LocalDate.now());
                eventEntity.setDateForWeekTasks(LocalDate.now());
                eventRepository.create(eventEntity);
            } else {
                eventEntity.setDate(LocalDate.now());
                eventRepository.update(eventEntity);
            }
        }
        if (LocalDate.now().isAfter(eventEntity.getDateForWeekTasks().plusDays(6))) {
            //weekly tasks execution
            weeklyTaskExecution(customFreeMarkerEmailTemplateProvider,session);
            eventEntity.setDateForWeekTasks(LocalDate.now());
            eventRepository.update(eventEntity);
        }

        session.realms().getRealmsStream().forEach(realm -> {
            GroupInvitationRepository groupInvitationRepository = new GroupInvitationRepository(session, realm);
            groupInvitationRepository.getAllByRealm().forEach(entity -> {
                TimerProvider timer = session.getProvider(TimerProvider.class);
                long invitationExpirationHour = realm.getAttribute(Utils.urlExpirationPeriod) != null ? Long.valueOf(realm.getAttribute(Utils.urlExpirationPeriod)) : 72;
                long interval = entity.getCreationDate().atZone(ZoneId.systemDefault()).toEpochSecond() + (invitationExpirationHour * 3600 * 1000) -LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
                if (interval <=  60 * 1000)
                    interval = 60 * 1000;
                timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new DeleteExpiredInvitationTask(entity.getId(), realm.getId()), interval), interval, "DeleteExpiredInvitation_"+entity.getId());
            });
        });
    }

    private void weeklyTaskExecution(CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, KeycloakSession session){
        Stream<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getSoonExpiredMemberships").setParameter("status", MemberStatusEnum.ENABLED).setParameter("date", LocalDate.now().plusDays(6)).getResultStream();
        results.forEach( entity -> {
            UserModel user = session.users().getUserById(session.realms().getRealm(entity.getUser().getRealmId()), entity.getUser().getId());
            customFreeMarkerEmailTemplateProvider.setUser(user);
            LocalDate date = entity.getMembershipExpiresAt() != null && ( entity.getAupExpiresAt() == null || entity.getMembershipExpiresAt().isBefore(entity.getAupExpiresAt()) ) ? entity.getMembershipExpiresAt() : entity.getAupExpiresAt();
            try {
                customFreeMarkerEmailTemplateProvider.sendExpiredGroupMembershipNotification(entity.getGroup().getName(), date.format(Utils.formatter),entity.getGroup().getId());
            } catch (EmailException e) {
                logger.info("problem sending email to user  "+ user.getFirstName()+ " "+ user.getLastName());
            }
        });
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
        entity.setStatus(MemberStatusEnum.ENABLED);
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

    @Transactional
    public void create( GroupInvitationRepository groupInvitationRepository, GroupInvitationEntity invitationEntity, UserModel userModel){
        UserGroupMembershipExtensionEntity entity = new UserGroupMembershipExtensionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        GroupEnrollmentConfigurationEntity configuration = invitationEntity.getGroupEnrollmentConfiguration();
        if (configuration.getAupExpirySec() != null)
            entity.setAupExpiresAt(LocalDate.ofEpochDay(Duration.ofMillis(Instant.now().toEpochMilli() + configuration.getAupExpirySec()).toDays()));
        String validThroughValue = configuration.getAttributes().stream().filter(at -> GroupEnrollmentAttributeEnum.VALID_THROUGH.equals(at.getAttribute())).findAny().orElse(new GroupEnrollmentConfigurationAttributesEntity()).getDefaultValue();
        if (validThroughValue != null)
            entity.setMembershipExpiresAt(LocalDate.parse(validThroughValue, Utils.formatter));
        String validFromValue = configuration.getAttributes().stream().filter(at -> GroupEnrollmentAttributeEnum.VALID_FROM.equals(at.getAttribute())).findAny().orElse(new GroupEnrollmentConfigurationAttributesEntity()).getDefaultValue();
        if (validFromValue != null)
            entity.setValidFrom(LocalDate.parse(validFromValue, Utils.formatter));
        entity.setGroup(configuration.getGroup());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userModel.getId());
        entity.setUser(userEntity);
        UserEntity editorUser = new UserEntity();
        editorUser.setId(userModel.getId());
        entity.setChangedBy(editorUser);
        entity.setStatus(MemberStatusEnum.ENABLED);
        create(entity);

        GroupModel group = realm.getGroupById(configuration.getGroup().getId());
        userModel.joinGroup(group);
        groupInvitationRepository.deleteEntity(invitationEntity.getId());
    }

    public void deleteByGroup(String groupId){
        em.createNamedQuery("deleteMembershipExtensionByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public void deleteByUser(String userId){
        em.createNamedQuery("deleteMembershipExtensionByUser").setParameter("userId", userId).executeUpdate();
    }

}
