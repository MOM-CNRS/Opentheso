package fr.cnrs.opentheso.v2.graph.ui;

import fr.cnrs.opentheso.v2.graph.service.GraphVisualizationUrlService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ConceptSelectionSource;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphExploreBeanTest {

    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ConceptSelectionSource conceptSelectionSource;
    @Mock
    private ApplicationUriService applicationUriService;
    @Mock
    private GraphVisualizationUrlService graphVisualizationUrlService;

    private GraphExploreBean bean;

    @BeforeEach
    void setUp() {
        bean = new GraphExploreBean(thesaurusContext, conceptSelectionSource, applicationUriService, graphVisualizationUrlService);
    }

    @Test
    void loadThesaurusGraph_syncsContextAndBuildsTreeDataUrl() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(applicationUriService.resolveApplicationRootUrl()).thenReturn("http://localhost:8080/opentheso/");
        when(graphVisualizationUrlService.buildThesaurusTreeDataUrl("http://localhost:8080/opentheso/", "TH1", "fr"))
                .thenReturn("http://localhost:8080/opentheso/openapi/v1/concept/TH1/thesoGraph?lang=fr");

        bean.loadThesaurusGraph();

        verify(thesaurusContext).syncFromViewParams();
        assertEquals("http://localhost:8080/opentheso/openapi/v1/concept/TH1/thesoGraph?lang=fr", bean.getTreeDataUrl());
    }

    @Test
    void loadBranchGraph_usesConceptIdFromUriWhenPresent() {
        bean.setConceptIdFromUri(" C1 ");
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(applicationUriService.resolveApplicationRootUrl()).thenReturn("http://localhost:8080/opentheso/");
        when(graphVisualizationUrlService.buildBranchTreeDataUrl("http://localhost:8080/opentheso/", "TH1", "C1", "fr"))
                .thenReturn("http://localhost:8080/opentheso/openapi/v1/concept/TH1/C1/graph/?lang=fr");

        bean.loadBranchGraph();

        assertEquals("http://localhost:8080/opentheso/openapi/v1/concept/TH1/C1/graph/?lang=fr", bean.getTreeDataUrl());
        verify(conceptSelectionSource, never()).getSelectedConceptId();
    }

    @Test
    void loadBranchGraph_fallsBackToThesaurusContextConceptWhenUriBlank() {
        bean.setConceptIdFromUri(null);
        when(thesaurusContext.getIdConceptFromUri()).thenReturn("C2");
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(applicationUriService.resolveApplicationRootUrl()).thenReturn("http://localhost:8080/opentheso/");

        bean.loadBranchGraph();

        verify(graphVisualizationUrlService).buildBranchTreeDataUrl("http://localhost:8080/opentheso/", "TH1", "C2", "fr");
        verify(conceptSelectionSource, never()).getSelectedConceptId();
    }

    @Test
    void loadBranchGraph_fallsBackToSelectionSourceWhenNoUriConcept() {
        bean.setConceptIdFromUri(null);
        when(thesaurusContext.getIdConceptFromUri()).thenReturn(null);
        when(conceptSelectionSource.getSelectedConceptId()).thenReturn(Optional.of("C3"));
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(applicationUriService.resolveApplicationRootUrl()).thenReturn("http://localhost:8080/opentheso/");

        bean.loadBranchGraph();

        verify(graphVisualizationUrlService).buildBranchTreeDataUrl("http://localhost:8080/opentheso/", "TH1", "C3", "fr");
    }

    @Test
    void loadBranchGraph_setsEmptyUrlWhenNothingResolved() {
        bean.setConceptIdFromUri(null);
        when(thesaurusContext.getIdConceptFromUri()).thenReturn(null);
        when(conceptSelectionSource.getSelectedConceptId()).thenReturn(Optional.empty());
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(applicationUriService.resolveApplicationRootUrl()).thenReturn("http://localhost:8080/opentheso/");

        bean.loadBranchGraph();

        assertEquals("", bean.getTreeDataUrl());
        verify(graphVisualizationUrlService, never()).buildBranchTreeDataUrl(any(), any(), any(), any());
    }
}
