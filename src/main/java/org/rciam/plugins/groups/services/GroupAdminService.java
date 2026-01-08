package org.rciam.plugins.groups.services;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import jakarta.ws.rs.core.Response;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.GroupPermissionEvaluator;
import org.keycloak.utils.GroupUtils;
import org.rciam.plugins.groups.enums.EnrollmentRequestStatusEnum;
import org.rciam.plugins.groups.enums.GroupTypeEnum;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.PagerParameters;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.GeneralJpaService;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRulesRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.GroupEnrollmentConfigurationRulesRepresentation;
import org.rciam.plugins.groups.representations.GroupEnrollmentRequestPager;
import org.rciam.plugins.groups.representations.GroupRepresentation;
import org.rciam.plugins.groups.representations.GroupsPager;
import org.rciam.plugins.groups.representations.UserRepresentationPager;
import org.keycloak.services.resources.admin.AdminEventBuilder;

public class GroupAdminService {

    private KeycloakSession session;
    private final ClientConnection clientConnection;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final AdminEventBuilder adminEvent;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;

    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GeneralJpaService generalJpaService;

    public GroupAdminService(KeycloakSession session, RealmModel realm, UserModel user, AdminEventBuilder adminEvent, ClientConnection clientConnection) {
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
        this.clientConnection = clientConnection;
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
            List<GroupRepresentation> results = populateGroupHierarchyFromSubGroups(session.groups().searchForGroupByNameStream(realm, search.trim(), exact, first, max));
            Long count = session.groups().searchForGroupByNameStream(realm, search.trim(), exact, null, null).count();
            return new GroupsPager(results, count);
        } else if (Objects.nonNull(search)) {
            List<GroupRepresentation> results = session.groups().searchForGroupByNameStream(realm, search.trim(), exact, first, max).map(g -> org.rciam.plugins.groups.helpers.ModelToRepresentation.toSimpleGroupHierarchy(g, true)).collect(Collectors.toList());
            Long count = realm.getGroupsCountByNameContaining(search);
            return new GroupsPager(results, count);
        } else {
            List<GroupRepresentation> results = session.groups().getTopLevelGroupsStream(realm, first, max).map(g -> org.rciam.plugins.groups.helpers.ModelToRepresentation.toSimpleGroupHierarchy(g, true)).collect(Collectors.toList());
            return new GroupsPager(results, realm.getGroupsCount(true));
        }
    }

    private List<GroupRepresentation> populateGroupHierarchyFromSubGroups(Stream<GroupModel> groups) {
        Map<String, GroupRepresentation> groupIdToGroups = new HashMap<>();
        groups.forEach(group -> {

           GroupRepresentation currGroup = org.rciam.plugins.groups.helpers.ModelToRepresentation.toSimpleGroupHierarchy(group, true);
           currGroup.setParentId(group.getParentId());
           groupIdToGroups.putIfAbsent(currGroup.getId(), currGroup);

            while(currGroup.getParentId() != null) {
                GroupModel parentModel = session.groups().getGroupById(realm, currGroup.getParentId());

                GroupRepresentation parent = groupIdToGroups.computeIfAbsent(currGroup.getParentId(),
                        id -> org.rciam.plugins.groups.helpers.ModelToRepresentation.toSimpleGroupHierarchy(parentModel, true));

                GroupRepresentation finalCurrGroup = currGroup;
                groupIdToGroups.remove(currGroup.getId());
                currGroup = parent;
            }
        });
        return groupIdToGroups.values().stream().sorted(Comparator.comparing(GroupRepresentation::getName)).toList();
    }

    @POST
    @Path("/group")
    public Response createTopLevelGroup(org.keycloak.representations.idm.GroupRepresentation rep) {

        if (!Utils.hasManageGroupsAccountRole(realm, groupAdmin)){
            throw new ForbiddenException("You could not create a top-level group");
        }
        return generalJpaService.addTopLevelGroup(rep, adminEvent);
    }


    @Path("/group/{groupId}")
    public GroupAdminGroup group(@PathParam("groupId") String groupId) {

        var group = realm.getGroupById(groupId);
        boolean isGroupAdmin = groupAdminRepository.isGroupAdmin(groupAdmin.getId(), group);
        if (!isGroupAdmin && !Utils.hasManageGroupsAccountRole(realm, groupAdmin)){
            throw new ErrorResponseException(Utils.NOT_ALLOWED, Utils.NOT_ALLOWED, Response.Status.FORBIDDEN);
        }
        if (group == null) {
            throw new ErrorResponseException("Could not find group by id", "Could not find group by id", Response.Status.NOT_FOUND);
        }

        return new GroupAdminGroup(session, realm, groupAdmin, group, userGroupMembershipExtensionRepository, groupAdminRepository, groupEnrollmentRequestRepository, groupEnrollmentConfigurationRulesRepository, adminEvent, isGroupAdmin);
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
            throw new ErrorResponseException(Utils.NOT_ALLOWED, Utils.NOT_ALLOWED, Response.Status.FORBIDDEN);
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

        var entity = groupEnrollmentRequestRepository.getEntity(enrollId);
        if (entity == null) {
            throw new ErrorResponseException("Could not find Group Enrollment Request by id", "Could not find Group Enrollment Request by id", Response.Status.NOT_FOUND);
        }
        var group = realm.getGroupById(entity.getGroupEnrollmentConfiguration().getGroup().getId());
        if (group == null) {
            throw new ErrorResponseException("Could not find the group of Group Enrollment Request by id", "Could not find the group of Group Enrollment Request by id", Response.Status.NOT_FOUND);
        }
        if (!groupAdminRepository.isGroupAdmin(groupAdmin.getId(), group)){
            throw new ErrorResponseException(Utils.NOT_ALLOWED, Utils.NOT_ALLOWED, Response.Status.FORBIDDEN);
        }

        return new GroupAdminEnrollementRequest(session, realm, groupEnrollmentRequestRepository, groupAdmin, entity, userGroupMembershipExtensionRepository, groupAdminRepository, clientConnection);
    }

}
