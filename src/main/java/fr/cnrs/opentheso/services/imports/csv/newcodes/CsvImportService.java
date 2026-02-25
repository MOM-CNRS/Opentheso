package fr.cnrs.opentheso.services.imports.csv.newcodes;

import fr.cnrs.opentheso.models.skos.SkosConceptDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CsvImportService {

    /**
     * Lit un CSV depuis un InputStream avec détection BOM UTF-8
     */
    public List<CSVRecord> readCsv(InputStream inputStream) throws IOException {
        try (
                BOMInputStream bomInputStream = new BOMInputStream(inputStream);
                Reader reader = new InputStreamReader(bomInputStream, StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT
                        .withDelimiter(';')
                        .withFirstRecordAsHeader()
                        .withIgnoreEmptyLines()
                        .withIgnoreHeaderCase()
                        .withTrim()
                        .parse(reader)
        ) {
            return parser.getRecords();
        }
    }

    /**
     * Mappe une liste de CSVRecord vers une liste de DTO génériques
     */
    public <T> List<T> mapCsvToDto(
            List<CSVRecord> records,
            List<CsvColumnDefinition<T, ?>> definitions,
            java.util.function.Supplier<T> supplier
    ) {
        return records.stream()
                .map(record -> CsvMapper.map(record, supplier, definitions))
                .collect(Collectors.toList());
    }

    /**
     * Mappe directement une liste de CSVRecord vers SkosConceptDto
     */
    public List<SkosConceptDto> mapCsvToSkosConceptDto(List<CSVRecord> records) {
        return mapCsvToDto(
                records,
                SkosConceptCsvDefinition.getDefinitions(),
                SkosConceptDto::new
        );
    }
}
