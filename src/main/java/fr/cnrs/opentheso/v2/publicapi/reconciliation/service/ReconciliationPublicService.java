package fr.cnrs.opentheso.v2.publicapi.reconciliation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReconciliationPublicService {

    private static final List<String> CANONICAL_PROPERTIES = List.of("prefLabel", "description", "aliases", "ark", "uri");
    private static final int DEFAULT_LIMIT = 15;

    private final ConceptReadService conceptReadService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    public ReconciliationPublicService(ConceptReadService conceptReadService) {
        this.conceptReadService = conceptReadService;
    }

    public Map<String, Object> metadata(String baseUrl, String thesaurusId, String lang) {
        String servicePrefix = baseUrl + "/openapi/v2/public/reconciliation";
        return Map.of(
                "name", "Opentheso Reconciliation Service",
                "versions", List.of("1.0"),
                "identifierSpace", baseUrl + "/",
                "schemaSpace", baseUrl + "/schema/",
                "view", Map.of("url", baseUrl + "/?idc={{id}}&idt=" + thesaurusId),
                "suggest", Map.of(
                        "entity", Map.of(
                                "service_url", servicePrefix + "/" + thesaurusId + "/" + lang,
                                "service_path", "/suggest/entity"
                        ),
                        "property", Map.of(
                                "service_url", servicePrefix,
                                "service_path", "/suggest/properties"
                        )
                ),
                "extend", Map.of(
                        "propose_properties", Map.of(
                                "service_url", servicePrefix,
                                "service_path", "/propose-properties"
                        )
                ),
                "preview", Map.of(
                        "url", servicePrefix + "/preview/" + thesaurusId + "/{{id}}",
                        "height", 120,
                        "width", 400
                ),
                "defaultTypes", List.of(Map.of("id", "concept", "name", "Concept")),
                "properties", List.of(
                        Map.of("id", "thesaurus", "name", "Thesaurus"),
                        Map.of("id", "lang", "name", "Language")
                )
        );
    }

    public Map<String, Object> reconcile(String baseUrl, String thesaurusId, String lang, String queriesJson) throws Exception {
        JsonNode root = objectMapper.readTree(queriesJson);
        Map<String, Object> response = new LinkedHashMap<>();

        var fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode q = root.get(key);
            String query = q.path("query").asText("");
            var candidates = search(thesaurusId, lang, query, DEFAULT_LIMIT);
            response.put(key, buildResult(candidates, query, baseUrl, thesaurusId, false));
        }
        return response;
    }

    public Map<String, Object> extend(String thesaurusId, String lang, String extendJson) throws Exception {
        JsonNode json = objectMapper.readTree(extendJson);

        List<String> ids = new ArrayList<>();
        JsonNode idsNode = json.get("ids");
        if (idsNode != null && idsNode.isArray()) {
            idsNode.forEach(n -> ids.add(n.asText()));
        }

        List<String> requestedProps = new ArrayList<>();
        JsonNode propsNode = json.get("properties");
        if (propsNode != null && propsNode.isArray()) {
            propsNode.forEach(p -> {
                JsonNode idNode = p.get("id");
                if (idNode != null) {
                    requestedProps.add(idNode.asText());
                }
            });
        }
        List<String> props = CANONICAL_PROPERTIES.stream().filter(requestedProps::contains).toList();

        Map<String, Object> rows = new LinkedHashMap<>();
        for (String id : ids) {
            var detail = conceptReadService.loadDetail(thesaurusId, id, lang).orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            for (String prop : props) {
                row.put(prop, buildExtendValues(prop, id, thesaurusId, detail));
            }
            for (String prop : CANONICAL_PROPERTIES) {
                row.putIfAbsent(prop, List.of(Map.of("str", "")));
            }
            rows.put(id, row);
        }

        List<Map<String, Object>> meta = List.of(
                Map.of("id", "prefLabel", "name", "Preferred label"),
                Map.of("id", "description", "name", "Definition"),
                Map.of("id", "aliases", "name", "Alternative labels"),
                Map.of("id", "ark", "name", "ARK Identifier"),
                Map.of("id", "uri", "name", "URI")
        );
        return Map.of("meta", meta, "rows", rows);
    }

    public Map<String, Object> suggestEntity(String baseUrl, String thesaurusId, String lang, String prefix) {
        var candidates = search(thesaurusId, lang, prefix, DEFAULT_LIMIT);
        List<Map<String, Object>> result = candidates.stream()
                .map(candidate -> buildConcept(candidate, prefix, baseUrl, thesaurusId, true))
                .sorted((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")))
                .toList();
        return Map.of("result", result);
    }

    public Map<String, Object> suggestProperties() {
        return Map.of("result", propertyDescriptors());
    }

    public Map<String, Object> proposeProperties() {
        return Map.of(
                "type", Map.of("id", "concept", "name", "Concept"),
                "properties", propertyDescriptors()
        );
    }

    public String preview(String thesaurusId, String conceptId) {
        var detail = conceptReadService.loadDetail(thesaurusId, conceptId, "fr").orElse(null);
        String label = detail != null ? detail.summary().preferredLabel() : conceptId;
        String description = detail != null ? firstDefinition(detail) : "";
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>body { font-family: Arial; font-size: 12px; margin: 6px; }"
                + "h4 { font-size: 13px; margin: 0 0 4px 0; }"
                + "p { font-size: 12px; margin: 2px 0; color: #444; }"
                + "hr { margin: 6px 0; border: none; border-top: 1px solid #ddd; }"
                + "small { font-size: 11px; color: #666; }</style></head><body>"
                + "<h4>" + safe(label) + "</h4>"
                + "<p>" + safe(description) + "</p>"
                + "<hr/><small>ID: " + conceptId + "</small>"
                + "</body></html>";
    }

    private record Candidate(String conceptId, String label, String arkId, String description, List<String> aliases) {
    }

    private List<Candidate> search(String thesaurusId, String lang, String query, int limit) {
        return conceptReadService.searchByLabel(thesaurusId, lang, query, limit).stream()
                .map(node -> {
                    var detail = conceptReadService.loadDetail(thesaurusId, node.getNodeId(), lang).orElse(null);
                    if (detail == null) {
                        return new Candidate(node.getNodeId(), node.getLabel(), null, "", List.of());
                    }
                    return new Candidate(
                            node.getNodeId(),
                            node.getLabel(),
                            detail.summary().arkId(),
                            firstDefinition(detail),
                            detail.synonyms()
                    );
                })
                .toList();
    }

    private String firstDefinition(ConceptDetail detail) {
        return detail.notesOfType("definition").stream()
                .findFirst()
                .map(note -> note.value())
                .orElse("");
    }

    private List<Object> buildExtendValues(String prop, String conceptId, String thesaurusId, ConceptDetail detail) {
        List<Object> values = new ArrayList<>();
        switch (prop) {
            case "prefLabel" -> values.add(Map.of("str", detail != null ? StringUtils.defaultString(detail.summary().preferredLabel()) : ""));
            case "description" -> values.add(Map.of("str", detail != null ? firstDefinition(detail) : ""));
            case "aliases" -> {
                if (detail != null) {
                    detail.synonyms().stream().filter(StringUtils::isNotBlank).forEach(alias -> values.add(Map.of("str", alias)));
                }
                if (values.isEmpty()) {
                    values.add(Map.of("str", ""));
                }
            }
            case "ark" -> values.add(Map.of("str", detail != null ? StringUtils.defaultString(detail.summary().arkId()) : ""));
            case "uri" -> values.add(Map.of("str", "?idc=" + conceptId + "&idt=" + thesaurusId));
            default -> values.add(Map.of("str", ""));
        }
        return values;
    }

    private Map<String, Object> buildResult(List<Candidate> candidates, String query, String baseUrl, String thesaurusId, boolean suggestMode) {
        List<Map<String, Object>> results = candidates.stream()
                .map(candidate -> buildConcept(candidate, query, baseUrl, thesaurusId, suggestMode))
                .toList();
        return Map.of("result", results);
    }

    private Map<String, Object> buildConcept(Candidate candidate, String query, String baseUrl, String thesaurusId, boolean suggestMode) {
        int score = computeScore(candidate.label(), query, suggestMode);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", candidate.conceptId());
        m.put("name", candidate.label());
        m.put("score", score);
        m.put("match", score >= 95);
        m.put("type", List.of(Map.of("id", "concept", "name", "Concept")));
        m.put("uri", baseUrl + "/openapi/v2/public/thesauri/" + thesaurusId + "/concepts/" + candidate.conceptId());
        if (StringUtils.isNotBlank(candidate.arkId())) {
            m.put("persistentIdentifier", candidate.arkId());
        }
        if (StringUtils.isNotBlank(candidate.description())) {
            m.put("description", candidate.description());
        }
        if (!candidate.aliases().isEmpty()) {
            m.put("aliases", candidate.aliases());
        }
        m.put("preview", Map.of(
                "url", baseUrl + "/openapi/v2/public/reconciliation/preview/" + thesaurusId + "/" + candidate.conceptId(),
                "height", 120,
                "width", 400
        ));
        return m;
    }

    private List<Map<String, Object>> propertyDescriptors() {
        return List.of(
                Map.of("id", "prefLabel", "name", "Preferred label"),
                Map.of("id", "description", "name", "Description"),
                Map.of("id", "aliases", "name", "Alternative labels"),
                Map.of("id", "ark", "name", "ARK Identifier"),
                Map.of("id", "uri", "name", "URI")
        );
    }

    private int computeScore(String label, String query, boolean suggestMode) {
        String l = normalize(label);
        String q = normalize(query);
        if (l.isEmpty() || q.isEmpty()) {
            return 0;
        }
        if (l.equals(q)) {
            return 100;
        }
        if (!suggestMode) {
            int max = Math.max(l.length(), q.length());
            int dist = levenshtein.apply(l, q);
            double sim = 1.0 - ((double) dist / max);
            return (int) (sim * 100);
        }

        int score;
        if (l.startsWith(q)) {
            score = 90;
        } else if (l.contains(q)) {
            score = 60;
        } else {
            score = 25;
        }
        if (l.startsWith(q) && l.length() == q.length()) {
            score += 5;
        }
        if (l.equals(q + "s")) {
            score -= 8;
        }
        if (l.matches(q + "\\d+")) {
            score -= 12;
        }
        int dist = levenshtein.apply(l, q);
        score -= Math.min(dist, 10);
        return Math.max(0, Math.min(score, 100));
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase().trim().replaceAll("[_\\-]", " ").replaceAll("\\s+", " ");
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
