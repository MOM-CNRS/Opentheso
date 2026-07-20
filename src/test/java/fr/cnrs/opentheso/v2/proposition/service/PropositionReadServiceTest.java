package fr.cnrs.opentheso.v2.proposition.service;

import fr.cnrs.opentheso.models.PropositionProjection;
import fr.cnrs.opentheso.repositories.PropositionModificationRepository;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropositionReadServiceTest {

    @Mock
    private PropositionModificationRepository repository;

    private PropositionReadService service;

    @BeforeEach
    void setUp() {
        service = new PropositionReadService(repository);
    }

    private static PropositionProjection projection(int id, String status) {
        return new PropositionProjection() {
            @Override
            public Integer getId() {
                return id;
            }

            @Override
            public String getIdConcept() {
                return "C" + id;
            }

            @Override
            public String getLang() {
                return "fr";
            }

            @Override
            public String getIdTheso() {
                return "TH1";
            }

            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public String getDate() {
                return "01-01-2026 10:00";
            }

            @Override
            public String getNom() {
                return "Author";
            }

            @Override
            public String getEmail() {
                return "a@b.fr";
            }

            @Override
            public String getCommentaire() {
                return "Please improve";
            }

            @Override
            public String getApprouvePar() {
                return "admin";
            }

            @Override
            public Instant getApprouveDate() {
                return null;
            }

            @Override
            public String getAdminComment() {
                return "ok";
            }

            @Override
            public String getLexicalValue() {
                return "Concept " + id;
            }

            @Override
            public String getCodePays() {
                return "fr";
            }
        };
    }

    @Test
    void countPending_returnsZeroForBlankThesaurus() {
        assertEquals(0, service.countPending(" "));
    }

    @Test
    void countPending_delegatesToRepository() {
        when(repository.countByIdThesoAndStatus("TH1", "ENVOYER")).thenReturn(2L);

        assertEquals(2, service.countPending("TH1"));
    }

    @Test
    void listPending_mapsSummaries() {
        PropositionProjection pending = projection(1, "ENVOYER");
        when(repository.findAllPropositionsByStatusAndTheso("ENVOYER", "TH1"))
                .thenReturn(List.of(pending));

        var result = service.listPending("TH1");

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).conceptId());
        assertEquals("Concept 1", result.get(0).conceptLabel());
    }

    @Test
    void listAll_mapsSummaries() {
        PropositionProjection first = projection(1, "ENVOYER");
        PropositionProjection second = projection(2, "APPROUVER");
        when(repository.findAllPropositionsByTheso("TH1"))
                .thenReturn(List.of(first, second));

        var result = service.listAll("TH1");

        assertEquals(2, result.size());
    }

    @Test
    void listAll_returnsEmptyForBlankThesaurus() {
        assertTrue(service.listAll("").isEmpty());
    }

    @Test
    void findDetail_mapsDetail() {
        PropositionProjection detailProjection = projection(5, "LU");
        when(repository.findProjectionById(5)).thenReturn(detailProjection);

        PropositionDetail detail = service.findDetail(5);

        assertEquals(5, detail.id());
        assertEquals("Please improve", detail.comment());
        assertEquals("admin", detail.reviewedBy());
    }

    @Test
    void findDetail_returnsNullWhenMissing() {
        when(repository.findProjectionById(99)).thenReturn(null);

        assertNull(service.findDetail(99));
    }
}
