package org.keycloak.plugins.groups.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.ModelToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentPager;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;
import org.keycloak.plugins.groups.stubs.ErrorResponse;
import org.keycloak.representations.idm.GroupRepresentation;

import javax.persistence.EntityManager;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

public class UserGroups {

    private static final Logger logger = Logger.getLogger(UserGroups.class);

    protected final KeycloakSession session;
    private final RealmModel realm;

    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupEnrollmentRepository groupEnrollmentRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final UserModel user;

    public UserGroups(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        this.user = new AuthenticationHelper(session).authenticateUserRequest().getUser();
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupEnrollmentRepository =  new GroupEnrollmentRepository(session, realm);
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
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
    @Path("/enroll-requests")
    @Produces("application/json")
    public GroupEnrollmentPager getMyEnrollments(@QueryParam("first") @DefaultValue("0") Integer first,
                                                 @QueryParam("max") @DefaultValue("10") Integer max,
                                                 @QueryParam("groupName") String groupName,
                                                 @QueryParam("status") EnrollmentStatusEnum status) {
        return groupEnrollmentRepository.groupEnrollmentPager(user.getId(), groupName, status, first, max);
    }

    @POST
    @Path("/enroll-request")
    @Consumes("application/json")
    public Response createEnrollmentRequest(GroupEnrollmentRepresentation rep) {
        GroupEnrollmentConfigurationEntity configuration = groupEnrollmentConfigurationRepository.getEntity(rep.getGroupEnrollmentConfiguration().getId());
        if (configuration == null)
            throw new NotFoundException("Could not find this group enrollment configuration");
        //active only member???
        UserGroupMembershipExtensionEntity member = userGroupMembershipExtensionRepository.getByUserAndGroup(configuration.getGroup().getId(), user.getId());
        if (member != null)
            throw new BadRequestException("You are already member of this group");
        if (groupEnrollmentRepository.countOngoingByUserAndGroup(user.getId(), configuration.getGroup().getId()) > 0)
            throw new BadRequestException("You have an ongoing request to become member of this group");

        groupEnrollmentRepository.create(rep, user.getId());
        return Response.noContent().build();
    }

    @Path("/enroll-request/{id}")
    public UserGroupEnrollmentAction groupEnrollment(@PathParam("id") String id) {

        GroupEnrollmentEntity entity = groupEnrollmentRepository.getEntity(id);
        if (entity == null)
            throw new NotFoundException("Could not find this group enrollment configuration");

        UserGroupEnrollmentAction service = new UserGroupEnrollmentAction(session, realm, groupEnrollmentConfigurationRepository, groupEnrollmentRepository, user, entity);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
