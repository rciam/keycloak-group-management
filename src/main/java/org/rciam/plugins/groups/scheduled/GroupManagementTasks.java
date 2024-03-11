package org.rciam.plugins.groups.scheduled;

import org.keycloak.models.KeycloakSession;
import org.rciam.plugins.groups.jpa.repositories.UserGroupMembershipExtensionRepository;
import org.keycloak.timer.ScheduledTask;

public class GroupManagementTasks implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        UserGroupMembershipExtensionRepository repository = new UserGroupMembershipExtensionRepository(session, null);
        repository.dailyExecutedActions();
    }
}
