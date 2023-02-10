package org.keycloak.plugins.groups.helpers;

import java.time.format.DateTimeFormatter;

import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.representations.account.UserRepresentation;

public class Utils {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final String expirationNotificationPeriod="expiration-notification-period";
    public static final String invitationExpirationPeriod ="invitation-expiration-period";
    public static final String defaultGroupRole ="member";

    public static final String eventId = "1";

    private static final String chronJobUserId ="chron-job";

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

}
