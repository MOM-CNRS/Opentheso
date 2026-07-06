package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.entites.AlignementType;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
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

        var result = service.updateAlignment(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("http://example.org/y", existing.getUriTarget());
        assertEquals("Getty", existing.getThesaurusTarget());
        assertEquals(exactMatchType, existing.getAlignementType());
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
        var existing = Alignement.builder().id(7).internalIdThesaurus("TH1").internalIdConcept("C1").build();
        when(alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId("TH1", "C1", 7))
                .thenReturn(Optional.of(existing));

        var result = service.deleteAlignment(command);

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(alignementRepository).delete(existing);
        verify(conceptWritePostMutationRepository).touchConcept("TH1", "C1", 42);
    }
}
