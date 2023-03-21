package org.keycloak.plugins.groups.jpa.repositories;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Query;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.GroupEnrollmentAttributeEnum;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.helpers.DummyClientConnection;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentRequestAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationAttributesEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupManagementEventEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestAttributesRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.theme.FreeMarkerUtil;

public class UserGroupMembershipExtensionRepository extends GeneralRepository<UserGroupMembershipExtensionEntity> {

    private static final Logger logger = Logger.getLogger(UserGroupMembershipExtensionRepository.class);
    private final String adminCli ="admin-cli";
    private final GroupManagementEventRepository eventRepository;
    private GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private GroupRolesRepository groupRolesRepository;

    public UserGroupMembershipExtensionRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
        this.eventRepository = new GroupManagementEventRepository(session, realm);
    }

    public UserGroupMembershipExtensionRepository(KeycloakSession session, RealmModel realm, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, GroupRolesRepository groupRolesRepository) {
        super(session, realm);
        this.eventRepository = new GroupManagementEventRepository(session, realm);
        this.groupEnrollmentConfigurationRepository = groupEnrollmentConfigurationRepository;
        this.groupRolesRepository = groupRolesRepository;
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
    public void dailyExecutedActions(KeycloakSession session) {
        GroupManagementEventEntity eventEntity = eventRepository.getEntity(Utils.eventId);
        CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        if (eventEntity == null || LocalDate.now().isAfter(eventEntity.getDate())) {
            logger.info("group management daily action is executing ...");
            Stream<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getExpiredMemberships").setParameter("date", LocalDate.now()).getResultStream();
            String serverUrl = eventEntity != null ? eventEntity.getServerUrl() : null;
            results.forEach(entity -> {
                RealmModel realmModel = session.realms().getRealm(entity.getUser().getRealmId());
                GroupAdminRepository groupAdminRepository = new GroupAdminRepository(session, realmModel);
                UserModel user = session.users().getUserById(realmModel, entity.getUser().getId());
                GroupModel group = realmModel.getGroupById(entity.getGroup().getId());
                logger.info(user.getFirstName() + " " + user.getFirstName() + " is removing from being member of group " + group.getName());
                deleteEntity(entity.getId());
                user.leaveGroup(group);
                AdminAuth adminAuth = new AdminAuth(realmModel, null, Utils.getChronJobUser(), realmModel.getClientByClientId(adminCli));
                AdminEventBuilder adminEvent = new AdminEventBuilder(realmModel, adminAuth, session, new DummyClientConnection("127.0.0.1"));
                adminEvent.realm(realmModel).operation(OperationType.DELETE).resource(ResourceType.GROUP_MEMBERSHIP).representation(EntityToRepresentation.toRepresentation(entity, realm)).resourcePath("127.0.0.1").success();
                customFreeMarkerEmailTemplateProvider.setRealm(realmModel);
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    customFreeMarkerEmailTemplateProvider.sendExpiredGroupMemberEmailToUser(group.getName(), group.getId(), serverUrl);
                } catch (EmailException e) {
                    logger.info("problem sending email to user " + user.getFirstName() + " " + user.getLastName());
                }
                groupAdminRepository.getAllAdminGroupUsers(group.getId()).map(id -> session.users().getUserById(realmModel, id)).forEach(admin -> {
                    if (admin != null) {
                        customFreeMarkerEmailTemplateProvider.setUser(admin);
                        try {
                            customFreeMarkerEmailTemplateProvider.sendExpiredGroupMemberEmailToAdmin(user, group.getName());
                        } catch (EmailException e) {
                            logger.info("problem sending email to group admin " + admin.getFirstName() + " " + admin.getLastName());
                        }
                    }
                });
            });

            if (eventEntity == null) {
                //first time, execute also weekly tasks
                weeklyTaskExecution(customFreeMarkerEmailTemplateProvider, session, serverUrl);
                eventEntity = new GroupManagementEventEntity();
                eventEntity.setId(Utils.eventId);
                eventEntity.setDate(LocalDate.now());
                eventEntity.setDateForWeekTasks(LocalDate.now());
                eventRepository.create(eventEntity);
            } else {
                eventEntity.setDate(LocalDate.now());
                eventRepository.update(eventEntity);

                if (LocalDate.now().isAfter(eventEntity.getDateForWeekTasks().plusDays(6))) {
                    //weekly tasks execution
                    weeklyTaskExecution(customFreeMarkerEmailTemplateProvider, session, serverUrl);
                    eventEntity.setDateForWeekTasks(LocalDate.now());
                    eventRepository.update(eventEntity);
                }
            }
        }

    }

    private void weeklyTaskExecution(CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, KeycloakSession session, String serverUrl) {
        session.realms().getRealmsStream().forEach(realmModel -> {
            customFreeMarkerEmailTemplateProvider.setRealm(realmModel);
            session.groups().getGroupsStream(realmModel).forEach(group -> {
                Integer dateBeforeNotification = group.getFirstAttribute(Utils.expirationNotificationPeriod) != null ? Integer.valueOf(group.getFirstAttribute(Utils.expirationNotificationPeriod)) : realmModel.getAttribute(Utils.expirationNotificationPeriod, 21);
                //find group membership that expires in less days than dateBeforeNotification (at the end of this week)
                Stream<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getExpiredMembershipsByGroup").setParameter("groupId",group.getId()).setParameter("date", LocalDate.now().plusDays(dateBeforeNotification + 6)).getResultStream();
                results.forEach(entity -> {
                    UserModel user = session.users().getUserById(realmModel, entity.getUser().getId());
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    LocalDate date = entity.getMembershipExpiresAt() != null && (entity.getAupExpiresAt() == null || entity.getMembershipExpiresAt().isBefore(entity.getAupExpiresAt())) ? entity.getMembershipExpiresAt() : entity.getAupExpiresAt();
                    try {
                        customFreeMarkerEmailTemplateProvider.sendExpiredGroupMembershipNotification(group.getName(), date.format(Utils.formatter), group.getId(), serverUrl);
                    } catch (EmailException e) {
                        e.printStackTrace();
                        logger.info("problem sending email to user  " + user.getFirstName() + " " + user.getLastName());
                    }
                });
            });
        });
    }

    public UserGroupMembershipExtensionRepresentationPager searchByGroup(String groupId, String search, MemberStatusEnum status, Integer first, Integer max) {

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
    public void create(UserGroupMembershipExtensionRepresentation rep, String editor, UserModel userModel, GroupModel groupModel, AdminEventBuilder adminEvent, KeycloakUriInfo uri){
        UserGroupMembershipExtensionEntity entity = new UserGroupMembershipExtensionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        entity.setAupExpiresAt(rep.getAupExpiresAt());
        entity.setMembershipExpiresAt(rep.getMembershipExpiresAt());
        GroupEntity group = new GroupEntity();
        group.setId(rep.getGroup().getId());
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
       // adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(entity).resourcePath(uri).success();
        userModel.joinGroup(groupModel);
    }

    @Transactional
    public void createOrUpdate(GroupEnrollmentRequestEntity enrollmentEntity, KeycloakSession session, String groupAdminId, AdminEventBuilder adminEvent){
        UserGroupMembershipExtensionEntity entity = getByUserAndGroup(enrollmentEntity.getGroupEnrollmentConfiguration().getGroup().getId(), enrollmentEntity.getUser().getId());
        boolean isNotMember = entity == null || !MemberStatusEnum.ENABLED.equals(entity.getStatus());
        if (entity == null) {
            entity = new UserGroupMembershipExtensionEntity();
            entity.setId(KeycloakModelUtils.generateId());
        }
        GroupEnrollmentConfigurationEntity configuration = enrollmentEntity.getGroupEnrollmentConfiguration();
        if (configuration.getAupExpiryDays() != null) {
            entity.setAupExpiresAt(LocalDate.now().plusDays(configuration.getAupExpiryDays()));
        } else {
            entity.setAupExpiresAt(null);
        }
        String validThroughValue = enrollmentEntity.getAttributes().stream().filter(at -> GroupEnrollmentAttributeEnum.VALID_THROUGH.equals(at.getConfigurationAttribute().getAttribute())).findAny().orElse(new GroupEnrollmentRequestAttributesEntity()).getValue();
        if (validThroughValue != null) {
            entity.setMembershipExpiresAt(LocalDate.parse(validThroughValue, Utils.formatter));
        } else {
            entity.setMembershipExpiresAt(null);
        }
        String validFromValue = enrollmentEntity.getAttributes().stream().filter(at -> GroupEnrollmentAttributeEnum.VALID_FROM.equals(at.getConfigurationAttribute().getAttribute())).findAny().orElse(new GroupEnrollmentRequestAttributesEntity()).getValue();
        if (validFromValue != null) {
            entity.setValidFrom(LocalDate.parse(validFromValue, Utils.formatter));
        } else {
            entity.setValidFrom(null);
        }
        entity.setGroup(configuration.getGroup());
        entity.setUser(enrollmentEntity.getUser());
        UserEntity editorUser = new UserEntity();
        editorUser.setId(groupAdminId);
        entity.setChangedBy(editorUser);
        entity.setJustification(enrollmentEntity.getAdminJustification());
        entity.setStatus(MemberStatusEnum.ENABLED);
        entity.setGroupEnrollmentConfigurationId(configuration.getId());
        if (enrollmentEntity.getGroupRoles() != null) {
            entity.setGroupRoles(enrollmentEntity.getGroupRoles().stream().map(x -> {
                GroupRolesEntity r = new GroupRolesEntity();
                r.setId(x.getId());
                r.setGroup(x.getGroup());
                r.setName(x.getName());
                return r;
            }).collect(Collectors.toList()));
        } else {
            entity.setGroupRoles(null);
        }
        update(entity);

        if (isNotMember) {
            GroupModel group = realm.getGroupById(configuration.getGroup().getId());
            UserModel userModel = session.users().getUserById(realm, enrollmentEntity.getUser().getId());
            userModel.joinGroup(group);
        }
        adminEvent.operation(isNotMember? OperationType.CREATE :  OperationType.UPDATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(EntityToRepresentation.toRepresentation(entity, realm)).resourcePath(session.getContext().getUri()).success();
    }

    @Transactional
    public void createOrUpdate(GroupEnrollmentRequestRepresentation rep, KeycloakSession session, UserModel user, AdminEventBuilder adminEvent){
        GroupEnrollmentConfigurationEntity configuration = groupEnrollmentConfigurationRepository.getEntity(rep.getGroupEnrollmentConfiguration().getId());
        UserGroupMembershipExtensionEntity entity = getByUserAndGroup(configuration.getGroup().getId(), user.getId());
        boolean isNotMember = entity == null || !MemberStatusEnum.ENABLED.equals(entity.getStatus());
        if (entity == null) {
            entity = new UserGroupMembershipExtensionEntity();
            entity.setId(KeycloakModelUtils.generateId());
        }
        if (configuration.getAupExpiryDays() != null) {
            entity.setAupExpiresAt(LocalDate.now().plusDays(configuration.getAupExpiryDays()));
        } else {
            entity.setAupExpiresAt(null);
        }
        entity.setMembershipExpiresAt(null);
        entity.setValidFrom(null);
        for (GroupEnrollmentConfigurationAttributesEntity attributeConfEntity : configuration.getAttributes()){
            if ( GroupEnrollmentAttributeEnum.VALID_THROUGH.equals(attributeConfEntity.getAttribute())) {
                String validThroughValue = rep.getAttributes().stream().filter(at -> attributeConfEntity.getId().equals(at.getConfigurationAttribute().getId())).findAny().orElse(new GroupEnrollmentRequestAttributesRepresentation()).getValue();
                entity.setMembershipExpiresAt(validThroughValue == null ? null : LocalDate.parse(validThroughValue, Utils.formatter));
            } else if ( GroupEnrollmentAttributeEnum.VALID_FROM.equals(attributeConfEntity.getAttribute())) {
                String validFromValue = rep.getAttributes().stream().filter(at -> attributeConfEntity.getId().equals(at.getConfigurationAttribute().getId())).findAny().orElse(new GroupEnrollmentRequestAttributesRepresentation()).getValue();
                entity.setValidFrom(validFromValue == null ? null : LocalDate.parse(validFromValue, Utils.formatter));
            }
        }
        entity.setGroup(configuration.getGroup());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(user.getId());
        entity.setUser(userEntity);
        entity.setChangedBy(null);
        entity.setJustification(null);
        entity.setStatus(MemberStatusEnum.ENABLED);
        entity.setGroupEnrollmentConfigurationId(configuration.getId());
        if (rep.getGroupRoles() != null) {
            entity.setGroupRoles(rep.getGroupRoles().stream().map(x -> groupRolesRepository.getGroupRolesByNameAndGroup(x, configuration.getGroup().getId())).filter(Objects::nonNull).collect(Collectors.toList()));
        } else {
            entity.setGroupRoles(null);
        }
        update(entity);

        if (isNotMember) {
            GroupModel group = realm.getGroupById(configuration.getGroup().getId());
            user.joinGroup(group);
        }
        adminEvent.operation(isNotMember? OperationType.CREATE :  OperationType.UPDATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(EntityToRepresentation.toRepresentation(entity, realm)).resourcePath(session.getContext().getUri()).success();
    }

    @Transactional
    public void create(GroupInvitationRepository groupInvitationRepository, GroupInvitationEntity invitationEntity, UserModel userModel, AdminEventBuilder adminEvent, KeycloakUriInfo uri) {
        GroupEnrollmentConfigurationEntity configuration = invitationEntity.getGroupEnrollmentConfiguration();
        UserGroupMembershipExtensionEntity entity = getByUserAndGroup(configuration.getGroup().getId(), userModel.getId());
        boolean isNotMember = entity == null || !MemberStatusEnum.ENABLED.equals(entity.getStatus());
        if (entity == null) {
            entity = new UserGroupMembershipExtensionEntity();
            entity.setId(KeycloakModelUtils.generateId());
        }

        if (configuration.getAupExpiryDays() != null) {
            entity.setAupExpiresAt(LocalDate.now().plusDays(configuration.getAupExpiryDays()));
        } else {
            entity.setAupExpiresAt(null);
        }
        String validThroughValue = configuration.getAttributes().stream().filter(at -> GroupEnrollmentAttributeEnum.VALID_THROUGH.equals(at.getAttribute())).findAny().orElse(new GroupEnrollmentConfigurationAttributesEntity()).getDefaultValue();
        entity.setMembershipExpiresAt(validThroughValue != null ? LocalDate.parse(validThroughValue, Utils.formatter) : null);
        String validFromValue = configuration.getAttributes().stream().filter(at -> GroupEnrollmentAttributeEnum.VALID_FROM.equals(at.getAttribute())).findAny().orElse(new GroupEnrollmentConfigurationAttributesEntity()).getDefaultValue();
        entity.setValidFrom(validFromValue != null ? LocalDate.parse(validFromValue, Utils.formatter) : null);
        entity.setGroup(configuration.getGroup());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userModel.getId());
        entity.setUser(userEntity);
        entity.setChangedBy(invitationEntity.getCheckAdmin());
        entity.setStatus(MemberStatusEnum.ENABLED);
        entity.setJustification(null);
        entity.setGroupEnrollmentConfigurationId(configuration.getId());
        if (invitationEntity.getGroupRoles() != null ) {
            entity.setGroupRoles(invitationEntity.getGroupRoles().stream().map(x -> {
                GroupRolesEntity r = new GroupRolesEntity();
                r.setId(x.getId());
                r.setGroup(x.getGroup());
                r.setName(x.getName());
                return r;
            }).collect(Collectors.toList()));
        } else {
            entity.setGroupRoles(null);
        }
        update(entity);

        if (isNotMember) {
            GroupModel group = realm.getGroupById(configuration.getGroup().getId());
            userModel.joinGroup(group);
        }
        adminEvent.operation(isNotMember? OperationType.CREATE :  OperationType.UPDATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(EntityToRepresentation.toRepresentation(entity, realm)).resourcePath(uri).success();
        groupInvitationRepository.deleteEntity(invitationEntity.getId());
    }

    public void deleteByGroup(String groupId){
        em.createNamedQuery("deleteMembershipExtensionByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public void deleteByUser(String userId){
        em.createNamedQuery("deleteMembershipExtensionByUser").setParameter("userId", userId).executeUpdate();
    }

}
