package fr.cnrs.opentheso.stats.services;

import fr.cnrs.opentheso.stats.config.StatAsyncConfig;
import fr.cnrs.opentheso.stats.entity.LogEvent;
import fr.cnrs.opentheso.stats.entity.StatEventType;
import fr.cnrs.opentheso.stats.repository.LogEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Point d'entrée unique pour enregistrer un événement statistique en base.
 * Chaque méthode est asynchrone et absorbe ses propres erreurs : le suivi
 * statistique ne doit jamais casser une fonctionnalité de l'application.
 */
@Service
public class StatEventService {

    private static final Logger log = LoggerFactory.getLogger(StatEventService.class);

    private final LogEventRepository logEventRepository;

    public StatEventService(LogEventRepository logEventRepository) {
        this.logEventRepository = logEventRepository;
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logConceptView(String conceptId, String conceptLabel, String lang,
                               String thesaurusId, String thesaurusLabel) {
        LogEvent event = new LogEvent();
        event.setEventType(StatEventType.CONCEPT_VIEW);
        event.setEventTime(LocalDateTime.now());
        event.setConceptId(conceptId);
        event.setConceptLabel(conceptLabel);
        event.setLang(lang);
        event.setThesaurusId(thesaurusId);
        event.setThesaurusLabel(thesaurusLabel);
        save(event);
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logCollectionView(String collectionId, String collectionLabel,
                                  String lang,
                                  String thesaurusId, String thesaurusLabel) {
        LogEvent event = new LogEvent();
        event.setEventType(StatEventType.GROUP_VIEW);
        event.setEventTime(LocalDateTime.now());
        event.setCollectionId(collectionId);
        event.setCollectionLabel(collectionLabel);
        event.setLang(lang);
        event.setThesaurusId(thesaurusId);
        event.setThesaurusLabel(thesaurusLabel);
        save(event);
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logApiCall(String url, String httpMethod) {
        LogEvent event = new LogEvent();
        event.setEventType(StatEventType.API_CALL);
        event.setEventTime(LocalDateTime.now());
        event.setUrl(url);
        event.setHttpMethod(httpMethod);
        save(event);
    }

    private void save(LogEvent event) {
        try {
            logEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Impossible d'enregistrer l'événement statistique {} : {}",
                    event.getEventType(), e.getMessage());
        }
    }
}