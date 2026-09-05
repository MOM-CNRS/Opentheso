package fr.cnrs.opentheso.v2.sync.model;

import java.util.ArrayList;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payload d'un concept envoyé par un thésaurus esclave vers le maître.
 */
public record SyncConceptPayload(
        String identifier,
        String permanentId,
        String notation,
        Map<String, String> prefLabels,
        Map<String, List<String>> altLabels,
        Map<String, List<String>> notes,
        Map<String, List<String>> definitions,
        Map<String, List<String>> scopeNotes
) implements Serializable {
    public SyncConceptPayload {
        prefLabels = prefLabels == null ? Map.of() : Map.copyOf(prefLabels);
        altLabels = copyListMap(altLabels);
        notes = copyListMap(notes);
        definitions = copyListMap(definitions);
        scopeNotes = copyListMap(scopeNotes);
    }

    private static Map<String, List<String>> copyListMap(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((lang, values) -> copy.put(lang, List.copyOf(values == null ? List.of() : values)));
        return Map.copyOf(copy);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String identifier;
        private String permanentId;
        private String notation;
        private Map<String, String> prefLabels = new LinkedHashMap<>();
        private Map<String, List<String>> altLabels = new LinkedHashMap<>();
        private Map<String, List<String>> notes = new LinkedHashMap<>();
        private Map<String, List<String>> definitions = new LinkedHashMap<>();
        private Map<String, List<String>> scopeNotes = new LinkedHashMap<>();

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder permanentId(String permanentId) {
            this.permanentId = permanentId;
            return this;
        }

        public Builder notation(String notation) {
            this.notation = notation;
            return this;
        }

        public Builder prefLabel(String lang, String value) {
            if (lang != null && value != null && !value.isBlank()) {
                prefLabels.put(lang, value.trim());
            }
            return this;
        }

        public Builder altLabel(String lang, String value) {
            addToListMap(altLabels, lang, value);
            return this;
        }

        public Builder note(String lang, String value) {
            addToListMap(notes, lang, value);
            return this;
        }

        public Builder definition(String lang, String value) {
            addToListMap(definitions, lang, value);
            return this;
        }

        public Builder scopeNote(String lang, String value) {
            addToListMap(scopeNotes, lang, value);
            return this;
        }

        public SyncConceptPayload build() {
            return new SyncConceptPayload(
                    identifier, permanentId, notation, prefLabels, altLabels, notes, definitions, scopeNotes);
        }

        private static void addToListMap(Map<String, List<String>> map, String lang, String value) {
            if (lang == null || value == null || value.isBlank()) {
                return;
            }
            map.computeIfAbsent(lang, ignored -> new ArrayList<>()).add(value.trim());
        }
    }
}
