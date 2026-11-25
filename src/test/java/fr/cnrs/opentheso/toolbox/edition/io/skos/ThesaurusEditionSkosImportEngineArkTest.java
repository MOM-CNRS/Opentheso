package fr.cnrs.opentheso.toolbox.edition.io.skos;

import fr.cnrs.opentheso.concept.io.rdf.parser.ReadRdf4jDocument;
import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.RelationGroup;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptReplacedByRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.toolbox.persistence.ToolboxThesaurusPersistence;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionSkosImportEngineArkTest {

    private static final String ARK = "https://ark.frantiq.fr/ark:/26678/";

    @Mock
    private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private PreferencesRepository preferencesRepository;
    @Mock
    private ThesaurusLabelRepository thesaurusLabelRepository;
    @Mock
    private ThesaurusDcTermRepository thesaurusDcTermRepository;
    @Mock
    private UserGroupThesaurusRepository userGroupThesaurusRepository;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ConceptFacetRepository conceptFacetRepository;
    @Mock
    private ExternalResourcesRepository externalResourcesRepository;
    @Mock
    private ImagesRepository imagesRepository;
    @Mock
    private ConceptGroupRepository conceptGroupRepository;
    @Mock
    private ConceptGroupConceptRepository conceptGroupConceptRepository;
    @Mock
    private ConceptGroupLabelRepository conceptGroupLabelRepository;
    @Mock
    private ConceptGroupLabelHistoriqueRepository conceptGroupLabelHistoriqueRepository;
    @Mock
    private RelationGroupRepository relationGroupRepository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private ConceptReplacedByRepository conceptReplacedByRepository;

    @InjectMocks
    private ThesaurusEditionSkosImportEngine engine;

    /** Collections avant les concepts, sous-collection par member et par superGroup, concept partagé. */
    private static final String COLLECTIONS = """
            <a/colA> a skos:Collection ; dcterms:identifier "G184" ; skos:prefLabel "A"@fr ;
              skos:member <a/c1>, <a/c2>, <a/colB> ;
              iso:subGroup <https://autre.example/ark:/26678/colC> .
            <a/colB> a skos:Collection ; dcterms:identifier "g185" ; skos:member <a/c1> .
            <a/colC> a skos:Collection ; dcterms:identifier "g186" ; iso:superGroup <a/colA> ; skos:member <a/colC> .
            <a/F> a iso:ThesaurusArray ; dcterms:identifier "F12" ; skos:prefLabel "f"@fr ;
              iso:superOrdinate <a/c1> ; skos:member <a/c2> .
            <a/TH_1> a skos:ConceptScheme ; skos:prefLabel "Theso"@fr ; dcterms:identifier "TH_1" ;
              skos:hasTopConcept <a/c1> .
            <a/c1> a skos:Concept ; dcterms:identifier "253965" ; skos:prefLabel "un"@fr .
            <a/c2> a skos:Concept ; dcterms:identifier "184" ; skos:prefLabel "deux"@fr ; skos:broader <a/c1> .
            """;

    @Test
    void collectionsUseIdentifierAndKeepEveryMember() throws Exception {
        configure(read(COLLECTIONS));
        when(conceptGroupRepository.getNextConceptGroupSequence()).thenReturn(1L);

        engine.addGroups(engine.getSkosXmlDocument().getGroupList(), "th9");

        var groups = ArgumentCaptor.forClass(ConceptGroup.class);
        verify(conceptGroupRepository, atLeastOnce()).save(groups.capture());
        assertEquals(List.of("g184", "g185", "g186"),
                groups.getAllValues().stream().map(ConceptGroup::getIdGroup).toList());
        assertEquals("26678/colA", groups.getAllValues().get(0).getIdArk());

        assertEquals(Set.of("g184>253965", "g184>184", "g185>253965"), savedMembers());

        var subGroups = ArgumentCaptor.forClass(RelationGroup.class);
        verify(relationGroupRepository, atLeastOnce()).save(subGroups.capture());
        assertEquals(2, subGroups.getAllValues().size());
        assertEquals(Set.of("g184>g185", "g184>g186"), subGroups.getAllValues().stream()
                .map(link -> link.getIdGroup1() + ">" + link.getIdGroup2()).collect(Collectors.toSet()));
    }

    @Test
    void facetUsesIdentifier() throws Exception {
        configure(read(COLLECTIONS));

        engine.addFacetsV2(engine.getSkosXmlDocument().getFacetList(), "th9");

        verify(conceptFacetRepository).addFacet(eq("F12"), eq(7), eq("th9"), eq("253965"), any(), eq("184"), any());
    }

    @Test
    void withoutIdentifierIdsStillComeFromTheUri() throws Exception {
        configure(read("""
                <http://ex.org/?idg=G5&idt=th1> a skos:Collection ; skos:member <http://ex.org/?idc=33&idt=th1> .
                <http://ex.org/?idc=33&idt=th1> a skos:Concept ; skos:prefLabel "x"@fr .
                """));
        when(conceptGroupRepository.getNextConceptGroupSequence()).thenReturn(1L);

        engine.addGroups(engine.getSkosXmlDocument().getGroupList(), "th9");

        assertEquals(Set.of("g5>33"), savedMembers());
    }

    @Test
    void schemeArkIsKeptAndOriginalUriDoesNotRepeatTheNaan() throws Exception {
        var preferences = createThesaurus(false);

        assertEquals("th9", engine.addThesaurus());
        engine.addConceptV2(engine.getSkosXmlDocument().getConceptList().get(0), "th9");

        verify(toolboxThesaurusPersistence).updateArkId("th9", "26678/TH_1");
        assertEquals("https://ark.frantiq.fr/ark:", preferences.getOriginalUri());
        assertEquals("https://ark.frantiq.fr/ark:/", preferences.getUriArk());
        assertEquals("26678", preferences.getIdNaan());
        assertEquals("http://monsite/", preferences.getCheminSite());
        assertEquals(Set.of(ARK + "c1"), engine.getHasTopConcceptList());
        assertEquals("TH_1", engine.getSkosXmlDocument().getConceptScheme().getIdentifier());
    }

    @Test
    void arkOfAnotherThesaurusIsNotReassigned() throws Exception {
        createThesaurus(true);

        assertEquals("th9", engine.addThesaurus());

        verify(toolboxThesaurusPersistence, never()).updateArkId(anyString(), anyString());
    }

    @Test
    void duplicateCollectionIdentifierStopsBeforeAnyWrite() throws Exception {
        configure(read("""
                <a/TH_1> a skos:ConceptScheme ; skos:prefLabel "Theso"@fr .
                <a/colA> a skos:Collection ; dcterms:identifier "g1" .
                <a/colB> a skos:Collection ; dcterms:identifier "G1" .
                """));

        assertNull(engine.addThesaurus());
        assertTrue(engine.getMessage().toString().contains("g1"));
        verify(toolboxThesaurusPersistence, never()).createThesaurusId();
    }

    private Preferences createThesaurus(boolean arkAlreadyUsed) throws IOException {
        configure(read(COLLECTIONS));
        var preferences = Preferences.builder().idThesaurus("th9").cheminSite("http://monsite/").build();
        when(toolboxThesaurusPersistence.createThesaurusId()).thenReturn("th9");
        when(toolboxThesaurusPersistence.arkExists("26678/TH_1")).thenReturn(arkAlreadyUsed);
        when(toolboxPreferencePersistence.findPreferences("th9")).thenReturn(preferences);
        return preferences;
    }

    @SuppressWarnings("unchecked")
    private Set<String> savedMembers() {
        ArgumentCaptor<List<ConceptGroupConcept>> links = ArgumentCaptor.forClass(List.class);
        verify(conceptGroupConceptRepository).saveAll(links.capture());
        return links.getValue().stream()
                .map(link -> link.getIdGroup() + ">" + link.getIdConcept()).collect(Collectors.toSet());
    }

    private void configure(SKOSXmlDocument document) {
        engine.setInfos("yyyy-MM-dd", 7, -1, "fr");
        engine.setSelectedIdentifier("ark");
        engine.setRdf4jThesaurus(document);
    }

    private static SKOSXmlDocument read(String body) throws IOException {
        String turtle = """
                @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
                @prefix iso: <http://purl.org/iso25964/skos-thes#> .
                @prefix dcterms: <http://purl.org/dc/terms/> .
                """ + body.replace("<a/", "<" + ARK);
        return new ReadRdf4jDocument().readRdfFlux(
                new ByteArrayInputStream(turtle.getBytes(StandardCharsets.UTF_8)),
                RDFFormat.TURTLE, "fr", new StringBuilder());
    }
}
