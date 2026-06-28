package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.entites.GraphView;
import fr.cnrs.opentheso.entites.GraphViewExportedConceptBranch;
import fr.cnrs.opentheso.repositories.GraphViewExportedConceptBranchRepository;
import fr.cnrs.opentheso.repositories.GraphViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphViewCommandServiceTest {

    @Mock
    private GraphViewRepository graphViewRepository;

    @Mock
    private GraphViewExportedConceptBranchRepository exportRepository;

    private GraphViewCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new GraphViewCommandService(graphViewRepository, exportRepository);
    }

    @Test
    void addExportEntry_returnsFalseWhenCombinationAlreadyExists() {
        when(exportRepository.findByGraphViewIdAndTopConceptIdAndTopConceptThesaurusId(4, "C1", "TH1"))
                .thenReturn(Optional.of(new GraphViewExportedConceptBranch()));

        assertFalse(commandService.addExportEntry(4, "TH1", "C1"));
    }

    @Test
    void addExportEntry_persistsNewCombination() {
        when(exportRepository.findByGraphViewIdAndTopConceptIdNullAndTopConceptThesaurusId(4, "TH1"))
                .thenReturn(Optional.empty());

        assertTrue(commandService.addExportEntry(4, "TH1", null));

        var captor = ArgumentCaptor.forClass(GraphViewExportedConceptBranch.class);
        verify(exportRepository).save(captor.capture());
        assertEquals("TH1", captor.getValue().getTopConceptThesaurusId());
    }

    @Test
    void addExportEntry_persistsBranchCombination() {
        when(exportRepository.findByGraphViewIdAndTopConceptIdAndTopConceptThesaurusId(4, "C1", "TH1"))
                .thenReturn(Optional.empty());

        assertTrue(commandService.addExportEntry(4, "TH1", "C1"));

        var captor = ArgumentCaptor.forClass(GraphViewExportedConceptBranch.class);
        verify(exportRepository).save(captor.capture());
        assertEquals("C1", captor.getValue().getTopConceptId());
    }

    @Test
    void createView_returnsPersistedId() {
        when(graphViewRepository.save(any(GraphView.class))).thenAnswer(invocation -> {
            GraphView view = invocation.getArgument(0);
            view.setId(42);
            return view;
        });

        assertEquals(42, commandService.createView("Vue", "Description", 7));
    }

    @Test
    void updateView_updatesFieldsAndSaves() {
        var graphView = new GraphView();
        when(graphViewRepository.getById(1)).thenReturn(graphView);

        commandService.updateView(1, "Nouveau nom", "Nouvelle description");

        assertEquals("Nouveau nom", graphView.getName());
        assertEquals("Nouvelle description", graphView.getDescription());
        verify(graphViewRepository).save(graphView);
    }

    @Test
    void deleteView_removesViewAndExports() {
        commandService.deleteView(3);

        verify(graphViewRepository).deleteById(3);
        verify(exportRepository).deleteAllByGraphViewId(3);
    }

    @Test
    void deleteView_acceptsStringId() {
        commandService.deleteView("8");

        verify(graphViewRepository).deleteById(8);
        verify(exportRepository).deleteAllByGraphViewId(8);
    }

    @Test
    void existsExportEntry_returnsTrueForThesaurusOnlyEntry() {
        when(exportRepository.findByGraphViewIdAndTopConceptIdNullAndTopConceptThesaurusId(2, "TH1"))
                .thenReturn(Optional.of(new GraphViewExportedConceptBranch()));

        assertTrue(commandService.existsExportEntry(2, "TH1", null));
    }

    @Test
    void existsExportEntry_returnsFalseWhenMissing() {
        when(exportRepository.findByGraphViewIdAndTopConceptIdAndTopConceptThesaurusId(2, "C1", "TH1"))
                .thenReturn(Optional.empty());

        assertFalse(commandService.existsExportEntry(2, "TH1", "C1"));
    }

    @Test
    void removeExportEntry_deletesThesaurusOnlyEntry() {
        commandService.removeExportEntry(1, "TH1", null);

        verify(exportRepository).deleteAllByGraphViewIdAndTopConceptThesaurusId(1, "TH1");
    }

    @Test
    void removeExportEntry_deletesBranchEntry() {
        commandService.removeExportEntry(1, "TH1", "C1");

        verify(exportRepository).deleteAllByGraphViewIdAndTopConceptIdAndTopConceptThesaurusId(1, "C1", "TH1");
    }
}
