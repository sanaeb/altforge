package dev.sanaeb.altforge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated executor for {@link dev.sanaeb.altforge.jobs.BatchJobAsyncWorker}.
 * Sized small on purpose — Render free tier has 1 CPU and 512 MB, and
 * Gemini calls are the dominant cost.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "altforgeJobExecutor")
    public Executor altforgeJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("altforge-job-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
