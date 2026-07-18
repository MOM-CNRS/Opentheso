package fr.cnrs.opentheso.v2.publicapi.graphql.service;

import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptGps;
import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptImage;
import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptLabel;
import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptNode;
import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptNote;
import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptRelation;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PublicGraphQlConceptService {

    private static final int SEARCH_LIMIT = 25;

    private final ConceptReadService conceptReadService;
    private final ConceptRepository conceptRepository;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    public Optional<PublicConceptNode> getConcept(String thesaurusId, String conceptId, String lang) {
        String workLang = resolveLang(thesaurusId, lang);
        return conceptReadService.loadDetail(thesaurusId, conceptId, workLang).map(this::toNode);
    }

    public List<PublicConceptNode> searchConcepts(String thesaurusId, String value, List<String> groupIds, String lang) {
        String workLang = resolveLang(thesaurusId, lang);
        var candidateIds = conceptReadService.searchByLabel(thesaurusId, workLang, value, SEARCH_LIMIT).stream()
                .map(node -> node.getNodeId())
                .toList();

        List<String> normalizedGroupIds = groupIds == null ? List.of() : groupIds.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .toList();

        List<String> filteredIds = candidateIds;
        if (!normalizedGroupIds.isEmpty()) {
            Set<String> groupMemberIds = conceptRepository.findAllByThesaurusAndGroups(thesaurusId, normalizedGroupIds).stream()
                    .map(view -> view.getIdConcept())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            filteredIds = candidateIds.stream().filter(groupMemberIds::contains).toList();
        }

        return filteredIds.stream()
                .map(id -> conceptReadService.loadDetail(thesaurusId, id, workLang))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toNode)
                .toList();
    }

    private PublicConceptNode toNode(ConceptDetail detail) {
        var summary = detail.summary();
        return new PublicConceptNode(
                summary.conceptId(),
                summary.thesaurusId(),
                summary.preferredLabel(),
                summary.notation(),
                summary.conceptType(),
                summary.status(),
                summary.arkId(),
                summary.created(),
                summary.modified(),
                detail.translations().stream().map(t -> new PublicConceptLabel(t.lang(), t.value())).toList(),
                detail.synonyms(),
                detail.hiddenSynonyms(),
                toRelations(detail.broaderTerms()),
                toRelations(detail.narrowerTerms()),
                toRelations(detail.relatedTerms()),
                toRelations(detail.collections()),
                toRelations(detail.facets()),
                toRelations(detail.replacedBy()),
                toRelations(detail.replaces()),
                toExactMatches(detail),
                toNotes(detail, "definition"),
                toNotes(detail, "example"),
                toNotes(detail, "editorialNote"),
                toNotes(detail, "scopeNote"),
                toNotes(detail, "historyNote"),
                toNotes(detail, "changeNote"),
                detail.images().stream()
                        .map(image -> new PublicConceptImage(image.uri(), image.imageName(), image.copyright(), image.creator()))
                        .toList(),
                detail.gpsPoints().stream()
                        .map(gps -> new PublicConceptGps(gps.latitude(), gps.longitude()))
                        .toList()
        );
    }

    private List<PublicConceptRelation> toRelations(List<ConceptRelation> relations) {
        return relations.stream()
                .map(relation -> new PublicConceptRelation(relation.conceptId(), relation.label(), relation.arkId()))
                .toList();
    }

    private List<PublicConceptRelation> toExactMatches(ConceptDetail detail) {
        if (CollectionUtils.isEmpty(detail.alignmentGroups())) {
            return List.of();
        }
        return detail.alignmentGroups().stream()
                .filter(group -> "exactmatch".equalsIgnoreCase(group.typeKey()))
                .flatMap(group -> group.items().stream())
                .map(alignment -> new PublicConceptRelation(alignment.uri(), alignment.sourceName(), null))
                .toList();
    }

    private List<PublicConceptNote> toNotes(ConceptDetail detail, String typeCode) {
        return detail.notesOfType(typeCode).stream()
                .map(note -> new PublicConceptNote(note.typeCode(), note.lang(), note.value()))
                .toList();
    }

    private String resolveLang(String thesaurusId, String lang) {
        return StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }
}
