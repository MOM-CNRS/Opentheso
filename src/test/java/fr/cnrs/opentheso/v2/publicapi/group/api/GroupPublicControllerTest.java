package fr.cnrs.opentheso.v2.publicapi.group.api;

import fr.cnrs.opentheso.v2.publicapi.group.api.dto.GroupBranchTreeEntryResponse;
import fr.cnrs.opentheso.v2.publicapi.group.api.dto.GroupSummaryResponse;
import fr.cnrs.opentheso.v2.publicapi.group.service.GroupPublicExportService;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport.ExportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupPublicControllerTest {

    @Mock
    private GroupPublicExportService groupPublicExportService;

    private GroupPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new GroupPublicController(groupPublicExportService);
    }

    @Test
    void exportGroup_returnsFileResponse() throws Exception {
        when(groupPublicExportService.exportGroup("TH1", "G1", "skos"))
                .thenReturn(new ExportResult(new byte[]{1, 2}, "TH1_G1.rdf", "application/xml"));

        var response = controller.exportGroup("TH1", "G1", "skos");

        assertEquals(2, response.getBody().length);
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("TH1_G1.rdf"));
    }

    @Test
    void exportBranch_returnsFileResponse() throws Exception {
        when(groupPublicExportService.exportBranch("TH1", List.of("G1", "G2"), "skos"))
                .thenReturn(new ExportResult(new byte[]{3}, "TH1_branch.rdf", "application/xml"));

        var response = controller.exportBranch("TH1", List.of("G1", "G2"), "skos");

        assertEquals(1, response.getBody().length);
    }

    @Test
    void branchTree_returnsServiceResult() {
        var entries = List.of(new GroupBranchTreeEntryResponse("G1", "Groupe 1", List.of()));
        when(groupPublicExportService.branchTree("TH1", List.of("G1"), "fr")).thenReturn(entries);

        var response = controller.branchTree("TH1", List.of("G1"), "fr");

        assertEquals(entries, response);
    }

    @Test
    void listGroups_returnsServiceResult() {
        var groups = List.of(new GroupSummaryResponse("G1", List.of()));
        when(groupPublicExportService.listGroups("TH1")).thenReturn(groups);

        var response = controller.listGroups("TH1");

        assertEquals(groups, response);
    }

    @Test
    void listSubGroups_returnsServiceResult() {
        var groups = List.of(new GroupSummaryResponse("G2", List.of()));
        when(groupPublicExportService.listSubGroups("TH1", "G1")).thenReturn(groups);

        var response = controller.listSubGroups("TH1", "G1");

        assertEquals(groups, response);
    }
}
