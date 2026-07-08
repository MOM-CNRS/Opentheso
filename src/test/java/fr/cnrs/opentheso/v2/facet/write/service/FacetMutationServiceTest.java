package fr.cnrs.opentheso.v2.facet.write.service;

import fr.cnrs.opentheso.entites.ConceptFacet;
import fr.cnrs.opentheso.entites.NodeLabel;
import fr.cnrs.opentheso.entites.ThesaurusArray;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.NodeLabelRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.CreateFacetCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RenameFacetLabelCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetMutationServiceTest {

    @Mock
    private ThesaurusArrayRepository thesaurusArrayRepository;
    @Mock
    private ConceptFacetRepository conceptFacetRepository;
    @Mock
    private NodeLabelRepository nodeLabelRepository;
    @Mock
    private BranchConceptSupport branchConceptSupport;

    @InjectMocks
    private FacetMutationService service;

    @Test
    void renamePreferredLabel_rejectsBlankLabel() {
        var result = service.renamePreferredLabel(new RenameFacetLabelCommand("TH1", "F1", "fr", "  "));

        assertEquals(MutationOutcome.VALIDATION_ERROR, result.outcome());
        verify(nodeLabelRepository, never()).save(any());
    }

    @Test
    void renamePreferredLabel_updatesLabel() {
        var label = NodeLabel.builder().idFacet("F1").idThesaurus("TH1").lang("fr").lexicalValue("Old").build();
        when(nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang("F1", "TH1", "fr"))
                .thenReturn(Optional.of(label));

        var result = service.renamePreferredLabel(new RenameFacetLabelCommand("TH1", "F1", "fr", "New"));

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("New", label.getLexicalValue());
        verify(nodeLabelRepository).save(label);
    }

    @Test
    void addMember_addsBranchConcepts() {
        when(branchConceptSupport.collectBranchConceptIds("TH1", "C1")).thenReturn(List.of("C1", "C2"));

        var result = service.addMember(new AddFacetMemberCommand("TH1", "F1", "C1", true));

        assertEquals(MutationOutcome.OK, result.outcome());
        assertTrue(result.message().contains("branche"));
        var captor = ArgumentCaptor.forClass(ConceptFacet.class);
        verify(conceptFacetRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals("F1", captor.getAllValues().get(0).getIdFacet());
    }

    @Test
    void removeMember_removesSingleConcept() {
        var result = service.removeMember(new RemoveFacetMemberCommand("TH1", "F1", "C1", false));

        assertEquals(MutationOutcome.OK, result.outcome());
        verify(conceptFacetRepository).deleteAllByIdConceptAndIdThesaurusAndIdFacet("C1", "TH1", "F1");
    }

    @Test
    void addTranslation_rejectsDuplicateLanguage() {
        when(nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang("F1", "TH1", "en"))
                .thenReturn(Optional.of(NodeLabel.builder().build()));

        var result = service.addTranslation(new AddFacetTranslationCommand("TH1", "F1", "en", "Facet"));

        assertEquals(MutationOutcome.DUPLICATE_LABEL, result.outcome());
        verify(nodeLabelRepository, never()).save(any());
    }

    @Test
    void createFacet_generatesFacetId() {
        when(thesaurusArrayRepository.getNextFacetSequenceId()).thenReturn(7L);
        when(conceptFacetRepository.findByIdFacet("F7")).thenReturn(Optional.empty());

        var result = service.createFacet(new CreateFacetCommand("TH1", "C1", "fr", "Facet"));

        assertEquals(MutationOutcome.OK, result.outcome());
        assertEquals("F7", result.createdConceptId());
        verify(nodeLabelRepository).save(any(NodeLabel.class));
        verify(thesaurusArrayRepository).save(any(ThesaurusArray.class));
    }
}
