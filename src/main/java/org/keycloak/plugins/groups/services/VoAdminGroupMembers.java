package org.keycloak.plugins.groups.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.StatusEnum;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;
import org.keycloak.plugins.groups.jpa.repositories.UserVoGroupMembershipRepository;
import org.keycloak.plugins.groups.representations.UserVoGroupMembershipRepresentation;
import org.keycloak.plugins.groups.representations.UserVoGroupMembershipRepresentationPager;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;

public class VoAdminGroupMembers {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private GroupModel group;
    private final UserVoGroupMembershipRepository userVoGroupMembershipRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public VoAdminGroupMembers(KeycloakSession session, RealmModel realm, UserModel voAdmin, UserVoGroupMembershipRepository userVoGroupMembershipRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider) {
        this.session = session;
        this.realm =  realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.userVoGroupMembershipRepository =  userVoGroupMembershipRepository;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
    }


    @Deprecated
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public Response addGroupMember(UserVoGroupMembershipRepresentation rep) {
        UserVoGroupMembershipEntity member = userVoGroupMembershipRepository.getByUserAndGroup(group.getId(), rep.getUser().getId());
        if ( member != null ) {
            return ErrorResponse.error("This user is already member of this group!", Response.Status.BAD_REQUEST);
        }
        UserModel user = session.users().getUserById(realm, rep.getUser().getId());
        if ( user == null ) {
            throw new NotFoundException("Could not find this User");
        }
        rep.setGroupId(group.getId());
        userVoGroupMembershipRepository.create(rep, voAdmin.getId(), user, group );
        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendGroupActionEmail(group.getName(), true);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }

    /**
     *
     * @param first
     * @param max
     * @param search user search
     * @param status status search
     * @return
     */
    @GET
    @Produces("application/json")
    public UserVoGroupMembershipRepresentationPager memberhipPager(@QueryParam("first") @DefaultValue("0") Integer first,
                                                                   @QueryParam("max") @DefaultValue("10") Integer max,
                                                                   @QueryParam("search") String search,
                                                                   @QueryParam("status") StatusEnum status){
        return userVoGroupMembershipRepository.searchByGroup(group.getId(), search, status, first, max, realm);
    }

}
