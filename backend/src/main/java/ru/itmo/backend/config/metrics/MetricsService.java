package ru.itmo.backend.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

/**
 * Service for tracking application metrics.
 * Provides counters and timers for monitoring operations.
 */
@Service
public class MetricsService {

    private final Counter cloneCounter;
    private final Counter cloneErrorCounter;
    private final Counter analysisCounter;
    private final Counter analysisErrorCounter;
    private final Counter gitPullCounter;
    private final Counter gitPullErrorCounter;
    private final Timer cloneTimer;
    private final Timer analysisTimer;
    private final Timer gitPullTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.cloneCounter = Counter.builder("babel.repository.clone.total")
                .description("Total number of repository clone operations")
                .register(meterRegistry);

        this.cloneErrorCounter = Counter.builder("babel.repository.clone.errors")
                .description("Number of failed repository clone operations")
                .register(meterRegistry);

        this.analysisCounter = Counter.builder("babel.analysis.total")
                .description("Total number of code analysis operations")
                .register(meterRegistry);

        this.analysisErrorCounter = Counter.builder("babel.analysis.errors")
                .description("Number of failed code analysis operations")
                .register(meterRegistry);

        this.gitPullCounter = Counter.builder("babel.repository.pull.total")
                .description("Total number of git pull operations")
                .register(meterRegistry);

        this.gitPullErrorCounter = Counter.builder("babel.repository.pull.errors")
                .description("Number of failed git pull operations")
                .register(meterRegistry);

        this.cloneTimer = Timer.builder("babel.repository.clone.duration")
                .description("Duration of repository clone operations")
                .register(meterRegistry);

        this.analysisTimer = Timer.builder("babel.analysis.duration")
                .description("Duration of code analysis operations")
                .register(meterRegistry);

        this.gitPullTimer = Timer.builder("babel.repository.pull.duration")
                .description("Duration of git pull operations")
                .register(meterRegistry);
    }

    public void recordClone(boolean success, Runnable operation) {
        cloneCounter.increment();
        if (!success) {
            cloneErrorCounter.increment();
        }
        cloneTimer.record(operation);
    }

    public Timer.Sample startCloneTimer() {
        return Timer.start();
    }

    public void recordCloneDuration(Timer.Sample sample, boolean success) {
        sample.stop(cloneTimer);
        cloneCounter.increment();
        if (!success) {
            cloneErrorCounter.increment();
        }
    }

    public void recordAnalysis(boolean success, Runnable operation) {
        analysisCounter.increment();
        if (!success) {
            analysisErrorCounter.increment();
        }
        analysisTimer.record(operation);
    }

    public Timer.Sample startAnalysisTimer() {
        return Timer.start();
    }

    public void recordAnalysisDuration(Timer.Sample sample, boolean success) {
        sample.stop(analysisTimer);
        analysisCounter.increment();
        if (!success) {
            analysisErrorCounter.increment();
        }
    }

    public void recordGitPull(boolean success, Runnable operation) {
        gitPullCounter.increment();
        if (!success) {
            gitPullErrorCounter.increment();
        }
        gitPullTimer.record(operation);
    }

    public Timer.Sample startGitPullTimer() {
        return Timer.start();
    }

    public void recordGitPullDuration(Timer.Sample sample, boolean success) {
        sample.stop(gitPullTimer);
        gitPullCounter.increment();
        if (!success) {
            gitPullErrorCounter.increment();
        }
    }
}

