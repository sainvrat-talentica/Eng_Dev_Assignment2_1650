package com.swifteats.analytics.service;

import com.swifteats.common.exception.ImportInProgressException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportLockTest {

    @Test
    void execute_rejectsConcurrentImport() throws Exception {
        ImportLock lock = new ImportLock();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger conflicts = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            executor.submit(() -> lock.execute(() -> {
                firstStarted.countDown();
                try {
                    releaseFirst.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return "done";
            }));

            firstStarted.await();
            executor.submit(() -> {
                try {
                    lock.execute(() -> "blocked");
                } catch (ImportInProgressException ex) {
                    conflicts.incrementAndGet();
                }
            }).get();

            assertThat(conflicts.get()).isEqualTo(1);
            releaseFirst.countDown();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void execute_allowsSequentialImport() {
        ImportLock lock = new ImportLock();
        assertThat(lock.execute(() -> 1)).isEqualTo(1);
        assertThat(lock.execute(() -> 2)).isEqualTo(2);
    }
}
