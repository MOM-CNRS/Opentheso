package fr.cnrs.opentheso.models.skos;

import fr.cnrs.opentheso.bean.importexport.newcsvimport.SkosConceptImageDto;
import lombok.Data;
import java.util.*;

/**
 * DTO orienté RDF / SKOS pour représenter un concept/thésaurus.
 */
@Data
public class SkosConceptDto {

    /* =========================
       🧱 MÉTADONNÉES GÉNÉRALES
       ========================= */

    private String uri;
    private String localUri;
    private String identifier;
    private String permanentId;
    private String notation;
    private ResourceType resourceType;
    private boolean deprecated;
    private String conceptType;
    private String creatorName;
    private List<String> contributorName = new ArrayList<>();
    private String created;
    private String modified;

    /** Images FOAF (URI vers les fichiers / médias) */
    private List<SkosConceptImageDto> images = new ArrayList<>();

    /* =========================
       🌍 LABELS SKOS (MULTI)
       type → lang → List<String>
       ========================= */

    private Map<String, Map<String, List<String>>> labels = new HashMap<>();

    public void addLabel(String type, String lang, String value) {
        if (value == null || value.isBlank()) return;

        // altLabel peut contenir plusieurs valeurs séparées par ##
        List<String> values = value.contains("##")
                ? Arrays.stream(value.split("##")).map(String::trim).filter(v -> !v.isBlank()).toList()
                : List.of(value.trim());

        labels
                .computeIfAbsent(type, k -> new HashMap<>())
                .computeIfAbsent(lang, k -> new ArrayList<>())
                .addAll(values);
    }

    public List<String> getLabels(String type, String lang) {
        return labels.getOrDefault(type, Map.of()).getOrDefault(lang, List.of());
    }

    public String getPrefLabel(String lang) {
        List<String> vals = getLabels("prefLabel", lang);
        return vals.isEmpty() ? null : vals.get(0);
    }

    /* =========================
       📝 NOTES / DEFINITIONS
       ========================= */

    private Map<String, Map<String, String>> notes = new HashMap<>();

    public void setNote(String type, String lang, String value) {
        if (value == null || value.isBlank()) return;
        notes.computeIfAbsent(type, k -> new HashMap<>()).put(lang, value);
    }

    public String getNote(String type, String lang) {
        return notes.getOrDefault(type, Map.of()).get(lang);
    }

    /* =========================
       🔗 RELATIONS SKOS
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
       🌍 GEO
       ========================= */

    private Double latitude;
    private Double longitude;
    private String geoGps;

    /* =========================
       📦 COLONNES CSV BRUTES
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
       🌐 TRANSLATIONS BRUTES CSV
       ========================= */

    private Map<String, Map<String, String>> translations = new HashMap<>();

    public void setTranslation(String field, String lang, String value) {
        if (value == null || value.isBlank()) return;
        translations.computeIfAbsent(field, k -> new HashMap<>()).put(lang, value);
    }

    public String getTranslation(String field, String lang) {
        return translations.getOrDefault(field, Map.of()).get(lang);
    }

    /* =========================
       🔄 POST-TRAITEMENT CSV
       ========================= */

    public void populateRelationsFromRaw() {
        String[] relationTypes = {"skos:broader", "skos:narrower", "skos:related",
                "broaderId", "narrowerId", "relatedId",
                "iso-thes:superOrdinate", "superOrdinateId",
                "skos:member", "memberId",
                "dcterms:isReplacedBy", "dcterms:replaces",
                "iso-thes:superOrdinate",
                "skos:exactMatch", "skos:closeMatch", "skos:relatedMatch", "skos:broadMatch", "skos:narrowMatch" };

        for (String type : relationTypes) {
            for (String uri : getRawColumn(type)) {
                addRelation(type.replace("skos:", ""), uri);
            }
        }
    }

    public void populateNotesFromTranslations() {
        String[] noteTypes = {
                "definition", "note", "scopeNote", "historyNote",
                "changeNote", "editorialNote", "example"
        };

        for (String type : noteTypes) {
            Map<String, String> map = translations.get(type);
            if (map != null) {
                map.forEach((lang, value) -> setNote(type, lang, value));
            }
        }
    }

    public void populateLabelsFromTranslations() {
        String[] labelTypes = {"prefLabel", "altLabel", "hiddenLabel"};

        for (String type : labelTypes) {
            Map<String, String> map = translations.get(type);
            if (map != null) {
                map.forEach((lang, value) -> addLabel(type, lang, value));
            }
        }
    }

    /**
     * Transforme la colonne "foaf:Image" en objets SkosConceptImageDto
     * en découpant d'abord par "##" pour plusieurs images,
     * puis par "@@" pour récupérer les métadonnées.
     */
    public void populateImagesFromRaw() {
        List<String> rawImages = this.getRawColumn("foaf:Image");
        for (String raw : rawImages) {
            String[] imageEntries = raw.split("##");
            for (String entry : imageEntries) {
                if (entry == null || entry.isBlank()) continue;

                SkosConceptImageDto imageDto = new SkosConceptImageDto();
                String[] parts = entry.split("@@");

                for (String part : parts) {
                    if (part.startsWith("rdf:about=")) {
                        imageDto.setUri(part.substring("rdf:about=".length()));
                    } else if (part.contains("=")) {
                        String[] kv = part.split("=", 2);
                        imageDto.addMeta(kv[0], kv[1]);
                    }
                }
                images.add(imageDto);
            }
        }
    }

    /**
     * Appeler UNE FOIS après import CSV
     */
    public void finalizeImport() {
        populateRelationsFromRaw();
        populateNotesFromTranslations();
        populateLabelsFromTranslations();
    }
}
