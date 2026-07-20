package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupLabel;
import fr.cnrs.opentheso.models.ConceptGroupProjection;
import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptStatusRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.v2.toolbox.export.StatisticsReportCsvWriter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ToolboxStatisticsPersistence {

    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxStatisticsQueryRepository toolboxStatisticsQueryRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final ConceptStatusRepository conceptStatusRepository;
    private final NoteRepository noteRepository;
    private final ConceptRepository conceptRepository;

    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String workLang) {
        return toolboxThesaurusPersistence.loadUsedLanguages(thesaurusId, workLang);
    }

    public List<DomaineDto> loadCollections(String thesaurusId, String language) {
        if (!toolboxThesaurusPersistence.exists(thesaurusId)) {
            return List.of();
        }
        return conceptGroupLabelRepository.findAllByIdThesaurusAndLang(thesaurusId, language).stream()
                .map(label -> DomaineDto.builder()
                        .id(label.getIdGroup())
                        .name(label.getLexicalValue())
                        .build())
                .toList();
    }

    public List<GenericStatistiqueData> loadCollectionStatistics(String thesaurusId, String language) {
        List<ConceptGroup> groups = conceptGroupRepository.findAllByIdThesaurus(thesaurusId);
        Map<String, String> labelsByGroup = loadLabelsByGroup(thesaurusId, language);

        Map<String, Integer> conceptsByGroup = toolboxStatisticsQueryRepository.countConceptsByGroup(thesaurusId);
        Map<String, Integer> notesByGroup = toolboxStatisticsQueryRepository.countNotesByGroup(thesaurusId, language);
        Map<String, Integer> synonymsByGroup = toolboxStatisticsQueryRepository.countSynonymsByGroup(thesaurusId, language);
        Map<String, int[]> alignmentsByGroup = toolboxStatisticsQueryRepository.countAlignmentsByGroup(thesaurusId);

        List<GenericStatistiqueData> result = new ArrayList<>(groups.size() + 1);
        for (ConceptGroup group : groups) {
            String groupId = group.getIdGroup();
            String key = normalizeGroupKey(groupId);
            int conceptNbr = conceptsByGroup.getOrDefault(key, 0);
            int synonymNbr = synonymsByGroup.getOrDefault(key, 0);
            int[] alignments = alignmentsByGroup.getOrDefault(key, new int[]{0, 0});
            result.add(GenericStatistiqueData.builder()
                    .idCollection(groupId)
                    .collection(labelsByGroup.getOrDefault(key, ""))
                    .notesNbr(notesByGroup.getOrDefault(key, 0))
                    .synonymesNbr(synonymNbr)
                    .conceptsNbr(conceptNbr)
                    .termesNonTraduitsNbr(Math.max(0, conceptNbr - synonymNbr))
                    .totalAlignment(alignments[0])
                    .wikidataAlignNbr(alignments[1])
                    .build());
        }
        result.sort(Comparator.comparing(
                GenericStatistiqueData::getCollection,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));
        result.add(buildWithoutGroupStatistics(thesaurusId, language));
        return result;
    }

    public Date loadLastModification(String thesaurusId) {
        try {
            var dates = conceptRepository.findLastModifiedDates(thesaurusId, PageRequest.of(0, 1));
            if (!dates.isEmpty()) {
                return dates.get(0);
            }
        } catch (Exception e) {
            // ignored
        }
        return null;
    }

    public List<ConceptStatisticData> loadConceptStatistics(
            String thesaurusId,
            String language,
            Date startDate,
            Date endDate,
            String collectionId,
            String resultLimit
    ) {
        int limit;
        try {
            limit = Integer.parseInt(resultLimit);
        } catch (Exception e) {
            limit = 100;
        }

        List<ConceptGroupProjection> rows;
        if (ObjectUtils.isEmpty(startDate) || ObjectUtils.isEmpty(endDate)) {
            rows = StringUtils.isEmpty(collectionId)
                    ? conceptStatusRepository.findRecentConceptsByLangAndThesaurus(thesaurusId, language, limit)
                    : conceptStatusRepository.findConceptsByGroupAndLang(thesaurusId, language, collectionId, limit);
        } else {
            rows = StringUtils.isEmpty(collectionId)
                    ? conceptStatusRepository.findConceptsModifiedBetween(thesaurusId, language, startDate, endDate, limit)
                    : conceptStatusRepository.findConceptsByGroupLangDate(
                            thesaurusId, language, collectionId, startDate, endDate, limit);
        }
        return mapConceptStatistics(rows);
    }

    public byte[] exportGenericReport(List<GenericStatistiqueData> rows) {
        var report = new StatisticsReportCsvWriter();
        report.createGenericStatistiquesRapport(rows);
        return report.getOutput().toByteArray();
    }

    public byte[] exportConceptReport(List<ConceptStatisticData> rows) {
        var report = new StatisticsReportCsvWriter();
        report.createConceptsStatistiquesRapport(rows);
        return report.getOutput().toByteArray();
    }

    private GenericStatistiqueData buildWithoutGroupStatistics(String thesaurusId, String language) {
        int conceptNbr = conceptStatusRepository.countConceptsWithoutGroup(thesaurusId);
        int translatedConcepts = conceptStatusRepository.countConceptsWithoutGroupByLangAndThesaurus(
                thesaurusId, language);
        int[] alignments = toolboxStatisticsQueryRepository.countAlignmentsWithoutGroup(thesaurusId);
        return GenericStatistiqueData.builder()
                .collection("Sans collection")
                .conceptsNbr(conceptNbr)
                .notesNbr(countNotesWithoutGroup(thesaurusId, language))
                .synonymesNbr(conceptStatusRepository.countNonPreferredTermsNotInGroup(language, thesaurusId))
                .termesNonTraduitsNbr(Math.max(0, conceptNbr - translatedConcepts))
                .totalAlignment(alignments[0])
                .wikidataAlignNbr(alignments[1])
                .build();
    }

    private Map<String, String> loadLabelsByGroup(String thesaurusId, String language) {
        List<ConceptGroupLabel> labels = conceptGroupLabelRepository.findAllByIdThesaurusAndLang(thesaurusId, language);
        Map<String, String> labelsByGroup = new HashMap<>();
        for (ConceptGroupLabel label : labels) {
            if (label.getIdGroup() == null) {
                continue;
            }
            labelsByGroup.putIfAbsent(normalizeGroupKey(label.getIdGroup()), StringUtils.defaultString(label.getLexicalValue()));
        }
        return labelsByGroup;
    }

    private int countNotesWithoutGroup(String thesaurusId, String language) {
        return noteRepository.countNotesWithoutGroupByLangAndThesaurus(thesaurusId, language)
                + noteRepository.countNotesOfTermsWithoutGroup(thesaurusId, language);
    }

    private List<ConceptStatisticData> mapConceptStatistics(List<ConceptGroupProjection> rows) {
        var dataFormat = new SimpleDateFormat("yyyy-MM-dd");
        return CollectionUtils.isEmpty(rows)
                ? List.of()
                : rows.stream()
                        .map(element -> ConceptStatisticData.builder()
                                .idConcept(element.getIdConcept())
                                .dateCreation(ObjectUtils.isEmpty(element.getCreated())
                                        ? null : dataFormat.format(element.getCreated()))
                                .dateModification(ObjectUtils.isEmpty(element.getModified())
                                        ? null : dataFormat.format(element.getModified()))
                                .label(element.getLexicalValue())
                                .utilisateur(element.getUsername())
                                .type("skos:prefLabel")
                                .build())
                        .toList();
    }

    private static String normalizeGroupKey(String groupId) {
        return groupId == null ? "" : groupId.toLowerCase(Locale.ROOT);
    }
}
