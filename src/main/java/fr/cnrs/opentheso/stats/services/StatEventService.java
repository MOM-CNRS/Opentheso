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
 *
 * Utilisation dans vos beans existants (GroupView, ConceptView,
 * LoggingInterceptor...) : on ajoute simplement un appel à ce service,
 * EN PLUS du log.info(...) déjà en place. Le fichier de log texte actuel
 * n'est pas modifié ni remplacé.
 *
 * Chaque méthode est asynchrone (@Async) : l'appelant n'attend jamais
 * l'écriture en base, et toute erreur est absorbée ici pour ne jamais
 * remonter vers le code appelant (le suivi statistique ne doit jamais
 * casser une fonctionnalité de l'application).
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
                               String thesaurusLabel, String thesaurusId) {
        LogEvent event = new LogEvent();
        event.setEventType(StatEventType.CONCEPT_VIEW);
        event.setEventTime(LocalDateTime.now());
        event.setConceptId(conceptId);
        event.setConceptLabel(conceptLabel);
        event.setLang(lang);
        event.setThesaurusLabel(thesaurusLabel);
        event.setThesaurusId(thesaurusId);
        save(event);
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logCollectionView(String collectionId, String collectionLabel, String lang,
                             String thesaurusLabel, String thesaurusId) {
        LogEvent event = new LogEvent();
        event.setEventType(StatEventType.COLLECTION_VIEW);
        event.setEventTime(LocalDateTime.now());
        event.setCollectionId(collectionId);
        event.setCollectionLabel(collectionLabel);
        event.setLang(lang);
        event.setThesaurusLabel(thesaurusLabel);
        event.setThesaurusId(thesaurusId);
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
            // On ne relance jamais : une panne d'écriture statistique ne doit
            // jamais impacter l'utilisateur ni remonter dans les logs d'erreur
            // applicatifs comme une vraie panne fonctionnelle.
            log.warn("Impossible d'enregistrer l'événement statistique {} : {}",
                    event.getEventType(), e.getMessage());
        }
    }
}
