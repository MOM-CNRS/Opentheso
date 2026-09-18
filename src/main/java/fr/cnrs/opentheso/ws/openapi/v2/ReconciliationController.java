package fr.cnrs.opentheso.ws.openapi.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.models.concept.ConceptNote;
import fr.cnrs.opentheso.models.concept.NodeAutoCompletion;
import fr.cnrs.opentheso.models.concept.NodeFullConcept;
import fr.cnrs.opentheso.ws.api.RestRDFHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@RestController
@RequestMapping("/api/v2")
@Tag(name = "Api v2")
@CrossOrigin(origins = "*")
public class ReconciliationController {

    private final RestRDFHelper restRDFHelper;
    private final ObjectMapper mapper = new ObjectMapper();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    /**
     * URL publique de l'instance (sans slash final), injectée depuis application.yaml :
     *
     * opentheso:
     *   public-base-url: ${OPENTHESO_PUBLIC_BASE_URL:http://localhost:8099}
     */
    @Value("${opentheso.public-base-url}")
    private String baseUrl;

    public ReconciliationController(RestRDFHelper restRDFHelper) {
        this.restRDFHelper = restRDFHelper;
    }

    private String base() {
        return (baseUrl != null && baseUrl.endsWith("/"))
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    // =========================================================
    // 1. METADATA (IMPORTANT OPENREFINE ENTRY POINT)
    // =========================================================
    @GetMapping(value = "/{thesaurus}/{lang}/reconcile", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "OpenRefine - Reconcile metadata",
            description = "Manifeste de service pour la réconciliation avec OpenRefine."
    )
    public Map<String, Object> metadata(@PathVariable String thesaurus, @PathVariable String lang) {

        String base = base();

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("name", "Opentheso Reconciliation Service");
        manifest.put("versions", List.of("0.2"));

        manifest.put("identifierSpace", base + "/");
        manifest.put("schemaSpace", base + "/schema/");

        manifest.put("defaultTypes", List.of(
                Map.of("id", "concept", "name", "Concept")
        ));

        manifest.put("view", Map.of(
                "url", base + "/?idc={{id}}&idt=" + thesaurus
        ));

        manifest.put("suggest", Map.of(
                "entity", Map.of(
                        "service_url", base + "/api/v2/" + thesaurus + "/" + lang,
                        "service_path", "/suggest/entity"
                ),
                "property", Map.of(
                        "service_url", base + "/api/v2",
                        "service_path", "/suggest/properties"
                )
        ));

        manifest.put("extend", Map.of(
                "propose_properties", Map.of(
                        "service_url", base + "/api/v2",
                        "service_path", "/propose_properties"
                )
        ));

        manifest.put("preview", Map.of(
                "url", base + "/api/v2/preview/" + thesaurus + "/{{id}}",
                "height", 120,
                "width", 400
        ));

        manifest.put("properties", List.of(
                Map.of("id", "thesaurus", "name", "Thesaurus"),
                Map.of("id", "lang", "name", "Language")
        ));

        return manifest;
    }

    // =========================================================
    // 2 & 3. RECONCILE / EXTEND (POST unique)
    // =========================================================
    @PostMapping(
            value = "/{thesaurus}/{lang}/reconcile",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "OpenRefine - Reconcile / Extend",
            description = "Réconciliation ou enrichissement des concepts, selon le paramètre présent."
    )
    public Map<String, Object> reconcilePost(
            @PathVariable String thesaurus,
            @PathVariable String lang,
            @RequestParam(required = false) String queries,
            @RequestParam(required = false) String extend
    ) throws Exception {

        if (extend != null) {
            return doExtend(thesaurus, lang, extend);
        }
        if (queries != null) {
            return doReconcile(thesaurus, lang, queries);
        }
        // certains clients interrogent le endpoint en POST sans corps
        return metadata(thesaurus, lang);
    }

    // Certains clients (et versions d'OpenRefine) envoient "queries" en GET
    @GetMapping(
            value = "/{thesaurus}/{lang}/reconcile",
            params = "queries",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> reconcileGet(
            @PathVariable String thesaurus,
            @PathVariable String lang,
            @RequestParam String queries
    ) throws Exception {
        return doReconcile(thesaurus, lang, queries);
    }

    private Map<String, Object> doReconcile(String thesaurus, String lang, String queries) throws Exception {

        JsonNode root = mapper.readTree(queries);
        Map<String, Object> response = new LinkedHashMap<>();

        Iterator<String> keys = root.fieldNames();
        while (keys.hasNext()) {
            String key = keys.next();
            JsonNode q = root.get(key);
            String query = q.path("query").asText("");
            int limit = q.path("limit").asInt(10);

            List<NodeAutoCompletion> data =
                    restRDFHelper.searchAutoCompletionWS(query, lang, null, thesaurus, true);

            response.put(key, buildResult(data, query, thesaurus, false, limit));
        }

        return response;
    }

    private Map<String, Object> doExtend(String thesaurus, String lang, String extend) throws Exception {

        JsonNode json = mapper.readTree(extend);

        // =========================
        // IDS
        // =========================
        List<String> ids = new ArrayList<>();
        JsonNode idsNode = json.get("ids");
        if (idsNode != null && idsNode.isArray()) {
            idsNode.forEach(n -> ids.add(n.asText()));
        }

        // =========================
        // PROPERTIES (ORDRE CANONIQUE)
        // =========================
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

        List<String> canonicalOrder = List.of("prefLabel", "description", "aliases", "ark", "uri");
        List<String> props = canonicalOrder.stream()
                .filter(requestedProps::contains)
                .toList();

        // =========================
        // ROWS
        // =========================
        Map<String, Object> rows = new LinkedHashMap<>();
        String base = base();

        for (String id : ids) {

            NodeFullConcept concept = restRDFHelper.getNodeFullConcept(thesaurus, id, lang);
            Map<String, Object> row = new LinkedHashMap<>();

            if (concept == null) {
                for (String p : canonicalOrder) {
                    row.put(p, List.of(Map.of("str", "")));
                }
                rows.put(id, row);
                continue;
            }

            for (String prop : props) {

                List<Map<String, String>> values = new ArrayList<>();

                switch (prop) {

                    case "prefLabel" -> {
                        String label = concept.getPrefLabel() != null
                                ? concept.getPrefLabel().getLabel()
                                : "";
                        values.add(Map.of("str", label != null ? label : ""));
                    }

                    case "description" -> {
                        String def = safeNote(concept.getDefinitions());
                        values.add(Map.of("str", def != null ? def : ""));
                    }

                    case "aliases" -> {
                        if (concept.getAltLabels() != null && !concept.getAltLabels().isEmpty()) {
                            for (var a : concept.getAltLabels()) {
                                if (a != null && isValid(a.getLabel())) {
                                    values.add(Map.of("str", a.getLabel()));
                                }
                            }
                        }
                        if (values.isEmpty()) {
                            values.add(Map.of("str", ""));
                        }
                    }

                    case "ark" -> values.add(Map.of(
                            "str",
                            isValid(concept.getPermanentId()) ? concept.getPermanentId() : ""
                    ));

                    case "uri" -> {
                        String uri = isValid(concept.getPermanentId())
                                ? concept.getPermanentId()
                                : base + "/?idc=" + id + "&idt=" + thesaurus;
                        values.add(Map.of("str", uri));
                    }

                    default -> values.add(Map.of("str", ""));
                }

                row.put(prop, values);
            }

            for (String p : canonicalOrder) {
                row.putIfAbsent(p, List.of(Map.of("str", "")));
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("meta", meta);
        result.put("rows", rows);
        return result;
    }

    private boolean isValid(String s) {
        return s != null && !s.isBlank();
    }

    // =========================================================
    // 4. SUGGEST ENTITY
    // =========================================================
    @GetMapping(value = "/{thesaurus}/{lang}/suggest/entity", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "OpenRefine - Suggest entity",
            description = "Auto-complétion pour la réconciliation avec OpenRefine."
    )
    public Map<String, Object> suggestEntity(
            @PathVariable String thesaurus,
            @PathVariable String lang,
            @RequestParam String prefix
    ) {

        List<NodeAutoCompletion> data =
                restRDFHelper.searchAutoCompletionWS(prefix, lang, null, thesaurus, false);

        List<Map<String, Object>> result = new ArrayList<>();

        if (data != null) {
            for (NodeAutoCompletion c : data) {
                result.add(buildConcept(c, prefix, thesaurus, true));
            }
        }

        result.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));

        return Map.of("result", result);
    }

    // =========================================================
    // 5. PROPERTIES
    // =========================================================
    @GetMapping(value = "/suggest/properties", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> suggestProperties() {
        return Map.of("result", propertyList());
    }

    @GetMapping(value = "/propose_properties", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> propose() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", Map.of("id", "concept", "name", "Concept"));
        result.put("properties", propertyList());
        return result;
    }

    private List<Map<String, String>> propertyList() {
        return List.of(
                Map.of("id", "prefLabel", "name", "Preferred label"),
                Map.of("id", "description", "name", "Description"),
                Map.of("id", "aliases", "name", "Alternative labels"),
                Map.of("id", "ark", "name", "ARK Identifier"),
                Map.of("id", "uri", "name", "URI")
        );
    }

    // =========================================================
    // 6. PREVIEW
    // =========================================================
    @GetMapping(value = "/preview/{thesaurus}/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(
            summary = "OpenRefine - Preview",
            description = "Aperçu HTML affiché dans l'iframe de réconciliation OpenRefine."
    )
    public ResponseEntity<String> preview(
            @PathVariable String thesaurus,
            @PathVariable String id
    ) {

        NodeFullConcept c = restRDFHelper.getNodeFullConcept(thesaurus, id, "fr");

        String label = "";
        String definition = "";

        if (c != null) {
            if (c.getPrefLabel() != null) {
                label = safe(c.getPrefLabel().getLabel());
            }
            definition = safeNote(c.getDefinitions());
        }

        String html =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<style>" +
                        "body { font-family: Arial; font-size: 12px; margin: 6px; }" +
                        "h4 { font-size: 13px; margin: 0 0 4px 0; }" +
                        "p { font-size: 12px; margin: 2px 0; color: #444; }" +
                        "hr { margin: 6px 0; border: none; border-top: 1px solid #ddd; }" +
                        "small { font-size: 11px; color: #666; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<h4>" + HtmlUtils.htmlEscape(label) + "</h4>" +
                        "<p>" + HtmlUtils.htmlEscape(definition) + "</p>" +
                        "<hr/>" +
                        "<small>ID: " + HtmlUtils.htmlEscape(id) + "</small>" +
                        "</body></html>";

        return ResponseEntity.ok()
                .header("Content-Security-Policy", "frame-ancestors *")
                .body(html);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String safeNote(List<ConceptNote> notes) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        return notes.get(0).getLabel();
    }

    // =========================================================
    // 7. RESULT BUILDER
    // =========================================================
    private Map<String, Object> buildResult(
            List<NodeAutoCompletion> data,
            String query,
            String thesaurus,
            boolean suggestMode,
            int limit
    ) {

        List<Map<String, Object>> results = new ArrayList<>();

        if (data != null) {
            for (NodeAutoCompletion c : data) {
                results.add(buildConcept(c, query, thesaurus, suggestMode));
            }
        }

        results.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));

        if (limit > 0 && results.size() > limit) {
            results = new ArrayList<>(results.subList(0, limit));
        }

        markBestMatch(results);

        return Map.of("result", results);
    }

    /**
     * Ne marque "match": true que si un seul candidat dépasse le seuil,
     * ou si le premier score dépasse nettement le second (évite les faux
     * appariements automatiques entre homonymes proches).
     */
    private void markBestMatch(List<Map<String, Object>> results) {

        for (Map<String, Object> r : results) {
            r.put("match", false);
        }

        if (results.isEmpty()) {
            return;
        }

        int top = (int) results.get(0).get("score");
        if (top < 95) {
            return;
        }

        if (results.size() == 1) {
            results.get(0).put("match", true);
            return;
        }

        int second = (int) results.get(1).get("score");
        if (top - second >= 10) {
            results.get(0).put("match", true);
        }
    }

    private Map<String, Object> buildConcept(
            NodeAutoCompletion c,
            String query,
            String thesaurus,
            boolean suggestMode
    ) {

        int score = computeScore(c.getPrefLabel(), query, suggestMode);
        String base = base();

        Map<String, Object> m = new LinkedHashMap<>();

        // =========================
        // CORE
        // =========================
        m.put("id", c.getIdConcept());
        m.put("name", c.getPrefLabel());
        m.put("score", score);
        m.put("match", false); // ajusté ensuite par markBestMatch()

        m.put("type", List.of(
                Map.of("id", "concept", "name", "Concept")
        ));

        // =========================
        // URI STABLE (ARK en priorité si disponible)
        // =========================
        boolean hasArk = c.getIdArk() != null && !c.getIdArk().isBlank();

        m.put("uri", hasArk
                ? c.getIdArk()
                : base + "/resource/" + thesaurus + "/" + c.getIdConcept());

        if (hasArk) {
            m.put("persistentIdentifier", c.getIdArk());
        }

        // =========================
        // DESCRIPTION (tooltip OpenRefine)
        // =========================
        if (c.getDefinition() != null && !c.getDefinition().isBlank()) {
            m.put("description", c.getDefinition());
        }

        // =========================
        // ALIASES
        // =========================
        if (c.getAltLabelValue() != null && !c.getAltLabelValue().isBlank()) {

            List<String> aliases = Arrays.stream(c.getAltLabelValue().split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

            if (!aliases.isEmpty()) {
                m.put("aliases", aliases);
            }
        }

        // =========================
        // PREVIEW
        // =========================
        m.put("preview", Map.of(
                "url", base + "/api/v2/preview/" + thesaurus + "/" + c.getIdConcept(),
                "height", 120,
                "width", 400
        ));

        return m;
    }

    // =========================================================
    // 8. SCORE ENGINE
    // =========================================================
    private int computeScore(String label, String query, boolean suggestMode) {

        String l = normalize(label);
        String q = normalize(query);

        if (l.isEmpty() || q.isEmpty()) return 0;

        if (l.equals(q)) return 100;

        if (!suggestMode) {
            int max = Math.max(l.length(), q.length());
            int dist = levenshtein.apply(l, q);
            double sim = 1.0 - ((double) dist / max);
            return (int) (sim * 100);
        }

        // =========================
        // SUGGEST MODE (REBALANCED)
        // =========================
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
        return (s == null) ? "" :
                s.toLowerCase()
                        .trim()
                        .replaceAll("[_\\-]", " ")
                        .replaceAll("\\s+", " ");
    }
}