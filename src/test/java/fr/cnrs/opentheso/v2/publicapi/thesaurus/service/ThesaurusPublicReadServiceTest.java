package fr.cnrs.opentheso.v2.publicapi.thesaurus.service;

import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.entites.ThesaurusLabel;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.model.ConceptLinkItem;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.persistence.ThesaurusPublicQueryRepository;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusPublicReadServiceTest {

    @Mock
    private ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    @Mock
    private ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    @Mock
    private ThesaurusPublicQueryRepository thesaurusPublicQueryRepository;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private ThesaurusRepository thesaurusRepository;
    @Mock
    private ThesaurusDcTermRepository thesaurusDcTermRepository;
    @Mock
    private ThesaurusLabelRepository thesaurusLabelRepository;

    private ThesaurusPublicReadService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusPublicReadService(
                thesaurusSkosDocumentBuilder,
                conceptSkosRdfExportEngine,
                conceptRepository,
                termRepository,
                thesaurusHomeQueryRepository,
                thesaurusPublicQueryRepository,
                thesaurusWorkLanguageService,
                thesaurusRepository,
                thesaurusDcTermRepository,
                thesaurusLabelRepository
        );
    }

    @Test
    void exportThesaurus_buildsAndSerializesFullDocument() throws Exception {
        var document = new SKOSXmlDocument();
        when(thesaurusSkosDocumentBuilder.buildFullDocument("TH1")).thenReturn(document);
        when(conceptSkosRdfExportEngine.serializeSkos(any(SKOSXmlDocument.class), any(RDFFormat.class)))
                .thenReturn(new byte[]{5});

        var result = service.exportThesaurus("TH1", "skos");

        assertEquals("TH1.rdf", result.filename());
    }

    @Test
    void exportThesaurus_wrapsBuildFailureAsIllegalState() throws Exception {
        when(thesaurusSkosDocumentBuilder.buildFullDocument("TH1")).thenThrow(new IllegalStateException("Préférences introuvables"));

        assertThrows(IllegalStateException.class, () -> service.exportThesaurus("TH1", "skos"));
    }

    @Test
    void flatList_mapsConceptLinkItems() {
        lenient().when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
        when(thesaurusPublicQueryRepository.findFlatConceptList("TH1", "fr"))
                .thenReturn(List.of(new ConceptLinkItem("C1", "Label")));

        var response = service.flatList("TH1", null);

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).conceptId());
        assertEquals("Label", response.get(0).label());
    }

    @Test
    void topConcepts_mapsNodeUriWithTranslations() {
        var nodeUri = NodeUri.builder().idArk("ark1").idHandle("hdl1").idDoi("").idConcept("C1").build();
        when(conceptRepository.findAllTopConceptsWithUris("TH1")).thenReturn(List.of(nodeUri));
        when(termRepository.findAllTraductionsOfConcept("C1", "TH1"))
                .thenReturn(List.of(new NodeTermTraduction("Label FR", "fr")));

        var response = service.topConcepts("TH1");

        assertEquals(1, response.size());
        assertEquals("C1", response.get(0).conceptId());
        assertEquals("ark1", response.get(0).arkId());
        assertEquals(1, response.get(0).translations().size());
        assertEquals("fr", response.get(0).translations().get(0).lang());
    }

    @Test
    void lastUpdate_returnsMappedInstant() {
        var date = new Date(1_700_000_000_000L);
        when(thesaurusHomeQueryRepository.findLastModificationDate("TH1")).thenReturn(Optional.of(date));

        var response = service.lastUpdate("TH1");

        assertEquals(date.toInstant(), response.lastModification());
    }

    @Test
    void lastUpdate_throwsWhenAbsent() {
        when(thesaurusHomeQueryRepository.findLastModificationDate("TH1")).thenReturn(Optional.empty());

        assertThrows(PublicResourceNotFoundException.class, () -> service.lastUpdate("TH1"));
    }

    @Test
    void usedLanguages_returnsDistinctLanguageList() {
        when(termRepository.searchDistinctLangInThesaurus("TH1")).thenReturn(List.of("fr", "en"));

        var response = service.usedLanguages("TH1");

        assertEquals(List.of("fr", "en"), response.languages());
    }

    @Test
    void listPublicThesauri_mapsTypeAndTranslations() {
        when(thesaurusRepository.findAllByIsPrivateFalseOrderByCreatedDesc())
                .thenReturn(List.of(Thesaurus.builder().idThesaurus("TH1").build()));
        when(thesaurusDcTermRepository.findAllByIdThesaurus("TH1"))
                .thenReturn(List.of(ThesaurusDcTerm.builder().name("type").value("siamois").build()));
        when(thesaurusLabelRepository.findDistinctLangByIdThesaurus("TH1")).thenReturn(List.of("fr"));
        when(thesaurusLabelRepository.findByIdThesaurusAndLang("TH1", "fr"))
                .thenReturn(Optional.of(ThesaurusLabel.builder().idThesaurus("TH1").lang("fr").title("Mon thésaurus").build()));

        var response = service.listPublicThesauri();

        assertEquals(1, response.size());
        assertEquals("TH1", response.get(0).thesaurusId());
        assertEquals("siamois", response.get(0).type());
        assertEquals(1, response.get(0).labels().size());
        assertEquals("fr", response.get(0).labels().get(0).lang());
        assertEquals("Mon thésaurus", response.get(0).labels().get(0).title());
    }

    @Test
    void listPublicThesauri_defaultsTypeToEmptyWhenNoDcTerm() {
        when(thesaurusRepository.findAllByIsPrivateFalseOrderByCreatedDesc())
                .thenReturn(List.of(Thesaurus.builder().idThesaurus("TH2").build()));
        when(thesaurusDcTermRepository.findAllByIdThesaurus("TH2")).thenReturn(List.of());
        when(thesaurusLabelRepository.findDistinctLangByIdThesaurus("TH2")).thenReturn(List.of());

        var response = service.listPublicThesauri();

        assertEquals("", response.get(0).type());
        assertEquals(List.of(), response.get(0).labels());
    }
}
