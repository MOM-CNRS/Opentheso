package fr.cnrs.opentheso.concept.model;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Toutes les lignes d'une fiche, lues en un seul aller-retour SQL.
 */
public record ConceptFicheRows(
        Object[] core,
        String creator,
        List<String> contributors,
        Object[] preferred,
        List<Object[]> altLabels,
        List<Object[]> hiddenLabels,
        List<Object[]> prefTranslations,
        List<Object[]> altTranslations,
        List<Object[]> hiddenTranslations,
        List<Object[]> notes,
        List<Object[]> broaders,
        List<Object[]> relateds,
        List<Object[]> narrowers,
        List<Object[]> alignments,
        List<Object[]> gps,
        List<Object[]> groups,
        List<Object[]> images,
        List<Object[]> replaces,
        List<Object[]> replacedBy,
        List<Object[]> facets,
        List<Object[]> externalResources,
        List<Object[]> ancestors
) {
    public ConceptFicheRows {
        contributors = contributors == null ? List.of() : contributors;
        altLabels = nullToEmpty(altLabels);
        hiddenLabels = nullToEmpty(hiddenLabels);
        prefTranslations = nullToEmpty(prefTranslations);
        altTranslations = nullToEmpty(altTranslations);
        hiddenTranslations = nullToEmpty(hiddenTranslations);
        notes = nullToEmpty(notes);
        broaders = nullToEmpty(broaders);
        relateds = nullToEmpty(relateds);
        narrowers = nullToEmpty(narrowers);
        alignments = nullToEmpty(alignments);
        gps = nullToEmpty(gps);
        groups = nullToEmpty(groups);
        images = nullToEmpty(images);
        replaces = nullToEmpty(replaces);
        replacedBy = nullToEmpty(replacedBy);
        facets = nullToEmpty(facets);
        externalResources = nullToEmpty(externalResources);
        ancestors = nullToEmpty(ancestors);
    }

    public static ConceptFicheRows parse(String json) {
        try (var reader = Json.createReader(new StringReader(json == null ? "{}" : json))) {
            JsonObject object = reader.readObject();
            return new ConceptFicheRows(
                    arrayOrNull(object, "core"),
                    text(object, "creator"),
                    strings(object, "contributors"),
                    arrayOrNull(object, "preferred"),
                    arrays(object, "alts"),
                    arrays(object, "hidden"),
                    arrays(object, "prefTranslations"),
                    arrays(object, "altTranslations"),
                    arrays(object, "hiddenTranslations"),
                    arrays(object, "notes"),
                    arrays(object, "broaders"),
                    arrays(object, "relateds"),
                    arrays(object, "narrowers"),
                    arrays(object, "alignments"),
                    arrays(object, "gps"),
                    arrays(object, "groups"),
                    arrays(object, "images"),
                    arrays(object, "replaces"),
                    arrays(object, "replacedBy"),
                    arrays(object, "facets"),
                    arrays(object, "externalResources"),
                    arrays(object, "ancestors")
            );
        }
    }

    private static List<Object[]> nullToEmpty(List<Object[]> values) {
        return values == null ? List.of() : values;
    }

    private static String text(JsonObject object, String key) {
        if (!object.containsKey(key) || object.isNull(key)) {
            return "";
        }
        JsonValue value = object.get(key);
        if (value.getValueType() == JsonValue.ValueType.STRING) {
            return ((JsonString) value).getString();
        }
        return value.toString();
    }

    private static List<String> strings(JsonObject object, String key) {
        JsonArray array = array(object, key);
        if (array == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonValue value : array) {
            if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
                continue;
            }
            values.add(value.getValueType() == JsonValue.ValueType.STRING
                    ? ((JsonString) value).getString()
                    : value.toString());
        }
        return values;
    }

    private static Object[] arrayOrNull(JsonObject object, String key) {
        JsonArray array = array(object, key);
        return array == null ? null : cells(array);
    }

    private static List<Object[]> arrays(JsonObject object, String key) {
        JsonArray array = array(object, key);
        if (array == null) {
            return List.of();
        }
        List<Object[]> rows = new ArrayList<>();
        for (JsonValue value : array) {
            if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                rows.add(cells((JsonArray) value));
            }
        }
        return rows;
    }

    private static JsonArray array(JsonObject object, String key) {
        if (!object.containsKey(key) || object.isNull(key)) {
            return null;
        }
        JsonValue value = object.get(key);
        return value.getValueType() == JsonValue.ValueType.ARRAY ? (JsonArray) value : null;
    }

    private static Object[] cells(JsonArray array) {
        Object[] cells = new Object[array.size()];
        for (int i = 0; i < array.size(); i++) {
            cells[i] = cell(array.get(i));
        }
        return cells;
    }

    private static Object cell(JsonValue value) {
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            return null;
        }
        return switch (value.getValueType()) {
            case STRING -> ((JsonString) value).getString();
            case NUMBER -> number((JsonNumber) value);
            case TRUE -> Boolean.TRUE;
            case FALSE -> Boolean.FALSE;
            default -> value.toString();
        };
    }

    private static Object number(JsonNumber number) {
        if (number.isIntegral()) {
            return number.longValue();
        }
        return number.doubleValue();
    }
}
