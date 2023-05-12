package org.keycloak.plugins.groups.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.representations.account.UserRepresentation;

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

    public static String createEdupersonEntitlement(String groupName,String role , String namespace, String authority) throws UnsupportedEncodingException {
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

    public static String getGroupNameForEdupersonEntitlement(GroupEntity group, RealmModel realm) throws UnsupportedEncodingException {
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

}
