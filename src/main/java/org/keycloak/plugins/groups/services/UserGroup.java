package org.keycloak.plugins.groups.services;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.services.ServicesLogger;
import org.keycloak.theme.FreeMarkerUtil;

public class UserGroup {

    private static final Logger logger = Logger.getLogger(UserGroups.class);

    protected KeycloakSession session;
    private RealmModel realm;

    private GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private GroupAdminRepository groupAdminRepository;
    private UserModel user;
    private GroupModel group;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public UserGroup(KeycloakSession session, RealmModel realm, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, UserModel user, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, GroupAdminRepository groupAdminRepository) {
        this.session = session;
        this.realm =  realm;
        this.groupEnrollmentConfigurationRepository =  groupEnrollmentConfigurationRepository;
        this.groupAdminRepository = groupAdminRepository;
        this.user = user;
        this.group = group;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
    }

    @POST
    @Path("/admin")
    public Response addAsGroupAdmin() {
        try {
            if (!groupAdminRepository.isGroupAdmin(user.getId(), group)) {
                groupAdminRepository.addGroupAdmin(user.getId(), group.getId());

                try {
                    customFreeMarkerEmailTemplateProvider.setUser(user);
                    customFreeMarkerEmailTemplateProvider.sendGroupAdminEmail(group.getName(), true);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("You are already group admin for the " + group.getName() + " group or one of its parent.").build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ModelDuplicateException.class.equals(e.getClass()) ? "Admin has already been existed" : "Problem during admin save").build();
        }
    }
}
