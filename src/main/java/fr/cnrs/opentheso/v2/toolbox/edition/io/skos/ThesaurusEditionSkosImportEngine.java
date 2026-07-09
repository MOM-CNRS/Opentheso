package fr.cnrs.opentheso.v2.toolbox.edition.io.skos;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.ConceptGroupLabelHistorique;
import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.RelationGroup;
import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
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
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusEditionSkosImportEngine {

    private static final String SEPERATEUR = "##";
    private static final String SOUS_SEPERATEUR = "@@";

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


    private List<String> idGroups = new ArrayList<>();
    private int idUser, idGroupUser;
    private List<String> idLangsFound = new ArrayList<>(), hasTopConcceptList = new ArrayList<>();;
    private String langueSource, formatDate, selectedIdentifier, prefixHandle, prefixDoi;
    private Preferences nodePreference;
    private StringBuilder message = new StringBuilder();
    private HashMap<String, String> memberHashMap = new HashMap<>();
    private HashMap<String, String> groupSubGroup = new HashMap<>(); // pour garder en mémoire les relations de types (member) pour détecter ce qui est groupe ou concept
    private SKOSXmlDocument skosXmlDocument;
    boolean isFirst = true;


    public void setInfos(String formatDate, int idUser, int idGroupUser, String langueSource) {
        this.formatDate = formatDate;
        this.idUser = idUser;
        this.idGroupUser = idGroupUser;
        this.langueSource = langueSource;
        this.isFirst = true;
    }

    public String addThesaurus() throws SQLException {

        SKOSResource conceptScheme = skosXmlDocument.getConceptScheme();
        if (conceptScheme == null) {
            message.append("Erreur SKOS !!! manque balise conceptSheme");
            return null;
        }

        Thesaurus thesaurus = conceptScheme.getThesaurus();

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

        String idTheso1;
        if (thesaurus.getLanguage() == null) {
            thesaurus.setLanguage(langueSource);
        }
        if ((idTheso1 = toolboxThesaurusPersistence.createThesaurusId()) == null) {
            message.append("Erreur lors de la création du thésaurus");
            return null;
        }

        // Si le Titre du thésaurus n'est pas detecter, on donne un nom par defaut
        if (skosXmlDocument.getConceptScheme().getLabelsList().isEmpty()) {
            if (thesaurus.getTitle().isEmpty()) {
                thesaurus.setTitle("theso_" + idTheso1);
            }
        }
        thesaurus.setId_thesaurus(idTheso1);

        // intégration des métadonnées DC
        for (DcElement dcElement : skosXmlDocument.getConceptScheme().getThesaurus().getDcElement()) {
            try {
                thesaurusDcTermRepository.save(ThesaurusDcTerm.builder()
                        .idThesaurus(idTheso1)
                        .name(dcElement.getName())
                        .value(dcElement.getValue())
                        .language(dcElement.getLanguage())
                        .dataType(dcElement.getType())
                        .build());
            } catch (DataIntegrityViolationException e) {

            }
        }

        // boucler pour les traductions
        for (SKOSLabel label : skosXmlDocument.getConceptScheme().getLabelsList()) {
            thesaurus.setTitle(label.getLabel());
            thesaurus.setLanguage(label.getLanguage());
            if (thesaurus.getLanguage() == null) {
                thesaurus.setLanguage("fr"); // cas où la langue n'est pas définie dans le SKOS
            }
            toolboxThesaurusPersistence.addTranslation(thesaurus);
        }

        // ajouter le thésaurus dans le group de l'utilisateur
        if (idGroupUser != -1) { // si le groupeUser = - 1, c'est le cas d'un SuperAdmin, alors on n'intègre pas le thésaurus dans un groupUser
            var userGroupThesaurus = UserGroupThesaurus.builder().idThesaurus(thesaurus.getId_thesaurus()).idGroup(idGroupUser).build();
            userGroupThesaurusRepository.save(userGroupThesaurus);
        }

        for (SKOSRelation relation : skosXmlDocument.getConceptScheme().getRelationsList()) {
            hasTopConcceptList.add(relation.getTargetUri());
        }
        initPreferencesThesaurus(idTheso1, skosXmlDocument.getTitle());
        return idTheso1;
    }

    private void initPreferencesThesaurus(String idThesaurus, String uri) {
        langueSource = StringUtils.isEmpty(langueSource) ? "fr" : langueSource;
        toolboxPreferencePersistence.initPreferences(idThesaurus, langueSource);
        nodePreference = toolboxPreferencePersistence.findPreferences(idThesaurus);
        nodePreference.setPreferredName(idThesaurus);
        if (selectedIdentifier.equalsIgnoreCase("ark")) {
            nodePreference.setOriginalUriIsArk(true);
        }
        if (selectedIdentifier.equalsIgnoreCase("handle")) {
            nodePreference.setOriginalUriIsHandle(true);
        }
        if (selectedIdentifier.equalsIgnoreCase("doi")) {
            nodePreference.setOriginalUriIsDoi(true);
        }
        preferencesRepository.save(nodePreference);
    }

    private void setOriginalUri(String idTheso, String uri) {

        if (nodePreference == null) {
            return;
        }
        nodePreference.setCheminSite(uri+"/");
        nodePreference.setPreferredName(idTheso);
        nodePreference.setOriginalUri(uri);
        preferencesRepository.save(nodePreference);
    }

    public void addGroups(ArrayList<SKOSResource> groupResource, String idTheso) {

        for (SKOSResource group : groupResource) {

            SKOSNotation notation = null;
            String idSubGroup;
            String idSubConcept;

            var idGroup = getIdFromUri(group.getUri());
            if (idGroup == null || idGroup.isEmpty()) {
                idGroup = group.getUri();
            }

            var notationList = group.getNotationList();
            if (notationList != null && !notationList.isEmpty()) {
                notation = notationList.get(0);
            }

            var notationValue = notation == null ? "" : notation.getNotation();

            var type = switch (group.getProperty()) {
                case SKOSProperty.COLLECTION -> "C";
                case SKOSProperty.CONCEPT_GROUP -> "G";
                case SKOSProperty.THEME -> "T";
                default -> "MT";
            };

            String idArkHandle = null;
            // option cochée
            if (!StringUtils.isEmpty(selectedIdentifier)) {
                if (selectedIdentifier.equalsIgnoreCase("ark")) {
                    idArkHandle = getIdArkFromUri(group.getUri());
                }
                if (selectedIdentifier.equalsIgnoreCase("handle")) {
                    idArkHandle = getIdHandleFromUri(group.getUri());
                }
                if (selectedIdentifier.equalsIgnoreCase("doi")) {
                    idArkHandle = getIdDoiFromUri(group.getUri());
                }
            }

            if (idArkHandle == null) {
                idArkHandle = "";
            }

            if (StringUtils.isEmpty(formatDate)) {
                formatDate = "dd-mm-yyyy";
            }
            Date created = null;
            Date modified = null;

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatDate);
            for (SKOSDate sKOSDate : group.getDateList()) {
                try {
                    if (!StringUtils.isEmpty(sKOSDate.getDate())) {
                        if (sKOSDate.getProperty() == SKOSProperty.CREATED) {
                            created = simpleDateFormat.parse(sKOSDate.getDate());
                        }
                        if (sKOSDate.getProperty() == SKOSProperty.MODIFIED) {
                            modified = simpleDateFormat.parse(sKOSDate.getDate());
                        }
                    }
                } catch (ParseException ex) {
                    Logger.getLogger(ThesaurusEditionSkosImportEngine.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            try {
                insertGroup(idGroup, idTheso, idArkHandle, type, notationValue, created, modified);
            } catch (Exception ex) {
                log.error(ex.getMessage());
                insertGroup(idGroup, idTheso, idArkHandle, type, notationValue, created, modified);
            }

            // group/sous_group
            for (SKOSRelation relation : group.getRelationsList()) {
                int prop = relation.getProperty();
                switch (prop) {
                    case SKOSProperty.SUBGROUP:
                        idSubGroup = getIdFromUri(relation.getTargetUri());
                        addSubGroup(idGroup, idSubGroup, idTheso);
                        break;
                    case SKOSProperty.MEMBER:
                        // Récupération de l'Id d'origine sauvegardé à l'import (idArk -> identifier)
                        idSubConcept = getOriginalId(relation.getTargetUri());
                        groupSubGroup.put(idSubConcept, idGroup);
                        saveConceptGroupConcept(idGroup, idSubConcept, idTheso);
                        break;
                    default:
                        break;
                }
            }

            for (SKOSLabel label : group.getLabelsList()) {
                // ajouter les traductions des Groupes
                ConceptGroupLabel conceptGroupLabel = new ConceptGroupLabel();
                conceptGroupLabel.setIdgroup(idGroup);
                conceptGroupLabel.setIdthesaurus(idTheso);
                conceptGroupLabel.setLang(label.getLanguage());
                conceptGroupLabel.setLexicalValue(label.getLabel());

                addGroupTraduction(conceptGroupLabel, idUser);
            }

            for (SKOSDocumentation documentation : group.getDocumentationsList()) {
                String noteTypeCode = "";
                int prop = documentation.getProperty();
                noteTypeCode = getString(noteTypeCode, prop);

                addGroupNote(idGroup, documentation.getLanguage(), idTheso, documentation.getText(), noteTypeCode);
            }
        }
        addGroupConceptGroup(idTheso);
    }

    private String getString(String noteTypeCode, int prop) {
        switch (prop) {
            case SKOSProperty.DEFINITION:
                noteTypeCode = "definition";
                break;
            case SKOSProperty.SCOPE_NOTE:
                noteTypeCode = "scopeNote";
                break;
            case SKOSProperty.EXAMPLE:
                noteTypeCode = "example";
                break;
            case SKOSProperty.HISTORY_NOTE:
                noteTypeCode = "historyNote";
                break;
            case SKOSProperty.EDITORIAL_NOTE:
                noteTypeCode = "editorialNote";
                break;
            case SKOSProperty.CHANGE_NOTE:
                noteTypeCode = "changeNote";
                break;
            case SKOSProperty.NOTE:
                noteTypeCode = "note";
                break;
        }
        return noteTypeCode;
    }

    private void addGroupConceptGroup(String idTheso) {
        // groupSubGroup : compositon du HashMap = idSubGroup(ou idConcept) -> idGroup
        // c'est pour séparer les concepts des groupes
        for (String idSubGroup : groupSubGroup.keySet()) {
            if (idGroups.contains(idSubGroup)) {
                // si la relation member est vers un sous groupe, alors on créé une relation groupe/sousGroupe
                addSubGroup(groupSubGroup.get(idSubGroup), idSubGroup, idTheso);
            } else {
                saveConceptGroupConcept(groupSubGroup.get(idSubGroup), idSubGroup, idTheso);
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

        String conceptStatus = "";
        if (StringUtils.isNotEmpty(forcedStatus)) {
            conceptStatus = forcedStatus;
        } else if (conceptResource.getStatus() == SKOSProperty.DEPRECATED) {
            conceptStatus = "dep";
        }

        // option cochée
        String idArk = "";
        if ("ark".equalsIgnoreCase(selectedIdentifier)) {
            idArk = getIdArkFromUri(conceptResource.getUri());
        }

        String idHandle = "";
        if ("handle".equalsIgnoreCase(selectedIdentifier)) {
            idHandle = getIdHandleFromUri(conceptResource.getUri());
        }

        String idDoi = "";
        if ("doi".equalsIgnoreCase(selectedIdentifier)) {
            idDoi = getIdDoiFromUri(conceptResource.getUri());
        }

        boolean isTopConcept = true;

        // IMAGES
        //-- 'url1##url2'
        String images = null;
        if (CollectionUtils.isNotEmpty(conceptResource.getNodeImages())) {
            images = "";
            for (NodeImage nodeImage : conceptResource.getNodeImages()) {
                if (StringUtils.isNotEmpty(nodeImage.getUri())) {
                    images = images + SEPERATEUR + nodeImage.getImageName() + SOUS_SEPERATEUR + nodeImage.getCopyRight() + SOUS_SEPERATEUR + nodeImage.getUri();
                }
            }
            if (!images.isEmpty()) {
                images = images.substring(SEPERATEUR.length());
            }
        }

        // ALIGNEMENT
        //-- 'author@concept_target@thesaurus_target@uri_target@alignement_id_type@internal_id_thesaurus@internal_id_concept'
        String alignements = null;
        if (CollectionUtils.isNotEmpty(conceptResource.getMatchList())) {
            alignements = "";
            for (SKOSMatch match : conceptResource.getMatchList()) {
                int id_type = -1;
                id_type = switch (match.getProperty()) {
                    case SKOSProperty.CLOSE_MATCH -> 2;
                    case SKOSProperty.EXACT_MATCH -> 1;
                    case SKOSProperty.BROAD_MATCH -> 3;
                    case SKOSProperty.NARROWER_MATCH -> 5;
                    case SKOSProperty.RELATED_MATCH -> 4;
                    default -> id_type;
                };

                alignements = alignements + SEPERATEUR + idUser + SOUS_SEPERATEUR + "" + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + match.getValue() + SOUS_SEPERATEUR + id_type
                        + SOUS_SEPERATEUR + idTheso + SOUS_SEPERATEUR + idConcept;
            }
            if (!alignements.isEmpty()) {
                alignements = alignements.substring(SEPERATEUR.length());
            }
        }

        //Non Pref Term
        //-- 'id_term@lexicalValue@lang@id_thesaurus@source@status@hiden'
        String nonPrefTerm = null;
        String prefTerm = null;
        if (CollectionUtils.isNotEmpty(conceptResource.getLabelsList())) {
            nonPrefTerm = "";
            prefTerm = "";
            for (SKOSLabel label : conceptResource.getLabelsList()) {
                if (label.getProperty() == SKOSProperty.PREF_LABEL) {
                    prefTerm += SEPERATEUR + label.getLabel() + SOUS_SEPERATEUR + label.getLanguage();
                } else {
                    String status = null;
                    boolean hiden = false;
                    if (label.getProperty() == SKOSProperty.ALT_LABEL) {
                        status = "USE";
                    } else if (label.getProperty() == SKOSProperty.HIDDEN_LABEL) {
                        status = "Hidden";
                        hiden = true;
                    }
                    nonPrefTerm += SEPERATEUR + idConcept
                            + SOUS_SEPERATEUR + label.getLabel()
                            + SOUS_SEPERATEUR + label.getLanguage()
                            + SOUS_SEPERATEUR + idTheso
                            + SOUS_SEPERATEUR + idUser
                            + SOUS_SEPERATEUR + status
                            + SOUS_SEPERATEUR + hiden;
                }
                appendNewLang(label.getLanguage());
            }
            if (!nonPrefTerm.isEmpty()) {
                nonPrefTerm = nonPrefTerm.substring(SEPERATEUR.length());
            }
            if (!prefTerm.isEmpty()) {
                prefTerm = prefTerm.substring(SEPERATEUR.length());
            }
        }

        //Relation
        //-- 'id_concept1@role@id_concept2'
        String collectionToAdd;
        String relations = null;
        if (CollectionUtils.isNotEmpty(conceptResource.getRelationsList())) {
            relations = "";
            for (SKOSRelation relation : conceptResource.getRelationsList()) {
                String role = switch (relation.getProperty()) {
                    case SKOSProperty.NARROWER -> "NT";
                    case SKOSProperty.NARROWER_GENERIC -> "NTG";
                    case SKOSProperty.NARROWER_PARTITIVE -> "NTP";
                    case SKOSProperty.NARROWER_INSTANTIAL -> "NTI";
                    case SKOSProperty.BROADER -> {
                        isTopConcept = false;
                        yield "BT";
                    }
                    case SKOSProperty.BROADER_GENERIC -> {
                        isTopConcept = false;
                        yield "BTG";
                    }
                    case SKOSProperty.BROADER_INSTANTIAL -> {
                        isTopConcept = false;
                        yield "BTI";
                    }
                    case SKOSProperty.BROADER_PARTITIVE -> {
                        isTopConcept = false;
                        yield "BTP";
                    }
                    case SKOSProperty.RELATED -> "RT";
                    case SKOSProperty.RELATED_HAS_PART -> "RHP";
                    case SKOSProperty.RELATED_PART_OF -> "RPO";
                    default -> "";
                };

                if (!role.isEmpty()) {
                    relations = relations + SEPERATEUR + idConcept + SOUS_SEPERATEUR + role + SOUS_SEPERATEUR + getOriginalId(relation.getTargetUri());
                } else if (relation.getProperty() == SKOSProperty.MEMBER_OF) {
                    collectionToAdd = getIdFromUri(relation.getTargetUri());
                }

                if (hasTopConcceptList.contains(conceptResource.getUri())) {
                    isTopConcept = true;
                }
            }
            if (!relations.isEmpty()) {
                relations = relations.substring(SEPERATEUR.length());
            }
        }

        //CustomRelation
        String customRelations = null;

        //Notes
        //-- 'value@typeCode@lang@id_term'
        String notes = null;
        if (CollectionUtils.isNotEmpty(conceptResource.getDocumentationsList())) {
            notes = "";
            for (SKOSDocumentation documentation : conceptResource.getDocumentationsList()) {
                String noteTypeCode = switch (documentation.getProperty()) {
                    case SKOSProperty.DEFINITION -> "definition";
                    case SKOSProperty.SCOPE_NOTE -> "scopeNote";
                    case SKOSProperty.EXAMPLE -> "example";
                    case SKOSProperty.HISTORY_NOTE -> "historyNote";
                    case SKOSProperty.EDITORIAL_NOTE -> "editorialNote";
                    case SKOSProperty.CHANGE_NOTE -> "changeNote";
                    case SKOSProperty.NOTE -> "note";
                    default -> "";
                };

                notes += SEPERATEUR + documentation.getText()
                        + SOUS_SEPERATEUR + noteTypeCode
                        + SOUS_SEPERATEUR + documentation.getLanguage()
                        + SOUS_SEPERATEUR + idConcept;
            }
            if (notes.length() > 0) {
                notes = notes.substring(SEPERATEUR.length());
            }
        }

        String notationConcept = "";
        if (CollectionUtils.isNotEmpty(conceptResource.getNotationList())) {
            for (SKOSNotation notation : conceptResource.getNotationList()) {
                notationConcept = notation.getNotation();
            }
        }

        if (isFirst) {
            isFirst = false;
            String uri = conceptResource.getUri().substring(0, conceptResource.getUri().lastIndexOf("/"));
            if (uri == null || uri.isEmpty()) {
                uri = conceptResource.getUri();
            }
            setOriginalUri(idTheso, uri);
        }

        String isReplacedBy = null;
        if (CollectionUtils.isNotEmpty(conceptResource.getsKOSReplaces())) {
            for (SKOSReplaces replace : conceptResource.getsKOSReplaces()) {
                if (SKOSProperty.IS_REPLACED_BY == replace.getProperty()) {
                    if (isReplacedBy == null) {
                        isReplacedBy = "";
                    }
                    isReplacedBy = isReplacedBy + SEPERATEUR + getOriginalId(replace.getTargetUri());
                }
            }
            if (isReplacedBy != null && isReplacedBy.length() > 0) {
                isReplacedBy = isReplacedBy.substring(SEPERATEUR.length());
            }
        }

        Date created = null;
        Date modified = null;

        if (StringUtils.isEmpty(formatDate)) {
            formatDate = "dd-mm-yyyy";
        }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatDate);
            for (SKOSDate date : conceptResource.getDateList()) {
                if (date.getDate() != null && !date.getDate().isEmpty()) {
                    if (date.getProperty() == SKOSProperty.CREATED) {
                        created = simpleDateFormat.parse(date.getDate());
                    }
                    if ((date.getProperty() == SKOSProperty.MODIFIED)) {
                        modified = simpleDateFormat.parse(date.getDate());
                    }
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(ThesaurusEditionSkosImportEngine.class.getName()).log(Level.SEVERE, null, ex);
        }

        String dcterms = null;
        for (SKOSAgent agent : conceptResource.getAgentList()) {
            switch (agent.getProperty()) {
                case SKOSProperty.CREATOR:
                    if (StringUtils.isEmpty(dcterms)) {
                        dcterms = "creator@@" + agent.getAgent() + "@@fr";//agent.getLang;
                    } else {
                        dcterms = dcterms + "##" + "creator@@" + agent.getAgent() + "@@fr";//agent.getLang;                    
                    }
                    break;
                case SKOSProperty.CONTRIBUTOR:
                    if (StringUtils.isEmpty(dcterms)) {
                        dcterms = "contributor@@" + agent.getAgent() + "@@fr";//agent.getLang;
                    } else {
                        dcterms = dcterms + "##" + "contributor@@" + agent.getAgent() + "@@fr";//agent.getLang;                    
                    }
                    break;
                default:
                    break;
            }
        }

        String gps = null;
        if (CollectionUtils.isNotEmpty(conceptResource.getGpsCoordinates())) {
            gps = "";
            for (SKOSGPSCoordinates gpsValue : conceptResource.getGpsCoordinates()) {
                gps += SEPERATEUR + gpsValue.getLat() + SOUS_SEPERATEUR + gpsValue.getLon();
            }

            gps = gps.substring(SEPERATEUR.length());
        }

        conceptRepository.addNewConcept(
                idTheso,
                idConcept,
                idUser,
                conceptStatus,
                "concept",
                notationConcept,
                idArk,
                isTopConcept,
                idHandle,
                idDoi,
                prefTerm,
                relations,
                customRelations,
                notes,
                nonPrefTerm,
                alignements,
                images,
                isReplacedBy,
                gps != null,
                gps,
                created,
                modified,
                dcterms);

        addExternalResources(idTheso, idConcept, conceptResource.getDcRelations());
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

    public void addFoafImages(ArrayList<SKOSResource> foafImages, String idTheso) {

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

    public void addFacetsV2(ArrayList<SKOSResource> facetResources, String idTheso) {

        for (SKOSResource facetSKOSResource : facetResources) {

            String idFacet = getIdFromUri(facetSKOSResource.getUri());
            if (idFacet == null) {
                continue;
            }

            if (CollectionUtils.isEmpty(facetSKOSResource.getLabelsList())) {
                continue;
            }

            String idConceptParent = null;
            for (SKOSRelation relation : facetSKOSResource.getRelationsList()) {
                if (relation.getProperty() == SKOSProperty.SUPER_ORDINATE) {
                    idConceptParent = getOriginalId(relation.getTargetUri());
                    break;
                }
            }
            if (idConceptParent == null) {
                continue;
            }

            String labels = "";
            for (SKOSLabel sKOSLabel : facetSKOSResource.getLabelsList()) {
                labels = labels + SEPERATEUR + sKOSLabel.getLabel() + SOUS_SEPERATEUR + sKOSLabel.getLanguage();
            }
            if (!labels.isEmpty()) {
                labels = labels.substring(2);
            }

            String membres = null;
            if (CollectionUtils.isNotEmpty(facetSKOSResource.getRelationsList())) {
                membres = "";
                for (SKOSRelation member : facetSKOSResource.getRelationsList()) {
                    if (member.getProperty() == SKOSProperty.MEMBER) {
                        membres = membres + SEPERATEUR + getOriginalId(member.getTargetUri());
                    }
                }
                if (!membres.isEmpty()) {
                    membres = membres.substring(2);
                }
            }

            //Notes
            //-- 'value@typeCode@lang@id_term'
            String notes = null;
            if (CollectionUtils.isNotEmpty(facetSKOSResource.getDocumentationsList())) {
                notes = "";
                for (SKOSDocumentation documentation : facetSKOSResource.getDocumentationsList()) {
                    String noteTypeCode = switch (documentation.getProperty()) {
                        case SKOSProperty.DEFINITION -> "definition";
                        case SKOSProperty.SCOPE_NOTE -> "scopeNote";
                        case SKOSProperty.EXAMPLE -> "example";
                        case SKOSProperty.HISTORY_NOTE -> "historyNote";
                        case SKOSProperty.EDITORIAL_NOTE -> "editorialNote";
                        case SKOSProperty.CHANGE_NOTE -> "changeNote";
                        case SKOSProperty.NOTE -> "note";
                        default -> "";
                    };

                    notes += SEPERATEUR + documentation.getText()
                            + SOUS_SEPERATEUR + noteTypeCode
                            + SOUS_SEPERATEUR + documentation.getLanguage()
                            + SOUS_SEPERATEUR + idFacet;
                }
                if (notes.length() > 0) {
                    notes = notes.substring(SEPERATEUR.length());
                }
            }

            String safeLabels = StringUtils.isNotEmpty(labels) ? labels.replace("'", "''") : null;
            String safeNotes  = StringUtils.isNotEmpty(notes) ? notes.replace("'", "''") : null;
            conceptFacetRepository.addFacet(idFacet, idUser, idTheso, idConceptParent, safeLabels, membres, safeNotes);
        }
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
        boolean pass = false;

        if (uri.contains("idc=")) {
            if (uri.contains("&")) {
                String str = uri.substring(uri.indexOf("idc="));
                uri = str.substring(4, str.indexOf("&"));
            } else {
                uri = uri.substring(uri.indexOf("idc=") + 4);
            }
            pass = true;
        }
        if (!pass) {
            if (uri.contains("idg=")) {
                if (uri.contains("&")) {
                    uri = uri.substring(uri.indexOf("idg=") + 4, uri.indexOf("&"));
                } else {
                    uri = uri.substring(uri.indexOf("idg=") + 4);
                }
                pass = true;
            }
        }
        if (!pass) {
            if (uri.contains("idf=")) {
                if (uri.contains("&")) {
                    uri = uri.substring(uri.indexOf("idf=") + 4, uri.indexOf("&"));
                } else {
                    uri = uri.substring(uri.indexOf("idf=") + 4);
                }
                pass = true;
            }
        }
        if (!pass) {
            if (uri.contains("#")) {
                uri = uri.substring(uri.indexOf("#") + 1);
            } else {
                uri = uri.substring(uri.lastIndexOf("/") + 1);
            }
        }
        return fr.cnrs.opentheso.utils.StringUtils.normalizeStringForIdentifier(uri);
    }

    private String getOriginalId(String uri) {
        String originalId;
        if (skosXmlDocument.getEquivalenceUriArkHandle().isEmpty()
                || skosXmlDocument.getEquivalenceUriArkHandle().get(uri) == null) {
            return getIdFromUri(uri);
        }

        originalId = skosXmlDocument.getEquivalenceUriArkHandle().get(uri);
        if (originalId == null) {
            if (message.length() != 0) {
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
                thesaurus1.setTitle("theso_" + idTheso + "_" + idLang);
                thesaurus1.setType("");
                toolboxThesaurusPersistence.addTranslation(thesaurus1);
            }
        }
    }

    public void setRdf4jThesaurus(SKOSXmlDocument rdf4jThesaurus) {
        this.skosXmlDocument = rdf4jThesaurus;
    }

    private void insertGroup(String idGroup, String idThesaurus, String idArk, String typeCode, String notation,
                             Date created, Date modified) {
        if (conceptGroupRepository.findByIdGroupAndIdThesaurus(idGroup, idThesaurus).isPresent()) {
            return;
        }
        conceptGroupRepository.save(ConceptGroup.builder()
                .id(conceptGroupRepository.getNextConceptGroupSequence().intValue())
                .idGroup(idGroup.toLowerCase())
                .idArk(StringUtils.defaultString(idArk))
                .idThesaurus(idThesaurus)
                .idTypeCode(typeCode)
                .notation(notation)
                .idHandle("")
                .idDoi("")
                .created(created == null ? new Date() : created)
                .modified(modified == null ? new Date() : modified)
                .build());
    }

    private void saveConceptGroupConcept(String idGroup, String idConcept, String idThesaurus) {
        conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                .idGroup(idGroup)
                .idThesaurus(idThesaurus)
                .idConcept(idConcept)
                .build());
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
                .created(new Date())
                .modified(new Date())
                .build());
        conceptGroupLabelHistoriqueRepository.save(ConceptGroupLabelHistorique.builder()
                .lexicalValue(conceptGroupLabel.getLexicalValue())
                .lang(conceptGroupLabel.getLang())
                .idThesaurus(conceptGroupLabel.getIdthesaurus())
                .idGroup(conceptGroupLabel.getIdgroup().toLowerCase())
                .idUser(userId)
                .modified(new Date())
                .build());
    }

    private void addGroupNote(String idGroup, String idLang, String idTheso, String text, String noteTypeCode) {
        String lexicalValue = StringEscapeUtils.unescapeXml(
                fr.cnrs.opentheso.utils.StringUtils.clearNoteFromP(
                        fr.cnrs.opentheso.utils.StringUtils.clearValue(text)));
        var existing = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(
                idGroup, idTheso, noteTypeCode, idLang);
        if (!existing.isEmpty()) {
            existing.get(0).setLexicalValue(lexicalValue);
            noteRepository.save(existing.get(0));
        } else {
            noteRepository.save(Note.builder()
                    .noteTypeCode(noteTypeCode)
                    .idThesaurus(idTheso)
                    .lang(idLang)
                    .lexicalValue(lexicalValue)
                    .identifier(idGroup)
                    .noteSource("")
                    .idUser(idUser)
                    .created(new Date())
                    .modified(new Date())
                    .build());
        }
    }
}
