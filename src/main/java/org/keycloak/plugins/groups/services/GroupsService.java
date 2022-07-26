package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.helpers.ModelToRepresentation;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.stubs.ErrorResponse;
import org.keycloak.plugins.groups.ui.UserInterfaceService;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

public class GroupsService {

    private static final Logger logger = Logger.getLogger(GroupsService.class);

    protected KeycloakSession session;

    private AuthenticationHelper authHelper;

    public GroupsService(KeycloakSession session) {
        this.session = session;
        this.authHelper = new AuthenticationHelper(session);
    }

    @Path("/")
    @GET
    @Produces("application/json")
    public Response getAllUserGroups() {
        UserModel user = authHelper.authenticateUserRequest();
        if(user == null)
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("Could not identify logged in user.")).build();
//        RealmModel realm = session.getContext().getRealm();
        List<GroupRepresentation> userGroups = user.getGroupsStream().map(g->ModelToRepresentation.toRepresentation(g,true)).collect(Collectors.toList());
        return Response.ok().entity(userGroups).build();
    }


}
