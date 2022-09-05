package org.keycloak.plugins.groups.services;


import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserVoGroupMembershipEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserVoGroupMembershipRepository;
import org.keycloak.plugins.groups.representations.GroupConfigurationRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.theme.FreeMarkerUtil;

public class AdminGroups {

    private static final Logger logger = Logger.getLogger(AdminGroups.class);

    private KeycloakSession session;
    private final RealmModel realm;
    private AdminPermissionEvaluator realmAuth;
    private GroupModel group;
    private GroupConfigurationRepository groupConfigurationRepository;
    private UserVoGroupMembershipRepository userVoGroupMembershipRepository;
    private CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public AdminGroups(KeycloakSession session, AdminPermissionEvaluator realmAuth, GroupModel group,  RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        this.realmAuth = realmAuth;
        this.group = group;
        this.groupConfigurationRepository =  new GroupConfigurationRepository(session, session.getContext().getRealm());
        this.userVoGroupMembershipRepository =  new UserVoGroupMembershipRepository(session, session.getContext().getRealm());
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
  }

    @GET
    @Produces("application/json")
    public GroupConfigurationRepresentation getGroupConfiguration() {
        GroupConfigurationEntity groupConfiguration = groupConfigurationRepository.getEntity(group.getId());
        //if not exist, group have only created from main Keycloak
        if(groupConfiguration == null) {
            GroupConfigurationRepresentation rep = new GroupConfigurationRepresentation(group.getId());
            return rep;
        } else {
            GroupConfigurationRepresentation rep = EntityToRepresentation.toRepresentation(groupConfiguration, realm);
            return rep;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveGroupConfiguration(GroupConfigurationRepresentation rep) {
        realmAuth.groups().requireManage(group);
        GroupConfigurationEntity entity = groupConfigurationRepository.getEntity(group.getId());
        if ( entity != null) {
            groupConfigurationRepository.update(entity, rep, realmAuth.adminAuth().getUser().getId());
        } else {
            //only group exists
            groupConfigurationRepository.create(rep, group.getId(), realmAuth.adminAuth().getUser().getId());
        }
        //aup change action
        return Response.noContent().build();
    }

    @POST
    @Path("/member/{userId}/admin")
    @Produces("application/json")
    public Response addOrRemoveVoAdmin(@PathParam("userId") String userId, @QueryParam("addVoMember") Boolean addVoMember) {
        UserModel user = session.users().getUserById(realm, userId);
        if ( user == null ) {
            throw new NotFoundException("Could not find this User");
        }
        realmAuth.users().requireManageGroupMembership(user);
        UserVoGroupMembershipEntity member = userVoGroupMembershipRepository.getByUserAndGroup(group.getId(), user.getId());
        if ( member == null ) {
            throw new NotFoundException("Could not find this Group member");
        }
        member.setIsAdmin(addVoMember);
        userVoGroupMembershipRepository.update(member);
        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendVoAdminEmail(group.getName(), addVoMember);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }




}
