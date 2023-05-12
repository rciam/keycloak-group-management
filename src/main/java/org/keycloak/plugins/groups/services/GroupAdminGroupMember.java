package org.keycloak.plugins.groups.services;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.keycloak.email.EmailException;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.email.CustomFreeMarkerEmailTemplateProvider;
import org.keycloak.plugins.groups.helpers.Utils;
import org.keycloak.plugins.groups.jpa.entities.EduPersonEntitlementConfigurationEntity;
import org.keycloak.plugins.groups.jpa.entities.GroupRolesEntity;
import org.keycloak.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.keycloak.plugins.groups.jpa.repositories.EduPersonEntitlementConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.services.ServicesLogger;

public class GroupAdminGroupMember {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel voAdmin;
    private GroupModel group;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider;

    private final EduPersonEntitlementConfigurationRepository eduPersonEntitlementConfigurationRepository;
    private final UserGroupMembershipExtensionEntity member;

    public GroupAdminGroupMember(KeycloakSession session, RealmModel realm, UserModel voAdmin, UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository, GroupModel group, CustomFreeMarkerEmailTemplateProvider customFreeMarkerEmailTemplateProvider, UserGroupMembershipExtensionEntity member, GroupRolesRepository groupRolesRepository) {
        this.session = session;
        this.realm =  realm;
        this.voAdmin = voAdmin;
        this.group = group;
        this.userGroupMembershipExtensionRepository = userGroupMembershipExtensionRepository;
        this.groupRolesRepository = groupRolesRepository;
        this.eduPersonEntitlementConfigurationRepository =  new EduPersonEntitlementConfigurationRepository(session);
        this.customFreeMarkerEmailTemplateProvider = customFreeMarkerEmailTemplateProvider;
        this.member = member;
    }

    @POST
    @Path("/role")
    public Response addGroupRole(@QueryParam("name") String name) {
        GroupRolesEntity role = groupRolesRepository.getGroupRolesByNameAndGroup(name, group.getId());
        if (role == null )
            throw new NotFoundException(" This role does not exist in this group");
        if (member.getGroupRoles() == null) {
            member.setGroupRoles(Stream.of(role).collect(Collectors.toList()));
        } else if (! member.getGroupRoles().stream().anyMatch(x -> role.getId().equals(x.getId()))) {
            member.getGroupRoles().add(role);
        }
        userGroupMembershipExtensionRepository.update(member);
        try {
            EduPersonEntitlementConfigurationEntity eduPersonEntitlement = eduPersonEntitlementConfigurationRepository.getByRealm(realm.getId());
            UserModel user = session.users().getUserById(realm, member.getUser().getId());
            List<String> eduPersonEntitlementValues = user.getAttribute(eduPersonEntitlement.getUserAttribute());
            String groupName = Utils.getGroupNameForEdupersonEntitlement(member.getGroup(), realm);
            eduPersonEntitlementValues.add(Utils.createEdupersonEntitlement(groupName, name, eduPersonEntitlement.getUrnNamespace(), eduPersonEntitlement.getAuthority()));
            user.setAttribute(eduPersonEntitlement.getUserAttribute(),eduPersonEntitlementValues);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/role/{name}")
    public Response deleteGroupRole(@PathParam("name") String name) {
        if (member.getGroupRoles() == null || member.getGroupRoles().stream().noneMatch(x -> name.equals(x.getName())))
            throw new NotFoundException("Could not find this user group member role");

        member.getGroupRoles().removeIf(x -> name.equals(x.getName()));
        userGroupMembershipExtensionRepository.update(member);
        try {
            EduPersonEntitlementConfigurationEntity eduPersonEntitlement = eduPersonEntitlementConfigurationRepository.getByRealm(realm.getId());
            UserModel user = session.users().getUserById(realm, member.getUser().getId());
            List<String> eduPersonEntitlementValues = user.getAttribute(eduPersonEntitlement.getUserAttribute());
            String groupName = Utils.getGroupNameForEdupersonEntitlement(member.getGroup(), realm);
            eduPersonEntitlementValues.removeIf(x-> x.startsWith(eduPersonEntitlement.getUrnNamespace()+Utils.groupStr+groupName+Utils.roleStr+name));
            user.setAttribute(eduPersonEntitlement.getUserAttribute(),eduPersonEntitlementValues);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/suspend")
    public Response suspendUser(@QueryParam("justification") String justification) {
        UserModel user = userGroupMembershipExtensionRepository.getUserModel(session, member.getUser());
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        }
        try {
            userGroupMembershipExtensionRepository.suspendUser(user, member, justification, group);
            try {
                EduPersonEntitlementConfigurationEntity eduPersonEntitlement = eduPersonEntitlementConfigurationRepository.getByRealm(realm.getId());
                List<String> eduPersonEntitlementValues = user.getAttribute(eduPersonEntitlement.getUserAttribute());
                String groupName = Utils.getGroupNameForEdupersonEntitlement(member.getGroup(), realm);
                eduPersonEntitlementValues.removeIf(x-> x.startsWith(eduPersonEntitlement.getUrnNamespace()+Utils.groupStr+groupName));
                user.setAttribute(eduPersonEntitlement.getUserAttribute(),eduPersonEntitlementValues);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("problem suspended group member");
        }
        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendSuspensionEmail(group.getName(), justification);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/activate")
    public Response activateUser(@QueryParam("justification") String justification) {
        UserModel user = userGroupMembershipExtensionRepository.getUserModel(session, member.getUser());
        if (user == null) {
            throw new NotFoundException("Could not find this User");
        }
        try {
            userGroupMembershipExtensionRepository.activateUser(user, member, justification, group);
            try {
                EduPersonEntitlementConfigurationEntity eduPersonEntitlement = eduPersonEntitlementConfigurationRepository.getByRealm(realm.getId());
                List<String> eduPersonEntitlementValues = user.getAttribute(eduPersonEntitlement.getUserAttribute());
                String groupName = Utils.getGroupNameForEdupersonEntitlement(member.getGroup(), realm);
                eduPersonEntitlementValues.removeIf(x-> x.startsWith(eduPersonEntitlement.getUrnNamespace()+Utils.groupStr+groupName));
                if (member.getGroupRoles() == null || member.getGroupRoles().isEmpty()) {
                    eduPersonEntitlementValues.add(Utils.createEdupersonEntitlement(groupName, null, eduPersonEntitlement.getUrnNamespace(), eduPersonEntitlement.getAuthority()));
                } else {
                    eduPersonEntitlementValues.addAll(member.getGroupRoles().stream().map(role -> {
                        try {
                            return Utils.createEdupersonEntitlement(groupName, role.getName(), eduPersonEntitlement.getUrnNamespace(), eduPersonEntitlement.getAuthority());
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList()));
                }
                user.setAttribute(eduPersonEntitlement.getUserAttribute(),eduPersonEntitlementValues);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException("problem activate group member");
        }
        try {
            customFreeMarkerEmailTemplateProvider.setUser(user);
            customFreeMarkerEmailTemplateProvider.sendActivationEmail(group.getName(), justification);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }
        return Response.noContent().build();
    }

}
