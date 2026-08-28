package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.AlignementType;
import fr.cnrs.opentheso.models.NodeAlignmentProjection;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateAlignmentCommand;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptAlignmentMutationServiceTest {

    @Mock
    private AlignementRepository alignementRepository;
    @Mock
    private AlignementTypeRepository alignementTypeRepository;
    @Mock
    private ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private ImagesRepository imagesRepository;
    @Mock
    private GpsRepository gpsRepository;

    @InjectMocks
    private ConceptAlignmentMutationService service;

    private final AlignementType exactMatchType = new AlignementType(1, "Equivalence exacte", "fr", "skos:exactMatch");

    @Test
    void listAlignmentTypes_mapsAndSortsByLabel() {
        var close = new AlignementType(2, "Close", "fr", "skos:closeMatch");
        when(alignementTypeRepository.findAll()).thenReturn(List.of(close, exactMatchType));

        var result = service.listAlignmentTypes();

        assertEquals(2, result.size());
        assertEquals("Close", result.get(0).getLabel());
        assertEquals("Equivalence exacte", result.get(1).getLabel());
    }

    @Test
    void addManualAlignment_blankUri_returnsValidationError() {
        var command = new AddManualAlignmentCommand("TH1", "C1", 1, "  ", "Wikidata", 42, "admin");

        var result = service.addManualAlignment(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void addManualAlignment_invalidUrl_returnsValidationError() {
        var command = new AddManualAlignmentCommand("TH1", "C1", 1, "not-a-url", "Wikidata", 42, "admin");

        var result = service.addManualAlignment(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        assertEquals("L'URL n'est pas valide !", result.message());
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void addManualAlignment_unknownType_returnsValidationError() {
        var command = new AddManualAlignmentCommand("TH1", "C1", 99, "http://example.org/x", "Wikidata", 42, "admin");
        when(alignementTypeRepository.findById(99)).thenReturn(Optional.empty());

        var result = service.addManualAlignment(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void addManualAlignment_duplicateSameTypeAndUri_returnsDuplicate() {
        var command = new AddManualAlignmentCommand("TH1", "C1", 1, "http://example.org/x", "Wikidata", 42, "admin");
        when(alignementTypeRepository.findById(1)).thenReturn(Optional.of(exactMatchType));
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 1, "http://example.org/x"))
                .thenReturn(true);

        var result = service.addManualAlignment(command);

        assertEquals(MutationOutcome.DUPLICATE_LABEL, result.outcome());
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void addManualAlignment_uriUsedByAnotherType_returnsDuplicate() {
        var command = new AddManualAlignmentCommand("TH1", "C1", 1, "http://example.org/x", "Wikidata", 42, "admin");
        when(alignementTypeRepository.findById(1)).thenReturn(Optional.of(exactMatchType));
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 1, "http://example.org/x"))
                .thenReturn(false);
        when(alignementRepository.existsByInternalIdThesaurusAndInternalIdConceptAndUriTarget(
                "TH1", "C1", "http://example.org/x")).thenReturn(true);

        var result = service.addManualAlignment(command);

        assertEquals(MutationOutcome.DUPLICATE_LABEL, result.outcome());
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void addManualAlignment_success_savesEntityAndTouchesConcept() {
        var command = new AddManualAlignmentCommand("TH1", "C1", 1, " http://example.org/x ", "Wikidata", 42, "admin");
        when(alignementTypeRepository.findById(1)).thenReturn(Optional.of(exactMatchType));
        when(alignementRepository.existsByConceptThesaurusTypeAndUri("TH1", "C1", 1, "http://example.org/x"))
                .thenReturn(false);
        when(alignementRepository.existsByInternalIdThesaurusAndInternalIdConceptAndUriTarget(
                "TH1", "C1", "http://example.org/x")).thenReturn(false);

        var result = service.addManualAlignment(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertFalse(result.warning());
        ArgumentCaptor<Alignement> captor = ArgumentCaptor.forClass(Alignement.class);
        verify(alignementRepository).save(captor.capture());
        Alignement saved = captor.getValue();
        assertEquals("http://example.org/x", saved.getUriTarget());
        assertEquals("Wikidata", saved.getThesaurusTarget());
        assertEquals("C1", saved.getInternalIdConcept());
        assertEquals("TH1", saved.getInternalIdThesaurus());
        assertTrue(saved.getUrlAvailable());
        assertEquals(exactMatchType, saved.getAlignementType());
        assertEquals(42, saved.getAuthor());
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 42);
        verify(conceptWritePostMutationRepository).saveContributorDcTerm("TH1", "C1", "admin");
    }

    @Test
    void updateAlignment_notFound_returnsValidationError() {
        var command = new UpdateAlignmentCommand("TH1", "C1", 7, 1, "http://example.org/y", "Wikidata", 42, "admin");
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.empty());

        var result = service.updateAlignment(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void updateAlignment_sameUri_skipsDuplicateCheckAgainstSelf() {
        var command = new UpdateAlignmentCommand("TH1", "C1", 7, 2, "http://example.org/old", "Getty", 42, "admin");
        var closeType = new AlignementType(2, "Close", "fr", "skos:closeMatch");
        var existing = Alignement.builder()
                .id(7)
                .internalIdThesaurus("TH1")
                .internalIdConcept("C1")
                .uriTarget("http://example.org/old")
                .thesaurusTarget("Old source")
                .urlAvailable(true)
                .build();
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.of(existing));
        when(alignementTypeRepository.findById(2)).thenReturn(Optional.of(closeType));

        var result = service.updateAlignment(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("http://example.org/old", existing.getUriTarget());
        assertEquals("Getty", existing.getThesaurusTarget());
        assertEquals(closeType, existing.getAlignementType());
        assertTrue(existing.getUrlAvailable());
        verify(alignementRepository, never()).existsByInternalIdThesaurusAndInternalIdConceptAndUriTarget(
                any(), any(), any());
        verify(alignementRepository).save(existing);
    }

    @Test
    void updateAlignment_uriAlreadyUsed_returnsDuplicate() {
        var command = new UpdateAlignmentCommand("TH1", "C1", 7, 1, "http://example.org/taken", "Getty", 42, "admin");
        var existing = Alignement.builder()
                .id(7)
                .internalIdThesaurus("TH1")
                .internalIdConcept("C1")
                .uriTarget("http://example.org/old")
                .thesaurusTarget("Old source")
                .urlAvailable(true)
                .build();
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.of(existing));
        when(alignementTypeRepository.findById(1)).thenReturn(Optional.of(exactMatchType));
        when(alignementRepository.existsByInternalIdThesaurusAndInternalIdConceptAndUriTarget(
                "TH1", "C1", "http://example.org/taken")).thenReturn(true);

        var result = service.updateAlignment(command);

        assertEquals(MutationOutcome.DUPLICATE_LABEL, result.outcome());
        verify(alignementRepository, never()).save(any());
    }

    @Test
    void updateAlignment_success_updatesFields() {
        var command = new UpdateAlignmentCommand("TH1", "C1", 7, 1, "http://example.org/y", "Getty", 42, "admin");
        var existing = Alignement.builder()
                .id(7)
                .internalIdThesaurus("TH1")
                .internalIdConcept("C1")
                .uriTarget("http://example.org/old")
                .thesaurusTarget("Old source")
                .urlAvailable(true)
                .build();
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.of(existing));
        when(alignementTypeRepository.findById(1)).thenReturn(Optional.of(exactMatchType));
        when(alignementRepository.existsByInternalIdThesaurusAndInternalIdConceptAndUriTarget(
                "TH1", "C1", "http://example.org/y")).thenReturn(false);

        var result = service.updateAlignment(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("http://example.org/y", existing.getUriTarget());
        assertEquals("Getty", existing.getThesaurusTarget());
        assertEquals(exactMatchType, existing.getAlignementType());
        assertTrue(existing.getUrlAvailable());
        verify(alignementRepository).save(existing);
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 42);
    }

    @Test
    void deleteAlignment_notFound_returnsValidationError() {
        var command = new DeleteAlignmentCommand("TH1", "C1", 7, 42, "admin");
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.empty());

        var result = service.deleteAlignment(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(alignementRepository, never()).delete(any());
    }

    @Test
    void deleteAlignment_success_deletesAndTouchesConcept() {
        var command = new DeleteAlignmentCommand("TH1", "C1", 7, 42, "admin");
        var existing = Alignement.builder()
                .id(7)
                .internalIdThesaurus("TH1")
                .internalIdConcept("C1")
                .thesaurusTarget("Wikidata")
                .build();
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.of(existing));
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C1", "TH1"))
                .thenReturn(List.of());

        var result = service.deleteAlignment(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(alignementRepository).delete(existing);
        verify(noteRepository).deleteByConceptThesaurusAndNoteSource("C1", "TH1", "Wikidata");
        verify(imagesRepository).deleteByIdThesaurusAndIdConceptAndImageCopyrightIgnoreCase(
                "TH1", "C1", "Wikidata");
        verify(gpsRepository, never()).deleteByIdConceptAndIdTheso(any(), any());
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 42);
    }

    @Test
    void deleteAlignment_keepsRelatedWhenAnotherAlignmentSharesSource() {
        var command = new DeleteAlignmentCommand("TH1", "C1", 7, 42, "admin");
        var existing = Alignement.builder()
                .id(7)
                .internalIdThesaurus("TH1")
                .internalIdConcept("C1")
                .thesaurusTarget("Wikidata")
                .build();
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.of(existing));
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C1", "TH1"))
                .thenReturn(List.of(remaining("Wikidata")));

        var result = service.deleteAlignment(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(alignementRepository).delete(existing);
        verify(noteRepository, never()).deleteByConceptThesaurusAndNoteSource(any(), any(), any());
        verify(imagesRepository, never()).deleteByIdThesaurusAndIdConceptAndImageCopyrightIgnoreCase(
                any(), any(), any());
    }

    @Test
    void deleteAlignment_geoNames_removesGpsWhenLastOfSource() {
        var command = new DeleteAlignmentCommand("TH1", "C1", 7, 42, "admin");
        var existing = Alignement.builder()
                .id(7)
                .internalIdThesaurus("TH1")
                .internalIdConcept("C1")
                .thesaurusTarget("GeoNames")
                .build();
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.of(existing));
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C1", "TH1"))
                .thenReturn(List.of());

        service.deleteAlignment(command);

        verify(gpsRepository).deleteByIdConceptAndIdTheso("C1", "TH1");
    }

    private static NodeAlignmentProjection remaining(String source) {
        return new NodeAlignmentProjection() {
            @Override public int getId() { return 8; }
            @Override public java.util.Date getCreated() { return null; }
            @Override public java.util.Date getModified() { return null; }
            @Override public int getAuthor() { return 1; }
            @Override public String getThesaurus_target() { return source; }
            @Override public String getConcept_target() { return ""; }
            @Override public String getUri_target() { return "http://ex.org/b"; }
            @Override public int getAlignement_id_type() { return 1; }
            @Override public String getInternal_id_thesaurus() { return "TH1"; }
            @Override public String getInternal_id_concept() { return "C1"; }
            @Override public Integer getId_alignement_source() { return null; }
            @Override public String getLabel() { return "exactMatch"; }
            @Override public String getLabel_skos() { return "skos:exactMatch"; }
            @Override public boolean getUrl_available() { return true; }
        };
    }
}
