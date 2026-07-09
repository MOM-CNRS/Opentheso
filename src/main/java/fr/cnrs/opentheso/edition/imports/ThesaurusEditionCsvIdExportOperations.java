package fr.cnrs.opentheso.edition.imports;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvIdExportOperations {

    private final ThesaurusCsvWriter thesaurusCsvWriter;

    public byte[] writeCsvById(String thesaurusId, String languageCode, List<String> groupIds, char delimiter) {
        return thesaurusCsvWriter.writeCsvById(thesaurusId, languageCode, groupIds, delimiter);
    }
}
