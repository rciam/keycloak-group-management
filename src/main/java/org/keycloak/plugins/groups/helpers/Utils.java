package org.keycloak.plugins.groups.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;

public class Utils {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final String expirationNotificationPeriod="expiration-notification-period";
    public static final String invitationExpirationPeriod ="invitation-expiration-period";
    public static final String defaultGroupRole ="member";

    public static final String eventId = "1";

    private static final String chronJobUserId ="chron-job";
    private static final String colon = ":";
    private static final String space = " ";
    private static final String sharp = "#";
    public static final String groupStr = ":group:";
    public static final String roleStr = "role=";

    public static UserAdapter getDummyUser(UserRepresentation userRep) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(userRep.getEmail(), true);
        userEntity.setFirstName(userRep.getFirstName());
        userEntity.setLastName(userRep.getLastName());
        UserAdapter user = new UserAdapter(null, null, null, userEntity);
        return user;
    }

    public static UserAdapter getDummyUser(String email, String firstName, String lastName) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email, true);
        userEntity.setFirstName(firstName);
        userEntity.setLastName(lastName);
        UserAdapter user = new UserAdapter(null, null, null, userEntity);
        return user;
    }

    public static UserAdapter getChronJobUser() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(chronJobUserId);
        UserAdapter user = new UserAdapter(null, null, null, userEntity);
        return user;
    }

    public static String createMemberUserAttribute(String groupName, String role , String namespace, String authority) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(namespace);
        sb.append(groupStr);
        sb.append(groupName);
        if (role!= null){
          sb.append(colon).append(roleStr).append(encode(role));
        }
        if (authority != null){
            sb.append(sharp).append(authority);
        }
        return sb.toString();
    }

    public static String getGroupNameForMemberUserAttribute(GroupEntity group, RealmModel realm) throws UnsupportedEncodingException {
        String groupName = encode(group.getName());
        GroupModel parent = realm.getGroupById(group.getParentId());
        while (parent != null) {
            groupName = encode(parent.getName()) + colon + groupName;
            parent = parent.getParent();
        }
        return groupName;
    }

    private static String encode(String x) throws UnsupportedEncodingException {
        return URLEncoder.encode(x.replace(space,"%20"), StandardCharsets.UTF_8.toString()).replace("%2520","%20");
    }

    public static Response addGroupChild(GroupRepresentation rep, RealmModel realm, GroupModel group, KeycloakSession session, AdminEventBuilder adminEvent, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, GroupRolesRepository groupRolesRepository) {

        //GroupResource addGroupChild method except return Response
        adminEvent.resource(ResourceType.GROUP);
        String groupName = rep.getName();
        if (ObjectUtil.isBlank(groupName)) {
            return ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
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
                return ErrorResponse.error("This subgroup has already existed", Response.Status.BAD_REQUEST);
            child = realm.createGroup(groupName, group);
            GroupResource.updateGroup(rep, child);
            rep.setId(child.getId());
            adminEvent.operation(OperationType.CREATE);
        }
        adminEvent.resourcePath(session.getContext().getUri()).representation(rep).success();

        //GroupRepresentation childRep = ModelToRepresentation.toGroupHierarchy(child, true);
        //custom agm implementation
        if (groupEnrollmentConfigurationRepository.getByGroup(rep.getId()).collect(Collectors.toList()).isEmpty()) {
            //group creation
            groupEnrollmentConfigurationRepository.createDefault(child.getId(), rep.getName());
            groupRolesRepository.create(Utils.defaultGroupRole,rep.getId());
        }
        return Response.noContent().build();
    }

    public static boolean removeMemberUserAttributeCondition(String x, String urnNamespace, String groupName){
        return x.equals(urnNamespace+groupStr+groupName) || x.startsWith(urnNamespace+groupStr+groupName+roleStr) || x.startsWith(urnNamespace+groupStr+groupName+sharp);
    }

    public static void changeUserAttributeValue(UserModel user, UserGroupMembershipExtensionEntity member, String groupName, MemberUserAttributeConfigurationEntity memberUserAttribute) throws UnsupportedEncodingException {
        List<String> memberUserAttributeValues = user.getAttribute(memberUserAttribute.getUserAttribute());
        memberUserAttributeValues.removeIf(x-> Utils.removeMemberUserAttributeCondition(x,memberUserAttribute.getUrnNamespace(),groupName));
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
        user.setAttribute(memberUserAttribute.getUserAttribute(),memberUserAttributeValues);
    }

}
