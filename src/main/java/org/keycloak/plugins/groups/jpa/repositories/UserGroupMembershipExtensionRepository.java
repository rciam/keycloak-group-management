package org.keycloak.plugins.groups.jpa.repositories;

import java.io.UnsupportedEncodingException;
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
import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.helpers.DummyClientConnection;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.LoginEventHelper;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupManagementEventEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.keycloak.plugins.groups.representations.UserRepresentationPager;
import org.keycloak.theme.FreeMarkerUtil;

public class UserGroupMembershipExtensionRepository extends GeneralRepository<UserGroupMembershipExtensionEntity> {

    private static final Logger logger = Logger.getLogger(UserGroupMembershipExtensionRepository.class);
    private static final String orderbyStr = " order by f.user.lastName, f.user.firstName";
    private final GroupManagementEventRepository eventRepository;
    private GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private GroupRolesRepository groupRolesRepository;

    public UserGroupMembershipExtensionRepository(KeycloakSession session, RealmModel realm) {
        super(session, realm);
        this.eventRepository = new GroupManagementEventRepository(session, realm);
        this.groupEnrollmentConfigurationRepository = new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupRolesRepository = new GroupRolesRepository(session, realm);
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

    public UserGroupMembershipExtensionEntity getByUserAndGroup(String groupId, String userId) {
        List<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getByUserAndGroup").setParameter("groupId", groupId).setParameter("userId", userId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public Stream<UserGroupMembershipExtensionEntity> getActiveByUser(String userId) {
        return em.createNamedQuery("getActiveByUser").setParameter("userId", userId).getResultStream();
    }

    @Transactional
    public void dailyExecutedActions(KeycloakSession session) {
        GroupManagementEventEntity eventEntity = eventRepository.getEntity(Utils.eventId);
        MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        if (eventEntity == null || LocalDate.now().isAfter(eventEntity.getDate())) {
            logger.info("group management daily action is executing ...");
            Stream<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getExpiredMemberships").setParameter("date", LocalDate.now()).getResultStream();
            results.forEach(entity -> {
                RealmModel realmModel = session.realms().getRealm(entity.getUser().getRealmId());
                String serverUrl = realm.getAttribute(Utils.KEYCLOAK_URL);
                GroupAdminRepository groupAdminRepository = new GroupAdminRepository(session, realmModel);
                UserModel user = session.users().getUserById(realmModel, entity.getUser().getId());
                GroupModel group = realmModel.getGroupById(entity.getGroup().getId());
                logger.info(user.getFirstName() + " " + user.getFirstName() + " is removing from being member of group " + group.getName());
                List<String> roleNames = entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList());
                deleteMember(entity, group, user);
                try {
                    MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
                    List<String> memberUserAttributeValues = user.getAttribute(memberUserAttribute.getUserAttribute());
                    String groupName = Utils.getGroupNameForMemberUserAttribute(entity.getGroup(), realm);
                    memberUserAttributeValues.removeIf(x -> Utils.removeMemberUserAttributeCondition(x, memberUserAttribute.getUrnNamespace(), groupName));
                    user.setAttribute(memberUserAttribute.getUserAttribute(), memberUserAttributeValues);
                } catch (UnsupportedEncodingException e) {
                    logger.warn("problem calculating user attribute value for group : " + group.getId()+ " and user :  " + user.getId());
                }
                LoginEventHelper.createGroupEvent(realm, session,  new DummyClientConnection("127.0.0.1"), user, null
                        , Utils.GROUP_MEMBERSHIP_DELETE, ModelToRepresentation.buildGroupPath(group), roleNames, LocalDate.now());

                customFreeMarkerEmailTemplateProvider.setRealm(realmModel);
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    customFreeMarkerEmailTemplateProvider.sendExpiredGroupMemberEmailToUser(group.getName(), group.getId(), serverUrl);
                } catch (EmailException e) {
                    logger.warn("problem sending email to user " + user.getFirstName() + " " + user.getLastName());
                }
                groupAdminRepository.getAllAdminIdsGroupUsers(group).map(id -> session.users().getUserById(realmModel, id)).filter(Objects::nonNull).forEach(admin -> {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    try {
                        customFreeMarkerEmailTemplateProvider.sendExpiredGroupMemberEmailToAdmin(user, group.getName());
                    } catch (EmailException e) {
                        logger.warn("problem sending email to group admin " + admin.getFirstName() + " " + admin.getLastName());
                    }

                });
            });

            Stream<UserGroupMembershipExtensionEntity> pendingMembers = em.createNamedQuery("getMembershipsByStatusAndValidFrom").setParameter("status", MemberStatusEnum.PENDING).setParameter("date", LocalDate.now()).getResultStream();
            pendingMembers.forEach(member -> {
                member.setStatus(MemberStatusEnum.ENABLED);
                update(member);
                GroupModel group = realm.getGroupById(member.getGroup().getId());
                UserModel userModel = session.users().getUserById(realm, member.getUser().getId());
                userModel.joinGroup(group);
                MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
                try {
                    Utils.changeUserAttributeValue(userModel, member, Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm), memberUserAttribute);
                } catch (UnsupportedEncodingException e) {
                    logger.warn("problem calculating user attribute value for group : " + member.getGroup().getId()+ " and user :  " + member.getUser().getId());
                }
                LoginEventHelper.createGroupEvent(realm, session, new DummyClientConnection("127.0.0.1"), userModel, member.getChangedBy() != null ? member.getChangedBy().getId() : userModel.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(userModel.getId())
                        , Utils.GROUP_MEMBERSHIP_CREATE, ModelToRepresentation.buildGroupPath(group), member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());
            });

