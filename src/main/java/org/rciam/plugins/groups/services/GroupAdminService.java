package org.rciam.plugins.groups.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.rciam.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.rciam.plugins.groups.enums.GroupTypeEnum;
import org.rciam.plugins.groups.enums.MemberStatusEnum;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.PagerParameters;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.GeneralJpaService;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentRequestEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRulesRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRulesRepresentation;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestPager;
import org.rciam.plugins.groups.representations.GroupsPager;
import org.rciam.plugins.groups.representations.UserRepresentationPager;
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

    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GeneralJpaService generalJpaService;

    public GroupAdminService(KeycloakSession session, RealmModel realm, UserModel user, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm =  realm;
        this.groupAdmin = user;
        this.adminEvent = adminEvent;
        this.groupAdminRepository =  new GroupAdminRepository(session, realm);
        this.groupEnrollmentRequestRepository =  new GroupEnrollmentRequestRepository(session, realm, new GroupRolesRepository(session, realm));
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
        this.groupEnrollmentConfigurationRulesRepository = new GroupEnrollmentConfigurationRulesRepository(session);
        this.groupEnrollmentConfigurationRepository = new GroupEnrollmentConfigurationRepository(session, realm);
        this.generalJpaService = new GeneralJpaService(session, realm, new GroupEnrollmentConfigurationRepository(session, realm));
    }


    @GET
    @Path("/groups")
    public GroupsPager getGroupAdminGroups(@QueryParam("search") String search,
                                           @QueryParam("toplevel") @DefaultValue("true") boolean toplevel,
                                           @QueryParam("exact") @DefaultValue("false") boolean exact,
                                           @QueryParam("first") @DefaultValue("0") Integer first,
                                           @QueryParam("max") @DefaultValue("10") Integer max){
        if (Utils.hasManageGroupsAccountRole(realm, groupAdmin)){
            return getAllGroups( search, first, max, toplevel, exact);
        } else {
            return groupAdminRepository.getAdminGroups(groupAdmin.getId(), search, first, max, exact);
        }
    }

    private GroupsPager getAllGroups(String search, Integer first, Integer max, boolean toplevel, boolean exact) {
        if (Objects.nonNull(search) && toplevel) {
            List<GroupRepresentation> results = ModelToRepresentation.searchForGroupModelByName(session, realm, false, search.trim(), exact, first, max).map(g -> org.rciam.plugins.groups.helpers.ModelToRepresentation.toSimpleGroupHierarchy(g, true)).collect(Collectors.toList());
            Long count = ModelToRepresentation.searchForGroupModelByName(session, realm, false, search.trim(), exact, null, null).count();
            return new GroupsPager(results, count);
        } else if (Objects.nonNull(search)) {
            return groupEnrollmentConfigurationRepository.searchForGroupByNameStream(search.trim(), exact, first, max);
        }else {
            List<GroupRepresentation> results = ModelToRepresentation.toGroupModelHierarchy(realm, false, first, max).map(g -> org.rciam.plugins.groups.helpers.ModelToRepresentation.toSimpleGroupHierarchy(g, true)).collect(Collectors.toList());
            return new GroupsPager(results, realm.getGroupsCount(true));
        }
    }

    @POST
    @Path("/group")
    public Response createTopLevelGroup(GroupRepresentation rep) {
        if (!Utils.hasManageGroupsAccountRole(realm, groupAdmin)){
            throw new ForbiddenException("You could not create a top-level group");
        }
        return generalJpaService.addTopLevelGroup(rep, adminEvent);
    }


    @Path("/group/{groupId}")
    public GroupAdminGroup group(@PathParam("groupId") String groupId) {
        GroupModel group = realm.getGroupById(groupId);
        boolean isGroupAdmin = groupAdminRepository.isGroupAdmin(groupAdmin.getId(), group);
        if (!isGroupAdmin && !Utils.hasManageGroupsAccountRole(realm, groupAdmin)){
            throw new ForbiddenException();
        }
        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }


        GroupAdminGroup service = new GroupAdminGroup(session, realm, groupAdmin, group, userGroupMembershipExtensionRepository, groupAdminRepository, groupEnrollmentRequestRepository, groupEnrollmentConfigurationRulesRepository, adminEvent, isGroupAdmin);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

    @GET
    @Path("/groupids/all")
    public List<String> getGroupIdsForAdmin(){
        return groupAdminRepository.getAllAdminGroupIds(groupAdmin.getId());
    }

    @GET
    @Path("/configuration-rules")
    @Produces("application/json")
    public  List<GroupEnrollmentConfigurationRulesRepresentation> getEnrollmentConfigurationRules(@QueryParam("type") @DefaultValue("TOP_LEVEL") GroupTypeEnum type) {
        if (!Utils.hasManageGroupsAccountRole(realm, groupAdmin) && !groupAdminRepository.hasAdminRights(groupAdmin.getId())){
            throw new ForbiddenException();
        }
        return groupEnrollmentConfigurationRulesRepository.getByRealmAndType(realm.getId(), type).map(EntityToRepresentation::toRepresentation).collect(Collectors.toList());
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
                                                  @QueryParam("serviceAccountClientLink") @DefaultValue("true") boolean serviceAccountClientLink,
                                                  @QueryParam("groups") String groupids){
        if (Utils.hasManageGroupsAccountRole(realm, groupAdmin)){
            Map<String, String> attributes = new HashMap<>();
            //for search add * in start and in end in order to be a like everywhere in string
            if (search != null && !search.isEmpty())
                attributes.put(UserModel.SEARCH, "*"+search.trim()+"*");
            if (!serviceAccountClientLink)
                attributes.put(UserModel.INCLUDE_SERVICE_ACCOUNT, "false");
            List<UserRepresentation> users = session.users().searchForUserStream(realm, attributes, first, max).map(user->org.rciam.plugins.groups.helpers.ModelToRepresentation.toBriefRepresentation(user, session, realm)).collect(Collectors.toList());
            int count = session.users().getUsersCount(realm, attributes);
            return new UserRepresentationPager(users, (long) count);
        } else {
            return userGroupMembershipExtensionRepository.searchByAdminGroups(Arrays.asList(groupids.split(",")), search, serviceAccountClientLink, first, max);
        }
    }

    @GET
    @Path("/enroll-requests")
    @Produces("application/json")
    public GroupEnrollmentRequestPager getAdminEnrollments(@QueryParam("first") @DefaultValue("0") Integer first,
                                                           @QueryParam("max") @DefaultValue("10") Integer max,
                                                           @QueryParam("groupName") String groupName,
                                                           @QueryParam("userSearch") String userSearch,
                                                           @QueryParam("status") EnrollmentRequestStatusEnum status,
                                                           @QueryParam("order") @DefaultValue("submittedDate") String order,
                                                           @QueryParam("asc") @DefaultValue("false") boolean asc) {
        List<String> groupIds = groupAdminRepository.getAdminGroupIdsByName(groupAdmin.getId(), groupName);
        return groupEnrollmentRequestRepository.groupAdminEnrollmentPager(groupIds, userSearch, status, new PagerParameters(first, max, Stream.of(order).collect(Collectors.toList()), asc ? "asc" : "desc"));
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

        GroupAdminEnrollementRequest service = new GroupAdminEnrollementRequest(session, realm, groupEnrollmentRequestRepository, groupAdmin, entity, userGroupMembershipExtensionRepository, groupAdminRepository);
        ResteasyProviderFactory.getInstance().injectProperties(service);
        return service;
    }

}
