package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.v2.concept.model.CorpusSearchContext;
import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptCorpusLinkItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.collection.read.CollectionReadService;
import fr.cnrs.opentheso.v2.facet.read.FacetReadService;
import fr.cnrs.opentheso.v2.concept.model.GroupDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.LeftTreeMode;
import fr.cnrs.opentheso.v2.concept.model.RightPanelMode;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusHomeOverview;
import fr.cnrs.opentheso.v2.concept.service.ConceptTypeReadService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.shared.session.ConceptTreeRefreshState;
import fr.cnrs.opentheso.v2.concept.service.ConceptFullReadService;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeReadService;
import fr.cnrs.opentheso.v2.concept.alignment.ui.ConceptAlignmentAdminBean;
import fr.cnrs.opentheso.v2.proposition.ui.PropositionBean;
import fr.cnrs.opentheso.v2.proposition.ui.PropositionSubmitBean;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.component.UIComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusBrowseBeanTest {

    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private CollectionReadService collectionReadService;
    @Mock
    private FacetReadService facetReadService;
    @Mock
    private ConceptFullReadService conceptFullReadService;
    @Mock
    private ThesaurusHomeReadService thesaurusHomeReadService;
    @Mock
    private ConceptHistoryBean conceptHistoryBean;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private UserSession userSession;
    @Mock
    private ConceptTypeReadService conceptTypeReadService;
    @Mock
    private ConceptSelectionContext conceptSelectionContext;
    @Mock
    private ConceptTreeRefreshState conceptTreeRefreshState;
    @Mock
    private RightsService rightsService;
    @Mock
    private ObjectProvider<ConceptAlignmentAdminBean> conceptAlignmentAdminBean;
    @Mock
    private ObjectProvider<PropositionSubmitBean> propositionSubmitBean;
    @Mock
    private ObjectProvider<PropositionBean> propositionBean;
    @Mock
    private PropositionBean propositionBeanInstance;
    @Mock
    private ObjectProvider<ThesaurusHomeEditorBean> thesaurusHomeEditorBean;

    private ThesaurusBrowseBean bean;

    @BeforeEach
    void setUp() {
        lenient().when(propositionBean.getObject()).thenReturn(propositionBeanInstance);
        bean = new ThesaurusBrowseBean(
                thesaurusContext,
                conceptSelectionContext,
                conceptReadService,
                collectionReadService,
                facetReadService,
                conceptFullReadService,
                thesaurusHomeReadService,
                conceptHistoryBean,
                thesaurusPreferenceService,
                userSession,
                conceptTypeReadService,
                conceptTreeRefreshState,
                rightsService,
                conceptAlignmentAdminBean,
                propositionSubmitBean,
                propositionBean,
                thesaurusHomeEditorBean
        );
    }

    @Test
    void load_buildsConceptTreeWhenThesaurusSelected() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of(
                new ConceptTreeNodeData("C1", "Concept 1", "C1", "concept", false)
        ));
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(true));

        bean.load();

        assertTrue(bean.isScreenAvailable());
        assertNotNull(bean.getConceptRoot());
        assertEquals(1, bean.getConceptRoot().getChildCount());
        assertNotNull(bean.getThesaurusHome());
        assertTrue(bean.isHomePanel());
        assertNull(bean.getArbreRoot());
    }

    @Test
    void load_skipsArbreTreeWhenUseConceptTreeDisabled() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(false));

        bean.load();

        assertNull(bean.getArbreRoot());
        verify(conceptReadService, never()).loadRootNodes("TH1", "fr", LeftTreeMode.ARBRE, false);
    }

    @Test
    void load_skipsTreeWhenNoThesaurus() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn(null);

        bean.load();

        assertFalse(bean.isScreenAvailable());
        verify(conceptReadService, never()).loadRootNodes(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void load_restoresConceptFromSessionSelectionOnRefresh() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusContext.matchesCurrentThesaurus("TH1")).thenReturn(true);
        when(conceptSelectionContext.hasSelection()).thenReturn(true);
        when(conceptSelectionContext.getThesaurusId()).thenReturn("TH1");
        when(conceptSelectionContext.getConceptId()).thenReturn("C9");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(false));
        var summary = new ConceptSummary("C9", "TH1", "Restored", "fr", "C", "", "concept", "", "", "", "");
        stubLoadDetailWithSource("TH1", "C9", "fr", minimalConceptDetail(summary));

        bean.load();

        assertTrue(bean.isConceptPanel());
        assertEquals("C9", bean.getSelectedConcept().summary().conceptId());
        verify(conceptReadService).loadDetailWithSource("TH1", "C9", "fr");
    }

    @Test
    void onNodeSelect_loadsConceptDetail() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var summary = new ConceptSummary(
                "C1", "TH1", "Label", "fr", "valid", "ark", "concept", "N1", "2024", "2025", "admin"
        );
        var detail = new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        stubLoadDetailWithSource("TH1", "C1", "fr", detail);

        var nodeData = new ConceptTreeNodeData("C1", "Label", "C1", "concept", false);
        var event = org.mockito.Mockito.mock(NodeSelectEvent.class);
        var treeNode = new org.primefaces.model.DefaultTreeNode<>("concept", nodeData, null);
        UIComponent component = org.mockito.Mockito.mock(UIComponent.class);
        when(event.getTreeNode()).thenReturn(treeNode);
        when(event.getComponent()).thenReturn(component);
        when(component.getAttributes()).thenReturn(Map.of("treeMode", LeftTreeMode.CONCEPT));

        bean.onNodeSelect(event);

        assertEquals(detail, bean.getSelectedConcept());
        assertTrue(bean.isConceptPanel());
    }

    @Test
    void onNodeSelect_loadsGroupDetailInCollectionTab() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var group = new GroupDetailOverview("G1", "Groupe", "fr", "C", "Type", "skos:Collection", 3, "", "", "", List.of(), List.of(), List.of());
        when(collectionReadService.loadDetail("TH1", "G1", "fr")).thenReturn(Optional.of(group));

        var nodeData = new ConceptTreeNodeData("G1", "Groupe", "G1", "group", true);
        var event = org.mockito.Mockito.mock(NodeSelectEvent.class);
        var treeNode = new org.primefaces.model.DefaultTreeNode<>("group", nodeData, null);
        UIComponent component = org.mockito.Mockito.mock(UIComponent.class);
        when(event.getTreeNode()).thenReturn(treeNode);
        when(event.getComponent()).thenReturn(component);
        when(component.getAttributes()).thenReturn(Map.of("treeMode", LeftTreeMode.COLLECTION));

        bean.onNodeSelect(event);

        assertNull(bean.getSelectedConcept());
        assertEquals(group, bean.getSelectedGroup());
        assertTrue(bean.isGroupPanel());
        verify(conceptReadService, never()).loadDetailWithSource(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void onNodeExpand_loadsLazyChildren() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var parentData = new ConceptTreeNodeData("C1", "Parent", "C1", "concept", true);
        var parentNode = new org.primefaces.model.DefaultTreeNode<>("concept", parentData, null);
        new org.primefaces.model.DefaultTreeNode<>("default", ConceptTreeNodeData.dummy(), parentNode);
        var childData = new ConceptTreeNodeData("C2", "Child", "C2", "file", false);
        when(conceptReadService.loadChildNodes("C1", "concept", "TH1", "fr", LeftTreeMode.CONCEPT, false))
                .thenReturn(List.of(childData));

        var event = org.mockito.Mockito.mock(NodeExpandEvent.class);
        UIComponent component = org.mockito.Mockito.mock(UIComponent.class);
        when(event.getTreeNode()).thenReturn(parentNode);
        when(event.getComponent()).thenReturn(component);
        when(component.getAttributes()).thenReturn(Map.of("treeMode", LeftTreeMode.CONCEPT));

        bean.onNodeExpand(event);

        assertEquals(1, parentNode.getChildCount());
        assertEquals("C2", ((ConceptTreeNodeData) parentNode.getChildren().get(0).getData()).nodeId());
    }

    @Test
    void focusGroup_switchesToCollectionTab() {
        bean.focusGroup("G1");

        assertEquals(LeftTreeMode.COLLECTION, bean.getActiveLeftTreeMode());
        assertEquals(2, bean.getLeftTabIndex());
        assertEquals(RightPanelMode.GROUP, bean.getRightPanelMode());
    }

    @Test
    void openThesaurusHome_resetsRightPanel() {
        bean.openThesaurusHome();

        assertTrue(bean.isHomePanel());
        assertNull(bean.getSelectedConcept());
        assertNull(bean.getSelectedGroup());
        assertNull(bean.getSelectedFacet());
    }

    @Test
    void focusFacet_switchesToConceptTab() {
        bean.setActiveLeftTreeMode(LeftTreeMode.COLLECTION);
        bean.focusFacet("F1");

        assertEquals(LeftTreeMode.CONCEPT, bean.getActiveLeftTreeMode());
        assertEquals(0, bean.getLeftTabIndex());
        assertEquals(RightPanelMode.FACET, bean.getRightPanelMode());
    }

    @Test
    void onNodeSelect_loadsFacetDetailInConceptTab() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var facet = new FacetDetailOverview("F1", "Facette", "fr", "C1", "Parent", List.of(), List.of(), List.of());
        when(facetReadService.loadDetail("TH1", "F1", "fr")).thenReturn(Optional.of(facet));

        var nodeData = new ConceptTreeNodeData("F1", "Facette", "F1", "facet", false);
        var event = org.mockito.Mockito.mock(NodeSelectEvent.class);
        var treeNode = new org.primefaces.model.DefaultTreeNode<>("facet", nodeData, null);
        UIComponent component = org.mockito.Mockito.mock(UIComponent.class);
        when(event.getTreeNode()).thenReturn(treeNode);
        when(event.getComponent()).thenReturn(component);
        when(component.getAttributes()).thenReturn(Map.of("treeMode", LeftTreeMode.CONCEPT));

        bean.onNodeSelect(event);

        assertNull(bean.getSelectedConcept());
        assertEquals(facet, bean.getSelectedFacet());
        assertTrue(bean.isFacetPanel());
    }

    @Test
    void onIndexChange_searchesWithPermutedAndAltLabelOptions() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        bean.setIndexQuery("chat");
        bean.setIndexPermuted(true);
        bean.setIndexWithAltLabel(true);
        when(conceptReadService.searchIndex("TH1", "fr", "chat", true, true, 100))
                .thenReturn(List.of(new ConceptTreeNodeData("C1", "Chat", "C1", "concept", false)));

        bean.onIndexChange();

        assertEquals(1, bean.getIndexResults().size());
        verify(conceptReadService).searchIndex("TH1", "fr", "chat", true, true, 100);
    }

    @Test
    void openConceptHistory_loadsHistoryWhenPreferredTermAvailable() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var summary = new ConceptSummary(
                "C1", "TH1", "Label", "fr", "valid", "ark", "concept", "N1", "2024", "2025", "admin"
        );
        var detail = new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                "",
                "T1"
        );
        stubLoadDetailWithSource("TH1", "C1", "fr", detail);

        bean.openConcept("C1");
        bean.openConceptHistory();

        verify(conceptHistoryBean).load("TH1", "C1", "T1");
    }

    @Test
    void getPageTitle_returnsSelectedConceptLabel() {
        var summary = new ConceptSummary(
                "C1", "TH1", "Mon concept", "fr", "valid", "ark", "concept", "N1", "2024", "2025", "admin"
        );
        var detail = new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        bean.setSelectedConcept(detail);

        assertEquals("Mon concept", bean.getPageTitle());
    }

    @Test
    void load_opensFacetFromUriParameter() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusContext.getIdFacetFromUri()).thenReturn("F1");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(true));
        var facet = new FacetDetailOverview("F1", "Facette", "fr", "C1", "Parent", List.of(), List.of(), List.of());
        when(facetReadService.loadDetail("TH1", "F1", "fr")).thenReturn(Optional.of(facet));

        bean.load();

        assertEquals(facet, bean.getSelectedFacet());
        assertTrue(bean.isFacetPanel());
        assertEquals(LeftTreeMode.CONCEPT, bean.getActiveLeftTreeMode());
        assertEquals(0, bean.getLeftTabIndex());
    }

    @Test
    void invalidateConceptTree_clearsArbreRoot() {
        bean.setArbreRoot(new org.primefaces.model.DefaultTreeNode<>("root", null, null));

        bean.invalidateConceptTree();

        assertNull(bean.getConceptRoot());
        assertNull(bean.getArbreRoot());
    }

    @Test
    void invalidateCollectionTree_clearsArbreRoot() {
        bean.setArbreRoot(new org.primefaces.model.DefaultTreeNode<>("root", null, null));

        bean.invalidateCollectionTree();

        assertNull(bean.getCollectionRoot());
        assertNull(bean.getArbreRoot());
    }

    @Test
    void load_opensGroupFromUriParameter() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusContext.getIdGroupFromUri()).thenReturn("G1");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(true));
        var group = new GroupDetailOverview("G1", "Collection", "fr", "", "", "", 0, "", "", "", List.of(), List.of(), List.of());
        when(collectionReadService.loadDetail("TH1", "G1", "fr")).thenReturn(Optional.of(group));

        bean.load();

        assertEquals(group, bean.getSelectedGroup());
        assertTrue(bean.isGroupPanel());
        assertEquals(LeftTreeMode.COLLECTION, bean.getActiveLeftTreeMode());
    }

    @Test
    void load_prefersConceptUriOverGroupUri() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(true));
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        stubLoadDetailWithSource("TH1", "C1", "fr", new ConceptDetail(
                        summary,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
        when(thesaurusContext.getIdConceptFromUri()).thenReturn("C1");
        when(thesaurusContext.getIdGroupFromUri()).thenReturn("G1");

        bean.load();

        assertTrue(bean.isConceptPanel());
        assertNull(bean.getSelectedGroup());
    }

    @Test
    void consultationPreferences_hideRestrictedNotesForAnonymousUsers() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(true, true, true, false, false, false));
        when(userSession.isLoggedIn()).thenReturn(false);

        bean.load();

        assertFalse(bean.isEditorialNoteVisible());
        assertFalse(bean.isHistoryNoteVisible());
        assertFalse(bean.isCustomRelationVisible());
    }

    @Test
    void consultationPreferences_showRestrictedNotesForLoggedInUsers() {
        when(userSession.isLoggedIn()).thenReturn(true);
        bean.setShowEditorialNote(false);
        bean.setShowHistoryNote(false);

        assertTrue(bean.isEditorialNoteVisible());
        assertTrue(bean.isHistoryNoteVisible());
    }

    @Test
    void getConceptTypeLabel_resolvesSpecificConceptType() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "place", "", "", "", "");
        bean.setSelectedConcept(new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        when(conceptTypeReadService.resolveLabel("place", "TH1", "fr")).thenReturn("Lieu");

        assertEquals("Lieu", bean.getConceptTypeLabel());
        assertTrue(bean.isConceptTypeBadgeVisible());
    }

    @Test
    void load_opensConceptFromUriParameter() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(true));
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        stubLoadDetailWithSource("TH1", "C1", "fr", new ConceptDetail(
                        summary,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
        when(thesaurusContext.getIdConceptFromUri()).thenReturn("C1");

        bean.load();

        assertEquals("C1", bean.getSelectedConcept().summary().conceptId());
        assertTrue(bean.isConceptPanel());
    }

    @Test
    void load_doesNotRequireLegacySync() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenReturn(consultationPreferences(true));

        bean.load();

        assertTrue(bean.isHomePanel());
    }

    @Test
    void onRightTabChange_setsAlignmentTabIndex() {
        ReflectionTestUtils.setField(bean, "rightPanelMode", RightPanelMode.CONCEPT);
        when(userSession.getCurrentUserId()).thenReturn(1);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(rightsService.canOnThesaurus(1, fr.cnrs.opentheso.v2.rights.Permission.MANAGE_THESAURUS, "TH1"))
                .thenReturn(true);
        var adminBean = org.mockito.Mockito.mock(ConceptAlignmentAdminBean.class);
        when(conceptAlignmentAdminBean.getObject()).thenReturn(adminBean);

        var event = org.mockito.Mockito.mock(org.primefaces.event.TabChangeEvent.class);
        var tab = org.mockito.Mockito.mock(org.primefaces.component.tabview.Tab.class);
        when(event.getTab()).thenReturn(tab);
        when(tab.getId()).thenReturn("viewTabAlignement");

        bean.onRightTabChange(event);

        // Concept(0), Collection(1), Alignement(2)
        assertEquals(2, bean.getRightTabIndex());
        verify(adminBean).openForCurrentConcept();
    }

    @Test
    void openGroup_activatesCollectionTab() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var group = new GroupDetailOverview("G1", "Groupe", "fr", "", "", "", 0, "", "", "", List.of(), List.of(), List.of());
        when(collectionReadService.loadDetail("TH1", "G1", "fr")).thenReturn(Optional.of(group));

        bean.openGroup("G1");

        assertTrue(bean.isGroupPanel());
        assertTrue(bean.isValueSelected());
        assertEquals(1, bean.getRightTabIndex());
    }

    @Test
    void onLeftTabChange_switchesToIndexMode() {
        var event = org.mockito.Mockito.mock(org.primefaces.event.TabChangeEvent.class);
        var tab = org.mockito.Mockito.mock(org.primefaces.component.tabview.Tab.class);
        when(event.getTab()).thenReturn(tab);
        when(tab.getId()).thenReturn("viewTabList");

        bean.onLeftTabChange(event);

        assertEquals(LeftTreeMode.INDEX, bean.getActiveLeftTreeMode());
        assertEquals(1, bean.getLeftTabIndex());
    }

    @Test
    void onNodeSelect_onRootOpensHome() {
        var nodeData = new ConceptTreeNodeData("root", "Root", "", "root", true);
        var event = org.mockito.Mockito.mock(NodeSelectEvent.class);
        var treeNode = new org.primefaces.model.DefaultTreeNode<>("root", nodeData, null);
        UIComponent component = org.mockito.Mockito.mock(UIComponent.class);
        when(event.getTreeNode()).thenReturn(treeNode);
        when(event.getComponent()).thenReturn(component);
        when(component.getAttributes()).thenReturn(Map.of("treeMode", LeftTreeMode.CONCEPT));

        bean.onNodeSelect(event);

        assertTrue(bean.isHomePanel());
    }

    @Test
    void onIndexSelect_opensConcept() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        stubLoadDetailWithSource("TH1", "C1", "fr", new ConceptDetail(
                        summary,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));
        var selected = new ConceptTreeNodeData("C1", "Label", "C1", "concept", false);
        var event = org.mockito.Mockito.mock(org.primefaces.event.SelectEvent.class);
        when(event.getObject()).thenReturn(selected);

        bean.onIndexSelect(event);

        assertEquals("C1", bean.getIndexSelected().nodeId());
        assertTrue(bean.isConceptPanel());
    }

    @Test
    void load_usesDefaultPreferencesWhenServiceFails() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr"))
                .thenThrow(new RuntimeException("db down"));

        bean.load();

        assertTrue(bean.isUseConceptTree());
        assertTrue(bean.isBreadcrumbEnabled());
    }

    @Test
    void getPageTitle_returnsGroupLabel() {
        bean.setSelectedGroup(new GroupDetailOverview("G1", "Collection", "fr", "", "", "", 0, "", "", "", List.of(), List.of(), List.of()));

        assertEquals("Collection", bean.getPageTitle());
    }

    @Test
    void openConcept_blankConceptOpensHome() {
        bean.openConcept(" ");

        assertTrue(bean.isHomePanel());
    }

    @Test
    void focusGroup_loadsGroupDetail() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var group = new GroupDetailOverview("G1", "Groupe", "fr", "", "", "", 0, "", "", "", List.of(), List.of(), List.of());
        when(collectionReadService.loadDetail("TH1", "G1", "fr")).thenReturn(Optional.of(group));

        bean.focusGroup("G1");

        assertEquals(group, bean.getSelectedGroup());
    }

    @Test
    void onIndexChange_clearsResultsWhenQueryBlank() {
        bean.setIndexQuery(" ");

        bean.onIndexChange();

        assertTrue(bean.getIndexResults().isEmpty());
    }

    @Test
    void onNodeSelect_loadsGroupDetailInArbreTab() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var group = new GroupDetailOverview("G1", "Groupe", "fr", "C", "Type", "skos:Collection", 3, "", "", "", List.of(), List.of(), List.of());
        when(collectionReadService.loadDetail("TH1", "G1", "fr")).thenReturn(Optional.of(group));

        var nodeData = new ConceptTreeNodeData("G1", "Groupe", "G1", "group", true);
        var event = org.mockito.Mockito.mock(NodeSelectEvent.class);
        var treeNode = new org.primefaces.model.DefaultTreeNode<>("group", nodeData, null);
        UIComponent component = org.mockito.Mockito.mock(UIComponent.class);
        when(event.getTreeNode()).thenReturn(treeNode);
        when(event.getComponent()).thenReturn(component);
        when(component.getAttributes()).thenReturn(Map.of("treeMode", LeftTreeMode.ARBRE));

        bean.onNodeSelect(event);

        assertEquals(group, bean.getSelectedGroup());
        assertTrue(bean.isGroupPanel());
    }

    @Test
    void openConcept_syncsTreeSelectionAlongBreadcrumb() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of(
                new ConceptTreeNodeData("C1", "Parent", "C1", "concept", true)
        ));
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test"))
                .thenReturn(new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), ""));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(consultationPreferences(true));
        when(conceptReadService.loadChildNodes("C1", "concept", "TH1", "fr", LeftTreeMode.CONCEPT, false))
                .thenReturn(List.of(new ConceptTreeNodeData("C2", "Child", "C2", "file", false)));

        bean.load();

        // Simule une ancienne sélection encore marquée dans l'arbre
        var stale = (org.primefaces.model.TreeNode) bean.getConceptRoot().getChildren().get(0);
        stale.setSelected(true);

        var summary = new ConceptSummary("C2", "TH1", "Child", "fr", "C", "", "concept", "", "", "", "");
        var detail = new ConceptDetail(
                summary,
                List.of(new BreadcrumbStep("C1", "Parent", 0), new BreadcrumbStep("C2", "Child", 1)),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(List.of(
                        new BreadcrumbStep("C1", "Parent", 0, true),
                        new BreadcrumbStep("C2", "Child", 1)
                )),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                "",
                "T1"
        );
        stubLoadDetailWithSource("TH1", "C2", "fr", detail);

        bean.openConcept("C2");

        assertNotNull(bean.getSelectedNode());
        assertEquals("C2", ((ConceptTreeNodeData) bean.getSelectedNode().getData()).nodeId());
        assertTrue(bean.getSelectedNode().isSelected());
        assertFalse(stale.isSelected());
    }

    @Test
    void onLeftTabChange_switchesToArbreMode() {
        var event = org.mockito.Mockito.mock(org.primefaces.event.TabChangeEvent.class);
        var tab = org.mockito.Mockito.mock(org.primefaces.component.tabview.Tab.class);
        when(event.getTab()).thenReturn(tab);
        when(tab.getId()).thenReturn("viewTabConceptTree");

        bean.onLeftTabChange(event);

        assertEquals(LeftTreeMode.ARBRE, bean.getActiveLeftTreeMode());
        assertEquals(3, bean.getLeftTabIndex());
    }

    @Test
    void openConceptHistory_skipsWhenPreferredTermMissing() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        bean.setSelectedConcept(new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ));

        bean.openConceptHistory();

        verify(conceptHistoryBean, never()).load(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void countBranchConcepts_loadsBranchSize() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        bean.setSelectedConcept(new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        when(conceptReadService.countBranchConcepts("TH1", "C1")).thenReturn(7);

        bean.countBranchConcepts();

        assertEquals(7, bean.getBranchConceptCount());
    }

    @Test
    void onNoteLanguageToggle_refreshesDisplayedNotesFromFullConcept() {
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        bean.setSelectedConcept(new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setIdentifier("C1");
        ConceptSnapshotNote frenchNote = new ConceptSnapshotNote(1, "fr", "valeur", "");
        fullConcept.setNotes(List.of(frenchNote));
        ConceptSnapshotNote englishNote = new ConceptSnapshotNote(2, "en", "value", "");
        fullConcept.setDefinitions(List.of(englishNote));
        ReflectionTestUtils.setField(bean, "selectedFullConcept", fullConcept);
        bean.setShowAllNoteLanguages(true);

        bean.onNoteLanguageToggle();

        assertEquals(2, bean.getDisplayedNotes().size());
    }

    @Test
    void openConcept_usesLoadDetailWithSource() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        var detail = minimalConceptDetail(summary);
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setIdentifier("C1");
        stubLoadDetailWithSource("TH1", "C1", "fr", detail, fullConcept);

        bean.openConcept("C1", false);

        verify(conceptReadService).loadDetailWithSource("TH1", "C1", "fr");
        assertEquals(detail, bean.getSelectedConcept());
        assertFalse(bean.isCorpusSearched());
    }

    @Test
    void load_refreshesActiveCorpusState() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.getCurrentThesaurusTitle()).thenReturn("Test");
        when(conceptReadService.hasActiveCorpus("TH1")).thenReturn(true);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(consultationPreferences(true));
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        when(thesaurusHomeReadService.loadOverview("TH1", "fr", "Test")).thenReturn(
                new ThesaurusHomeOverview("Test", 0, "", "", "", "", List.of(), List.of(), "")
        );
        when(conceptReadService.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT, false)).thenReturn(List.of());

        bean.load();

        assertTrue(bean.isHaveActiveCorpus());
        verify(conceptReadService).hasActiveCorpus("TH1");
    }

    @Test
    void searchCorpusLinks_showsNoResultWhenEmpty() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setIdentifier("C1");
        stubLoadDetailWithSource("TH1", "C1", "fr", minimalConceptDetail(summary), fullConcept);
        var context = new CorpusSearchContext("C1", "", "Label");
        when(conceptReadService.toCorpusSearchContext(summary)).thenReturn(context);
        when(conceptReadService.loadCorpusLinks("TH1", context)).thenReturn(Collections.emptyList());

        bean.openConcept("C1", false);
        bean.searchCorpusLinks();

        assertTrue(bean.isCorpusSearched());
        assertTrue(bean.isCorpusNoResult());
        assertFalse(bean.hasDisplayedCorpusLinks());
    }

    @Test
    void searchCorpusLinks_loadsLinksOnDemand() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setIdentifier("C1");
        stubLoadDetailWithSource("TH1", "C1", "fr", minimalConceptDetail(summary), fullConcept);
        var context = new CorpusSearchContext("C1", "", "Label");
        when(conceptReadService.toCorpusSearchContext(summary)).thenReturn(context);
        when(conceptReadService.loadCorpusLinks("TH1", context)).thenReturn(List.of(
                new ConceptCorpusLinkItem("Gallica", "http://gallica.bnf.fr/C1", 0, true, true)
        ));

        bean.openConcept("C1", false);
        bean.searchCorpusLinks();

        verify(conceptReadService).loadCorpusLinks("TH1", context);
        assertEquals(1, bean.getDisplayedCorpusLinks().size());
        assertTrue(bean.hasDisplayedCorpusLinks());
        assertTrue(bean.isCorpusSearched());
        assertFalse(bean.isCorpusNoResult());
    }

    @Test
    void searchCorpusLinks_marksCorpusSearchedWhenConceptMissing() {
        bean.setSelectedConcept(null);

        bean.searchCorpusLinks();

        assertTrue(bean.getDisplayedCorpusLinks().isEmpty());
        assertTrue(bean.isCorpusSearched());
        verify(conceptReadService, never()).loadCorpusLinks(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getBranchGraphConceptId_usesSelectedConceptWhenTreeNodeMissing() {
        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "C", "", "concept", "", "", "", "");
        bean.setSelectedConcept(new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        ));
        when(conceptSelectionContext.isHasNarrowers()).thenReturn(true);

        assertEquals("C1", bean.getBranchGraphConceptId());
        assertTrue(bean.isBranchGraphEnabled());
    }

    private ThesaurusPreferences consultationPreferences(boolean useConceptTree) {
        return consultationPreferences(useConceptTree, true, true, true, true, true);
    }

    private ThesaurusPreferences consultationPreferences(
            boolean useConceptTree,
            boolean breadcrumb,
            boolean displayUserName,
            boolean useCustomRelation,
            boolean showHistoryNote,
            boolean showEditorialNote
    ) {
        var base = SettingTestFixtures.samplePreferences();
        return new ThesaurusPreferences(
                base.thesaurusId(),
                base.sourceLang(),
                base.identifierType(),
                base.cheminSite(),
                base.idNaan(),
                base.preferredName(),
                base.originalUri(),
                base.exportUriType(),
                base.identifierServerType(),
                base.useHandle(),
                base.userHandle(),
                base.passHandle(),
                base.pathKeyHandle(),
                base.pathCertHandle(),
                base.urlApiHandle(),
                base.prefixIdHandle(),
                base.privatePrefixHandle(),
                base.uriArk(),
                base.useArk(),
                base.serverArk(),
                base.prefixArk(),
                base.userArk(),
                base.passArk(),
                base.generateHandle(),
                base.autoExpandTree(),
                base.sortByNotation(),
                base.treeCache(),
                base.useArkLocal(),
                base.naanArkLocal(),
                base.prefixArkLocal(),
                base.sizeIdArkLocal(),
                breadcrumb,
                useConceptTree,
                displayUserName,
                base.suggestion(),
                useCustomRelation,
                base.uppercaseForArk(),
                showHistoryNote,
                showEditorialNote,
                base.useHandleWithCertificat(),
                base.adminHandle(),
                base.indexHandle(),
                base.useDeeplTranslation(),
                base.deeplApiKey(),
                base.webservices(),
                base.kohaLink(),
                base.useOpenArk(),
                base.serverOpenArk(),
                base.naanOpenArk(),
                base.prefixOpenArk(),
                base.apiKeyOpenArk(),
                base.languages()
        );
    }

    private ConceptDetail minimalConceptDetail(ConceptSummary summary) {
        return new ConceptDetail(
                summary,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private void stubLoadDetailWithSource(String thesaurusId, String conceptId, String lang, ConceptDetail detail) {
        stubLoadDetailWithSource(thesaurusId, conceptId, lang, detail, new ConceptFullSnapshot());
    }

    private void stubLoadDetailWithSource(
            String thesaurusId,
            String conceptId,
            String lang,
            ConceptDetail detail,
            ConceptFullSnapshot fullConcept
    ) {
        when(conceptReadService.loadDetailWithSource(thesaurusId, conceptId, lang))
                .thenReturn(Optional.of(new ConceptReadService.ConceptDetailLoadResult(detail, fullConcept)));
    }
}
