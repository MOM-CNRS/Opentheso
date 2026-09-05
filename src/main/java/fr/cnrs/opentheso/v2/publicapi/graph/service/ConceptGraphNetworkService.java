package fr.cnrs.opentheso.v2.publicapi.graph.service;

import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.graph.api.dto.D3jsGraphNodeResponse;
import fr.cnrs.opentheso.v2.publicapi.graph.api.dto.D3jsGraphRelationshipResponse;
import fr.cnrs.opentheso.v2.publicapi.graph.api.dto.D3jsGraphResponse;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConceptGraphNetworkService {

    private static final String RESOURCE = "Resource";

    private static final int MAX_CONCEPTS = 2000;

    public record GraphRequestEntry(String thesaurusId, String conceptId) {
    }

    private final ConceptReadService conceptReadService;
    private final ConceptRepository conceptRepository;
    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    public D3jsGraphResponse buildGraph(List<GraphRequestEntry> entries, String lang, boolean limit) {
        Map<String, D3jsGraphNodeResponse> nodes = new LinkedHashMap<>();
        List<D3jsGraphRelationshipResponse> relationships = new ArrayList<>();
        Set<String> processedConcepts = new LinkedHashSet<>();

        for (GraphRequestEntry entry : entries) {
            String workLang = StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(entry.thesaurusId());
            addThesaurusNode(entry.thesaurusId(), nodes);

            List<String> conceptIds;
            if (StringUtils.isBlank(entry.conceptId())) {
                conceptIds = conceptRepository.findAllTopConceptIdsByThesaurus(entry.thesaurusId());
                for (String topConceptId : conceptIds) {
                    relationships.add(new D3jsGraphRelationshipResponse(
                            thesaurusUri(entry.thesaurusId()), conceptUri(entry.thesaurusId(), topConceptId), "skos__hasTopConcept"));
                }
                conceptIds = allConceptIdsOfThesaurus(entry.thesaurusId(), workLang, limit);
            } else {
                conceptIds = collectBranch(entry.thesaurusId(), entry.conceptId(), workLang, limit);
            }

            for (String conceptId : conceptIds) {
                String key = entry.thesaurusId() + "::" + conceptId;
                if (!processedConcepts.add(key)) {
                    continue;
                }
                addConceptNodeAndRelations(entry.thesaurusId(), conceptId, workLang, nodes, relationships);
            }
        }

        return new D3jsGraphResponse(new ArrayList<>(nodes.values()), relationships);
    }

    private List<String> allConceptIdsOfThesaurus(String thesaurusId, String lang, boolean limit) {
        var ids = conceptRepository.findAllTopConceptIdsByThesaurus(thesaurusId);
        Set<String> collected = new LinkedHashSet<>(ids);
        for (String topId : ids) {
            collected.addAll(collectBranch(thesaurusId, topId, lang, limit));
        }
        List<String> result = new ArrayList<>(collected);
        if (limit && result.size() > MAX_CONCEPTS) {
            return result.subList(0, MAX_CONCEPTS);
        }
        return result;
    }

    private List<String> collectBranch(String thesaurusId, String conceptId, String lang, boolean limit) {
        Set<String> visited = new LinkedHashSet<>();
        collectBranch(thesaurusId, conceptId, lang, limit, visited);
        return new ArrayList<>(visited);
    }

    private void collectBranch(String thesaurusId, String conceptId, String lang, boolean limit, Set<String> visited) {
        if (limit && visited.size() >= MAX_CONCEPTS || !visited.add(conceptId)) {
            return;
        }
        var detail = conceptReadService.loadDetail(thesaurusId, conceptId, lang).orElse(null);
        if (detail == null) {
            return;
        }
        for (ConceptRelation narrower : detail.narrowerTerms()) {
            collectBranch(thesaurusId, narrower.conceptId(), lang, limit, visited);
        }
    }

    private void addThesaurusNode(String thesaurusId, Map<String, D3jsGraphNodeResponse> nodes) {
        String uri = thesaurusUri(thesaurusId);
        nodes.putIfAbsent(uri, new D3jsGraphNodeResponse(
                uri, List.of(RESOURCE, "skos__ConceptScheme"), uri, List.of()));
    }

    private void addConceptNodeAndRelations(
            String thesaurusId,
            String conceptId,
            String lang,
            Map<String, D3jsGraphNodeResponse> nodes,
            List<D3jsGraphRelationshipResponse> relationships
    ) {
        var detail = conceptReadService.loadDetail(thesaurusId, conceptId, lang).orElse(null);
        if (detail == null) {
            return;
        }
        String uri = conceptUri(thesaurusId, conceptId);
        String prefLabel = detail.summary().preferredLabel();
        nodes.putIfAbsent(uri, new D3jsGraphNodeResponse(
                uri, List.of(RESOURCE, "skos__Concept"), uri, prefLabel != null ? List.of(prefLabel) : List.of()));

        addRelations(uri, detail.narrowerTerms(), "skos__narrower", thesaurusId, relationships);
        addRelations(uri, detail.broaderTerms(), "skos__broader", thesaurusId, relationships);
        addRelations(uri, detail.relatedTerms(), "skos__related", thesaurusId, relationships);
        addRelations(uri, detail.replacedBy(), "ns0__isReplacedBy", thesaurusId, relationships);
        addRelations(uri, detail.replaces(), "ns0__replace", thesaurusId, relationships);
        addRelations(uri, detail.collections(), "ns2__memberOf", thesaurusId, relationships);

        for (var alignmentGroup : detail.alignmentGroups()) {
            if (!"exactmatch".equalsIgnoreCase(alignmentGroup.typeKey())) {
                continue;
            }
            for (var alignment : alignmentGroup.items()) {
                if (StringUtils.isBlank(alignment.uri())) {
                    continue;
                }
                nodes.putIfAbsent(alignment.uri(), new D3jsGraphNodeResponse(
                        alignment.uri(), List.of(RESOURCE), alignment.uri(), List.of()));
                relationships.add(new D3jsGraphRelationshipResponse(uri, alignment.uri(), "skos__exactMatch"));
            }
        }

        relationships.add(new D3jsGraphRelationshipResponse(uri, thesaurusUri(thesaurusId), "skos__inScheme"));
    }

    private void addRelations(
            String sourceUri,
            List<ConceptRelation> relations,
            String label,
            String thesaurusId,
            List<D3jsGraphRelationshipResponse> relationships
    ) {
        for (ConceptRelation relation : relations) {
            relationships.add(new D3jsGraphRelationshipResponse(sourceUri, conceptUri(thesaurusId, relation.conceptId()), label));
        }
    }

    private String conceptUri(String thesaurusId, String conceptId) {
        return conceptSkosRdfExportEngine.findThesaurusPreferences(thesaurusId)
                .map(preferences -> preferences.getCheminSite() + "?idc=" + conceptId + "&idt=" + thesaurusId)
                .orElse(thesaurusId + "/" + conceptId);
    }

    private String thesaurusUri(String thesaurusId) {
        return conceptSkosRdfExportEngine.findThesaurusPreferences(thesaurusId)
                .map(preferences -> preferences.getCheminSite() + "?idt=" + thesaurusId)
                .orElse(thesaurusId);
    }
}
