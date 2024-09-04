package org.rciam.plugins.groups.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;

import static org.keycloak.userprofile.UserProfileContext.USER_API;

public class Utils {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String dateToStringFormat = "yyyy-MM-dd";

    public static final String dateTimeToStringFormat = "yyyy-MM-dd HH:mm:ss";
    public static final String expirationNotificationPeriod = "expiration-notification-period";
    public static final String invitationExpirationPeriod = "invitation-expiration-period";
    public static final String defaultGroupRole = "member";

    public static final String eventId = "1";

    private static final String chronJobUserId = "chron-job";
    public static final String colon = ":";
    private static final String space = " ";
    private static final String sharp = "#";
    public static final String groupStr = ":group:";
    public static final String roleStr = "role=";
    public static final String EVENT_GROUP = "group";
    public static final String EVENT_ACTION_USER = "actionUser";
    public static final String EVENT_ROLES = "roles";
    public static final String EVENT_MEMBERSHIP_EXPIRATION = "membership expiration";
    public static final String VO_PERSON_ID ="voPersonID";
    public static final String KEYCLOAK_URL = "keycloakUrl";
    public static final String DEFAULT_CONFIGURATION_NAME = "defaultConfiguration";
    public static final String GROUP_MEMBERSHIP_CREATE = "GROUP_MEMBERSHIP_CREATE";
    public static final String GROUP_MEMBERSHIP_UPDATE = "GROUP_MEMBERSHIP_UPDATE";
    public static final String GROUP_MEMBERSHIP_DELETE = "GROUP_MEMBERSHIP_DELETE";
    public static final String GROUP_MEMBERSHIP_SUSPEND = "GROUP_MEMBERSHIP_SUSPEND";
    public static final String NO_FOUND_GROUP_CONFIGURATION = "Could not find this group configuration";
    public static final String DEFAULT_GROUP_ROLE_NAME = "manage-groups";
    public static final String DESCRIPTION = "description";
    public static final String USER_ASSURANCE_FOR_ENROLLMENT = "userAssuranceForEnrollment";
    public static final String DEFAULT_USER_ASSURANCE_FOR_ENROLLMENT = "assurance";
    public static final String USER_IDENTIFIER_FOR_ENROLLMENT = "userIdentifierForEnrollment";
    public static final String DEFAULT_USER_IDENTIFIER_FOR_ENROLLMENT = "username";

