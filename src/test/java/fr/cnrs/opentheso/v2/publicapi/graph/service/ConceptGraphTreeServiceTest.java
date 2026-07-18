package fr.cnrs.opentheso.v2.publicapi.graph.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.ThesaurusLabel;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptGraphTreeServiceTest {

    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ThesaurusLabelRepository thesaurusLabelRepository;
    @Mock
    private ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    private ConceptGraphTreeService service;

    @BeforeEach
    void setUp() {
        service = new ConceptGraphTreeService(
                conceptReadService, conceptRepository, thesaurusLabelRepository, conceptSkosRdfExportEngine, thesaurusWorkLanguageService);
        lenient().when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
        lenient().when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1"))
                .thenReturn(Optional.of(Preferences.builder().cheminSite("https://site/").build()));
    }

    private ConceptDetail leafDetail(String conceptId, List<ConceptNote> notes, List<ConceptImageItem> images, List<String> synonyms) {
        var summary = new ConceptSummary(conceptId, "TH1", "Label " + conceptId, "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");
        return new ConceptDetail(
                summary, List.of(), List.of(), List.of(), List.of(),
                synonyms, List.of(), List.of(), notes,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(List.of()), images == null ? List.of() : images, List.of(), List.of(), List.of(), List.of(),
                null, "", "");
    }

    private ConceptDetail withNarrower(String conceptId, List<ConceptRelation> narrower) {
        var summary = new ConceptSummary(conceptId, "TH1", "Label " + conceptId, "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin");
        return new ConceptDetail(
                summary, List.of(), List.of(), narrower, List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void buildThesaurusTree_buildsRootWithTopConceptsAsChildren() {
        when(thesaurusLabelRepository.findByIdThesaurusAndLang("TH1", "fr"))
                .thenReturn(Optional.of(ThesaurusLabel.builder().title("Mon thésaurus").build()));
        when(conceptRepository.findAllTopConceptIdsByThesaurus("TH1")).thenReturn(List.of("C1"));
        when(conceptReadService.loadDetail("TH1", "C1", "fr"))
                .thenReturn(Optional.of(leafDetail("C1", List.of(), List.of(), List.of())));

        var tree = service.buildThesaurusTree("TH1", null, false);

        assertEquals("Mon thésaurus", tree.name());
        assertEquals("type1", tree.type());
        assertEquals("https://site/?idt=TH1", tree.url());
        assertEquals(1, tree.children().size());
        assertEquals("Label C1", tree.children().get(0).name());
        assertEquals("type3", tree.children().get(0).type());
    }

    @Test
    void buildThesaurusTree_fallsBackToThesaurusIdWhenNoLabel() {
        when(thesaurusLabelRepository.findByIdThesaurusAndLang("TH1", "fr")).thenReturn(Optional.empty());
        when(conceptRepository.findAllTopConceptIdsByThesaurus("TH1")).thenReturn(List.of());

        var tree = service.buildThesaurusTree("TH1", null, false);

        assertEquals("TH1", tree.name());
        assertTrue(tree.children().isEmpty());
    }

    @Test
    void buildConceptTree_mapsDefinitionsImagesSynonymsAndChildren() {
        var childDetail = leafDetail("C2", List.of(), List.of(), List.of());
        var rootNotes = List.of(new ConceptNote("N1", "definition", "fr", "Une définition"));
        var rootImages = List.of(new ConceptImageItem(1, "img.png", "copy", "creator", "https://img/1.png"));
        var rootDetail = new ConceptDetail(
                new ConceptSummary("C1", "TH1", "Label C1", "fr", "C", "ark", "concept", "N1", "2024", "2025", "admin"),
                List.of(), List.of(), List.of(new ConceptRelation("C2", "Label C2", "arkC2")), List.of(),
                List.of("Synonyme"), List.of(), List.of(), rootNotes,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(List.of()), rootImages, List.of(), List.of(), List.of(), List.of(),
                null, "", "");

        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(rootDetail));
        when(conceptReadService.loadDetail("TH1", "C2", "fr")).thenReturn(Optional.of(childDetail));

        var tree = service.buildConceptTree("TH1", "C1", null, false);

        assertEquals("type1", tree.type());
        assertEquals("Label C1", tree.name());
        assertEquals(List.of("Une définition"), tree.definition());
        assertEquals(List.of("https://img/1.png"), tree.image());
        assertEquals(List.of("Synonyme"), tree.synonym());
        assertEquals(1, tree.children().size());
        assertEquals("Label C2", tree.children().get(0).name());
        assertEquals("type3", tree.children().get(0).type());
    }

    @Test
    void buildConceptTree_throwsWhenConceptNotFound() {
        when(conceptReadService.loadDetail("TH1", "C9", "fr")).thenReturn(Optional.empty());

        assertThrows(PublicResourceNotFoundException.class, () -> service.buildConceptTree("TH1", "C9", null, false));
    }

    @Test
    void buildConceptTree_marksNodeAsType2WhenItHasChildren() {
        var childDetail = leafDetail("C2", List.of(), List.of(), List.of());
        var rootDetail = withNarrower("C1", List.of(new ConceptRelation("C2", "Label C2", "arkC2")));

        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(rootDetail));
        when(conceptReadService.loadDetail("TH1", "C2", "fr")).thenReturn(Optional.of(childDetail));

        var tree = service.buildConceptTree("TH1", "C1", null, false);

        assertEquals(1, tree.children().size());
    }
}
