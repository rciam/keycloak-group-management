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
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;

public class GroupAdminGroupMembers {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private GroupModel group;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public GroupAdminGroupMembers(KeycloakSession session, RealmModel realm, UserModel voAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider) {
        this.session = session;
        this.realm =  realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
    }


    @Deprecated
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public Response addGroupMember(UserGroupMembershipExtensionRepresentation rep) {
        UserGroupMembershipExtensionEntity member = userGroupMembershipExtensionRepository.getByUserAndGroup(group.getId(), rep.getUser().getId());
        if ( member != null ) {
            return ErrorResponse.error("This user is already member of this group!", Response.Status.BAD_REQUEST);
        }
        UserModel user = session.users().getUserById(realm, rep.getUser().getId());
        if ( user == null ) {
            throw new NotFoundException("Could not find this User");
        }
        rep.setGroupId(group.getId());
        userGroupMembershipExtensionRepository.create(rep, voAdmin.getId(), user, group );
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
    public UserGroupMembershipExtensionRepresentationPager memberhipPager(@QueryParam("first") @DefaultValue("0") Integer first,
                                                                          @QueryParam("max") @DefaultValue("10") Integer max,
                                                                          @QueryParam("search") String search,
                                                                          @QueryParam("status") MemberStatusEnum status){
        return userGroupMembershipExtensionRepository.searchByGroup(group.getId(), search, status, first, max, realm);
    }

}
