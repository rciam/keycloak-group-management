package org.rciam.plugins.groups.jpa;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.GroupResource;
import org.rciam.plugins.groups.helpers.EntityToRepresentation;
import org.rciam.plugins.groups.helpers.ModelToRepresentation;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.GroupRolesEntity;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.entities.UserGroupMembershipExtensionEntity;
import org.rciam.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.rciam.plugins.groups.representations.GroupRepresentation;

public class GeneralJpaService {

    private final RealmModel realm;
    private KeycloakSession session;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final GroupRolesRepository groupRolesRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository;

    public GeneralJpaService(KeycloakSession session, RealmModel realm, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository) {
        this.realm = realm;
        this.session = session;
        this.groupRolesRepository = new GroupRolesRepository(session, realm);
        this.groupEnrollmentConfigurationRepository = groupEnrollmentConfigurationRepository;
        groupEnrollmentConfigurationRepository.setGroupRolesRepository(groupRolesRepository);
        this.groupAdminRepository = new GroupAdminRepository(session, realm);
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
        this.groupEnrollmentRequestRepository = new GroupEnrollmentRequestRepository(session, realm, null);
        this.groupInvitationRepository = new GroupInvitationRepository(session, realm);
        this.memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);

    }

    @Transactional
    public void removeGroup(GroupModel group, UserModel groupAdmin, ClientConnection clientConnection) {
        //extra delete UserGroupMembershipExtensionEntity, GroupEnrollmentConfigurationEntity, GroupAdminEntity, GroupEnrollmentRequestEntity
        groupEnrollmentRequestRepository.deleteByGroup(group.getId());
        groupInvitationRepository.deleteByGroup(group.getId());
        Stream<UserGroupMembershipExtensionEntity> members = userGroupMembershipExtensionRepository.getByGroup(group.getId());
        members.forEach(member -> {
            MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
            UserModel user = session.users().getUserById(realm, member.getUser().getId());
            userGroupMembershipExtensionRepository.deleteMember(member, group, user, clientConnection, groupAdmin.getId(), memberUserAttribute);
        });
        groupEnrollmentConfigurationRepository.deleteByGroup(group.getId());
        groupAdminRepository.deleteByGroup(group.getId());
        groupRolesRepository.deleteByGroup(group.getId());

        realm.removeGroup(group);
    }

    @Transactional
    public boolean removeUser(UserModel user) {

        try {
            //extra delete UserGroupMembershipExtensionEntity, GroupAdminEntity, GroupEnrollmentRequestEntity
            //admin sto GroupEnrollmentRequestEntity set null
            groupEnrollmentRequestRepository.deleteByUser(user.getId());
            userGroupMembershipExtensionRepository.deleteByUser(user.getId());
            groupAdminRepository.deleteByUser(user.getId());
            if (session.users().removeUser(realm, user)) {
                session.getKeycloakSessionFactory().publish(new UserModel.UserRemovedEvent() {

                    @Override
                    public RealmModel getRealm() {
                        return realm;
                    }

                    @Override
                    public UserModel getUser() {
                        return user;
                    }

                    @Override
                    public KeycloakSession getKeycloakSession() {
                        return session;
                    }

                });
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public GroupRepresentation getAllGroupInfo( GroupModel group) throws UnsupportedEncodingException {
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());
        String groupNameUserAttribute = Utils.getGroupNameForMemberUserAttribute(group);
        GroupRepresentation rep = ModelToRepresentation.toRepresentationWithAttributes(group);
        rep.setGroupRoles(groupRolesRepository.getGroupRolesByGroup(group.getId()).collect(Collectors.toMap(GroupRolesEntity::getName, role -> {
            try {
                return Utils.createMemberUserAttribute(groupNameUserAttribute, role.getName(), memberUserAttribute.getUrnNamespace(), memberUserAttribute.getAuthority());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        })));
        rep.setEnrollmentConfigurationList(groupEnrollmentConfigurationRepository.getByGroup(group.getId()).map(x -> EntityToRepresentation.toRepresentation(x, false, realm)).collect(Collectors.toList()));
        rep.setAdmins(groupAdminRepository.getAdminsForGroup(group));
        return rep;
    }

    public List<GroupEntity> getGroupByName(String name){
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return em.createQuery("from GroupEntity f where f.name = :name",GroupEntity.class).setParameter("name",name).getResultList();
    }

    public Response addTopLevelGroup(org.keycloak.representations.idm.GroupRepresentation rep, AdminEventBuilder adminEvent) {

        //method based on GroupsResource.addTopLevelGroup (any upgrade changes need to be passed)
        //with extra create default role and default configuration

        GroupModel child;
        Response.ResponseBuilder builder = Response.status(204);
        String groupName = rep.getName();

        if (ObjectUtil.isBlank(groupName)) {
            throw ErrorResponse.error("Group name is missing", Response.Status.BAD_REQUEST);
        }

        try {
            child = realm.createGroup(groupName);
            GroupResource.updateGroup(rep, child, realm, session);
            URI uri = session.getContext().getUri().getAbsolutePathBuilder()
                    .path(child.getId()).build();
            builder.status(201).location(uri);

            rep.setId(child.getId());

            //create defaults group
            if (groupEnrollmentConfigurationRepository.getByGroup(rep.getId()).collect(Collectors.toList()).isEmpty()) {
                //group creation - group configuration no exist
                groupRolesRepository.create(Utils.defaultGroupRole,rep.getId());
                groupEnrollmentConfigurationRepository.createDefault(realm.getGroupById(rep.getId()), rep.getName(), realm.getId());
            }

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), child.getId());
        } catch (ModelDuplicateException mde) {
            throw ErrorResponse.exists("Top level group named '" + groupName + "' already exists.");
        }

        adminEvent.representation(rep).success();
        return builder.build();
    }
}
