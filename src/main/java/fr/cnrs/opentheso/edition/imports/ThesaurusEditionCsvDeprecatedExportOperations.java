package fr.cnrs.opentheso.edition.imports;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvDeprecatedExportOperations {

    private final ThesaurusCsvWriter thesaurusCsvWriter;

    public byte[] writeCsvByDeprecated(String thesaurusId, String languageCode, char delimiter) {
        return thesaurusCsvWriter.writeCsvByDeprecated(thesaurusId, languageCode, delimiter);
    }
}
