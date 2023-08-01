package org.keycloak.plugins.groups.helpers;

import java.time.LocalDate;
import java.util.List;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;


public class LoginEventHelper {

    public static void createGroupEvent(RealmModel realm, KeycloakSession session, ClientConnection clientConnection, UserModel user, String actionUserId, String eventType, String groupPath, List<String> groupRolesNames, LocalDate expirationDate){
        EventBuilder event = new EventBuilder(realm, session, clientConnection)
                .event(EventType.valueOf(eventType))
                .user(user)
                .detail(Utils.EVENT_GROUP, groupPath)
                .detail(Utils.EVENT_ACTION_USER, actionUserId )
                .detail(Utils.EVENT_ROLES, groupRolesNames);
        if (expirationDate != null)
            event.detail(Utils.EVENT_MEMBERSHIP_EXPIRATION, expirationDate.format(Utils.formatter));
        event.success();
    }
}
