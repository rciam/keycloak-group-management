package org.rciam.plugins.groups.jpa.repositories;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.TypedQuery;

import org.jboss.logging.Logger;
import org.keycloak.events.Details;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.JsonSerialization;
import org.rciam.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.PagerParameters;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.rciam.plugins.groups.representations.AuthnAuthorityRepresentation;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestPager;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestRepresentation;

public class GroupEnrollmentRequestRepository extends GeneralRepository<GroupEnrollmentRequestEntity> {

    private static final Logger logger = Logger.getLogger(GroupEnrollmentRequestRepository.class);
    private final GroupRolesRepository groupRolesRepository;
    private static final String IDENTITY_PROVIDER_AUTHN_AUTHORITIES = "identity_provider_authnAuthorities";
    private static final String IDENTITY_PROVIDER_ID = "identity_provider_id";

    public GroupEnrollmentRequestRepository(KeycloakSession session, RealmModel realm, GroupRolesRepository groupRolesRepository) {
        super(session, realm);
        this.groupRolesRepository = groupRolesRepository;
    }

    @Override
    protected Class<GroupEnrollmentRequestEntity> getTClass() {
        return GroupEnrollmentRequestEntity.class;
    }

    public GroupEnrollmentRequestEntity create(GroupEnrollmentRequestRepresentation rep, UserModel user, GroupEnrollmentConfigurationEntity configuration, boolean isPending, RealmModel realm, UserSessionModel userSession) {
        GroupEnrollmentRequestEntity entity = new GroupEnrollmentRequestEntity();
        entity.setId(KeycloakModelUtils.generateId());
        UserEntity userEntity = new UserEntity();
        userEntity.setId(user.getId());
        entity.setUser(userEntity);
        String userIdentifier = realm.getAttribute(Utils.USER_IDENTIFIER_FOR_ENROLLMENT) != null ? realm.getAttribute(Utils.USER_IDENTIFIER_FOR_ENROLLMENT) : Utils.DEFAULT_USER_IDENTIFIER_FOR_ENROLLMENT;
        String assurance = realm.getAttribute(Utils.USER_ASSURANCE_FOR_ENROLLMENT) != null ? realm.getAttribute(Utils.USER_ASSURANCE_FOR_ENROLLMENT) : Utils.DEFAULT_USER_ASSURANCE_FOR_ENROLLMENT;
        entity.setUserFirstName(user.getFirstName());
        entity.setUserLastName(user.getLastName());
        entity.setUserEmail(user.getEmail());
        entity.setUserIdentifier("username".equals(userIdentifier)? user.getUsername() : user.getAttributeStream(userIdentifier).collect(Collectors.joining(",")));
        entity.setUserAssurance(user.getAttributeStream(assurance).collect(Collectors.toSet()));
        String idpAlias = userSession.getNote(Details.IDENTITY_PROVIDER);
        if (idpAlias != null) {
            try {
                entity.setUserAuthnAuthorities(getAuthnAuthorities(userSession, idpAlias));
            } catch (IOException e) {
                logger.warn("Problem setting userAuthnAuthorities for user enrollment request");
                e.printStackTrace();
            }
        }
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

    private String getAuthnAuthorities(UserSessionModel userSession, String idpAlias) throws IOException {
        LinkedList<AuthnAuthorityRepresentation> authnAuthorities = new LinkedList<>();
        String previousAauthnAuthorities = userSession.getNote(IDENTITY_PROVIDER_AUTHN_AUTHORITIES);
        RealmModel realm = userSession.getRealm();
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(idpAlias);
        //add first authn autohrities
        authnAuthorities.add(new AuthnAuthorityRepresentation(userSession.getNote(IDENTITY_PROVIDER_ID), getIdPName(idp)));
        if (previousAauthnAuthorities != null) {
                authnAuthorities.addAll(JsonSerialization.readValue(previousAauthnAuthorities, new TypeReference<LinkedList<AuthnAuthorityRepresentation>>() {
                }));
        }
        return JsonSerialization.writeValueAsString(authnAuthorities);
    }

    private String getIdPName(IdentityProviderModel idp) {
        return idp.getDisplayName() != null ? idp.getDisplayName() : idp.getAlias();
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

        TypedQuery<GroupEnrollmentRequestEntity> query = em.createQuery("select f " + sqlQueryMain.toString()+ " order by f." + pagerParameters.getOrder().get(0) + " " + pagerParameters.getOrderType(), GroupEnrollmentRequestEntity.class);
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

        TypedQuery<GroupEnrollmentRequestEntity> query = em.createQuery("select f " + sqlQueryMain.toString()+ " order by f." + pagerParameters.getOrder().get(0) + " " + pagerParameters.getOrderType(), GroupEnrollmentRequestEntity.class);
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
