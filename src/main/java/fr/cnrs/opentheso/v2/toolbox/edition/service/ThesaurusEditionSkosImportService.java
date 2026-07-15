package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionSkosImportSupport;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionSkosImportService {

    private final ThesaurusEditionSkosImportSupport thesaurusEditionSkosImportSupport;
    private final NewThesaurusService newThesaurusService;

    public SkosLoadResult loadSkosFile(
            InputStream inputStream,
            int typeImport,
            String selectedLang,
            StringBuffer errorBuffer
    ) throws IOException {
        String lang = StringUtils.isBlank(selectedLang) ? "fr" : selectedLang;
        var document = thesaurusEditionSkosImportSupport.readSkos(
                inputStream,
                SkosRdfFormatSupport.resolveImportFormat(typeImport),
                lang,
                errorBuffer
        );
        return new SkosLoadResult(
                document,
                document.getTitle(),
                document.getConceptList() == null ? 0 : document.getConceptList().size()
        );
    }

    @Transactional
    public String importNewThesaurus(
            SKOSXmlDocument document,
            String formatDate,
            int userId,
            boolean superAdmin,
            Integer projectGroupId,
            String sourceLang,
            String selectedIdentifier,
            String prefixHandle,
            String prefixDoi
    ) throws SQLException {
        var preferences = new Preferences();
        preferences.setSourceLang(StringUtils.defaultIfBlank(sourceLang, "fr"));

        Integer groupId = projectGroupId;
        if (!superAdmin && groupId == null) {
            var options = newThesaurusService.loadFormOptions(userId, false);
            if (options.projects().size() == 1) {
                groupId = options.projects().get(0).id();
            }
        }

        String thesaurusId = thesaurusEditionSkosImportSupport.importNewThesaurus(
                document,
                StringUtils.defaultIfBlank(formatDate, "yyyy-MM-dd"),
                userId,
                groupId,
                StringUtils.defaultIfBlank(sourceLang, "fr"),
                StringUtils.defaultIfBlank(selectedIdentifier, "sans"),
                StringUtils.defaultIfBlank(prefixHandle, ""),
                StringUtils.defaultIfBlank(prefixDoi, ""),
                preferences
        );

        if (thesaurusId == null) {
            throw new IllegalStateException(thesaurusEditionSkosImportSupport.getLastErrorMessage());
        }
        return thesaurusId;
    }

    public record SkosLoadResult(SKOSXmlDocument document, String uri, int totalConcepts) {
    }
}
