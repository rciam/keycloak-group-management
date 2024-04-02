package org.rciam.plugins.groups.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.rciam.plugins.groups.jpa.repositories.GroupInvitationRepository;
import org.keycloak.timer.ScheduledTask;

public class DeleteExpiredInvitationTask implements ScheduledTask {

    private final String id;
    private final String realmId;

    public DeleteExpiredInvitationTask(String id, String realmId){
        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm != null) {
            GroupInvitationRepository repository = new GroupInvitationRepository(session, realm);
            repository.deleteEntity(id);
        }
    }
}
