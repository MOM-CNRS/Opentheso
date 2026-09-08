package fr.cnrs.opentheso.v2.toolbox.edition.io.skos;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.ConceptGroupLabelHistorique;
import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.RelationGroup;
import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.entites.ThesaurusLabel;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.group.ConceptGroupLabel;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.skosapi.SKOSAgent;
import fr.cnrs.opentheso.models.skosapi.SKOSDate;
import fr.cnrs.opentheso.models.skosapi.SKOSDocumentation;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSMatch;
import fr.cnrs.opentheso.models.skosapi.SKOSNotation;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSRelation;
import fr.cnrs.opentheso.models.skosapi.SKOSReplaces;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import fr.cnrs.opentheso.v2.shared.time.V2Dates;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusEditionSkosImportEngine {

    private static final String SEPERATEUR = "##";
    private static final String SOUS_SEPERATEUR = "@@";
    private static final String IDENTIFIER_HANDLE = "handle";
    private static final String NOTE_DEFINITION = "definition";
    private static final String NOTE_SCOPE = "scopeNote";
    private static final String NOTE_EXAMPLE = "example";
    private static final String NOTE_HISTORY = "historyNote";
    private static final String NOTE_EDITORIAL = "editorialNote";
    private static final String NOTE_CHANGE = "changeNote";
    private static final String NOTE_GENERIC = "note";

    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final PreferencesRepository preferencesRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptFacetRepository conceptFacetRepository;
    private final ExternalResourcesRepository externalResourcesRepository;
    private final ImagesRepository imagesRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final ConceptGroupLabelHistoriqueRepository conceptGroupLabelHistoriqueRepository;
    private final RelationGroupRepository relationGroupRepository;
    private final NoteRepository noteRepository;


    private final Set<String> idGroups = new HashSet<>();
    private int idUser;
    private int idGroupUser;
    private final Set<String> idLangsFound = new HashSet<>();
    private Set<String> hasTopConcceptList = new HashSet<>();
    private String langueSource;
    private String formatDate;
    private String selectedIdentifier;
    private String prefixHandle;
    private String prefixDoi;
    private Preferences nodePreference;
    private StringBuilder message = new StringBuilder();
    private HashMap<String, String> memberHashMap = new HashMap<>();
    private HashMap<String, String> groupSubGroup = new HashMap<>(); // pour garder en mémoire les relations de types (member) pour détecter ce qui est groupe ou concept
    private final List<ConceptGroupConcept> pendingGroupConcepts = new ArrayList<>();
    private SKOSXmlDocument skosXmlDocument;
    private DateTimeFormatter dateFormatter;
    boolean isFirst = true;
    /** Rôle à appliquer à l'import (false = esclave par défaut). */
    private boolean importAsMaster;


    public void setInfos(String formatDate, int idUser, int idGroupUser, String langueSource) {
        this.formatDate = formatDate;
        this.idUser = idUser;
        this.idGroupUser = idGroupUser;
        this.langueSource = langueSource;
        this.isFirst = true;
        this.idGroups.clear();
        this.idLangsFound.clear();
        this.hasTopConcceptList.clear();
        this.memberHashMap.clear();
        this.groupSubGroup.clear();
        this.pendingGroupConcepts.clear();
        this.message = new StringBuilder();
        this.dateFormatter = DateTimeFormatter.ofPattern(StringUtils.defaultIfBlank(formatDate, "yyyy-MM-dd"));
        this.importAsMaster = false;
    }

    public String addThesaurus() {

        SKOSResource conceptScheme = skosXmlDocument.getConceptScheme();
        if (conceptScheme == null) {
            message.append("Erreur SKOS !!! manque balise conceptSheme");
            return null;
        }

        Thesaurus thesaurus = conceptScheme.getThesaurus();
        applyConceptSchemeAgents(conceptScheme, thesaurus);

        String idTheso1;
        langueSource = StringUtils.defaultIfBlank(normalizeLangCode(langueSource), "fr");
        if (thesaurus.getLanguage() == null) {
            thesaurus.setLanguage(langueSource);
        }
        if ((idTheso1 = toolboxThesaurusPersistence.createThesaurusId()) == null) {
            message.append("Erreur lors de la création du thésaurus");
            return null;
        }

        // Titre initial : prefLabel source, puis dcterms:title, puis défaut
        String dctermsTitle = StringUtils.trimToNull(thesaurus.getTitle());
        if (looksLikeUri(dctermsTitle)) {
            dctermsTitle = null;
            thesaurus.setTitle(null);
        }
        String displayTitle = resolveImportDisplayTitle(conceptScheme, dctermsTitle, idTheso1);
        thesaurus.setId_thesaurus(idTheso1);
        thesaurus.setTitle(displayTitle);

        persistConceptSchemeDcTerms(idTheso1);
        displayTitle = persistConceptSchemeTitles(thesaurus, displayTitle);

        // ajouter le thésaurus dans le group de l'utilisateur
        if (idGroupUser != -1) { // si le groupeUser = - 1, c'est le cas d'un SuperAdmin, alors on n'intègre pas le thésaurus dans un groupUser
            var userGroupThesaurus = UserGroupThesaurus.builder().idThesaurus(thesaurus.getId_thesaurus()).idGroup(idGroupUser).build();
            userGroupThesaurusRepository.save(userGroupThesaurus);
        }

        for (SKOSRelation relation : skosXmlDocument.getConceptScheme().getRelationsList()) {
            hasTopConcceptList.add(relation.getTargetUri());
        }
        initPreferencesThesaurus(
                idTheso1,
                nodePreference.getPreferredName() == null ? displayTitle : nodePreference.getPreferredName()
        );
        initPreferencesThesaurus(idTheso1, displayTitle);
        captureMasterLinkFromConceptScheme(conceptScheme, idTheso1);
        return idTheso1;
    }

    private void applyConceptSchemeAgents(SKOSResource conceptScheme, Thesaurus thesaurus) {
        String creator = "";
        String contributor = "";
        for (SKOSAgent agent : conceptScheme.getAgentList()) {
            if (agent.getProperty() == SKOSProperty.CREATOR) {
                creator = agent.getAgent();
            } else if (agent.getProperty() == SKOSProperty.CONTRIBUTOR) {
                contributor = agent.getAgent();
            }
        }
        thesaurus.setCreator(creator);
        thesaurus.setContributor(contributor);
    }

    private void persistConceptSchemeDcTerms(String idTheso) {
        for (DcElement dcElement : dedupeSingularThesaurusDcTerms(
                skosXmlDocument.getConceptScheme().getThesaurus().getDcElement())) {
            try {
                thesaurusDcTermRepository.save(ThesaurusDcTerm.builder()
                        .idThesaurus(idTheso)
                        .name(dcElement.getName())
                        .value(dcElement.getValue())
                        .language(dcElement.getLanguage())
                        .dataType(dcElement.getType())
                        .build());
            } catch (DataIntegrityViolationException e) {
                // terme DC déjà présent : ignoré
            }
        }
    }

    private String persistConceptSchemeTitles(Thesaurus thesaurus, String displayTitle) {
        TitlePersistState state = persistPrefLabelTitles(thesaurus, displayTitle);
        if (!state.titlePersisted() && StringUtils.isNotBlank(state.displayTitle())) {
            persistFallbackTitle(thesaurus, state.displayTitle());
            return state.displayTitle();
        }
        if (!state.titlePersisted()) {
            persistFallbackTitle(thesaurus, state.displayTitle());
            return state.displayTitle();
        }
        if (!state.sourceLangTitlePersisted()) {
            persistFallbackTitle(thesaurus, state.displayTitle());
        }
        return state.displayTitle();
    }

    private TitlePersistState persistPrefLabelTitles(Thesaurus thesaurus, String displayTitle) {
        boolean titlePersisted = false;
        boolean sourceLangTitlePersisted = false;
        Set<String> persistedLangs = new HashSet<>();
        for (SKOSLabel label : skosXmlDocument.getConceptScheme().getLabelsList()) {
            String updatedTitle = persistOnePrefLabel(thesaurus, label, persistedLangs);
            if (updatedTitle == null) {
                continue;
            }
            titlePersisted = true;
            if (langueSource.equalsIgnoreCase(normalizeLangCode(label.getLanguage()))) {
                sourceLangTitlePersisted = true;
                displayTitle = updatedTitle;
            }
        }
        return new TitlePersistState(titlePersisted, sourceLangTitlePersisted, displayTitle);
    }

    private String persistOnePrefLabel(Thesaurus thesaurus, SKOSLabel label, Set<String> persistedLangs) {
        if (StringUtils.isBlank(label.getLabel()) || looksLikeUri(label.getLabel())) {
            return null;
        }
        String labelLang = normalizeLangCode(label.getLanguage());
        if (!persistedLangs.add(labelLang)) {
            return null;
        }
        thesaurus.setTitle(label.getLabel().trim());
        thesaurus.setLanguage(labelLang);
        toolboxThesaurusPersistence.addTranslation(thesaurus);
        return label.getLabel().trim();
    }

    private void persistFallbackTitle(Thesaurus thesaurus, String displayTitle) {
        thesaurus.setTitle(displayTitle);
        thesaurus.setLanguage(langueSource);
        toolboxThesaurusPersistence.addTranslation(thesaurus);
    }

    private record TitlePersistState(boolean titlePersisted, boolean sourceLangTitlePersisted, String displayTitle) {
    }

    private void captureMasterLinkFromConceptScheme(SKOSResource conceptScheme, String localThesaurusId) {
        if (conceptScheme == null || StringUtils.isBlank(conceptScheme.getUri())) {
            return;
        }
        String uri = conceptScheme.getUri().trim();
        String masterThesaurusId = extractQueryParam(uri, "idt");
        String masterServerUrl = extractServerBaseUrl(uri);
        if (StringUtils.isAnyBlank(masterServerUrl, masterThesaurusId)) {
            return;
        }
        toolboxPreferencePersistence.updateMasterLink(localThesaurusId, masterServerUrl, masterThesaurusId, null);
    }

    private String extractQueryParam(String uri, String paramName) {
        int queryIndex = uri.indexOf('?');
        if (queryIndex < 0 || queryIndex >= uri.length() - 1) {
            return null;
        }
        String query = uri.substring(queryIndex + 1);
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && paramName.equalsIgnoreCase(pair[0].trim())) {
                return pair[1].trim();
            }
        }
        return null;
    }

    private String extractServerBaseUrl(String uri) {
        try {
            java.net.URI parsed = java.net.URI.create(uri);
            if (StringUtils.isBlank(parsed.getScheme()) || StringUtils.isBlank(parsed.getHost())) {
                return null;
            }
            StringBuilder base = new StringBuilder();
            base.append(parsed.getScheme()).append("://").append(parsed.getHost());
            if (parsed.getPort() > 0) {
                base.append(':').append(parsed.getPort());
            }
            // Garder le contexte applicatif s'il existe (ex. /opentheso)
            String path = parsed.getPath();
            if (StringUtils.isNotBlank(path) && path.contains("/")) {
                // Retirer le dernier segment ressource éventuel, garder le préfixe applicatif.
                String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
                int lastSlash = normalized.lastIndexOf('/');
                if (lastSlash > 0) {
                    base.append(normalized.substring(0, lastSlash));
                }
            }
            return base.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveImportDisplayTitle(SKOSResource conceptScheme, String dctermsTitle, String idTheso) {
        String fromLabels = findDisplayTitleFromLabels(conceptScheme);
        if (fromLabels != null) {
            return fromLabels;
        }
        if (StringUtils.isNotBlank(dctermsTitle)) {
            return dctermsTitle.trim();
        }
        String fromDc = findDisplayTitleFromDc(conceptScheme);
        if (fromDc != null) {
            return fromDc;
        }
        return "theso_" + idTheso;
    }

    private String findDisplayTitleFromLabels(SKOSResource conceptScheme) {
        if (conceptScheme.getLabelsList() == null) {
            return null;
        }
        for (SKOSLabel label : conceptScheme.getLabelsList()) {
            if (StringUtils.isBlank(label.getLabel()) || looksLikeUri(label.getLabel())) {
                continue;
            }
            if (langueSource.equalsIgnoreCase(normalizeLangCode(label.getLanguage()))) {
                return label.getLabel().trim();
            }
        }
        for (SKOSLabel label : conceptScheme.getLabelsList()) {
            if (StringUtils.isNotBlank(label.getLabel()) && !looksLikeUri(label.getLabel())) {
                return label.getLabel().trim();
            }
        }
        return null;
    }

    private String findDisplayTitleFromDc(SKOSResource conceptScheme) {
        if (conceptScheme.getThesaurus() == null || CollectionUtils.isEmpty(conceptScheme.getThesaurus().getDcElement())) {
            return null;
        }
        for (DcElement dcElement : conceptScheme.getThesaurus().getDcElement()) {
            if ("title".equalsIgnoreCase(dcElement.getName())
                    && StringUtils.isNotBlank(dcElement.getValue())
                    && !looksLikeUri(dcElement.getValue())) {
                return dcElement.getValue().trim();
            }
        }
        return null;
    }

    private String normalizeLangCode(String lang) {
        if (StringUtils.isBlank(lang)) {
            return langueSource;
        }
        String normalized = lang.trim().toLowerCase();
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            normalized = normalized.substring(0, dash);
        }
        return normalized;
    }

    private boolean looksLikeUri(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("urn:");
    }

    /**
     * Pour {@code created}/{@code modified}, ne conserve qu'une seule valeur (la plus récente).
     * Les autres propriétés DC peuvent rester multi-valuées.
     */
    static java.util.List<DcElement> dedupeSingularThesaurusDcTerms(java.util.List<DcElement> source) {
        if (source == null || source.isEmpty()) {
            return java.util.List.of();
        }
        java.util.Map<String, DcElement> singularBest = new java.util.LinkedHashMap<>();
        java.util.List<DcElement> others = new java.util.ArrayList<>();
        for (DcElement element : source) {
            if (element == null || StringUtils.isBlank(element.getName())) {
                continue;
            }
            String name = element.getName().trim().toLowerCase();
            if ("created".equals(name) || "modified".equals(name)) {
                DcElement previous = singularBest.get(name);
                if (previous == null || isNewerDcDate(element.getValue(), previous.getValue())) {
                    singularBest.put(name, element);
                }
            } else {
                others.add(element);
            }
        }
        java.util.List<DcElement> result = new java.util.ArrayList<>(others.size() + singularBest.size());
        result.addAll(others);
        result.addAll(singularBest.values());
        return result;
    }

    private static boolean isNewerDcDate(String candidate, String current) {
        if (StringUtils.isBlank(candidate)) {
            return false;
        }
        if (StringUtils.isBlank(current)) {
            return true;
        }
        try {
            return java.time.Instant.parse(normalizeDcDate(candidate))
                    .isAfter(java.time.Instant.parse(normalizeDcDate(current)));
        } catch (Exception ignored) {
            // Comparaison textuelle en dernier recours (ISO-like).
            return candidate.compareTo(current) > 0;
        }
    }

    private static String normalizeDcDate(String value) {
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return trimmed + "T00:00:00Z";
        }
        if (trimmed.endsWith("Z") || trimmed.contains("+") || trimmed.matches(".*[+-]\\d{2}:\\d{2}$")) {
            return trimmed;
        }
        return trimmed + "Z";
    }

    private void initPreferencesThesaurus(String idThesaurus, String preferredTitle) {
        langueSource = StringUtils.isEmpty(langueSource) ? "fr" : langueSource;
        toolboxPreferencePersistence.initPreferences(idThesaurus, langueSource);
        toolboxPreferencePersistence.updatePreferredName(idThesaurus, preferredTitle);
        nodePreference = toolboxPreferencePersistence.findPreferences(idThesaurus);
        if (selectedIdentifier.equalsIgnoreCase("ark")) {
            nodePreference.setOriginalUriIsArk(true);
        }
        if (selectedIdentifier.equalsIgnoreCase(IDENTIFIER_HANDLE)) {
            nodePreference.setOriginalUriIsHandle(true);
        }
        if (selectedIdentifier.equalsIgnoreCase("doi")) {
            nodePreference.setOriginalUriIsDoi(true);
        }
        nodePreference.setMaster(importAsMaster);
        preferencesRepository.save(nodePreference);
    }

    private void setOriginalUri(String uri) {

        if (nodePreference == null) {
            return;
        }
        nodePreference.setCheminSite(uri+"/");
        // Ne pas écraser le nom préféré avec l'identifiant technique
        nodePreference.setOriginalUri(uri);
        preferencesRepository.save(nodePreference);
    }

    public void addGroups(List<SKOSResource> groupResource, String idTheso) {
        for (SKOSResource group : groupResource) {
            addOneGroup(group, idTheso);
        }
        addGroupConceptGroup(idTheso);
        flushPendingGroupConcepts();
    }

    private void addOneGroup(SKOSResource group, String idTheso) {
        var idGroup = getIdFromUri(group.getUri());
        if (idGroup == null || idGroup.isEmpty()) {
            idGroup = group.getUri();
        }
        String notationValue = firstNotationValue(group);
        String type = groupTypeCode(group.getProperty());
        String idArkHandle = resolveSelectedIdentifier(group.getUri());
        ParsedDates dates = parseSkosDates(group.getDateList());
        try {
            insertGroup(idGroup, idTheso, idArkHandle, type, notationValue, dates.created(), dates.modified());
        } catch (Exception ex) {
            log.error(ex.getMessage());
            insertGroup(idGroup, idTheso, idArkHandle, type, notationValue, dates.created(), dates.modified());
        }
        idGroups.add(idGroup);
        addGroupRelations(group, idGroup, idTheso);
        addGroupLabels(group, idGroup, idTheso);
        addGroupNotesFromResource(group, idGroup, idTheso);
    }

    private String firstNotationValue(SKOSResource group) {
        var notationList = group.getNotationList();
        if (notationList == null || notationList.isEmpty()) {
            return "";
        }
        SKOSNotation notation = notationList.get(0);
        return notation == null ? "" : notation.getNotation();
    }

    private String groupTypeCode(int property) {
        return switch (property) {
            case SKOSProperty.COLLECTION -> "C";
            case SKOSProperty.CONCEPT_GROUP -> "G";
            case SKOSProperty.THEME -> "T";
            default -> "MT";
        };
    }

    private String resolveSelectedIdentifier(String uri) {
        if (StringUtils.isEmpty(selectedIdentifier)) {
            return "";
        }
        String idArkHandle = null;
        if (selectedIdentifier.equalsIgnoreCase("ark")) {
            idArkHandle = getIdArkFromUri(uri);
        }
        if (selectedIdentifier.equalsIgnoreCase(IDENTIFIER_HANDLE)) {
            idArkHandle = getIdHandleFromUri(uri);
        }
        if (selectedIdentifier.equalsIgnoreCase("doi")) {
            idArkHandle = getIdDoiFromUri(uri);
        }
        return idArkHandle == null ? "" : idArkHandle;
    }

    private void addGroupRelations(SKOSResource group, String idGroup, String idTheso) {
        for (SKOSRelation relation : group.getRelationsList()) {
            addOneGroupRelation(relation, idGroup, idTheso);
        }
    }

    private void addOneGroupRelation(SKOSRelation relation, String idGroup, String idTheso) {
        int prop = relation.getProperty();
        if (prop == SKOSProperty.SUBGROUP) {
            addSubGroup(idGroup, getIdFromUri(relation.getTargetUri()), idTheso);
            return;
        }
        if (prop == SKOSProperty.MEMBER) {
            groupSubGroup.put(getOriginalId(relation.getTargetUri()), idGroup);
        }
    }

    private void addGroupLabels(SKOSResource group, String idGroup, String idTheso) {
        for (SKOSLabel label : group.getLabelsList()) {
            ConceptGroupLabel conceptGroupLabel = new ConceptGroupLabel();
            conceptGroupLabel.setIdgroup(idGroup);
            conceptGroupLabel.setIdthesaurus(idTheso);
            conceptGroupLabel.setLang(label.getLanguage());
            conceptGroupLabel.setLexicalValue(label.getLabel());
            addGroupTraduction(conceptGroupLabel, idUser);
        }
    }

    private void addGroupNotesFromResource(SKOSResource group, String idGroup, String idTheso) {
        for (SKOSDocumentation documentation : group.getDocumentationsList()) {
            addGroupNote(idGroup, documentation.getLanguage(), idTheso, documentation.getText(),
                    toNoteTypeCode(documentation.getProperty()));
        }
    }

    private String toNoteTypeCode(int prop) {
        return getString("", prop);
    }

    private String getString(String noteTypeCode, int prop) {
        switch (prop) {
            case SKOSProperty.DEFINITION:
                return NOTE_DEFINITION;
            case SKOSProperty.SCOPE_NOTE:
                return NOTE_SCOPE;
            case SKOSProperty.EXAMPLE:
                return NOTE_EXAMPLE;
            case SKOSProperty.HISTORY_NOTE:
                return NOTE_HISTORY;
            case SKOSProperty.EDITORIAL_NOTE:
                return NOTE_EDITORIAL;
            case SKOSProperty.CHANGE_NOTE:
                return NOTE_CHANGE;
            case SKOSProperty.NOTE:
                return NOTE_GENERIC;
            default:
                return noteTypeCode;
        }
    }

    private void addGroupConceptGroup(String idTheso) {
        // groupSubGroup : compositon du HashMap = idSubGroup(ou idConcept) -> idGroup
        // c'est pour séparer les concepts des groupes
        for (var entry : groupSubGroup.entrySet()) {
            String idSubGroup = entry.getKey();
            if (idGroups.contains(idSubGroup)) {
                // si la relation member est vers un sous groupe, alors on créé une relation groupe/sousGroupe
                addSubGroup(entry.getValue(), idSubGroup, idTheso);
            } else {
                queueConceptGroupConcept(entry.getValue(), idSubGroup, idTheso);
            }
        }
    }

    public void addConceptV2(SKOSResource conceptResource, String idTheso) {
        addConceptV2(conceptResource, idTheso, null);
    }

    public void importConcept(SKOSResource conceptResource, String idTheso, boolean asCandidate) {
        String idConcept = resolveConceptId(conceptResource);
        if (asCandidate && conceptRepository.existsByIdConceptAndIdThesaurus(idConcept, idTheso)) {
            return;
        }
        addConceptV2(conceptResource, idTheso, asCandidate ? "CA" : null);
    }

    public String resolveConceptId(SKOSResource conceptResource) {
        if (StringUtils.isEmpty(conceptResource.getIdentifier())) {
            return getOriginalId(conceptResource.getUri());
        }
        return conceptResource.getIdentifier();
    }

    private void addConceptV2(SKOSResource conceptResource, String idTheso, String forcedStatus) {
        String idConcept = resolveConceptId(conceptResource);
        rememberOriginalUriIfFirst(conceptResource);
        SkosLabelPayload labels = buildSkosLabels(conceptResource, idTheso, idConcept);
        SkosRelationPayload relations = buildSkosRelations(conceptResource, idConcept);
        ParsedDates dates = parseSkosDates(conceptResource.getDateList());
        String gps = buildSkosGps(conceptResource);
        conceptRepository.addNewConcept(
                idTheso,
                idConcept,
                idUser,
                resolveSkosConceptStatus(conceptResource, forcedStatus),
                "concept",
                lastNotation(conceptResource),
                "ark".equalsIgnoreCase(selectedIdentifier) ? getIdArkFromUri(conceptResource.getUri()) : "",
                relations.isTopConcept(),
                IDENTIFIER_HANDLE.equalsIgnoreCase(selectedIdentifier) ? getIdHandleFromUri(conceptResource.getUri()) : "",
                "doi".equalsIgnoreCase(selectedIdentifier) ? getIdDoiFromUri(conceptResource.getUri()) : "",
                labels.prefTerm(),
                relations.relations(),
                null,
                buildSkosNotes(conceptResource, idConcept),
                labels.nonPrefTerm(),
                buildSkosAlignments(conceptResource, idTheso, idConcept),
                buildSkosImages(conceptResource),
                buildSkosReplacedBy(conceptResource),
                gps != null,
                gps,
                V2Dates.toSqlDate(dates.created()),
                V2Dates.toSqlDate(dates.modified()),
                buildSkosDcterms(conceptResource));
        addExternalResources(idTheso, idConcept, conceptResource.getDcRelations());
    }

    private String resolveSkosConceptStatus(SKOSResource conceptResource, String forcedStatus) {
        if (StringUtils.isNotEmpty(forcedStatus)) {
            return forcedStatus;
        }
        return conceptResource.getStatus() == SKOSProperty.DEPRECATED ? "dep" : "";
    }

    private void rememberOriginalUriIfFirst(SKOSResource conceptResource) {
        if (!isFirst) {
            return;
        }
        isFirst = false;
        String uri = conceptResource.getUri().substring(0, conceptResource.getUri().lastIndexOf("/"));
        if (uri == null || uri.isEmpty()) {
            uri = conceptResource.getUri();
        }
        setOriginalUri(uri);
    }

    private String buildSkosImages(SKOSResource conceptResource) {
        if (CollectionUtils.isEmpty(conceptResource.getNodeImages())) {
            return null;
        }
        StringBuilder imagesBuilder = new StringBuilder();
        for (NodeImage nodeImage : conceptResource.getNodeImages()) {
            if (StringUtils.isNotEmpty(nodeImage.getUri())) {
                imagesBuilder.append(SEPERATEUR).append(nodeImage.getImageName())
                        .append(SOUS_SEPERATEUR).append(nodeImage.getCopyRight())
                        .append(SOUS_SEPERATEUR).append(nodeImage.getUri());
            }
        }
        return imagesBuilder.isEmpty() ? null : imagesBuilder.substring(SEPERATEUR.length());
    }

    private String buildSkosAlignments(SKOSResource conceptResource, String idTheso, String idConcept) {
        if (CollectionUtils.isEmpty(conceptResource.getMatchList())) {
            return null;
        }
        StringBuilder alignementsBuilder = new StringBuilder();
        for (SKOSMatch match : conceptResource.getMatchList()) {
            int idType = switch (match.getProperty()) {
                case SKOSProperty.CLOSE_MATCH -> 2;
                case SKOSProperty.EXACT_MATCH -> 1;
                case SKOSProperty.BROAD_MATCH -> 3;
                case SKOSProperty.NARROWER_MATCH -> 5;
                case SKOSProperty.RELATED_MATCH -> 4;
                default -> -1;
            };
            alignementsBuilder.append(SEPERATEUR).append(idUser).append(SOUS_SEPERATEUR).append("")
                    .append(SOUS_SEPERATEUR).append("")
                    .append(SOUS_SEPERATEUR).append(match.getValue()).append(SOUS_SEPERATEUR).append(idType)
                    .append(SOUS_SEPERATEUR).append(idTheso).append(SOUS_SEPERATEUR).append(idConcept);
        }
        return alignementsBuilder.isEmpty() ? null : alignementsBuilder.substring(SEPERATEUR.length());
    }

    private SkosLabelPayload buildSkosLabels(SKOSResource conceptResource, String idTheso, String idConcept) {
        if (CollectionUtils.isEmpty(conceptResource.getLabelsList())) {
            return new SkosLabelPayload(null, null);
        }
        StringBuilder nonPrefTermBuilder = new StringBuilder();
        StringBuilder prefTermBuilder = new StringBuilder();
        for (SKOSLabel label : conceptResource.getLabelsList()) {
            appendSkosLabel(label, idTheso, idConcept, prefTermBuilder, nonPrefTermBuilder);
        }
        return new SkosLabelPayload(
                prefTermBuilder.isEmpty() ? null : prefTermBuilder.substring(SEPERATEUR.length()),
                nonPrefTermBuilder.isEmpty() ? null : nonPrefTermBuilder.substring(SEPERATEUR.length())
        );
    }

    private void appendSkosLabel(SKOSLabel label, String idTheso, String idConcept,
                                 StringBuilder prefTermBuilder, StringBuilder nonPrefTermBuilder) {
        if (label.getProperty() == SKOSProperty.PREF_LABEL) {
            prefTermBuilder.append(SEPERATEUR).append(label.getLabel()).append(SOUS_SEPERATEUR).append(label.getLanguage());
        } else {
            String status = null;
            boolean hiden = false;
            if (label.getProperty() == SKOSProperty.ALT_LABEL) {
                status = "USE";
            } else if (label.getProperty() == SKOSProperty.HIDDEN_LABEL) {
                status = "Hidden";
                hiden = true;
            }
            nonPrefTermBuilder.append(SEPERATEUR).append(idConcept)
                    .append(SOUS_SEPERATEUR).append(label.getLabel())
                    .append(SOUS_SEPERATEUR).append(label.getLanguage())
                    .append(SOUS_SEPERATEUR).append(idTheso)
                    .append(SOUS_SEPERATEUR).append(idUser)
                    .append(SOUS_SEPERATEUR).append(status)
                    .append(SOUS_SEPERATEUR).append(hiden);
        }
        appendNewLang(label.getLanguage());
    }

    private SkosRelationPayload buildSkosRelations(SKOSResource conceptResource, String idConcept) {
        boolean isTopConcept = true;
        String relations = null;
        if (CollectionUtils.isNotEmpty(conceptResource.getRelationsList())) {
            StringBuilder relationsBuilder = new StringBuilder();
            for (SKOSRelation relation : conceptResource.getRelationsList()) {
                String role = roleForRelation(relation.getProperty());
                if (isBroaderRole(role)) {
                    isTopConcept = false;
                }
                if (!role.isEmpty()) {
                    relationsBuilder.append(SEPERATEUR).append(idConcept).append(SOUS_SEPERATEUR).append(role)
                            .append(SOUS_SEPERATEUR).append(getOriginalId(relation.getTargetUri()));
                }
            }
            if (!relationsBuilder.isEmpty()) {
                relations = relationsBuilder.substring(SEPERATEUR.length());
            }
        }
        if (hasTopConcceptList.contains(conceptResource.getUri())) {
            isTopConcept = true;
        }
        return new SkosRelationPayload(relations, isTopConcept);
    }

    private String roleForRelation(int property) {
        return switch (property) {
            case SKOSProperty.NARROWER -> "NT";
            case SKOSProperty.NARROWER_GENERIC -> "NTG";
            case SKOSProperty.NARROWER_PARTITIVE -> "NTP";
            case SKOSProperty.NARROWER_INSTANTIAL -> "NTI";
            case SKOSProperty.BROADER -> "BT";
            case SKOSProperty.BROADER_GENERIC -> "BTG";
            case SKOSProperty.BROADER_INSTANTIAL -> "BTI";
            case SKOSProperty.BROADER_PARTITIVE -> "BTP";
            case SKOSProperty.RELATED -> "RT";
            case SKOSProperty.RELATED_HAS_PART -> "RHP";
            case SKOSProperty.RELATED_PART_OF -> "RPO";
            default -> "";
        };
    }

    private boolean isBroaderRole(String role) {
        return "BT".equals(role) || "BTG".equals(role) || "BTI".equals(role) || "BTP".equals(role);
    }

    private String buildSkosNotes(SKOSResource conceptResource, String idConcept) {
        if (CollectionUtils.isEmpty(conceptResource.getDocumentationsList())) {
            return null;
        }
        StringBuilder notesBuilder = new StringBuilder();
        for (SKOSDocumentation documentation : conceptResource.getDocumentationsList()) {
            notesBuilder.append(SEPERATEUR).append(documentation.getText())
                    .append(SOUS_SEPERATEUR).append(toNoteTypeCode(documentation.getProperty()))
                    .append(SOUS_SEPERATEUR).append(documentation.getLanguage())
                    .append(SOUS_SEPERATEUR).append(idConcept);
        }
        return notesBuilder.isEmpty() ? null : notesBuilder.substring(SEPERATEUR.length());
    }

    private String lastNotation(SKOSResource conceptResource) {
        String notationConcept = "";
        if (CollectionUtils.isNotEmpty(conceptResource.getNotationList())) {
            for (SKOSNotation notation : conceptResource.getNotationList()) {
                notationConcept = notation.getNotation();
            }
        }
        return notationConcept;
    }

    private String buildSkosReplacedBy(SKOSResource conceptResource) {
        if (CollectionUtils.isEmpty(conceptResource.getsKOSReplaces())) {
            return null;
        }
        StringBuilder isReplacedByBuilder = new StringBuilder();
        for (SKOSReplaces replace : conceptResource.getsKOSReplaces()) {
            if (SKOSProperty.IS_REPLACED_BY == replace.getProperty()) {
                isReplacedByBuilder.append(SEPERATEUR).append(getOriginalId(replace.getTargetUri()));
            }
        }
        return isReplacedByBuilder.isEmpty() ? null : isReplacedByBuilder.substring(SEPERATEUR.length());
    }

    private String buildSkosDcterms(SKOSResource conceptResource) {
        StringBuilder dctermsBuilder = new StringBuilder();
        for (SKOSAgent agent : conceptResource.getAgentList()) {
            appendSkosAgent(dctermsBuilder, agent);
        }
        return dctermsBuilder.isEmpty() ? null : dctermsBuilder.toString();
    }

    private void appendSkosAgent(StringBuilder dctermsBuilder, SKOSAgent agent) {
        if (agent.getProperty() == SKOSProperty.CREATOR) {
            if (!dctermsBuilder.isEmpty()) {
                dctermsBuilder.append("##");
            }
            dctermsBuilder.append("creator@@").append(agent.getAgent()).append("@@fr");
            return;
        }
        if (agent.getProperty() == SKOSProperty.CONTRIBUTOR) {
            if (!dctermsBuilder.isEmpty()) {
                dctermsBuilder.append("##");
            }
            dctermsBuilder.append("contributor@@").append(agent.getAgent()).append("@@fr");
        }
    }

    private String buildSkosGps(SKOSResource conceptResource) {
        if (CollectionUtils.isEmpty(conceptResource.getGpsCoordinates())) {
            return null;
        }
        StringBuilder gpsBuilder = new StringBuilder();
        for (SKOSGPSCoordinates gpsValue : conceptResource.getGpsCoordinates()) {
            gpsBuilder.append(SEPERATEUR).append(gpsValue.getLat()).append(SOUS_SEPERATEUR).append(gpsValue.getLon());
        }
        return gpsBuilder.substring(SEPERATEUR.length());
    }

    private record SkosLabelPayload(String prefTerm, String nonPrefTerm) {
    }

    private record SkosRelationPayload(String relations, boolean isTopConcept) {
    }

    private void addExternalResources(String idTheso, String idConcept, ArrayList<String> externalRelations) {

        for (String externalRelation : externalRelations) {
            if (externalRelation == null || externalRelation.isEmpty()) {
                return;
            }
            if (!fr.cnrs.opentheso.utils.StringUtils.urlValidator(externalRelation)) {
                return;
            }

            externalResourcesRepository.save(ExternalResource.builder().idThesaurus(idTheso).idConcept(idConcept)
                    .externalUri(externalRelation).build());
        }
    }

    public void addFoafImages(List<SKOSResource> foafImages, String idTheso) {

        for (SKOSResource sKOSResource : foafImages) {
            if (sKOSResource.getFoafImage() == null) {
                return;
            }

            var imagesStr = fr.cnrs.opentheso.utils.StringUtils.convertString(sKOSResource.getFoafImage().getImageName())
                    + SOUS_SEPERATEUR + fr.cnrs.opentheso.utils.StringUtils.convertString(sKOSResource.getFoafImage().getCopyRight())
                    + SOUS_SEPERATEUR + sKOSResource.getUri()
                    + SOUS_SEPERATEUR + fr.cnrs.opentheso.utils.StringUtils.convertString(sKOSResource.getFoafImage().getCreator());

            if (StringUtils.isEmpty(imagesStr)) {
                return;
            }

            imagesRepository.addExternalImages(idTheso, sKOSResource.getIdentifier(), idUser, imagesStr);
        }
    }

    public void addFacetsV2(List<SKOSResource> facetResources, String idTheso) {
        for (SKOSResource facetSKOSResource : facetResources) {
            addOneFacet(facetSKOSResource, idTheso);
        }
    }

    private void addOneFacet(SKOSResource facetSKOSResource, String idTheso) {
        String idFacet = getIdFromUri(facetSKOSResource.getUri());
        if (idFacet == null) {
            return;
        }
        if (CollectionUtils.isEmpty(facetSKOSResource.getLabelsList())) {
            return;
        }
        String idConceptParent = findSuperOrdinate(facetSKOSResource);
        if (idConceptParent == null) {
            return;
        }
        String labelsValue = buildFacetLabels(facetSKOSResource);
        String notes = buildFacetNotes(facetSKOSResource, idFacet);
        String safeLabels = StringUtils.isNotEmpty(labelsValue) ? labelsValue.replace("'", "''") : null;
        String safeNotes = StringUtils.isNotEmpty(notes) ? notes.replace("'", "''") : null;
        conceptFacetRepository.addFacet(idFacet, idUser, idTheso, idConceptParent, safeLabels,
                buildFacetMembers(facetSKOSResource), safeNotes);
    }

    private String findSuperOrdinate(SKOSResource facetSKOSResource) {
        for (SKOSRelation relation : facetSKOSResource.getRelationsList()) {
            if (relation.getProperty() == SKOSProperty.SUPER_ORDINATE) {
                return getOriginalId(relation.getTargetUri());
            }
        }
        return null;
    }

    private String buildFacetLabels(SKOSResource facetSKOSResource) {
        StringBuilder labels = new StringBuilder();
        for (SKOSLabel sKOSLabel : facetSKOSResource.getLabelsList()) {
            labels.append(SEPERATEUR).append(sKOSLabel.getLabel()).append(SOUS_SEPERATEUR).append(sKOSLabel.getLanguage());
        }
        return labels.isEmpty() ? "" : labels.substring(2);
    }

    private String buildFacetMembers(SKOSResource facetSKOSResource) {
        if (CollectionUtils.isEmpty(facetSKOSResource.getRelationsList())) {
            return null;
        }
        StringBuilder membresBuilder = new StringBuilder();
        for (SKOSRelation member : facetSKOSResource.getRelationsList()) {
            if (member.getProperty() == SKOSProperty.MEMBER) {
                membresBuilder.append(SEPERATEUR).append(getOriginalId(member.getTargetUri()));
            }
        }
        return membresBuilder.isEmpty() ? null : membresBuilder.substring(2);
    }

    private String buildFacetNotes(SKOSResource facetSKOSResource, String idFacet) {
        if (CollectionUtils.isEmpty(facetSKOSResource.getDocumentationsList())) {
            return null;
        }
        StringBuilder notesBuilder = new StringBuilder();
        for (SKOSDocumentation documentation : facetSKOSResource.getDocumentationsList()) {
            notesBuilder.append(SEPERATEUR).append(documentation.getText())
                    .append(SOUS_SEPERATEUR).append(toNoteTypeCode(documentation.getProperty()))
                    .append(SOUS_SEPERATEUR).append(documentation.getLanguage())
                    .append(SOUS_SEPERATEUR).append(idFacet);
        }
        return notesBuilder.isEmpty() ? null : notesBuilder.substring(SEPERATEUR.length());
    }

    private void appendNewLang(String idLang) {
        if (idLang == null || idLang.isEmpty()) {
            return;
        }
        if (idLangsFound.contains(idLang)) {
            return;
        }
        idLangsFound.add(idLang);
    }

    private String getIdFromUri(String uri) {
        String extracted = extractQueryId(uri, "idc=");
        if (extracted == null) {
            extracted = extractQueryId(uri, "idg=");
        }
        if (extracted == null) {
            extracted = extractQueryId(uri, "idf=");
        }
        if (extracted == null) {
            extracted = uri.contains("#")
                    ? uri.substring(uri.indexOf("#") + 1)
                    : uri.substring(uri.lastIndexOf("/") + 1);
        }
        return fr.cnrs.opentheso.utils.StringUtils.normalizeStringForIdentifier(extracted);
    }

    private String extractQueryId(String uri, String key) {
        if (!uri.contains(key)) {
            return null;
        }
        if ("idc=".equals(key) && uri.contains("&")) {
            String str = uri.substring(uri.indexOf(key));
            return str.substring(4, str.indexOf("&"));
        }
        if (uri.contains("&")) {
            return uri.substring(uri.indexOf(key) + key.length(), uri.indexOf("&"));
        }
        return uri.substring(uri.indexOf(key) + key.length());
    }

    private String getOriginalId(String uri) {
        String originalId;
        if (skosXmlDocument.getEquivalenceUriArkHandle().isEmpty()
                || skosXmlDocument.getEquivalenceUriArkHandle().get(uri) == null) {
            return getIdFromUri(uri);
        }

        originalId = skosXmlDocument.getEquivalenceUriArkHandle().get(uri);
        if (originalId == null) {
            if (!message.isEmpty()) {
                message.append(System.lineSeparator());
            }
            message.append("Identifiant (DC:Identifier) non détecté pour l'URL:");
            message.append(uri);
            originalId = getIdFromUri(uri);
            return originalId;
        }
        return originalId;
    }

    private String getIdArkFromUri(String uri) {

        if (uri.contains("ark:/")) {
            return uri.substring(uri.indexOf("ark:/") + 5);
        }
        return "";
    }

    private String getIdHandleFromUri(String uri) {
        // URI de type Handle
        String id = null;
        if (prefixHandle == null) {
            return getIdFromUri(uri);
        }
        if (uri.contains(prefixHandle)) {
            id = uri.substring(uri.indexOf(prefixHandle));
        }
        if (id == null) {
            return getIdFromUri(uri);
        }
        return id;
    }

    private String getIdDoiFromUri(String uri) {
        // URI de type Doi
        String id = null;
        if (prefixDoi == null) {
            return getIdFromUri(uri);
        }
        if (uri.contains(prefixDoi)) {
            id = uri.substring(uri.indexOf(prefixDoi));
        }
        if (id == null) {
            return getIdFromUri(uri);
        }
        return id;
    }

    public void addLangsToThesaurus(String idTheso) {
        String primaryTitle = resolvePrimaryThesaurusTitle(idTheso);

        for (String idLang : idLangsFound) {
            if (thesaurusLabelRepository.findByIdThesaurusAndLang(idTheso, idLang).isEmpty()) {
                Thesaurus thesaurus1 = new Thesaurus();
                thesaurus1.setId_thesaurus(idTheso);
                thesaurus1.setContributor("");
                thesaurus1.setCoverage("");
                thesaurus1.setCreator("");
                thesaurus1.setDescription("");
                thesaurus1.setFormat("");
                thesaurus1.setLanguage(idLang);
                thesaurus1.setPublisher("");
                thesaurus1.setRelation("");
                thesaurus1.setRights("");
                thesaurus1.setSource("");
                thesaurus1.setSubject("");
                thesaurus1.setTitle(primaryTitle);
                thesaurus1.setType("");
                toolboxThesaurusPersistence.addTranslation(thesaurus1);
            }
        }
    }

    private String resolvePrimaryThesaurusTitle(String idTheso) {
        if (StringUtils.isNotBlank(langueSource)) {
            var sourceLabel = thesaurusLabelRepository.findByIdThesaurusAndLang(idTheso, langueSource);
            if (sourceLabel.isPresent() && StringUtils.isNotBlank(sourceLabel.get().getTitle())) {
                return sourceLabel.get().getTitle();
            }
        }
        return thesaurusLabelRepository.findByIdThesaurus(idTheso).stream()
                .map(ThesaurusLabel::getTitle)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("theso_" + idTheso);
    }

    public void setRdf4jThesaurus(SKOSXmlDocument rdf4jThesaurus) {
        this.skosXmlDocument = rdf4jThesaurus;
    }

    private void insertGroup(String idGroup, String idThesaurus, String idArk, String typeCode, String notation,
                             Instant created, Instant modified) {
        Instant now = V2Dates.nowInstant();
        conceptGroupRepository.save(ConceptGroup.builder()
                .id(conceptGroupRepository.getNextConceptGroupSequence().intValue())
                .idGroup(idGroup.toLowerCase())
                .idArk(StringUtils.defaultString(idArk))
                .idThesaurus(idThesaurus)
                .idTypeCode(typeCode)
                .notation(notation)
                .idHandle("")
                .idDoi("")
                .created(V2Dates.toUtilDate(created == null ? now : created))
                .modified(V2Dates.toUtilDate(modified == null ? now : modified))
                .build());
    }

    private void queueConceptGroupConcept(String idGroup, String idConcept, String idThesaurus) {
        pendingGroupConcepts.add(ConceptGroupConcept.builder()
                .idGroup(idGroup)
                .idThesaurus(idThesaurus)
                .idConcept(idConcept)
                .build());
    }

    private void flushPendingGroupConcepts() {
        if (pendingGroupConcepts.isEmpty()) {
            return;
        }
        conceptGroupConceptRepository.saveAll(pendingGroupConcepts);
        pendingGroupConcepts.clear();
    }

    private void addSubGroup(String fatherGroupId, String childGroupId, String thesaurusId) {
        relationGroupRepository.save(RelationGroup.builder()
                .idGroup1(fatherGroupId.toLowerCase())
                .idThesaurus(thesaurusId)
                .relation("sub")
                .idGroup2(childGroupId.toLowerCase())
                .build());
    }

    private void addGroupTraduction(ConceptGroupLabel conceptGroupLabel, int userId) {
        conceptGroupLabel.setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(conceptGroupLabel.getLexicalValue()));
        conceptGroupLabelRepository.save(fr.cnrs.opentheso.entites.ConceptGroupLabel.builder()
                .lexicalValue(conceptGroupLabel.getLexicalValue())
                .lang(conceptGroupLabel.getLang())
                .idThesaurus(conceptGroupLabel.getIdthesaurus())
                .idGroup(conceptGroupLabel.getIdgroup().toLowerCase())
                .created(V2Dates.nowUtilDate())
                .modified(V2Dates.nowUtilDate())
                .build());
        conceptGroupLabelHistoriqueRepository.save(ConceptGroupLabelHistorique.builder()
                .lexicalValue(conceptGroupLabel.getLexicalValue())
                .lang(conceptGroupLabel.getLang())
                .idThesaurus(conceptGroupLabel.getIdthesaurus())
                .idGroup(conceptGroupLabel.getIdgroup().toLowerCase())
                .idUser(userId)
                .modified(V2Dates.nowUtilDate())
                .build());
    }

    private void addGroupNote(String idGroup, String idLang, String idTheso, String text, String noteTypeCode) {
        // Import d'un thésaurus neuf : pas de find-before-insert (évite N+1)
        String lexicalValue = StringEscapeUtils.unescapeXml(
                fr.cnrs.opentheso.utils.StringUtils.clearNoteFromP(
                        fr.cnrs.opentheso.utils.StringUtils.clearValue(text)));
        noteRepository.save(Note.builder()
                .noteTypeCode(noteTypeCode)
                .idThesaurus(idTheso)
                .lang(idLang)
                .lexicalValue(lexicalValue)
                .identifier(idGroup)
                .noteSource("")
                .idUser(idUser)
                .created(V2Dates.nowUtilDate())
                .modified(V2Dates.nowUtilDate())
                .build());
    }

    private ParsedDates parseSkosDates(List<SKOSDate> dates) {
        ensureDateFormatter();
        Instant created = null;
        Instant modified = null;
        if (dates == null) {
            return new ParsedDates(null, null);
        }
        for (SKOSDate date : dates) {
            if (date.getDate() == null || date.getDate().isEmpty()) {
                continue;
            }
            Instant parsed = parseToInstant(date.getDate());
            if (date.getProperty() == SKOSProperty.CREATED) {
                created = parsed;
            }
            if (date.getProperty() == SKOSProperty.MODIFIED) {
                modified = parsed;
            }
        }
        return new ParsedDates(created, modified);
    }

    private void ensureDateFormatter() {
        if (dateFormatter != null) {
            return;
        }
        if (StringUtils.isEmpty(formatDate)) {
            formatDate = "dd-mm-yyyy";
        }
        dateFormatter = DateTimeFormatter.ofPattern(formatDate);
    }

    private Instant parseToInstant(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            TemporalAccessor parsed = dateFormatter.parse(value);
            if (parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
                return Instant.from(parsed);
            }
            if (parsed.isSupported(ChronoField.EPOCH_DAY)) {
                return LocalDate.from(parsed).atStartOfDay(V2Dates.zone()).toInstant();
            }
            return LocalDateTime.from(parsed).atZone(V2Dates.zone()).toInstant();
        } catch (DateTimeParseException ex) {
            Logger.getLogger(ThesaurusEditionSkosImportEngine.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }


    /** PostgreSQL attend des {@code date} ; {@link Instant} est converti en type date. */

    private record ParsedDates(Instant created, Instant modified) {
    }
}
