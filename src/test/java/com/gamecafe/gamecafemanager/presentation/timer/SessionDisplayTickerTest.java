package com.gamecafe.gamecafemanager.presentation.timer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SessionDisplayTickerTest {

    private CountingScheduler scheduler;
    private SessionDisplayTicker ticker;

    @AfterEach
    void closeTicker() {
        if (ticker != null) {
            ticker.close();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Test
    void repeatedStartReusesOneScheduledTask() {
        scheduler = new CountingScheduler();
        ticker = new SessionDisplayTicker(
                scheduler, Runnable::run, 10L, TimeUnit.MILLISECONDS);
        AtomicInteger firstUpdates = new AtomicInteger();
        AtomicInteger replacementUpdates = new AtomicInteger();

        ticker.start(firstUpdates::incrementAndGet);
        ticker.start(replacementUpdates::incrementAndGet);

        assertTrue(ticker.isRunning());
        assertEquals(1, scheduler.getScheduleCount());
        awaitAtLeast(replacementUpdates, 1);
        assertEquals(0, firstUpdates.get());

        ticker.stop();
        assertFalse(ticker.isRunning());
        ticker.start(replacementUpdates::incrementAndGet);
        assertEquals(2, scheduler.getScheduleCount());
    }

    @Test
    void coalescesTicksWhileOneUiUpdateIsQueued() throws Exception {
        scheduler = new CountingScheduler();
        BlockingQueue<Runnable> uiQueue = new LinkedBlockingQueue<>();
        ticker = new SessionDisplayTicker(
                scheduler, uiQueue::add, 5L, TimeUnit.MILLISECONDS);
        AtomicInteger updates = new AtomicInteger();

        ticker.start(updates::incrementAndGet);
        Runnable queued = uiQueue.poll(1L, TimeUnit.SECONDS);
        assertNotNull(queued);
        Thread.sleep(40L);

        assertTrue(uiQueue.isEmpty());
        assertEquals(0, updates.get());
        queued.run();
        assertEquals(1, updates.get());
    }

    @Test
    void stoppedTickerDropsAlreadyQueuedUiUpdate() throws Exception {
        scheduler = new CountingScheduler();
        BlockingQueue<Runnable> uiQueue = new LinkedBlockingQueue<>();
        ticker = new SessionDisplayTicker(
                scheduler, uiQueue::add, 5L, TimeUnit.MILLISECONDS);
        AtomicInteger updates = new AtomicInteger();

        ticker.start(updates::incrementAndGet);
        Runnable queued = uiQueue.poll(1L, TimeUnit.SECONDS);
        assertNotNull(queued);
        ticker.stop();
        queued.run();

        assertEquals(0, updates.get());
        assertFalse(ticker.isRunning());
    }

    @Test
    void closedTickerCannotBeRestarted() {
        scheduler = new CountingScheduler();
        ticker = new SessionDisplayTicker(
                scheduler, Runnable::run, 10L, TimeUnit.MILLISECONDS);

        ticker.close();

        assertThrows(IllegalStateException.class, () -> ticker.start(() -> { }));
        assertTrue(scheduler.isShutdown());
    }

    private void awaitAtLeast(AtomicInteger value, int expected) {
        long deadline = System.nanoTime() + Duration.ofSeconds(1L).toNanos();
        while (value.get() < expected && System.nanoTime() < deadline) {
            Thread.yield();
        }
        assertTrue(value.get() >= expected);
    }

    private static final class CountingScheduler extends ScheduledThreadPoolExecutor {

        private final AtomicInteger scheduleCount = new AtomicInteger();

        private CountingScheduler() {
            super(1);
            setRemoveOnCancelPolicy(true);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command,
                long initialDelay,
                long delay,
                TimeUnit unit) {
            scheduleCount.incrementAndGet();
            return super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }

        private int getScheduleCount() {
            return scheduleCount.get();
        }
    }
}
