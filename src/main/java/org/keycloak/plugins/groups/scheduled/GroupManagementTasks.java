package org.keycloak.plugins.groups.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.timer.ScheduledTask;

public class GroupManagementTasks implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        UserGroupMembershipExtensionRepository repository = new UserGroupMembershipExtensionRepository(session, null);
        repository.dailyExecutedActions(session);
    }
}
