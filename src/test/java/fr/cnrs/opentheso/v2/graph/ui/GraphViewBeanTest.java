package fr.cnrs.opentheso.v2.graph.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchKind;
import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import fr.cnrs.opentheso.v2.graph.service.GraphConceptSearchService;
import fr.cnrs.opentheso.v2.graph.service.GraphNeo4jExportService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewCommandService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewReadService;
import fr.cnrs.opentheso.v2.graph.service.GraphVisualizationUrlService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelection;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import jakarta.faces.event.AjaxBehaviorEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.component.chip.Chip;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphViewBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private GraphViewReadService graphViewReadService;
    @Mock
    private GraphViewCommandService graphViewCommandService;
    @Mock
    private GraphVisualizationUrlService graphVisualizationUrlService;
    @Mock
    private GraphNeo4jExportService graphNeo4jExportService;
    @Mock
    private GraphConceptSearchService graphConceptSearchService;
    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ApplicationUriService applicationUriService;
    @Mock
    private AjaxBehaviorEvent event;
    @Mock
    private Chip chip;

    private GraphViewBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        bean = new GraphViewBean(
                userSession,
                graphViewReadService,
                graphViewCommandService,
                graphVisualizationUrlService,
                graphNeo4jExportService,
                graphConceptSearchService,
                thesaurusSelectionService,
                thesaurusWorkLanguageService,
                conceptReadService,
                applicationUriService
        );
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
    }

    @Test
    void isScreenAvailable_falseWhenNotLoggedIn() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertEquals(false, bean.isScreenAvailable());
    }

    @Test
    void isScreenAvailable_trueWhenLoggedIn() {
        when(userSession.isLoggedIn()).thenReturn(true);

        assertTrue(bean.isScreenAvailable());
    }

    @Test
    void load_doesNothingWhenScreenNotAvailable() {
        when(userSession.isLoggedIn()).thenReturn(false);

        bean.load();

        verify(graphViewReadService, never()).reloadViewsForUser(anyInt());
    }

    @Test
    void load_refreshesViewsWhenScreenAvailable() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());

        bean.load();

        verify(graphViewReadService).reloadViewsForUser(7);
    }

    @Test
    void refreshViews_emptyListWhenNoCurrentUser() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        bean.refreshViews();

        assertEquals(List.of(), bean.getGraphViews());
        verify(graphViewReadService, never()).reloadViewsForUser(anyInt());
    }

    @Test
    void refreshViews_loadsViewsAndResetsSelection() throws Exception {
        when(userSession.getCurrentUserId()).thenReturn(7);
        var summary = new GraphViewSummary(1, "Vue", "desc");
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of(summary));
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost:8080/opentheso");
        when(graphVisualizationUrlService.resolveWorkLanguageForThesaurus(null)).thenReturn("fr");
        when(graphVisualizationUrlService.buildVisualizationUrl(summary, "http://localhost:8080/opentheso", "fr"))
                .thenReturn("http://localhost:8080/opentheso/v2/graph/visualize/force.xhtml?dataUrl=x");
        bean.setSelectedIdTheso("TH1");

        bean.refreshViews();

        assertEquals(List.of(summary), bean.getGraphViews());
        assertEquals("http://localhost:8080/opentheso/v2/graph/visualize/force.xhtml?dataUrl=x", summary.getVisualizationUrl());
        assertNull(bean.getSelectedIdTheso());
        assertNull(bean.getSearchSelected());
    }

    @Test
    void initNewViewDialog_resetsFields() {
        bean.setSelectedViewId(5);
        bean.setNewViewName("old");

        bean.initNewViewDialog();

        assertEquals(-1, bean.getSelectedViewId());
        assertNull(bean.getNewViewName());
        assertNull(bean.getNewViewDescription());
        assertEquals(0, bean.getNewViewExports().size());
    }

    @Test
    void initEditViewDialog_doesNothingWhenViewNotFound() {
        when(graphViewReadService.loadView("99")).thenReturn(null);

        bean.initEditViewDialog("99");

        assertEquals(-1, bean.getSelectedViewId());
    }

    @Test
    void initEditViewDialog_populatesFieldsFromView() {
        var summary = new GraphViewSummary(3, "Vue", "desc");
        summary.setExports(List.of(new GraphExportEntry("TH1", null)));
        when(graphViewReadService.loadView("3")).thenReturn(summary);

        bean.initEditViewDialog("3");

        assertEquals(3, bean.getSelectedViewId());
        assertEquals("Vue", bean.getNewViewName());
        assertEquals("desc", bean.getNewViewDescription());
        assertEquals(1, bean.getNewViewExports().size());
    }

    @Test
    void getAutoComplete_emptyWhenNoThesaurusSelected() {
        bean.setSelectedIdTheso(null);

        var result = bean.getAutoComplete("abc");

        assertEquals(List.of(), result);
        verify(graphConceptSearchService, never()).searchForRelation(any(), any());
    }

    @Test
    void getAutoComplete_delegatesToSearchService() {
        bean.setSelectedIdTheso("TH1");
        var suggestion = new ConceptSearchSuggestion("C1", "Concept 1", "", ConceptSearchKind.CONCEPT, false);
        when(graphConceptSearchService.searchForRelation("abc", "TH1")).thenReturn(List.of(suggestion));

        var result = bean.getAutoComplete("abc");

        assertEquals(List.of(suggestion), result);
    }

    @Test
    void onSelectThesaurus_showsInformationMessageWithTitle() {
        when(event.getSource()).thenReturn(chip);
        when(chip.getLabel()).thenReturn("TH1");
        when(thesaurusSelectionService.resolve("TH1")).thenReturn(new ThesaurusSelection("TH1", "Thesaurus 1"));

        bean.onSelectThesaurus(event);

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Thesaurus : Thesaurus 1"));
    }

    @Test
    void onSelectThesaurusConcept_showsThesaurusAndConceptMessages() {
        when(event.getSource()).thenReturn(chip);
        when(chip.getLabel()).thenReturn("TH1, C1");
        when(thesaurusSelectionService.resolve("TH1")).thenReturn(new ThesaurusSelection("TH1", "Thesaurus 1"));
        when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
        when(conceptReadService.loadSummary("TH1", "C1", "fr"))
                .thenReturn(Optional.of(new ConceptSummary(
                        "C1", "TH1", "Concept 1", "fr", "valid", null, null, null, null, null, null)));

        bean.onSelectThesaurusConcept(event);

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Thesaurus : Thesaurus 1"));
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Concept : Concept 1"));
    }

    @Test
    void onSelectThesaurusConcept_fallsBackToIdWhenConceptSummaryMissing() {
        when(event.getSource()).thenReturn(chip);
        when(chip.getLabel()).thenReturn("TH1, C1");
        when(thesaurusSelectionService.resolve("TH1")).thenReturn(new ThesaurusSelection("TH1", "Thesaurus 1"));
        when(thesaurusWorkLanguageService.resolveForThesaurus("TH1")).thenReturn("fr");
        when(conceptReadService.loadSummary("TH1", "C1", "fr")).thenReturn(Optional.empty());

        bean.onSelectThesaurusConcept(event);

        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Concept : C1"));
    }

    @Test
    void exportToNeo4J_delegatesThenRefreshes() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost:8080/opentheso");
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());

        bean.exportToNeo4J("3");

        verify(graphNeo4jExportService).exportView("3", "http://localhost:8080/opentheso");
        verify(graphViewReadService).reloadViewsForUser(7);
    }

    @Test
    void removeView_deletesThenRefreshesAndShowsMessage() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());

        bean.removeView("3");

        verify(graphViewCommandService).deleteView("3");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Vue supprimée avec succès"));
        verify(graphViewReadService).reloadViewsForUser(7);
    }

    @Test
    void addDataToNewViewList_doesNothingWhenNoViewSelected() {
        bean.setSelectedViewId(-1);
        bean.setSelectedIdTheso("TH1");

        bean.addDataToNewViewList();

        verify(graphViewCommandService, never()).addExportEntry(anyInt(), anyString(), anyString());
    }

    @Test
    void addDataToNewViewList_doesNothingWhenThesaurusBlank() {
        bean.setSelectedViewId(3);
        bean.setSelectedIdTheso("");

        bean.addDataToNewViewList();

        verify(graphViewCommandService, never()).addExportEntry(3, "", null);
    }

    @Test
    void addDataToNewViewList_addsEntryAndReloadsEditDialog() {
        bean.setSelectedViewId(3);
        bean.setSelectedIdTheso("TH1");
        when(graphViewCommandService.addExportEntry(3, "TH1", null)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());
        var summary = new GraphViewSummary(3, "Vue", "desc");
        summary.setExports(List.of(new GraphExportEntry("TH1", null)));
        when(graphViewReadService.loadView("3")).thenReturn(summary);

        bean.addDataToNewViewList();

        assertEquals(1, bean.getNewViewExports().size());
        assertEquals("TH1", bean.getNewViewExports().get(0).thesaurusId());
    }

    @Test
    void addDataToNewViewList_showsWarningWhenDuplicate() {
        bean.setSelectedViewId(3);
        bean.setSelectedIdTheso("TH1");
        when(graphViewCommandService.addExportEntry(3, "TH1", null)).thenReturn(false);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());

        bean.addDataToNewViewList();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Cette combinaison existe déjà !"));
        assertEquals(0, bean.getNewViewExports().size());
    }

    @Test
    void applyView_showsErrorWhenNameOrDescriptionBlank() {
        bean.setNewViewName("");
        bean.setNewViewDescription("desc");

        bean.applyView();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Une vue doit possèder un nom et une description"));
        verify(graphViewCommandService, never()).createView(any(), any(), anyInt());
    }

    @Test
    void applyView_createsNewViewWhenNoneSelected() {
        bean.setSelectedViewId(-1);
        bean.setNewViewName("Vue");
        bean.setNewViewDescription("desc");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(graphViewCommandService.createView("Vue", "desc", 7)).thenReturn(42);
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());

        bean.applyView();

        assertEquals(42, bean.getSelectedViewId());
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Vue créée avec succès"));
    }

    @Test
    void applyView_updatesExistingView() {
        bean.setSelectedViewId(5);
        bean.setNewViewName("Vue");
        bean.setNewViewDescription("desc");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());

        bean.applyView();

        verify(graphViewCommandService).updateView(5, "Vue", "desc");
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Vue modifiée avec succès"));
    }

    @Test
    void applyView_doesNothingWhenNoCurrentUser() {
        bean.setNewViewName("Vue");
        bean.setNewViewDescription("desc");
        when(userSession.getCurrentUserId()).thenReturn(null);

        bean.applyView();

        verify(graphViewCommandService, never()).createView(any(), any(), anyInt());
        verify(graphViewCommandService, never()).updateView(anyInt(), any(), any());
    }

    @Test
    void removeExportedDataRow_removesMatchingThesaurusOnlyEntry() {
        bean.setSelectedViewId(3);
        bean.setNewViewExports(new java.util.ArrayList<>(List.of(new GraphExportEntry("TH1", null))));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());
        when(graphViewReadService.loadView("3")).thenReturn(new GraphViewSummary(3, "Vue", "desc"));

        bean.removeExportedDataRow("TH1", null);

        verify(graphViewCommandService).removeExportEntry(3, "TH1", null);
        assertEquals(0, bean.getNewViewExports().size());
    }

    @Test
    void removeExportedDataRow_doesNothingWhenNoMatch() {
        bean.setSelectedViewId(-1);
        bean.setNewViewExports(new java.util.ArrayList<>(List.of(new GraphExportEntry("TH1", null))));
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(graphViewReadService.reloadViewsForUser(7)).thenReturn(List.of());

        bean.removeExportedDataRow("TH2", null);

        verify(graphViewCommandService, never()).removeExportEntry(anyInt(), anyString(), any());
        assertEquals(1, bean.getNewViewExports().size());
    }

    @Test
    void generateGraphVisualizationUrl_usesFirstExportThesaurusLanguage() throws Exception {
        var summary = new GraphViewSummary(3, "Vue", "desc");
        summary.setExports(List.of(new GraphExportEntry("TH1", null)));
        when(graphViewReadService.loadView("3")).thenReturn(summary);
        when(graphVisualizationUrlService.resolveWorkLanguageForThesaurus("TH1")).thenReturn("fr");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost:8080/opentheso");
        when(graphVisualizationUrlService.buildVisualizationUrl("3", "http://localhost:8080/opentheso", "fr"))
                .thenReturn("http://localhost:8080/opentheso/v2/graph/visualize/force.xhtml?dataUrl=...");

        var url = bean.generateGraphVisualizationUrl("3");

        assertEquals("http://localhost:8080/opentheso/v2/graph/visualize/force.xhtml?dataUrl=...", url);
    }

    @Test
    void generateGraphVisualizationUrl_usesNullLanguageWhenViewHasNoExports() throws Exception {
        var summary = new GraphViewSummary(3, "Vue", "desc");
        when(graphViewReadService.loadView("3")).thenReturn(summary);
        when(graphVisualizationUrlService.resolveWorkLanguageForThesaurus(null)).thenReturn("fr");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost:8080/opentheso");

        bean.generateGraphVisualizationUrl("3");

        verify(graphVisualizationUrlService).resolveWorkLanguageForThesaurus(null);
    }
}
