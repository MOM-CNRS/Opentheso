package fr.cnrs.opentheso.services.imports.rdf4j;

import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.group.ConceptGroupLabel;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.terms.NodeEM;
import fr.cnrs.opentheso.models.nodes.NodeGps;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.status.NodeStatus;
import fr.cnrs.opentheso.models.terms.NodeTerm;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.candidats.VoteDto;
import fr.cnrs.opentheso.models.imports.AddConceptsStruct;
import fr.cnrs.opentheso.models.skosapi.SKOSAgent;
import fr.cnrs.opentheso.models.skosapi.SKOSDate;
import fr.cnrs.opentheso.models.skosapi.SKOSDiscussion;
import fr.cnrs.opentheso.models.skosapi.SKOSDocumentation;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSMatch;
import fr.cnrs.opentheso.models.skosapi.SKOSNotation;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSRelation;
import fr.cnrs.opentheso.models.skosapi.SKOSReplaces;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSStatus;
import fr.cnrs.opentheso.models.skosapi.SKOSVote;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.repositories.StatusRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.services.AlignmentService;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.FacetService;
import fr.cnrs.opentheso.services.GpsService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.ImageService;
import fr.cnrs.opentheso.services.NonPreferredTermService;
import fr.cnrs.opentheso.services.NoteService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.RelationGroupService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.services.ThesaurusService;

