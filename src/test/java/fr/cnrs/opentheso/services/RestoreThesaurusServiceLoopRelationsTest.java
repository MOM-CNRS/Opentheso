package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.models.relations.NodeRelation;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestoreThesaurusServiceLoopRelationsTest {

    @Mock
    private GroupService groupService;
    @Mock
    private ArkService arkService;
    @Mock
    private ConceptService conceptService;
    @Mock
    private RelationService relationService;
    @Mock
    private ThesaurusService thesaurusService;
    @Mock
    private PreferenceService preferenceService;
    @Mock
    private TermService termService;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ThesaurusRepository thesaurusRepository;

    private RestoreThesaurusService service;

    @BeforeEach
    void setUp() {
        service = new RestoreThesaurusService(
                groupService,
                arkService,
                conceptService,
                relationService,
                thesaurusService,
                preferenceService,
                termService,
                conceptRepository,
                thesaurusRepository
        );
    }

    @Test
    void preview_countsUniquePairsWhenBothSidesAreInTheBranch() {
        when(conceptService.getIdsOfBranchWithoutLoop("C1", "TH1")).thenReturn(List.of("C1", "C2", "C3"));
        NodeRelation loop = loop("C2", "C1");
        when(relationService.getLoopRelation("TH1", "C1")).thenReturn(loop);
        when(relationService.getLoopRelation("TH1", "C2")).thenReturn(loop);
        when(relationService.getLoopRelation("TH1", "C3")).thenReturn(null);

        var preview = service.previewLoopRelations("TH1", "C1");

        assertEquals(3, preview.branchSize());
        assertEquals(1, preview.loopCount());
    }

    @Test
    void delete_removesBtAndNtAndReturnsHowManyTimesALoopWasFound() {
        when(conceptService.getIdsOfBranchWithoutLoop("C1", "TH1")).thenReturn(List.of("C1", "C3"));
        NodeRelation loop = loop("C2", "C1");
        when(relationService.getLoopRelation("TH1", "C1")).thenReturn(loop);
        when(relationService.getLoopRelation("TH1", "C3")).thenReturn(null);

        int deleted = service.deleteLoopRelations("TH1", "C1");

        assertEquals(1, deleted);
        verify(relationService).deleteThisRelation("C1", "TH1", "BT", "C2");
        verify(relationService).deleteThisRelation("C2", "TH1", "NT", "C1");
        verify(relationService, never()).deleteThisRelation("C3", "TH1", "BT", "C1");
        verify(relationService, times(1)).deleteThisRelation("C1", "TH1", "BT", "C2");
    }

    private static NodeRelation loop(String id1, String id2) {
        NodeRelation nodeRelation = new NodeRelation();
        nodeRelation.setIdConcept1(id1);
        nodeRelation.setIdConcept2(id2);
        nodeRelation.setRelation("BT");
        return nodeRelation;
    }
}
