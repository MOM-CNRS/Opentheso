package fr.cnrs.opentheso.v2.publicapi.thesaurus.service;

import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.PublicThesaurusSummaryResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusFlatEntryResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusLanguagesResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusLastUpdateResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusTopConceptResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.persistence.ThesaurusPublicQueryRepository;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesaurusPublicReadService {

    private final ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    private final ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    private final ConceptRepository conceptRepository;
    private final TermRepository termRepository;
    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    private final ThesaurusPublicQueryRepository thesaurusPublicQueryRepository;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    private final ThesaurusRepository thesaurusRepository;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;

    public ExportResult exportThesaurus(String thesaurusId, String formatCode) throws IOException {
        try {
            var document = thesaurusSkosDocumentBuilder.buildFullDocument(thesaurusId);
            var format = SkosRdfFormatSupport.resolveExportFormat(formatCode);
            byte[] content = conceptSkosRdfExportEngine.serializeSkos(document, format.rdfFormat());
            return new ExportResult(content, thesaurusId + format.extension(), "application/xml");
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Export SKOS du thésaurus impossible", ex);
        }
    }

    public List<ThesaurusFlatEntryResponse> flatList(String thesaurusId, String lang) {
        String workLang = resolveLang(thesaurusId, lang);
        return thesaurusPublicQueryRepository.findFlatConceptList(thesaurusId, workLang).stream()
                .map(item -> new ThesaurusFlatEntryResponse(item.conceptId(), item.label()))
                .toList();
    }

    public List<ThesaurusTopConceptResponse> topConcepts(String thesaurusId) {
        return conceptRepository.findAllTopConceptsWithUris(thesaurusId).stream()
                .map(nodeUri -> {
                    var translations = termRepository.findAllTraductionsOfConcept(nodeUri.getIdConcept(), thesaurusId)
                            .stream()
                            .map(t -> new ThesaurusTopConceptResponse.Translation(t.getLang(), t.getLexicalValue()))
                            .toList();
                    return new ThesaurusTopConceptResponse(
                            nodeUri.getIdConcept(),
                            nodeUri.getIdArk(),
                            nodeUri.getIdHandle(),
                            translations
                    );
                })
                .toList();
    }

    public ThesaurusLastUpdateResponse lastUpdate(String thesaurusId) {
        return thesaurusHomeQueryRepository.findLastModificationDate(thesaurusId)
                .map(date -> new ThesaurusLastUpdateResponse(date.toInstant()))
                .orElseThrow(() -> new PublicResourceNotFoundException("Aucune date de modification pour le thésaurus " + thesaurusId));
    }

    public ThesaurusLanguagesResponse usedLanguages(String thesaurusId) {
        return new ThesaurusLanguagesResponse(termRepository.searchDistinctLangInThesaurus(thesaurusId));
    }

    public List<PublicThesaurusSummaryResponse> listPublicThesauri() {
        return thesaurusRepository.findAllByIsPrivateFalseOrderByCreatedDesc().stream()
                .map(thesaurus -> {
                    String thesaurusId = thesaurus.getIdThesaurus();
                    String type = thesaurusDcTermRepository.findAllByIdThesaurus(thesaurusId).stream()
                            .filter(term -> "type".equalsIgnoreCase(term.getName()))
                            .map(term -> term.getValue())
                            .findFirst()
                            .orElse("");
                    var labels = thesaurusLabelRepository.findDistinctLangByIdThesaurus(thesaurusId).stream()
                            .flatMap(lang -> thesaurusLabelRepository.findByIdThesaurusAndLang(thesaurusId, lang).stream())
                            .map(label -> new PublicThesaurusSummaryResponse.Translation(label.getLang(), label.getTitle()))
                            .toList();
                    return new PublicThesaurusSummaryResponse(thesaurusId, type, labels);
                })
                .toList();
    }

    private String resolveLang(String thesaurusId, String lang) {
        return StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }
}
