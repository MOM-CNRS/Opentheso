package fr.cnrs.opentheso.v2.concept.alignment.service;

import fr.cnrs.opentheso.entites.AlignementSource;
import fr.cnrs.opentheso.models.AlignementSourceProjection;
import fr.cnrs.opentheso.models.NodeAlignmentProjection;
import fr.cnrs.opentheso.models.NodeSelectedAlignmentProjection;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.ThesaurusAlignementSourceRepository;
import fr.cnrs.opentheso.v2.candidat.alignment.AlignmentAutoExternalSearch;
import fr.cnrs.opentheso.v2.candidat.alignment.persistence.CandidatAutoAlignmentPersistence;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentAdminRow;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentProposition;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptAlignmentAdminServiceTest {

    @Mock
    private BranchConceptSupport branchConceptSupport;
    @Mock
    private ConceptSearchQueryRepository conceptSearchQueryRepository;
    @Mock
    private AlignementRepository alignementRepository;
    @Mock
    private AlignementSourceRepository alignementSourceRepository;
    @Mock
    private ThesaurusAlignementSourceRepository thesaurusAlignementSourceRepository;
    @Mock
    private AlignmentAutoExternalSearch alignmentAutoExternalSearch;
    @Mock
    private ConceptAlignmentMutationService conceptAlignmentMutationService;
    @Mock
    private CandidatAutoAlignmentPersistence candidatAutoAlignmentPersistence;
    @Mock
    private AlignmentPropositionEnricher alignmentPropositionEnricher;

    private ConceptAlignmentAdminService service;

    @BeforeEach
    void setUp() {
        service = new ConceptAlignmentAdminService(
                branchConceptSupport,
                conceptSearchQueryRepository,
                alignementRepository,
                alignementSourceRepository,
                thesaurusAlignementSourceRepository,
                alignmentAutoExternalSearch,
                conceptAlignmentMutationService,
                candidatAutoAlignmentPersistence,
                alignmentPropositionEnricher
        );
    }

    @Test
    void loadBranchSummary_includesPlaceholderAndAlignments() {
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C1")).thenReturn(List.of("C1", "C2"));
        when(conceptSearchQueryRepository.findPreferredLabelsByIds(List.of("C1", "C2"), "TH1", "fr"))
                .thenReturn(Map.of("C1", "Racine", "C2", "Enfant"));
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C1", "TH1"))
                .thenReturn(List.of(projection(10, "C1", "http://ex.org/a", "exactMatch", "Wikidata", true)));
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C2", "TH1"))
                .thenReturn(List.of());

        List<AlignmentAdminRow> rows = service.loadBranchSummary("TH1", "C1", "fr");

        assertEquals(3, rows.size()); // C1 alignment + C1 placeholder + C2 placeholder
        assertEquals(1, service.countAlignments(rows));
        assertTrue(rows.stream().anyMatch(AlignmentAdminRow::isPlaceholder));
    }

    @Test
    void listSourcesForManagement_returnsAllSourcesAndMarksThoseActiveForThesaurus() {
        when(alignementSourceRepository.findByIsGlobalTrueOrIdThesaurusOwner("TH1")).thenReturn(List.of(
                AlignementSource.builder()
                        .id(1)
                        .source("Wikidata")
                        .description("Wiki")
                        .sourceFilter("wikidata")
                        .requete("https://www.wikidata.org/")
                        .isGlobal(true)
                        .build(),
                AlignementSource.builder()
                        .id(2)
                        .source("Local")
                        .description("Opentheso local")
                        .sourceFilter("Opentheso")
                        .requete("https://ex.org/")
                        .isGlobal(false)
                        .idThesaurusOwner("TH1")
                        .build()
        ));
        when(alignementRepository.findSelectedAlignmentsByThesaurus("TH1"))
                .thenReturn(List.of(selectedSource(1, "Wikidata", "Wiki")));

        List<AlignmentSourceItem> sources = service.listSourcesForManagement("TH1");

        assertEquals(2, sources.size());
        assertEquals("Wikidata", sources.get(0).getLabel());
        assertTrue(sources.get(0).isSelected());
        assertTrue(sources.get(0).isGlobal());
        assertEquals("wikidata", sources.get(0).getSourceType());
        assertEquals("https://www.wikidata.org/", sources.get(0).getUrl());
        assertEquals("Local", sources.get(1).getLabel());
        assertFalse(sources.get(1).isSelected());
        assertFalse(sources.get(1).isGlobal());
        assertEquals("TH1", sources.get(1).getThesaurusOwner());
        assertTrue(sources.get(1).isLocalSource());
    }

    @Test
    void searchPropositionsForConcept_acceptsHitsWithoutGps() {
        var source = fr.cnrs.opentheso.models.alignment.AlignementSource.builder()
                .id(4)
                .source("Wikidata")
                .source_filter("WIKIDATA_REST")
                .build();
        NodeAlignment hit = new NodeAlignment();
        hit.setConcept_target("Cat");
        hit.setUri_target("http://www.wikidata.org/entity/Q1");
        hit.setDef_target("a feline");
        hit.setAlignement_id_type(1);
        when(alignmentAutoExternalSearch.search(eq(source), any()))
                .thenReturn(new AlignmentAutoExternalSearch.SearchOutcome(List.of(hit), null));

        List<AlignmentProposition> hits = service.searchPropositionsForConcept(
                "TH1", "fr", "C1", "Chat", source);

        assertEquals(1, hits.size());
        assertEquals("Cat", hits.get(0).getTargetLabel());
        assertEquals("http://www.wikidata.org/entity/Q1", hits.get(0).getTargetUri());
        assertNull(hits.get(0).getLatitude());
        assertNull(hits.get(0).getLongitude());
    }

    @Test
    void searchComparisonsForConcept_marksCurrentUriAndLimitsHits() {
        var source = fr.cnrs.opentheso.models.alignment.AlignementSource.builder()
                .id(4)
                .source("Wikidata")
                .source_filter("WIKIDATA_REST")
                .requete("https://www.wikidata.org")
                .build();
        NodeAlignment same = hit("Cat", "http://www.wikidata.org/entity/Q1");
        NodeAlignment other = hit("Felis", "http://www.wikidata.org/entity/Q9");
        NodeAlignment extra = hit("Extra", "http://www.wikidata.org/entity/Q3");
        NodeAlignment fourth = hit("Fourth", "http://www.wikidata.org/entity/Q4");
        when(alignmentAutoExternalSearch.search(eq(source), any()))
                .thenReturn(new AlignmentAutoExternalSearch.SearchOutcome(
                        List.of(same, other, extra, fourth), null));

        var local = new ConceptAlignment("1", "http://www.wikidata.org/entity/Q1", "exactMatch", "Wikidata", true, 1);
        List<AlignmentProposition> hits = service.searchComparisonsForConcept(
                "TH1", "fr", "C1", "Chat", "un félin", List.of(local), source);

        assertEquals(3, hits.size());
        assertTrue(hits.get(0).isAlreadyAligned());
        assertFalse(hits.get(1).isAlreadyAligned());
        assertEquals("http://www.wikidata.org/entity/Q1", hits.get(0).getLocalUri());
        assertEquals("un félin", hits.get(0).getLocalDefinition());
        assertEquals("Felis", hits.get(1).getTargetLabel());
    }

    @Test
    void searchComparisonsForConcept_skipsRemoteSearchWithoutLocalAlignment() {
        var source = fr.cnrs.opentheso.models.alignment.AlignementSource.builder()
                .id(4)
                .source("Wikidata")
                .build();

        List<AlignmentProposition> hits = service.searchComparisonsForConcept(
                "TH1", "fr", "C1", "Chat", "", List.of(), source);

        assertTrue(hits.isEmpty());
        verify(alignmentAutoExternalSearch, never()).search(any(), any());
    }

    @Test
    void loadBranchSummary_blankArgs_returnsEmpty() {
        assertTrue(service.loadBranchSummary(" ", "C1", "fr").isEmpty());
        assertEquals(0, service.countAlignments(null));
        assertEquals(0, service.countAlignmentsForConcept(null, "C1"));
        assertEquals(0, service.checkUrlsForConcept("TH1", "C1", null));
        assertTrue(service.listSourcesForManagement(" ").isEmpty());
        assertTrue(service.listActiveSources(" ").isEmpty());
    }

    @Test
    void setSourceSelected_savesOrDeletesLink() {
        service.setSourceSelected("TH1", 4, true);
        verify(thesaurusAlignementSourceRepository).save(any());

        service.setSourceSelected("TH1", 4, false);
        verify(thesaurusAlignementSourceRepository).deleteByIdThesaurusAndIdAlignementSource("TH1", 4);

        service.setSourceSelected(" ", 4, true);
        verify(thesaurusAlignementSourceRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void deleteLocalSource_returnsTrueWhenDeleted() {
        when(alignementSourceRepository.deleteByIdAlignementSource(8)).thenReturn(1);
        assertTrue(service.deleteLocalSource(8));
    }

    @Test
    void deleteLocalSource_returnsFalseOnError() {
        when(alignementSourceRepository.deleteByIdAlignementSource(8)).thenThrow(new RuntimeException("db"));
        assertFalse(service.deleteLocalSource(8));
    }

    @Test
    void listActiveSources_mapsProjectionAndFindById() {
        when(alignementSourceRepository.findAllByThesaurus("TH1")).thenReturn(List.of(activeSource(4, "Wikidata")));
        var sources = service.listActiveSources("TH1");
        assertEquals(1, sources.size());
        assertEquals(4, sources.get(0).getId());
        assertEquals("Wikidata", service.findActiveSource("TH1", 4).getSource());
        assertNull(service.findActiveSource("TH1", 99));
    }

    @Test
    void searchPropositions_runsForEachConceptInSummary() {
        var source = fr.cnrs.opentheso.models.alignment.AlignementSource.builder()
                .id(4).source("Wikidata").source_filter("WIKIDATA_REST").build();
        NodeAlignment hit = new NodeAlignment();
        hit.setConcept_target("Cat");
        hit.setUri_target("http://www.wikidata.org/entity/Q1");
        when(alignmentAutoExternalSearch.search(eq(source), any()))
                .thenReturn(new AlignmentAutoExternalSearch.SearchOutcome(List.of(hit), null));

        List<AlignmentAdminRow> rows = List.of(
                new AlignmentAdminRow("C1", "Chat", null, null, null, 0, null, true)
        );
        List<AlignmentProposition> hits = service.searchPropositions("TH1", "fr", rows, source);
        assertEquals(1, hits.size());
        assertEquals("C1", hits.get(0).getConceptId());
    }

    @Test
    void enrichProposition_delegates() {
        AlignmentProposition proposition = AlignmentProposition.builder().conceptId("C1").build();
        var source = fr.cnrs.opentheso.models.alignment.AlignementSource.builder().id(1).build();
        service.enrichProposition(proposition, source, "TH1", "fr");
        verify(alignmentPropositionEnricher).enrich(proposition, source, "TH1", "fr");
    }

    @Test
    void acceptProposition_persistsManualAlignmentAndEnrichments() {
        AlignmentProposition proposition = AlignmentProposition.builder()
                .conceptId("C1")
                .targetUri("http://www.wikidata.org/entity/Q1")
                .targetLabel("Cat")
                .sourceName("Wikidata")
                .sourceId(0)
                .alignmentTypeId(1)
                .build();
        when(conceptAlignmentMutationService.addManualAlignment(any())).thenReturn(MutationResult.ok("ok"));
        when(candidatAutoAlignmentPersistence.addSelectedTranslations(eq("TH1"), eq("C1"), eq(7), any())).thenReturn(true);
        when(candidatAutoAlignmentPersistence.addSelectedDefinitions(eq("C1"), eq("TH1"), eq(7), eq("Wikidata"), any())).thenReturn(true);
        when(candidatAutoAlignmentPersistence.addSelectedImages(eq("C1"), eq("TH1"), eq(7), any(), eq("Wikidata"), any())).thenReturn(true);

        assertTrue(service.acceptProposition("TH1", proposition, 7, "alice"));
        verify(candidatAutoAlignmentPersistence).touchConcept("TH1", "C1", 7);
    }

    @Test
    void acceptProposition_usesSourceAlignmentAndGpsForGeoNames() {
        AlignmentProposition proposition = AlignmentProposition.builder()
                .conceptId("C1")
                .targetUri("http://geonames.org/1")
                .targetLabel("Paris")
                .sourceName("GeoNames")
                .sourceId(9)
                .latitude(48.8)
                .longitude(2.3)
                .build();
        when(candidatAutoAlignmentPersistence.addAlignment(eq(7), eq("Paris"), eq("GeoNames"),
                eq("http://geonames.org/1"), eq(1), eq("C1"), eq("TH1"), eq(9))).thenReturn(true);
        when(candidatAutoAlignmentPersistence.addSelectedTranslations(any(), any(), eq(7), any())).thenReturn(true);
        when(candidatAutoAlignmentPersistence.addSelectedDefinitions(any(), any(), eq(7), any(), any())).thenReturn(true);
        when(candidatAutoAlignmentPersistence.addSelectedImages(any(), any(), eq(7), any(), any(), any())).thenReturn(true);

        assertTrue(service.acceptProposition("TH1", proposition, 7, "alice"));
        verify(candidatAutoAlignmentPersistence).insertGpsCoordinates("C1", "TH1", 48.8, 2.3);
    }

    @Test
    void acceptProposition_rejectsIncompletePayload() {
        assertFalse(service.acceptProposition("TH1", null, 1, "x"));
        assertFalse(service.acceptProposition("TH1", AlignmentProposition.builder().conceptId("C1").build(), 1, "x"));
    }

    @Test
    void searchComparisons_keepsExistingUriWhenRemoteEmpty() {
        var source = fr.cnrs.opentheso.models.alignment.AlignementSource.builder()
                .id(4).source("Wikidata").requete("https://www.wikidata.org/wiki").build();
        when(alignmentAutoExternalSearch.search(eq(source), any()))
                .thenReturn(new AlignmentAutoExternalSearch.SearchOutcome(List.of(), null));
        List<AlignmentAdminRow> rows = List.of(
                new AlignmentAdminRow("C1", "Chat", 10, "http://www.wikidata.org/entity/Q1", "exact", 1, "Wikidata", true),
                new AlignmentAdminRow("C1", "Chat", null, null, null, 0, null, true)
        );
        List<AlignmentProposition> hits = service.searchComparisons("TH1", "fr", rows, source);
        assertEquals(1, hits.size());
        assertEquals("http://www.wikidata.org/entity/Q1", hits.get(0).getTargetUri());
        assertTrue(hits.get(0).isAlreadyAligned());
    }

    @Test
    void replaceAlignmentFromProposition_deletesSameSourceThenAdds() {
        AlignmentProposition proposition = AlignmentProposition.builder()
                .conceptId("C1")
                .targetUri("http://www.wikidata.org/entity/Q9")
                .sourceName("Wikidata")
                .alignmentTypeId(1)
                .build();
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C1", "TH1"))
                .thenReturn(List.of(projection(10, "C1", "http://www.wikidata.org/entity/Q1", "exactMatch", "Wikidata", true)));
        when(conceptAlignmentMutationService.addManualAlignment(any())).thenReturn(MutationResult.ok("ok"));

        assertTrue(service.replaceAlignmentFromProposition("TH1", proposition, 7, "alice"));
        verify(alignementRepository).deleteByIdAndThesaurus(10, "TH1");
    }

    @Test
    void updateLocalSource_validatesAndSaves() {
        var entity = fr.cnrs.opentheso.entites.AlignementSource.builder().id(3).source("Old").build();
        when(alignementSourceRepository.findById(3)).thenReturn(java.util.Optional.of(entity));

        assertEquals("Le nom de la source est obligatoire !", service.updateLocalSource(3, " ", "http://x", "d"));
        assertNull(service.updateLocalSource(3, "Local", "http://x", "desc", "Opentheso"));
        verify(alignementSourceRepository).save(entity);
        assertEquals("Opentheso", entity.getSourceFilter());
    }

    @Test
    void addLocalSource_savesAndOptionallySelects() {
        var saved = fr.cnrs.opentheso.entites.AlignementSource.builder().id(12).source("Local").build();
        when(alignementSourceRepository.save(any())).thenReturn(saved);

        assertNull(service.addLocalSource("TH1", 7, "Local", "http://ex.org", "d", "Opentheso", true));
        verify(thesaurusAlignementSourceRepository).save(any());
        assertEquals("Le nom de la source est obligatoire !", service.addLocalSource("TH1", 7, " ", "http://x", "", "", false));
    }

    @Test
    void validateOpenthesoSource_rejectsBlankFieldsAndUnreachableUri() {
        assertEquals("Le nom de la source est obligatoire !", service.validateOpenthesoSource(" ", "http://x", "th"));
        assertEquals("L'URL est obligatoire !", service.validateOpenthesoSource("n", " ", "th"));
        assertEquals("L'Id. du thésaurus est obligatoire !", service.validateOpenthesoSource("n", "http://x", " "));
        assertEquals("Uri du serveur non valide !", service.validateOpenthesoSource("n", "not a uri", "th"));
    }

    @Test
    void alignmentsTowardSource_matchesByNameOrHost() {
        var source = fr.cnrs.opentheso.models.alignment.AlignementSource.builder()
                .source("Wikidata").requete("https://www.wikidata.org/wiki").build();
        var sameName = new ConceptAlignment("1", "http://other.org/a", "exact", "Wikidata", true, 1);
        var sameHost = new ConceptAlignment("2", "https://www.wikidata.org/entity/Q1", "exact", "Other", true, 1);
        var other = new ConceptAlignment("3", "https://example.org/x", "exact", "Gemet", true, 1);

        var matched = service.alignmentsTowardSource(java.util.Arrays.asList(sameName, sameHost, other, null), source);
        assertEquals(2, matched.size());
        assertTrue(service.alignmentsTowardSource(null, source).isEmpty());
    }

    @Test
    void checkUrlsForConcept_marksUnreachableUrls() {
        List<AlignmentAdminRow> rows = List.of(
                new AlignmentAdminRow("C1", "Chat", 10, "not-a-url", "exact", 1, "Wikidata", true),
                new AlignmentAdminRow("C1", "Chat", null, null, null, 0, null, true)
        );
        var entity = fr.cnrs.opentheso.entites.Alignement.builder().id(10).urlAvailable(true).build();
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 10))
                .thenReturn(java.util.Optional.of(entity));

        assertEquals(1, service.checkUrlsForConcept("TH1", "C1", rows));
        verify(alignementRepository).save(entity);
        assertFalse(entity.getUrlAvailable());
    }

    private static AlignementSourceProjection activeSource(int id, String source) {
        return new AlignementSourceProjection() {
            @Override public String getSource() { return source; }
            @Override public String getRequete() { return "https://www.wikidata.org"; }
            @Override public String getTypeRequete() { return "REST"; }
            @Override public String getAlignement_format() { return "skos"; }
            @Override public int getId() { return id; }
            @Override public String getDescription() { return source; }
            @Override public String getSource_filter() { return "wikidata"; }
            @Override public boolean getGps() { return false; }
        };
    }

    private static NodeAlignment hit(String label, String uri) {
        NodeAlignment node = new NodeAlignment();
        node.setConcept_target(label);
        node.setUri_target(uri);
        return node;
    }

    private static NodeSelectedAlignmentProjection selectedSource(int id, String source, String description) {
        return new NodeSelectedAlignmentProjection() {
            @Override public int getId_alignement_source() { return id; }
            @Override public String getSource() { return source; }
            @Override public String getDescription() { return description; }
        };
    }

    private NodeAlignmentProjection projection(
            int id, String conceptId, String uri, String label, String source, boolean available
    ) {
        return new NodeAlignmentProjection() {
            @Override public int getId() { return id; }
            @Override public Date getCreated() { return null; }
            @Override public Date getModified() { return null; }
            @Override public int getAuthor() { return 1; }
            @Override public String getThesaurus_target() { return source; }
            @Override public String getConcept_target() { return ""; }
            @Override public String getUri_target() { return uri; }
            @Override public int getAlignement_id_type() { return 1; }
            @Override public String getInternal_id_thesaurus() { return "TH1"; }
            @Override public String getInternal_id_concept() { return conceptId; }
            @Override public Integer getId_alignement_source() { return null; }
            @Override public String getLabel() { return label; }
            @Override public String getLabel_skos() { return "skos:exactMatch"; }
            @Override public boolean getUrl_available() { return available; }
        };
    }
}
