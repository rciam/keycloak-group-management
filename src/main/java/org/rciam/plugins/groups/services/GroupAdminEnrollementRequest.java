package org.rciam.plugins.groups.services;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.ModelToRepresentation;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestRepresentation;
import org.keycloak.services.ServicesLogger;

public class GroupAdminEnrollementRequest {

    private static final String statusErrorMessage = "Enrollment is not in status Pending approval";

    @Context
    private ClientConnection clientConnection;

    protected final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final GroupEnrollmentRequestEntity enrollmentEntity;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;
    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;

    public GroupAdminEnrollementRequest(KeycloakSession session, RealmModel realm, GroupEnrollmentRequestRepository groupEnrollmentRequestRepository, UserModel groupAdmin, GroupEnrollmentRequestEntity enrollmentEntity, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository) {
        this.session = session;
        this.realm = realm;
        this.groupEnrollmentRequestRepository = groupEnrollmentRequestRepository;
        this.groupAdmin = groupAdmin;
        this.enrollmentEntity = enrollmentEntity;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session);
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        this.customFreeMarkerEmailTemplateProvider.setSignatureMessage(memberUserAttribute.getSignatureMessage());
    }

    @GET
    @Produces("application/json")
    public GroupEnrollmentRequestRepresentation getGroupEnrollment() {
        return EntityToRepresentation.toRepresentation(enrollmentEntity, realm);
    }

    @POST
    @Path("/extra-info")
    public Response askForExtraInformation(@NotNull @QueryParam("comment") String comment) {
        if (!EnrollmentRequestStatusEnum.PENDING_APPROVAL.equals(enrollmentEntity.getStatus()))
            throw new BadRequestException(statusErrorMessage);
        enrollmentEntity.setStatus(EnrollmentRequestStatusEnum.WAITING_FOR_REPLY);
        enrollmentEntity.setComment(comment);
        groupEnrollmentRequestRepository.update(enrollmentEntity);
        return Response.noContent().build();
    }

    @POST
    @Path("/accept")
    public Response acceptEnrollment(@QueryParam("adminJustification") String adminJustification) throws UnsupportedEncodingException {
        if (!EnrollmentRequestStatusEnum.PENDING_APPROVAL.equals(enrollmentEntity.getStatus()))
            throw new BadRequestException(statusErrorMessage);

        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        userGroupMembershipExtensionRepository.createOrUpdate(enrollmentEntity, session, groupAdmin, memberUserAttribute, clientConnection);
        updateEnrollmentRequest(enrollmentEntity, EnrollmentRequestStatusEnum.ACCEPTED, adminJustification);

        try {
            GroupModel group= realm.getGroupById(enrollmentEntity.getGroupEnrollmentConfiguration().getGroup().getId());
            customFreeMarkerEmailTemplateProvider.setUser(session.users().getUserById(realm, enrollmentEntity.getUser().getId()));
            customFreeMarkerEmailTemplateProvider.sendAcceptRejectEnrollmentEmail(true, ModelToRepresentation.buildGroupPath(group), enrollmentEntity.getAdminJustification());
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/reject")
    public Response rejectEnrollment(@QueryParam("adminJustification") String adminJustification) {
        if (!EnrollmentRequestStatusEnum.PENDING_APPROVAL.equals(enrollmentEntity.getStatus()))
            throw new BadRequestException(statusErrorMessage);
        updateEnrollmentRequest(enrollmentEntity, EnrollmentRequestStatusEnum.REJECTED, adminJustification);

        try {
            GroupModel group= realm.getGroupById(enrollmentEntity.getGroupEnrollmentConfiguration().getGroup().getId());
            customFreeMarkerEmailTemplateProvider.setUser(session.users().getUserById(realm, enrollmentEntity.getUser().getId()));
            customFreeMarkerEmailTemplateProvider.sendAcceptRejectEnrollmentEmail(false, ModelToRepresentation.buildGroupPath(group), enrollmentEntity.getAdminJustification());
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }

    private void updateEnrollmentRequest(GroupEnrollmentRequestEntity enrollmentEntity, EnrollmentRequestStatusEnum status, String adminJustification) {
        enrollmentEntity.setStatus(status);
        enrollmentEntity.setAdminJustification(adminJustification);
        enrollmentEntity.setApprovedDate(LocalDateTime.now());
        UserEntity groupAdminEntity = new UserEntity();
        groupAdminEntity.setId(groupAdmin.getId());
        enrollmentEntity.setCheckAdmin(groupAdminEntity);
        groupEnrollmentRequestRepository.update(enrollmentEntity);
    }


}
