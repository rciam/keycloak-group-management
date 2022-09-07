package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupConfigurationRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.stubs.ErrorResponse;
import org.keycloak.representations.idm.GroupRepresentation;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

public class UserGroups {

    private static final Logger logger = Logger.getLogger(UserGroups.class);

    protected KeycloakSession session;
    private RealmModel realm;

    private AuthenticationHelper authHelper;
    private GroupConfigurationRepository groupConfigurationRepository;

    public UserGroups(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        this.authHelper = new AuthenticationHelper(session);
        this.groupConfigurationRepository =  new GroupConfigurationRepository(session, realm);
    }



    @GET
    @Produces("application/json")
    public Response getAllUserGroups() {
        UserModel user = authHelper.authenticateUserRequest();
        if(user == null)
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("Could not identify logged in user.")).build();
//        RealmModel realm = session.getContext().getRealm();
        List<GroupRepresentation> userGroups = user.getGroupsStream().map(g-> ModelToRepresentation.toRepresentation(g,true)).collect(Collectors.toList());
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(userGroups).build();
    }

//    @POST
//    @Produces("application/json")
//    public Response addUserGroup() {
//        UserModel user = authHelper.authenticateUserRequest();
//        if(user == null)
//            return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("Could not identify logged in user.")).build();
////        RealmModel realm = session.getContext().getRealm();
//        List<GroupRepresentation> userGroups = user.getGroupsStream().map(g-> ModelToRepresentation.toRepresentation(g,true)).collect(Collectors.toList());
//        return Response.ok().type(MediaType.APPLICATION_JSON).entity(userGroups).build();
//    }

    @GET
    @Path("/enroll/request")
    @Produces("application/json")
    public List<GroupEnrollmentEntity> getMyEnrollments() {
        UserModel user = authHelper.authenticateUserRequest();
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        List<GroupEnrollmentEntity> groupEnrollmentEntities = em.createNamedQuery("getAllUserGroupEnrollments", GroupEnrollmentEntity.class)
                .setParameter("userId", user.getId())
                .getResultList();
        return groupEnrollmentEntities;
    }


    //REMOVE THIS ONE, IT'S FOR TESTING PURPOSES
    @GET
    @Path("/test/get-all")
    @Produces("application/json")
    public List<GroupEnrollmentRepresentation> getAll() {
//        UserModel user = authHelper.authenticateUserRequest();
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        List<GroupEnrollmentRepresentation> res = em.createQuery("select ge from GroupEnrollmentEntity ge", GroupEnrollmentEntity.class)
                .getResultStream()
                .map(entity -> EntityToRepresentation.toRepresentation(entity, realm))
                .collect(Collectors.toList());
        return res;
    }

}
