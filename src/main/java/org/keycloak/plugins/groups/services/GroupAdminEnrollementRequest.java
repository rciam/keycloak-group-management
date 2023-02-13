package org.keycloak.plugins.groups.services;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.theme.FreeMarkerUtil;

public class GroupAdminEnrollementRequest {

    private static final Logger logger = Logger.getLogger(UserGroupEnrollmentRequestAction.class);
    private static final String statusErrorMessage = "Enrollment is not in status Pending approval";

    protected final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private final AdminEventBuilder adminEvent;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final GroupEnrollmentRequestEntity enrollmentEntity;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    public GroupAdminEnrollementRequest(KeycloakSession session, RealmModel realm, GroupEnrollmentRequestRepository groupEnrollmentRequestRepository, UserModel groupAdmin, GroupEnrollmentRequestEntity enrollmentEntity, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm =  realm;
        this.adminEvent = adminEvent;
        this.groupEnrollmentConfigurationRepository =  new GroupEnrollmentConfigurationRepository(session, realm);
        this.groupEnrollmentRequestRepository = groupEnrollmentRequestRepository;
        this.groupAdmin = groupAdmin;
        this.enrollmentEntity = enrollmentEntity;
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
        this.customFreeMarkerEmailTemplateProvider = new CustomFreeMarkerEmailTemplateProvider(session, new FreeMarkerUtil());
        this.customFreeMarkerEmailTemplateProvider.setRealm(realm);
    }

    @GET
    @Produces("application/json")
    public GroupEnrollmentRequestRepresentation getGroupEnrollment() {
        return EntityToRepresentation.toRepresentation(enrollmentEntity, realm);
    }

    @POST
    @Path("/extra-info")
    public Response askForExtraInformation(@NotNull @QueryParam("comment") String comment) {
        if (!EnrollmentStatusEnum.PENDING_APPROVAL.equals(enrollmentEntity.getStatus()))
            throw new BadRequestException(statusErrorMessage);
        enrollmentEntity.setStatus(EnrollmentStatusEnum.WAITING_FOR_REPLY);
        enrollmentEntity.setComment(comment);
        groupEnrollmentRequestRepository.update(enrollmentEntity);
        return Response.noContent().build();
    }

    @POST
    @Path("/accept")
    public Response acceptEnrollment(@QueryParam("adminJustification") String adminJustification) {
        if (!EnrollmentStatusEnum.PENDING_APPROVAL.equals(enrollmentEntity.getStatus()))
            throw new BadRequestException(statusErrorMessage);

        userGroupMembershipExtensionRepository.createOrUpdate(enrollmentEntity, session, groupAdmin.getId(),adminEvent);
        enrollmentEntity.setStatus(EnrollmentStatusEnum.ACCEPTED);
        enrollmentEntity.setAdminJustification(adminJustification);

        groupEnrollmentRequestRepository.update(enrollmentEntity);

        try {
            customFreeMarkerEmailTemplateProvider.setUser(session.users().getUserById(realm,enrollmentEntity.getUser().getId()));
            customFreeMarkerEmailTemplateProvider.sendAcceptRejectEnrollmentEmail(true, enrollmentEntity.getGroupEnrollmentConfiguration().getGroup().getName(), enrollmentEntity.getAdminJustification());
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/reject")
    public Response rejectEnrollment(@QueryParam("adminJustification") String adminJustification) {
        if (!EnrollmentStatusEnum.PENDING_APPROVAL.equals(enrollmentEntity.getStatus()))
            throw new BadRequestException(statusErrorMessage);
        enrollmentEntity.setStatus(EnrollmentStatusEnum.REJECTED);
        enrollmentEntity.setAdminJustification(adminJustification);
        groupEnrollmentRequestRepository.update(enrollmentEntity);

        try {
            customFreeMarkerEmailTemplateProvider.setUser(session.users().getUserById(realm,enrollmentEntity.getUser().getId()));
            customFreeMarkerEmailTemplateProvider.sendAcceptRejectEnrollmentEmail(false, enrollmentEntity.getGroupEnrollmentConfiguration().getGroup().getName(), enrollmentEntity.getAdminJustification());
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }


}