import java.sql.SQLException;
import javax.sql.DataSource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportRdf4jHelper {

    private final static String SEPERATEUR = "##";
    private final static String SOUS_SEPERATEUR = "@@";

    private final DataSource dataSource;
    private final AlignmentService alignmentService;
    private final ImageService imageService;
    private final PreferenceService preferenceService;
    private final ExternalResourcesRepository externalResourcesRepository;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;
    private final RelationService relationService;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;
    private final GpsService gpsService;
    private final StatusRepository statusRepository;
    private final RelationGroupService relationGroupService;
    private final CandidatStatusRepository candidatStatusRepository;
    private final NonPreferredTermService nonPreferredTermService;
    private final TermService termService;
    private final GroupService groupService;
    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;
    private final NoteService noteService;
    private final FacetService facetService;
    private final ConceptAddService conceptAddService;
    private final ConceptRepository conceptRepository;
    private final ImagesRepository imagesRepository;
    private final ConceptFacetRepository conceptFacetRepository;


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


    /**
     * initialisation des paramètres d'import
     */
    public void setInfos(String formatDate, int idUser, int idGroupUser, String langueSource) {
        this.formatDate = formatDate;
        this.idUser = idUser;
        this.idGroupUser = idGroupUser;
        this.langueSource = langueSource;
        this.isFirst = true;
    }

    /**
     * Cette fonction permet de créer un thésaurus avec ses traductions (Import)
     */
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
        if ((idTheso1 = thesaurusService.addThesaurusRollBack()) == null) {
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
            thesaurusService.addThesaurusTraductionRollBack(thesaurus);
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
        preferenceService.initPreferences(idThesaurus, langueSource);
        nodePreference = preferenceService.getThesaurusPreferences(idThesaurus);
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
        preferenceService.updateAllPreferenceUser(nodePreference);
    }

    private void setOriginalUri(String idTheso, String uri) {

        if (nodePreference == null) {
            return;
        }
        nodePreference.setCheminSite(uri+"/");
        nodePreference.setPreferredName(idTheso);
        nodePreference.setOriginalUri(uri);
        preferenceService.updateAllPreferenceUser(nodePreference);
    }

    public void addFacets(ArrayList<SKOSResource> facetResources, String idTheso) {

        boolean first = true;
        for (SKOSResource facetSKOSResource : facetResources) {

            String idConceptParent = null;
            String idFacet = getIdFromUri(facetSKOSResource.getUri());
            if (idFacet == null) {
                continue;
            }
            for (SKOSRelation relation : facetSKOSResource.getRelationsList()) {
                if (relation.getProperty() == SKOSProperty.SUPER_ORDINATE) {
                    idConceptParent = getOriginalId(relation.getTargetUri());
                }
            }
            if (idConceptParent == null) {
                continue;
            }
            if (facetSKOSResource.getLabelsList().isEmpty()) {
                continue;
            }

            for (SKOSLabel sKOSLabel : facetSKOSResource.getLabelsList()) {
                if (first) {
                    facetService.addNewFacet(idFacet, idTheso, idConceptParent, sKOSLabel.getLabel(), sKOSLabel.getLanguage());
                    first = false;
                } else {
                    facetService.addFacetTraduction(idFacet, idTheso, sKOSLabel.getLabel(), sKOSLabel.getLanguage());
                }
            }
            for (SKOSRelation member : facetSKOSResource.getRelationsList()) {
                if (member.getProperty() == SKOSProperty.MEMBER) {
                    facetService.addConceptToFacet(idFacet, idTheso, getOriginalId(member.getTargetUri()));
                }
            }
            first = true;
        }

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
                    Logger.getLogger(ImportRdf4jHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            try {
                groupService.insertGroup(idGroup, idTheso, idArkHandle, type, notationValue, created, modified);
            } catch (Exception ex) {
                log.error(ex.getMessage());
                groupService.insertGroup(idGroup, idTheso, idArkHandle, type, notationValue, created, modified);
            }

            // group/sous_group
            for (SKOSRelation relation : group.getRelationsList()) {
                int prop = relation.getProperty();
                switch (prop) {
                    case SKOSProperty.SUBGROUP:
                        idSubGroup = getIdFromUri(relation.getTargetUri());
                        relationGroupService.addSubGroup(idGroup, idSubGroup, idTheso);
                        break;
                    case SKOSProperty.MEMBER:
                        // Récupération de l'Id d'origine sauvegardé à l'import (idArk -> identifier)
                        idSubConcept = getOriginalId(relation.getTargetUri());
                        groupSubGroup.put(idSubConcept, idGroup);
                        groupService.addConceptGroupConcept(idGroup, idSubConcept, idTheso);
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

                groupService.addGroupTraduction(conceptGroupLabel, idUser);
            }

            for (SKOSDocumentation documentation : group.getDocumentationsList()) {
                String noteTypeCode = "";
                int prop = documentation.getProperty();
                noteTypeCode = getString(noteTypeCode, prop);

                noteService.addNote(idGroup, documentation.getLanguage(), idTheso, documentation.getText(), noteTypeCode, "", idUser);
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

    /**
     * permet d'ajouter les relations entre les groupes / sousGroupes et les
     * groupes / concepts
     */
    private void addGroupConceptGroup(String idTheso) {
        // groupSubGroup : compositon du HashMap = idSubGroup(ou idConcept) -> idGroup
        // c'est pour séparer les concepts des groupes
        for (String idSubGroup : groupSubGroup.keySet()) {
            if (idGroups.contains(idSubGroup)) {
                // si la relation member est vers un sous groupe, alors on créé une relation groupe/sousGroupe
                relationGroupService.addSubGroup(groupSubGroup.get(idSubGroup), idSubGroup, idTheso);
            } else {
                groupService.addConceptGroupConcept(groupSubGroup.get(idSubGroup), idSubGroup, idTheso);
            }
        }
    }

    public void addConcept(SKOSResource conceptResource, String idTheso, boolean isCandidatImport) {
        if (isCandidatImport) {
            if (conceptAddService.isIdExiste(conceptResource.getIdentifier(), idTheso)) {
                return;
            }
        }

        AddConceptsStruct acs = new AddConceptsStruct();
        initAddConceptsStruct(acs, conceptResource, idTheso, isCandidatImport);
        addRelation(acs, idTheso);

        if (isCandidatImport) {
            acs.concept.setStatus("CA");
        }

        // envoie du concept à la BDD
        addConceptToBdd(acs, idTheso, isCandidatImport);
    }

    @Transactional
    public void addConceptV2(SKOSResource conceptResource, String idTheso) {
        String idConcept;
        if (StringUtils.isEmpty(conceptResource.getIdentifier())) {
            idConcept = getOriginalId(conceptResource.getUri());
        } else {
            idConcept = conceptResource.getIdentifier();
        }

        String conceptStatus = "";

        if (conceptResource.getStatus() == SKOSProperty.DEPRECATED) {
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
            Logger.getLogger(ImportRdf4jHelper.class.getName()).log(Level.SEVERE, null, ex);
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

    public void initAddConceptsStruct(AddConceptsStruct acs, SKOSResource conceptResource,
            String idTheso, boolean isCandidatImport) {

        acs.conceptResource = conceptResource;
        acs.concept = new Concept();
        String idArk;

        String idConcept = getOriginalId(conceptResource.getUri());
        acs.concept.setIdConcept(idConcept);
        if (conceptResource.getStatus() == SKOSProperty.DEPRECATED) {
            acs.conceptStatus = "dep";
        }

        // option cochée
        if ("ark".equalsIgnoreCase(selectedIdentifier)) {
            //   if(conceptResource.getArkId() != null && !conceptResource.getArkId().isEmpty())
            idArk = getIdArkFromUri(conceptResource.getUri());
            acs.concept.setIdArk(idArk);
        }
        if ("handle".equalsIgnoreCase(selectedIdentifier)) {
            acs.concept.setIdHandle(getIdHandleFromUri(conceptResource.getUri()));
        }
        if ("doi".equalsIgnoreCase(selectedIdentifier)) {
            acs.concept.setIdDoi(getIdDoiFromUri(conceptResource.getUri()));
        }

        acs.concept.setIdThesaurus(idTheso);
        addNotation(acs);
        addGPSCoordinates(acs);
        addLabel(acs);
        addDocumentation(acs);
        addDate(acs);
        addReplaces(acs);

        for (SKOSAgent agent : conceptResource.getAgentList()) {
            if (agent.getProperty() == SKOSProperty.CREATOR) {
                acs.concept.setCreatorName(agent.getAgent());
            }

            if (agent.getProperty() == SKOSProperty.CONTRIBUTOR) {
                acs.concept.setContributorName(agent.getAgent());
            }
        }

        if (isCandidatImport) {
            addMessages(acs);
            addVotes(acs);
            addStatut(acs, conceptResource.getSkosStatus(), idTheso, idConcept);
        }

        addAlignment(acs, idTheso);
        addImages(acs);

        if (isFirst) {
            isFirst = false;
            String uri = conceptResource.getUri().substring(0, conceptResource.getUri().lastIndexOf("/"));
            if (uri == null || uri.isEmpty()) {
                uri = conceptResource.getUri();
            }
            setOriginalUri(idTheso, uri);
        }

        //autre
        //ajout des termes et traductions
        acs.nodeTerm.setNodeTermTraduction(acs.nodeTermTraductionList);
        acs.nodeTerm.setIdTerm(acs.concept.getIdConcept());
        acs.nodeTerm.setIdConcept(acs.concept.getIdConcept());
        acs.nodeTerm.setIdThesaurus(idTheso);
        acs.nodeTerm.setSource("");
        acs.nodeTerm.setStatus("");
        acs.nodeTerm.setCreated(acs.concept.getCreated());
        acs.nodeTerm.setModified(acs.concept.getModified());
    }

    private void addStatut(AddConceptsStruct acs, SKOSStatus skosStatus, String idTheso, String idConcept) {
        acs.status = new NodeStatus();

        acs.status.setIdThesaurus(idTheso);
        acs.status.setIdConcept(idConcept);
        if (skosStatus == null) {
            acs.status.setMessage("");
            acs.status.setIdStatus("1");
        } else {
            acs.status.setMessage(skosStatus.getMessage());
            acs.status.setIdStatus(skosStatus.getIdStatus());
        }
    }

    public void addConceptToBdd(AddConceptsStruct acs, String idThesaurus, boolean isCandidatImport) {

        conceptService.insertConcept(acs.concept);

        termService.addTerms(acs.nodeTerm, idUser);

        for (HierarchicalRelationship hierarchicalRelationship : acs.hierarchicalRelationships) {
            switch (hierarchicalRelationship.getRole()) {
                case "NT":
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept1(),
                            hierarchicalRelationship.getIdThesaurus(),
                            hierarchicalRelationship.getRole(),
                            hierarchicalRelationship.getIdConcept2()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    // pour créer la relation réciproque si elle n'existe pas
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept2(),
                            hierarchicalRelationship.getIdThesaurus(),
                            "BT",
                            hierarchicalRelationship.getIdConcept1()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    break;
                case "BT":
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept1(),
                            hierarchicalRelationship.getIdThesaurus(),
                            hierarchicalRelationship.getRole(),
                            hierarchicalRelationship.getIdConcept2()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    // pour créer la relation réciproque si elle n'existe pas
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept2(),
                            hierarchicalRelationship.getIdThesaurus(),
                            "NT",
                            hierarchicalRelationship.getIdConcept1()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    break;
                case "RT":
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept1(),
                            hierarchicalRelationship.getIdThesaurus(),
                            hierarchicalRelationship.getRole(),
                            hierarchicalRelationship.getIdConcept2()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    // pour créer la relation réciproque si elle n'existe pas
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept2(),
                            hierarchicalRelationship.getIdThesaurus(),
                            "RT",
                            hierarchicalRelationship.getIdConcept1()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    break;
                case "NTP":
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept1(),
                            hierarchicalRelationship.getIdThesaurus(),
                            hierarchicalRelationship.getRole(),
                            hierarchicalRelationship.getIdConcept2()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    // pour créer la relation réciproque si elle n'existe pas
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept2(),
                            hierarchicalRelationship.getIdThesaurus(),
                            "BTP",
                            hierarchicalRelationship.getIdConcept1()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    break;
                case "NTG":
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept1(),
                            hierarchicalRelationship.getIdThesaurus(),
                            hierarchicalRelationship.getRole(),
                            hierarchicalRelationship.getIdConcept2()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    // pour créer la relation réciproque si elle n'existe pas
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept2(),
                            hierarchicalRelationship.getIdThesaurus(),
                            "BTG",
                            hierarchicalRelationship.getIdConcept1()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    break;
                case "NTI":
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept1(),
                            hierarchicalRelationship.getIdThesaurus(),
                            hierarchicalRelationship.getRole(),
                            hierarchicalRelationship.getIdConcept2()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    // pour créer la relation réciproque si elle n'existe pas
                    if (relationService.addHierarchicalRelation(
                            hierarchicalRelationship.getIdConcept2(),
                            hierarchicalRelationship.getIdThesaurus(),
                            "BTI",
                            hierarchicalRelationship.getIdConcept1()) == null) {

                        message.append(System.lineSeparator());
                        message.append("Erreur sur la relation = ");
                        message.append(acs.concept.getIdConcept());
                        message.append(" ## ");
                        message.append(hierarchicalRelationship.getRole());
                    }
                    break;
            }
        }

        for (NodeNote nodeNoteList1 : acs.nodeNotes) {
            noteService.addNote(acs.concept.getIdConcept(), nodeNoteList1.getLang(), idThesaurus,
                    nodeNoteList1.getLexicalValue(), nodeNoteList1.getNoteTypeCode(), "", idUser);
        }

        for (NodeAlignment nodeAlignment : acs.nodeAlignments) {
            alignmentService.addNewAlignment(nodeAlignment);
        }
        for (NodeEM nodeEMList1 : acs.nodeEMList) {
            acs.term.setIdConcept(acs.concept.getIdConcept());
            acs.term.setIdTerm(acs.nodeTerm.getIdTerm());
            acs.term.setLexicalValue(nodeEMList1.getLexicalValue());
            acs.term.setLang(nodeEMList1.getLang());
            acs.term.setIdThesaurus(idThesaurus);//thesaurus.getId_thesaurus());
            acs.term.setSource(nodeEMList1.getSource());
            acs.term.setStatus(nodeEMList1.getStatus());
            acs.term.setHidden(nodeEMList1.isHiden());
            nonPreferredTermService.addNonPreferredTerm(acs.term, idUser);
        }

        if (CollectionUtils.isNotEmpty(acs.nodeGps)) {
            for (NodeGps nodeGps : acs.nodeGps) {
                if (nodeGps.getLatitude() != 0.0 && nodeGps.getLongitude() != 0.0) {
                    // insertion des données GPS
                    gpsService.insertCoordinates(acs.concept.getIdConcept(), idThesaurus,
                            nodeGps.getLatitude(), nodeGps.getLongitude());
                }
            }
        }

        if (acs.isTopConcept) {
            conceptService.setTopConcept(acs.concept.getIdConcept(), idThesaurus, true);
        }

        // ajout des images externes URI
        for (NodeImage nodeImage : acs.nodeImages) {
            imageService.addExternalImage(acs.concept.getIdConcept(), idThesaurus, nodeImage.getImageName(),
                    nodeImage.getCopyRight(), nodeImage.getUri(), "", idUser);
        }

        if (acs.conceptStatus.equalsIgnoreCase("dep")) {
            conceptService.deprecateConcept(acs.concept.getIdConcept(), idThesaurus, idUser);
        }
        /// ajout des relations de concepts dépréciés
        for (NodeIdValue nodeIdValue : acs.replacedBy) {
            conceptService.addReplacedBy(acs.concept.getIdConcept(), idThesaurus, nodeIdValue.getId(), idUser);
        }
        if (isCandidatImport) {
            candidatStatusRepository.save(CandidatStatus.builder()
                    .idConcept(acs.concept.getIdConcept())
                    .idThesaurus(idThesaurus)
                    .idUser(idUser)
                    .date(new Date())
                    .status(statusRepository.findById(1).orElse(null))
                    .build());
        }

        // initialisation des variables
        acs.concept = null;
        acs.concept = new Concept();

        acs.nodeTerm = null;
        acs.nodeTerm = new NodeTerm();

        if (acs.nodeTermTraductionList != null) {
            acs.nodeTermTraductionList.clear();
        }

        if (acs.nodeEMList != null) {
            acs.nodeEMList.clear();
        }

        if (acs.nodeNotes != null) {
            acs.nodeNotes.clear();
        }

        if (acs.nodeAlignments != null) {
            acs.nodeAlignments.clear();
        }

        if (acs.hierarchicalRelationships != null) {
            acs.hierarchicalRelationships.clear();
        }

        if (acs.idGrps != null) {
            acs.idGrps.clear();
        }

        acs.isTopConcept = false;
        acs.nodeGps = new ArrayList<>();

        if (acs.nodeImages != null) {
            acs.nodeImages.clear();
        }

        if (acs.replacedBy != null) {
            acs.replacedBy.clear();
        }

        if (acs.replaces != null) {
            acs.replaces.clear();
        }
        acs.conceptStatus = "";

    }

    private String formatLinkToHtmlTag(String str) {
        Pattern MY_PATTERN = Pattern.compile("[a-zA-Z]+ \\(http(.*?)\\)");
        Matcher m = MY_PATTERN.matcher(str);
        while (m.find()) {
            String brut = m.group();
            String titre = brut.substring(0, brut.indexOf('(') - 1);
            String value = brut.substring(brut.indexOf('(') + 1, brut.indexOf(')'));

            String link = "<a href='" + value + "'>" + titre + "</a>";
            str = str.replace(brut, link);
        }
        return str;
    }

    private void addAlignment(AddConceptsStruct acs, String idTheso) {
        int prop;
        int id_type = -1;
        NodeAlignment nodeAlignment;
        for (SKOSMatch match : acs.conceptResource.getMatchList()) {
            prop = match.getProperty();
            nodeAlignment = new NodeAlignment();
            id_type = switch (prop) {
                case SKOSProperty.CLOSE_MATCH -> 2;
                case SKOSProperty.EXACT_MATCH -> 1;
                case SKOSProperty.BROAD_MATCH -> 3;
                case SKOSProperty.NARROWER_MATCH -> 5;
                case SKOSProperty.RELATED_MATCH -> 4;
                default -> id_type;
            };
            nodeAlignment.setId_author(idUser);
            nodeAlignment.setConcept_target("");
            nodeAlignment.setThesaurus_target("");
            nodeAlignment.setUri_target(match.getValue());
            nodeAlignment.setInternal_id_concept(acs.concept.getIdConcept());
            nodeAlignment.setInternal_id_thesaurus(idTheso);
            nodeAlignment.setAlignement_id_type(id_type);
            acs.nodeAlignments.add(nodeAlignment);
        }
    }

    private void addMessages(AddConceptsStruct acs) {
        for (SKOSDiscussion discussion : acs.conceptResource.getMessages()) {
            MessageDto messageDto = new MessageDto();
            messageDto.setDate(discussion.getDate());
            messageDto.setIdUser(discussion.getIdUser());
            messageDto.setMsg(discussion.getMsg());
            acs.messages.add(messageDto);
        }
    }

    private void addVotes(AddConceptsStruct acs) {
        for (SKOSVote vote : acs.conceptResource.getVotes()) {
            VoteDto voteDto = new VoteDto();
            voteDto.setTypeVote(vote.getTypeVote());
            voteDto.setIdUser(vote.getIdUser());
            voteDto.setIdConcept(vote.getIdConcept());

            if (!StringUtils.isEmpty(vote.getIdNote())) {
                String str = formatLinkToHtmlTag(vote.getIdNote());
                str = str.replaceAll("'", "''");
                NodeNote nodeNote = noteService.getNoteByValue(str);
                if (nodeNote != null) {
                    voteDto.setIdNote(nodeNote.getIdNote() + "");
                }
            } else {
                voteDto.setIdNote(null);
            }

            acs.votes.add(voteDto);
        }
    }

    private void addNotation(AddConceptsStruct acs) {
        acs.concept.setNotation("");
        for (SKOSNotation notation : acs.conceptResource.getNotationList()) {
            if (notation.getNotation() != null) {
                acs.concept.setNotation(notation.getNotation());
            }
        }
    }

    private void addImages(AddConceptsStruct acs) {
        for (NodeImage nodeImage : acs.conceptResource.getNodeImages()) {
            if (nodeImage != null && (!nodeImage.getUri().isEmpty())) {
                NodeImage nodeImage1 = new NodeImage();
                nodeImage1.setImageName(nodeImage.getImageName());
                nodeImage1.setCopyRight(nodeImage.getCopyRight());
                nodeImage1.setUri(nodeImage.getUri());
                acs.nodeImages.add(nodeImage1);
            }
        }
    }

    private void addGPSCoordinates(AddConceptsStruct acs) {
        for (SKOSGPSCoordinates element : acs.conceptResource.getGpsCoordinates()) {
            NodeGps nodeGps = new NodeGps();
            nodeGps.setLatitude(Double.parseDouble(element.getLat()));
            nodeGps.setLongitude(Double.parseDouble(element.getLon()));
            acs.nodeGps.add(nodeGps);
        }
    }

    private void addLabel(AddConceptsStruct acs) {
        NodeTermTraduction nodeTermTraduction;

        for (SKOSLabel label : acs.conceptResource.getLabelsList()) {
            if (label.getProperty() == SKOSProperty.PREF_LABEL) {
                nodeTermTraduction = new NodeTermTraduction();
                nodeTermTraduction.setLexicalValue(label.getLabel());
                nodeTermTraduction.setLang(label.getLanguage());
                acs.nodeTermTraductionList.add(nodeTermTraduction);
            } else {
                NodeEM nodeEM = new NodeEM();
                String status = "";
                boolean hiden = false;
                if (label.getProperty() == SKOSProperty.ALT_LABEL) {
                    status = "USE";

                } else if (label.getProperty() == SKOSProperty.HIDDEN_LABEL) {
                    status = "Hidden";
                    hiden = true;
                }
                nodeEM.setLexicalValue(label.getLabel());
                nodeEM.setLang(label.getLanguage());
                nodeEM.setSource("" + idUser);
                nodeEM.setStatus(status);
                nodeEM.setHiden(hiden);
                acs.nodeEMList.add(nodeEM);
            }
            appendNewLang(label.getLanguage());
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

    private void addDocumentation(AddConceptsStruct acs) {
        NodeNote nodeNote;
        for (SKOSDocumentation documentation : acs.conceptResource.getDocumentationsList()) {
            if ("status".equalsIgnoreCase(documentation.getLanguage()) || "vote".equalsIgnoreCase(documentation.getLanguage()) || "message".equalsIgnoreCase(documentation.getLanguage())) {
                continue;
            }
            String noteTypeCode = "";
            int prop = documentation.getProperty();
            nodeNote = new NodeNote();
            noteTypeCode = getString(noteTypeCode, prop);
            nodeNote.setLang(documentation.getLanguage());
            nodeNote.setLexicalValue(documentation.getText());
            nodeNote.setNoteTypeCode(noteTypeCode);
            acs.nodeNotes.add(nodeNote);
        }
    }

    private void addDate(AddConceptsStruct acs) {
        try {
            if (StringUtils.isEmpty(formatDate)) {
                formatDate = "dd-mm-yyyy";
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatDate);
            for (SKOSDate date : acs.conceptResource.getDateList()) {
                if (date.getProperty() == SKOSProperty.CREATED) {
                    acs.concept.setCreated(simpleDateFormat.parse(date.getDate()));
                } else if ((date.getProperty() == SKOSProperty.MODIFIED)) {
                    acs.concept.setModified(simpleDateFormat.parse(date.getDate()));
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(ImportRdf4jHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addReplaces(AddConceptsStruct acs) {
        int prop;
        for (SKOSReplaces replace : acs.conceptResource.getsKOSReplaces()) {
            prop = replace.getProperty();
            switch (prop) {
                case SKOSProperty.IS_REPLACED_BY:
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId(getOriginalId(replace.getTargetUri()));
                    acs.replacedBy.add(nodeIdValue);
                    break;
                case SKOSProperty.REPLACES:
                    NodeIdValue nodeIdValue2 = new NodeIdValue();
                    nodeIdValue2.setId(getOriginalId(replace.getTargetUri()));
                    acs.replaces.add(nodeIdValue2);
                    break;
                default:
                    break;
            }
        }
    }

    public void addRelation(AddConceptsStruct acs, String idTheso) {
        HierarchicalRelationship hierarchicalRelationship;
        int prop;
        String idConcept2;
        acs.isTopConcept = true;
        for (SKOSRelation relation : acs.conceptResource.getRelationsList()) {
            prop = relation.getProperty();

            hierarchicalRelationship = new HierarchicalRelationship();
            String role;

            switch (prop) {
                case SKOSProperty.NARROWER:
                    role = "NT";
                    break;
                case SKOSProperty.NARROWER_GENERIC:
                    role = "NTG";
                    break;
                case SKOSProperty.NARROWER_PARTITIVE:
                    role = "NTP";
                    break;
                case SKOSProperty.NARROWER_INSTANTIAL:
                    role = "NTI";
                    break;
                case SKOSProperty.BROADER:
                    acs.isTopConcept = false;
                    role = "BT";
                    break;
                case SKOSProperty.BROADER_GENERIC:
                    acs.isTopConcept = false;
                    role = "BTG";
                    break;
                case SKOSProperty.BROADER_INSTANTIAL:
                    acs.isTopConcept = false;
                    role = "BTI";
                    break;
                case SKOSProperty.BROADER_PARTITIVE:
                    acs.isTopConcept = false;
                    role = "BTP";
                    break;
                case SKOSProperty.RELATED:
                    role = "RT";
                    break;
                case SKOSProperty.RELATED_HAS_PART:
                    role = "RHP";
                    break;
                case SKOSProperty.RELATED_PART_OF:
                    role = "RPO";
                    break;
                default:
                    role = "";
            }

            if (!role.equals("")) {
                hierarchicalRelationship.setIdConcept1(acs.concept.getIdConcept());
                hierarchicalRelationship.setIdConcept2(getOriginalId(relation.getTargetUri()));
                hierarchicalRelationship.setIdThesaurus(idTheso);
                hierarchicalRelationship.setRole(role);
                acs.hierarchicalRelationships.add(hierarchicalRelationship);

            } else if (prop == SKOSProperty.INSCHEME) {

            } else if (prop == SKOSProperty.TOP_CONCEPT_OF) {
                acs.isTopConcept = true;
            } else if (prop == SKOSProperty.MEMBER_OF) {
                acs.collectionToAdd = getIdFromUri(relation.getTargetUri());
            }
            if (hasTopConcceptList.contains(acs.conceptResource.getUri())) {
                if (!acs.isTopConcept == false) {
                    acs.isTopConcept = true;
                }
            }
        }
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

    /**
     * Permet de retourner l'id d'origine d'un concept SKOS en s'appuyant sur le
     * DC:identifier sauvegardé à l'import Si l'identifiant d'origine n'a pas
     * été trouvé, c'est l'ID de l'URI qui est récupéré
     *
     * @return #MR
     */
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
            if (thesaurusService.isLanguageExistOfThesaurus(idTheso, idLang)) {
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
                thesaurusService.addThesaurusTraduction(thesaurus1);
            }
        }
    }

    public SKOSXmlDocument getRdf4jThesaurus() {
        return skosXmlDocument;
    }

    public void setRdf4jThesaurus(SKOSXmlDocument rdf4jThesaurus) {
        this.skosXmlDocument = rdf4jThesaurus;
    }
}
