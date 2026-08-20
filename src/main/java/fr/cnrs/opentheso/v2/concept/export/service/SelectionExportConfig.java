package fr.cnrs.opentheso.v2.concept.export.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class SelectionExportConfig {

    @Bean(name = "selectionExportExecutor")
    public ThreadPoolTaskExecutor selectionExportExecutor() {
        int processors = Math.max(2, Runtime.getRuntime().availableProcessors());
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("selection-export-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(Math.min(8, processors));
        executor.setQueueCapacity(32);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(8);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
