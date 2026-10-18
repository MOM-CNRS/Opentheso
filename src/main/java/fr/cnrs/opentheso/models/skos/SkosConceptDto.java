package fr.cnrs.opentheso.models.skos;

import fr.cnrs.opentheso.entites.Gps;
import lombok.Data;

import java.util.*;

/**
 * DTO orienté RDF / SKOS pour représenter un concept/thésaurus.
 */
@Data
public class SkosConceptDto {

    /* =========================
       MÉTADONNÉES GÉNÉRALES
       ========================= */

    private String uri;
    private String localUri;
    private String identifier;
    private String permanentId;
    private String notation;
    private ResourceType resourceType; // CONCEPT, FOAF_IMAGE, COLLECTION
    private boolean deprecated;
    private String conceptType; // concept, place
    private String creatorName;
    private List<String> contributorName = new ArrayList<>();
    private String created;
    private String modified;

    /** Liste des points GPS construits depuis geoGps */
    private List<Gps> gpsPoints = new ArrayList<>();

    /** Images FOAF (URI vers les fichiers / médias) */
    private List<SkosConceptImageDto> images = new ArrayList<>();

    /* =========================
       LABELS SKOS
       ========================= */

    /** prefLabel : un seul par langue */
    private Map<String, String> prefLabels = new HashMap<>();

    /** altLabel : plusieurs valeurs possibles par langue */
    private Map<String, List<String>> altLabels = new HashMap<>();

    /** hiddenLabel : plusieurs valeurs possibles par langue */
    private Map<String, List<String>> hiddenLabels = new HashMap<>();

    public void setPrefLabel(String lang, String value) {
        if (value != null && !value.isBlank()) {
            prefLabels.put(lang, value.trim());
        }
    }

    public String getPrefLabel(String lang) {
        return prefLabels.get(lang);
    }

    public void addAltLabel(String lang, String value) {
        if (value == null || value.isBlank()) return;
        List<String> list = altLabels.computeIfAbsent(lang, k -> new ArrayList<>());
        String[] parts = value.split("##");
        for (String p : parts) {
            if (!p.isBlank()) list.add(p.trim());
        }
    }

    public List<String> getAltLabels(String lang) {
        return altLabels.getOrDefault(lang, List.of());
    }

    public void addHiddenLabel(String lang, String value) {
        if (value == null || value.isBlank()) return;
        List<String> list = hiddenLabels.computeIfAbsent(lang, k -> new ArrayList<>());
        String[] parts = value.split("##");
        for (String p : parts) {
            if (!p.isBlank()) list.add(p.trim());
        }
    }

    public List<String> getHiddenLabels(String lang) {
        return hiddenLabels.getOrDefault(lang, List.of());
    }

    /* =========================
       NOTES / DEFINITIONS
       ========================= */

    /** notes optimisées : un seul texte par type et langue */
    private Map<String, Map<String, String>> notes = new HashMap<>();

    public void setNote(String type, String lang, String value) {
        if (value == null || value.isBlank()) return;
        notes.computeIfAbsent(type, k -> new HashMap<>()).put(lang, value.trim());
    }

    public String getNote(String type, String lang) {
        return notes.getOrDefault(type, Map.of()).get(lang);
    }

    /* =========================
       RELATIONS SKOS
       ========================= */

    private Map<String, List<String>> relations = new HashMap<>();

    public void addRelation(String relationType, String targetUri) {
        if (targetUri == null || targetUri.isBlank()) return;
        relations.computeIfAbsent(relationType, k -> new ArrayList<>()).add(targetUri);
    }

    public List<String> getRelations(String relationType) {
        return relations.getOrDefault(relationType, List.of());
    }

    /* =========================
       GEO
       ========================= */

    private Double latitude;
    private Double longitude;
    private String geoGps;

    /* =========================
       COLONNES CSV BRUTES
       ========================= */

    private Map<String, List<String>> rawColumns = new HashMap<>();

    public void setRawColumn(String column, String value) {
        if (value == null || value.isBlank()) return;

        List<String> values = value.contains("##")
                ? Arrays.stream(value.split("##")).map(String::trim).filter(v -> !v.isBlank()).toList()
                : List.of(value.trim());

        rawColumns.put(column, values);
    }

    public List<String> getRawColumn(String column) {
        return rawColumns.getOrDefault(column, List.of());
    }

    /* =========================
       TRANSLATIONS BRUTES CSV
       ========================= */

    private Map<String, Map<String, String>> translations = new HashMap<>();

    public void setTranslation(String field, String lang, String value) {
        if (value == null || value.isBlank()) return;
        translations.computeIfAbsent(field, k -> new HashMap<>()).put(lang, value.trim());
    }

    public String getTranslation(String field, String lang) {
        return translations.getOrDefault(field, Map.of()).get(lang);
    }
}