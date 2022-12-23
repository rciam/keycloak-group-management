package org.keycloak.plugins.groups.services;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.helpers.EntityToRepresentation;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentConfigurationRepresentation;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRepresentation;

public class UserGroupEnrollmentAction {

    private static final Logger logger = Logger.getLogger(UserGroupEnrollmentAction.class);

    protected final KeycloakSession session;
    private final RealmModel realm;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupEnrollmentRepository groupEnrollmentRepository;
    private final UserModel user;
    private final GroupEnrollmentEntity enrollmentEntity;

    public UserGroupEnrollmentAction(KeycloakSession session, RealmModel realm, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository, GroupEnrollmentRepository groupEnrollmentRepository, UserModel user, GroupEnrollmentEntity enrollmentEntity) {
        this.session = session;
        this.realm =  realm;
        this.groupEnrollmentConfigurationRepository =  groupEnrollmentConfigurationRepository;
        this.groupEnrollmentRepository =  groupEnrollmentRepository;
        this.user = user;
        this.enrollmentEntity = enrollmentEntity;
    }

    @GET
    @Produces("application/json")
    public GroupEnrollmentRepresentation getGroupEnrollment() {
       return EntityToRepresentation.toRepresentation(enrollmentEntity, realm);
    }

    @POST
    @Path("/respond")
    public Response askForExtraInformation(@NotNull @QueryParam("comment") String comment) {
        if (!EnrollmentStatusEnum.WAITING_FOR_REPLY.equals(enrollmentEntity.getStatus()))
            throw new BadRequestException("You can not change this group enrollment");
        enrollmentEntity.setStatus(EnrollmentStatusEnum.PENDING_APPROVAL);
        enrollmentEntity.setComment(comment);
        groupEnrollmentRepository.update(enrollmentEntity);
        return Response.noContent().build();
    }


}
