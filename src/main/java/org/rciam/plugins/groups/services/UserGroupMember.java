package org.rciam.plugins.groups.services;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.ServicesLogger;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import java.util.List;
import java.util.stream.Collectors;

public class UserGroupMember {

    private final ClientConnection clientConnection;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserGroupMembershipExtensionEntity member;

    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;

    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final UserModel user;

    public UserGroupMember(KeycloakSession session, RealmModel realm, UserModel user, UserGroupMembershipExtensionEntity member, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupAdminRepository groupAdminRepository, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, ClientConnection clientConnection) {
        this.session = session;
        this.realm = realm;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.user = user;
        this.member = member;
        this.memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        this.groupAdminRepository = groupAdminRepository;
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.clientConnection = clientConnection;
    }

    @GET
    @Produces("application/json")
    public UserGroupMembershipExtensionRepresentation getMember() {
        return EntityToRepresentation.toRepresentation(member, realm, true);
    }

    @DELETE
    public Response leaveGroup() {
        GroupModel group = realm.getGroupById(member.getGroup().getId());
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        List<String> subgroupPaths = userGroupMembershipExtensionRepository.deleteMember(member, group, user, clientConnection, user.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(user.getId()), memberUserAttribute, false).stream().map(ModelToRepresentation::buildGroupPath).collect(Collectors.toList());

        String groupPath = ModelToRepresentation.buildGroupPath(group);
        groupAdminRepository.getAllAdminIdsGroupUsers(group).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
            try {
                customFreeMarkerEmailTemplateProvider.setUser(admin);
                customFreeMarkerEmailTemplateProvider.sendLeaveMemberAdminInformationEmail(group.getId(), groupPath, subgroupPaths, user);
            } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
            }
        });


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
