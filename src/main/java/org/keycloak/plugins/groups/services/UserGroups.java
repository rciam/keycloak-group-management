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
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
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
    private GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private UserModel user;

    public UserGroups(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        this.authHelper = new AuthenticationHelper(session);
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
    }



    @GET
    @Path("/groups")
    @Produces("application/json")
    public Response getAllUserGroups() {
        UserModel user = authHelper.authenticateUserRequest().getUser();
        if(user == null)
            return Response.status(Response.Status.UNAUTHORIZED).entity(new ErrorResponse("Could not identify logged in user.")).build();
//        RealmModel realm = session.getContext().getRealm();
        List<GroupRepresentation> userGroups = user.getGroupsStream().map(g-> ModelToRepresentation.toRepresentation(g,true)).collect(Collectors.toList());
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(userGroups).build();
    }

    @POST
    @Path("/group/{groupId}/admin")
    @Produces("application/json")
    public Response addAsGroupAdmin() {
        //    UserModel user = session.users().getUserById(realm, userId);
//        if ( user == null ) {
//        throw new NotFoundException("Could not find this User");
//    }
//        try {
//        if (!groupAdminRepository.isGroupAdmin(user.getId(), group)) {
//            groupAdminRepository.addGroupAdmin(userId, group.getId());
//
//            try {
//                customFreeMarkerEmailTemplateProvider.setUser(user);
//                customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(group.getName(), true);
//            } catch (EmailException e) {
//                ServicesLogger.LOGGER.failedToSendEmail(e);
//            }
//            return Response.noContent().build();
//        } else {
//            return Response.status(Response.Status.BAD_REQUEST).entity(user.getUsername() + " is already group admin for the " + group.getName() + " group or one of its parent.").build();
//        }
//    } catch (Exception e) {
//        return Response.status(Response.Status.BAD_REQUEST).entity(ModelDuplicateException.class.equals(e.getClass()) ? "Admin has already been existed" : "Problem during admin save").build();
//    }
        return Response.ok().build();
    }

    @GET
    @Path("/enroll/request")
    @Produces("application/json")
    public List<GroupEnrollmentEntity> getMyEnrollments() {
        UserModel user = authHelper.authenticateUserRequest().getUser();
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
