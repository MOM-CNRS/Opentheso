package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvDeprecatedExportSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusEditionCsvDeprecatedExportSupport implements ThesaurusEditionCsvDeprecatedExportSupport {

    private final ThesaurusCsvWriter thesaurusCsvWriter;

    @Override
    public byte[] writeCsvByDeprecated(String thesaurusId, String languageCode, char delimiter) {
        return thesaurusCsvWriter.writeCsvByDeprecated(thesaurusId, languageCode, delimiter);
    }
}
