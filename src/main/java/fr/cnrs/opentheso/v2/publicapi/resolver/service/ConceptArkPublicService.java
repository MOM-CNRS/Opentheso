package fr.cnrs.opentheso.v2.publicapi.resolver.service;

import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.v2.concept.api.mapper.ConceptApiMapper;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.service.ConceptBreadcrumbReadService;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ArkFullPathResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ConceptChildrenArkResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ConceptPrefLabelResponse;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptArkPublicService {

    private final ConceptRepository conceptRepository;
    private final ThesaurusRepository thesaurusRepository;
    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    private final ConceptReadService conceptReadService;
    private final ConceptBreadcrumbReadService conceptBreadcrumbReadService;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    public ExportResult exportByArk(String naan, String arkId, String formatCode) throws IOException {
        String fullArkId = naan + "/" + arkId;
        String thesaurusId = conceptRepository.findIdThesaurusListByArkId(fullArkId);
        String conceptId = StringUtils.isNotBlank(thesaurusId)
                ? conceptRepository.findConceptIdByArkIgnoreCase(fullArkId, thesaurusId).orElse(null)
                : null;
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(conceptId)) {
            throw new PublicResourceNotFoundException("Aucun concept trouvé pour l'identifiant ARK : " + fullArkId);
        }
        var resource = conceptSkosRdfExportEngine.exportConcept(thesaurusId, conceptId);
        var document = new fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument();
        document.addconcept(resource);
        var format = SkosRdfFormatSupport.resolveExportFormat(formatCode);
        byte[] content = conceptSkosRdfExportEngine.serializeSkos(document, format.rdfFormat());
        return new ExportResult(content, thesaurusId + "_" + conceptId + format.extension(), "application/xml");
    }

    public ConceptChildrenArkResponse loadChildrenArkIds(String naan, String arkId) {
        String fullArkId = naan + "/" + arkId;
        String thesaurusId = thesaurusRepository.findIdThesaurusByArkId(fullArkId).orElse(null);
        String conceptId = StringUtils.isNotBlank(thesaurusId)
                ? conceptRepository.findConceptIdByArkIgnoreCase(fullArkId, thesaurusId).orElse(null)
                : null;
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(conceptId)) {
            return new ConceptChildrenArkResponse(0, List.of());
        }
        List<String> childArkIds = conceptRepository.findArkIdsOfChildren(thesaurusId, conceptId);
        return new ConceptChildrenArkResponse(childArkIds.size(), childArkIds);
    }

    public ConceptPrefLabelResponse loadPrefLabel(String naan, String arkId, String lang) {
        String fullArkId = naan + "/" + arkId;
        String thesaurusId = thesaurusRepository.findIdThesaurusByArkId(fullArkId).orElse(null);
        String conceptId = StringUtils.isNotBlank(thesaurusId)
                ? conceptRepository.findConceptIdByArkIgnoreCase(fullArkId, thesaurusId).orElse(null)
                : null;
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(conceptId)) {
            throw new PublicResourceNotFoundException("Aucun concept trouvé pour l'identifiant ARK : " + fullArkId);
        }
        String workLang = StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
        return conceptReadService.loadSummary(thesaurusId, conceptId, workLang)
                .map(summary -> new ConceptPrefLabelResponse(summary.preferredLabel()))
                .orElseThrow(() -> new PublicResourceNotFoundException("Concept introuvable : " + conceptId));
    }

    public ExportResult exportByHandle(String handle, String idHandle, String formatCode) throws IOException {
        String fullHandleId = handle + "/" + idHandle;
        var concept = conceptRepository.findByIdHandle(fullHandleId).orElse(null);
        String thesaurusId = concept != null ? concept.getIdThesaurus() : null;
        String conceptId = StringUtils.isNotBlank(thesaurusId)
                ? conceptRepository.findConceptIdByHandleIgnoreCase(fullHandleId).orElse(null)
                : null;
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(conceptId)) {
            throw new PublicResourceNotFoundException("Aucun concept trouvé pour l'identifiant Handle : " + fullHandleId);
        }
        var resource = conceptSkosRdfExportEngine.exportConcept(thesaurusId, conceptId);
        var document = new fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument();
        document.addconcept(resource);
        var format = SkosRdfFormatSupport.resolveExportFormat(formatCode);
        byte[] content = conceptSkosRdfExportEngine.serializeSkos(document, format.rdfFormat());
        return new ExportResult(content, thesaurusId + "_" + conceptId + format.extension(), "application/xml");
    }

    public List<ArkFullPathResponse> fullPathByArk(List<String> arkIds, String lang) {
        return arkIds.stream()
                .map(arkId -> resolveFullPath(arkId, lang))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private ArkFullPathResponse resolveFullPath(String arkId, String lang) {
        String thesaurusId = conceptRepository.findIdThesaurusListByArkId(arkId);
        if (StringUtils.isBlank(thesaurusId)) {
            return null;
        }
        String conceptId = conceptRepository.findConceptIdByArkIgnoreCase(arkId, thesaurusId).orElse(null);
        if (StringUtils.isBlank(conceptId)) {
            return null;
        }
        String workLang = StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
        var paths = conceptBreadcrumbReadService.loadBreadcrumbPaths(thesaurusId, conceptId, workLang).stream()
                .map(path -> path.stream().map(ConceptApiMapper::toBreadcrumb).toList())
                .toList();
        return new ArkFullPathResponse(arkId, thesaurusId, conceptId, paths);
    }
}
