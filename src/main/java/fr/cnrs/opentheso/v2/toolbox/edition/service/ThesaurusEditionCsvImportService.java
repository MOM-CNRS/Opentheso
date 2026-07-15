package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvImportPersistence;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionCsvImportService {

    private final ThesaurusEditionCsvImportPersistence thesaurusEditionCsvImportPersistence;
    private final NewThesaurusService newThesaurusService;

    public CsvLoadResult loadCsvFile(byte[] content, char delimiter) {
        var result = thesaurusEditionCsvImportPersistence.parse(content, delimiter);
        return new CsvLoadResult(
                result.conceptObjects(),
                result.languages(),
                result.totalConcepts(),
                result.warning(),
                result.error(),
                result.isSuccess()
        );
    }

    public CsvImportOutcome importNewThesaurus(
            String thesaurusName,
            String sourceLang,
            String formatDate,
            int userId,
            String userName,
            boolean superAdmin,
            Integer projectGroupId,
            List<ThesaurusCsvConceptObject> conceptObjects,
            List<String> languages
    ) {
        Integer groupId = projectGroupId;
        if (!superAdmin && groupId == null) {
            NewThesaurusFormOptions options = newThesaurusService.loadFormOptions(userId, false);
            if (options.projects().size() == 1) {
                groupId = options.projects().get(0).id();
            }
        }

        var result = thesaurusEditionCsvImportPersistence.importNewThesaurus(
                thesaurusName,
                StringUtils.defaultIfBlank(sourceLang, "fr"),
                StringUtils.defaultIfBlank(formatDate, "yyyy-MM-dd"),
                groupId,
                userId,
                userName,
                conceptObjects,
                languages
        );

        if (!result.isSuccess()) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(result.message(), "Import CSV impossible"));
        }
        return new CsvImportOutcome(result.thesaurusId(), result.importedConcepts(), result.message());
    }

    public record CsvLoadResult(
            List<ThesaurusCsvConceptObject> conceptObjects,
            List<String> languages,
            int totalConcepts,
            String warning,
            String error,
            boolean success
    ) {
    }

    public record CsvImportOutcome(String thesaurusId, int importedConcepts, String message) {
    }
}
