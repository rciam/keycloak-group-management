package org.keycloak.plugins.groups.scheduled;

import java.util.TimerTask;

import org.keycloak.timer.TimerProvider;

public class AgmTimerTaskContextImpl implements TimerProvider.TimerTaskContext {

    private final Runnable runnable;
    final TimerTask timerTask;
    private final long intervalMillis;

    public AgmTimerTaskContextImpl(Runnable runnable, TimerTask timerTask, long intervalMillis) {
        this.runnable = runnable;
        this.timerTask = timerTask;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public Runnable getRunnable() {
        return runnable;
    }

    @Override
    public long getIntervalMillis() {
        return intervalMillis;
    }
}

