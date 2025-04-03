package org.rciam.plugins.groups.services;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorResponseException;
import org.rciam.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestRepresentation;

public class UserGroupEnrollmentRequestAction {

    private static final Logger logger = Logger.getLogger(UserGroupEnrollmentRequestAction.class);

    protected final KeycloakSession session;
    private final RealmModel realm;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final UserModel user;
    private final GroupEnrollmentRequestEntity enrollmentEntity;

    public UserGroupEnrollmentRequestAction(KeycloakSession session, RealmModel realm, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, GroupEnrollmentRequestRepository groupEnrollmentRequestRepository, UserModel user, GroupEnrollmentRequestEntity enrollmentEntity) {
        this.session = session;
        this.realm =  realm;
        this.groupEnrollmentConfigurationRepository =  groupEnrollmentConfigurationRepository;
        this.groupEnrollmentRequestRepository = groupEnrollmentRequestRepository;
        this.user = user;
        this.enrollmentEntity = enrollmentEntity;
    }

    @GET
    @Produces("application/json")
    public GroupEnrollmentRequestRepresentation getGroupEnrollment() {
       return EntityToRepresentation.toRepresentation(enrollmentEntity, realm);
    }

    @POST
    @Path("/respond")
    public Response askForExtraInformation(@NotNull @QueryParam("comment") String comment) {
        if (!EnrollmentRequestStatusEnum.WAITING_FOR_REPLY.equals(enrollmentEntity.getStatus()))
            throw new ErrorResponseException("You can not change this group enrollment", "You can not change this group enrollment", Response.Status.BAD_REQUEST);
        enrollmentEntity.setStatus(EnrollmentRequestStatusEnum.PENDING_APPROVAL);
        enrollmentEntity.setComment(comment);
        groupEnrollmentRequestRepository.update(enrollmentEntity);
        return Response.noContent().build();
    }


}
