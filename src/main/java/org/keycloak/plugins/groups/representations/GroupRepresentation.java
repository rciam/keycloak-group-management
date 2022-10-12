package org.keycloak.plugins.groups.representations;

import java.util.List;

public class GroupRepresentation extends org.keycloak.representations.idm.GroupRepresentation {

    private Boolean hasRights;
    private List<GroupRepresentation> subGroups;

    public Boolean getHasRights() {
        return hasRights;
    }

    public void setHasRights(Boolean hasRights) {
        this.hasRights = hasRights;
    }

    public List<GroupRepresentation> getExtraSubGroups() {
        return this.subGroups;
    }

    public void setExtraSubGroups(List<GroupRepresentation> subGroups) {
        this.subGroups = subGroups;
    }
}
