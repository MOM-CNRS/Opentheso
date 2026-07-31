package fr.cnrs.opentheso.v2.concept.alignment.service;

import fr.cnrs.opentheso.models.NodeAlignmentProjection;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementSourceRepository;
import fr.cnrs.opentheso.repositories.ThesaurusAlignementSourceRepository;
import fr.cnrs.opentheso.v2.candidat.alignment.AlignmentAutoExternalSearch;
import fr.cnrs.opentheso.v2.candidat.alignment.persistence.CandidatAutoAlignmentPersistence;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentAdminRow;
import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptAlignmentAdminServiceTest {

    @Mock
    private BranchConceptSupport branchConceptSupport;
    @Mock
    private ConceptSearchQueryRepository conceptSearchQueryRepository;
    @Mock
    private AlignementRepository alignementRepository;
    @Mock
    private AlignementSourceRepository alignementSourceRepository;
    @Mock
    private ThesaurusAlignementSourceRepository thesaurusAlignementSourceRepository;
    @Mock
    private AlignmentAutoExternalSearch alignmentAutoExternalSearch;
    @Mock
    private ConceptAlignmentMutationService conceptAlignmentMutationService;
    @Mock
    private CandidatAutoAlignmentPersistence candidatAutoAlignmentPersistence;
    @Mock
    private AlignmentPropositionEnricher alignmentPropositionEnricher;

    private ConceptAlignmentAdminService service;

    @BeforeEach
    void setUp() {
        service = new ConceptAlignmentAdminService(
                branchConceptSupport,
                conceptSearchQueryRepository,
                alignementRepository,
                alignementSourceRepository,
                thesaurusAlignementSourceRepository,
                alignmentAutoExternalSearch,
                conceptAlignmentMutationService,
                candidatAutoAlignmentPersistence,
                alignmentPropositionEnricher
        );
    }

    @Test
    void loadBranchSummary_includesPlaceholderAndAlignments() {
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C1")).thenReturn(List.of("C1", "C2"));
        when(conceptSearchQueryRepository.findPreferredLabelsByIds(List.of("C1", "C2"), "TH1", "fr"))
                .thenReturn(Map.of("C1", "Racine", "C2", "Enfant"));
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C1", "TH1"))
                .thenReturn(List.of(projection(10, "C1", "http://ex.org/a", "exactMatch", "Wikidata", true)));
        when(alignementRepository.findAllAlignmentsByConceptAndThesaurus("C2", "TH1"))
                .thenReturn(List.of());

        List<AlignmentAdminRow> rows = service.loadBranchSummary("TH1", "C1", "fr");

        assertEquals(3, rows.size()); // C1 alignment + C1 placeholder + C2 placeholder
        assertEquals(1, service.countAlignments(rows));
        assertTrue(rows.stream().anyMatch(AlignmentAdminRow::isPlaceholder));
    }

    private NodeAlignmentProjection projection(
            int id, String conceptId, String uri, String label, String source, boolean available
    ) {
        return new NodeAlignmentProjection() {
            @Override public int getId() { return id; }
            @Override public Date getCreated() { return null; }
            @Override public Date getModified() { return null; }
            @Override public int getAuthor() { return 1; }
            @Override public String getThesaurus_target() { return source; }
            @Override public String getConcept_target() { return ""; }
            @Override public String getUri_target() { return uri; }
            @Override public int getAlignement_id_type() { return 1; }
            @Override public String getInternal_id_thesaurus() { return "TH1"; }
            @Override public String getInternal_id_concept() { return conceptId; }
            @Override public Integer getId_alignement_source() { return null; }
            @Override public String getLabel() { return label; }
            @Override public String getLabel_skos() { return "skos:exactMatch"; }
            @Override public boolean getUrl_available() { return available; }
        };
    }
}
