package fr.cnrs.opentheso.services.imports.csv;

import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.group.ConceptGroupLabel;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.nodes.NodeGps;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.relations.NodeReplaceValueByValue;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
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
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.services.ThesaurusService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportHelper {

    private final static String SEPERATEUR = "##";
    private final static String SOUS_SEPERATEUR = "@@";

    private final GpsService gpsService;
    private final ImageService imageService;
    private final GroupService groupService;
    private final RelationService relationService;
    private final TermService termService;
    private final TermRepository termRepository;
    private final NonPreferredTermService nonPreferredTermService;
    private final AlignmentService alignmentService;
    private final NoteService noteService;
    private final FacetService facetService;
    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final ThesaurusService thesaurusService;
    private final ExternalResourcesRepository externalResourcesRepository;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptFacetRepository conceptFacetRepository;

    private String message = "";
    private Preferences nodePreference;
    private String langueSource, formatDate;
    private int idUser;


    /**
     * Cette fonction permet de créer un thésaurus avec ses traductions (Import)
     * elle retourne l'identifiant du thésaurus, sinon Null
     */
    public String createThesaurus(String thesoName, String idLang, int idProject, NodeUser nodeUser) {

        Thesaurus thesaurus = new Thesaurus();
        thesaurus.setCreator(nodeUser.getName());
        thesaurus.setContributor(nodeUser.getName());
        thesaurus.setLanguage(idLang);

        var idThesaurus = thesaurusService.addThesaurusRollBack();
        thesaurus.setId_thesaurus(idThesaurus);

        if (thesoName.isEmpty()) {
            thesoName = "theso_" + idThesaurus;
        }
        thesaurus.setTitle(thesoName);

        thesaurusService.addThesaurusTraductionRollBack(thesaurus);

        // ajouter le thésaurus dans le group de l'utilisateur
        if (idProject != -1) {
            // si le groupeUser = - 1, c'est le cas d'un SuperAdmin, alors on n'intègre pas le thésaurus dans un groupUser
            var userGroupThesaurus = UserGroupThesaurus.builder().idThesaurus(thesaurus.getId_thesaurus()).idGroup(idProject).build();
            userGroupThesaurusRepository.save(userGroupThesaurus);
        }
        return idThesaurus;
    }

    public void addLangsToThesaurus(List<String> langs, String idTheso) {

        for (String idLang : langs) {
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

    public void addSingleConcept(String idTheso, String idConceptPere, String idGroup, int idUser,
            CsvReadHelper.ConceptObject conceptObject, Preferences nodePreference) {
        boolean first = true;
        String idConcept = null;
        String idTerm = null;

        // ajout du concept
        Concept concept = new Concept();

        // On vérifie si le conceptPere est un Groupe, alors il faut ajouter un TopTerm, sinon, c'est un concept avec des relations
        if (idConceptPere == null) {
            if(conceptObject.getBroaders() == null || conceptObject.getBroaders().isEmpty())
                concept.setTopConcept(true);
        } else {
            concept.setTopConcept(false);
        }

        concept.setIdGroup(idGroup);
        concept.setIdThesaurus(idTheso);
        concept.setStatus("");
        concept.setNotation("");
        concept.setIdConcept(conceptObject.getIdConcept());

        Term term = new Term();
        term.setIdThesaurus(idTheso);

        // ajout des PrefLabel
        for (CsvReadHelper.Label prefLabel : conceptObject.getPrefLabels()) {
            if (first) {
                term.setLang(prefLabel.getLang());
                term.setLexicalValue(prefLabel.getLabel());
                term.setSource("");
                term.setStatus("");
                idConcept = conceptAddService.addConcept(idConceptPere, "NT", concept, term, idUser);
                if (idConcept == null) {
                    message = message + "\n" + "erreur dans l'intégration du concept " + prefLabel.getLabel();
                    return;
                } else {
                    conceptObject.setIdConcept(idConcept);

                    var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, idConcept);
                    idTerm = preferredTerm.map(PreferredTerm::getIdTerm).orElse(null);
                    conceptObject.setIdTerm(idTerm);

                    // synonymes et cachés
                    addAltLabels(idTheso, conceptObject);

                    // notes
                    addNotes(idTheso, conceptObject);

                    // relations
                    addRelations(idTheso, conceptObject);

                    // alignements
                    addAlignments(idTheso, conceptObject);

                    // géolocalisation
                    addGeoLocalisation(idTheso, conceptObject);

                    // appartenance à une facette
                    addConceptToFacet(idTheso, conceptObject);

                    // ajout à une collection
                    addConceptToCollection(idTheso, conceptObject);

                    first = false;
                }

            } // ajout des traductions
            else {
                if (idConcept != null) {
                    term.setIdThesaurus(idTheso);
                    term.setLang(prefLabel.getLang());
                    term.setLexicalValue(prefLabel.getLabel());
                    term.setIdTerm(idTerm);
                    term.setContributor(idUser);
                    term.setCreator(idUser);
                    term.setSource("");
                    term.setStatus("");
                    termService.addTermTraduction(term, idUser);
                }
            }
        }

    }

    /**
     * Permet d'ajouter les groupes/collections...
     */
    public boolean addGroup(String idTheso, CsvReadHelper.ConceptObject conceptObject) {
        String idGroup = conceptObject.getIdConcept();
        if (idGroup == null || idGroup.isEmpty()) {
            message = message + "\n" + "Identifiant Groupe manquant";
            return false;
        }
        
        // ajout des concepts à la collection
        if (!conceptObject.getMembers().isEmpty()) {
            for (String conceptId : conceptObject.getMembers()) {
                groupService.addConceptGroupConcept(idGroup, conceptId, idTheso);
            }
        }
        
        if (StringUtils.isEmpty(formatDate)) {
            formatDate = "yyyy-MM-dd";
        }
        Date created = null;
        Date modified = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatDate);        
        try {
            if(conceptObject.getCreated() != null && !conceptObject.getCreated().isEmpty())
                created = simpleDateFormat.parse(conceptObject.getCreated());
            if(conceptObject.getModified() != null && !conceptObject.getModified().isEmpty())
                modified = simpleDateFormat.parse(conceptObject.getModified());            
        } catch (ParseException ex) {
            Logger.getLogger(CsvImportHelper.class.getName()).log(Level.SEVERE, null, ex);
        }        
        
        groupService.insertGroup(idGroup, idTheso, "", "C", conceptObject.getNotation(), created, modified);

        ConceptGroupLabel conceptGroupLabel = new ConceptGroupLabel();
        for (CsvReadHelper.Label label : conceptObject.getPrefLabels()) {
            // ajouter les traductions des Groupes
            conceptGroupLabel.setIdgroup(idGroup);
            conceptGroupLabel.setIdthesaurus(idTheso);
            conceptGroupLabel.setLang(label.getLang());
            conceptGroupLabel.setLexicalValue(label.getLabel());
            groupService.addGroupTraduction(conceptGroupLabel, idUser);
        }

        addNotes(idTheso, conceptObject);
        
        return true;
    }

    public boolean addConcept(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, conceptObject.getIdConcept());
        conceptObject.setIdTerm(preferredTerm.map(PreferredTerm::getIdTerm).orElse(null));

        if (!addPrefLabel(idTheso, conceptObject)) {
            return false;
        }
        // synonymes et cachés
        if (!addAltLabels(idTheso, conceptObject)) {
            return false;
        }
        // notes
        if (!addNotes(idTheso, conceptObject)) {
            return false;
        }
        // relations
        if (!addRelations(idTheso, conceptObject)) {
            return false;
        }
        // alignements
        if (!addAlignments(idTheso, conceptObject)) {
            return false;
        }
        // géolocalisation
        if (!addGeoLocalisation(idTheso, conceptObject)) {
            return false;
        }
        // Membres ou appartenance aux groupes
        if (!addMembers(idTheso, conceptObject)) {
            return false;
        }

        return true;
    }

    public boolean addConceptV2(String idTheso, CsvReadHelper.ConceptObject conceptObject, int idUser, String formatDate) {

        // Membres ou appartenance aux groupes
        if (!addMembers(idTheso, conceptObject)) {
            return false;
        }

        String conceptStatus;
        String conceptType;
        String idHandle = "";
        String idDoi = "";
        boolean isTopConcept = true;
        
        String replacedBy = null;
        
        // le status du concept (déprécié ...)
        if(conceptObject.isDeprecated()) {
            conceptStatus = "DEP";
            if (CollectionUtils.isNotEmpty(conceptObject.getReplacedBy())) {
                for (String replace : conceptObject.getReplacedBy()) {
                    if(StringUtils.isEmpty(replacedBy)) {
                        replacedBy = replace;
                    } else {
                        replacedBy = replacedBy + SEPERATEUR + replace;
                    }
                }
            }            
        }
        else
            conceptStatus= "D";
        
        // concept type
        conceptType = conceptObject.getConceptType();
        if(StringUtils.isEmpty(conceptType)) 
            conceptType = "concept";

        // IMAGES
        //-- 'name1@@copyright1@@url1##name2@@copyright2@@url2'
        String images = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getImages())) {
            images = "";
            for (NodeImage nodeImage : conceptObject.getImages()) {
                if(nodeImage == null) continue;
                if (StringUtils.isEmpty(nodeImage.getUri())) continue;
                
                if(StringUtils.isEmpty(images)) {
                    images = nodeImage.getImageName() + SOUS_SEPERATEUR + nodeImage.getCopyRight() + SOUS_SEPERATEUR + nodeImage.getUri() + SOUS_SEPERATEUR + nodeImage.getCreator();
                }
                else {    
                    images = images + SEPERATEUR + nodeImage.getImageName() + SOUS_SEPERATEUR + nodeImage.getCopyRight() + SOUS_SEPERATEUR + nodeImage.getUri() + SOUS_SEPERATEUR + nodeImage.getCreator();
                }
            }
        }

        // ALIGNEMENT
        //-- 'author@concept_target@thesaurus_target@uri_target@alignement_id_type@internal_id_thesaurus@internal_id_concept'
        String alignements = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getExactMatchs())) {
            alignements = "";
            for (String uri : conceptObject.getExactMatchs()) {
                alignements = alignements + SEPERATEUR + idUser
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + uri
                        + SOUS_SEPERATEUR + 1
                        + SOUS_SEPERATEUR + idTheso
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getCloseMatchs())) {
            if (alignements == null) {
                alignements = "";
            }
            for (String uri : conceptObject.getCloseMatchs()) {
                alignements = alignements + SEPERATEUR + idUser
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + uri
                        + SOUS_SEPERATEUR + 2
                        + SOUS_SEPERATEUR + idTheso
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getBroadMatchs())) {
            if (alignements == null) {
                alignements = "";
            }
            for (String uri : conceptObject.getBroadMatchs()) {
                alignements = alignements + SEPERATEUR + idUser
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + uri
                        + SOUS_SEPERATEUR + 3
                        + SOUS_SEPERATEUR + idTheso
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getRelatedMatchs())) {
            if (alignements == null) {
                alignements = "";
            }
            for (String uri : conceptObject.getRelatedMatchs()) {
                alignements = alignements + SEPERATEUR + idUser
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + uri
                        + SOUS_SEPERATEUR + 4
                        + SOUS_SEPERATEUR + idTheso
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getNarrowMatchs())) {
            if (alignements == null) {
                alignements = "";
            }
            for (String uri : conceptObject.getNarrowMatchs()) {
                alignements = alignements + SEPERATEUR + idUser
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + ""
                        + SOUS_SEPERATEUR + uri
                        + SOUS_SEPERATEUR + 5
                        + SOUS_SEPERATEUR + idTheso
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (alignements != null && alignements.length() > 0) {
            alignements = alignements.substring(SEPERATEUR.length(), alignements.length());
        }

        String prefTerm = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getPrefLabels())) {
            prefTerm = "";
            for (CsvReadHelper.Label label : conceptObject.getPrefLabels()) {
                prefTerm += SEPERATEUR + label.getLabel() + SOUS_SEPERATEUR + label.getLang();
            }
            if (prefTerm.length() > 0) {
                prefTerm = prefTerm.substring(SEPERATEUR.length(), prefTerm.length());
            }
        }

        //Non Pref Term
        //-- 'id_term@lexicalValue@lang@id_thesaurus@source@status@hiden'
        String nonPrefTerm = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getAltLabels())) {
            nonPrefTerm = "";
            for (CsvReadHelper.Label label : conceptObject.getAltLabels()) {
                nonPrefTerm = nonPrefTerm + SEPERATEUR + conceptObject.getIdConcept()
                        + SOUS_SEPERATEUR + label.getLabel()
                        + SOUS_SEPERATEUR + label.getLang()
                        + SOUS_SEPERATEUR + idTheso
                        + SOUS_SEPERATEUR + idUser
                        + SOUS_SEPERATEUR + "USE"
                        + SOUS_SEPERATEUR + false;
            }
        }

        if (CollectionUtils.isNotEmpty(conceptObject.getAltLabels())) {
            if (nonPrefTerm == null) {
                nonPrefTerm = "";
            }
            for (CsvReadHelper.Label altLabel : conceptObject.getHiddenLabels()) {
                nonPrefTerm = nonPrefTerm + SEPERATEUR + conceptObject.getIdConcept()
                        + SOUS_SEPERATEUR + altLabel.getLabel()
                        + SOUS_SEPERATEUR + altLabel.getLang()
                        + SOUS_SEPERATEUR + idTheso
                        + SOUS_SEPERATEUR + idUser
                        + SOUS_SEPERATEUR + "Hiddden"
                        + SOUS_SEPERATEUR + true;
            }
        }
        if (nonPrefTerm != null && nonPrefTerm.length() > 0) {
            nonPrefTerm = nonPrefTerm.substring(SEPERATEUR.length(), nonPrefTerm.length());
        }

        //Relation
        //-- 'id_concept1@role@id_concept2'
        String relations = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getBroaders())) {
            relations = "";
            isTopConcept = false;
            for (String idConcept2 : conceptObject.getBroaders()) {
                relations += SEPERATEUR + conceptObject.getIdConcept()
                        + SOUS_SEPERATEUR + "BT"
                        + SOUS_SEPERATEUR + idConcept2;
                relations += SEPERATEUR + idConcept2
                        + SOUS_SEPERATEUR + "NT"
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();                
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getNarrowers())) {
            if (relations == null) {
                relations = "";
            }
            for (String idConcept2 : conceptObject.getNarrowers()) {
                relations += SEPERATEUR + conceptObject.getIdConcept()
                        + SOUS_SEPERATEUR + "NT"
                        + SOUS_SEPERATEUR + idConcept2;
                relations += SEPERATEUR + idConcept2
                        + SOUS_SEPERATEUR + "BT"
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();                
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getRelateds())) {
            if (relations == null) {
                relations = "";
            }
            for (String idConcept2 : conceptObject.getRelateds()) {
                relations += SEPERATEUR + conceptObject.getIdConcept()
                        + SOUS_SEPERATEUR + "RT"
                        + SOUS_SEPERATEUR + idConcept2;
                relations += SEPERATEUR + idConcept2
                        + SOUS_SEPERATEUR + "RT"
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();                
            }
        }
        if (relations != null && relations.length() > 0) {
            relations = relations.substring(SEPERATEUR.length(), relations.length());
        }

        //CustomRelation
        //-- 'id_concept1@role@id_concept2'
        String customRelations = null;        
        if (CollectionUtils.isNotEmpty(conceptObject.getCustomRelations())) {
            customRelations = "";
            for (NodeIdValue nodeIdValue  : conceptObject.getCustomRelations()) {
                customRelations += SEPERATEUR + conceptObject.getIdConcept()
                        + SOUS_SEPERATEUR + nodeIdValue.getValue()
                        + SOUS_SEPERATEUR + nodeIdValue.getId();
            }
        }    
        if (customRelations != null && customRelations.length() > 0) {
            customRelations = customRelations.substring(SEPERATEUR.length(), customRelations.length());
        }        

        //Notes
        //-- 'value@typeCode@lang@id_term'
        String notes = getNotes(conceptObject);

        String gps = null;
        if (StringUtils.isNotEmpty(conceptObject.getLatitude())) {
            gps = conceptObject.getLatitude() + SOUS_SEPERATEUR + conceptObject.getLongitude();
        }
        if (StringUtils.isNotEmpty(conceptObject.getGps())) {
            if (gps == null) {
                gps = "";
            } else {
                gps += gps + SEPERATEUR;
            }
            var gpsList = ConceptView.readGps(conceptObject.getGps(), "", "");
            if (CollectionUtils.isNotEmpty(gpsList)) {
                for (Gps gpsValue : gpsList) {
                    gps += SEPERATEUR + gpsValue.getLatitude() + SOUS_SEPERATEUR + gpsValue.getLongitude();
                }
            }
        }

        try {
            conceptRepository.addNewConcept(
                    idTheso,
                    conceptObject.getIdConcept(),
                    idUser,
                    conceptStatus,
                    conceptType,
                    conceptObject.getNotation(),
                    conceptObject.getArkId(),
                    isTopConcept,
                    idHandle,
                    idDoi,
                    (prefTerm == null ? null : prefTerm),
                    relations,
                    customRelations,
                    (notes == null ? null : notes),
                    (nonPrefTerm == null ? null : nonPrefTerm),
                    (alignements == null ? null : alignements),
                    images,
                    replacedBy,
                    gps != null,
                    gps,
                    toSqlDate(conceptObject.getCreated() == null ? null : new SimpleDateFormat(formatDate).parse(conceptObject.getCreated())),
                    toSqlDate(conceptObject.getModified() == null ? null : new SimpleDateFormat(formatDate).parse(conceptObject.getModified())),
                    null);

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à opentheso_add_new_concept pour le concept {} : {}", conceptObject.getIdConcept(), e.getMessage(), e);
            message += "Erreur concept : " + prefTerm + " (" + conceptObject.getIdConcept() + ")\n";
            return false;
        }


        addExternalResources(idTheso, conceptObject.getIdConcept(), conceptObject.getExternalResources());        
        return true;
    }

    private String getNotes(CsvReadHelper.ConceptObject conceptObject){
        //Notes
        //-- 'value@typeCode@lang@id_term'
        String notes = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getNote())) {
            notes = "";
            for (CsvReadHelper.Label note : conceptObject.getNote()) {
                notes += SEPERATEUR + note.getLabel()
                        + SOUS_SEPERATEUR + "note"
                        + SOUS_SEPERATEUR + note.getLang()
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getDefinitions())) {
            if (notes == null) {
                notes = "";
            }
            for (CsvReadHelper.Label definition : conceptObject.getDefinitions()) {
                notes += SEPERATEUR + definition.getLabel()
                        + SOUS_SEPERATEUR + "definition"
                        + SOUS_SEPERATEUR + definition.getLang()
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getChangeNotes())) {
            if (notes == null) {
                notes = "";
            }
            for (CsvReadHelper.Label changeNote : conceptObject.getChangeNotes()) {
                notes += SEPERATEUR + changeNote.getLabel()
                        + SOUS_SEPERATEUR + "changeNote"
                        + SOUS_SEPERATEUR + changeNote.getLang()
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getEditorialNotes())) {
            if (notes == null) {
                notes = "";
            }
            for (CsvReadHelper.Label editorialNote : conceptObject.getEditorialNotes()) {
                notes += SEPERATEUR + editorialNote.getLabel()
                        + SOUS_SEPERATEUR + "editorialNote"
                        + SOUS_SEPERATEUR + editorialNote.getLang()
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getHistoryNotes())) {
            if (notes == null) {
                notes = "";
            }
            for (CsvReadHelper.Label historyNote : conceptObject.getHistoryNotes()) {
                notes += SEPERATEUR + historyNote.getLabel()
                        + SOUS_SEPERATEUR + "historyNote"
                        + SOUS_SEPERATEUR + historyNote.getLang()
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getScopeNotes())) {
            if (notes == null) {
                notes = "";
            }
            for (CsvReadHelper.Label scopeNote : conceptObject.getScopeNotes()) {
                notes += SEPERATEUR + scopeNote.getLabel()
                        + SOUS_SEPERATEUR + "scopeNote"
                        + SOUS_SEPERATEUR + scopeNote.getLang()
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getExamples())) {
            if (notes == null) {
                notes = "";
            }
            for (CsvReadHelper.Label example : conceptObject.getExamples()) {
                notes += SEPERATEUR + example.getLabel()
                        + SOUS_SEPERATEUR + "example"
                        + SOUS_SEPERATEUR + example.getLang()
                        + SOUS_SEPERATEUR + conceptObject.getIdConcept();
            }
        }
        if (notes != null && notes.length() > 0) {
            notes = notes.substring(SEPERATEUR.length(), notes.length());
        } 
        return notes;
    }
    
    private void addExternalResources(String idTheso, String idConcept, ArrayList<String> externalResources) {
        
        for (String externalResource : externalResources) {
            if(externalResource == null || externalResource.isEmpty()) {
                return;
            }
            if(!fr.cnrs.opentheso.utils.StringUtils.urlValidator(externalResource)){
                return;            
            }

            externalResourcesRepository.save(ExternalResource.builder().idConcept(idConcept).idThesaurus(idTheso)
                    .externalUri(externalResource).build());
        }
    }

    public void addFacets(CsvReadHelper.ConceptObject conceptObject, String idTheso) {

        if (conceptObject.getIdConcept() == null) {
            return;
        }

        if (conceptObject.getPrefLabels().isEmpty()) {
            return;
        }

        String idConceptParent = conceptObject.getSuperOrdinate();
        if(StringUtils.isEmpty(idConceptParent)) return;

        String labels = "";
        for (CsvReadHelper.Label prefLabel : conceptObject.getPrefLabels()) {
            if(StringUtils.isEmpty(labels)){
                labels = prefLabel.getLabel() + SOUS_SEPERATEUR + prefLabel.getLang();
            } else {
                labels = labels + SEPERATEUR + prefLabel.getLabel() + SOUS_SEPERATEUR + prefLabel.getLang();
            }
        }

        String membres = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getMembers())) {
            membres = "";
            for (String member : conceptObject.getMembers()) {
                if(StringUtils.isEmpty(membres)){
                    membres = member;
                } else {
                    membres = membres + SEPERATEUR + member;
                }
            }
        }

        var notes = getNotes(conceptObject);

        conceptFacetRepository.addFacet(conceptObject.getIdConcept(), idUser, idTheso, idConceptParent, labels, membres, notes);
    }    
    
    private boolean addPrefLabel(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
            message = message + "\n" + "concept sans identifiant : " + conceptObject.getPrefLabels().toString();
            return false;
        }
        Concept concept = new Concept();

        // On vérifie si le concept a des BT (termes génériques), alors il faut ajouter un TopTerm, sinon, c'est un concept avec des rerlations
        if (conceptObject.getBroaders().isEmpty()) {
            concept.setTopConcept(true);
        } else {
            concept.setTopConcept(false);
        }

        concept.setIdThesaurus(idTheso);
        concept.setStatus("");
        concept.setNotation(conceptObject.getNotation());
        concept.setIdConcept(conceptObject.getIdConcept());
        concept.setIdArk(conceptObject.getArkId());
        
        if (StringUtils.isEmpty(formatDate)) {
            formatDate = "dd-mm-yyyy";
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatDate);        
        try {
            if(conceptObject.getCreated() != null && !conceptObject.getCreated().isEmpty())
                concept.setCreated(simpleDateFormat.parse(conceptObject.getCreated()));
            if(conceptObject.getModified() != null && !conceptObject.getModified().isEmpty())
                concept.setModified(simpleDateFormat.parse(conceptObject.getModified()));            
        } catch (ParseException ex) {
            Logger.getLogger(CsvImportHelper.class.getName()).log(Level.SEVERE, null, ex);
        }

        // ajout du concept
        conceptService.insertConcept(concept);

        Term term = new Term();
        term.setIdThesaurus(idTheso);
        term.setIdTerm(conceptObject.getIdConcept());
        conceptObject.setIdTerm(conceptObject.getIdConcept());
        term.setContributor(idUser);
        term.setCreator(idUser);
        term.setSource("");
        term.setStatus("");

        if (!termService.addLinkTerm(term.getIdConcept(), term.getIdThesaurus(), conceptObject.getIdTerm())) {
            message = message + "\n" + "erreur dans l'intégration du concept " + conceptObject.getIdConcept();
            return false;
        }

        // ajout des PrefLabel
        for (CsvReadHelper.Label prefLabel : conceptObject.getPrefLabels()) {
            term.setLang(prefLabel.getLang());
            term.setLexicalValue(prefLabel.getLabel());
            termService.addTermTraduction(term, idUser);
        }

        return true;
    }

    /**
     * Intègre les synonymes et synonymes cachés
     */
    private boolean addAltLabels(String idTheso, CsvReadHelper.ConceptObject conceptObject) {
        Term term = new Term();
        for (CsvReadHelper.Label altLabel : conceptObject.getAltLabels()) {
            term.setIdTerm(conceptObject.getIdTerm());
            term.setIdThesaurus(idTheso);
            term.setLang(altLabel.getLang());
            term.setLexicalValue(altLabel.getLabel());
            term.setHidden(false);
            term.setStatus("USE");
            term.setSource("");
            nonPreferredTermService.addNonPreferredTerm(term, idUser);
        }
        for (CsvReadHelper.Label altLabel : conceptObject.getHiddenLabels()) {
            term.setIdTerm(conceptObject.getIdTerm());
            term.setIdThesaurus(idTheso);
            term.setLang(altLabel.getLang());
            term.setLexicalValue(altLabel.getLabel());
            term.setHidden(true);
            term.setStatus("Hiddden");
            term.setSource("");

            nonPreferredTermService.addNonPreferredTerm(term, idUser);
        }
        return true;
    }

    /**
     * Intègre les notes
     */
    private boolean addNotes(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        for (CsvReadHelper.Label note : conceptObject.getNote()) {
            noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "note","", idUser);
        }
        for (CsvReadHelper.Label note : conceptObject.getDefinitions()) {
            noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "definition","", idUser);
        }
        for (CsvReadHelper.Label note : conceptObject.getChangeNotes()) {
            noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "changeNote","", idUser);
        }
        for (CsvReadHelper.Label note : conceptObject.getEditorialNotes()) {
            noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "editorialNote","", idUser);
        }
        for (CsvReadHelper.Label note : conceptObject.getHistoryNotes()) {
            noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "historyNote", "",idUser);
        }
        for (CsvReadHelper.Label note : conceptObject.getScopeNotes()) {
            noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "scopeNote", "",idUser);
        }
        for (CsvReadHelper.Label note : conceptObject.getExamples()) {
            noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "example", "",idUser);
        }
        return true;
    }

    private boolean addRelations(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        for (String idConcept2 : conceptObject.getBroaders()) {
            if (relationService.addHierarchicalRelation(conceptObject.getIdConcept(), idTheso, "BT", idConcept2) == null) {
                message = message + "\n" + "erreur dans de la relation BT: " + conceptObject.getIdConcept();
            }
            // pour créer la relation réciproque si elle n'existe pas
            if (relationService.addHierarchicalRelation(idConcept2, idTheso, "NT", conceptObject.getIdConcept()) == null) {
                message = message + "\n" + "erreur dans de la relation BT: " + conceptObject.getIdConcept();
            }
            conceptService.setTopConcept(conceptObject.getIdConcept(), idTheso, false);
        }

        for (String idConcept2 : conceptObject.getNarrowers()) {
            if (relationService.addHierarchicalRelation(conceptObject.getIdConcept(), idTheso, "NT", idConcept2) == null) {
                message = message + "\n" + "erreur dans de la relation NT: " + conceptObject.getIdConcept();
            }
            // pour créer la relation réciproque si elle n'existe pas
            if (relationService.addHierarchicalRelation(idConcept2, idTheso, "BT", conceptObject.getIdConcept()) == null) {
                message = message + "\n" + "erreur dans de la relation NT: " + conceptObject.getIdConcept();
            }
        }

        for (String idConcept2 : conceptObject.getRelateds()) {
            if (relationService.addHierarchicalRelation(conceptObject.getIdConcept(), idTheso, "RT", idConcept2) == null) {
                message = message + "\n" + "erreur dans de la relation RT: " + conceptObject.getIdConcept();
            }
            // pour créer la relation réciproque si elle n'existe pas
            if (relationService.addHierarchicalRelation(idConcept2, idTheso, "RT", conceptObject.getIdConcept()) == null) {
                message = message + "\n" + "erreur dans de la relation RT: " + conceptObject.getIdConcept();
            }
        }
        return true;
    }

    private boolean addAlignments(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        NodeAlignment nodeAlignment = new NodeAlignment();
        nodeAlignment.setId_author(idUser);
        nodeAlignment.setConcept_target("");
        nodeAlignment.setThesaurus_target("");
        nodeAlignment.setInternal_id_concept(conceptObject.getIdConcept());
        nodeAlignment.setInternal_id_thesaurus(idTheso);

//        exactMatch   = 1;
//        closeMatch   = 2;
//        broadMatch   = 3;
//        relatedMatch = 4;        
//        narrowMatch  = 5;
        for (String uri : conceptObject.getExactMatchs()) {
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(1);
            if (!alignmentService.addNewAlignment(nodeAlignment)) {
                message = message + "\n" + "erreur dans l'ajout de l'alignement : " + conceptObject.getIdConcept();
            }
        }
        for (String uri : conceptObject.getCloseMatchs()) {
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(2);
            if (!alignmentService.addNewAlignment(nodeAlignment)) {
                message = message + "\n" + "erreur dans l'ajout de l'alignement : " + conceptObject.getIdConcept();
            }
        }
        for (String uri : conceptObject.getBroadMatchs()) {
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(3);
            if (!alignmentService.addNewAlignment(nodeAlignment)) {
                message = message + "\n" + "erreur dans l'ajout de l'alignement : " + conceptObject.getIdConcept();
            }
        }
        for (String uri : conceptObject.getRelatedMatchs()) {
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(4);
            if (!alignmentService.addNewAlignment(nodeAlignment)) {
                message = message + "\n" + "erreur dans l'ajout de l'alignement : " + conceptObject.getIdConcept();
            }
        }
        for (String uri : conceptObject.getNarrowMatchs()) {
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(5);
            if (!alignmentService.addNewAlignment(nodeAlignment)) {
                message = message + "\n" + "erreur dans l'ajout de l'alignement : " + conceptObject.getIdConcept();
            }
        }

        return true;
    }

    private boolean addConceptToFacet(String idTheso, CsvReadHelper.ConceptObject conceptObject) {
        if (CollectionUtils.isEmpty(conceptObject.getMemberOfFacets())) {
            return true;
        }
        for(String idFacet : conceptObject.getMemberOfFacets()) {
            facetService.addConceptToFacet(idFacet, idTheso, conceptObject.getIdConcept());
        }
        return true;
    }

    private boolean addConceptToCollection(String idTheso, CsvReadHelper.ConceptObject conceptObject) {
        if (CollectionUtils.isEmpty(conceptObject.getMembers())) {
            return true;
        }
        for(String idGroup : conceptObject.getMembers()) {
            groupService.addConceptGroupConcept(idGroup,  conceptObject.getIdConcept(), idTheso);
        }
        return true;
    }

    private boolean addGeoLocalisation(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        if (StringUtils.isEmpty(conceptObject.getLatitude())) {
            if (StringUtils.isEmpty(conceptObject.getGps()) || conceptObject.getGps().length() < 3) {
                return true;
            }

            String[] values = conceptObject.getGps().split("##");
            for (String value1 : values) {
                String[] gps = value1.split("@@");
                gpsService.insertCoordinates(conceptObject.getIdConcept(), idTheso,
                        Double.valueOf(gps[1]), Double.valueOf(gps[2]));
            }
            return true;
        } else {
            return addPointGeoLocalisation(idTheso, conceptObject);
        }
    }
    private boolean addPointGeoLocalisation(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        Double latitude;
        Double longitude;

        if (conceptObject.getLatitude() == null || conceptObject.getLatitude().isEmpty()) {
            return true;
        }
        if (conceptObject.getLongitude() == null || conceptObject.getLongitude().isEmpty()) {
            return true;
        }
        try {
            latitude = Double.parseDouble(conceptObject.getLatitude());
            longitude = Double.parseDouble(conceptObject.getLongitude());
        } catch (Exception e) {
            return true;
        }

        gpsService.insertCoordinates(conceptObject.getIdConcept(), idTheso, latitude, longitude);
        return true;
    }
    
    
    private boolean addMembers(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        if (!conceptObject.getMembers().isEmpty()) {
            for (String member : conceptObject.getMembers()) {
                groupService.addConceptGroupConcept(member.trim(), conceptObject.getIdConcept(), idTheso);
            }
        }
        return true;
    }

