package com.clincanywhere.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.core.task.TaskDecorator;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.jspecify.annotations.NonNull;

@Configuration
public class PlatformConfig implements SchedulingConfigurer {
    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolCoreSize);
        executor.setMaxPoolSize(poolMaxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("clincanywhere-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(mdcTaskDecorator());
        return executor;
    }

    @Bean(name = "whatsappTaskExecutor")
    public Executor whatsappTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(whatsappPoolSize);
        executor.setMaxPoolSize(whatsappPoolSize);
        executor.setQueueCapacity(whatsappQueueCapacity);
        executor.setThreadNamePrefix("clincanywhere-whatsapp-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(mdcTaskDecorator());
        return executor;
    }

    private @NonNull TaskDecorator mdcTaskDecorator() {
        return task -> {
            var callerContext = MDC.getCopyOfContextMap();
            return () -> {
                var previousContext = MDC.getCopyOfContextMap();
                try {
                    if (callerContext == null) MDC.clear(); else MDC.setContextMap(callerContext);
                    task.run();
                } finally {
                    if (previousContext == null) MDC.clear(); else MDC.setContextMap(previousContext);
                }
            };
        };
    }

    @Value("${ASYNC_CORE_POOL_SIZE:4}") private int poolCoreSize;
    @Value("${ASYNC_MAX_POOL_SIZE:16}") private int poolMaxSize;
    @Value("${ASYNC_QUEUE_CAPACITY:200}") private int queueCapacity;
    @Value("${WHATSAPP_POOL_SIZE:4}") private int whatsappPoolSize;
    @Value("${WHATSAPP_QUEUE_CAPACITY:100}") private int whatsappQueueCapacity;
    @Value("${SCHEDULER_POOL_SIZE:2}") private int schedulerPoolSize;

    @Bean(name = "taskScheduler")
    public @NonNull ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerPoolSize);
        scheduler.setThreadNamePrefix("clincanywhere-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return scheduler;
    }

    @Override public void configureTasks(@NonNull ScheduledTaskRegistrar registrar) { registrar.setTaskScheduler(taskScheduler()); }

    @Bean
    org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {
        var config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOrigins(java.util.Arrays.stream(origins.split(",")).map(origin -> origin.trim()).filter(origin -> !origin.isBlank()).toList());
        config.setAllowedOriginPatterns(java.util.List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setExposedHeaders(java.util.List.of("X-Correlation-ID"));
        config.setMaxAge(3600L);
        config.setAllowCredentials(false);
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
