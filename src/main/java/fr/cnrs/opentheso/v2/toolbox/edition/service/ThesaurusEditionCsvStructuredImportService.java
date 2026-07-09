package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvStructuredImportSupport;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredImportService {

    private final ThesaurusEditionCsvStructuredImportSupport thesaurusEditionCsvStructuredImportSupport;
    private final NewThesaurusService newThesaurusService;

    public StructuredLoadResult loadCsvFile(byte[] content, char delimiter) {
        var result = thesaurusEditionCsvStructuredImportSupport.parse(content, delimiter);
        return new StructuredLoadResult(result.root(), result.totalConcepts(), result.error(), result.isSuccess());
    }

    public StructuredImportOutcome importNewThesaurus(
            String thesaurusName,
            String sourceLang,
            int userId,
            String userName,
            boolean superAdmin,
            Integer projectGroupId,
            NodeTree root
    ) {
        Integer groupId = projectGroupId;
        if (!superAdmin && groupId == null) {
            NewThesaurusFormOptions options = newThesaurusService.loadFormOptions(userId, false);
            if (options.projects().size() == 1) {
                groupId = options.projects().get(0).id();
            }
        }

        var result = thesaurusEditionCsvStructuredImportSupport.importNewThesaurus(
                thesaurusName,
                StringUtils.defaultIfBlank(sourceLang, "fr"),
                groupId,
                userId,
                userName,
                root
        );

        if (!result.isSuccess()) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(result.message(), "Import CSV structuré impossible"));
        }
        return new StructuredImportOutcome(result.thesaurusId(), result.importedConcepts(), result.message());
    }

    public record StructuredLoadResult(NodeTree root, int totalConcepts, String error, boolean success) {
    }

    public record StructuredImportOutcome(String thesaurusId, int importedConcepts, String message) {
    }
}