/////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////// mise à jour des concepts /////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////    
    
    
    
    
    
    //////////////////////////////////////////////////////////////////////////////////////    
    //////// Méthodes pour le remplacement des valeurs suivant le type de données ////////    
    //////////////////////////////////////////////////////////////////////////////////////
    /**
     * permet de remplacer un lot de concepts, l'ancienne valeure par la nouvelle
     * @param idTheso
     * @param nodeReplaceValueByValue
     * @param idUser1
     * @return 
     */
    public boolean updateConceptValueByNewValue(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser1) {
        message = "";
        switch (nodeReplaceValueByValue.getSKOSProperty()) {
            case SKOSProperty.PREF_LABEL:
                if(!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
                    if (!updatePrefLabel(idTheso, nodeReplaceValueByValue, idUser1)) {
                        addMessage("Erreur : ", nodeReplaceValueByValue);
                    }
                }
                break;
            case SKOSProperty.ALT_LABEL:
                if (!updateAltLabel(idTheso, nodeReplaceValueByValue, idUser1)) {
                    addMessage("Erreur : ", nodeReplaceValueByValue);
                }
                break;    
            case SKOSProperty.DEFINITION:
                if (!updateDefinition(idTheso, nodeReplaceValueByValue, idUser1)) {
                    addMessage("Erreur : ", nodeReplaceValueByValue);
                }
                break;                  
                
                
            /*    Action dangereuse, à activer plus tard */
            case SKOSProperty.BROADER:
                if (!updateBroader(idTheso, nodeReplaceValueByValue, idUser1)) {
                    addMessage("Erreur : ", nodeReplaceValueByValue);
                }
                break;                
                
            default:
                break;
        }
        return true;
    }

    private boolean updatePrefLabel(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser1) {
        if (nodeReplaceValueByValue.getIdConcept() == null || nodeReplaceValueByValue.getIdConcept().isEmpty()) {
            addMessage("concept sans identifiant :", nodeReplaceValueByValue);
            return false;
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, nodeReplaceValueByValue.getIdConcept());
        if(termService.isTermExistInLangAndThesaurus(preferredTerm.get().getIdTerm(),idTheso, nodeReplaceValueByValue.getIdLang())) {
            termService.updateTermTraduction(nodeReplaceValueByValue.getNewValue(), preferredTerm.get().getIdTerm(),
                    nodeReplaceValueByValue.getIdLang(), idTheso, idUser1);
        } else {
            termService.addTermTraduction(nodeReplaceValueByValue.getNewValue(), preferredTerm.get().getIdTerm(),
                    nodeReplaceValueByValue.getIdLang(), idTheso, idUser1);
        }
        return true;
    }

    private boolean updateAltLabel(String idThesaurus, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser1) {
        if (nodeReplaceValueByValue.getIdConcept() == null || nodeReplaceValueByValue.getIdConcept().isEmpty()) {
            addMessage("concept sans identifiant :", nodeReplaceValueByValue);
            return false;
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idThesaurus, nodeReplaceValueByValue.getIdConcept());
        if (preferredTerm.isEmpty()) {
            return false;
        }
        
        // si l'ancienne valeur et la nouvelle valeur sont présente
        if(!StringUtils.isEmpty(nodeReplaceValueByValue.getOldValue())) {
            if(!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
                // on met remplace la valeur du altLabel par la nouvelle valeur
                if(nonPreferredTermService.updateNonPreferredTerm(nodeReplaceValueByValue.getOldValue(),
                        nodeReplaceValueByValue.getNewValue(), preferredTerm.get().getIdTerm(), nodeReplaceValueByValue.getIdLang(),
                        idThesaurus, false, idUser1)) {
                    addMessage("Rename AltLabel error :", nodeReplaceValueByValue);
                }                
            }
        } else {
            if(!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
                // on ajoute un nouvel altLabel
                var term = Term.builder()
                        .idTerm(preferredTerm.get().getIdTerm())
                        .lexicalValue(nodeReplaceValueByValue.getNewValue())
                        .lang(nodeReplaceValueByValue.getIdLang())
                        .idThesaurus(idThesaurus)
                        .hidden(false)
                        .source("")
                        .status("")
                        .build();
                nonPreferredTermService.addNonPreferredTerm(term, idUser1);
            }
        }
        return true;
    }

    private boolean updateDefinition(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser1) {
        if (nodeReplaceValueByValue.getIdConcept() == null || nodeReplaceValueByValue.getIdConcept().isEmpty()) {
            addMessage("concept sans identifiant :", nodeReplaceValueByValue);
            return false;
        }
       
        // si l'ancienne valeur et la nouvelle valeur sont présente
        if(!StringUtils.isEmpty(nodeReplaceValueByValue.getOldValue())) {
            if(!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
                // on supprime d'abord l'ancienne note
                int idNote = noteService.getNoteByValueAndThesaurus(nodeReplaceValueByValue.getOldValue(),
                        "definition", nodeReplaceValueByValue.getIdLang(), idTheso); 
                if(idNote != -1){
                    // on remplace la valeur du altLabel par la nouvelle valeur
                    if(!noteService.updateNote(idNote, nodeReplaceValueByValue.getIdConcept(), nodeReplaceValueByValue.getIdLang(), idTheso,
                            nodeReplaceValueByValue.getNewValue(), "", "definition", idUser1)) {
                        addMessage("Rename definition error :", nodeReplaceValueByValue);
                    }                      
                } else {
                    if (!noteService.isNoteExist(nodeReplaceValueByValue.getIdConcept(), idTheso,
                            nodeReplaceValueByValue.getIdLang(), nodeReplaceValueByValue.getNewValue(), "definition")) {
                        noteService.addNote(nodeReplaceValueByValue.getIdConcept(), nodeReplaceValueByValue.getIdLang(), idTheso,
                                nodeReplaceValueByValue.getNewValue(), "definition", "", idUser1);
                    }                    
                }
            }
        } else {
            if(!StringUtils.isEmpty(nodeReplaceValueByValue.getNewValue())) {
                // on ajoute une nouvelle définition
                if (!noteService.isNoteExist(nodeReplaceValueByValue.getIdConcept(), idTheso,
                        nodeReplaceValueByValue.getIdLang(), nodeReplaceValueByValue.getNewValue(), "definition")) {
                    noteService.addNote(nodeReplaceValueByValue.getIdConcept(), nodeReplaceValueByValue.getIdLang(), idTheso,
                            nodeReplaceValueByValue.getNewValue(), "definition", "", idUser1);
                }
            }
        }
        return true;
    }      
    private boolean updateBroader(String idTheso, NodeReplaceValueByValue nodeReplaceValueByValue, int idUser1) {
        if (nodeReplaceValueByValue.getIdConcept() == null || nodeReplaceValueByValue.getIdConcept().isEmpty()) {
            addMessage("concept sans identifiant :", nodeReplaceValueByValue);
            return false;
        }

        if(!StringUtils.isEmpty( nodeReplaceValueByValue.getOldValue())){
            relationService.deleteRelationBT(nodeReplaceValueByValue.getIdConcept(), idTheso, nodeReplaceValueByValue.getOldValue(), idUser1);
        }

        relationService.addRelationBT(nodeReplaceValueByValue.getIdConcept(), idTheso, nodeReplaceValueByValue.getNewValue(), idUser1);
        conceptService.setTopConcept(nodeReplaceValueByValue.getIdConcept(), idTheso, false);
        return true;
    }      
    private void addMessage(String error, NodeReplaceValueByValue nodeReplaceValueByValue) {
        message = message + error + nodeReplaceValueByValue.getIdConcept() + "##" + nodeReplaceValueByValue.getOldValue() + "##" + nodeReplaceValueByValue.getNewValue() + "##" + nodeReplaceValueByValue.getIdLang() + "\n";
    }

    
    public boolean updateConcept(String idTheso, CsvReadHelper.ConceptObject conceptObject, int idUser1) {

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, conceptObject.getIdConcept());
        conceptObject.setIdTerm(preferredTerm.map(PreferredTerm::getIdTerm).orElse(null));
        updatePrefLabel(idTheso, conceptObject, idUser1);
        updateAltLabel(idTheso, conceptObject, idUser1);
        updateNotes(idTheso, conceptObject, idUser1);
        updateAlignments(idTheso, conceptObject, idUser1);
        updateGeoLocalisation(idTheso, conceptObject);
        updateImages(idTheso, conceptObject, idUser1);
        
        addExternalResources(idTheso, conceptObject.getIdConcept(), conceptObject.getExternalResources()); 
        return true;
    }

    private boolean updatePrefLabel(String idTheso, CsvReadHelper.ConceptObject conceptObject, int idUser1) {
        if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
            message = message + "\n" + "concept sans identifiant : " + conceptObject.getPrefLabels().toString();
            return false;
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, conceptObject.getIdConcept());
        if(preferredTerm.isEmpty()) {
            return false;
        }

        String oldLabel;

        for (CsvReadHelper.Label prefLabel : conceptObject.getPrefLabels()) {
            // si le label n'existe pas dans cette langue, on l'ajoute
            if (!termService.isTermExistInLangAndThesaurus(preferredTerm.get().getIdTerm(), idTheso, prefLabel.getLang())) {
                var termToSave = Term.builder()
                        .lexicalValue(prefLabel.getLabel())
                        .idTerm(preferredTerm.get().getIdTerm())
                        .lang(prefLabel.getLang())
                        .idThesaurus(idTheso)
                        .source("import")
                        .status("")
                        .build();
                termService.addTermTraduction(termToSave, idUser1);
            } else {
                var term = termRepository.findByIdTermAndIdThesaurusAndLang(preferredTerm.get().getIdTerm(), idTheso, prefLabel.getLang());
                oldLabel = term.isPresent() ? term.get().getLexicalValue() : "";
                
                // si le label est fourni vide, il faut alors supprimer cette traduction
                if (prefLabel.getLabel().isEmpty()) {
                    termRepository.deleteByIdTermAndLangAndIdThesaurus(preferredTerm.get().getIdTerm(), prefLabel.getLang(), idTheso);
                    return true;
                } 
                
                // si le label d'origine est vide (cas rare et normalement impossible)
                if (oldLabel.isEmpty()) {
                    //le terme est alors à mettre à jour
                    termService.updateTermTraduction(prefLabel.getLabel(), preferredTerm.get().getIdTerm(), prefLabel.getLang(), idTheso, idUser1);
                    return true;
                }        
                
                // on vérifie si le terme est identique, on ne fait rien
                if (oldLabel.trim().equals(prefLabel.getLabel().trim())) {
                    continue;
                }  
                
                // le terme est alors à mettre à jour
                termService.updateTermTraduction(prefLabel.getLabel(), preferredTerm.get().getIdTerm(), prefLabel.getLang(), idTheso, idUser1);
            }            
        }
        return true;
    }

    private boolean updateAltLabel(String idTheso, CsvReadHelper.ConceptObject conceptObject, int idUser1) {
        if (conceptObject.getIdConcept() == null || conceptObject.getIdConcept().isEmpty()) {
            message = message + "\n" + "concept sans identifiant : " + conceptObject.getPrefLabels().toString();
            return false;
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idTheso, conceptObject.getIdConcept());
        if(preferredTerm.isEmpty()) {
            return false;
        }

        // suppression des altLabel par langue
        var langs = getLangs(conceptObject.getAltLabels());
        for (String lang : langs) {
            var oldLabels = nonPreferredTermService.getNonPreferredTermValue(preferredTerm.get().getIdTerm(), idTheso, lang);
            for (String oldLabel : oldLabels) {
                nonPreferredTermService.deleteNonPreferredTerm(preferredTerm.get().getIdTerm(), lang, oldLabel, idTheso, idUser1);
            }
        }

        for (CsvReadHelper.Label altLabel : conceptObject.getAltLabels()) {
            // on ajoute les nouveaux prefLabels dans cette langue
            if (!altLabel.getLabel().isEmpty()) {
                var term = Term.builder()
                        .idTerm(preferredTerm.get().getIdTerm())
                        .lexicalValue(altLabel.getLabel())
                        .lang(altLabel.getLang())
                        .idThesaurus(idTheso)
                        .hidden(false)
                        .source("import")
                        .status("")
                        .build();
                nonPreferredTermService.addNonPreferredTerm(term, idUser);
            }
        }
        return true;
    }

    private ArrayList<String> getLangs(ArrayList<CsvReadHelper.Label> labels) {
        ArrayList<String> langs = new ArrayList<>();
        for (CsvReadHelper.Label label : labels) {
            if (!langs.contains(label.getLang())) {
                langs.add(label.getLang());
            }
        }
        return langs;
    }

    /**
     * Intègre les notes
     *
     * @param idTheso
     * @param conceptObject
     * @return
     */
    private boolean updateNotes(String idTheso, CsvReadHelper.ConceptObject conceptObject, int idUser1) {

        // suppression des Notes  par langue
        ArrayList<String> langs = getLangs(conceptObject.getNote());
        for (String lang : langs) {
            noteService.deleteNoteByLang(conceptObject.getIdConcept(), idTheso, lang, "note");
        }
        for (CsvReadHelper.Label note : conceptObject.getNote()) {
            if (!note.getLabel().isEmpty()) {
                noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "note","", idUser1);
            }
        }
        langs = getLangs(conceptObject.getScopeNotes());
        for (String lang : langs) {
            noteService.deleteNoteByLang(conceptObject.getIdConcept(), idTheso, lang, "scopeNote");
        }
        for (CsvReadHelper.Label note : conceptObject.getScopeNotes()) {
            if (!note.getLabel().isEmpty()) {
                noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(),
                        "scopeNote","", idUser1);
            }
        }

        langs = getLangs(conceptObject.getDefinitions());
        for (String lang : langs) {
            noteService.deleteNoteByLang(conceptObject.getIdConcept(), idTheso, lang, "definition");
        }
        for (CsvReadHelper.Label note : conceptObject.getDefinitions()) {
            if (!note.getLabel().isEmpty()) {
                noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "definition", "",idUser1);
            }
        }

        langs = getLangs(conceptObject.getChangeNotes());
        for (String lang : langs) {
            noteService.deleteNoteByLang(conceptObject.getIdConcept(), idTheso, lang, "changeNote");
        }
        for (CsvReadHelper.Label note : conceptObject.getChangeNotes()) {
            if (!note.getLabel().isEmpty()) {
                noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "changeNote", "",idUser1);
            }
        }

        langs = getLangs(conceptObject.getEditorialNotes());
        for (String lang : langs) {
            noteService.deleteNoteByLang(conceptObject.getIdConcept(), idTheso, lang, "editorialNote");
        }
        for (CsvReadHelper.Label note : conceptObject.getEditorialNotes()) {
            if (!note.getLabel().isEmpty()) {
                noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "editorialNote", "",idUser1);
            }
        }

        langs = getLangs(conceptObject.getHistoryNotes());
        for (String lang : langs) {
            noteService.deleteNoteByLang(conceptObject.getIdConcept(), idTheso, lang, "historyNote");
        }
        for (CsvReadHelper.Label note : conceptObject.getHistoryNotes()) {
            if (!note.getLabel().isEmpty()) {
                noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "historyNote", "",idUser1);
            }
        }

        langs = getLangs(conceptObject.getExamples());
        for (String lang : langs) {
            noteService.deleteNoteByLang(conceptObject.getIdConcept(), idTheso, lang, "example");
        }
        for (CsvReadHelper.Label note : conceptObject.getExamples()) {
            if (!note.getLabel().isEmpty()) {
                noteService.addNote(conceptObject.getIdConcept(), note.getLang(), idTheso, note.getLabel(), "example", "",idUser1);
            }
        }
        return true;
    }

    private boolean updateAlignments(String idTheso, CsvReadHelper.ConceptObject conceptObject, int idUser1) {

        NodeAlignment nodeAlignment = new NodeAlignment();
        nodeAlignment.setId_author(idUser1);
        nodeAlignment.setConcept_target("");
        nodeAlignment.setThesaurus_target("");
        nodeAlignment.setInternal_id_concept(conceptObject.getIdConcept());
        nodeAlignment.setInternal_id_thesaurus(idTheso);

        /// suppression des alignements 
        if (!conceptObject.getExactMatchs().isEmpty()) {
            alignmentService.deleteAlignmentOfConceptByType(conceptObject.getIdConcept(), idTheso, 1);
        }
        for (String uri : conceptObject.getExactMatchs()) {
            if (uri.isEmpty()) {
                continue;
            }
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(1);
            alignmentService.addNewAlignment(nodeAlignment);
        }

        if (!conceptObject.getCloseMatchs().isEmpty()) {
            alignmentService.deleteAlignmentOfConceptByType(conceptObject.getIdConcept(), idTheso, 2);
        }
        for (String uri : conceptObject.getCloseMatchs()) {
            if (uri.isEmpty()) {
                continue;
            }
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(2);
            alignmentService.addNewAlignment(nodeAlignment);
        }

        if (!conceptObject.getBroadMatchs().isEmpty()) {
            alignmentService.deleteAlignmentOfConceptByType(conceptObject.getIdConcept(), idTheso, 3);
        }
        for (String uri : conceptObject.getBroadMatchs()) {
            if (uri.isEmpty()) {
                continue;
            }
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(3);
            alignmentService.addNewAlignment(nodeAlignment);
        }

        if (!conceptObject.getRelatedMatchs().isEmpty()) {
            alignmentService.deleteAlignmentOfConceptByType(conceptObject.getIdConcept(), idTheso, 4);
        }
        for (String uri : conceptObject.getRelatedMatchs()) {
            if (uri.isEmpty()) {
                continue;
            }
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(4);
            alignmentService.addNewAlignment(nodeAlignment);
        }

        if (!conceptObject.getNarrowMatchs().isEmpty()) {
            alignmentService.deleteAlignmentOfConceptByType(conceptObject.getIdConcept(), idTheso, 5);
        }
        for (String uri : conceptObject.getNarrowMatchs()) {
            if (uri.isEmpty()) {
                continue;
            }
            nodeAlignment.setUri_target(uri);
            nodeAlignment.setAlignement_id_type(5);
            alignmentService.addNewAlignment(nodeAlignment);
        }

        return true;
    }

    private boolean updateGeoLocalisation(String idTheso, CsvReadHelper.ConceptObject conceptObject) {

        if (conceptObject.getLatitude() == null || conceptObject.getLongitude() == null) {
            if(conceptObject.getGps() == null || conceptObject.getGps().isEmpty())
                return true;
        }

        // nettoyege des ancienne valeurs
        gpsService.deleteGpsByConceptIdAndThesaurusId(conceptObject.getIdConcept(), idTheso);

        List<NodeGps> nodeGpses = new ArrayList<>();
        
        if (StringUtils.isNotEmpty(conceptObject.getLatitude())) {
            NodeGps nodeGps = new NodeGps();
            try {
                nodeGps.setLatitude(Double.valueOf(conceptObject.getLatitude()));
                nodeGps.setLongitude(Double.valueOf(conceptObject.getLongitude()));
                nodeGps.setPosition(1);
                nodeGpses.add(nodeGps);
            } catch (Exception e) {
                return true;
            }
            gpsService.saveNewGps(conceptObject.getIdConcept(), idTheso, nodeGpses);
            
        } else {
            if (StringUtils.isNotEmpty(conceptObject.getGps())) {
                var gpsList = ConceptView.readGps(conceptObject.getGps(), "", "");
                if (CollectionUtils.isNotEmpty(gpsList)) {
                    for (Gps gpsValue : gpsList) {
                        NodeGps nodeGps = new NodeGps();
                        nodeGps.setLatitude(gpsValue.getLatitude());
                        nodeGps.setLongitude(gpsValue.getLongitude());
                        nodeGps.setPosition(gpsValue.getPosition());
                        nodeGpses.add(nodeGps);                                            
                    }
                    gpsService.saveNewGps(conceptObject.getIdConcept(), idTheso, nodeGpses);
                }
            }
        }
        return true;
    }

    private boolean updateImages(String idTheso, CsvReadHelper.ConceptObject conceptObject, int idUser1) {

        if (conceptObject.getImages() == null || conceptObject.getImages().isEmpty()) {
            return true;
        }

        imageService.deleteImages(idTheso, conceptObject.getIdConcept(), null);
        
        for (NodeImage nodeImage : conceptObject.getImages()) {

            if(nodeImage == null) continue;

            if (StringUtils.isEmpty(nodeImage.getUri())) continue;

            imageService.addExternalImage(conceptObject.getIdConcept(), idTheso, nodeImage.getImageName(),
                    nodeImage.getCopyRight(), nodeImage.getUri(), "", idUser1);
        }
        return true;
    }

    private static java.sql.Date toSqlDate(Date date) {
        return date == null ? null : new java.sql.Date(date.getTime());
    }
}
