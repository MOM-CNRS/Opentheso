package fr.cnrs.opentheso.v2.stats.service;

import fr.cnrs.opentheso.v2.stats.config.StatAsyncConfig;
import fr.cnrs.opentheso.v2.stats.model.StatEventType;
import fr.cnrs.opentheso.v2.stats.persistence.StatEventCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StatEventService {

    private static final Logger log = LoggerFactory.getLogger(StatEventService.class);

    private final StatEventCommandRepository commandRepository;

    public StatEventService(StatEventCommandRepository commandRepository) {
        this.commandRepository = commandRepository;
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logConceptView(String conceptId, String conceptLabel, String lang,
                               String thesaurusId, String thesaurusLabel) {
        save(StatEventType.CONCEPT_VIEW, thesaurusId, thesaurusLabel, conceptId, conceptLabel, lang,
                null, null, null, null, null, null, null);
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logCollectionView(String collectionId, String collectionLabel, String lang,
                                  String thesaurusId, String thesaurusLabel) {
        save(StatEventType.GROUP_VIEW, thesaurusId, thesaurusLabel, null, null, lang,
                collectionId, collectionLabel, null, null, null, null, null);
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logApiCall(String url, String httpMethod) {
        save(StatEventType.API_CALL, null, null, null, null, null,
                null, null, url, httpMethod, null, null, null);
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logSearchNoResult(String searchedTerm, int nbResults,
                                  String thesaurusId, String thesaurusLabel, String lang) {
        save(StatEventType.SEARCH_NO_RESULT, thesaurusId, thesaurusLabel, null, null, lang,
                null, null, null, null, searchedTerm, null, nbResults);
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logSearchResultSelected(String searchedTerm, String selectedTerm,
                                        String thesaurusId, String thesaurusLabel, String lang) {
        save(StatEventType.SEARCH_RESULT_SELECTED, thesaurusId, thesaurusLabel, null, null, lang,
                null, null, null, null, searchedTerm, selectedTerm, null);
    }

    @Async(StatAsyncConfig.EXECUTOR_NAME)
    public void logSearchApplied(String searchedTerm, int nbResults,
                                 String thesaurusId, String thesaurusLabel, String lang) {
        save(StatEventType.SEARCH_APPLIED, thesaurusId, thesaurusLabel, null, null, lang,
                null, null, null, null, searchedTerm, null, nbResults);
    }

    private void save(StatEventType type, String thesaurusId, String thesaurusLabel,
                      String conceptId, String conceptLabel, String lang,
                      String collectionId, String collectionLabel,
                      String url, String httpMethod,
                      String searchedTerm, String selectedTerm, Integer nbResults) {
        try {
            commandRepository.insert(
                    type, LocalDateTime.now(),
                    thesaurusId, thesaurusLabel,
                    conceptId, conceptLabel, lang,
                    collectionId, collectionLabel,
                    url, httpMethod,
                    searchedTerm, selectedTerm, nbResults
            );
        } catch (Exception e) {
            log.warn("Impossible d'enregistrer l'événement statistique {} : {}", type, e.getMessage());
        }
    }
}
