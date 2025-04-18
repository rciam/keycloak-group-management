package org.rciam.plugins.groups.services;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.hibernate.StaleObjectStateException;
import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.rciam.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.rciam.plugins.groups.enums.GroupTypeEnum;
import org.rciam.plugins.groups.enums.MemberStatusEnum;
import org.rciam.plugins.groups.helpers.LoginEventHelper;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupEnrollmentConfigurationRulesEntity;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.GroupRolesEntity;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRulesRepository;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.UserGroupMembershipExtensionRepresentation;
import org.keycloak.services.ServicesLogger;

public class GroupAdminGroupMember {

    @Context
    private ClientConnection clientConnection;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel groupAdmin;
    private GroupModel group;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final GroupEnrollmentConfigurationRulesRepository groupEnrollmentConfigurationRulesRepository;
    private final UserGroupMembershipExtensionEntity member;
    private final boolean isGroupAdmin;

    public GroupAdminGroupMember(KeycloakSession session, RealmModel realm, UserModel groupAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, UserGroupMembershipExtensionEntity member, GroupRolesRepository groupRolesRepository, GroupAdminRepository groupAdminRepository,boolean isGroupAdmin) {
        this.session = session;
        this.realm = realm;
        this.groupAdmin = groupAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.groupRolesRepository = groupRolesRepository;
        this.groupAdminRepository = groupAdminRepository;
        this.memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.groupEnrollmentConfigurationRulesRepository = new GroupEnrollmentConfigurationRulesRepository(session);
        this.member = member;
        this.isGroupAdmin = isGroupAdmin;
    }

    @PUT
    @Consumes("application/json")
    public Response updateMember(UserGroupMembershipExtensionRepresentation rep) {
        //validation tasks
        //1. active member
        //2. at least one role
        //3. Member since: date cannot be in the past or after expiration for PENDING members
        //4. Expiration date: date cannot be in the past
        boolean extendedRole = Utils.hasManageExtendedGroupsAccountRole(realm, groupAdmin);
        if (!extendedRole && MemberStatusEnum.SUSPENDED.equals(member.getStatus())) {
            throw new ErrorResponseException("Suspended members can not be updated", "Suspended members can not be updated", Response.Status.BAD_REQUEST);
        } else if (rep.getGroupRoles() == null || rep.getGroupRoles().isEmpty()) {
            throw new ErrorResponseException("At least one role must be existed", "At least one role must be existed", Response.Status.BAD_REQUEST);
        } else if (rep.getMembershipExpiresAt() != null && LocalDate.now().isAfter(rep.getMembershipExpiresAt())) {
            throw new ErrorResponseException("Expiration date must not be in the past", "Expiration date must not be in the past", Response.Status.BAD_REQUEST);
        } else if (MemberStatusEnum.PENDING.equals(member.getStatus()) && ( rep.getValidFrom() == null || LocalDate.now().isBefore(rep.getValidFrom()) || (rep.getMembershipExpiresAt() != null && rep.getMembershipExpiresAt().isBefore(rep.getValidFrom())))) {
            throw new ErrorResponseException("Member since must not be in the past or after expiration date", "Member since must not be in the past or after expiration date", Response.Status.BAD_REQUEST);
        } else if (!extendedRole && MemberStatusEnum.ENABLED.equals(member.getStatus())) {
            //For enabled member do to change valid from
            rep.setValidFrom(member.getValidFrom());
        }
        GroupEnrollmentConfigurationRulesEntity configurationRule = groupEnrollmentConfigurationRulesRepository.getByRealmAndTypeAndField(realm.getId(), member.getGroup().getParentId().trim().isEmpty() ? GroupTypeEnum.TOP_LEVEL : GroupTypeEnum.SUBGROUP, "membershipExpirationDays");
        if (!extendedRole && configurationRule != null && configurationRule.getRequired() && rep.getMembershipExpiresAt() == null) {
            throw new ErrorResponseException("Expiration date must not be empty", "Expiration date must not be empty", Response.Status.BAD_REQUEST);
        } else if (configurationRule != null && configurationRule.getMax() != null && MemberStatusEnum.PENDING.equals(member.getStatus()) && rep.getValidFrom().plusDays(Long.valueOf(configurationRule.getMax())).isBefore(rep.getMembershipExpiresAt())) {
            throw new ErrorResponseException("Membership can not last more than "+ configurationRule.getMax() + " days", "Membership can not last more than "+ configurationRule.getMax() + " days", Response.Status.BAD_REQUEST);
        }

        if (!extendedRole && configurationRule != null && configurationRule.getMax() != null && MemberStatusEnum.ENABLED.equals(member.getStatus()) && LocalDate.now().plusDays(Long.valueOf(configurationRule.getMax())).isBefore(rep.getMembershipExpiresAt())) {
            throw new ErrorResponseException("Membership can not last more than "+ configurationRule.getMax() + " days", "Membership can not last more than "+ configurationRule.getMax() + " days", Response.Status.BAD_REQUEST);
        }


        try {

            userGroupMembershipExtensionRepository.update(rep, member, group, session, groupAdmin, clientConnection);
            String groupPath = ModelToRepresentation.buildGroupPath(group);

            UserModel memberUser = session.users().getUserById(realm, member.getUser().getId());
            customFreeMarkerEmailTemplateProvider.setUser(memberUser);
            customFreeMarkerEmailTemplateProvider.sendMemberUpdateUserInformEmail(groupPath, groupAdmin, rep.getValidFrom(), rep.getMembershipExpiresAt(), rep.getGroupRoles());

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendMemberUpdateAdminInformEmail(groupPath, memberUser, groupAdmin, rep.getValidFrom(), rep.getMembershipExpiresAt(), rep.getGroupRoles());
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        } catch (ModelException | OptimisticLockException | StaleObjectStateException e) {
            e.printStackTrace();
            return Response.status(Response.Status.CONFLICT).entity(String.format("Concurrent modification detected: conflicting group membership update for user %s in group %s.", member.getUser().getUsername(), group.getName())).build();
        }
        return Response.noContent().build();
    }


