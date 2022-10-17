package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.stubs.ErrorResponse;
import org.keycloak.representations.idm.GroupRepresentation;

import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

public class UserGroups {

    private static final Logger logger = Logger.getLogger(UserGroups.class);

    protected KeycloakSession session;
    private RealmModel realm;
    private GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private  UserModel user;

    public UserGroups(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
        this.user = authHelper.authenticateUserRequest().getUser();
    }



    @GET
    @Path("/groups")
    @Produces("application/json")
    public Response getAllUserGroups() {
        List<GroupRepresentation> userGroups = user.getGroupsStream().map(g-> ModelToRepresentation.toRepresentation(g,true)).collect(Collectors.toList());
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(userGroups).build();
    }

    @Path("/group/{groupId}")
    public UserGroup userGroup(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        UserGroup service = new UserGroup(session, realm, groupEnrollmentConfigurationRepository, user, group);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @GET
    @Path("/enroll/request")
    @Produces("application/json")
    public List<GroupEnrollmentEntity> getMyEnrollments() {
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
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        List<GroupEnrollmentRepresentation> res = em.createQuery("select ge from GroupEnrollmentEntity ge", GroupEnrollmentEntity.class)
                .getResultStream()
                .map(entity -> EntityToRepresentation.toRepresentation(entity, realm))
                .collect(Collectors.toList());
        return res;
    }

}
