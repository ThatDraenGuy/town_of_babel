package ru.itmo.backend.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void testRecordClone_RecordsCounter() {
        metricsService.recordClone(true, () -> {
            // Simulate clone operation
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Counter counter = meterRegistry.find("babel.repository.clone.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void testRecordClone_RecordsErrorCounter() {
        metricsService.recordClone(false, () -> {
            // Simulate failed clone
        });

        Counter errorCounter = meterRegistry.find("babel.repository.clone.errors").counter();
        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count());
    }

    @Test
    void testRecordClone_RecordsTimer() {
        metricsService.recordClone(true, () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Timer timer = meterRegistry.find("babel.repository.clone.duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 50);
    }

    @Test
    void testStartCloneTimer_RecordsDuration() {
        Timer.Sample sample = metricsService.startCloneTimer();
        
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.recordCloneDuration(sample, true);

        Counter counter = meterRegistry.find("babel.repository.clone.total").counter();
        assertEquals(1.0, counter.count());

        Timer timer = meterRegistry.find("babel.repository.clone.duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void testRecordAnalysis_RecordsCounter() {
        metricsService.recordAnalysis(true, () -> {
            // Simulate analysis
        });

        Counter counter = meterRegistry.find("babel.analysis.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void testRecordAnalysis_RecordsErrorCounter() {
        metricsService.recordAnalysis(false, () -> {
            // Simulate failed analysis
        });

        Counter errorCounter = meterRegistry.find("babel.analysis.errors").counter();
        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count());
    }

    @Test
    void testRecordGitPull_RecordsCounter() {
        metricsService.recordGitPull(true, () -> {
            // Simulate git pull
        });

        Counter counter = meterRegistry.find("babel.repository.pull.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void testRecordGitPull_RecordsErrorCounter() {
        metricsService.recordGitPull(false, () -> {
            // Simulate failed git pull
        });

        Counter errorCounter = meterRegistry.find("babel.repository.pull.errors").counter();
        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count());
    }
}

