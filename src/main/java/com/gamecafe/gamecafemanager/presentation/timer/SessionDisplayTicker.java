package com.gamecafe.gamecafemanager.presentation.timer;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Provides one coalesced, low-frequency clock for visible session displays.
 * The scheduled thread never mutates JavaFX state; it queues at most one
 * update on the JavaFX Application Thread.
 */
public final class SessionDisplayTicker implements AutoCloseable {

    private static final long DEFAULT_INTERVAL = 1L;

    private final ScheduledExecutorService scheduler;
    private final Consumer<Runnable> uiDispatcher;
    private final long interval;
    private final TimeUnit intervalUnit;
    private final AtomicBoolean updateQueued = new AtomicBoolean();

    private volatile Runnable activeUpdate;
    private ScheduledFuture<?> scheduledTask;
    private boolean closed;

    public SessionDisplayTicker() {
        this(
                Executors.newSingleThreadScheduledExecutor(daemonThreadFactory()),
                Platform::runLater,
                DEFAULT_INTERVAL,
                TimeUnit.SECONDS);
    }

    SessionDisplayTicker(
            ScheduledExecutorService scheduler,
            Consumer<Runnable> uiDispatcher,
            long interval,
            TimeUnit intervalUnit) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher, "uiDispatcher");
        if (interval <= 0L) {
            throw new IllegalArgumentException("Ticker interval must be positive");
        }
        this.interval = interval;
        this.intervalUnit = Objects.requireNonNull(intervalUnit, "intervalUnit");
    }

    /**
     * Activates or replaces the visible display update without creating a
     * second scheduled task.
     */
    public synchronized void start(Runnable update) {
        if (closed) {
            throw new IllegalStateException("Session display ticker is closed");
        }
        activeUpdate = Objects.requireNonNull(update, "update");
        if (scheduledTask == null || scheduledTask.isCancelled()) {
            scheduledTask = scheduler.scheduleWithFixedDelay(
                    this::queueUpdate,
                    interval,
                    interval,
                    intervalUnit);
        }
    }

    /** Stops recurring work for the previously visible session display. */
    public synchronized void stop() {
        activeUpdate = null;
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }

    boolean isRunning() {
        synchronized (this) {
            return !closed && scheduledTask != null && !scheduledTask.isCancelled();
        }
    }

    private void queueUpdate() {
        if (activeUpdate == null || !updateQueued.compareAndSet(false, true)) {
            return;
        }
        try {
            uiDispatcher.accept(() -> {
                try {
                    Runnable update = activeUpdate;
                    if (update != null) {
                        update.run();
                    }
                } finally {
                    updateQueued.set(false);
                }
            });
        } catch (RuntimeException failure) {
            updateQueued.set(false);
            throw failure;
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        stop();
        closed = true;
        scheduler.shutdownNow();
    }

    private static ThreadFactory daemonThreadFactory() {
        return task -> {
            Thread thread = new Thread(task, "session-display-ticker");
            thread.setDaemon(true);
            return thread;
        };
    }
}
