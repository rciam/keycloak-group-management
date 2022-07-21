package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.plugins.groups.ui.UserInterfaceService;

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

    @Context
    private HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    private ClientConnection clientConnection;

    @Context
    protected Providers providers;

    @Context
    protected KeycloakSession session;


    public GroupsService() {

    }

    @Path("/")
    @GET
    @Produces("application/json")
    public Response getAllRealmGroups() {
        RealmModel realm = session.getContext().getRealm();
        return Response.ok().entity(realm.getGroupsStream()).build();
    }

    @Path("/dummy")
    @GET
    @Produces("text/plain")
    public Response dummy() {
//        RealmModel realm = session.getContext().getRealm();
        return Response.ok().entity("this is a dummy response").build();
    }

}
