package fr.cnrs.opentheso.v2.candidat.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatDiscussionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatIdValueRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatNoteDetailRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatTranslationRow;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CandidatDetailJsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CandidatDetailJsonParser() {
    }

    public record ParsedDetail(
            List<CandidatIdValueRow> collections,
            List<CandidatIdValueRow> broaderRelations,
            List<CandidatIdValueRow> relatedRelations,
            List<String> synonyms,
            List<CandidatNoteDetailRow> notes,
            Set<String> votedNoteIds,
            List<CandidatTranslationRow> translations,
            List<CandidatDiscussionRow> messages
    ) {
    }

    public static ParsedDetail parse(
            String collectionsJson,
            String broaderJson,
            String relatedJson,
            String synonymsJson,
            String notesJson,
            String noteVoteIdsJson,
            String translationsJson,
            String messagesJson
    ) {
        return new ParsedDetail(
                parseIdValues(collectionsJson),
                parseIdValues(broaderJson),
                parseIdValues(relatedJson),
                parseStringList(synonymsJson),
                parseNotes(notesJson),
                parseNoteVoteIds(noteVoteIdsJson),
                parseTranslations(translationsJson),
                parseMessages(messagesJson)
        );
    }

    private static List<CandidatIdValueRow> parseIdValues(String json) {
        try {
            JsonNode root = MAPPER.readTree(defaultArray(json));
            if (!root.isArray()) {
                return List.of();
            }
            return MAPPER.convertValue(root, new TypeReference<List<CandidatIdValueRow>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static List<String> parseStringList(String json) {
        try {
            JsonNode root = MAPPER.readTree(defaultArray(json));
            if (!root.isArray()) {
                return List.of();
            }
            return MAPPER.convertValue(root, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static List<CandidatNoteDetailRow> parseNotes(String json) {
        try {
            JsonNode root = MAPPER.readTree(defaultArray(json));
            if (!root.isArray()) {
                return List.of();
            }
            return MAPPER.convertValue(root, new TypeReference<List<CandidatNoteDetailRow>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static Set<String> parseNoteVoteIds(String json) {
        try {
            JsonNode root = MAPPER.readTree(defaultArray(json));
            if (!root.isArray()) {
                return Set.of();
            }
            Set<String> ids = new HashSet<>();
            root.forEach(node -> ids.add(node.asText()));
            return ids;
        } catch (Exception ex) {
            return Collections.emptySet();
        }
    }

    private static List<CandidatTranslationRow> parseTranslations(String json) {
        try {
            JsonNode root = MAPPER.readTree(defaultArray(json));
            if (!root.isArray()) {
                return List.of();
            }
            return MAPPER.convertValue(root, new TypeReference<List<CandidatTranslationRow>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static List<CandidatDiscussionRow> parseMessages(String json) {
        try {
            JsonNode root = MAPPER.readTree(defaultArray(json));
            if (!root.isArray()) {
                return List.of();
            }
            return MAPPER.convertValue(root, new TypeReference<List<CandidatDiscussionRow>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static String defaultArray(String json) {
        return json == null || json.isBlank() ? "[]" : json;
    }
}
