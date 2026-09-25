package fr.cnrs.opentheso.v2.portal.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosUriSupport;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

@Service
public class ThesaurusPortalPublishService {

    public static final String DEFAULT_PORTAL_URL = "https://hsportal.espadon.net";
    static final long DEFAULT_CREATE_PAUSE_MS = 1500L;

    private final OntoPortalClient ontoPortalClient;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final long createPauseMs;

    @Autowired
    public ThesaurusPortalPublishService(
            OntoPortalClient ontoPortalClient,
            ToolboxPreferencePersistence toolboxPreferencePersistence
    ) {
        this(ontoPortalClient, toolboxPreferencePersistence, DEFAULT_CREATE_PAUSE_MS);
    }

    ThesaurusPortalPublishService(
            OntoPortalClient ontoPortalClient,
            ToolboxPreferencePersistence toolboxPreferencePersistence,
            long createPauseMs
    ) {
        this.ontoPortalClient = ontoPortalClient;
        this.toolboxPreferencePersistence = toolboxPreferencePersistence;
        this.createPauseMs = createPauseMs;
    }

    public record PortalConfig(
            String portalUrl,
            String apiKey,
            String acronym,
            String username,
            String contactName,
            String contactEmail
    ) {
    }

    public record PublishResult(boolean created, String ontologyUrl, String pullLocation) {
    }

    public PortalConfig loadConfig(String thesaurusId) {
        Preferences prefs = requirePreferences(thesaurusId);
        return new PortalConfig(
                StringUtils.defaultIfBlank(prefs.getPortalUrl(), DEFAULT_PORTAL_URL),
                prefs.getPortalApiKey(),
                prefs.getPortalAcronym(),
                prefs.getPortalUsername(),
                prefs.getPortalContactName(),
                prefs.getPortalContactEmail()
        );
    }

    public LocalDateTime lastSyncAt(String thesaurusId) {
        Preferences prefs = toolboxPreferencePersistence.findPreferences(thesaurusId);
        return prefs == null ? null : prefs.getPortalLastSyncAt();
    }

    public void saveLink(String thesaurusId, PortalConfig config) {
        requirePreferences(thesaurusId);
        PortalConfig normalized = validate(config, false, false);
        try {
            toolboxPreferencePersistence.updatePortalLink(
                    thesaurusId,
                    normalized.portalUrl(),
                    normalized.apiKey(),
                    normalized.acronym(),
                    normalized.username(),
                    normalized.contactName(),
                    normalized.contactEmail()
            );
        } catch (RuntimeException ex) {
            throw new InvalidToolboxDataException(
                    "Enregistrement du lien portail impossible: "
                            + StringUtils.defaultIfBlank(ex.getMessage(), "erreur inconnue"));
        }
    }

    public PublishResult publish(String thesaurusId, String thesaurusTitle, PortalConfig config) {
        return publish(thesaurusId, thesaurusTitle, config, null);
    }

    public PublishResult publish(
            String thesaurusId,
            String thesaurusTitle,
            PortalConfig config,
            BiConsumer<Integer, String> progress
    ) {
        report(progress, 8, "v2.portal.progress.prepare");
        PortalConfig normalized = validate(config, true, true);
        saveLink(thesaurusId, normalized);
        report(progress, 18, "v2.portal.progress.export");
        Preferences prefs = requirePreferences(thesaurusId);
        String pullLocation = resolvePullLocation(prefs, thesaurusId);
        String ontologyUri = resolveOntologyUri(prefs, thesaurusId, pullLocation);
        report(progress, 42, "v2.portal.progress.check");
        boolean created = ensureOntology(normalized, thesaurusTitle, progress);
        if (created) {
            pauseAfterCreate();
        }
        report(progress, 70, "v2.portal.progress.submit");
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        ontoPortalClient.submitSkos(
                normalized.portalUrl(),
                normalized.apiKey(),
                normalized.acronym(),
                normalized.contactName(),
                normalized.contactEmail(),
                today,
                pullLocation,
                ontologyUri,
                StringUtils.defaultIfBlank(thesaurusTitle, normalized.acronym())
        );
        report(progress, 92, "v2.portal.progress.finish");
        try {
            toolboxPreferencePersistence.updatePortalLastSyncAt(thesaurusId, LocalDateTime.now());
        } catch (RuntimeException ex) {
            throw new InvalidToolboxDataException(
                    "Publication réussie, mais la date n'a pas pu être enregistrée: "
                            + StringUtils.defaultIfBlank(ex.getMessage(), "erreur inconnue"));
        }
        report(progress, 100, created ? "v2.portal.progress.doneCreated" : "v2.portal.progress.done");
        return new PublishResult(
                created,
                OntoPortalClient.publicOntologyUrl(normalized.portalUrl(), normalized.acronym()),
                pullLocation
        );
    }

    public void unpublish(String thesaurusId, PortalConfig config) {
        unpublish(thesaurusId, config, null);
    }

