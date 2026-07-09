package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionCsvIdExportSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusEditionCsvIdExportSupport implements ThesaurusEditionCsvIdExportSupport {

    private final ThesaurusCsvWriter thesaurusCsvWriter;

    @Override
    public byte[] writeCsvById(String thesaurusId, String languageCode, List<String> groupIds, char delimiter) {
        return thesaurusCsvWriter.writeCsvById(thesaurusId, languageCode, groupIds, delimiter);
    }
}
