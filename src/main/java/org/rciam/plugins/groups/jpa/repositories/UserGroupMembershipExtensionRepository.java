package org.rciam.plugins.groups.jpa.repositories;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

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
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.enums.MemberStatusEnum;
import org.rciam.plugins.groups.helpers.DummyClientConnection;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.LoginEventHelper;
import org.rciam.plugins.groups.helpers.PagerParameters;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.rciam.plugins.groups.jpa.entities.GroupInvitationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupManagementEventEntity;
import org.rciam.plugins.groups.jpa.entities.GroupRolesEntity;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestRepresentation;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.rciam.plugins.groups.representations.UserRepresentationPager;
import org.rciam.plugins.groups.scheduled.AgmTimerProvider;
import org.rciam.plugins.groups.scheduled.SubgroupsExpirationDateCalculationTask;

import static org.keycloak.userprofile.UserProfileContext.USER_API;

public class UserGroupMembershipExtensionRepository extends GeneralRepository<UserGroupMembershipExtensionEntity> {

    private static final Logger logger = Logger.getLogger(UserGroupMembershipExtensionRepository.class);
    private static final String orderbyStr = " order by f.user.lastName, f.user.firstName";
    private static final String localIp = "127.0.0.1";
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

    public Long countByUserAndGroupsAndSuspended(String userId, List<String> groupIds) {
        return em.createNamedQuery("countByUserAndGroupsAndSuspended", Long.class).setParameter("userId", userId).setParameter("groupIds", groupIds).getSingleResult();
    }

    public Stream<UserGroupMembershipExtensionEntity> getActiveByUser(String userId) {
        return em.createNamedQuery("getActiveByUser").setParameter("userId", userId).getResultStream();
    }

    public Stream<UserGroupMembershipExtensionEntity> getByGroup(String groupId) {
        return em.createNamedQuery("getMembersByGroup").setParameter("groupId", groupId).getResultStream();
    }

    public Stream<UserGroupMembershipExtensionEntity> getByGroup(String userId, Set<String> groupIds) {
        return em.createNamedQuery("getByUserAndGroups", UserGroupMembershipExtensionEntity.class).setParameter("userId", userId).setParameter("groupIds", groupIds).getResultStream();
    }

    public UserGroupMembershipExtensionEntity getMinExpirationDateForUserAndGroups(String userId, List<String> groupIds, LocalDate expirationDate) {
        return expirationDate == null
                ? em.createNamedQuery("getByUserAndGroupsAndNullExpiration", UserGroupMembershipExtensionEntity.class).setParameter("userId", userId).setParameter("groupIds", groupIds).getResultStream().findFirst().orElse(null)
                : em.createNamedQuery("getByUserAndGroupsAndLessExpiration", UserGroupMembershipExtensionEntity.class).setParameter("userId", userId).setParameter("groupIds", groupIds).setParameter("expirationDate", expirationDate).getResultStream().findFirst().orElse(null);
    }

