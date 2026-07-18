package fr.cnrs.opentheso.v2.publicapi.resolver.service;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfExportEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArkRedirectServiceTest {

    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ThesaurusRepository thesaurusRepository;
    @Mock
    private ConceptGroupRepository conceptGroupRepository;
    @Mock
    private ConceptSkosRdfExportEngine conceptSkosRdfExportEngine;

    private ArkRedirectService service;

    @BeforeEach
    void setUp() {
        service = new ArkRedirectService(conceptRepository, thesaurusRepository, conceptGroupRepository, conceptSkosRdfExportEngine);
        lenient().when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH1"))
                .thenReturn(Optional.of(Preferences.builder().cheminSite("https://site/").build()));
        lenient().when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH2"))
                .thenReturn(Optional.of(Preferences.builder().cheminSite("https://site/").build()));
        lenient().when(conceptSkosRdfExportEngine.findThesaurusPreferences("TH3"))
                .thenReturn(Optional.of(Preferences.builder().cheminSite("https://site/").build()));
    }

    @Test
    void buildRedirectUrl_resolvesConcept() {
        when(conceptRepository.findIdThesaurusListByArkId("naan/ark1")).thenReturn("TH1");
        when(conceptRepository.findConceptIdByArkIgnoreCase("naan/ark1", "TH1")).thenReturn(Optional.of("C1"));

        var url = service.buildRedirectUrl("naan", "ark1");

        assertEquals(Optional.of("https://site/?idc=C1&idt=TH1"), url);
    }

    @Test
    void buildRedirectUrl_resolvesThesaurusWhenNoConceptMatch() {
        when(conceptRepository.findIdThesaurusListByArkId("naan/ark2")).thenReturn(null);
        when(thesaurusRepository.findIdThesaurusByArkId("naan/ark2")).thenReturn(Optional.of("TH2"));

        var url = service.buildRedirectUrl("naan", "ark2");

        assertEquals(Optional.of("https://site/?idt=TH2"), url);
    }

    @Test
    void buildRedirectUrl_resolvesGroupWhenNoConceptOrThesaurusMatch() {
        when(conceptRepository.findIdThesaurusListByArkId("naan/ark3")).thenReturn(null);
        when(thesaurusRepository.findIdThesaurusByArkId("naan/ark3")).thenReturn(Optional.empty());
        when(conceptGroupRepository.findThesaurusIdByArkId("naan/ark3")).thenReturn("TH3");
        when(conceptGroupRepository.findAllByIdThesaurusAndIdArk("TH3", "naan/ark3"))
                .thenReturn(Optional.of(ConceptGroup.builder().idGroup("G1").build()));

        var url = service.buildRedirectUrl("naan", "ark3");

        assertEquals(Optional.of("https://site/?idg=G1&idt=TH3"), url);
    }

    @Test
    void buildRedirectUrl_returnsEmptyWhenNothingResolves() {
        when(conceptRepository.findIdThesaurusListByArkId("naan/ark4")).thenReturn(null);
        when(thesaurusRepository.findIdThesaurusByArkId("naan/ark4")).thenReturn(Optional.empty());
        when(conceptGroupRepository.findThesaurusIdByArkId("naan/ark4")).thenReturn(null);

        var url = service.buildRedirectUrl("naan", "ark4");

        assertTrue(url.isEmpty());
    }

    @Test
    void buildRedirectUrl_returnsEmptyForBlankArkId() {
        var url = service.buildRedirectUrl("naan", "");

        assertTrue(url.isEmpty());
    }
}
