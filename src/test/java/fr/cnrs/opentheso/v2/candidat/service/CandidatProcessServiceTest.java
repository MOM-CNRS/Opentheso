package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.v2.candidat.session.CandidatProcessLegacySupport;
import fr.cnrs.opentheso.v2.shared.session.ConceptTreeRefreshSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatProcessServiceTest {

    @Mock
    private CandidatProcessLegacySupport legacySupport;
    @Mock
    private CandidatReadService candidatReadService;
    @Mock
    private ConceptDcTermRepository conceptDcTermRepository;
    @Mock
    private ConceptTreeRefreshSupport conceptTreeRefreshSupport;

    private CandidatProcessService service;

    @BeforeEach
    void setUp() {
        service = new CandidatProcessService(
                legacySupport,
                candidatReadService,
                conceptDcTermRepository,
                conceptTreeRefreshSupport
        );
    }

    @Test
    void exportProcessedCandidatesCsv_delegatesToLegacySupport() {
        var candidates = List.of(new CandidatDto());
        when(legacySupport.exportProcessedCandidatesCsv(candidates)).thenReturn(new byte[]{1, 2});

        assertEquals(2, service.exportProcessedCandidatesCsv(candidates).length);
    }

    @Test
    void afterCandidateAccepted_updatesMetadataAndRefreshesTree() {
        var candidate = new CandidatDto();
        candidate.setIdThesaurus("TH1");
        candidate.setIdConcepte("C1");
        var preferences = Preferences.builder().build();

        service.afterCandidateAccepted(candidate, 7, "admin", preferences);

        verify(legacySupport).updateConceptDate("TH1", "C1", 7);
        verify(legacySupport).generatePersistentIds(preferences, candidate);
        verify(conceptTreeRefreshSupport).refreshConceptTree();
        ArgumentCaptor<ConceptDcTerm> captor = ArgumentCaptor.forClass(ConceptDcTerm.class);
        verify(conceptDcTermRepository).save(captor.capture());
        assertEquals(DCMIResource.CONTRIBUTOR, captor.getValue().getName());
        assertEquals("admin", captor.getValue().getValue());
    }

    @Test
    void prepareCandidatesForAccept_delegatesToReadService() {
        var candidates = List.of(new CandidatDto());

        service.prepareCandidatesForAccept(candidates, "TH1", "fr");

        verify(candidatReadService).prepareCandidatesForAccept(candidates, "TH1", "fr");
    }

    @Test
    void sendMail_delegatesToLegacySupport() {
        when(legacySupport.sendMail("a@b.c", "subject", "body")).thenReturn(true);

        assertTrue(service.sendMail("a@b.c", "subject", "body"));
    }
}
