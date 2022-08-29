package org.keycloak.plugins.groups.helpers;

import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;

public class EntityToRepresentation {

    public static GroupConfigurationRepresentation toRepresentation(GroupConfigurationEntity entity) {
        GroupConfigurationRepresentation rep = new GroupConfigurationRepresentation(entity.getId());
        rep.setDescription(entity.getDescription());
        rep.setRequireApproval(entity.getRequireApproval());
        rep.setRequireAupAcceptance(entity.getRequireAupAcceptance());
        rep.setAupExpirySec(entity.getAupExpirySec());
        rep.setMembershipExpirationSec(entity.getMembershipExpirationSec());
        return rep;
    }
}
