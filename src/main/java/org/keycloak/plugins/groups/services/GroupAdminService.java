package org.keycloak.plugins.groups.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.enums.EnrollmentStatusEnum;
import org.keycloak.plugins.groups.helpers.AuthenticationHelper;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentPager;
import org.keycloak.plugins.groups.representations.GroupsPager;
import org.keycloak.services.ForbiddenException;

public class GroupAdminService {

    private KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupEnrollmentRepository groupEnrollmentRepository;

    public GroupAdminService(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm =  realm;
        AuthenticationHelper authHelper = new AuthenticationHelper(session);
        this.voAdmin = authHelper.authenticateUserRequest().getUser();
        this.groupAdminRepository =  new GroupAdminRepository(session, realm);
        this.groupEnrollmentRepository =  new GroupEnrollmentRepository(session, realm);
    }


    @GET
    @Path("/groups")
    public GroupsPager getGroupAdminGroups(@QueryParam("first") @DefaultValue("0") Integer first,
                                           @QueryParam("max") @DefaultValue("10") Integer max){
        return groupAdminRepository.getAdminGroups(voAdmin.getId(), first, max);
    }

    @Path("/group/{groupId}")
    public GroupAdminGroup group(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        if (!groupAdminRepository.isGroupAdmin(voAdmin.getId(), group)){
            throw new ForbiddenException();
        }

        GroupAdminGroup service = new GroupAdminGroup(session, realm, voAdmin, group);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @GET
    @Path("/enroll-requests")
    @Produces("application/json")
    public GroupEnrollmentPager getAdminEnrollments(@QueryParam("first") @DefaultValue("0") Integer first,
                                                    @QueryParam("max") @DefaultValue("10") Integer max,
                                                    @QueryParam("groupId") String groupId,
                                                    @QueryParam("userSearch") String userSearch,
                                                    @QueryParam("status") EnrollmentStatusEnum status) {
        List<String> groupIds = groupId != null ? Stream.of(groupId).collect(Collectors.toList()):groupAdminRepository.getAllAdminGroupIds(voAdmin.getId());
        return groupEnrollmentRepository.groupAdminEnrollmentPager(groupIds, userSearch, status, first, max);
    }

    @Path("/enroll-request/{enrollId}")
    public GroupAdminEnrollement enrollment(@PathParam("enrollId") String enrollId) {
        GroupEnrollmentEntity entity = groupEnrollmentRepository.getEntity(enrollId);
        if (entity == null) {
            throw new NotFoundException("Could not find Group Enrollment Request by id");
        }
        GroupModel group = realm.getGroupById(entity.getGroupEnrollmentConfiguration().getGroup().getId());
        if (group == null) {
            throw new NotFoundException("Could not find the group of Group Enrollment Request by id");
        }
        if (!groupAdminRepository.isGroupAdmin(voAdmin.getId(), group)){
            throw new ForbiddenException();
        }

        GroupAdminEnrollement service = new GroupAdminEnrollement(session, realm, groupEnrollmentRepository, voAdmin, entity);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
