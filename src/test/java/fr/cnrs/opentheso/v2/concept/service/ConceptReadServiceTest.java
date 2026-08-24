package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptHeaderRow;
import fr.cnrs.opentheso.v2.concept.model.ConceptHierarchicalRelation;
import fr.cnrs.opentheso.v2.concept.model.ConceptResourceStatus;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptTreeRow;
import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.LeftTreeMode;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptReadServiceTest {

    @Mock
    private ConceptQueryRepository conceptQueryRepository;
    @Mock
    private ConceptDetailEnrichmentService conceptDetailEnrichmentService;
    @Mock
    private ConceptTreeConsultationService conceptTreeConsultationService;
    @Mock
    private ConceptBreadcrumbReadService conceptBreadcrumbReadService;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private ApplicationUriService applicationUriService;
    @Mock
    private AuthenticatedUserSource authenticatedUserSource;

    private ConceptReadService service;

    @BeforeEach
    void setUp() {
        service = new ConceptReadService(
                conceptQueryRepository,
                conceptDetailEnrichmentService,
                conceptTreeConsultationService,
                conceptBreadcrumbReadService,
                thesaurusPreferenceService,
                applicationUriService,
                authenticatedUserSource
        );
        lenient().when(conceptTreeConsultationService.sortNodes(any(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void loadTreeRootNodes_keepsCandidates() {
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(conceptQueryRepository.findTreeRootConcepts("TH1", "fr")).thenReturn(List.of(
                new ConceptTreeRow("C1", "", "Lieux", "C", true),
                new ConceptTreeRow("CA1", "", "Tjarou", "CA", false),
                new ConceptTreeRow("R1", "", "Rejeté", "REJ", false)
        ));

        var nodes = service.loadTreeRootNodes("TH1", "fr");

        assertEquals(3, nodes.size());
        assertEquals("concept", nodes.get(0).nodeType());
        assertEquals("candidat", nodes.get(1).nodeType());
        assertEquals("rejete", nodes.get(2).nodeType());
        assertEquals("Tjarou", nodes.get(1).label());
        verify(conceptTreeConsultationService).sortNodes(any(), eq(false));
    }

    @Test
    void loadTreeRootNodes_usesNotationSortWhenPreferenceIsOn() {
        var preferences = mock(fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences.class);
        when(preferences.sortByNotation()).thenReturn(true);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(preferences);
        when(conceptQueryRepository.findTreeRootConcepts("TH1", "fr")).thenReturn(List.of(
                new ConceptTreeRow("C1", "02", "Zèbre", "C", false),
                new ConceptTreeRow("C2", "01", "Abeille", "C", false)
        ));

        service.loadTreeRootNodes("TH1", "fr");

        verify(conceptTreeConsultationService).sortNodes(any(), eq(true));
    }

    @Test
    void loadDetail_canIncludeCandidates() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(conceptDetailEnrichmentService.loadFullConcept("TH1", "CA1", "fr", false, true))
                .thenReturn(Optional.empty());

        service.loadDetail("TH1", "CA1", "fr", true);

        verify(conceptDetailEnrichmentService).loadFullConcept("TH1", "CA1", "fr", false, true);
    }

    @Test
    void countSubtreeConcepts_addsTheNodeToItsDescendants() {
        when(conceptQueryRepository.countDescendantConcepts("TH1", "C1")).thenReturn(4);

        assertEquals(5, service.countSubtreeConcepts("TH1", "C1", "concept"));
        verify(conceptQueryRepository).countDescendantConcepts("TH1", "C1");
    }

    @Test
    void countSubtreeConcepts_usesGroupMembershipForCollections() {
        when(conceptQueryRepository.countConceptsInGroup("TH1", "G1")).thenReturn(12);

        assertEquals(12, service.countSubtreeConcepts("TH1", "G1", "group"));
        verify(conceptQueryRepository, never()).countDescendantConcepts("TH1", "G1");
    }

    @Test
    void loadRootNodes_conceptTab_returnsTopConcepts() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(conceptTreeConsultationService.loadTopConcepts("TH1", "fr", false, false)).thenReturn(List.of(
                new fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData("C1", "Top", "N1", "concept", true)
        ));

        var nodes = service.loadRootNodes("TH1", "fr", LeftTreeMode.CONCEPT);

        assertEquals(1, nodes.size());
        assertEquals("concept", nodes.get(0).nodeType());
        verify(conceptTreeConsultationService).loadTopConcepts("TH1", "fr", false, false);
    }

    @Test
    void loadRootNodes_collectionTab_returnsGroups() {
        when(conceptQueryRepository.findConceptGroups("TH1", "fr")).thenReturn(
                Collections.singletonList(new Object[]{"G1", "TH1", "Groupe", "N1", true})
        );

        var nodes = service.loadRootNodes("TH1", "fr", LeftTreeMode.COLLECTION);

        assertEquals(1, nodes.size());
        assertEquals("group", nodes.get(0).nodeType());
    }

    @Test
    void loadChildNodes_collectionTab_loadsSubGroupsAndAllConcepts() {
        when(conceptQueryRepository.findChildConceptGroups("G1", "TH1", "fr")).thenReturn(
                Collections.singletonList(new Object[]{"SG1", "TH1", "Sous-groupe", "N2", false})
        );
        when(conceptQueryRepository.findConceptsOfGroup("G1", "TH1", "fr")).thenReturn(
                Collections.singletonList(new Object[]{"C1", "TH1", "Child", "", "C", false})
        );

        var nodes = service.loadChildNodes("G1", "group", "TH1", "fr", LeftTreeMode.COLLECTION);

        assertEquals(2, nodes.size());
        verify(conceptQueryRepository).findConceptsOfGroup("G1", "TH1", "fr");
    }

    @Test
    void loadChildNodes_arbreTab_loadsSubGroupsAndTopConcepts() {
        when(conceptQueryRepository.findChildConceptGroups("G1", "TH1", "fr")).thenReturn(List.of());
        when(conceptQueryRepository.findTopConceptsOfGroup("G1", "TH1", "fr")).thenReturn(
                List.of(new ConceptTreeRow("C1", "", "Child", "C", false))
        );

        var nodes = service.loadChildNodes("G1", "group", "TH1", "fr", LeftTreeMode.ARBRE);

        assertEquals(1, nodes.size());
        verify(conceptQueryRepository).findTopConceptsOfGroup("G1", "TH1", "fr");
    }

    @Test
    void loadChildNodes_loadsNarrowerConcepts() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(conceptTreeConsultationService.loadConceptTreeChildren("TH1", "C1", "concept", "fr", false, false))
                .thenReturn(List.of(new fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData(
                        "C2", "Child", "N2", "concept", true
                )));

        var nodes = service.loadChildNodes("C1", "concept", "TH1", "fr", LeftTreeMode.CONCEPT);

        assertEquals(1, nodes.size());
        assertEquals("C2", nodes.get(0).nodeId());
        verify(conceptTreeConsultationService).loadConceptTreeChildren("TH1", "C1", "concept", "fr", false, false);
    }

    @Test
    void loadChildNodes_arbreTab_loadsTreeChildren() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(conceptTreeConsultationService.loadConceptTreeChildren("TH1", "C1", "concept", "fr", false, true))
                .thenReturn(List.of());

        var nodes = service.loadChildNodes("C1", "concept", "TH1", "fr", LeftTreeMode.ARBRE);

        assertTrue(nodes.isEmpty());
        verify(conceptTreeConsultationService).loadConceptTreeChildren("TH1", "C1", "concept", "fr", false, true);
    }

    @Test
    void loadSummary_returnsMappedHeader() {
        when(conceptQueryRepository.findConceptHeader("C1", "TH1", "fr"))
                .thenReturn(Optional.of(new ConceptHeaderRow(
                        "C1", "TH1", "Label", "fr", "C", "", "concept", "", "01/01/2024", "02/01/2024", ""
                )));

        var summary = service.loadSummary("TH1", "C1", "fr");

        assertTrue(summary.isPresent());
        assertEquals("Label", summary.get().preferredLabel());
    }

    @Test
    void loadDetail_aggregatesStoredProcedureData() {
        ConceptFullSnapshot fullConcept = buildFullConcept();
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(conceptDetailEnrichmentService.loadFullConcept("TH1", "C1", "fr", false, false))
                .thenReturn(Optional.of(fullConcept));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(conceptDetailEnrichmentService.mapExportIds(fullConcept, null))
                .thenReturn(new ConceptDetailEnrichmentService.ConceptExportIds("", "", ""));
        when(conceptDetailEnrichmentService.mapImages(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapGpsPoints(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapExternalResources(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapContributors(fullConcept)).thenReturn("");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");

        var detail = service.loadDetail("TH1", "C1", "fr");

        assertTrue(detail.isPresent());
        assertEquals(1, detail.get().broaderTerms().size());
        assertEquals("Parent", detail.get().broaderTerms().get(0).label());
        verify(conceptDetailEnrichmentService).loadFullConcept("TH1", "C1", "fr", false, false);
    }

    @Test
    void loadGroupDetail_includesMembersFromRepository() {
        when(conceptQueryRepository.findGroupHeader("G1", "TH1", "fr")).thenReturn(Optional.of(
                new Object[]{"G1", "Groupe", "N1", "ark:/123", "hdl", "MT"}
        ));
        when(conceptQueryRepository.findGroupType("MT")).thenReturn(Optional.of(new Object[]{"Type", "skos:Collection"}));
        when(conceptQueryRepository.countConceptsInGroup("TH1", "G1")).thenReturn(1);
        when(conceptQueryRepository.findGroupTranslations("G1", "TH1", "fr")).thenReturn(List.of());
        when(conceptQueryRepository.findNotesByIdentifier("G1", "TH1", "fr")).thenReturn(List.of());
        when(conceptQueryRepository.findConceptsOfGroup("G1", "TH1", "fr")).thenReturn(
                Collections.singletonList(new Object[]{"C1", "TH1", "Member", "", "C", false})
        );

        var detail = service.loadGroupDetail("TH1", "G1", "fr");

        assertTrue(detail.isPresent());
        assertEquals(1, detail.get().members().size());
        assertEquals("C1", detail.get().members().get(0).conceptId());
        assertEquals("Member", detail.get().members().get(0).label());
        assertEquals("Type", detail.get().typeLabel());
    }

    @Test
    void loadFacetDetail_loadsFromRepository() {
        when(conceptQueryRepository.findFacetHeader("F1", "TH1", "fr")).thenReturn(Optional.of(
                new Object[]{"F1", "C1", "Facette"}
        ));
        when(conceptQueryRepository.findConceptHeader("C1", "TH1", "fr")).thenReturn(Optional.of(
                new ConceptHeaderRow("C1", "TH1", "Parent", "fr", "C", "", "concept", "", "01/01/2024", "02/01/2024", "")
        ));
        when(conceptQueryRepository.findFacetMembers("F1", "TH1", "fr")).thenReturn(List.of());
        when(conceptQueryRepository.findFacetTranslations("F1", "TH1", "fr")).thenReturn(List.of());
        when(conceptQueryRepository.findNotesByIdentifier("F1", "TH1", "fr")).thenReturn(List.of());

        var detail = service.loadFacetDetail("TH1", "F1", "fr");

        assertTrue(detail.isPresent());
        assertEquals("Facette", detail.get().label());
        assertEquals("Parent", detail.get().parentConceptLabel());
    }

    @Test
    void searchIndex_appliesPermutedAndAltLabelOptions() {
        when(conceptQueryRepository.searchIndexSynonymsPermuted("TH1", "fr", "alpha")).thenReturn(
                List.<Object[]>of(new Object[]{"C1", "Alpha"})
        );

        var nodes = service.searchIndex("TH1", "fr", "alpha", true, true, 100);

        assertEquals(1, nodes.size());
        verify(conceptQueryRepository).searchIndexSynonymsPermuted("TH1", "fr", "alpha");
    }

    @Test
    void searchIndex_limitsResults() {
        when(conceptQueryRepository.searchIndexPreferredPrefix("TH1", "fr", "a")).thenReturn(List.<Object[]>of(
                new Object[]{"C1", "A"},
                new Object[]{"C2", "B"},
                new Object[]{"C3", "C"}
        ));

        var nodes = service.searchIndex("TH1", "fr", "a", false, false, 2);

        assertEquals(2, nodes.size());
        assertEquals("C1", nodes.get(0).nodeId());
        assertEquals("C2", nodes.get(1).nodeId());
    }

    @Test
    void loadBreadcrumb_usesNativePathsWhenAvailable() {
        var path = List.of(new BreadcrumbStep("C1", "Root", 0, true));
        when(conceptBreadcrumbReadService.loadBreadcrumbPaths("TH1", "C1", "fr")).thenReturn(List.of(path));

        var breadcrumb = service.loadBreadcrumb("TH1", "C1", "fr");

        assertEquals(1, breadcrumb.size());
        assertEquals("Root", breadcrumb.get(0).label());
        verify(conceptQueryRepository, never()).findBreadcrumb(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void loadDetail_buildsIdentifiersWithOriginalUriAndQrCode() {
        ConceptFullSnapshot fullConcept = buildFullConcept();
        fullConcept.setPermanentId("ark:/66666/crt/123");
        fullConcept.setUri("https://ark.example.com/ark:/66666/crt/123");
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(conceptDetailEnrichmentService.loadFullConcept("TH1", "C1", "fr", true, false))
                .thenReturn(java.util.Optional.of(fullConcept));
        when(conceptDetailEnrichmentService.mapImages(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapGpsPoints(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapExternalResources(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapContributors(fullConcept)).thenReturn("");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");
        var preferences = mock(fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences.class);
        when(preferences.exportUriType()).thenReturn(ExportUriType.ARK);
        when(preferences.originalUri()).thenReturn("https://ark.example.com");
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(preferences);
        when(conceptDetailEnrichmentService.mapExportIds(fullConcept, preferences))
                .thenReturn(new ConceptDetailEnrichmentService.ConceptExportIds(
                        "ark:/66666/crt/123", "", ""
                ));

        var detail = service.loadDetail("TH1", "C1", "fr");

        assertTrue(detail.isPresent());
        assertEquals("https://ark.example.com/ark:/66666/crt/123", detail.get().identifiers().originalUri());
        assertTrue(detail.get().identifiers().showOriginalUri());
        assertTrue(detail.get().identifiers().showQrCode());
        assertEquals("https://ark.example.com/ark:/66666/crt/123", detail.get().identifiers().qrCodeValue());
    }

    @Test
    void loadBreadcrumb_fallsBackToRepositoryWhenNativeEmpty() {
        when(conceptBreadcrumbReadService.loadBreadcrumbPaths("TH1", "C1", "fr")).thenReturn(List.of());
        when(conceptQueryRepository.findBreadcrumb("C1", "TH1", "fr"))
                .thenReturn(Collections.singletonList(new Object[]{"C1", "Root", 0}));

        var breadcrumb = service.loadBreadcrumb("TH1", "C1", "fr");

        assertEquals(1, breadcrumb.size());
        verify(conceptQueryRepository).findBreadcrumb("C1", "TH1", "fr");
    }

    @Test
    void loadChildNodes_facetType_loadsFacetMembers() {
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(conceptTreeConsultationService.loadConceptTreeChildren("TH1", "F1", "facet", "fr", false, false))
                .thenReturn(List.of(new fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData(
                        "C2", "Child", "", "file", false
                )));

        var nodes = service.loadChildNodes("F1", "facet", "TH1", "fr", LeftTreeMode.CONCEPT);

        assertEquals(1, nodes.size());
        verify(conceptTreeConsultationService).loadConceptTreeChildren("TH1", "F1", "facet", "fr", false, false);
    }

    @Test
    void searchIndex_deduplicatesConceptsWithSameId() {
        // Same conceptId "C1" appears twice (e.g. matched via two different synonyms)
        when(conceptQueryRepository.searchIndexSynonymsPrefix("TH1", "fr", "foo")).thenReturn(List.<Object[]>of(
                new Object[]{"C1", "Foo"},
                new Object[]{"C1", "Foo-alt"},
                new Object[]{"C2", "Foobar"}
        ));

        var nodes = service.searchIndex("TH1", "fr", "foo", false, true, 100);

        assertEquals(2, nodes.size());
        assertEquals("C1", nodes.get(0).nodeId());
        assertEquals("C2", nodes.get(1).nodeId());
    }

    @Test
    void searchIndex_returnsEmptyWhenQueryBlank() {
        assertTrue(service.searchIndex("TH1", "fr", " ", false, false, 10).isEmpty());
    }

    @Test
    void loadFacetDetail_returnsEmptyWhenFacetMissing() {
        assertTrue(service.loadFacetDetail("TH1", "", "fr").isEmpty());
    }

    @Test
    void loadChildNodes_ignoresPreferenceLookupFailureForSortFlag() {
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(conceptTreeConsultationService.loadConceptTreeChildren("TH1", "C1", "concept", "fr", false, false))
                .thenReturn(List.of());

        var nodes = service.loadChildNodes("C1", "concept", "TH1", "fr", LeftTreeMode.CONCEPT);

        assertTrue(nodes.isEmpty());
    }

    @Test
    void loadChildNodes_sortByNotation_usesPreferenceFlag() {
        var preferences = mock(fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences.class);
        when(preferences.sortByNotation()).thenReturn(true);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(preferences);
        when(authenticatedUserSource.isLoggedIn()).thenReturn(true);
        when(conceptTreeConsultationService.loadConceptTreeChildren("TH1", "C1", "concept", "fr", true, true))
                .thenReturn(List.of(new fr.cnrs.opentheso.v2.concept.model.ConceptTreeNodeData(
                        "C2", "Child", "N2", "concept", false
                )));

        var nodes = service.loadChildNodes("C1", "concept", "TH1", "fr", LeftTreeMode.CONCEPT);

        assertEquals(1, nodes.size());
        assertEquals("N2", nodes.get(0).notation());
    }

    @Test
    void loadDetail_showsOriginalUriForHandleExportType() {
        ConceptFullSnapshot fullConcept = buildFullConcept();
        fullConcept.setPermanentId("12345/abc");
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(conceptDetailEnrichmentService.loadFullConcept("TH1", "C1", "fr", false, false))
                .thenReturn(java.util.Optional.of(fullConcept));
        when(conceptDetailEnrichmentService.mapImages(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapGpsPoints(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapExternalResources(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapContributors(fullConcept)).thenReturn("");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");
        var preferences = mock(fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences.class);
        when(preferences.exportUriType()).thenReturn(ExportUriType.HANDLE);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(preferences);
        when(conceptDetailEnrichmentService.mapExportIds(fullConcept, preferences))
                .thenReturn(new ConceptDetailEnrichmentService.ConceptExportIds("", "12345/abc", ""));

        var detail = service.loadDetail("TH1", "C1", "fr");

        assertTrue(detail.isPresent());
        assertTrue(detail.get().identifiers().showOriginalUri());
    }

    @Test
    void loadRootNodes_returnsEmptyWhenThesaurusBlank() {
        assertTrue(service.loadRootNodes("", "fr").isEmpty());
    }

    @Test
    void loadSummary_returnsEmptyWhenConceptMissing() {
        assertTrue(service.loadSummary("TH1", "", "fr").isEmpty());
    }

    @Test
    void searchByLabel_returnsEmptyWhenQueryBlank() {
        assertTrue(service.searchByLabel("TH1", "fr", " ", 10).isEmpty());
    }

    @Test
    void loadGroupDetail_returnsEmptyWhenGroupMissing() {
        assertTrue(service.loadGroupDetail("TH1", "", "fr").isEmpty());
    }

    @Test
    void countBranchConcepts_usesRepository() {
        when(conceptQueryRepository.countBranchConcepts("TH1", "C1")).thenReturn(12);

        assertEquals(12, service.countBranchConcepts("TH1", "C1"));
    }

    @Test
    void loadDetailWithSource_returnsDetailAndFullConcept() {
        ConceptFullSnapshot fullConcept = buildFullConcept();
        when(authenticatedUserSource.isLoggedIn()).thenReturn(false);
        when(conceptDetailEnrichmentService.loadFullConcept("TH1", "C1", "fr", false, false))
                .thenReturn(Optional.of(fullConcept));
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(null);
        when(conceptDetailEnrichmentService.mapExportIds(fullConcept, null))
                .thenReturn(new ConceptDetailEnrichmentService.ConceptExportIds("", "", ""));
        when(conceptDetailEnrichmentService.mapImages(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapGpsPoints(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapExternalResources(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapContributors(fullConcept)).thenReturn("");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");

        var loaded = service.loadDetailWithSource("TH1", "C1", "fr");

        assertTrue(loaded.isPresent());
        assertEquals(fullConcept, loaded.get().fullConcept());
        assertEquals("C1", loaded.get().detail().summary().conceptId());
    }

    @Test
    void buildDetailFromFullConcept_skipsBreadcrumbWhenPreferenceDisabled() {
        ConceptFullSnapshot fullConcept = buildFullConcept();
        var preferences = mockDetailPreferences(false, false);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(preferences);
        when(conceptDetailEnrichmentService.mapExportIds(fullConcept, preferences))
                .thenReturn(new ConceptDetailEnrichmentService.ConceptExportIds("", "", ""));
        when(conceptDetailEnrichmentService.mapImages(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapGpsPoints(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapExternalResources(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapContributors(fullConcept)).thenReturn("");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");

        ConceptDetail detail = service.buildDetailFromFullConcept(fullConcept, "TH1", "C1", "fr");

        assertTrue(detail.breadcrumb().isEmpty());
        verify(conceptBreadcrumbReadService, never()).loadBreadcrumbPaths(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void buildDetailFromFullConcept_loadsBreadcrumbWhenPreferenceEnabled() {
        ConceptFullSnapshot fullConcept = buildFullConcept();
        var preferences = mockDetailPreferences(true, false);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(preferences);
        when(conceptBreadcrumbReadService.loadBreadcrumbPaths("TH1", "C1", "fr")).thenReturn(List.of(
                List.of(new BreadcrumbStep("C1", "Root", 0, true))
        ));
        when(conceptDetailEnrichmentService.mapExportIds(fullConcept, preferences))
                .thenReturn(new ConceptDetailEnrichmentService.ConceptExportIds("", "", ""));
        when(conceptDetailEnrichmentService.mapImages(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapGpsPoints(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapExternalResources(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapContributors(fullConcept)).thenReturn("");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");

        ConceptDetail detail = service.buildDetailFromFullConcept(fullConcept, "TH1", "C1", "fr");

        assertEquals(1, detail.breadcrumb().size());
        assertEquals("Root", detail.breadcrumb().get(0).label());
        verify(conceptBreadcrumbReadService).loadBreadcrumbPaths("TH1", "C1", "fr");
    }

    @Test
    void buildDetailFromFullConcept_loadsCustomRelationsWhenPreferenceEnabled() {
        ConceptFullSnapshot fullConcept = buildFullConcept();
        var preferences = mockDetailPreferences(false, true);
        when(thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr")).thenReturn(preferences);
        when(conceptDetailEnrichmentService.loadCustomRelations("TH1", "C1", "fr")).thenReturn(List.of(
                new ConceptCustomRelationItem("C2", "Target", "REL", "Relation", false)
        ));
        when(conceptDetailEnrichmentService.mapExportIds(fullConcept, preferences))
                .thenReturn(new ConceptDetailEnrichmentService.ConceptExportIds("", "", ""));
        when(conceptDetailEnrichmentService.mapImages(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapGpsPoints(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapExternalResources(fullConcept)).thenReturn(List.of());
        when(conceptDetailEnrichmentService.mapContributors(fullConcept)).thenReturn("");
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("http://localhost");

        ConceptDetail detail = service.buildDetailFromFullConcept(fullConcept, "TH1", "C1", "fr");

        assertEquals(1, detail.customRelations().size());
        assertEquals("C2", detail.customRelations().get(0).targetConceptId());
        verify(conceptDetailEnrichmentService).loadCustomRelations("TH1", "C1", "fr");
    }

    private ConceptFullSnapshot buildFullConcept() {
        ConceptFullSnapshot fullConcept = new ConceptFullSnapshot();
        fullConcept.setIdentifier("C1");
        fullConcept.setResourceStatus(ConceptResourceStatus.CONCEPT);
        fullConcept.setConceptType("concept");
        fullConcept.setPrefLabel(new ConceptTermLabel("fr", "Label", "T1", 1));
        fullConcept.setBroaders(List.of(
                new ConceptHierarchicalRelation("", "BT1", "Parent", "BT")
        ));
        return fullConcept;
    }

    private fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences mockDetailPreferences(
            boolean breadcrumb,
            boolean useCustomRelation
    ) {
        var preferences = mock(fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences.class);
        when(preferences.breadcrumb()).thenReturn(breadcrumb);
        when(preferences.useCustomRelation()).thenReturn(useCustomRelation);
        when(preferences.exportUriType()).thenReturn(ExportUriType.URI);
        when(preferences.originalUri()).thenReturn("");
        return preferences;
    }
}
