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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestRepresentation;

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
            throw new BadRequestException("You can not change this group enrollment");
        enrollmentEntity.setStatus(EnrollmentRequestStatusEnum.PENDING_APPROVAL);
        enrollmentEntity.setComment(comment);
        groupEnrollmentRequestRepository.update(enrollmentEntity);
        return Response.noContent().build();
    }


}
