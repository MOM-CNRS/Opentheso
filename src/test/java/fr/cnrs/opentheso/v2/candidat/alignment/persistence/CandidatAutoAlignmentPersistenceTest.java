package fr.cnrs.opentheso.v2.candidat.alignment.persistence;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.AlignementType;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.entites.ImageExterne;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.models.AlignementSourceProjection;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptTranslationWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptWritePostMutationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatAutoAlignmentPersistenceTest {

    @Mock private AlignementSourceRepository alignementSourceRepository;
    @Mock private AlignementTypeRepository alignementTypeRepository;
    @Mock private ThesaurusLabelRepository thesaurusLabelRepository;
    @Mock private AlignementRepository alignementRepository;
    @Mock private PreferredTermRepository preferredTermRepository;
    @Mock private TermRepository termRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private ImagesRepository imagesRepository;
    @Mock private GpsRepository gpsRepository;
    @Mock private ConceptRepository conceptRepository;
    @Mock private ConceptTranslationWriteRepository conceptTranslationWriteRepository;
    @Mock private ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @InjectMocks
    private CandidatAutoAlignmentPersistence persistence;

    @Test
    void loadAlignmentSources_returnsEmptyWhenNoneFound() {
        when(alignementSourceRepository.findAllByThesaurus("TH1")).thenReturn(List.of());

        assertTrue(persistence.loadAlignmentSources("TH1").isEmpty());
    }

    @Test
    void loadAlignmentSources_mapsProjections() {
        var projection = mock(AlignementSourceProjection.class);
        when(projection.getId()).thenReturn(1);
        when(projection.getSource()).thenReturn("Wikidata");
        when(alignementSourceRepository.findAllByThesaurus("TH1")).thenReturn(List.of(projection));

        var result = persistence.loadAlignmentSources("TH1");

        assertEquals(1, result.size());
        assertEquals("Wikidata", result.get(0).getSource());
    }

    @Test
    void loadAlignmentTypes_mapsIdToLabel() {
        var type = new AlignementType();
        type.setId(3);
        type.setLabelSkos("exactMatch");
        when(alignementTypeRepository.findAll()).thenReturn(List.of(type));

        var result = persistence.loadAlignmentTypes();

        assertEquals(1, result.size());
        assertEquals("3", result.get(0).getKey());
        assertEquals("exactMatch", result.get(0).getValue());
    }

    @Test
    void loadThesaurusLanguages_delegatesToRepository() {
        when(thesaurusLabelRepository.findDistinctLangByIdThesaurus("TH1")).thenReturn(List.of("fr", "en"));

        assertEquals(List.of("fr", "en"), persistence.loadThesaurusLanguages("TH1"));
    }

    @Test
    void loadExistingAlignments_returnsEmptyWhenNoneFound() {
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C1", "TH1")).thenReturn(List.of());

        assertTrue(persistence.loadExistingAlignments("C1", "TH1").isEmpty());
    }

    @Test
    void loadNotes_mapsNoteEntities() {
        when(noteRepository.findAllByIdentifierAndIdThesaurus("C1", "TH1")).thenReturn(List.of(
                Note.builder().id(1).lang("fr").lexicalValue("Value").noteTypeCode("note").build()));

        var result = persistence.loadNotes("C1", "TH1");

        assertEquals(1, result.size());
        assertEquals("Value", result.get(0).getLexicalValue());
    }

    @Test
    void loadImages_mapsImageEntities() {
        when(imagesRepository.findAllByIdConceptAndIdThesaurus("C1", "TH1")).thenReturn(List.of(
                ImageExterne.builder().idConcept("C1").idThesaurus("TH1").externalUri("http://x").build()));

        var result = persistence.loadImages("C1", "TH1");

        assertEquals(1, result.size());
        assertEquals("http://x", result.get(0).getUri());
    }

    @Test
    void loadAlignmentSmallList_returnsEmptyWhenNoneFound() {
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C1", "TH1")).thenReturn(List.of());

        assertTrue(persistence.loadAlignmentSmallList("C1", "TH1").isEmpty());
    }

    @Test
    void addAlignment_shortCircuitsWhenAlignmentAlreadyExists() {
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 2, "http://x")).thenReturn(true);

        assertTrue(persistence.addAlignment(7, "C2", "TH2", "http://x", 2, "C1", "TH1", 0));
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void addAlignment_returnsFalseWhenTypeNotFound() {
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 2, "http://x")).thenReturn(false);
        when(alignementTypeRepository.findById(2)).thenReturn(Optional.empty());

        assertFalse(persistence.addAlignment(7, "C2", "TH2", "http://x", 2, "C1", "TH1", 0));
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void addAlignment_savesAlignmentWithoutSourceWhenSourceIdIsZero() {
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 2, "http://x")).thenReturn(false);
        when(alignementTypeRepository.findById(2)).thenReturn(Optional.of(new AlignementType()));

        assertTrue(persistence.addAlignment(7, "C2", "TH2", "http://x", 2, "C1", "TH1", 0));

        ArgumentCaptor<Alignement> captor = ArgumentCaptor.forClass(Alignement.class);
        verify(alignementRepository).save(captor.capture());
        assertEquals("C1", captor.getValue().getInternalIdConcept());
        assertEquals(7, captor.getValue().getAuthor());
        verify(alignementSourceRepository, never()).findById(any());
    }

    @Test
    void addAlignment_savesAlignmentWithResolvedSourceWhenSourceIdProvided() {
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 2, "http://x")).thenReturn(false);
        when(alignementTypeRepository.findById(2)).thenReturn(Optional.of(new AlignementType()));
        when(alignementSourceRepository.findById(5)).thenReturn(Optional.of(new fr.cnrs.opentheso.entites.AlignementSource()));

        assertTrue(persistence.addAlignment(7, "C2", "TH2", "http://x", 2, "C1", "TH1", 5));

        verify(alignementSourceRepository).findById(5);
        verify(alignementRepository).save(any(Alignement.class));
    }

    @Test
    void addSelectedTranslations_returnsFalseWhenPreferredTermMissing() {
        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1")).thenReturn(Optional.empty());

        assertFalse(persistence.addSelectedTranslations("TH1", "C1", 7, List.of(new SelectedResource())));
        verify(conceptTranslationWriteRepository, never()).insertTranslation(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void addSelectedTranslations_skipsUnselectedResources() {
        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(fr.cnrs.opentheso.entites.PreferredTerm.builder().idTerm("T1").build()));
        var resource = new SelectedResource();
        resource.setSelected(false);

        assertTrue(persistence.addSelectedTranslations("TH1", "C1", 7, List.of(resource)));
        verify(termRepository, never()).findByIdTermAndIdThesaurusAndLang(any(), any(), any());
    }

    @Test
    void addSelectedTranslations_updatesExistingTranslation() {
        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(fr.cnrs.opentheso.entites.PreferredTerm.builder().idTerm("T1").build()));
        var resource = new SelectedResource();
        resource.setIdLang("en");
        resource.setGettedValue("New value");
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "en"))
                .thenReturn(Optional.of(new fr.cnrs.opentheso.entites.Term()));

        assertTrue(persistence.addSelectedTranslations("TH1", "C1", 7, List.of(resource)));

        verify(conceptTranslationWriteRepository).updateTranslation(eq("T1"), eq("TH1"), eq("en"), anyString(), eq(7));
        verify(conceptTranslationWriteRepository, never()).insertTranslation(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void addSelectedTranslations_insertsNewTranslation() {
        when(preferredTermRepository.findByIdThesaurusAndIdConcept("TH1", "C1"))
                .thenReturn(Optional.of(fr.cnrs.opentheso.entites.PreferredTerm.builder().idTerm("T1").build()));
        var resource = new SelectedResource();
        resource.setIdLang("en");
        resource.setGettedValue("New value");
        when(termRepository.findByIdTermAndIdThesaurusAndLang("T1", "TH1", "en")).thenReturn(Optional.empty());

        assertTrue(persistence.addSelectedTranslations("TH1", "C1", 7, List.of(resource)));

        verify(conceptTranslationWriteRepository).insertTranslation(eq("T1"), eq("TH1"), eq("en"), anyString(), eq(7));
    }

    @Test
    void addSelectedDefinitions_skipsDuplicateNote() {
        var resource = new SelectedResource();
        resource.setIdLang("fr");
        resource.setGettedValue("Existing definition");
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLangAndLexicalValue(
                eq("C1"), eq("TH1"), eq("definition"), eq("fr"), anyString())).thenReturn(Optional.of(new Note()));

        assertTrue(persistence.addSelectedDefinitions("C1", "TH1", 7, "src", List.of(resource)));

        verify(noteRepository, never()).save(any());
    }

    @Test
    void addSelectedDefinitions_updatesExistingDefinition() {
        var resource = new SelectedResource();
        resource.setIdLang("fr");
        resource.setGettedValue("New definition");
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLangAndLexicalValue(
                any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        var existingNote = Note.builder().id(1).lexicalValue("Old").build();
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang("C1", "TH1", "definition", "fr"))
                .thenReturn(List.of(existingNote));

        assertTrue(persistence.addSelectedDefinitions("C1", "TH1", 7, "src", List.of(resource)));

        verify(noteRepository).save(existingNote);
        assertEquals("New definition", existingNote.getLexicalValue());
    }

    @Test
    void addSelectedDefinitions_createsNewDefinitionWhenNoneExists() {
        var resource = new SelectedResource();
        resource.setIdLang("fr");
        resource.setGettedValue("Brand new definition");
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLangAndLexicalValue(
                any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang("C1", "TH1", "definition", "fr"))
                .thenReturn(List.of());

        assertTrue(persistence.addSelectedDefinitions("C1", "TH1", 7, "src", List.of(resource)));

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertEquals("definition", captor.getValue().getNoteTypeCode());
        assertEquals("Brand new definition", captor.getValue().getLexicalValue());
    }

    @Test
    void addSelectedImages_savesSelectedImagesOnly() {
        var selected = new SelectedResource();
        selected.setSelected(true);
        selected.setGettedValue("http://img");
        var unselected = new SelectedResource();
        unselected.setSelected(false);
        when(imagesRepository.save(any())).thenReturn(new ImageExterne());

        assertTrue(persistence.addSelectedImages("C1", "TH1", 7, "name", "src", List.of(selected, unselected)));

        verify(imagesRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void insertGpsCoordinates_updatesExistingCoordinates() {
        when(gpsRepository.findByIdConceptAndIdThesoOrderByPosition("C1", "TH1")).thenReturn(List.of(new Gps()));
        when(gpsRepository.updateCoordinates("C1", "TH1", 1.0, 2.0)).thenReturn(1);

        assertTrue(persistence.insertGpsCoordinates("C1", "TH1", 1.0, 2.0));
        verify(gpsRepository, never()).save(any());
    }

    @Test
    void insertGpsCoordinates_createsNewCoordinatesAndTagsConcept() {
        when(gpsRepository.findByIdConceptAndIdThesoOrderByPosition("C1", "TH1")).thenReturn(List.of());
        when(gpsRepository.save(any(Gps.class))).thenReturn(new Gps());
        when(conceptRepository.setGpsTag(true, "C1", "TH1")).thenReturn(1);

        assertTrue(persistence.insertGpsCoordinates("C1", "TH1", 1.0, 2.0));
        verify(conceptRepository).setGpsTag(true, "C1", "TH1");
    }

    @Test
    void touchConcept_delegatesToRepository() {
        persistence.touchConcept("TH1", "C1", 7);

        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 7);
    }
}
