package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptTransferWritePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptTransferMutationServiceTest {

    @Mock
    private ConceptTransferWritePersistence persistence;

    private ConceptTransferMutationService service;

    @BeforeEach
    void setUp() {
        service = new ConceptTransferMutationService(persistence);
    }

    @Test
    void moveConceptToThesaurus_delegatesToPersistence() {
        var command = mock(MoveConceptToThesaurusCommand.class);
        when(persistence.moveConceptToThesaurus(command)).thenReturn(MutationResult.ok("moved"));

        assertTrue(service.moveConceptToThesaurus(command).success());
    }

    @Test
    void listAdminThesauri_delegatesToPersistence() {
        var options = List.of(new ConceptWriteThesaurusOption("TH2", "Autre"));
        when(persistence.listAdminThesauri(7, true, "TH1", "fr")).thenReturn(options);

        assertEquals(options, service.listAdminThesauri(7, true, "TH1", "fr"));
    }
}
