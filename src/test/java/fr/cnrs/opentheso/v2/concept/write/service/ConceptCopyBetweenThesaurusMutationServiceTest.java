package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.CopyBranchBetweenThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptCopyBetweenThesaurusWritePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptCopyBetweenThesaurusMutationServiceTest {

    @Mock
    private ConceptCopyBetweenThesaurusWritePersistence persistence;

    private ConceptCopyBetweenThesaurusMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptCopyBetweenThesaurusMutationService(persistence);
    }

    @Test
    void validateAndCopy_delegateToPersistence() {
        var command = mock(CopyBranchBetweenThesaurusCommand.class);
        when(persistence.validateIdsAvailable("TH2", List.of("C1"))).thenReturn(MutationResult.ok("ok"));
        when(persistence.copyBranch(command)).thenReturn(MutationResult.ok("copied"));

        assertTrue(service.validateIdsAvailable("TH2", List.of("C1")).success());
        assertTrue(service.copyBranch(command).success());
    }
}
