package fr.cnrs.opentheso.v2.toolbox.edition.io.csv;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.ConceptGroupLabelHistorique;
import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.RelationGroup;
import fr.cnrs.opentheso.entites.ThesaurusLabel;
import fr.cnrs.opentheso.entites.UserGroupThesaurus;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.repositories.UserGroupThesaurusRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.support.ThesaurusCsvGpsParser;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
@Slf4j
@Component
@RequiredArgsConstructor
public class ThesaurusCsvImportEngine {

    private static final String SEPERATEUR = "##";
    private static final String SOUS_SEPERATEUR = "@@";

    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ConceptRepository conceptRepository;
    private final ConceptFacetRepository conceptFacetRepository;
    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final ConceptGroupLabelHistoriqueRepository conceptGroupLabelHistoriqueRepository;
    private final ExternalResourcesRepository externalResourcesRepository;
    private final NoteRepository noteRepository;
    private final RelationGroupRepository relationGroupRepository;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final UserGroupThesaurusRepository userGroupThesaurusRepository;

    private String message = "";
    private Preferences nodePreference;
    private String formatDate;
    private int idUser;
    private SimpleDateFormat dateFormat;

    public void setFormatDate(String formatDate) {
        this.formatDate = formatDate;
        this.dateFormat = new SimpleDateFormat(StringUtils.defaultIfBlank(formatDate, "yyyy-MM-dd"));
    }

    public String createThesaurus(String thesoName, String idLang, int idProject, String userName) {
        String idThesaurus = toolboxThesaurusPersistence.createThesaurusId();
        var thesaurus = new Thesaurus();
        thesaurus.setCreator(userName);
        thesaurus.setContributor(userName);
        thesaurus.setLanguage(idLang);
        thesaurus.setId_thesaurus(idThesaurus);
        thesaurus.setTitle(StringUtils.isBlank(thesoName) ? "theso_" + idThesaurus : thesoName);
        toolboxThesaurusPersistence.addTranslation(thesaurus);
        if (idProject != -1) {
            userGroupThesaurusRepository.save(UserGroupThesaurus.builder()
                    .idThesaurus(idThesaurus)
                    .idGroup(idProject)
                    .build());
        }
        return idThesaurus;
    }

    public void addSubGroup(String fatherGroupId, String childGroupId, String thesaurusId) {
        relationGroupRepository.save(RelationGroup.builder()
                .idGroup1(fatherGroupId.toLowerCase())
                .idThesaurus(thesaurusId)
                .relation("sub")
                .idGroup2(childGroupId.toLowerCase())
                .build());
    }


