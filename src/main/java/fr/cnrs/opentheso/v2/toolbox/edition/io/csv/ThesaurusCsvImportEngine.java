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
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

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
        this.dateFormat = new SimpleDateFormat(StringUtils.defaultIfBlank(formatDate, DEFAULT_DATE_FORMAT));
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
        StringBuilder notes = new StringBuilder();
        appendConceptNotes(notes, conceptObject.getNote(), "note", conceptObject.getIdConcept());
        appendConceptNotes(notes, conceptObject.getDefinitions(), "definition", conceptObject.getIdConcept());
        appendConceptNotes(notes, conceptObject.getChangeNotes(), "changeNote", conceptObject.getIdConcept());
        appendConceptNotes(notes, conceptObject.getEditorialNotes(), "editorialNote", conceptObject.getIdConcept());
        appendConceptNotes(notes, conceptObject.getHistoryNotes(), "historyNote", conceptObject.getIdConcept());
        appendConceptNotes(notes, conceptObject.getScopeNotes(), "scopeNote", conceptObject.getIdConcept());
        appendConceptNotes(notes, conceptObject.getExamples(), "example", conceptObject.getIdConcept());
        return stripLeadingSeparator(notes);
    }

    private void appendConceptNotes(StringBuilder notes, List<ThesaurusCsvConceptLabel> labels, String type, String idConcept) {
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }
        for (ThesaurusCsvConceptLabel label : labels) {
            notes.append(SEPERATEUR).append(label.getLabel())
                    .append(SOUS_SEPERATEUR).append(type)
                    .append(SOUS_SEPERATEUR).append(label.getLang())
                    .append(SOUS_SEPERATEUR).append(idConcept);
        }
    }

    private void appendAlignments(StringBuilder alignements, List<String> uris, int idUser, int type, String idTheso, String idConcept) {
        if (CollectionUtils.isEmpty(uris)) {
            return;
        }
        for (String uri : uris) {
            alignements.append(SEPERATEUR).append(idUser)
                    .append(SOUS_SEPERATEUR).append("")
                    .append(SOUS_SEPERATEUR).append("")
                    .append(SOUS_SEPERATEUR).append(uri)
                    .append(SOUS_SEPERATEUR).append(type)
                    .append(SOUS_SEPERATEUR).append(idTheso)
                    .append(SOUS_SEPERATEUR).append(idConcept);
        }
    }

    private void appendRelationPair(StringBuilder relations, String id1, String role1, String id2, String role2) {
        relations.append(SEPERATEUR).append(id1).append(SOUS_SEPERATEUR).append(role1).append(SOUS_SEPERATEUR).append(id2);
        relations.append(SEPERATEUR).append(id2).append(SOUS_SEPERATEUR).append(role2).append(SOUS_SEPERATEUR).append(id1);
    }

    private static String stripLeadingSeparator(StringBuilder builder) {
        if (builder.isEmpty()) {
            return null;
        }
        return builder.substring(SEPERATEUR.length());
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
            formatDate = DEFAULT_DATE_FORMAT;
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

        StringBuilder labels = new StringBuilder();
        for (ThesaurusCsvConceptLabel prefLabel : conceptObject.getPrefLabels()) {
            if(labels.isEmpty()){
                labels.append(prefLabel.getLabel()).append(SOUS_SEPERATEUR).append(prefLabel.getLang());
            } else {
                labels.append(SEPERATEUR).append(prefLabel.getLabel()).append(SOUS_SEPERATEUR).append(prefLabel.getLang());
            }
        }

        String membres = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getMembers())) {
            StringBuilder membresBuilder = new StringBuilder();
            for (String member : conceptObject.getMembers()) {
                if(membresBuilder.isEmpty()){
                    membresBuilder.append(member);
                } else {
                    membresBuilder.append(SEPERATEUR).append(member);
                }
            }
            membres = membresBuilder.toString();
        }

        var notes = getNotes(conceptObject);

        conceptFacetRepository.addFacet(conceptObject.getIdConcept(), idUser, idTheso, idConceptParent, labels.toString(), membres, notes);
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
                StringBuilder replacedByBuilder = new StringBuilder();
                for (String replace : conceptObject.getReplacedBy()) {
                    if(replacedByBuilder.isEmpty()) {
                        replacedByBuilder.append(replace);
                    } else {
                        replacedByBuilder.append(SEPERATEUR).append(replace);
                    }
                }
                replacedBy = replacedByBuilder.toString();
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
            StringBuilder imagesBuilder = new StringBuilder();
            for (NodeImage nodeImage : conceptObject.getImages()) {
                if(nodeImage == null) continue;
                if (StringUtils.isEmpty(nodeImage.getUri())) continue;
                
                if(!imagesBuilder.isEmpty()) {
                    imagesBuilder.append(SEPERATEUR);
                }
                imagesBuilder.append(nodeImage.getImageName()).append(SOUS_SEPERATEUR)
                        .append(nodeImage.getCopyRight()).append(SOUS_SEPERATEUR)
                        .append(nodeImage.getUri()).append(SOUS_SEPERATEUR)
                        .append(nodeImage.getCreator());
            }
            images = imagesBuilder.toString();
        }

        // ALIGNEMENT
        //-- 'author@concept_target@thesaurus_target@uri_target@alignement_id_type@internal_id_thesaurus@internal_id_concept'
        StringBuilder alignementsBuilder = new StringBuilder();
        appendAlignments(alignementsBuilder, conceptObject.getExactMatchs(), idUser, 1, idTheso, conceptObject.getIdConcept());
        appendAlignments(alignementsBuilder, conceptObject.getCloseMatchs(), idUser, 2, idTheso, conceptObject.getIdConcept());
        appendAlignments(alignementsBuilder, conceptObject.getBroadMatchs(), idUser, 3, idTheso, conceptObject.getIdConcept());
        appendAlignments(alignementsBuilder, conceptObject.getRelatedMatchs(), idUser, 4, idTheso, conceptObject.getIdConcept());
        appendAlignments(alignementsBuilder, conceptObject.getNarrowMatchs(), idUser, 5, idTheso, conceptObject.getIdConcept());
        String alignements = stripLeadingSeparator(alignementsBuilder);

        String prefTerm = null;
        if (CollectionUtils.isNotEmpty(conceptObject.getPrefLabels())) {
            StringBuilder prefTermBuilder = new StringBuilder();
            for (ThesaurusCsvConceptLabel label : conceptObject.getPrefLabels()) {
                prefTermBuilder.append(SEPERATEUR).append(label.getLabel()).append(SOUS_SEPERATEUR).append(label.getLang());
            }
            prefTerm = stripLeadingSeparator(prefTermBuilder);
        }

        //Non Pref Term
        //-- 'id_term@lexicalValue@lang@id_thesaurus@source@status@hiden'
        StringBuilder nonPrefTermBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(conceptObject.getAltLabels())) {
            for (ThesaurusCsvConceptLabel label : conceptObject.getAltLabels()) {
                nonPrefTermBuilder.append(SEPERATEUR).append(conceptObject.getIdConcept())
                        .append(SOUS_SEPERATEUR).append(label.getLabel())
                        .append(SOUS_SEPERATEUR).append(label.getLang())
                        .append(SOUS_SEPERATEUR).append(idTheso)
                        .append(SOUS_SEPERATEUR).append(idUser)
                        .append(SOUS_SEPERATEUR).append("USE")
                        .append(SOUS_SEPERATEUR).append(false);
            }
        }

        if (CollectionUtils.isNotEmpty(conceptObject.getAltLabels())) {
            for (ThesaurusCsvConceptLabel altLabel : conceptObject.getHiddenLabels()) {
                nonPrefTermBuilder.append(SEPERATEUR).append(conceptObject.getIdConcept())
                        .append(SOUS_SEPERATEUR).append(altLabel.getLabel())
                        .append(SOUS_SEPERATEUR).append(altLabel.getLang())
                        .append(SOUS_SEPERATEUR).append(idTheso)
                        .append(SOUS_SEPERATEUR).append(idUser)
                        .append(SOUS_SEPERATEUR).append("Hiddden")
                        .append(SOUS_SEPERATEUR).append(true);
            }
        }
        String nonPrefTerm = stripLeadingSeparator(nonPrefTermBuilder);

        //Relation
        //-- 'id_concept1@role@id_concept2'
        StringBuilder relationsBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(conceptObject.getBroaders())) {
            isTopConcept = false;
            for (String idConcept2 : conceptObject.getBroaders()) {
                appendRelationPair(relationsBuilder, conceptObject.getIdConcept(), "BT", idConcept2, "NT");
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getNarrowers())) {
            for (String idConcept2 : conceptObject.getNarrowers()) {
                appendRelationPair(relationsBuilder, conceptObject.getIdConcept(), "NT", idConcept2, "BT");
            }
        }
        if (CollectionUtils.isNotEmpty(conceptObject.getRelateds())) {
            for (String idConcept2 : conceptObject.getRelateds()) {
                appendRelationPair(relationsBuilder, conceptObject.getIdConcept(), "RT", idConcept2, "RT");
            }
        }
        String relations = stripLeadingSeparator(relationsBuilder);

        //CustomRelation
        //-- 'id_concept1@role@id_concept2'
        StringBuilder customRelationsBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(conceptObject.getCustomRelations())) {
            for (NodeIdValue nodeIdValue  : conceptObject.getCustomRelations()) {
                customRelationsBuilder.append(SEPERATEUR).append(conceptObject.getIdConcept())
                        .append(SOUS_SEPERATEUR).append(nodeIdValue.getValue())
                        .append(SOUS_SEPERATEUR).append(nodeIdValue.getId());
            }
        }
        String customRelations = stripLeadingSeparator(customRelationsBuilder);

        //Notes
        //-- 'value@typeCode@lang@id_term'
        String notes = getNotes(conceptObject);

        StringBuilder gpsBuilder = new StringBuilder();
        if (StringUtils.isNotEmpty(conceptObject.getLatitude())) {
            gpsBuilder.append(conceptObject.getLatitude()).append(SOUS_SEPERATEUR).append(conceptObject.getLongitude());
        }
        if (StringUtils.isNotEmpty(conceptObject.getGps())) {
            if (!gpsBuilder.isEmpty()) {
                gpsBuilder.append(gpsBuilder.toString()).append(SEPERATEUR);
            }
            var gpsList = ThesaurusCsvGpsParser.readGps(conceptObject.getGps(), "", "");
            if (CollectionUtils.isNotEmpty(gpsList)) {
                for (Gps gpsValue : gpsList) {
                    gpsBuilder.append(SEPERATEUR).append(gpsValue.getLatitude()).append(SOUS_SEPERATEUR).append(gpsValue.getLongitude());
                }
            }
        }
        String gps = gpsBuilder.isEmpty() ? null : gpsBuilder.toString();

        try {
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat(StringUtils.defaultIfBlank(formatDate, DEFAULT_DATE_FORMAT));
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
                    toSqlDate(conceptObject.getCreated() == null ? null : dateFormat.parse(conceptObject.getCreated())),
                    toSqlDate(conceptObject.getModified() == null ? null : dateFormat.parse(conceptObject.getModified())),
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

    private static java.sql.Date toSqlDate(Date date) {
        return date == null ? null : new java.sql.Date(date.getTime());
    }
}
