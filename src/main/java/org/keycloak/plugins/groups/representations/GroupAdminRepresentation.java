package org.keycloak.plugins.groups.representations;

import org.keycloak.representations.idm.UserRepresentation;

public class GroupAdminRepresentation {

    private UserRepresentation user;
    private boolean direct;

    public GroupAdminRepresentation(UserRepresentation user,  boolean direct){
        this.user = user;
        this.direct = direct;
    }

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }
}
