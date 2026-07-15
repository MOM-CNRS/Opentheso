package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatMutationPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatMutationServiceTest {

    @Mock
    private CandidatMutationPersistence candidatMutationPersistence;
    @Mock
    private ConceptDcTermRepository conceptDcTermRepository;

    private CandidatMutationService service;

    @BeforeEach
    void setUp() {
        service = new CandidatMutationService(candidatMutationPersistence, conceptDcTermRepository);
    }

    @Test
    void deleteConcept_delegatesToPersistence() {
        when(candidatMutationPersistence.deleteConcept("C1", "TH1")).thenReturn(true);

        assertTrue(service.deleteConcept("C1", "TH1"));
    }

    @Test
    void saveContributorMetadata_persistsCreatorMetadata() {
        service.saveContributorMetadata("C1", "TH1", "admin");

        ArgumentCaptor<ConceptDcTerm> captor = ArgumentCaptor.forClass(ConceptDcTerm.class);
        verify(conceptDcTermRepository).save(captor.capture());
        assertEquals(DCMIResource.CREATOR, captor.getValue().getName());
        assertEquals("admin", captor.getValue().getValue());
        assertEquals("C1", captor.getValue().getIdConcept());
    }

    @Test
    void resolveUserName_delegatesToPersistence() {
        when(candidatMutationPersistence.resolveUserName(7)).thenReturn("admin");

        assertEquals("admin", service.resolveUserName(7));
    }
}
