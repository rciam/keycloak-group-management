package org.keycloak.plugins.groups.services;

import java.util.Arrays;
import java.util.List;
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
import org.keycloak.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.keycloak.plugins.groups.enums.MemberStatusEnum;
import org.keycloak.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.plugins.groups.representations.GroupEnrollmentRequestPager;
import org.keycloak.plugins.groups.representations.GroupsPager;
import org.keycloak.plugins.groups.representations.UserGroupMembershipExtensionRepresentationPager;
import org.keycloak.plugins.groups.representations.UserRepresentationPager;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.resources.admin.AdminEventBuilder;

public class GroupAdminService {

    private KeycloakSession session;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final AdminEventBuilder adminEvent;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;

    public GroupAdminService(KeycloakSession session, RealmModel realm, UserModel user, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm =  realm;
        this.groupAdmin = user;
        this.adminEvent = adminEvent;
        this.groupAdminRepository =  new GroupAdminRepository(session, realm);
        this.groupEnrollmentRequestRepository =  new GroupEnrollmentRequestRepository(session, realm, new GroupRolesRepository(session, realm));
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
    }


    @GET
    @Path("/groups")
    public GroupsPager getGroupAdminGroups(@QueryParam("search") String search,
                                           @QueryParam("first") @DefaultValue("0") Integer first,
                                           @QueryParam("max") @DefaultValue("10") Integer max){
        return groupAdminRepository.getAdminGroups(groupAdmin.getId(), search, first, max);
    }

    @Path("/group/{groupId}")
    public GroupAdminGroup group(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }
        if (!groupAdminRepository.isGroupAdmin(groupAdmin.getId(), group)){
            throw new ForbiddenException();
        }

        GroupAdminGroup service = new GroupAdminGroup(session, realm, groupAdmin, group, userGroupMembershipExtensionRepository, groupAdminRepository, groupEnrollmentRequestRepository, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @GET
    @Path("/groupids/all")
    public List<String> getGroupIdsForAdmin(){
        return groupAdminRepository.getAllAdminGroupIds(groupAdmin.getId());
    }

    /**
     *
     * @param first
     * @param max
     * @param search user search
     * @param status status search
     * @return
     */
    @GET
    @Path("groups/members")
    @Produces("application/json")
    public UserRepresentationPager memberhipPager(@QueryParam("first") @DefaultValue("0") Integer first,
                                                  @QueryParam("max") @DefaultValue("10") Integer max,
                                                  @QueryParam("search") String search,
                                                  @QueryParam("status") MemberStatusEnum status,
                                                  @QueryParam("groups") String groupids){
        return userGroupMembershipExtensionRepository.searchByAdminGroups(Arrays.asList(groupids.split(",")), search, status, first, max);
    }

    @GET
    @Path("/enroll-requests")
    @Produces("application/json")
    public GroupEnrollmentRequestPager getAdminEnrollments(@QueryParam("first") @DefaultValue("0") Integer first,
                                                           @QueryParam("max") @DefaultValue("10") Integer max,
                                                           @QueryParam("groupId") String groupId,
                                                           @QueryParam("userSearch") String userSearch,
                                                           @QueryParam("status") EnrollmentRequestStatusEnum status) {
        List<String> groupIds = groupId != null ? Stream.of(groupId).collect(Collectors.toList()):groupAdminRepository.getAllAdminGroupIds(groupAdmin.getId());
        return groupEnrollmentRequestRepository.groupAdminEnrollmentPager(groupIds, userSearch, status, first, max);
    }

    @Path("/enroll-request/{enrollId}")
    public GroupAdminEnrollementRequest enrollment(@PathParam("enrollId") String enrollId) {
        GroupEnrollmentRequestEntity entity = groupEnrollmentRequestRepository.getEntity(enrollId);
        if (entity == null) {
            throw new NotFoundException("Could not find Group Enrollment Request by id");
        }
        GroupModel group = realm.getGroupById(entity.getGroupEnrollmentConfiguration().getGroup().getId());
        if (group == null) {
            throw new NotFoundException("Could not find the group of Group Enrollment Request by id");
        }
        if (!groupAdminRepository.isGroupAdmin(groupAdmin.getId(), group)){
            throw new ForbiddenException();
        }

        GroupAdminEnrollementRequest service = new GroupAdminEnrollementRequest(session, realm, groupEnrollmentRequestRepository, groupAdmin, entity, adminEvent, userGroupMembershipExtensionRepository);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
