package ru.itmo.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for scheduled tasks thread pool.
 * Allows configuring the size of the thread pool used for @Scheduled tasks.
 */
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    private final int corePoolSize;
    private final int maxPoolSize;

    public SchedulingConfig(
            @Value("${scheduled.tasks.core-pool-size:2}") int corePoolSize,
            @Value("${scheduled.tasks.max-pool-size:5}") int maxPoolSize
    ) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
    }

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(corePoolSize);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }
}

