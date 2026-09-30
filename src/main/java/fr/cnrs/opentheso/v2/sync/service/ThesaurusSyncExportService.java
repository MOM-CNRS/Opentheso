package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import fr.cnrs.opentheso.v2.sync.model.SyncChangesResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.sync.model.SyncExportResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncPendingConcept;
import fr.cnrs.opentheso.v2.sync.repository.ThesaurusSyncQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lecture côté maître : concepts modifiés et export de payloads pour un pull.
 */
@Service
@RequiredArgsConstructor
public class ThesaurusSyncExportService {

    public static final int LIST_LIMIT = 2000;
    public static final int EXPORT_LIMIT = 2000;

    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ConceptRepository conceptRepository;
    private final ThesaurusSyncQueryRepository thesaurusSyncQueryRepository;
    private final ThesaurusSyncPayloadBuilder payloadBuilder;

    @Transactional(readOnly = true)
    public SyncChangesResponse listChanges(String thesaurusId, String since, String lang) {
        return listChanges(thesaurusId, parseSince(since), lang);
    }

    private SyncChangesResponse listChanges(String thesaurusId, LocalDateTime since, String lang) {
        requireMaster(thesaurusId);
        String workLang = StringUtils.firstNonBlank(
                lang, toolboxPreferencePersistence.getWorkLanguage(thesaurusId), "fr");
        int total = countChanged(thesaurusId, since);
        List<SyncPendingConcept> concepts = thesaurusSyncQueryRepository.findPendingConcepts(
                thesaurusId,
                since == null ? null : V2Dates.toUtilDate(since),
                workLang,
                LIST_LIMIT
        );
        return new SyncChangesResponse(total, concepts);
    }

    @Transactional(readOnly = true)
    public SyncExportResponse exportConcepts(String thesaurusId, List<String> conceptIds, String lang) {
        requireMaster(thesaurusId);
        String workLang = StringUtils.firstNonBlank(
                lang, toolboxPreferencePersistence.getWorkLanguage(thesaurusId), "fr");
        List<SyncConceptPayload> payloads = new ArrayList<>();
        int exported = 0;
        Set<String> seen = new LinkedHashSet<>();
        for (String conceptId : conceptIds == null ? List.<String>of() : conceptIds) {
            if (StringUtils.isBlank(conceptId) || !seen.add(conceptId.trim()) || exported >= EXPORT_LIMIT) {
                continue;
            }
            payloadBuilder.build(thesaurusId, conceptId.trim(), workLang).ifPresent(payloads::add);
            exported++;
        }
        return new SyncExportResponse(payloads);
    }

    private int countChanged(String thesaurusId, LocalDateTime since) {
        if (since == null) {
            return (int) conceptRepository.findAllByIdThesaurusAndStatusNot(thesaurusId, "CA").stream()
                    .map(Concept::getIdConcept)
                    .count();
        }
        List<String> ids = conceptRepository.findConceptIdsChangedSince(thesaurusId, V2Dates.toUtilDate(since));
        return ids == null ? 0 : ids.size();
    }

    private void requireMaster(String thesaurusId) {
        Preferences prefs = toolboxPreferencePersistence.findPreferences(thesaurusId);
        if (prefs == null || !prefs.isMaster()) {
            throw new IllegalStateException("Le thésaurus cible n'est pas configuré comme maître");
        }
    }

    static LocalDateTime parseSince(String since) {
        if (StringUtils.isBlank(since)) {
            return null;
        }
        String trimmed = since.trim();
        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // ISO-8601 local ou instant
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(trimmed), V2Dates.zone());
        } catch (DateTimeParseException ignored) {
            // suite
        }
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Date since invalide: " + since);
        }
    }
}
