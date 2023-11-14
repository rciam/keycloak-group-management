package org.rciam.plugins.groups.scheduled;

import org.keycloak.timer.TimerProvider;

import java.util.TimerTask;

public class AgmTimerTaskContextImpl implements AgmTimerProvider.AgmTimerTaskContext {

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