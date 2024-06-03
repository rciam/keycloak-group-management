package org.rciam.plugins.groups.jpa.repositories;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.TypedQuery;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.rciam.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.PagerParameters;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestPager;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestRepresentation;

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

    public GroupEnrollmentRequestEntity create(GroupEnrollmentRequestRepresentation rep, UserModel user, GroupEnrollmentConfigurationEntity configuration, boolean isPending, RealmModel realm) {
        GroupEnrollmentRequestEntity entity = new GroupEnrollmentRequestEntity();
        entity.setId(KeycloakModelUtils.generateId());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(user.getId());
        entity.setUser(userEntity);
        String userIdentifier = realm.getAttribute(Utils.USER_IDENTIFIER_FOR_ENROLLMENT) != null ? realm.getAttribute(Utils.USER_IDENTIFIER_FOR_ENROLLMENT) : Utils.DEFAULT_USER_IDENTIFIER_FOR_ENROLLMENT;
        entity.setUserName(user.getFirstName()+" "+user.getLastName());
        entity.setUserEmail(user.getEmail());
        entity.setUserIdentifier("username".equals(userIdentifier)? user.getUsername() : user.getAttributeStream(userIdentifier).collect(Collectors.joining(",")));
        entity.setGroupEnrollmentConfiguration(configuration);
        entity.setComments(rep.getComments());
        entity.setStatus(isPending ? EnrollmentRequestStatusEnum.PENDING_APPROVAL : EnrollmentRequestStatusEnum.NO_APPROVAL);
        entity.setSubmittedDate(LocalDateTime.now());
        if (!isPending)
            entity.setApprovedDate(LocalDateTime.now());
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

    public GroupEnrollmentRequestPager groupEnrollmentPager(String userId, String groupId, String groupName, EnrollmentRequestStatusEnum status, PagerParameters pagerParameters) {
        Set<String> groupIds = new HashSet<>();
        if (groupId != null) {
            groupIds =  Utils.getGroupIdsWithSubgroups(realm.getGroupById(groupId)).collect(Collectors.toSet());
        } else  if (groupName != null) {
            em.createQuery("select f.id from GroupEntity f where  lower(f.name) like :groupName", String.class).setParameter("groupName", "%" + groupName.toLowerCase() + "%").getResultStream().
                    map(realm::getGroupById).flatMap(Utils::getGroupIdsWithSubgroups).collect(Collectors.toSet());
        }
        StringBuilder sqlQueryMain = new StringBuilder("from GroupEnrollmentRequestEntity f");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        if (groupIds.isEmpty()) {
            sqlQueryMain.append(" where f.user.id = :userId");
        } else {
            sqlQueryMain.append(" join f.groupEnrollmentConfiguration c where f.user.id = :userId and c.group.id in (:groupIds)");
            parameters.put("groupIds", groupIds);
        }
        if (status != null) {
            sqlQueryMain.append(" and f.status = :status");
            parameters.put("status", status);
        }

        TypedQuery<GroupEnrollmentRequestEntity> query = em.createQuery("select f " + sqlQueryMain.toString()+ " order by f." + pagerParameters.getOrder() + " " + pagerParameters.getOrderType(), GroupEnrollmentRequestEntity.class);
        TypedQuery<Long> queryCount = em.createQuery("select count(f) " + sqlQueryMain.toString(), Long.class);
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
            queryCount.setParameter(e.getKey(), e.getValue());
        }
        List<GroupEnrollmentRequestRepresentation> enrollments = query.setFirstResult(pagerParameters.getFirst()).setMaxResults(pagerParameters.getMax()).getResultStream().map(x -> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList());
        return new GroupEnrollmentRequestPager(enrollments, queryCount.getSingleResult());

    }

    public GroupEnrollmentRequestPager groupAdminEnrollmentPager(List<String> groupIds, String userSearch, EnrollmentRequestStatusEnum status, PagerParameters pagerParameters) {
        StringBuilder sqlQueryMain = new StringBuilder("from GroupEnrollmentRequestEntity f join f.groupEnrollmentConfiguration c join c.group g");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("groupIds", groupIds);
        if (userSearch == null) {
            sqlQueryMain.append(" where g.id in (:groupIds)");
        } else {
            sqlQueryMain.append(" join f.user u where g.id in (:groupIds) and (lower(u.firstName) like :userSearch or lower(u.lastName) like :userSearch) ");
            parameters.put("userSearch", "%" + userSearch.toLowerCase() + "%");
        }
        if (status != null) {
            sqlQueryMain.append(" and f.status = :status");
            parameters.put("status", status);
        }

        TypedQuery<GroupEnrollmentRequestEntity> query = em.createQuery("select f " + sqlQueryMain.toString()+ " order by f." + pagerParameters.getOrder() + " " + pagerParameters.getOrderType(), GroupEnrollmentRequestEntity.class);
        TypedQuery<Long> queryCount = em.createQuery("select count(f) " + sqlQueryMain.toString(), Long.class);
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
            queryCount.setParameter(e.getKey(), e.getValue());
        }
        List<GroupEnrollmentRequestRepresentation> enrollments = query.setFirstResult(pagerParameters.getFirst()).setMaxResults(pagerParameters.getMax()).getResultStream().map(x -> EntityToRepresentation.toRepresentation(x, realm)).collect(Collectors.toList());
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

    public Stream<GroupEnrollmentRequestEntity> getRequestsByConfiguration(String configurationId) {
        return em.createNamedQuery("getRequestsByConfiguration").setParameter("configurationId", configurationId).getResultStream();
    }

}
