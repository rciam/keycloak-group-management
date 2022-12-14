package org.keycloak.plugins.groups.helpers;

import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.representations.account.UserRepresentation;

public class Utils {

    public static UserAdapter getDummyUser(UserRepresentation userRep) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(userRep.getEmail(), true);
        userEntity.setFirstName(userRep.getFirstName());
        userEntity.setLastName(userRep.getLastName());
        UserAdapter user = new UserAdapter(null, null, null, userEntity);
        return user;
    }
}