    @Transactional
    public void dailyExecutedActions() {
        GroupManagementEventEntity eventEntity = eventRepository.getEntity(Utils.eventId);
        MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session);
        if (eventEntity == null || LocalDate.now().isAfter(eventEntity.getDate())) {
            logger.info("group management daily action is executing ...");
            Stream<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getExpiredMemberships").setParameter("date", LocalDate.now()).getResultStream();
            results.forEach(entity -> {
                setRealm(session.realms().getRealm(entity.getUser().getRealmId()));
                session.getContext().setRealm(this.realm);
                MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
                customFreeMarkerEmailTemplateProvider.setSignatureMessage(memberUserAttribute.getSignatureMessage());
                String serverUrl = realm.getAttribute(Utils.KEYCLOAK_URL);
                GroupAdminRepository groupAdminRepository = new GroupAdminRepository(session, realm);
                UserModel user = session.users().getUserById(realm, entity.getUser().getId());
                GroupModel group = realm.getGroupById(entity.getGroup().getId());
                logger.info(user.getFirstName() + " " + user.getFirstName() + " is removing from being member of group " + group.getName());
                List<String> subgroupsPaths = deleteMember(entity, group, user, new DummyClientConnection(localIp), null, memberUserAttribute, false).stream().map(ModelToRepresentation::buildGroupPath).collect(Collectors.toList());

                customFreeMarkerEmailTemplateProvider.setRealm(realm);
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    customFreeMarkerEmailTemplateProvider.sendExpiredGroupMemberEmailToUser(ModelToRepresentation.buildGroupPath(group), group.getId(), subgroupsPaths, serverUrl);
                } catch (EmailException e) {
                    logger.warn("problem sending email to user " + user.getFirstName() + " " + user.getLastName());
                }
                groupAdminRepository.getAllAdminIdsGroupUsers(group).map(id -> session.users().getUserById(realm, id)).filter(Objects::nonNull).forEach(admin -> {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    try {
                        customFreeMarkerEmailTemplateProvider.sendExpiredGroupMemberEmailToAdmin(user, group.getName(), subgroupsPaths);
                    } catch (EmailException e) {
                        logger.warn("problem sending email to group admin " + admin.getFirstName() + " " + admin.getLastName());
                    }

                });
            });

            Stream<UserGroupMembershipExtensionEntity> pendingMembers = em.createNamedQuery("getMembershipsByStatusAndValidFrom").setParameter("status", MemberStatusEnum.PENDING).setParameter("date", LocalDate.now()).getResultStream();
            pendingMembers.forEach(member -> {
                member.setStatus(MemberStatusEnum.ENABLED);
                update(member);
                setRealm(session.realms().getRealm(member.getUser().getRealmId()));
                session.getContext().setRealm(this.realm);
                GroupModel group = realm.getGroupById(member.getGroup().getId());
                UserModel userModel = session.users().getUserById(realm, member.getUser().getId());
                userModel.joinGroup(group);
                MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
                try {
                    Utils.changeUserAttributeValue(userModel, member, Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm), memberUserAttribute, session);
                } catch (UnsupportedEncodingException e) {
                    logger.warn("problem calculating user attribute value for group : " + member.getGroup().getId() + " and user :  " + member.getUser().getId());
                }
                LoginEventHelper.createGroupEvent(realm, session, new DummyClientConnection(localIp), userModel, member.getChangedBy() != null ? member.getChangedBy().getId() : userModel.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(userModel.getId())
                        , Utils.GROUP_MEMBERSHIP_CREATE, ModelToRepresentation.buildGroupPath(group), member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), member.getMembershipExpiresAt());

                AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
                timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new SubgroupsExpirationDateCalculationTask(realm.getId(), userModel.getId(), group.getId(), member.getMembershipExpiresAt()), 100),  100, "SubgroupsExpirationDateCalculationTask_"+ member.getId());
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
    public Set<GroupModel> deleteMember(UserGroupMembershipExtensionEntity member, GroupModel group, UserModel user, ClientConnection clientConnection, String actionUserId, MemberUserAttributeConfigurationEntity memberUserAttribute, boolean isRealmRemove) {
        String groupName = "";
        if (isRealmRemove) {
            deleteEntity(member);
            return new HashSet<>();
        } else {
            logger.info(user.getFirstName() + " " + user.getFirstName() + " is removing from being member of group " + group.getName());
            List<String> roleNames = member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList());
            try {
                groupName = Utils.getGroupNameForMemberUserAttribute(member.getGroup(), realm);
            } catch (UnsupportedEncodingException e) {
                logger.warn("problem calculating user attribute value for group : " + group.getId() + " and user :  " + user.getId());
            }

            deleteEntity(member.getId());
            user.leaveGroup(group);
            clearUserAttribute(groupName, user, memberUserAttribute);
            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, actionUserId
                    , Utils.GROUP_MEMBERSHIP_DELETE, ModelToRepresentation.buildGroupPath(group), roleNames, LocalDate.now());

            //delete also subgroup members if exists
            Set<GroupModel> subgroups = Utils.getAllSubgroups(group);
            if (!subgroups.isEmpty()) {
                getByGroup(user.getId(),subgroups.stream().map(GroupModel::getId).collect(Collectors.toSet())).forEach(memberEntity -> {
                    try {
                        List<String> roleSubgroupNames = member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList());
                        String groupId = memberEntity.getGroup().getId();
                        String groupNameCh = Utils.getGroupNameForMemberUserAttribute(memberEntity.getGroup(), realm);
                        em.remove(memberEntity);
                        GroupModel groupChild = subgroups.stream().filter(x -> groupId.equals(x.getId())).findFirst().get();
                        user.leaveGroup(groupChild);
                        clearUserAttribute(groupNameCh, user, memberUserAttribute);
                        em.flush();
                        LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, actionUserId
                                , Utils.GROUP_MEMBERSHIP_DELETE, ModelToRepresentation.buildGroupPath(groupChild), roleSubgroupNames, LocalDate.now());
                    } catch (UnsupportedEncodingException e) {
                        logger.warn("problem calculating user attribute value for group : " + group.getId() + " and user :  " + user.getId());
                    }

                });
            }
            return subgroups;
        }
    }

    @Transactional
    public List<String> suspendUser(UserModel user, UserGroupMembershipExtensionEntity member, String justification, GroupModel group, MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository) {
        member.setStatus(MemberStatusEnum.SUSPENDED);
        member.setJustification(justification);
        update(member);
        user.leaveGroup(group);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        try {
            clearUserAttribute(Utils.getGroupNameForMemberUserAttribute(group), user, memberUserAttribute);
        } catch (UnsupportedEncodingException e) {
            logger.warn("problem calculating user attribute value for group : " + group.getId() + " and user :  " + user.getId());
        }

        //suspend also subgroup members if exists
        Set<String> subgroupsIds = Utils.getAllSubgroupsIds(group);
        if (!subgroupsIds.isEmpty()) {
            List<String> results = new ArrayList<>();
            getByGroup(user.getId(), subgroupsIds).forEach(memberEntity -> {
                try {
                    memberEntity.setStatus(MemberStatusEnum.SUSPENDED);
                    memberEntity.setJustification(justification);
                    update(memberEntity);
                    GroupModel groupChild = realm.getGroupById(memberEntity.getGroup().getId());
                    results.add(ModelToRepresentation.buildGroupPath(groupChild));
                    user.leaveGroup(groupChild);
                    clearUserAttribute(Utils.getGroupNameForMemberUserAttribute(groupChild), user, memberUserAttribute);
                } catch (UnsupportedEncodingException e) {
                    logger.warn("problem calculating user attribute value for group : " + group.getId() + " and user :  " + user.getId());
                }
            });
            return results;
        }
        return new ArrayList<>();
    }

    private void clearUserAttribute(String groupName, UserModel user, MemberUserAttributeConfigurationEntity memberUserAttribute) {
        List<String> memberUserAttributeValues = user.getAttributeStream(memberUserAttribute.getUserAttribute()).collect(Collectors.toList());
        memberUserAttributeValues.removeIf(x -> Utils.removeMemberUserAttributeCondition(x, memberUserAttribute.getUrnNamespace(), groupName));
        Map<String, List<String>> attributes = user.getAttributes();
        attributes.put(memberUserAttribute.getUserAttribute(), memberUserAttributeValues);
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(USER_API, attributes, user);
        profile.update(true);
    }


    private void weeklyTaskExecution(CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realmModel -> {
            MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realmModel.getId());
            customFreeMarkerEmailTemplateProvider.setSignatureMessage(memberUserAttribute.getSignatureMessage());
            customFreeMarkerEmailTemplateProvider.setRealm(realmModel);
            String serverUrl = realmModel.getAttribute(Utils.KEYCLOAK_URL);
            session.groups().getGroupsStream(realmModel).forEach(group -> {
                Integer dateBeforeNotification = group.getFirstAttribute(Utils.expirationNotificationPeriod) != null ? Integer.valueOf(group.getFirstAttribute(Utils.expirationNotificationPeriod)) : realmModel.getAttribute(Utils.expirationNotificationPeriod, 21);
                //find group membership that expires in less days than dateBeforeNotification (at the end of this week)
                Stream<UserGroupMembershipExtensionEntity> results = em.createNamedQuery("getExpiredMembershipsByGroup").setParameter("groupId", group.getId()).setParameter("date", LocalDate.now().plusDays(dateBeforeNotification + 6)).getResultStream();
                results.forEach(entity -> {
                    UserModel user = session.users().getUserById(realmModel, entity.getUser().getId());
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    try {
                        customFreeMarkerEmailTemplateProvider.sendExpiredGroupMembershipNotification(ModelToRepresentation.buildGroupPath(group), entity.getMembershipExpiresAt().format(Utils.dateFormatter), group.getId(), serverUrl);
                    } catch (EmailException e) {
                        e.printStackTrace();
                        logger.info("problem sending email to user  " + user.getFirstName() + " " + user.getLastName());
                    }
                });
            });
        });
    }

    public UserGroupMembershipExtensionRepresentationPager userpager(String userId, String search, PagerParameters pagerParameters) {

        String sqlQuery = "from UserGroupMembershipExtensionEntity f ";
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        if (search != null) {
            sqlQuery += "join GroupEntity g on f.group.id = g.id where f.user.id = :userId and f.status = 'ENABLED' and g.name like :search";
            params.put("search", "%" + search + "%");
        } else {
            sqlQuery += "where f.user.id = :userId and f.status = 'ENABLED'";
        }

        Query queryList = em.createQuery("select f " + sqlQuery + " order by f." + pagerParameters.getOrder() + " " + pagerParameters.getOrderType()).setFirstResult(pagerParameters.getFirst()).setMaxResults(pagerParameters.getMax());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryList.setParameter(entry.getKey(), entry.getValue());
        }
        Stream<UserGroupMembershipExtensionEntity> results = queryList.getResultStream();

        Query queryCount = em.createQuery("select count(f) " + sqlQuery, Long.class);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryCount.setParameter(entry.getKey(), entry.getValue());
        }
        Long count = (Long) queryCount.getSingleResult();

        return new UserGroupMembershipExtensionRepresentationPager(results.map(x -> EntityToRepresentation.toRepresentation(x, realm, true)).collect(Collectors.toList()), count);
    }

    public UserGroupMembershipExtensionRepresentationPager searchByGroupAndSubGroups(String groupId, Set<String> groupIdList, String search, MemberStatusEnum status, String role, Integer first, Integer max) {


        StringBuilder fromQuery = new StringBuilder("from UserGroupMembershipExtensionEntity f");
        StringBuilder sqlQuery = new StringBuilder(" where f.group.id in ( :groupIdList)");
        Map<String, Object> params = new HashMap<>();
        params.put("groupIdList", groupIdList);
        if (role != null) {
            fromQuery.append(" join f.groupRoles r ");
            sqlQuery.append(" and r.name like :role");
            params.put("role", "%" + role + "%");
        }
        if (search != null) {
            fromQuery.append(", UserEntity u");
            sqlQuery.append(" and f.user.id = u.id and (lower(u.email) like :search or lower(u.firstName) like :search or lower(u.lastName) like :search or lower(u.username) like :search)");
            params.put("search", "%" + search.toLowerCase() + "%");
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

        return new UserGroupMembershipExtensionRepresentationPager(results.map(x -> EntityToRepresentation.toRepresentation(x, realm, groupId)).collect(Collectors.toList()), count);
    }

    public UserRepresentationPager searchByAdminGroups(List<String> groupids, String search, boolean serviceAccountClientLink, Integer first, Integer max) {

        String sqlQuery = "from UserEntity u where ( u.id in ( select distinct(f.user.id) from  UserGroupMembershipExtensionEntity f where f.group.id in (:groupids)) or u.id in ( select distinct(g.user.id) from GroupAdminEntity g where g.group.id in (:groupids)) )";
        Map<String, Object> params = new HashMap<>();
        params.put("groupids", groupids);
        if (search != null) {
            sqlQuery += " and (u.email like :search or u.firstName like :search or u.lastName like :search)";
            params.put("search", "%" + search + "%");
        }
        if (!serviceAccountClientLink ){
            sqlQuery += " and u.serviceAccountClientLink is null";
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
    public List<String> reActivateUser(UserModel user, UserGroupMembershipExtensionEntity member, String justification, GroupModel group, MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository) {
        member.setStatus(MemberStatusEnum.ENABLED);
        member.setJustification(justification);
        update(member);
        user.joinGroup(group);

        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        try {
            Utils.changeUserAttributeValue(user, member, Utils.getGroupNameForMemberUserAttribute(group), memberUserAttribute, session);
        } catch (UnsupportedEncodingException e) {
            logger.warn("problem calculating user attribute value for group : " + group.getId() + " and user :  " + user.getId());
        }

        //reactivate also subgroup members if exists
        Set<String> subgroupsIds = Utils.getAllSubgroupsIds(group);
        if (!subgroupsIds.isEmpty()) {
            List<String> results = new ArrayList<>();
            getByGroup(user.getId(), subgroupsIds).forEach(memberEntity -> {
                try {
                    memberEntity.setStatus(MemberStatusEnum.ENABLED);
                    memberEntity.setJustification(justification);
                    update(memberEntity);
                    GroupModel groupChild = realm.getGroupById(memberEntity.getGroup().getId());
                    results.add(ModelToRepresentation.buildGroupPath(groupChild));
                    user.joinGroup(groupChild);
                    Utils.changeUserAttributeValue(user, memberEntity, Utils.getGroupNameForMemberUserAttribute(groupChild), memberUserAttribute, session);
                } catch (UnsupportedEncodingException e) {
                    logger.warn("problem calculating user attribute value for group : " + memberEntity.getGroup().getId() + " and user :  " + user.getId());
                }
            });
            return results;
        }
        return new ArrayList<>();
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
        if (configuration.getMembershipExpirationDays() != null && entity.getValidFrom().isAfter(LocalDate.now())) {
            entity.setMembershipExpiresAt(entity.getValidFrom().plusDays(configuration.getMembershipExpirationDays()));
        } else if (configuration.getMembershipExpirationDays() != null) {
            entity.setMembershipExpiresAt(LocalDate.now().plusDays(configuration.getMembershipExpirationDays()));
        }else {
            entity.setMembershipExpiresAt(null);
        }
        entity.setGroup(configuration.getGroup());
        entity.setUser(enrollmentEntity.getUser());
        setEffectiveGroupMembershipExpiresAt(entity);
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
            if (isNotMember && MemberStatusEnum.ENABLED.equals(entity.getStatus())) {
                user.joinGroup(group);
            }

            Utils.changeUserAttributeValue(user, entity, Utils.getGroupNameForMemberUserAttribute(group), memberUserAttribute, session);
            String eventState = isNotMember && MemberStatusEnum.ENABLED.equals(entity.getStatus()) ? Utils.GROUP_MEMBERSHIP_CREATE : Utils.GROUP_MEMBERSHIP_UPDATE;
            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , eventState, ModelToRepresentation.buildGroupPath(group), entity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), entity.getMembershipExpiresAt());

            if (MemberStatusEnum.ENABLED.equals(entity.getStatus()) && group.getSubGroupsStream().count() > 0) {
                AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
                timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new SubgroupsExpirationDateCalculationTask(realm.getId(), user.getId(), group.getId(), entity.getMembershipExpiresAt()), 100),  100, "SubgroupsExpirationDateCalculationTask_"+ entity.getId());
            }
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
        if (isNotMember) {
            entity.setValidFrom(configuration.getValidFrom() == null || !configuration.getValidFrom().isAfter(LocalDate.now()) ? LocalDate.now() : configuration.getValidFrom());
        }
        if (configuration.getMembershipExpirationDays() != null) {
            entity.setMembershipExpiresAt(entity.getValidFrom().plusDays(configuration.getMembershipExpirationDays()));
        } else {
            entity.setMembershipExpiresAt(null);
        }
        entity.setStatus(entity.getValidFrom().isAfter(LocalDate.now()) ? MemberStatusEnum.PENDING : MemberStatusEnum.ENABLED);
        entity.setGroup(configuration.getGroup());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(user.getId());
        entity.setUser(userEntity);
        setEffectiveGroupMembershipExpiresAt(entity);
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
            Utils.changeUserAttributeValue(user, entity, Utils.getGroupNameForMemberUserAttribute(group), memberUserAttribute, session);
            String eventState = isNotMember && MemberStatusEnum.ENABLED.equals(entity.getStatus()) ? Utils.GROUP_MEMBERSHIP_CREATE : Utils.GROUP_MEMBERSHIP_UPDATE;
            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, user.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(user.getId())
                    , eventState, ModelToRepresentation.buildGroupPath(group), rep.getGroupRoles(), entity.getMembershipExpiresAt());

            if (MemberStatusEnum.ENABLED.equals(entity.getStatus()) && group.getSubGroupsStream().count() > 0) {
                AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
                timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new SubgroupsExpirationDateCalculationTask(realm.getId(), user.getId(), group.getId(), entity.getMembershipExpiresAt()), 100),  100, "SubgroupsExpirationDateCalculationTask_"+ entity.getId());
            }
        }

    }

    @Transactional
    public void update(UserGroupMembershipExtensionRepresentation rep, UserGroupMembershipExtensionEntity entity, GroupModel group, KeycloakSession session, UserModel groupAdmin, ClientConnection clientConnection) throws UnsupportedEncodingException {
        boolean isNotMember = !MemberStatusEnum.ENABLED.equals(entity.getStatus());
        boolean isMembershipExpiresAtChanges = ( rep.getMembershipExpiresAt() == null && entity.getMembershipExpiresAt() == null) || (rep.getMembershipExpiresAt() != null && rep.getMembershipExpiresAt().equals(entity.getMembershipExpiresAt()));
        entity.setValidFrom(rep.getValidFrom());
        if (isMembershipExpiresAtChanges){
            //only if membershipExpiresAt has been changed
            entity.setMembershipExpiresAt(rep.getMembershipExpiresAt());
            setEffectiveGroupMembershipExpiresAt(entity);
        }
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
        }

        //only if user keep not being member of group do not do anything
        if (!isNotMember || MemberStatusEnum.ENABLED.equals(entity.getStatus())) {
            MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            Utils.changeUserAttributeValue(user, entity, Utils.getGroupNameForMemberUserAttribute(group), memberUserAttribute, session);

            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , eventState, ModelToRepresentation.buildGroupPath(group), rep.getGroupRoles(), entity.getMembershipExpiresAt());
        }

        if (isMembershipExpiresAtChanges && MemberStatusEnum.ENABLED.equals(entity.getStatus()) && group.getSubGroupsStream().count() > 0) {
            AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
            timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new SubgroupsExpirationDateCalculationTask(realm.getId(), entity.getUser().getId(), group.getId(), entity.getMembershipExpiresAt()), 100),  100, "SubgroupsExpirationDateCalculationTask_"+ entity.getId());
        }
    }

    @Transactional
    public UserGroupMembershipExtensionEntity create(GroupInvitationEntity invitationEntity, UserModel userModel, KeycloakUriInfo uri, MemberUserAttributeConfigurationEntity memberUserAttribute, ClientConnection clientConnection) {
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
        setEffectiveGroupMembershipExpiresAt(entity);
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
                Utils.changeUserAttributeValue(userModel, entity, Utils.getGroupNameForMemberUserAttribute(group), memberUserAttribute, session);
            } catch (UnsupportedEncodingException e) {
                logger.warn("problem calculating user attribute value for group : " + group.getId() + " and user :  " + userModel.getId());
            }
            LoginEventHelper.createGroupEvent(realm, session, new DummyClientConnection(localIp), userModel, userModel.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(userModel.getId())
                    , Utils.GROUP_MEMBERSHIP_CREATE, ModelToRepresentation.buildGroupPath(group), invitationEntity.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toList()), entity.getMembershipExpiresAt());

            if (group.getSubGroupsStream().count() > 0) {
                AgmTimerProvider timer = session.getProvider(AgmTimerProvider.class);
                timer.scheduleOnce(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new SubgroupsExpirationDateCalculationTask(realm.getId(), userModel.getId(), group.getId(), entity.getMembershipExpiresAt()), 100),  100, "SubgroupsExpirationDateCalculationTask_"+ entity.getId());
            }
        }

        return entity;
    }

    @Transactional
    public void migrateEffectiveExpiresAt(){
        em.createNamedQuery("getAllMembers", UserGroupMembershipExtensionEntity.class).getResultStream().forEach(member -> {
            setEffectiveGroupMembershipExpiresAtForMigration(member);
            em.merge(member);
        });
    }

    public void deleteByUser(String userId) {
        em.createNamedQuery("deleteMembershipExtensionByUser").setParameter("userId", userId).executeUpdate();
    }

    private void setEffectiveGroupMembershipExpiresAt(UserGroupMembershipExtensionEntity member) {
        if (! member.getGroup().getParentId().trim().isEmpty()) {
            GroupModel group = session.groups().getGroupById(realm, member.getGroup().getId());
            List<String> parentsIds = Utils.findParentGroupIds(group);
            UserGroupMembershipExtensionEntity effectiveMember = getMinExpirationDateForUserAndGroups(member.getUser().getId(), parentsIds, member.getMembershipExpiresAt());
            if (effectiveMember != null) {
                member.setEffectiveMembershipExpiresAt(effectiveMember.getMembershipExpiresAt());
                member.setEffectiveGroupId(effectiveMember.getGroup().getId());
            } else {
                member.setEffectiveMembershipExpiresAt(member.getMembershipExpiresAt());
                member.setEffectiveGroupId(null);
            }
        } else {
            member.setEffectiveMembershipExpiresAt(member.getMembershipExpiresAt());
        }
    }

    private void setEffectiveGroupMembershipExpiresAtForMigration(UserGroupMembershipExtensionEntity member) {
        if (! member.getGroup().getParentId().trim().isEmpty()) {
            RealmModel realmModel = session.realms().getRealm(member.getGroup().getRealm());
            GroupModel group = session.groups().getGroupById(realmModel, member.getGroup().getId());
            List<String> parentsIds = Utils.findParentGroupIds(group);
            UserGroupMembershipExtensionEntity effectiveMember = getMinExpirationDateForUserAndGroups(member.getUser().getId(), parentsIds, member.getMembershipExpiresAt());
            if (effectiveMember != null) {
                member.setEffectiveMembershipExpiresAt(effectiveMember.getMembershipExpiresAt());
                member.setEffectiveGroupId(effectiveMember.getGroup().getId());
            } else {
                member.setEffectiveMembershipExpiresAt(member.getMembershipExpiresAt());
            }
        } else {
            member.setEffectiveMembershipExpiresAt(member.getMembershipExpiresAt());
        }
    }



}