    private String getNotes(ThesaurusCsvConceptObject conceptObject){
        //Notes
        //-- 'value@typeCode@lang@id_term'
        String notes = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getNote())) {
            notes = "";
            for (ThesaurusCsvConceptLabel note : conceptObject.getNote()) {
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
            for (ThesaurusCsvConceptLabel definition : conceptObject.getDefinitions()) {
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
            for (ThesaurusCsvConceptLabel changeNote : conceptObject.getChangeNotes()) {
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
            for (ThesaurusCsvConceptLabel editorialNote : conceptObject.getEditorialNotes()) {
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
            for (ThesaurusCsvConceptLabel historyNote : conceptObject.getHistoryNotes()) {
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
            for (ThesaurusCsvConceptLabel scopeNote : conceptObject.getScopeNotes()) {
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
            for (ThesaurusCsvConceptLabel example : conceptObject.getExamples()) {
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

    private boolean addMembers(String idTheso, ThesaurusCsvConceptObject conceptObject) {

        if (!conceptObject.getMembers().isEmpty()) {
            List<ConceptGroupConcept> links = new ArrayList<>();
            for (String member : conceptObject.getMembers()) {
                links.add(ConceptGroupConcept.builder()
                        .idGroup(member.trim())
                        .idThesaurus(idTheso)
                        .idConcept(conceptObject.getIdConcept())
                        .build());
            }
            conceptGroupConceptRepository.saveAll(links);
        }
        return true;
    }

    public boolean addGroup(String idTheso, ThesaurusCsvConceptObject conceptObject) {
        String idGroup = conceptObject.getIdConcept();
        if (idGroup == null || idGroup.isEmpty()) {
            message = message + "\n" + "Identifiant Groupe manquant";
            return false;
        }
        
        // ajout des concepts à la collection
        if (!conceptObject.getMembers().isEmpty()) {
            List<ConceptGroupConcept> links = new ArrayList<>();
            for (String conceptId : conceptObject.getMembers()) {
                links.add(ConceptGroupConcept.builder()
                        .idGroup(idGroup)
                        .idThesaurus(idTheso)
                        .idConcept(conceptId)
                        .build());
            }
            conceptGroupConceptRepository.saveAll(links);
        }
        
        if (StringUtils.isEmpty(formatDate)) {
            formatDate = "yyyy-MM-dd";
            dateFormat = new SimpleDateFormat(formatDate);
        }
        Date created = null;
        Date modified = null;

        try {
            if(conceptObject.getCreated() != null && !conceptObject.getCreated().isEmpty())
                created = dateFormat.parse(conceptObject.getCreated());
            if(conceptObject.getModified() != null && !conceptObject.getModified().isEmpty())
                modified = dateFormat.parse(conceptObject.getModified());            
        } catch (ParseException ex) {
            Logger.getLogger(ThesaurusCsvImportEngine.class.getName()).log(Level.SEVERE, null, ex);
        }        
        
        insertGroup(idGroup, idTheso, "", "C", conceptObject.getNotation(), created, modified);

        fr.cnrs.opentheso.models.group.ConceptGroupLabel conceptGroupLabel = new fr.cnrs.opentheso.models.group.ConceptGroupLabel();
        for (ThesaurusCsvConceptLabel label : conceptObject.getPrefLabels()) {
            // ajouter les traductions des Groupes
            conceptGroupLabel.setIdgroup(idGroup);
            conceptGroupLabel.setIdthesaurus(idTheso);
            conceptGroupLabel.setLang(label.getLang());
            conceptGroupLabel.setLexicalValue(label.getLabel());
            addGroupTraduction(conceptGroupLabel, idUser);
        }

        addNotes(idTheso, conceptObject);
        
        return true;
    }

    private boolean addNotes(String idTheso, ThesaurusCsvConceptObject conceptObject) {
        addNoteIfPresent(idTheso, conceptObject.getIdConcept(), conceptObject.getNote(), "note");
        addNoteIfPresent(idTheso, conceptObject.getIdConcept(), conceptObject.getDefinitions(), "definition");
        addNoteIfPresent(idTheso, conceptObject.getIdConcept(), conceptObject.getChangeNotes(), "changeNote");
        addNoteIfPresent(idTheso, conceptObject.getIdConcept(), conceptObject.getEditorialNotes(), "editorialNote");
        addNoteIfPresent(idTheso, conceptObject.getIdConcept(), conceptObject.getHistoryNotes(), "historyNote");
        addNoteIfPresent(idTheso, conceptObject.getIdConcept(), conceptObject.getScopeNotes(), "scopeNote");
        addNoteIfPresent(idTheso, conceptObject.getIdConcept(), conceptObject.getExamples(), "example");
        return true;
    }

    private void addNoteIfPresent(String idTheso, String identifier, List<ThesaurusCsvConceptLabel> notes, String noteTypeCode) {
        if (CollectionUtils.isEmpty(notes)) {
            return;
        }
        // Import d'un thésaurus neuf : insert direct sans find-before-insert (évite N+1)
        for (ThesaurusCsvConceptLabel note : notes) {
            String lexicalValue = StringEscapeUtils.unescapeXml(
                    fr.cnrs.opentheso.utils.StringUtils.clearNoteFromP(
                            fr.cnrs.opentheso.utils.StringUtils.clearValue(note.getLabel())));
            noteRepository.save(Note.builder()
                    .noteTypeCode(noteTypeCode)
                    .idThesaurus(idTheso)
                    .lang(note.getLang())
                    .lexicalValue(lexicalValue)
                    .identifier(identifier)
                    .noteSource("")
                    .idUser(idUser)
                    .created(new Date())
                    .modified(new Date())
                    .build());
        }
    }

    public void addFacets(ThesaurusCsvConceptObject conceptObject, String idTheso) {

        if (conceptObject.getIdConcept() == null) {
            return;
        }

        if (conceptObject.getPrefLabels().isEmpty()) {
            return;
        }

        String idConceptParent = conceptObject.getSuperOrdinate();
        if(StringUtils.isEmpty(idConceptParent)) return;

        String labels = "";
        for (ThesaurusCsvConceptLabel prefLabel : conceptObject.getPrefLabels()) {
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

    public boolean addConceptV2(String idTheso, ThesaurusCsvConceptObject conceptObject, int idUser, String formatDate) {

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
            for (ThesaurusCsvConceptLabel label : conceptObject.getPrefLabels()) {
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
            for (ThesaurusCsvConceptLabel label : conceptObject.getAltLabels()) {
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
            for (ThesaurusCsvConceptLabel altLabel : conceptObject.getHiddenLabels()) {
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
            var gpsList = ThesaurusCsvGpsParser.readGps(conceptObject.getGps(), "", "");
            if (CollectionUtils.isNotEmpty(gpsList)) {
                for (Gps gpsValue : gpsList) {
                    gps += SEPERATEUR + gpsValue.getLatitude() + SOUS_SEPERATEUR + gpsValue.getLongitude();
                }
            }
        }

        try {
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat(StringUtils.defaultIfBlank(formatDate, "yyyy-MM-dd"));
            }
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
                    (conceptObject.getCreated()== null ? null : dateFormat.parse(conceptObject.getCreated())),
                    (conceptObject.getModified()== null ? null : dateFormat.parse(conceptObject.getModified())),
                    null);

        } catch (Exception e) {
            log.error("Erreur lors de l'appel à opentheso_add_new_concept pour le concept {} : {}", conceptObject.getIdConcept(), e.getMessage(), e);
            message += "Erreur concept : " + prefTerm + " (" + conceptObject.getIdConcept() + ")\n";
            return false;
        }


        addExternalResources(idTheso, conceptObject.getIdConcept(), conceptObject.getExternalResources());        
        return true;
    }

    public void addLangsToThesaurus(List<String> langs, String idTheso) {
        String primaryTitle = resolvePrimaryThesaurusTitle(idTheso);

        for (String idLang : langs) {
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
        return thesaurusLabelRepository.findByIdThesaurus(idTheso).stream()
                .map(ThesaurusLabel::getTitle)
                .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
                .findFirst()
                .orElse("theso_" + idTheso);
    }

    private void saveConceptGroupConcept(String idGroup, String idConcept, String idThesaurus) {
        conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                .idGroup(idGroup)
                .idThesaurus(idThesaurus)
                .idConcept(idConcept)
                .build());
    }

    private void insertGroup(String idGroup, String idThesaurus, String idArk, String typeCode, String notation,
                             Date created, Date modified) {
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

    private void addGroupTraduction(fr.cnrs.opentheso.models.group.ConceptGroupLabel conceptGroupLabel, int userId) {
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
}
