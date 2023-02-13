package org.keycloak.plugins.groups.jpa;

import javax.transaction.Transactional;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.plugins.groups.jpa.repositories.GroupAdminRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupEnrollmentRequestRepository;
import org.keycloak.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;

public class GeneralJpaService {

    private final RealmModel realm;
    private KeycloakSession session;
    private final GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository;
    private final GroupAdminRepository groupAdminRepository;
    private final UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository;
    private final GroupEnrollmentRequestRepository groupEnrollmentRequestRepository;
    private final GroupRolesRepository groupRolesRepository;

    public GeneralJpaService(KeycloakSession session, RealmModel realm, GroupEnrollmentConfigurationRepository groupEnrollmentConfigurationRepository) {
        this.realm = realm;
        this.session = session;
        this.groupEnrollmentConfigurationRepository = groupEnrollmentConfigurationRepository;
        this.groupAdminRepository = new GroupAdminRepository(session, realm);
        this.userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm);
        this.groupEnrollmentRequestRepository = new GroupEnrollmentRequestRepository(session, realm, null);
        this.groupRolesRepository = new GroupRolesRepository(session, realm);

    }

    @Transactional
    public void removeGroup(GroupModel group) {
        //extra delete UserGroupMembershipExtensionEntity, GroupEnrollmentConfigurationEntity, GroupAdminEntity, GroupEnrollmentRequestEntity
        groupEnrollmentRequestRepository.deleteByGroup(group.getId());
        userGroupMembershipExtensionRepository.deleteByGroup(group.getId());
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
}
