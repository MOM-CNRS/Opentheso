package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusMaintenancePersistenceCollectionsTest {

    @Mock private ThesaurusRepository thesaurusRepository;
    @Mock private ConceptRepository conceptRepository;
    @Mock private HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    @Mock private ConceptGroupRepository conceptGroupRepository;
    @Mock private ConceptGroupConceptRepository conceptGroupConceptRepository;
    @Mock private ConceptGroupLabelRepository conceptGroupLabelRepository;
    @Mock private PreferredTermRepository preferredTermRepository;
    @Mock private TermRepository termRepository;
    @Mock private ToolboxPreferencePersistence toolboxPreferencePersistence;

    private ThesaurusMaintenancePersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new ThesaurusMaintenancePersistence(
                thesaurusRepository,
                conceptRepository,
                hierarchicalRelationshipRepository,
                conceptGroupRepository,
                conceptGroupConceptRepository,
                conceptGroupLabelRepository,
                preferredTermRepository,
                termRepository,
                toolboxPreferencePersistence
        );
    }

    @Test
    void reorganizeConceptsAndCollections_cleansEmptyMissingGroupAndMissingConceptLinks() {
        when(conceptGroupConceptRepository.findByIdGroupAndIdThesaurus("", "TH1"))
                .thenReturn(List.of(ConceptGroupConcept.builder()
                        .idGroup("")
                        .idThesaurus("TH1")
                        .idConcept("C0")
                        .build()));
        when(conceptGroupConceptRepository.findGroupIdsMissingFromConceptGroup("TH1"))
                .thenReturn(List.of("G-GONE"));
        when(conceptGroupConceptRepository.findByIdGroupAndIdThesaurus("G-GONE", "TH1"))
                .thenReturn(List.of(
                        ConceptGroupConcept.builder().idGroup("G-GONE").idThesaurus("TH1").idConcept("C1").build(),
                        ConceptGroupConcept.builder().idGroup("G-GONE").idThesaurus("TH1").idConcept("C2").build()
                ));
        when(conceptGroupConceptRepository.findGroupConceptLinksWithMissingConcepts("TH1"))
                .thenReturn(List.<Object[]>of(new Object[]{"G1", "C-MISSING"}));

        int cleaned = persistence.reorganizeConceptsAndCollections("TH1");

        assertEquals(4, cleaned); // 1 empty + 2 missing group + 1 missing concept
        verify(conceptGroupConceptRepository).deleteAllByIdThesaurusAndIdGroup("TH1", "");
        verify(conceptGroupConceptRepository).deleteAllByIdGroupAndIdThesaurus("G-GONE", "TH1");
        verify(conceptGroupConceptRepository).deleteByIdGroupAndIdConceptAndIdThesaurus("G1", "C-MISSING", "TH1");
    }
}
