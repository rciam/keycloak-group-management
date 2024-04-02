package org.rciam.plugins.groups.scheduled;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.basic.TimerTaskContextImpl;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BasicAgmTimerProviderFactory implements AgmTimerProviderFactory {

    private Timer timer;

    private int transactionTimeout;

    public static final String TRANSACTION_TIMEOUT = "transactionTimeout";

    private ConcurrentMap<String, AgmTimerTaskContextImpl> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public AgmTimerProvider create(KeycloakSession session) {
        return new BasicAgmTimerProvider(session, timer, transactionTimeout, this);
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
        return "basic";
    }

    protected AgmTimerTaskContextImpl putTask(String taskName, AgmTimerTaskContextImpl task) {
        return scheduledTasks.put(taskName, task);
    }

    protected AgmTimerTaskContextImpl removeTask(String taskName) {
        return scheduledTasks.remove(taskName);
    }

}
