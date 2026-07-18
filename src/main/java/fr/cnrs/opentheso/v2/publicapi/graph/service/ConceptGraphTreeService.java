package fr.cnrs.opentheso.v2.publicapi.graph.service;

import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.publicapi.graph.api.dto.D3jsTreeNodeResponse;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ConceptGraphTreeService {

    private static final int MAX_NODES = 2000;

    private final ConceptReadService conceptReadService;
    private final ConceptRepository conceptRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    public D3jsTreeNodeResponse buildThesaurusTree(String thesaurusId, String lang, boolean limit) {
        String workLang = resolveLang(thesaurusId, lang);
        String title = thesaurusLabelRepository.findByIdThesaurusAndLang(thesaurusId, workLang)
                .map(label -> label.getTitle())
                .orElse(thesaurusId);
        String url = buildThesaurusUrl(thesaurusId);

        var counter = new AtomicInteger(0);
        List<D3jsTreeNodeResponse> children = conceptRepository.findAllTopConceptIdsByThesaurus(thesaurusId).stream()
                .map(id -> buildNode(thesaurusId, id, workLang, limit, counter))
                .filter(Objects::nonNull)
                .toList();

        return new D3jsTreeNodeResponse(title, "type1", url, List.of(), List.of(), List.of(), children);
    }

    public D3jsTreeNodeResponse buildConceptTree(String thesaurusId, String conceptId, String lang, boolean limit) {
        String workLang = resolveLang(thesaurusId, lang);
        var counter = new AtomicInteger(0);
        var node = buildNode(thesaurusId, conceptId, workLang, limit, counter);
        if (node == null) {
            throw new PublicResourceNotFoundException("Concept introuvable : " + conceptId);
        }
        return new D3jsTreeNodeResponse(
                node.name(), "type1", node.url(), node.definition(), node.image(), node.synonym(), node.children());
    }

    private D3jsTreeNodeResponse buildNode(String thesaurusId, String conceptId, String lang, boolean limit, AtomicInteger counter) {
        if (limit && counter.get() > MAX_NODES) {
            return null;
        }
        counter.incrementAndGet();

        var detail = conceptReadService.loadDetail(thesaurusId, conceptId, lang).orElse(null);
        if (detail == null) {
            return null;
        }

        String name = StringUtils.isNotBlank(detail.summary().preferredLabel())
                ? detail.summary().preferredLabel()
                : "(" + conceptId + ")";
        String url = buildConceptUrl(thesaurusId, conceptId);
        List<String> definitions = detail.notesOfType("definition").stream().map(ConceptNote::value).toList();
        List<String> images = detail.images().stream().map(ConceptImageItem::uri).toList();
        List<String> synonyms = detail.synonyms();

        List<D3jsTreeNodeResponse> children = detail.narrowerTerms().stream()
                .map(relation -> buildNode(thesaurusId, relation.conceptId(), lang, limit, counter))
                .filter(Objects::nonNull)
                .toList();

        String type = children.isEmpty() ? "type3" : "type2";
        return new D3jsTreeNodeResponse(name, type, url, definitions, images, synonyms, children);
    }

    private String buildConceptUrl(String thesaurusId, String conceptId) {
        return conceptSkosRdfExportEngine.findThesaurusPreferences(thesaurusId)
                .map(preferences -> preferences.getCheminSite() + "?idc=" + conceptId + "&idt=" + thesaurusId)
                .orElse("");
    }

    private String buildThesaurusUrl(String thesaurusId) {
        return conceptSkosRdfExportEngine.findThesaurusPreferences(thesaurusId)
                .map(preferences -> preferences.getCheminSite() + "?idt=" + thesaurusId)
                .orElse("");
    }

    private String resolveLang(String thesaurusId, String lang) {
        return StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }
}
