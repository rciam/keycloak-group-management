package org.rciam.plugins.groups.representations;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.keycloak.representations.idm.UserRepresentation;

@Schema
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
