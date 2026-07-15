package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptHistoryEntry;
import fr.cnrs.opentheso.v2.concept.model.ConceptHistoryOverview;
import fr.cnrs.opentheso.v2.shared.repository.HistoryQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptHistoryReadService {

    private final HistoryQueryRepository historyQueryRepository;

    @Transactional(readOnly = true)
    public ConceptHistoryOverview load(String thesaurusId, String conceptId, String preferredTermId) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, preferredTermId)) {
            return emptyOverview();
        }
        return new ConceptHistoryOverview(
                mapStandardEntries(historyQueryRepository.findTermHistories(preferredTermId, thesaurusId)),
                mapStandardEntries(historyQueryRepository.findSynonymHistories(preferredTermId, thesaurusId)),
                mapRelationEntries(historyQueryRepository.findRelationHistories(preferredTermId, thesaurusId)),
                mapNoteEntries(historyQueryRepository.findNoteHistories(conceptId, preferredTermId, thesaurusId))
        );
    }

    private ConceptHistoryOverview emptyOverview() {
        return new ConceptHistoryOverview(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private List<ConceptHistoryEntry> mapStandardEntries(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new ConceptHistoryEntry(
                        stringAt(row, 0),
                        stringAt(row, 1),
                        stringAt(row, 2),
                        toDate(row, 3),
                        stringAt(row, 4),
                        null,
                        null
                ))
                .toList();
    }

    private List<ConceptHistoryEntry> mapRelationEntries(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new ConceptHistoryEntry(
                        stringAt(row, 0),
                        null,
                        stringAt(row, 2),
                        toDate(row, 3),
                        stringAt(row, 4),
                        null,
                        stringAt(row, 1)
                ))
                .toList();
    }

    private List<ConceptHistoryEntry> mapNoteEntries(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new ConceptHistoryEntry(
                        stringAt(row, 0),
                        stringAt(row, 2),
                        stringAt(row, 3),
                        toDate(row, 4),
                        stringAt(row, 5),
                        stringAt(row, 1),
                        null
                ))
                .toList();
    }

    private static String stringAt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return String.valueOf(row[index]);
    }

    private static Date toDate(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return null;
        }
        Object value = row[index];
        if (value instanceof Date date) {
            return date;
        }
        if (value instanceof Timestamp timestamp) {
            return new Date(timestamp.getTime());
        }
        return null;
    }
}