    public void unpublish(String thesaurusId, PortalConfig config, BiConsumer<Integer, String> progress) {
        report(progress, 8, "v2.portal.progress.remove");
        PortalConfig normalized = validate(config, true, false);
        saveLink(thesaurusId, normalized);
        report(progress, 45, "v2.portal.progress.removeRemote");
        ontoPortalClient.deleteOntology(normalized.portalUrl(), normalized.apiKey(), normalized.acronym());
        report(progress, 85, "v2.portal.progress.finish");
        try {
            toolboxPreferencePersistence.updatePortalLastSyncAt(thesaurusId, null);
        } catch (RuntimeException ex) {
            throw new InvalidToolboxDataException(
                    "Suppression réussie, mais la date n'a pas pu être effacée: "
                            + StringUtils.defaultIfBlank(ex.getMessage(), "erreur inconnue"));
        }
        report(progress, 100, "v2.portal.progress.removed");
    }

    static String resolvePullLocation(Preferences prefs, String thesaurusId) {
        if (prefs == null || StringUtils.isBlank(thesaurusId)) {
            throw new InvalidToolboxDataException("Thésaurus manquant");
        }
        String base = StringUtils.removeEnd(StringUtils.trimToEmpty(
                StringUtils.defaultIfBlank(prefs.getCheminSite(), prefs.getOriginalUri())), "/");
        if (StringUtils.isBlank(base) || !base.regionMatches(true, 0, "http", 0, 4)) {
            throw new InvalidToolboxDataException(
                    "L'URL publique du site (chemin du site) est obligatoire pour que HSPortal puisse tirer le SKOS");
        }
        return base + "/openapi/v1/thesaurus/" + thesaurusId.trim();
    }

    static String resolveOntologyUri(Preferences prefs, String thesaurusId, String pullLocation) {
        String uri = ThesaurusSkosUriSupport.uriFromId(
                thesaurusId, prefs, ThesaurusSkosUriSupport.resolveBaseUrl(prefs));
        if (StringUtils.isNotBlank(uri) && uri.regionMatches(true, 0, "http", 0, 4)) {
            return uri;
        }
        return pullLocation;
    }

    private boolean ensureOntology(
            PortalConfig config,
            String thesaurusTitle,
            BiConsumer<Integer, String> progress
    ) {
        OntoPortalClient.OntologyProbe probe = ontoPortalClient.probeOntology(
                config.portalUrl(), config.apiKey(), config.acronym());
        if (probe == OntoPortalClient.OntologyProbe.PRESENT) {
            return false;
        }
        report(progress, 55, "v2.portal.progress.create");
        try {
            return ontoPortalClient.createOntology(
                    config.portalUrl(),
                    config.apiKey(),
                    config.acronym(),
                    StringUtils.defaultIfBlank(thesaurusTitle, config.acronym()),
                    config.username()
            );
        } catch (RuntimeException ex) {
            if (probe == OntoPortalClient.OntologyProbe.UNREADABLE) {
                return false;
            }
            throw ex;
        }
    }

    private void pauseAfterCreate() {
        if (createPauseMs <= 0) {
            return;
        }
        try {
            Thread.sleep(createPauseMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InvalidToolboxDataException("Publication interrompue");
        }
    }

    private static void report(BiConsumer<Integer, String> progress, int percent, String message) {
        if (progress != null) {
            progress.accept(percent, message);
        }
    }

    private PortalConfig validate(PortalConfig config, boolean requireApiKey, boolean requireContact) {
        if (config == null) {
            throw new InvalidToolboxDataException("La configuration du portail est obligatoire");
        }
        String url = StringUtils.removeEnd(StringUtils.trimToEmpty(config.portalUrl()), "/");
        if (StringUtils.isBlank(url)) {
            url = DEFAULT_PORTAL_URL;
        }
        String acronym = StringUtils.trimToEmpty(config.acronym()).toUpperCase();
        if (StringUtils.isBlank(acronym)) {
            throw new InvalidToolboxDataException("L'acronyme du portail est obligatoire");
        }
        if (requireApiKey && StringUtils.isBlank(config.apiKey())) {
            throw new InvalidToolboxDataException("La clé API du portail est obligatoire");
        }
        if (requireContact && StringUtils.isBlank(config.username())) {
            throw new InvalidToolboxDataException("L'identifiant du compte portail est obligatoire");
        }
        if (requireContact && StringUtils.isBlank(config.contactName())) {
            throw new InvalidToolboxDataException("Le nom du contact est obligatoire");
        }
        if (requireContact && StringUtils.isBlank(config.contactEmail())) {
            throw new InvalidToolboxDataException("L'email du contact est obligatoire");
        }
        return new PortalConfig(
                url,
                StringUtils.trimToNull(config.apiKey()),
                acronym,
                StringUtils.trimToNull(config.username()),
                StringUtils.trimToNull(config.contactName()),
                StringUtils.trimToNull(config.contactEmail())
        );
    }

    private Preferences requirePreferences(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new InvalidToolboxDataException("Thésaurus manquant");
        }
        Preferences prefs = toolboxPreferencePersistence.findPreferences(thesaurusId);
        if (prefs == null) {
            throw new InvalidToolboxDataException("Préférences introuvables pour le thésaurus");
        }
        return prefs;
    }
}