    public static UserAdapter getDummyUser(String email, String firstName, String lastName) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email, true);
        userEntity.setFirstName(firstName);
        userEntity.setLastName(lastName);
        return new UserAdapter(null, null, null, userEntity);
    }

    public static UserAdapter getChronJobUser() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(chronJobUserId);
        return new UserAdapter(null, null, null, userEntity);
    }

    public static String createMemberUserAttribute(String groupName, String role, String namespace, String authority) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(namespace);
        sb.append(groupStr);
        sb.append(groupName);
        if (role != null) {
            sb.append(colon).append(roleStr).append(encode(role));
        }
        if (authority != null) {
            sb.append(sharp).append(authority);
        }
        return sb.toString();
    }

    public static String getGroupNameForMemberUserAttribute(GroupEntity group, RealmModel realm) throws UnsupportedEncodingException {
        return addParentGroupName(encode(group.getName()), realm.getGroupById(group.getParentId()));
    }

    public static String getGroupNameForMemberUserAttribute(GroupModel group) throws UnsupportedEncodingException {
        return addParentGroupName(encode(group.getName()), group.getParent());
    }

    private static String addParentGroupName(String groupName, GroupModel parent) throws UnsupportedEncodingException {
        while (parent != null) {
            groupName = encode(parent.getName()) + colon + groupName;
            parent = parent.getParent();
        }
        return groupName;
    }

    public static List<String> findParentGroupIds(GroupModel group) {
        List<String> parentIds =  new ArrayList<>();
        while (group.getParent() != null) {
            parentIds.add(group.getParentId());
            group = group.getParent();
        }
        return parentIds;
    }

    private static String encode(String x) throws UnsupportedEncodingException {
        return URLEncoder.encode(x.replace(space, "%20"), StandardCharsets.UTF_8.toString()).replace("%2520", "%20");
    }

    public static Response addGroupChild(GroupRepresentation rep, RealmModel realm, GroupModel group, KeycloakSession session, AdminEventBuilder adminEvent, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, GroupRolesRepository groupRolesRepository) {

        //GroupResource addGroupChild method except return Response
        adminEvent.resource(ResourceType.GROUP);
        String groupName = rep.getName();
        if (ObjectUtil.isBlank(groupName)) {
            throw new ErrorResponseException("Group name is missing", "Group name is missing", Response.Status.BAD_REQUEST);
        }

        GroupModel child = null;
        if (rep.getId() != null) {
            child = realm.getGroupById(rep.getId());
            if (child == null) {
                throw new NotFoundException("Could not find child by id");
            }
            realm.moveGroup(child, group);
            adminEvent.operation(OperationType.UPDATE);
        } else {
            if (group.getSubGroupsStream().anyMatch(g -> groupName.equals(g.getName())))
                throw new ErrorResponseException("This subgroup has already existed", "This subgroup has already existed", Response.Status.BAD_REQUEST);
            child = realm.createGroup(groupName, group);
            GroupResource.updateGroup(rep, child, realm, session);
            rep.setId(child.getId());
            adminEvent.operation(OperationType.CREATE);
        }
        adminEvent.resourcePath(session.getContext().getUri()).representation(rep).success();

        //GroupRepresentation childRep = ModelToRepresentation.toGroupHierarchy(child, true);
        //custom agm implementation
        if (groupEnrollmentConfigurationRepository.getByGroup(rep.getId()).collect(Collectors.toList()).isEmpty()) {
            //group configuration creation
            groupRolesRepository.create(Utils.defaultGroupRole, rep.getId());
            groupEnrollmentConfigurationRepository.createDefault(child, rep.getName(), realm.getId());
        }
        return Response.noContent().build();
    }

    public static boolean removeMemberUserAttributeCondition(String x, String urnNamespace, String groupName) {
        return x.equals(urnNamespace + groupStr + groupName) || x.startsWith(urnNamespace + groupStr + groupName + colon + roleStr) || x.startsWith(urnNamespace + groupStr + groupName + sharp);
    }

    public static void changeUserAttributeValue(UserModel user, UserGroupMembershipExtensionEntity member, String groupName, MemberUserAttributeConfigurationEntity memberUserAttribute, KeycloakSession session) throws UnsupportedEncodingException {
        List<String> memberUserAttributeValues = user.getAttributeStream(memberUserAttribute.getUserAttribute()).collect(Collectors.toList());
        memberUserAttributeValues.removeIf(x -> Utils.removeMemberUserAttributeCondition(x, memberUserAttribute.getUrnNamespace(), groupName));
        if (member.getGroupRoles() == null || member.getGroupRoles().isEmpty()) {
            memberUserAttributeValues.add(Utils.createMemberUserAttribute(groupName, null, memberUserAttribute.getUrnNamespace(), memberUserAttribute.getAuthority()));
        } else {
            memberUserAttributeValues.addAll(member.getGroupRoles().stream().map(role -> {
                try {
                    return Utils.createMemberUserAttribute(groupName, role.getName(), memberUserAttribute.getUrnNamespace(), memberUserAttribute.getAuthority());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList()));
        }
        Map<String, List<String>> attributes = user.getAttributes();
        attributes.put(memberUserAttribute.getUserAttribute(), memberUserAttributeValues);
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(USER_API, attributes, user);
        profile.update(true);
    }

    public static Stream<GroupModel> getGroupWithSubgroups(GroupModel group){
        Set<GroupModel> groups = getAllSubgroups(group);
        groups.add(group);
        return groups.stream();
    }

    public static Stream<String> getGroupIdsWithSubgroups(GroupModel group){
        Set<String> groups = getAllSubgroupsIds(group);
        groups.add(group.getId());
        return groups.stream();
    }

    public static Set<String> getAllSubgroupsIds(GroupModel group){
        Set<String> allSubGroups = group.getSubGroupsStream().map(g -> g.getId()).collect(Collectors.toSet());
        group.getSubGroupsStream().forEach(g -> allSubGroups.addAll(getAllSubgroupsIds(g)) );
        return allSubGroups;
    }

    public static Set<GroupModel> getAllSubgroups(GroupModel group){
        Set<GroupModel> allSubGroups = group.getSubGroupsStream().collect(Collectors.toSet());
        group.getSubGroupsStream().forEach(g -> allSubGroups.addAll(getAllSubgroups(g)) );
        return allSubGroups;
    }

    public static boolean hasManageGroupsAccountRole(RealmModel realm, UserModel user) {
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        return client!= null && user.hasRole(client.getRole(DEFAULT_GROUP_ROLE_NAME));
    }

    public static FederatedIdentityRepresentation getFederatedIdentityRep(RealmModel realm, String idPAlias) {
        FederatedIdentityRepresentation rep = new FederatedIdentityRepresentation();
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(idPAlias);
        rep.setIdentityProvider(idp.getDisplayName() != null ? idp.getDisplayName() : idPAlias);
        return rep;
    }

    public static String getIdPName(IdentityProviderModel idp) {
        return idp.getDisplayName() != null ? idp.getDisplayName() : idp.getAlias();
    }

}
