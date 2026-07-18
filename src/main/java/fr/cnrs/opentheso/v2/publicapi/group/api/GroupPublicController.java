package fr.cnrs.opentheso.v2.publicapi.group.api;

import fr.cnrs.opentheso.v2.publicapi.group.api.dto.GroupBranchTreeEntryResponse;
import fr.cnrs.opentheso.v2.publicapi.group.api.dto.GroupSummaryResponse;
import fr.cnrs.opentheso.v2.publicapi.group.service.GroupPublicExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController("v2PublicGroupController")
@RequestMapping("/openapi/v2/public/thesauri/{thesaurusId}/groups")
@RequiredArgsConstructor
@Tag(name = "Groupes (public)", description = "Export public des groupes/collections (v2, sans authentification)")
public class GroupPublicController {

    private final GroupPublicExportService groupPublicExportService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liste tous les groupes/collections d'un thésaurus avec leurs traductions")
    public List<GroupSummaryResponse> listGroups(@PathVariable String thesaurusId) {
        return groupPublicExportService.listGroups(thesaurusId);
    }

    @GetMapping(value = "/{groupId}/subgroups", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liste les sous-collections d'une collection")
    public List<GroupSummaryResponse> listSubGroups(
            @PathVariable String thesaurusId,
            @PathVariable String groupId
    ) {
        return groupPublicExportService.listSubGroups(thesaurusId, groupId);
    }

    @GetMapping(value = "/{groupId}/export")
    @Operation(summary = "Export SKOS d'un groupe/collection", description = "Formats : skos (défaut), jsonld, turtle, json")
    public ResponseEntity<byte[]> exportGroup(
            @PathVariable String thesaurusId,
            @PathVariable String groupId,
            @RequestParam(defaultValue = "skos") String format
    ) throws IOException {
        var result = groupPublicExportService.exportGroup(thesaurusId, groupId, format);
        return toFileResponse(result.content(), result.filename(), result.contentType());
    }

    @GetMapping(value = "/branch")
    @Operation(summary = "Export SKOS d'une branche de groupes", description = "groupIds séparés par des virgules")
    public ResponseEntity<byte[]> exportBranch(
            @PathVariable String thesaurusId,
            @RequestParam List<String> groupIds,
            @RequestParam(defaultValue = "skos") String format
    ) throws IOException {
        var result = groupPublicExportService.exportBranch(thesaurusId, groupIds, format);
        return toFileResponse(result.content(), result.filename(), result.contentType());
    }

    @GetMapping(value = "/branch-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Chemin vers la racine d'une branche de groupes", description = "groupIds séparés par des virgules")
    public List<GroupBranchTreeEntryResponse> branchTree(
            @PathVariable String thesaurusId,
            @RequestParam List<String> groupIds,
            @RequestParam(required = false) String lang
    ) {
        return groupPublicExportService.branchTree(thesaurusId, groupIds, lang);
    }

    private ResponseEntity<byte[]> toFileResponse(byte[] content, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
