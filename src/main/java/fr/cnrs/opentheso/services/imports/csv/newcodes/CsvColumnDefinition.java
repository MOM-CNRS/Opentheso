package fr.cnrs.opentheso.services.imports.csv.newcodes;

import fr.cnrs.opentheso.models.skos.SkosConceptDto;
import lombok.Getter;
import org.apache.commons.csv.CSVRecord;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Définit le mapping entre une colonne CSV et un DTO générique.
 *
 * @param <T> type du DTO
 * @param <V> type de la valeur convertie
 */
@Getter
public class CsvColumnDefinition<T, V> {

    private final String csvColumnName;
    private final Function<String, V> converter;
    private final BiConsumer<T, V> setter;

    public CsvColumnDefinition(String csvColumnName,
                               Function<String, V> converter,
                               BiConsumer<T, V> setter) {
        this.csvColumnName = csvColumnName;
        this.converter = converter;
        this.setter = setter;
    }

    /**
     * Applique la valeur CSV sur le DTO cible
     *
     * @param target DTO cible
     * @param record ligne CSV
     */
    public void apply(T target, CSVRecord record) {
        if (!record.isMapped(csvColumnName)) return;

        String raw = record.get(csvColumnName);
        if (raw == null || raw.isBlank()) return;

        // 🎯 Cas spécifique SkosConceptDto
        if (target instanceof SkosConceptDto dto) {

            // 🌍 Colonnes multilingues (ex: skos:prefLabel@fr)
            if (csvColumnName.contains("@")) {
                handleMultilang(dto, raw);
                return;
            }

            // 🔧 Setter standard avec conversion
            if (setter != null && converter != null) {
                V value = converter.apply(raw);
                setter.accept(target, value);
                return;
            }

            // 📦 Colonne non mappée → rawColumns (String forcé)
            dto.setRawColumn(csvColumnName, raw);
            return;
        }

        // 🧱 Cas générique
        if (setter != null && converter != null) {
            setter.accept(target, converter.apply(raw));
        }
    }

    /* =========================
       Helpers
       ========================= */

    /**
     * Gestion des colonnes multilingues
     */
    private void handleMultilang(SkosConceptDto dto, String raw) {
        String[] parts = csvColumnName.split("@");
        if (parts.length != 2) return;

        // Retirer préfixes RDF courants
        String field = parts[0].replaceAll("skos:|dcterms:|foaf:|iso-thes:|geo:", "");
        String lang = parts[1];

        dto.setTranslation(field, lang, raw);
    }

    /* =========================
       Définition pratique pour les colonnes raw
       ========================= */

    /**
     * Factory pour créer une colonne brute (String) directement vers rawColumns
     */
    public static CsvColumnDefinition<SkosConceptDto, String> raw(String columnName) {
        return new CsvColumnDefinition<>(
                columnName,
                Function.identity(),
                (dto, value) -> dto.setRawColumn(columnName, value) // lambda
        );
    }
}
