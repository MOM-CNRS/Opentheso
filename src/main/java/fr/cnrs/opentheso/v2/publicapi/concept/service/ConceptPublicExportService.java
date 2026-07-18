package fr.cnrs.opentheso.v2.publicapi.concept.service;

import fr.cnrs.opentheso.models.NodeIdValueProjection;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptLabelResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptRelationResponse;
import fr.cnrs.opentheso.v2.concept.api.mapper.ConceptApiMapper;
import fr.cnrs.opentheso.v2.concept.export.service.ConceptSkosExportService;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.concept.api.dto.OntomeLinkedConceptResponse;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConceptPublicExportService {

    private static final int MAX_BRANCH_NODES = 500;
    private static final int MAX_BRANCH_DEPTH = 50;

    private final ConceptReadService conceptReadService;
    private final ConceptSkosExportService conceptSkosExportService;
    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    private final ConceptRepository conceptRepository;
    private final AlignementRepository alignementRepository;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    public ExportResult exportConcept(String thesaurusId, String conceptId, String formatCode) throws IOException {
        return conceptSkosExportService.exportConcept(thesaurusId, conceptId, formatCode);
    }

    public List<ConceptLabelResponse> loadLabels(String thesaurusId, String conceptId, String lang) {
        String workLang = resolveLang(thesaurusId, lang);
        return conceptReadService.loadDetail(thesaurusId, conceptId, workLang)
                .map(detail -> detail.translations().stream().map(ConceptApiMapper::toLabel).toList())
                .orElseThrow(() -> new PublicResourceNotFoundException("Concept introuvable : " + conceptId));
    }

    public List<ConceptRelationResponse> loadNarrower(String thesaurusId, String conceptId, String lang) {
        String workLang = resolveLang(thesaurusId, lang);
        return conceptReadService.loadDetail(thesaurusId, conceptId, workLang)
                .map(detail -> detail.narrowerTerms().stream().map(ConceptApiMapper::toRelation).toList())
                .orElseThrow(() -> new PublicResourceNotFoundException("Concept introuvable : " + conceptId));
    }

    public ExportResult exportExpansion(String thesaurusId, String conceptId, String way, String formatCode) throws IOException {
        String workLang = resolveLang(thesaurusId, null);
        Set<String> branchConceptIds = new LinkedHashSet<>();
        collectBranch(thesaurusId, conceptId, way, workLang, branchConceptIds, 0);
        return exportConceptIds(thesaurusId, branchConceptIds, formatCode);
    }

    public ExportResult exportModifiedSince(String thesaurusId, String dateStr, String formatCode) throws IOException {
        try {
            LocalDate localDate = LocalDate.parse(dateStr);
            Date startDate = Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant());
            List<String> conceptIds = conceptRepository.findConceptsModifiedSince(thesaurusId, startDate);
            return exportConceptIds(thesaurusId, new LinkedHashSet<>(conceptIds), formatCode);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Format de date invalide : " + dateStr, ex);
        }
    }

    public List<OntomeLinkedConceptResponse> loadOntomeLinkedConcepts(String thesaurusId, String cidocClass) {
        List<NodeIdValueProjection> projections = StringUtils.isBlank(cidocClass)
                ? alignementRepository.findAllLinkedConceptsWithOntome(thesaurusId)
                : alignementRepository.findLinkedConceptsWithOntome(thesaurusId, cidocClass);
        return projections.stream()
                .map(p -> new OntomeLinkedConceptResponse(p.getInternal_id_concept(), p.getUri_target()))
                .toList();
    }

    private ExportResult exportConceptIds(String thesaurusId, Set<String> conceptIds, String formatCode) throws IOException {
        if (conceptIds.isEmpty()) {
            throw new PublicResourceNotFoundException("Aucun concept trouvé pour l'export");
        }
        SKOSXmlDocument document = new SKOSXmlDocument();
        for (String id : conceptIds) {
            var resource = conceptSkosRdfExportEngine.exportConcept(thesaurusId, id);
            if (resource != null) {
                document.addconcept(resource);
            }
        }
        var format = SkosRdfFormatSupport.resolveExportFormat(formatCode);
        byte[] content = conceptSkosRdfExportEngine.serializeSkos(document, format.rdfFormat());
        return new ExportResult(content, thesaurusId + "_branch" + format.extension(), "application/xml");
    }

    private void collectBranch(
            String thesaurusId,
            String conceptId,
            String way,
            String lang,
            Set<String> visited,
            int depth
    ) {
        if (depth > MAX_BRANCH_DEPTH || visited.size() >= MAX_BRANCH_NODES || !visited.add(conceptId)) {
            return;
        }
        var detail = conceptReadService.loadDetail(thesaurusId, conceptId, lang).orElse(null);
        if (detail == null) {
            return;
        }
        var relations = "top".equalsIgnoreCase(way) ? detail.broaderTerms() : detail.narrowerTerms();
        for (var relation : relations) {
            collectBranch(thesaurusId, relation.conceptId(), way, lang, visited, depth + 1);
        }
    }

    private String resolveLang(String thesaurusId, String lang) {
        return StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }
}
