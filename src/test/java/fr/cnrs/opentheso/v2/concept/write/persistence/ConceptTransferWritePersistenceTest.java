package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.concept.identifier.ConceptArkWriteService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptTransferWritePersistenceTest {

    @Mock
    private ConceptTransferWriteRepository conceptTransferWriteRepository;
    @Mock
    private ConceptRelationWriteRepository conceptRelationWriteRepository;
    @Mock
    private ConceptLifecycleWriteRepository conceptLifecycleWriteRepository;
    @Mock
    private ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    @Mock
    private ConceptGroupConceptRepository conceptGroupConceptRepository;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private PreferencesRepository preferencesRepository;
    @Mock
    private ConceptArkWriteService conceptArkWriteService;
    @Mock
    private EditionQueryRepository editionQueryRepository;

    @InjectMocks
    private ConceptTransferWritePersistence support;

    @Test
    void moveConceptToThesaurus_rejectsWhenConceptAlreadyExistsInTarget() {
        when(conceptRepository.existsByIdConceptAndIdThesaurus("1893420", "th33")).thenReturn(true);
        var command = new MoveConceptToThesaurusCommand(
                "th1", "th33", "1893420", List.of("1893420"), "fr", 1, "admin", null);

        var result = support.moveConceptToThesaurus(command);

        assertEquals(MutationOutcome.DUPLICATE_LABEL, result.outcome());
        assertTrue(result.message().contains("1893420"));
        verify(conceptTransferWriteRepository, never())
                .moveConceptToAnotherThesaurus(anyString(), anyString(), anyString());
    }

    @Test
    void moveConceptToThesaurus_rejectsSameSourceAndTarget() {
        var command = new MoveConceptToThesaurusCommand(
                "th33", "th33", "1893420", List.of("1893420"), "fr", 1, "admin", null);

        var result = support.moveConceptToThesaurus(command);

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(conceptTransferWriteRepository, never())
                .moveConceptToAnotherThesaurus(anyString(), anyString(), anyString());
        verify(conceptRepository, never()).existsByIdConceptAndIdThesaurus(any(), any());
    }
}
