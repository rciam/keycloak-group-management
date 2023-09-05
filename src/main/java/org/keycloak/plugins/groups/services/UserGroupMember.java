package org.keycloak.plugins.groups.services;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.helpers.LoginEventHelper;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;

public class UserGroupMember {

    @Context
    private ClientConnection clientConnection;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserGroupMembershipExtensionEntity member;

    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;

    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;
    private final UserModel user;

    public UserGroupMember(KeycloakSession session, RealmModel realm, UserModel user, UserGroupMembershipExtensionEntity member, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository) {
        this.session = session;
        this.realm = realm;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.user = user;
        this.member = member;
        this.memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
    }

    @GET
    @Produces("application/json")
    public UserGroupMembershipExtensionRepresentation getMember() {
        return EntityToRepresentation.toRepresentation(member, realm);
    }

    @DELETE
    public Response leaveGroup() {
        GroupModel group = realm.getGroupById(member.getGroup().getId());
        userGroupMembershipExtensionRepository.deleteMember(member, group, user, clientConnection, user.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(user.getId()), memberUserAttributeConfigurationRepository);
        return Response.noContent().build();
    }

//    @POST
//    @Path("/aup-renew")
//    public Response aupRenew() {
//
//        if (!MemberStatusEnum.ENABLED.equals(member.getStatus())) {
//            throw new NotFoundException("You are not active member of this group");
//        }
//        GroupEnrollmentConfigurationEntity configuration = groupEnrollmentConfigurationRepository.getEntity(member.getGroupEnrollmentConfigurationId());
//        if (configuration == null) {
//            throw new NotFoundException("This configuration does not exist. You need to create new enrollment flow.");
//        }
//        if (configuration.getAupExpiryDays() == null) {
//            throw new BadRequestException("This configuration does not have aup renew configuration.");
//        }
//        member.setAupExpiresAt((member.getAupExpiresAt()!=null ? member.getAupExpiresAt() : LocalDate.now()).plusDays(configuration.getAupExpiryDays()));
//        userGroupMembershipExtensionRepository.update(member);
//        return Response.noContent().build();
//    }
}
