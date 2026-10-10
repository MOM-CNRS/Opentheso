package fr.cnrs.opentheso.v2.stats.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

@Configuration
@EnableAsync
public class StatAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(StatAsyncConfig.class);

    public static final String EXECUTOR_NAME = "statTaskExecutor";

    @Bean(name = EXECUTOR_NAME)
    public Executor statTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("stat-event-");
        executor.setRejectedExecutionHandler(discardWithWarning());
        executor.initialize();
        return executor;
    }

    private RejectedExecutionHandler discardWithWarning() {
        return (runnable, threadPoolExecutor) ->
                log.warn("File d'attente des statistiques saturée : un événement a été perdu.");
    }
}
