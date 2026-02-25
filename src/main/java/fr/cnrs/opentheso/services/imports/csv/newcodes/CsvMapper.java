package fr.cnrs.opentheso.services.imports.csv.newcodes;

import fr.cnrs.opentheso.models.skos.SkosConceptDto;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Mapper générique CSV → DTO compatible CsvColumnDefinition<T, V>
 */
public class CsvMapper {

    /**
     * Mappe un CSVRecord vers un DTO générique
     *
     * @param record      Ligne CSV
     * @param supplier    Fournisseur du DTO (ex : SkosConceptDto::new)
     * @param definitions Liste des définitions de colonnes
     * @param <T>         Type du DTO
     * @return DTO mappé
     */
    public static <T> T map(
            CSVRecord record,
            Supplier<T> supplier,
            List<CsvColumnDefinition<T, ?>> definitions
    ) {
        T instance = supplier.get();

        // 1️⃣ Mapping via CsvColumnDefinition (colonnes simples)
        for (CsvColumnDefinition<T, ?> def : definitions) {
            def.apply(instance, record);
        }

        // 2️⃣ Mapping automatique pour SkosConceptDto
        if (instance instanceof SkosConceptDto dto) {

            Map<String, String> row = record.toMap();

            for (Map.Entry<String, String> entry : row.entrySet()) {
                String header = entry.getKey();
                String value = entry.getValue();

                if (value == null || value.isBlank()) continue;

                // Ignorer colonnes déjà mappées par CsvColumnDefinition
                boolean alreadyMapped = definitions.stream()
                        .anyMatch(d -> d.getCsvColumnName().equalsIgnoreCase(header));
                if (alreadyMapped) continue;

                // 🌍 Colonnes multilingues (ex: skos:prefLabel@fr)
                if (header.contains("@")) {
                    String[] parts = header.split("@");
                    if (parts.length == 2) {
                        String field = parts[0]
                                .replace("skos:", "")
                                .replace("dcterms:", "")
                                .replace("iso-thes:", "")
                                .replace("geo:", "");
                        String lang = parts[1];
                        dto.setTranslation(field, lang, value);
                        continue;
                    }
                }

                // 🧩 Toutes les autres colonnes → rawColumns
                dto.setRawColumn(header, value);
            }
        }

        return instance;
    }
}
