package com.tradingdiary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置，定义采集线程池和调度器
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 采集任务专用线程池
     *
     * @return 采集线程池执行器
     */
    @Bean("collectionExecutor")
    public Executor collectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("collection-");
        executor.initialize();
        return executor;
    }
}