    @DELETE
    public Response deleteMember() {
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        String groupPath = ModelToRepresentation.buildGroupPath(group);
        userGroupMembershipExtensionRepository.deleteMember(member, group, user, clientConnection, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId()), memberUserAttribute, false);
        LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                , Utils.GROUP_MEMBERSHIP_DELETE, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()), member.getMembershipExpiresAt());

        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendRemoveMemberEmail(groupPath, groupAdmin);

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendRemoveMemberAdminInformationEmail(group.getId(), groupPath, groupAdmin, user);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/role")
    public Response addGroupRole(@QueryParam("name") String name) {
        if (!isGroupAdmin){
            throw new ErrorResponseException(Utils.NOT_ALLOWED, Utils.NOT_ALLOWED, Response.Status.FORBIDDEN);
        }

        GroupRolesEntity role = groupRolesRepository.getGroupRolesByNameAndGroup(name, group.getId());
        if (role == null) {
            throw new ErrorResponseException("This role does not exist in this group", "This role does not exist in this group", Response.Status.NOT_FOUND);
        }
        if (member.getGroupRoles() == null) {
            member.setGroupRoles(Stream.of(role).collect(Collectors.toSet()));
        } else if (member.getGroupRoles().stream().noneMatch(x -> role.getId().equals(x.getId()))) {
            member.getGroupRoles().add(role);
        }  else {
            throw new ErrorResponseException("This role already exists.", "This role already exists.", Response.Status.BAD_REQUEST);
        }

        userGroupMembershipExtensionRepository.update(member);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        userGroupMembershipExtensionRepository.changeUserAttributeValue(user, memberUserAttribute);

        String groupPath = ModelToRepresentation.buildGroupPath(group);
        LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                , Utils.GROUP_MEMBERSHIP_UPDATE, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()), member.getMembershipExpiresAt());

        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendRolesChangesUserEmail(groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()));

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendRolesChangesGroupAdminEmail(groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()), groupAdmin, user);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }

        return Response.noContent().build();
    }

    @DELETE
    @Path("/role/{name}")
    public Response deleteGroupRole(@PathParam("name") String name) {
        if (!isGroupAdmin){
            throw new ErrorResponseException(Utils.NOT_ALLOWED, Utils.NOT_ALLOWED, Response.Status.FORBIDDEN);
        }

        if (member.getGroupRoles() == null || member.getGroupRoles().stream().noneMatch(x -> name.equals(x.getName())))
            throw new ErrorResponseException("Could not find this user group member role", "Could not find this user group member role", Response.Status.NOT_FOUND);

        member.getGroupRoles().removeIf(x -> name.equals(x.getName()));
        userGroupMembershipExtensionRepository.update(member);
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        userGroupMembershipExtensionRepository.changeUserAttributeValue(user, memberUserAttribute);
        String groupPath = ModelToRepresentation.buildGroupPath(group);
        LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                , Utils.GROUP_MEMBERSHIP_UPDATE, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()), member.getMembershipExpiresAt());

        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendRolesChangesUserEmail(groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()));

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendRolesChangesGroupAdminEmail(groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()), groupAdmin, user);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }

        return Response.noContent().build();
    }

    @POST
    @Path("/suspend")
    public Response suspendUser(@QueryParam("justification") String justification) {
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        if (user == null) {
            throw new ErrorResponseException(Utils.NO_USER_FOUND, Utils.NO_USER_FOUND, Response.Status.NOT_FOUND);
        } else if (!MemberStatusEnum.ENABLED.equals(member.getStatus())) {
            throw new ErrorResponseException("Only enabled users can be suspended.", "Only enabled users can be suspended.", Response.Status.NOT_FOUND);
        }
        try {
            String groupPath = ModelToRepresentation.buildGroupPath(group);
            List<String> subgroupPaths = userGroupMembershipExtensionRepository.suspendUser(user, member, justification, group, memberUserAttributeConfigurationRepository);
            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , Utils.GROUP_MEMBERSHIP_SUSPEND, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()), member.getMembershipExpiresAt());

            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendSuspensionEmail(groupPath, subgroupPaths, justification);

            groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                try {
                    customFreeMarkerEmailTemplateProvider.setUser(admin);
                    customFreeMarkerEmailTemplateProvider.sendSuspensionEmailToAdmins(groupPath, subgroupPaths, justification, user, groupAdmin);
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.failedToSendEmail(e);
                }
            });
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ErrorResponseException("problem suspended group member", "problem suspended group member", Response.Status.BAD_REQUEST);
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/activate")
    public Response activateUser(@QueryParam("justification") String justification) {
        UserModel user = session.users().getUserById(realm, member.getUser().getId());
        if (user == null) {
            throw new ErrorResponseException(Utils.NO_USER_FOUND, Utils.NO_USER_FOUND, Response.Status.NOT_FOUND);
        } else if (!MemberStatusEnum.SUSPENDED.equals(member.getStatus())) {
            throw new ErrorResponseException("Only suspended users can be reactivated.", "Only suspended users can be reactivated.", Response.Status.BAD_REQUEST);
        }
        List<String> parentGroupIds = Utils.findParentGroupIds(group);
        if (!parentGroupIds.isEmpty() && userGroupMembershipExtensionRepository.countByUserAndGroupsAndSuspended(user.getId(),parentGroupIds) > 0) {
            throw new ErrorResponseException("Unable to reactivate membership because it's suspended in a higher-level group.", "Unable to reactivate membership because it's suspended in a higher-level group.", Response.Status.BAD_REQUEST);
        }

        try {
            String groupPath = ModelToRepresentation.buildGroupPath(group);
            List<String> subgroupPaths = userGroupMembershipExtensionRepository.reActivateUser(user, member, justification, group, memberUserAttributeConfigurationRepository);

            LoginEventHelper.createGroupEvent(realm, session, clientConnection, user, groupAdmin.getAttributeStream(Utils.VO_PERSON_ID).findAny().orElse(groupAdmin.getId())
                    , Utils.GROUP_MEMBERSHIP_CREATE, groupPath, member.getGroupRoles().stream().map(GroupRolesEntity::getName).collect(Collectors.toSet()), member.getMembershipExpiresAt());
                customFreeMarkerEmailTemplateProvider.setUser(user);
                customFreeMarkerEmailTemplateProvider.sendActivationEmail(groupPath, subgroupPaths, justification);

                groupAdminRepository.getAllAdminIdsGroupUsers(group).filter(x -> !groupAdmin.getId().equals(x)).map(id -> session.users().getUserById(realm, id)).forEach(admin -> {
                    try {
                        customFreeMarkerEmailTemplateProvider.setUser(admin);
                        customFreeMarkerEmailTemplateProvider.sendActivationEmailToAdmins(groupPath, subgroupPaths, justification, user, groupAdmin);
                    } catch (EmailException e) {
                        ServicesLogger.LOGGER.failedToSendEmail(e);
                    }
                });
        } catch (EmailException e) {
                ServicesLogger.LOGGER.failedToSendEmail(e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ErrorResponseException("problem activate group member", "problem activate group member", Response.Status.BAD_REQUEST);
        }

        return Response.noContent().build();
    }

}
