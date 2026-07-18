package fr.cnrs.opentheso.v2.toolbox.workshop.persistence;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.AlignementSource;
import fr.cnrs.opentheso.entites.AlignementType;
import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.ConceptHistorique;
import fr.cnrs.opentheso.entites.ConceptReplacedBy;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.entites.NonPreferredTermHistorique;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptReplacedByRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipHistoriqueRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.UserGroupLabelRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.v2.concept.search.service.ConceptSearchEngine;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvImportEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopBulkImportPersistenceTest {

    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private UserGroupLabelRepository userGroupLabelRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ConceptHistoriqueRepository conceptHistoriqueRepository;
    @Mock
    private ConceptReplacedByRepository conceptReplacedByRepository;
    @Mock
    private ConceptDcTermRepository conceptDcTermRepository;
    @Mock
    private ConceptGroupRepository conceptGroupRepository;
    @Mock
    private ConceptGroupConceptRepository conceptGroupConceptRepository;
    @Mock
    private PreferredTermRepository preferredTermRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private TermHistoriqueRepository termHistoriqueRepository;
    @Mock
    private NonPreferredTermRepository nonPreferredTermRepository;
    @Mock
    private NonPreferredTermHistoriqueRepository nonPreferredTermHistoriqueRepository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private AlignementRepository alignementRepository;
    @Mock
    private AlignementTypeRepository alignementTypeRepository;
    @Mock
    private AlignementSourceRepository alignementSourceRepository;
    @Mock
    private HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    @Mock
    private HierarchicalRelationshipHistoriqueRepository hierarchicalRelationshipHistoriqueRepository;
    @Mock
    private ImagesRepository imagesRepository;
    @Mock
    private ConceptSearchEngine conceptSearchEngine;
    @Mock
    private ThesaurusCsvImportEngine thesaurusCsvImportEngine;
    @Mock
    private WorkshopCsvConceptUpdater workshopCsvConceptUpdater;

    @InjectMocks
    private WorkshopBulkImportPersistence persistence;

    // ---- addNote ----

    @Test
    void addNote_savesNewNote_whenNoneExistsForIdentifierLangAndType() {
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
                "C1", "TH1", "definition", "fr")).thenReturn(List.of());

        persistence.addNote("C1", "fr", "TH1", "Une definition", "definition", "manual", 7);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        Note saved = captor.getValue();
        assertEquals("C1", saved.getIdentifier());
        assertEquals("TH1", saved.getIdThesaurus());
        assertEquals("fr", saved.getLang());
        assertEquals("definition", saved.getNoteTypeCode());
        assertEquals("Une definition", saved.getLexicalValue());
        assertEquals("manual", saved.getNoteSource());
        assertEquals(7, saved.getIdUser());
    }

    @Test
    void addNote_updatesExistingNote_insteadOfCreatingDuplicate() {
        Note existing = Note.builder()
                .id(99)
                .identifier("C1")
                .idThesaurus("TH1")
                .lang("fr")
                .noteTypeCode("definition")
                .lexicalValue("Ancienne valeur")
                .noteSource("old-source")
                .build();
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
                "C1", "TH1", "definition", "fr")).thenReturn(List.of(existing));

        persistence.addNote("C1", "fr", "TH1", "Nouvelle valeur", "definition", "new-source", 7);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        Note saved = captor.getValue();
        assertEquals(99, saved.getId());
        assertEquals("Nouvelle valeur", saved.getLexicalValue());
        assertEquals("new-source", saved.getNoteSource());
    }

    // ---- addNewAlignment ----

    @Test
    void addNewAlignment_savesAlignment_whenNotAlreadyPresent() {
        NodeAlignment nodeAlignment = NodeAlignment.builder()
                .id_source(null)
                .thesaurus_target("Wikidata")
                .internal_id_concept("C1")
                .internal_id_thesaurus("TH1")
                .alignement_id_type(1)
                .uri_target("http://example.org/x")
                .concept_target("Concept X")
                .id_author(42)
                .build();
        AlignementType type = new AlignementType(1, "Equivalence exacte", "fr", "skos:exactMatch");
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 1, "http://example.org/x"))
                .thenReturn(false);
        when(alignementTypeRepository.findById(1)).thenReturn(Optional.of(type));

        boolean result = persistence.addNewAlignment(nodeAlignment);

        assertTrue(result);
        ArgumentCaptor<Alignement> captor = ArgumentCaptor.forClass(Alignement.class);
        verify(alignementRepository).save(captor.capture());
        Alignement saved = captor.getValue();
        assertEquals("C1", saved.getInternalIdConcept());
        assertEquals("TH1", saved.getInternalIdThesaurus());
        assertEquals("http://example.org/x", saved.getUriTarget());
        assertEquals("Wikidata", saved.getThesaurusTarget());
        assertEquals("Concept X", saved.getConceptTarget());
        assertEquals(type, saved.getAlignementType());
        assertEquals(42, saved.getAuthor());
        assertTrue(saved.getUrlAvailable());
        verify(alignementSourceRepository, never()).findById(anyInt());
    }

    @Test
    void addNewAlignment_doesNotDuplicate_whenAlignmentAlreadyExists() {
        NodeAlignment nodeAlignment = NodeAlignment.builder()
                .internal_id_concept("C1")
                .internal_id_thesaurus("TH1")
                .alignement_id_type(1)
                .uri_target("http://example.org/x")
                .build();
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 1, "http://example.org/x"))
                .thenReturn(true);

        boolean result = persistence.addNewAlignment(nodeAlignment);

        assertTrue(result);
        verify(alignementRepository, never()).save(any());
        verify(alignementTypeRepository, never()).findById(anyInt());
    }

    @Test
    void addNewAlignment_returnsFalse_whenAlignmentTypeUnknown() {
        NodeAlignment nodeAlignment = NodeAlignment.builder()
                .internal_id_concept("C1")
                .internal_id_thesaurus("TH1")
                .alignement_id_type(999)
                .uri_target("http://example.org/x")
                .build();
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 999, "http://example.org/x"))
                .thenReturn(false);
        when(alignementTypeRepository.findById(999)).thenReturn(Optional.empty());

        boolean result = persistence.addNewAlignment(nodeAlignment);

        assertFalse(result);
        verify(alignementRepository, never()).save(any());
    }

    // ---- addNonPreferredTerm ----

    @Test
    void addNonPreferredTerm_savesTermAndTrace_whenLabelNotAlreadyUsed() {
        Term term = Term.builder()
                .idTerm("T1")
                .lexicalValue("Synonyme")
                .lang("fr")
                .idThesaurus("TH1")
                .source("import")
                .status("")
                .hidden(false)
                .build();
        when(nonPreferredTermRepository.isAltLabelExist("Synonyme", "TH1", "fr")).thenReturn(false);

        boolean result = persistence.addNonPreferredTerm(term, 7);

        assertTrue(result);
        ArgumentCaptor<NonPreferredTerm> captor = ArgumentCaptor.forClass(NonPreferredTerm.class);
        verify(nonPreferredTermRepository).save(captor.capture());
        NonPreferredTerm saved = captor.getValue();
        assertEquals("T1", saved.getIdTerm());
        assertEquals("Synonyme", saved.getLexicalValue());
        assertEquals("fr", saved.getLang());
        assertEquals("TH1", saved.getIdThesaurus());
        assertFalse(saved.isHiden());

        ArgumentCaptor<NonPreferredTermHistorique> historiqueCaptor = ArgumentCaptor.forClass(NonPreferredTermHistorique.class);
        verify(nonPreferredTermHistoriqueRepository).save(historiqueCaptor.capture());
        assertEquals("ADD", historiqueCaptor.getValue().getAction());
        assertEquals("T1", historiqueCaptor.getValue().getIdTerm());
    }

    @Test
    void addNonPreferredTerm_returnsFalse_whenLabelAlreadyExists() {
        Term term = Term.builder()
                .idTerm("T1")
                .lexicalValue("Synonyme")
                .lang("fr")
                .idThesaurus("TH1")
                .build();
        when(nonPreferredTermRepository.isAltLabelExist("Synonyme", "TH1", "fr")).thenReturn(true);

        boolean result = persistence.addNonPreferredTerm(term, 7);

        assertFalse(result);
        verify(nonPreferredTermRepository, never()).save(any());
        verify(nonPreferredTermHistoriqueRepository, never()).save(any());
    }

    // ---- deprecateConcept ----

    @Test
    void deprecateConcept_updatesStatusAndRecordsHistorique() {
        Concept existingConcept = Concept.builder()
                .idConcept("C1")
                .idThesaurus("TH1")
                .idArk("ark123")
                .status("VA")
                .notation("N1")
                .topConcept(false)
                .creator(7)
                .contributor(7)
                .created(new Date())
                .modified(new Date())
                .build();
        when(conceptRepository.findByIdConceptAndIdThesaurus("C1", "TH1")).thenReturn(Optional.of(existingConcept));

        boolean result = persistence.deprecateConcept("C1", "TH1", 7);

        assertTrue(result);
        verify(conceptRepository).setStatus("DEP", "C1", "TH1");
        ArgumentCaptor<ConceptHistorique> captor = ArgumentCaptor.forClass(ConceptHistorique.class);
        verify(conceptHistoriqueRepository).save(captor.capture());
        ConceptHistorique saved = captor.getValue();
        assertEquals("C1", saved.getIdConcept());
        assertEquals("TH1", saved.getIdThesaurus());
        assertEquals(7, saved.getIdUser());
    }

    // ---- addReplacedBy ----

    @Test
    void addReplacedBy_savesReplacementLink() {
        persistence.addReplacedBy("C1", "TH1", "C2", 7);

        ArgumentCaptor<ConceptReplacedBy> captor = ArgumentCaptor.forClass(ConceptReplacedBy.class);
        verify(conceptReplacedByRepository).save(captor.capture());
        ConceptReplacedBy saved = captor.getValue();
        assertEquals("C1", saved.getIdConcept1());
        assertEquals("C2", saved.getIdConcept2());
        assertEquals("TH1", saved.getIdThesaurus());
        assertEquals(7, saved.getIdUser());
    }

    // ---- deleteAlignmentByUri ----

    @Test
    void deleteAlignmentByUri_returnsTrue_whenRowsWereDeleted() {
        when(alignementRepository.deleteByUriAndConceptAndThesaurus("http://example.org/x", "C1", "TH1"))
                .thenReturn(1);

        boolean result = persistence.deleteAlignmentByUri("http://example.org/x", "C1", "TH1");

        assertTrue(result);
    }

    @Test
    void deleteAlignmentByUri_returnsFalse_whenRepositoryThrows() {
        when(alignementRepository.deleteByUriAndConceptAndThesaurus(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("db error"));

        boolean result = persistence.deleteAlignmentByUri("http://example.org/x", "C1", "TH1");

        assertFalse(result);
        assertTrue(persistence.getMessage().contains("db error"));
    }

    // ---- addConceptGroupConcept ----

    @Test
    void addConceptGroupConcept_savesGroupMembership() {
        boolean result = persistence.addConceptGroupConcept("G1", "C1", "TH1");

        assertTrue(result);
        ArgumentCaptor<ConceptGroupConcept> captor = ArgumentCaptor.forClass(ConceptGroupConcept.class);
        verify(conceptGroupConceptRepository).save(captor.capture());
        ConceptGroupConcept saved = captor.getValue();
        assertEquals("G1", saved.getIdGroup());
        assertEquals("C1", saved.getIdConcept());
        assertEquals("TH1", saved.getIdThesaurus());
    }
}
