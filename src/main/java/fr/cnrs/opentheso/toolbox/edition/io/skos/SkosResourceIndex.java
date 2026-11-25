package fr.cnrs.opentheso.toolbox.edition.io.skos;

import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Table URI → ressource (type + identifiant local) d'un document SKOS entièrement lu.
 * <p>L'identifiant vient de {@code dcterms:identifier}, sinon de l'URI. La table est construite
 * avant toute écriture : l'ordre des ressources dans le fichier n'a donc pas d'importance.</p>
 */
final class SkosResourceIndex {

    private static final String ARK_MARKER = "ark:/";

    enum Kind { SCHEME, CONCEPT, GROUP, FACET }

    record Ref(Kind kind, String id) {
    }

    private final UnaryOperator<String> idFromUri;
    private final Map<String, Ref> byUri = new HashMap<>();
    /** Même ressource citée avec une autre base d'URI (uriArk / originalUri). */
    private final Map<String, Ref> byArk = new HashMap<>();
    private final Map<String, String> groupUriById = new HashMap<>();
    private String duplicateGroupId;

    SkosResourceIndex(SKOSXmlDocument document, UnaryOperator<String> idFromUri) {
        this.idFromUri = idFromUri;
        if (document == null) {
            return;
        }
        if (document.getConceptScheme() != null) {
            add(document.getConceptScheme(), Kind.SCHEME);
        }
        addAll(document.getConceptList(), Kind.CONCEPT);
        addAll(document.getFacetList(), Kind.FACET);
        addAll(document.getGroupList(), Kind.GROUP);
    }

    Optional<Ref> find(String uri) {
        if (uri == null) {
            return Optional.empty();
        }
        Ref ref = byUri.get(uri);
        if (ref == null) {
            String ark = arkOf(uri);
            ref = ark == null ? null : byArk.get(ark);
        }
        return Optional.ofNullable(ref);
    }

    /** Identifiant d'une ressource selon son type ; celui d'une collection est toujours en minuscules. */
    String idOf(SKOSResource resource, Kind kind) {
        String id = StringUtils.trimToNull(resource.getIdentifier());
        if (id == null) {
            id = StringUtils.defaultIfBlank(idFromUri.apply(resource.getUri()), resource.getUri());
        }
        return kind == Kind.GROUP ? normalizeGroupId(id) : id;
    }

    boolean containsGroup(String groupId) {
        return groupUriById.containsKey(groupId);
    }

    /** Identifiant partagé par deux collections différentes, s'il y en a un. */
    Optional<String> duplicateGroupId() {
        return Optional.ofNullable(duplicateGroupId);
    }

    static String normalizeGroupId(String id) {
        return id == null ? null : id.toLowerCase(Locale.ROOT);
    }

    private void addAll(List<SKOSResource> resources, Kind kind) {
        if (resources != null) {
            resources.forEach(resource -> add(resource, kind));
        }
    }

    private void add(SKOSResource resource, Kind kind) {
        String uri = resource.getUri();
        if (StringUtils.isBlank(uri)) {
            return;
        }
        Ref ref = new Ref(kind, idOf(resource, kind));
        byUri.put(uri, ref);
        String ark = arkOf(uri);
        if (ark != null) {
            byArk.put(ark, ref);
        }
        if (kind == Kind.GROUP) {
            String previousUri = groupUriById.putIfAbsent(ref.id(), uri);
            if (previousUri != null && !previousUri.equals(uri)) {
                duplicateGroupId = ref.id();
            }
        }
    }

    private static String arkOf(String uri) {
        int index = uri.indexOf(ARK_MARKER);
        return index < 0 ? null : uri.substring(index + ARK_MARKER.length());
    }
}
