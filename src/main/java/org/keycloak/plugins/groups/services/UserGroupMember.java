package org.keycloak.plugins.groups.services;

import java.time.LocalDate;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;

public class UserGroupMember {

    private static final Logger logger = Logger.getLogger(UserGroups.class);

    protected KeycloakSession session;
    private RealmModel realm;
    private final UserGroupMembershipExtensionEntity member;

    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final UserModel user;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public UserGroupMember(KeycloakSession session, RealmModel realm, UserModel user, UserGroupMembershipExtensionEntity member, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository) {
        this.session = session;
        this.realm =  realm;
        this.userGroupMembershipExtensionRepository =  userGroupMembershipExtensionRepository;
        this.user = user;
        this.member = member;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.groupEnrollmentConfigurationRepository = groupEnrollmentConfigurationRepository;
    }

    @GET
    @Produces("application/json")
    public UserGroupMembershipExtensionRepresentation getMember() {
        return EntityToRepresentation.toRepresentation(member, realm);
    }

    @POST
    @Path("/aup-renew")
    public Response aupRenew() {

        if (!MemberStatusEnum.ENABLED.equals(member.getStatus())) {
            throw new NotFoundException("You are not active member of this group");
        }
        GroupEnrollmentConfigurationEntity configuration = groupEnrollmentConfigurationRepository.getEntity(member.getGroupEnrollmentConfigurationId());
        if (configuration == null) {
            throw new NotFoundException("This configuration does not exist. You need to create new enrollment flow.");
        }
        if (configuration.getAupExpiryDays() == null) {
            throw new BadRequestException("This configuration does not have aup renew configuration.");
        }
        member.setAupExpiresAt((member.getAupExpiresAt()!=null ? member.getAupExpiresAt() : LocalDate.now()).plusDays(configuration.getAupExpiryDays()));
        userGroupMembershipExtensionRepository.update(member);
        return Response.noContent().build();
    }
}
