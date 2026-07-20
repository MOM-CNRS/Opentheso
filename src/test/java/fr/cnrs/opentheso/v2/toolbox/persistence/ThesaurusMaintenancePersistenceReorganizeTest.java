package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusMaintenancePersistenceReorganizeTest {

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
    void reorganizingThesaurus_marksOrphanAsTopConcept() {
        Concept orphan = Concept.builder().idConcept("C1").idThesaurus("TH1").topConcept(false).status("").build();
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH1", "CA")).thenReturn(List.of(orphan));
        when(hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike("TH1", "C1", "BT%"))
                .thenReturn(List.of());
        when(hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept2AndRoleLike("TH1", "C1", "NT%"))
                .thenReturn(List.of());
        when(conceptRepository.findByIdConceptAndIdThesaurus("C1", "TH1")).thenReturn(Optional.of(orphan));

        assertTrue(persistence.reorganizingThesaurus("TH1"));

        verify(conceptRepository).setTopConceptTag(true, "C1", "TH1");
    }

    @Test
    void reorganizingThesaurus_addsMissingInverseForSpecializedRoles() {
        Concept child = Concept.builder().idConcept("C2").idThesaurus("TH1").topConcept(false).status("").build();
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH1", "CA")).thenReturn(List.of(child));
        when(hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike("TH1", "C2", "BT%"))
                .thenReturn(List.of());
        when(hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept2AndRoleLike("TH1", "C2", "NT%"))
                .thenReturn(List.of(HierarchicalRelationship.builder()
                        .idConcept1("C1")
                        .idConcept2("C2")
                        .idThesaurus("TH1")
                        .role("NTG")
                        .build()));
        when(hierarchicalRelationshipRepository.existsByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                "TH1", "C2", "C1", "BTG")).thenReturn(false);

        assertTrue(persistence.reorganizingThesaurus("TH1"));

        ArgumentCaptor<HierarchicalRelationship> captor = ArgumentCaptor.forClass(HierarchicalRelationship.class);
        verify(hierarchicalRelationshipRepository).save(captor.capture());
        HierarchicalRelationship saved = captor.getValue();
        assertEquals("C2", saved.getIdConcept1());
        assertEquals("C1", saved.getIdConcept2());
        assertEquals("BTG", saved.getRole());
    }

    @Test
    void reorganizingThesaurus_skipsExistingInverseRelation() {
        Concept child = Concept.builder().idConcept("C2").idThesaurus("TH1").topConcept(false).status("").build();
        when(conceptRepository.findAllByIdThesaurusAndStatusNot("TH1", "CA")).thenReturn(List.of(child));
        when(hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike("TH1", "C2", "BT%"))
                .thenReturn(List.of(HierarchicalRelationship.builder()
                        .idConcept1("C2")
                        .idConcept2("C1")
                        .idThesaurus("TH1")
                        .role("BT")
                        .build()));
        when(hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept2AndRoleLike("TH1", "C2", "NT%"))
                .thenReturn(List.of(HierarchicalRelationship.builder()
                        .idConcept1("C1")
                        .idConcept2("C2")
                        .idThesaurus("TH1")
                        .role("NT")
                        .build()));

        assertTrue(persistence.reorganizingThesaurus("TH1"));

        verify(hierarchicalRelationshipRepository, never()).save(any());
        verify(conceptRepository, never()).setTopConceptTag(eq(true), eq("C2"), eq("TH1"));
    }
}
