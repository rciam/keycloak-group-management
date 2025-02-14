package org.rciam.plugins.groups.scheduled;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.rciam.plugins.groups.helpers.Utils;
import org.rciam.plugins.groups.jpa.entities.MemberUserAttributeConfigurationEntity;
import org.rciam.plugins.groups.jpa.repositories.MemberUserAttributeConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupEnrollmentConfigurationRepository;
import org.rciam.plugins.groups.jpa.repositories.GroupRolesRepository;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.timer.ScheduledTask;

public class MemberUserAttributeCalculatorTask implements ScheduledTask {

    private static final Logger logger = Logger.getLogger(MemberUserAttributeCalculatorTask.class);

    private final String realmId;

    public MemberUserAttributeCalculatorTask(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm(realmId);
        MemberUserAttributeConfigurationRepository memberUserAttributeConfigurationRepository = new MemberUserAttributeConfigurationRepository(session);
        UserGroupMembershipExtensionRepository userGroupMembershipExtensionRepository = new UserGroupMembershipExtensionRepository(session, realm, new GroupEnrollmentConfigurationRepository(session, realm), new GroupRolesRepository(session, realm));
        MemberUserAttributeConfigurationEntity memberUserAttribute = memberUserAttributeConfigurationRepository.getByRealm(realm.getId());

        logger.info("Strarting calculating " + memberUserAttribute.getUserAttribute() + " for all " + realm.getName() + " realm users.");

        session.users().searchForUserStream(realm, new HashMap<>()).forEach(user -> {
            userGroupMembershipExtensionRepository.changeUserAttributeValue(user, memberUserAttribute);
        });

        logger.info("Finish calculating " + memberUserAttribute.getUserAttribute() + " for all " + realm.getName() + " realm users.");
    }
}
