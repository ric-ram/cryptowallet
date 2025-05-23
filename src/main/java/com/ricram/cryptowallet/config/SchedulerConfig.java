package com.ricram.cryptowallet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SchedulerConfig {

    @Bean("priceUpdateExecutor")
    public ThreadPoolTaskExecutor priceUpdateExecutor() {

        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();

        exec.setCorePoolSize(3);
        exec.setMaxPoolSize(3);
        exec.setMaxPoolSize(3);
        exec.setQueueCapacity(10);
        exec.setThreadNamePrefix("price-updater-");
        exec.initialize();
        return exec;
    }
}
