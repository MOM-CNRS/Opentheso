package fr.cnrs.opentheso.toolbox.edition.support;

import fr.cnrs.opentheso.toolbox.persistence.ThesaurusLifecyclePersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Retire un thésaurus créé par un import interrompu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusImportAbort {

    private final ThesaurusLifecyclePersistence thesaurusLifecyclePersistence;
    private final ThesaurusImportBatchSupport importBatchSupport;

    public void deleteCreatedThesaurus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        try {
            importBatchSupport.inTransaction(() -> {
                thesaurusLifecyclePersistence.deleteRights(thesaurusId);
                thesaurusLifecyclePersistence.deleteThesaurus(thesaurusId);
            });
        } catch (RuntimeException ex) {
            log.warn("Impossible de retirer le thésaurus {} après annulation de l'import", thesaurusId, ex);
        }
    }
}
