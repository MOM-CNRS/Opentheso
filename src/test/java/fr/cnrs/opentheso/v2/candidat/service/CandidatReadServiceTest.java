package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.v2.candidat.mapper.CandidatDetailJsonParser;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatReadPersistence;
import fr.cnrs.opentheso.v2.shared.repository.CandidatQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatConceptRelationRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatDetailBundle;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatListRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatReadServiceTest {

    @Mock
    private CandidatQueryRepository candidatQueryRepository;

    @Mock
    private CandidatReadPersistence candidatReadPersistence;

    private CandidatReadService service;

    @BeforeEach
    void setUp() {
        service = new CandidatReadService(candidatQueryRepository, candidatReadPersistence);
    }

    @Test
    void loadByStatus_returnsEmptyListWhenThesaurusMissing() {
        assertTrue(service.loadByStatus("", "fr", 1).isEmpty());
        verifyNoInteractions(candidatQueryRepository);
    }

    @Test
    void searchByStatus_usesRepositorySearchWhenTermProvided() {
        when(candidatQueryRepository.findCandidatesByStatus("TH1", "fr", 1, "chien"))
                .thenReturn(List.of(new CandidatListRow(
                        "C1", null, null, 4, null, "", "chien", "user", "Utilisateur inconnu",
                        0, 0, 0, 0
                )));

        var result = service.searchByStatus("TH1", "fr", 1, "chien");

        assertEquals(1, result.size());
        assertEquals("chien", result.get(0).getNomPref());
        verify(candidatQueryRepository).findCandidatesByStatus("TH1", "fr", 1, "chien");
    }

    @Test
    void loadDetails_usesSingleDetailBundleQuery() {
        var candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setLang("fr");
        candidat.setUserId(5);

        var parsed = CandidatDetailJsonParser.parse(
                "[{\"id\":\"G1\",\"value\":\"Domaine\"}]",
                "[{\"id\":\"BT1\",\"value\":\"Parent\"}]",
                "[]",
                "[\"syn\"]",
                "[]",
                "[]",
                "[]",
                "[]"
        );
        when(candidatQueryRepository.findCandidateDetailBundle("TH1", "C1", "fr", 5))
                .thenReturn(Optional.of(new CandidatDetailBundle("T1", true, parsed)));
        when(candidatReadPersistence.loadAlignments("C1", "TH1")).thenReturn(List.of());
        when(candidatReadPersistence.loadExternalImages("TH1", "C1")).thenReturn(List.of());

        service.loadDetails(candidat, "TH1");

        assertEquals("T1", candidat.getIdTerm());
        assertTrue(candidat.isVoted());
        assertEquals("G1", candidat.getCollections().get(0).getId());
        assertEquals("BT1", candidat.getTermesGenerique().get(0).getId());
        assertEquals(List.of("syn"), candidat.getEmployePourList());
        verify(candidatQueryRepository).findCandidateDetailBundle("TH1", "C1", "fr", 5);
    }

    @Test
    void prepareCandidatesForAccept_loadsBroaderTermsInBulk() {
        var first = new CandidatDto();
        first.setIdConcepte("C1");
        var second = new CandidatDto();
        second.setIdConcepte("C2");
        when(candidatQueryRepository.findBroaderRelationsForConcepts(eq("TH1"), anyList(), eq("fr")))
                .thenReturn(List.of(
                        new CandidatConceptRelationRow("C1", "BT1", "Broader 1"),
                        new CandidatConceptRelationRow("C2", "BT2", "Broader 2")
                ));

        service.prepareCandidatesForAccept(List.of(first, second), "TH1", "fr");

        assertEquals(1, first.getTermesGenerique().size());
        assertEquals("BT1", first.getTermesGenerique().get(0).getId());
        assertEquals(1, second.getTermesGenerique().size());
        assertFalse(second.getTermesGenerique().isEmpty());
    }
}
