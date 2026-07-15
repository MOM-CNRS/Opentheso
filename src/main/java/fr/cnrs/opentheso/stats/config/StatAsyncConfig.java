package fr.cnrs.opentheso.stats.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * Pool de threads dédié à l'écriture des statistiques.
 *
 * Objectif : que le suivi statistique n'ait JAMAIS d'impact sur les temps de
 * réponse HTTP de l'application, ni ne puisse la faire planter en cas de
 * souci base de données. Si le pool est saturé, on jette l'événement (log
 * d'avertissement) plutôt que de bloquer ou de faire échouer la requête
 * utilisateur en cours.
 */
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

        // Si la file est pleine (pic de charge, DB lente...), on ne bloque
        // jamais le thread HTTP appelant : on journalise et on abandonne
        // l'événement statistique plutôt que de dégrader l'expérience utilisateur.
        executor.setRejectedExecutionHandler(discardWithWarning());

        executor.initialize();
        return executor;
    }

    private RejectedExecutionHandler discardWithWarning() {
        return (runnable, threadPoolExecutor) ->
                log.warn("File d'attente des statistiques saturée : un événement a été perdu.");
    }
}
