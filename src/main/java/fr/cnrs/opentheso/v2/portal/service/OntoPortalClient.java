package fr.cnrs.opentheso.v2.portal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OntoPortalClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public OntoPortalClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    OntoPortalClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public enum OntologyProbe {
        PRESENT,
        ABSENT,
        UNREADABLE
    }

    public boolean ontologyExists(String portalUrl, String apiKey, String acronym) {
        return probeOntology(portalUrl, apiKey, acronym) == OntologyProbe.PRESENT;
    }

    public OntologyProbe probeOntology(String portalUrl, String apiKey, String acronym) {
        URI uri = URI.create(withApiKey(ontologyUrl(portalUrl, acronym), apiKey));
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", authorization(apiKey))
                .header("Accept", "application/json")
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build());
        if (response.statusCode() == 405 || response.statusCode() == 501) {
            response = send(HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", authorization(apiKey))
                    .header("Accept", "application/json")
                    .GET()
                    .build());
        }
        int status = response.statusCode();
        if (status == 200) {
            return OntologyProbe.PRESENT;
        }
        if (status == 404) {
            return OntologyProbe.ABSENT;
        }
        if (status == 401 || status == 403) {
            throw httpError("l'authentification au portail", status, response.body());
        }
        OntologyProbe listed = lookupInOntologyList(portalUrl, apiKey, acronym);
        if (listed != OntologyProbe.UNREADABLE) {
            return listed;
        }
        if (status >= 500) {
            return OntologyProbe.UNREADABLE;
        }
        throw httpError("la lecture de l'ontologie", status, response.body());
    }

    /**
     * @return {@code true} si l'ontologie vient d'être créée, {@code false} si elle existait déjà
     */
    public boolean createOntology(String portalUrl, String apiKey, String acronym, String name, String username) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("acronym", acronym);
            payload.put("name", name);
            payload.put("administeredBy", List.of(StringUtils.trimToEmpty(username)));
            String json = objectMapper.writeValueAsString(payload);
            String url = withApiKey(ontologyUrl(portalUrl, acronym), apiKey);
            logOutgoingRequest("PUT", url, json);
            HttpResponse<String> response = sendPost(HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", authorization(apiKey))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build());
            log.info("HSPortal réponse PUT {} {}", response.statusCode(), maskApiKey(url));
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return true;
            }
            if (alreadyExists(status, response.body())) {
                return false;
            }
            throw httpError("la création de l'ontologie", status, response.body());
        } catch (InvalidToolboxDataException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrap("Impossible de créer l'ontologie sur le portail", ex);
        }
    }

    /**
     * @return {@code true} si l'ontologie a été supprimée, {@code false} si elle n'existait déjà plus
     */
    public boolean deleteOntology(String portalUrl, String apiKey, String acronym) {
        String url = withApiKey(ontologyUrl(portalUrl, acronym), apiKey);
        log.info("HSPortal requête DELETE {}", maskApiKey(url));
        HttpResponse<String> response = sendPost(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", authorization(apiKey))
                .header("Accept", "application/json")
                .DELETE()
                .build());
        log.info("HSPortal réponse DELETE {} {}", response.statusCode(), maskApiKey(url));
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return true;
        }
        if (status == 404) {
            return false;
        }
        throw httpError("la suppression de l'ontologie", status, response.body());
    }

    public void submitSkos(
            String portalUrl,
            String apiKey,
            String acronym,
            String contactName,
            String contactEmail,
            String released,
            String pullLocation,
            String ontologyUri,
            String description
    ) {
        if (StringUtils.isBlank(pullLocation) || !pullLocation.regionMatches(true, 0, "http", 0, 4)) {
            throw new InvalidToolboxDataException(
                    "L'URL SKOS publique (pullLocation) est obligatoire pour HSPortal");
        }
        String uri = StringUtils.defaultIfBlank(ontologyUri, pullLocation);
        if (!uri.regionMatches(true, 0, "http", 0, 4)) {
            throw new InvalidToolboxDataException(
                    "L'URI de l'ontologie (ConceptScheme) est obligatoire pour HSPortal");
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contact", List.of(Map.of(
                    "name", contactName,
                    "email", contactEmail
            )));
            payload.put("ontology", ontologyUrl(portalUrl, acronym));
            payload.put("hasOntologyLanguage", "SKOS");
            payload.put("released", dateTime(released));
            payload.put("pullLocation", pullLocation);
            payload.put("URI", uri);
            payload.put("status", "production");
            payload.put("description", StringUtils.defaultIfBlank(
                    description, "Publication OpenTheso " + acronym));
            String json = objectMapper.writeValueAsString(payload);
            String url = withApiKey(join(portalUrl, "/ontologies/" + encode(acronym) + "/submissions"), apiKey);
            logOutgoingRequest("POST", url, json);
            HttpResponse<String> response = sendPost(HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .header("Authorization", authorization(apiKey))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build());
            log.info("HSPortal réponse POST {} {}", response.statusCode(), maskApiKey(url));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw httpError("la publication SKOS", response.statusCode(), response.body());
            }
        } catch (InvalidToolboxDataException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrap("Impossible d'enregistrer la soumission SKOS", ex);
        }
    }

    private void logOutgoingRequest(String method, String url, String json) {
        log.info("HSPortal requête {} {}{}", method, maskApiKey(url), prettyJson(json));
    }

    private String prettyJson(String json) {
        try {
            return System.lineSeparator() + objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(json));
        } catch (Exception ex) {
            return " " + json;
        }
    }

    static String maskApiKey(String url) {
        return StringUtils.defaultString(url).replaceAll("(?i)([?&]apikey=)[^&]*", "$1***");
    }

    static String withApiKey(String url, String apiKey) {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(apiKey) || url.contains("apikey=")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + "apikey=" + encode(apiKey);
    }

    /**
     * L'UI OntoPortal (ex. hsportal.espadon.net) n'accepte pas les POST API :
     * le REST est sur {@code data.} + même hôte.
     */
    static String resolveApiBase(String portalUrl) {
        String base = StringUtils.removeEnd(StringUtils.trimToEmpty(portalUrl), "/");
        if (StringUtils.isBlank(base)) {
            throw new InvalidToolboxDataException("L'URL du portail est obligatoire");
        }
        URI uri = URI.create(base);
        String host = uri.getHost();
        if (host == null || host.startsWith("data.")) {
            return base;
        }
        return rebuildHost(uri, "data." + host);
    }

    static String resolveUiBase(String portalUrl) {
        String base = StringUtils.removeEnd(StringUtils.trimToEmpty(portalUrl), "/");
        if (StringUtils.isBlank(base)) {
            return base;
        }
        URI uri = URI.create(base);
        String host = uri.getHost();
        if (host != null && host.startsWith("data.")) {
            return rebuildHost(uri, host.substring("data.".length()));
        }
        return base;
    }

    private static String rebuildHost(URI uri, String host) {
        String scheme = StringUtils.defaultIfBlank(uri.getScheme(), "https");
        String rebuilt = scheme + "://" + host;
        if (uri.getPort() > 0) {
            rebuilt += ":" + uri.getPort();
        }
        return rebuilt;
    }

    static String join(String portalUrl, String path) {
        return resolveApiBase(portalUrl) + path;
    }

    static String ontologyUrl(String portalUrl, String acronym) {
        return join(portalUrl, "/ontologies/" + encode(acronym));
    }

    public static String publicOntologyUrl(String portalUrl, String acronym) {
        return resolveUiBase(portalUrl) + "/ontologies/" + encode(acronym);
    }

    private OntologyProbe lookupInOntologyList(String portalUrl, String apiKey, String acronym) {
        try {
            HttpResponse<String> response = send(HttpRequest.newBuilder()
                    .uri(URI.create(withApiKey(join(portalUrl, "/ontologies?include=acronym"), apiKey)))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", authorization(apiKey))
                    .header("Accept", "application/json")
                    .GET()
                    .build());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return OntologyProbe.UNREADABLE;
            }
            return listContainsAcronym(response.body(), acronym)
                    ? OntologyProbe.PRESENT
                    : OntologyProbe.ABSENT;
        } catch (RuntimeException ex) {
            return OntologyProbe.UNREADABLE;
        }
    }

    private boolean listContainsAcronym(String body, String acronym) {
        try {
            JsonNode root = objectMapper.readTree(StringUtils.defaultIfBlank(body, "[]"));
            JsonNode items = root.isArray() ? root : root.path("collection");
            if (!items.isArray()) {
                return false;
            }
            String expected = StringUtils.upperCase(StringUtils.trimToEmpty(acronym));
            for (JsonNode item : items) {
                if (expected.equals(StringUtils.upperCase(item.path("acronym").asText()))) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean alreadyExists(int status, String body) {
        if (status == 409) {
            return true;
        }
        String lower = StringUtils.defaultString(body).toLowerCase();
        return lower.contains("already") || lower.contains("taken") || lower.contains("exist");
    }

    static String userIri(String portalUrl, String username) {
        return resolveApiBase(portalUrl) + "/users/" + encode(StringUtils.trimToEmpty(username));
    }

    static String dateTime(String released) {
        String value = StringUtils.trimToEmpty(released);
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return value + "T00:00:00Z";
        }
        return StringUtils.defaultIfBlank(value, LocalDate.now() + "T00:00:00Z");
    }

    private HttpResponse<String> sendPost(HttpRequest request) {
        HttpResponse<String> response = send(request);
        int hops = 0;
        while (isRedirect(response.statusCode()) && hops++ < 4) {
            URI next = locationOf(request.uri(), response);
            if (next == null) {
                break;
            }
            if (isLoginRedirect(next)) {
                throw new InvalidToolboxDataException(
                        "Le portail a redirigé vers la page de connexion. Vérifiez la clé API.");
            }
            if (!shouldReplayPost(request.uri(), next)) {
                throw new InvalidToolboxDataException(
                        "Le portail a redirigé la publication vers " + next
                                + ". Vérifiez l'URL du portail et la clé API.");
            }
            request = retarget(request, next);
            response = send(request);
        }
        return response;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw wrap("Impossible de contacter le portail", ex);
        } catch (Exception ex) {
            throw wrap("Impossible de contacter le portail", ex);
        }
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static URI locationOf(URI current, HttpResponse<?> response) {
        return response.headers().firstValue("Location")
                .or(() -> response.headers().firstValue("location"))
                .map(StringUtils::trimToNull)
                .map(current::resolve)
                .orElse(null);
    }

    private static boolean isLoginRedirect(URI uri) {
        String path = StringUtils.defaultString(uri.getPath()).toLowerCase();
        return path.contains("login") || path.contains("session") || path.contains("sign_in")
                || path.contains("signin") || path.contains("users/password");
    }

    private static boolean shouldReplayPost(URI from, URI to) {
        String fromPath = stripSlash(from.getPath());
        String toPath = stripSlash(to.getPath());
        return fromPath.equals(toPath)
                || toPath.equals(fromPath + ".json")
                || toPath.startsWith(fromPath + "/");
    }

    private static String stripSlash(String path) {
        return StringUtils.removeEnd(StringUtils.defaultString(path), "/");
    }

    private static HttpRequest retarget(HttpRequest original, URI next) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(next)
                .timeout(original.timeout().orElse(Duration.ofMinutes(5)))
                .method(original.method(), original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));
        original.headers().map().forEach((name, values) -> {
            if (isHopByHop(name)) {
                return;
            }
            values.forEach(value -> builder.header(name, value));
        });
        return builder.build();
    }

    private static boolean isHopByHop(String header) {
        String name = StringUtils.defaultString(header).toLowerCase();
        return "host".equals(name) || "connection".equals(name) || "content-length".equals(name)
                || "expect".equals(name) || "upgrade".equals(name);
    }

    private static String authorization(String apiKey) {
        return "apikey token=" + apiKey;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static InvalidToolboxDataException httpError(String action, int status, String body) {
        return new InvalidToolboxDataException(
                "Erreur HTTP " + status + " lors de " + action + " : " + readableBody(body, status));
    }

    static String readableBody(String body, int status) {
        String text = StringUtils.defaultString(body);
        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
            return "le portail a renvoyé une redirection. "
                    + "Vérifiez l'URL du portail et la clé API.";
        }
        if (status >= 500 && (StringUtils.containsIgnoreCase(text, "<h1>")
                || StringUtils.containsIgnoreCase(text, "Internal Server Error")
                || StringUtils.containsIgnoreCase(text, "<html")
                || StringUtils.containsIgnoreCase(text, "administrator of this website"))) {
            return "le portail a échoué en enregistrant la soumission. "
                    + "Vérifiez le compte, la clé API et que l'URL SKOS est publique.";
        }
        if (StringUtils.containsIgnoreCase(text, "<html")
                || StringUtils.containsIgnoreCase(text, "administrator of this website")) {
            return "réponse HTML inattendue du portail.";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return StringUtils.abbreviate(StringUtils.defaultIfBlank(compact, "sans détail"), 280);
    }

    private static InvalidToolboxDataException wrap(String message, Exception ex) {
        if (ex instanceof InvalidToolboxDataException invalid) {
            return invalid;
        }
        return new InvalidToolboxDataException(message + ": " + ex.getMessage());
    }
}
