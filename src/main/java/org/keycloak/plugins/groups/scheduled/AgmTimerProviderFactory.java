package org.keycloak.plugins.groups.scheduled;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProviderFactory;

public class AgmTimerProviderFactory implements TimerProviderFactory {

    private Timer timer;

    private int transactionTimeout;

    public static final String TRANSACTION_TIMEOUT = "transactionTimeout";

    private ConcurrentMap<String, AgmTimerTaskContextImpl> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public TimerProvider create(KeycloakSession session) {
        return new AgmTimerProvider(session, timer, transactionTimeout, this);
    }

    @Override
    public void init(Config.Scope config) {
        transactionTimeout = config.getInt(TRANSACTION_TIMEOUT, 0);
        timer = new Timer();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {
        timer.cancel();
        timer = null;
    }

    @Override
    public String getId() {
        return "agm";
    }

    protected AgmTimerTaskContextImpl putTask(String taskName, AgmTimerTaskContextImpl task) {
        return scheduledTasks.put(taskName, task);
    }

    protected AgmTimerTaskContextImpl removeTask(String taskName) {
        return scheduledTasks.remove(taskName);
    }

}
