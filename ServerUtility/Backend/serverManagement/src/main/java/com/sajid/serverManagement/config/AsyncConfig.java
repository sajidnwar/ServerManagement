package com.sajid.serverManagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "extractionTaskExecutor")
    public Executor extractionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);  // Base number of threads
        executor.setMaxPoolSize(5);   // Maximum number of threads
        executor.setQueueCapacity(100); // Queue capacity for pending tasks
        executor.setThreadNamePrefix("extraction-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
