package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.ConceptGroupLabel;
import fr.cnrs.opentheso.models.ConceptGroupProjection;
import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.repositories.AlignementRepository;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class ToolboxStatisticsPersistence {

    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final ConceptStatusRepository conceptStatusRepository;
    private final NoteRepository noteRepository;
    private final AlignementRepository alignementRepository;
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
        List<GenericStatistiqueData> result = new ArrayList<>();
        var listGroup = new ArrayList<>(loadConceptGroups(thesaurusId, language));
        listGroup.sort(Comparator.comparing(NodeGroup::getLexicalValue, String.CASE_INSENSITIVE_ORDER));

        listGroup.forEach(group -> {
            String groupId = group.getConceptGroup().getIdGroup();
            var noteNbr = noteRepository.countNotesByGroupAndLangAndThesaurus(groupId, thesaurusId, language);
            var conceptNbr = conceptStatusRepository.countConceptsInGroup(thesaurusId, groupId);
            var traductionOfGroupNbr = conceptStatusRepository.countNonPreferredTermsByLangAndGroup(
                    thesaurusId, groupId, language);
            var wikidataAlignNbr = countWikidataAlignments(thesaurusId, groupId);
            var totalAlignmentNbr = loadAlignments(thesaurusId, groupId).size();
            result.add(GenericStatistiqueData.builder()
                    .idCollection(groupId)
                    .collection(group.getLexicalValue())
                    .notesNbr(noteNbr)
                    .synonymesNbr(traductionOfGroupNbr)
                    .conceptsNbr(conceptNbr)
                    .termesNonTraduitsNbr(conceptNbr - traductionOfGroupNbr)
                    .wikidataAlignNbr(wikidataAlignNbr)
                    .totalAlignment(totalAlignmentNbr)
                    .build());
        });

        var conceptNbr = conceptStatusRepository.countConceptsWithoutGroup(thesaurusId);
        result.add(GenericStatistiqueData.builder()
                .collection("Sans collection")
                .conceptsNbr(conceptNbr)
                .notesNbr(countNotesWithoutGroup(thesaurusId, language))
                .synonymesNbr(conceptStatusRepository.countNonPreferredTermsNotInGroup(thesaurusId, language))
                .termesNonTraduitsNbr(conceptNbr - conceptStatusRepository.countConceptsWithoutGroupByLangAndThesaurus(
                        thesaurusId, language))
                .wikidataAlignNbr(countWikidataAlignments(thesaurusId, null))
                .totalAlignment(loadAlignments(thesaurusId, null).size())
                .build());
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

    private List<NodeGroup> loadConceptGroups(String thesaurusId, String language) {
        var groupIds = conceptGroupRepository.findAllByIdThesaurus(thesaurusId).stream()
                .map(group -> group.getIdGroup())
                .toList();
        return groupIds.stream()
                .map(idGroup -> {
                    var conceptGroup = conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, thesaurusId);
                    if (conceptGroup.isEmpty()) {
                        return null;
                    }
                    var labels = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(
                            thesaurusId, idGroup, language);
                    return NodeGroup.builder()
                            .groupPrivate(conceptGroup.get().isPrivate())
                            .conceptGroup(conceptGroup.get())
                            .lexicalValue(CollectionUtils.isNotEmpty(labels) ? labels.get(0).getLexicalValue() : "")
                            .idLang(language)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private int countNotesWithoutGroup(String thesaurusId, String language) {
        return noteRepository.countNotesWithoutGroupByLangAndThesaurus(thesaurusId, language)
                + noteRepository.countNotesOfTermsWithoutGroup(thesaurusId, language);
    }

    private int countWikidataAlignments(String thesaurusId, String groupId) {
        return (int) loadAlignments(thesaurusId, groupId).stream()
                .filter(element -> StringUtils.isNotEmpty(element.getUriTarget())
                        && element.getUriTarget().contains("wikidata.org"))
                .count();
    }

    private List<Alignement> loadAlignments(String thesaurusId, String groupId) {
        var alignements = StringUtils.isEmpty(groupId)
                ? alignementRepository.findAlignementsNotInConceptGroup(thesaurusId)
                : alignementRepository.findAlignementsByGroupAndThesaurus(groupId, thesaurusId);
        return CollectionUtils.isNotEmpty(alignements) ? alignements : List.of();
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
}
