package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.CorpusSearchContext;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptCorpusSearchServiceTest {

    @Mock
    private ThesaurusCorpusService thesaurusCorpusService;

    private ConceptCorpusSearchService service;

    @BeforeEach
    void setUp() {
        service = new ConceptCorpusSearchService(thesaurusCorpusService);
    }

    @Test
    void loadCorpusLinks_buildsOnlyUriLinkWithoutRemoteCall() {
        when(thesaurusCorpusService.listCorpus("TH1")).thenReturn(List.of(
                new ThesaurusCorpus(
                        "Gallica",
                        "http://gallica.bnf.fr/##id##",
                        "",
                        true,
                        true,
                        false,
                        1
                )
        ));

        var links = service.loadCorpusLinks("TH1", sampleContext());

        assertEquals(1, links.size());
        assertEquals("http://gallica.bnf.fr/C1", links.get(0).uriLink());
    }

    @Test
    void hasActiveCorpus_returnsTrueWhenActiveCorpusConfigured() {
        when(thesaurusCorpusService.hasActiveCorpus("TH1")).thenReturn(true);

        assertTrue(service.hasActiveCorpus("TH1"));
    }

    @Test
    void hasActiveCorpus_returnsFalseWhenNoActiveCorpus() {
        when(thesaurusCorpusService.hasActiveCorpus("TH1")).thenReturn(false);

        assertFalse(service.hasActiveCorpus("TH1"));
    }

    @Test
    void loadCorpusLinks_returnsEmptyWhenNoCorpusConfigured() {
        when(thesaurusCorpusService.listCorpus("TH1")).thenReturn(List.of());

        assertTrue(service.loadCorpusLinks("TH1", sampleContext()).isEmpty());
    }

    private CorpusSearchContext sampleContext() {
        return new CorpusSearchContext("C1", "", "Label");
    }
}