            if (eventEntity == null) {
                //first time, execute also weekly tasks
                weeklyTaskExecution(customFreeMarkerEmailTemplateProvider, session);
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
                    weeklyTaskExecution(customFreeMarkerEmailTemplateProvider, session);
                    eventEntity.setDateForWeekTasks(LocalDate.now());
                    eventRepository.update(eventEntity);
                }
            }
        }

    }

    @Transactional
    public void deleteMember(UserGroupMembershipExtensionEntity entity, GroupModel group, UserModel user) {
        logger.info(user.getFirstName() + " " + user.getFirstName() + " is removing from being member of group " + group.getName());
        deleteEntity(entity.getId());
        user.leaveGroup(group);
    }

    private void weeklyTaskExecution(CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realmModel -> {
            customFreeMarkerEmailTemplateProvider.setRealm(realmModel);
            String serverUrl = realm.getAttribute(Utils.KEYCLOAK_URL);
            session.groups().getGroupsStream(realmModel).forEach(group -> {
                Integer dateBeforeNotification = group.getFirstAttribute(Utils.expirationNotificationPeriod) != null ? Integer.valueOf(group.getFirstAttribute(Utils.expirationNotificationPeriod)) : realmModel.getAttribute(Utils.expirationNotificationPeriod, 21);
                //find group membership that expires in less days than dateBeforeNotification (at the end of this week)
                Stream<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getExpiredMembershipsByGroup").setParameter("groupId", group.getId()).setParameter("date", LocalDate.now().plusDays(dateBeforeNotification + 6)).getResultStream();
                results.forEach(entity -> {
                    UserModel user = session.users().getUserById(realmModel, entity.getUser().getId());
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    try {
                        customFreeMarkerEmailTemplateProvider.sendExpiredGroupMembershipNotification(group.getName(), entity.getMembershipExpiresAt().format(Utils.formatter), group.getId(), serverUrl);
                    } catch (EmailException e) {
                        e.printStackTrace();
                        logger.info("problem sending email to user  " + user.getFirstName() + " " + user.getLastName());
                    }
                });
            });
        });
    }

    public UserGroupMembershipExtensionRepresentationPager userpager(String userId, String search, Integer first, Integer max, String order, String orderType) {

        String sqlQuery = "from UserGroupMembershipExtensionEntity f ";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        if (search != null) {
            sqlQuery += "join GroupEntity g on f.group.id = g.id where f.user.id = :userId and f.status = 'ENABLED' and g.name like :search";
            params.put("search", "%" + search + "%");
        } else {
            sqlQuery += "where f.user.id = :userId and f.status = 'ENABLED'";
        }

        Query queryList = em.createQuery("select f " + sqlQuery + " order by " + order + " " + orderType).setFirstResult(first).setMaxResults(max);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryList.setParameter(entry.getKey(), entry.getValue());
        }
        Stream<UserGroupMembershipExtensionEntity> results = queryList.getResultStream();

        Query queryCount = em.createQuery("select count(f) " + sqlQuery, Long.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryCount.setParameter(entry.getKey(), entry.getValue());
        }
        Long count = (Long) queryCount.getSingleResult();

        return new UserGroupMembershipExtensionRepresentationPager(results.map(x -> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList()), count);
    }

    public UserGroupMembershipExtensionRepresentationPager searchByGroup(String groupId, String search, MemberStatusEnum status, String role, Integer first, Integer max) {


        StringBuilder fromQuery = new StringBuilder("from UserGroupMembershipExtensionEntity f");
        StringBuilder sqlQuery = new StringBuilder(" where f.group.id = :groupId");
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        if (role != null) {
            fromQuery.append(" join f.groupRoles r ");
            sqlQuery.append(" and r.name like :role");
            params.put("role", "%" + role + "%");
        }
        if (search != null) {
            fromQuery.append(", UserEntity u");
            sqlQuery.append(" and f.user.id = u.id and (u.email like :search or u.firstName like :search or u.lastName like :search)");
            params.put("search", "%" + search + "%");
        }
        if (status != null) {
            sqlQuery.append(" and f.status = :status");
            params.put("status", status);
        }

        Query queryList = em.createQuery("select f " + fromQuery + sqlQuery + orderbyStr).setFirstResult(first).setMaxResults(max);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryList.setParameter(entry.getKey(), entry.getValue());
        }
        Stream<UserGroupMembershipExtensionEntity> results = queryList.getResultStream();

        Query queryCount = em.createQuery("select count(f) " + fromQuery + sqlQuery, Long.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryCount.setParameter(entry.getKey(), entry.getValue());
        }
        Long count = (Long) queryCount.getSingleResult();

        return new UserGroupMembershipExtensionRepresentationPager(results.map(x -> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList()), count);
    }

    public UserRepresentationPager searchByAdminGroups(List<String> groupids, String search, MemberStatusEnum status, Integer first, Integer max) {

        String sqlQuery = "from UserGroupMembershipExtensionEntity f join UserEntity u on f.user.id = u.id where f.group.id in (:groupids) ";
        Map<String, Object> params = new HashMap<>();
        params.put("groupids", groupids);
        if (search != null) {
            sqlQuery += " and (u.email like :search or u.firstName like :search or u.lastName like :search)";
            params.put("search", "%" + search + "%");
        }
        if (status != null) {
            sqlQuery += " and f.status = :status";
            params.put("status", status);
        }

        Query queryList = em.createQuery("select distinct(u) " + sqlQuery).setFirstResult(first).setMaxResults(max);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryList.setParameter(entry.getKey(), entry.getValue());
        }
        Stream<UserEntity> results = queryList.getResultStream();

        Query queryCount = em.createQuery("select count(distinct u.id) " + sqlQuery, Long.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryCount.setParameter(entry.getKey(), entry.getValue());
        }
        Long count = (Long) queryCount.getSingleResult();

        return new UserRepresentationPager(results.map(x -> EntityToRepresentation.toBriefRepresentation(x, realm)).collect(Collectors.toList()), count);
    }

    @Transactional
    public void suspendUser(UserModel user, UserGroupMembershipExtensionEntity member, String justification, GroupModel group) {
        member.setStatus(MemberStatusEnum.SUSPENDED);
        member.setJustification(justification);
        update(member);
        user.leaveGroup(group);
    }

    @Transactional
    public void activateUser(UserModel user, UserGroupMembershipExtensionEntity member, String justification, GroupModel group) {
        member.setStatus(MemberStatusEnum.ENABLED);
        member.setJustification(justification);
        update(member);
        user.joinGroup(group);
    }

    @Transactional
    public void createOrUpdate(GroupEnrollmentRequestEntity enrollmentEntity, KeycloakSession session, UserModel groupAdmin, MemberUserAttributeConfigurationEntity memberUserAttribute, ClientConnection clientConnection) throws UnsupportedEncodingException {
        UserGroupMembershipExtensionEntity entity = getByUserAndGroup(enrollmentEntity.getGroupEnrollmentConfiguration().getGroup().getId(), enrollmentEntity.getUser().getId());
        boolean isNotMember = entity == null || !MemberStatusEnum.ENABLED.equals(entity.getStatus());
        if (entity == null) {
            entity = new UserGroupMembershipExtensionEntity();
            entity.setId(KeycloakModelUtils.generateId());
        }
        GroupEnrollmentConfigurationEntity configuration = enrollmentEntity.getGroupEnrollmentConfiguration();
        if (isNotMember) {
            entity.setValidFrom(configuration.getValidFrom() == null || !configuration.getValidFrom().isAfter(LocalDate.now()) ? LocalDate.now() : configuration.getValidFrom());
        }
        if (configuration.getMembershipExpirationDays() != null) {
            entity.setMembershipExpiresAt(entity.getValidFrom().plusDays(configuration.getMembershipExpirationDays()));
        } else {
            entity.setMembershipExpiresAt(null);
        }
        entity.setGroup(configuration.getGroup());
        entity.setUser(enrollmentEntity.getUser());
        UserEntity editorUser = new UserEntity();
        editorUser.setId(groupAdmin.getId());
        entity.setChangedBy(editorUser);
        entity.setJustification(enrollmentEntity.getAdminJustification());
        entity.setStatus(entity.getValidFrom().isAfter(LocalDate.now()) ? MemberStatusEnum.PENDING : MemberStatusEnum.ENABLED);
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

        //only if user keep not being member of group do not do anything
        if (!isNotMember || MemberStatusEnum.ENABLED.equals(entity.getStatus())) {
            GroupModel group = realm.getGroupById(configuration.getGroup().getId());
            UserModel user = session.users().getUserById(realm, enrollmentEntity.getUser().getId());
            if (isNotMember && MemberStatusEnum.ENABLED.equals(entity.getStatus()))
                user.joinGroup(group);

            Utils.changeUserAttributeValue(user, entity, Utils.getGroupNameForMemberUserAttribute(enrollmentEntity.getGroupEnrollmentConfiguration().getGroup(), realm), memberUserAttribute);
            String eventState = isNotMember && MemberStatusEnum.ENABLED.equals(entity.getStatus()) ? Utils.GROUP_MEMBERSHIP_CREATE : Utils.GROUP_MEMBERSHIP_UPDATE;
            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , eventState, ModelToRepresentation.buildGroupPath(group), entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), entity.getMembershipExpiresAt());
        }


    }

    @Transactional
    public void createOrUpdate(GroupEnrollmentRequestRepresentation rep, KeycloakSession session, UserModel user, ClientConnection clientConnection) throws UnsupportedEncodingException {
        GroupEnrollmentConfigurationEntity configuration = groupEnrollmentConfigurationRepository.getEntity(rep.getGroupEnrollmentConfiguration().getId());
        UserGroupMembershipExtensionEntity entity = getByUserAndGroup(configuration.getGroup().getId(), user.getId());
        boolean isNotMember = entity == null || !MemberStatusEnum.ENABLED.equals(entity.getStatus());
        if (entity == null) {
            entity = new UserGroupMembershipExtensionEntity();
            entity.setId(KeycloakModelUtils.generateId());
        }
        entity.setMembershipExpiresAt(null);
        if (isNotMember) {
            entity.setValidFrom(configuration.getValidFrom() == null || !configuration.getValidFrom().isAfter(LocalDate.now()) ? LocalDate.now() : configuration.getValidFrom());
        }
        entity.setStatus(entity.getValidFrom().isAfter(LocalDate.now()) ? MemberStatusEnum.PENDING : MemberStatusEnum.ENABLED);
        entity.setGroup(configuration.getGroup());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(user.getId());
        entity.setUser(userEntity);
        entity.setChangedBy(null);
        entity.setJustification(null);
        entity.setGroupEnrollmentConfigurationId(configuration.getId());
        if (rep.getGroupRoles() != null) {
            entity.setGroupRoles(rep.getGroupRoles().stream().map(x -> groupRolesRepository.getGroupRolesByNameAndGroup(x, configuration.getGroup().getId())).filter(Objects::nonNull).collect(Collectors.toList()));
        } else {
            entity.setGroupRoles(null);
        }

        update(entity);


        //only if user keep not being member of group do not do anything
        if (!isNotMember || MemberStatusEnum.ENABLED.equals(entity.getStatus())) {
            GroupModel group = realm.getGroupById(configuration.getGroup().getId());
            if (isNotMember && MemberStatusEnum.ENABLED.equals(entity.getStatus()))
                user.joinGroup(group);

            MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            Utils.changeUserAttributeValue(user, entity, Utils.getGroupNameForMemberUserAttribute(entity.getGroup(), realm), memberUserAttribute);
            String eventState = isNotMember && MemberStatusEnum.ENABLED.equals(entity.getStatus()) ? Utils.GROUP_MEMBERSHIP_CREATE : Utils.GROUP_MEMBERSHIP_UPDATE;
            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, user.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(user.getId())
                    , eventState, ModelToRepresentation.buildGroupPath(group), rep.getGroupRoles(), entity.getMembershipExpiresAt());
        }

    }

    @Transactional
    public void update(UserGroupMembershipExtensionRepresentation rep, UserGroupMembershipExtensionEntity entity, GroupModel group, KeycloakSession session, UserModel groupAdmin, ClientConnection clientConnection) throws UnsupportedEncodingException {
        boolean isNotMember = !MemberStatusEnum.ENABLED.equals(entity.getStatus());
        entity.setMembershipExpiresAt(rep.getMembershipExpiresAt());
        entity.setValidFrom(rep.getValidFrom());
        entity.setStatus(LocalDate.now().isBefore(entity.getValidFrom()) ? MemberStatusEnum.PENDING : MemberStatusEnum.ENABLED);
        if (rep.getGroupRoles() != null) {
            entity.setGroupRoles(rep.getGroupRoles().stream().map(x -> groupRolesRepository.getGroupRolesByNameAndGroup(x, entity.getGroup().getId())).filter(Objects::nonNull).collect(Collectors.toList()));
        } else {
            entity.setGroupRoles(null);
        }

        update(entity);

        UserModel user = session.users().getUserById(realm, entity.getUser().getId());
        String eventState = Utils.GROUP_MEMBERSHIP_UPDATE;
        if (isNotMember && MemberStatusEnum.ENABLED.equals(entity.getStatus())) {
            user.joinGroup(group);
            eventState = Utils.GROUP_MEMBERSHIP_CREATE;
        } else if (!isNotMember && MemberStatusEnum.PENDING.equals(entity.getStatus())) {
            user.leaveGroup(group);
            eventState = Utils.GROUP_MEMBERSHIP_DELETE;
        }

        //only if user keep not being member of group do not do anything
        if (!isNotMember || MemberStatusEnum.ENABLED.equals(entity.getStatus())) {
            MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            Utils.changeUserAttributeValue(user, entity, Utils.getGroupNameForMemberUserAttribute(entity.getGroup(), realm), memberUserAttribute);

            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , eventState, ModelToRepresentation.buildGroupPath(group), rep.getGroupRoles(), entity.getMembershipExpiresAt());
        }
   }

    @Transactional
    public void create(GroupInvitationRepository groupInvitationRepository, GroupInvitationEntity invitationEntity, UserModel userModel, KeycloakUriInfo uri, MemberUserAttributeConfigurationEntity memberUserAttribute, ClientConnection clientConnection) {
        GroupEnrollmentConfigurationEntity configuration = invitationEntity.getGroupEnrollmentConfiguration();
        UserGroupMembershipExtensionEntity entity = new UserGroupMembershipExtensionEntity();
        entity.setId(KeycloakModelUtils.generateId());
        if (configuration.getValidFrom() == null || !configuration.getValidFrom().isAfter(LocalDate.now())) {
            entity.setValidFrom(LocalDate.now());
            entity.setStatus(MemberStatusEnum.ENABLED);
        } else {
            entity.setValidFrom(configuration.getValidFrom());
            entity.setStatus(MemberStatusEnum.PENDING);
        }
        if (configuration.getMembershipExpirationDays() != null) {
            entity.setMembershipExpiresAt(entity.getValidFrom().plusDays(configuration.getMembershipExpirationDays()));
        } else {
            entity.setMembershipExpiresAt(null);
        }
        entity.setGroup(configuration.getGroup());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userModel.getId());
        entity.setUser(userEntity);
        entity.setChangedBy(invitationEntity.getCheckAdmin());
        entity.setJustification(null);
        entity.setGroupEnrollmentConfigurationId(configuration.getId());
        if (invitationEntity.getGroupRoles() != null) {
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

        if (MemberStatusEnum.ENABLED.equals(entity.getStatus())) {
            GroupModel group = realm.getGroupById(configuration.getGroup().getId());
            userModel.joinGroup(group);

            try {
                Utils.changeUserAttributeValue(userModel, entity, Utils.getGroupNameForMemberUserAttribute(configuration.getGroup(), realm), memberUserAttribute);
            } catch (UnsupportedEncodingException e) {
                logger.warn("problem calculating user attribute value for group : " + group.getId()+ " and user :  " + userModel.getId());
            }
            LoginEventHelper.createGroupEvent(realm, session, new DummyClientConnection("127.0.0.1"), userModel, userModel.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(userModel.getId())
                    , Utils.GROUP_MEMBERSHIP_CREATE, ModelToRepresentation.buildGroupPath(group), invitationEntity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), entity.getMembershipExpiresAt());
        }
        groupInvitationRepository.deleteEntity(invitationEntity.getId());
    }

    public void deleteByGroup(String groupId) {
        em.createNamedQuery("deleteMembershipExtensionByGroup").setParameter("groupId", groupId).executeUpdate();
    }

    public void deleteByUser(String userId) {
        em.createNamedQuery("deleteMembershipExtensionByUser").setParameter("userId", userId).executeUpdate();
    }

}
