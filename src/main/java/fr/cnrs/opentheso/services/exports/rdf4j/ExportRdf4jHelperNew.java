package fr.cnrs.opentheso.services.exports.rdf4j;

import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentSmall;
import fr.cnrs.opentheso.models.terms.NodeEM;
import fr.cnrs.opentheso.models.facets.NodeFacet;
import fr.cnrs.opentheso.models.nodes.NodeGps;
import fr.cnrs.opentheso.models.relations.NodeHieraRelation;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.concept.NodeConceptExport;
import fr.cnrs.opentheso.models.group.NodeGroupLabel;
import fr.cnrs.opentheso.models.group.NodeGroupTraductions;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.models.thesaurus.NodeThesaurus;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.candidats.VoteDto;
import fr.cnrs.opentheso.models.skosapi.SKOSDiscussion;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSStatus;
import fr.cnrs.opentheso.models.skosapi.SKOSVote;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.FacetService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.NoteService;
import fr.cnrs.opentheso.services.RelationGroupService;
import fr.cnrs.opentheso.services.ThesaurusService;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ExportRdf4jHelperNew {

    private final FacetService facetService;
    private final NoteService noteService;
    private final GroupService groupService;
    private final ConceptService conceptService;
    private final RelationGroupService relationGroupService;
    private final ThesaurusService thesaurusService;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;
    private final CandidatStatusRepository candidatStatusRepository;

    private String messages;
    private Preferences nodePreference;
    private SKOSXmlDocument skosXmlDocument = new SKOSXmlDocument();
    private HashMap<String, String> superGroupHashMap= new HashMap();


    public boolean setInfos(Preferences nodePreference) {
        skosXmlDocument = new SKOSXmlDocument();
        this.nodePreference = nodePreference;
        messages = "";
        return true;
    }

    public SKOSResource exportConceptV2(String idTheso, String idConcept, boolean isCandidatExport) {

        SKOSResource sKOSResource = new SKOSResource();
        NodeConceptExport nodeConcept = conceptService.getConceptForExport(idConcept, idTheso, isCandidatExport);

        if (nodeConcept == null) {
            messages = messages + ("Erreur concept non exporté importé: " + idConcept + "\n");
            return null;
        }

        if(nodeConcept.getNodeListOfBT().isEmpty() ){
            sKOSResource.addRelation(idTheso, getUriFromId(idTheso), SKOSProperty.TOP_CONCEPT_OF);
        }

        sKOSResource.setUri(getUri(nodeConcept));
        sKOSResource.setLocalUri(getLocalUri(nodeConcept));
        sKOSResource.setProperty(SKOSProperty.CONCEPT);

        //// définir le status du Concept (CA=candidat, DEP= déprécié, autre= concept)
        setStatusOfConcept(nodeConcept.getConcept().getStatus(), sKOSResource);

        // ajout des concepts de remplacements ReplacedBy et Replaces
        if(nodeConcept.getReplacedBy() != null && !nodeConcept.getReplacedBy().isEmpty()) {
            addReplaces(nodeConcept.getReplacedBy(), sKOSResource, idTheso);
        }
        if(nodeConcept.getReplaces() != null && !nodeConcept.getReplaces().isEmpty()) {
            addReplaces(nodeConcept.getReplaces(), sKOSResource, idTheso);
        }

        // pour l'export des données du module candidat
        if (isCandidatExport) {
            var candidatStatus = candidatStatusRepository.findByIdConcept(idConcept);
            candidatStatus.ifPresent(status -> sKOSResource.setSkosStatus(addStatut(status)));
            addDiscussions(nodeConcept.getMessages(), sKOSResource);
            addVotes(nodeConcept.getVotes(), sKOSResource);
        }

        // prefLabel
        for (NodeTermTraduction traduction : nodeConcept.getNodeTermTraductions()) {
            sKOSResource.addLabel(traduction.getLexicalValue(), traduction.getLang(), SKOSProperty.PREF_LABEL);
        }
        // altLabel
        for (NodeEM nodeEM : nodeConcept.getNodeEM()) {
            if (nodeEM.isHiden()) {
                sKOSResource.addLabel(nodeEM.getLexicalValue(), nodeEM.getLang(), SKOSProperty.HIDDEN_LABEL);
            } else {
                sKOSResource.addLabel(nodeEM.getLexicalValue(), nodeEM.getLang(), SKOSProperty.ALT_LABEL);
            }
        }

        List<NodeNote> nodeNotes = nodeConcept.getNodeNotes();
        addNoteGiven(nodeNotes, sKOSResource);
        addGPSGiven(nodeConcept.getNodeGps(), sKOSResource);
        addAlignementGiven(nodeConcept.getNodeAlignmentsList(), sKOSResource);

        // relations
        addRelationGiven(nodeConcept.getNodeListOfBT(), nodeConcept.getNodeListOfNT(),
                nodeConcept.getNodeListIdsOfRT(), sKOSResource, nodeConcept.getConcept().getIdThesaurus());


        if (nodeConcept.getConcept().getNotation() != null && !nodeConcept.getConcept().getNotation().equals("null")) {
            sKOSResource.addNotation(nodeConcept.getConcept().getNotation());
        }

        sKOSResource.addRelation(idTheso, getUriFromId(idTheso), SKOSProperty.INSCHEME);

        for (NodeUri nodeUri : nodeConcept.getNodeListIdsOfConceptGroup()) {
            sKOSResource.addRelation(nodeUri.getIdConcept(), getUriGroupFromNodeUri(nodeUri, idTheso), SKOSProperty.MEMBER_OF);
        }

        sKOSResource.addIdentifier(idConcept, SKOSProperty.IDENTIFIER);
        if(nodeConcept.getConcept().getIdArk() != null && !nodeConcept.getConcept().getIdArk().isEmpty() ){
            sKOSResource.setArkId(nodeConcept.getConcept().getIdArk());
        }


        if(nodeConcept.getListFacetsOfConcept() != null) {
            for (String idFacette : nodeConcept.getListFacetsOfConcept()) {
                int prop = SKOSProperty.SUB_ORDINATE_ARRAY;
                sKOSResource.addRelation(idFacette, getUriForFacette(idFacette, idTheso), prop);
            }
        }

        // les images
        if(nodeConcept.getNodeImages() != null) {
            for (NodeImage nodeImage : nodeConcept.getNodeImages()) {
                sKOSResource.addNodeImage(nodeImage);
            }
        }

        // createur et contributeur
        if (nodeConcept.getConcept().getCreatorName()!= null && !nodeConcept.getConcept().getCreatorName().isEmpty()) {
            sKOSResource.addAgent(nodeConcept.getConcept().getCreatorName(), SKOSProperty.CREATOR);
        }
        if (nodeConcept.getConcept().getContributorName()!= null && !nodeConcept.getConcept().getContributorName().isEmpty()) {
            sKOSResource.addAgent(nodeConcept.getConcept().getContributorName(), SKOSProperty.CONTRIBUTOR);
        }

        // dates
        if (nodeConcept.getConcept().getCreated() != null) {
            sKOSResource.addDate(nodeConcept.getConcept().getCreated().toString(), SKOSProperty.CREATED);
        }
        if (nodeConcept.getConcept().getModified() != null) {
            sKOSResource.addDate(nodeConcept.getConcept().getModified().toString(), SKOSProperty.MODIFIED);
        }

        return sKOSResource;
    }

    public SKOSResource addSingleConceptByLangV2(String idTheso, String idConcept, String idLang, boolean showLabels) {

        SKOSResource sKOSResource = new SKOSResource();
        NodeConceptExport nodeConcept = conceptService.getConceptForExport(idConcept, idTheso, false);

        if (nodeConcept == null) {
            return null;
        }

        sKOSResource.setUri(getUri(nodeConcept));
        sKOSResource.setProperty(SKOSProperty.CONCEPT);

        // prefLabel
        for (NodeTermTraduction traduction : nodeConcept.getNodeTermTraductions()) {
            if(traduction.getLang().equalsIgnoreCase(idLang))
                sKOSResource.addLabel(traduction.getLexicalValue(), traduction.getLang(), SKOSProperty.PREF_LABEL);
        }

        // altLabel
        for (NodeEM nodeEM : nodeConcept.getNodeEM()) {
            if(nodeEM.getLang().equalsIgnoreCase(idLang)) {
                if(nodeEM.isHiden())
                    sKOSResource.addLabel(nodeEM.getLexicalValue(), nodeEM.getLang(), SKOSProperty.HIDDEN_LABEL);
                else
                    sKOSResource.addLabel(nodeEM.getLexicalValue(), nodeEM.getLang(), SKOSProperty.ALT_LABEL);
            }
        }
        ArrayList<NodeNote> nodeNotes = new ArrayList<>();
        for (NodeNote nodeNote : nodeConcept.getNodeNotes()) {
            if(nodeNote.getLang().equalsIgnoreCase(idLang))
                nodeNotes.add(nodeNote);
        }

        addNoteGiven(nodeNotes, sKOSResource);
        addGPSGiven(nodeConcept.getNodeGps(), sKOSResource);
        addAlignementGiven(nodeConcept.getNodeAlignmentsList(), sKOSResource);

        if(!showLabels) {
            addRelationGiven(nodeConcept.getNodeListOfBT(), nodeConcept.getNodeListOfNT(),
                    nodeConcept.getNodeListIdsOfRT(), sKOSResource, nodeConcept.getConcept().getIdThesaurus());
        }
        String notation = nodeConcept.getConcept().getNotation();
        String created = nodeConcept.getConcept().getCreated().toString();
        String modified = nodeConcept.getConcept().getModified().toString();

        if (notation != null && !notation.equals("null")) {
            sKOSResource.addNotation(notation);
        }
        if (created != null) {
            sKOSResource.addDate(created, SKOSProperty.CREATED);
        }
        if (modified != null) {
            sKOSResource.addDate(modified, SKOSProperty.MODIFIED);
        }
        sKOSResource.addRelation(idTheso, getUriFromId(idTheso), SKOSProperty.INSCHEME);
        for (NodeUri nodeUri : nodeConcept.getNodeListIdsOfConceptGroup()) {
            sKOSResource.addRelation(nodeUri.getIdConcept(), getUriGroupFromNodeUri(nodeUri,idTheso), SKOSProperty.MEMBER_OF);
        }
        sKOSResource.addIdentifier(idConcept, SKOSProperty.IDENTIFIER);

        return sKOSResource;
    }

    /**
     * permet de récupérer les informations du thésaurus et les TopConcept pour
     * construire SKOSResource #MR
     */
    public SKOSResource exportThesoV2(String idTheso, Preferences nodePreference) {
        this.nodePreference = nodePreference;
        NodeThesaurus nodeThesaurus = thesaurusService.getNodeThesaurus(idTheso);
        SKOSResource conceptScheme = new SKOSResource(getUriFromId(nodeThesaurus.getIdThesaurus()), SKOSProperty.CONCEPT_SCHEME);
        for (Thesaurus thesaurus : nodeThesaurus.getListThesaurusTraduction()) {

            if (thesaurus.getCreator() != null && !thesaurus.getCreator().equalsIgnoreCase("null")) {
                conceptScheme.addAgent(thesaurus.getCreator(), SKOSProperty.CREATOR);
            }
            if (thesaurus.getContributor() != null && !thesaurus.getContributor().equalsIgnoreCase("null")) {
                conceptScheme.addAgent(thesaurus.getContributor(), SKOSProperty.CONTRIBUTOR);
            }
            if (thesaurus.getTitle() != null && thesaurus.getLanguage() != null) {
                conceptScheme.addLabel(thesaurus.getTitle(),
                        thesaurus.getLanguage(), SKOSProperty.PREF_LABEL);
            }

            //dates
            if (thesaurus.getCreated() != null) {
                conceptScheme.addDate(thesaurus.getCreated().toString(), SKOSProperty.CREATED);
            }
            if (thesaurus.getModified() != null) {
                conceptScheme.addDate(thesaurus.getModified().toString(), SKOSProperty.MODIFIED);
            }
            conceptScheme.setThesaurus(thesaurus);
        }

        /// ajout des DCMI
        var tmp = thesaurusDcTermRepository.findAllByIdThesaurus(idTheso);
        if (CollectionUtils.isNotEmpty(tmp)) {
            conceptScheme.getThesaurus().setDcElement(tmp.stream().map(element -> DcElement.builder()
                    .id(element.getId().intValue())
                    .name(element.getName())
                    .value(element.getValue())
                    .language(element.getLanguage())
                    .type(element.getDataType())
                    .build()).toList());
        }

        //liste top concept
        var nodeTTs = conceptService.getAllTopConcepts(idTheso);

        nodeTTs.forEach((nodeTT) -> {
            conceptScheme.addRelation(nodeTT.getIdConcept(), getUriFromNodeUri(nodeTT, idTheso), SKOSProperty.HAS_TOP_CONCEPT);
        });
        return conceptScheme;
    }

    public List<SKOSResource> exportFacettesV2(String idTheso){

        var facets = facetService.getAllFacetsDetailsOfThesaurus(idTheso);
        List<SKOSResource> facetList = new ArrayList<>();
        for (NodeFacet facet : facets) {
            SKOSResource sKOSResource =  new SKOSResource(getUriForFacette(facet.getIdFacet(), idTheso), SKOSProperty.FACET);
            sKOSResource.addRelation(facet.getIdFacet(), getUriFromNodeUri(facet.getNodeUri(), idTheso), SKOSProperty.SUPER_ORDINATE);
            sKOSResource.addLabel(facet.getLexicalValue(), facet.getLang(), SKOSProperty.PREF_LABEL);
            sKOSResource.addDate(facet.getCreated(), SKOSProperty.CREATED);
            sKOSResource.addDate(facet.getModified(), SKOSProperty.MODIFIED);
            addFacetMembers(sKOSResource, facet, idTheso);
            facetList.add(sKOSResource);
        }

        return facetList;
    }

    public List<SKOSResource> exportCollectionsV2(String idTheso){
        var rootGroupList = groupService.getListIdOfRootGroup(idTheso, false);
        List<SKOSResource> skosResourcesList = new ArrayList<>();
        for (String idGroup : rootGroupList) {
            var nodeGroupLabel = groupService.getNodeGroupLabel(idGroup, idTheso);
            SKOSResource sKOSResource = new SKOSResource(getUriFromGroup(nodeGroupLabel), SKOSProperty.CONCEPT_GROUP);
            sKOSResource.setIdentifier(idGroup);
            sKOSResource.addRelation(nodeGroupLabel.getIdGroup(), getUriFromGroup(nodeGroupLabel), SKOSProperty.MICROTHESAURUS_OF);
            skosResourcesList.add(addChildsGroupRecursiveV2(idTheso, idGroup, sKOSResource));
        }
        return skosResourcesList;
    }

    public SKOSResource addChildsGroupRecursiveV2(String idTheso, String idParent, SKOSResource sKOSResource) {

        var listIdsOfGroupChilds = relationGroupService.getListGroupChildIdOfGroup(idParent, idTheso);
        var skosResource = writeGroupInfoV2(sKOSResource, idTheso, idParent);

        for (String idOfGroupChild : listIdsOfGroupChilds) {
            sKOSResource = new SKOSResource();
            sKOSResource.setIdentifier(idOfGroupChild);
            addChildsGroupRecursiveV2(idTheso, idOfGroupChild, sKOSResource);
        }

        return skosResource;
    }

    private SKOSResource writeGroupInfoV2(SKOSResource sKOSResource, String idTheso, String idOfGroupChild) {

        NodeGroupLabel nodeGroupLabel = groupService.getNodeGroupLabel(idOfGroupChild, idTheso);

        sKOSResource.setUri(getUriFromGroup(nodeGroupLabel));
        sKOSResource.setProperty(SKOSProperty.CONCEPT_GROUP);

        //dates
        String created = null;
        String modified = null;
        if(nodeGroupLabel.getCreated()!=null)
            created = nodeGroupLabel.getCreated().toString();
        if(nodeGroupLabel.getModified()!=null)
            modified = nodeGroupLabel.getModified().toString();
        if (created != null) {
            sKOSResource.addDate(created, SKOSProperty.CREATED);
        }
        if (modified != null) {
            sKOSResource.addDate(modified, SKOSProperty.MODIFIED);
        }

        for (NodeGroupTraductions traduction : nodeGroupLabel.getNodeGroupTraductionses()) {
            sKOSResource.addLabel(traduction.getTitle(), traduction.getIdLang(), SKOSProperty.PREF_LABEL);
        }

        var childURIs = relationGroupService.getListGroupChildOfGroup(idTheso, idOfGroupChild);
        var nodeUris = conceptService.getListConceptsOfGroup(idTheso, idOfGroupChild);

        for (var nodeUri : nodeUris) {
            sKOSResource.addRelation(nodeUri.getIdConcept(), getUriFromNodeUri(nodeUri, idTheso), SKOSProperty.MEMBER);
        }

        for (NodeUri nodeUri : childURIs) {
            sKOSResource.addRelation(nodeUri.getIdConcept(), getUriGroupFromNodeUri(nodeUri, idTheso), SKOSProperty.SUBGROUP);
            superGroupHashMap.put(nodeUri.getIdConcept(), idOfGroupChild);
        }

        String idSuperGroup = superGroupHashMap.get(idOfGroupChild);
        if (idSuperGroup != null) {
            NodeUri nodeUri1 = groupService.getThisGroupIds(idSuperGroup, idTheso);
            if(nodeUri1 != null){
                sKOSResource.addRelation(idSuperGroup, getUriGroupFromNodeUri(nodeUri1, idTheso), SKOSProperty.SUPERGROUP);
                superGroupHashMap.remove(idOfGroupChild);
            }
        }

        // ajout de la notation
        if (nodeGroupLabel.getNotation() != null && !nodeGroupLabel.getNotation().equals("null")) {
            if(!nodeGroupLabel.getNotation().isEmpty())
                sKOSResource.addNotation(nodeGroupLabel.getNotation());
        }

        /// Ajout des notes
        var nodeNotes = noteService.getListNotesAllLang(idOfGroupChild, idTheso);
        addNoteGiven(nodeNotes, sKOSResource);

        return sKOSResource;
    }

    public SKOSResource addSingleGroupV2(String idThesaurus, String idGroup) {

        NodeGroupLabel nodeGroupLabel = groupService.getNodeGroupLabel(idGroup, idThesaurus);
        if (nodeGroupLabel == null) {
            return null;
        }
        SKOSResource sKOSResource = new SKOSResource();
        sKOSResource.setUri(getUriFromGroup(nodeGroupLabel));
        sKOSResource.setProperty(SKOSProperty.CONCEPT_GROUP);

        for (NodeGroupTraductions traduction : nodeGroupLabel.getNodeGroupTraductionses()) {
            sKOSResource.addLabel(traduction.getTitle(), traduction.getIdLang(), SKOSProperty.PREF_LABEL);
            //dates
            String created;
            String modified;
            created = traduction.getCreated().toString();
            modified = traduction.getModified().toString();
            if (created != null) {
                sKOSResource.addDate(created, SKOSProperty.CREATED);
            }
            if (modified != null) {
                sKOSResource.addDate(modified, SKOSProperty.MODIFIED);
            }
        }

        // pour exporter les membres (tous les concepts du group

        var childURI = relationGroupService.getListGroupChildIdOfGroup(idGroup, idThesaurus);
        HashMap<String, String> superGroupHashMapTemp = new HashMap();
        for (String id : childURI) {
            sKOSResource.addRelation(id, getUriFromId(id), SKOSProperty.SUBGROUP);
            superGroupHashMapTemp.put(id, idGroup);
        }
        String idSuperGroup = superGroupHashMapTemp.get(idGroup);

        if (idSuperGroup != null) {
            sKOSResource.addRelation(idSuperGroup, getUriFromId(idSuperGroup), SKOSProperty.SUPERGROUP);
            superGroupHashMapTemp.remove(idGroup);
        }
        sKOSResource.addIdentifier(idGroup, SKOSProperty.IDENTIFIER);
        return sKOSResource;
    }

    public List<SKOSResource> exportSelectedCollectionsV2(String idTheso, List<String> selectedGroups){
        NodeGroupLabel nodeGroupLabel;
        List<SKOSResource> skosResourcesList = new ArrayList<>();
        for (String idGroup : selectedGroups) {
            nodeGroupLabel = groupService.getNodeGroupLabel(idGroup, idTheso);
            SKOSResource sKOSResource = new SKOSResource(getUriFromGroup(nodeGroupLabel), SKOSProperty.CONCEPT_GROUP);
            sKOSResource.addRelation(nodeGroupLabel.getIdGroup(), getUriFromGroup(nodeGroupLabel), SKOSProperty.MICROTHESAURUS_OF);
            skosResourcesList.add(addChildsGroupRecursiveV2(idTheso, idGroup, sKOSResource));
        }
        return skosResourcesList;
    }
    
    private void addFacetMembers(SKOSResource sKOSResource, NodeFacet nodeFacet, String idTheso){

        List<String> members = facetService.getAllMembersOfFacet(nodeFacet.getIdFacet(), idTheso);
        for (String idConcept : members) {
            NodeUri nodeUri = conceptService.getNodeUriOfConcept(idConcept, idTheso);
            sKOSResource.addRelation(nodeUri.getIdConcept(), getUriFromNodeUri(nodeUri, idTheso), SKOSProperty.MEMBER);
        }
    }

    public SKOSResource exportThisCollectionV2(String idTheso, String idGroup){

        NodeGroupLabel nodeGroupLabel = groupService.getNodeGroupLabel(idGroup, idTheso);
        SKOSResource sKOSResource = new SKOSResource(getUriFromGroup(nodeGroupLabel), SKOSProperty.CONCEPT_GROUP);
        sKOSResource.addRelation(nodeGroupLabel.getIdGroup(), getUriFromGroup(nodeGroupLabel), SKOSProperty.MICROTHESAURUS_OF);
        return writeGroupInfoV2(sKOSResource, idTheso, idGroup);
    }

    public void addChildsGroupRecursive(String idTheso, String idParent, SKOSResource sKOSResource) {

        var listIdsOfGroupChilds = relationGroupService.getListGroupChildIdOfGroup(idParent, idTheso);
        writeGroupInfo(sKOSResource, idTheso, idParent);

        for (String idOfGroupChild : listIdsOfGroupChilds) {
            sKOSResource = new SKOSResource();
            sKOSResource.setIdentifier(idOfGroupChild);
            addChildsGroupRecursive(idTheso, idOfGroupChild, sKOSResource);
        }
    }

    private void writeGroupInfo(SKOSResource sKOSResource, String idTheso, String idOfGroupChild) {

        NodeGroupLabel nodeGroupLabel = groupService.getNodeGroupLabel(idOfGroupChild, idTheso);

        sKOSResource.setUri(getUriFromGroup(nodeGroupLabel));
        sKOSResource.setProperty(SKOSProperty.CONCEPT_GROUP);

        //dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if(nodeGroupLabel.getCreated()!=null) {
            sKOSResource.addDate(dateFormat.format(nodeGroupLabel.getCreated()), SKOSProperty.CREATED);
        }

        if(nodeGroupLabel.getModified()!=null) {
            sKOSResource.addDate(dateFormat.format(nodeGroupLabel.getModified()), SKOSProperty.MODIFIED);
        }
        
        for (NodeGroupTraductions traduction : nodeGroupLabel.getNodeGroupTraductionses()) {
            sKOSResource.addLabel(traduction.getTitle(), traduction.getIdLang(), SKOSProperty.PREF_LABEL);
        }

        var childURIs = relationGroupService.getListGroupChildOfGroup(idTheso, idOfGroupChild);
        var nodeUris = conceptService.getListConceptsOfGroup(idTheso, idOfGroupChild);

        for (NodeUri nodeUri : nodeUris) {
            sKOSResource.addRelation(nodeUri.getIdConcept(), getUriFromNodeUri(nodeUri, idTheso), SKOSProperty.MEMBER);
        }

        for (NodeUri nodeUri : childURIs) {
            sKOSResource.addRelation(nodeUri.getIdConcept(), getUriGroupFromNodeUri(nodeUri, idTheso), SKOSProperty.SUBGROUP);
            superGroupHashMap.put(nodeUri.getIdConcept(), idOfGroupChild);
        }

        String idSuperGroup = superGroupHashMap.get(idOfGroupChild);
        if (idSuperGroup != null) {
            NodeUri nodeUri1 = groupService.getThisGroupIds(idSuperGroup, idTheso);
            if(nodeUri1 != null){
                sKOSResource.addRelation(idSuperGroup, getUriGroupFromNodeUri(nodeUri1, idTheso), SKOSProperty.SUPERGROUP);
                superGroupHashMap.remove(idOfGroupChild);
            }
        }

        // ajout de la notation
        if (nodeGroupLabel.getNotation() != null && !nodeGroupLabel.getNotation().equals("null")) {
            if(!nodeGroupLabel.getNotation().isEmpty())
                sKOSResource.addNotation(nodeGroupLabel.getNotation());
        }
        
        /// Ajout des notes
        var nodeNotes = noteService.getListNotesAllLang(idOfGroupChild, idTheso);
        addNoteGiven(nodeNotes, sKOSResource);
        
        skosXmlDocument.addGroup(sKOSResource);
    }

    private void setStatusOfConcept(String status, SKOSResource sKOSResource){
        switch (status.toLowerCase()) {
            case "ca":
                sKOSResource.setStatus(SKOSProperty.CANDIDATE);
                break;
            case "dep":
                sKOSResource.setStatus(SKOSProperty.DEPRECATED);
                break;
            default:
                sKOSResource.setStatus(SKOSProperty.CONCEPT);
                break;              
        }
        
    }
    private SKOSStatus addStatut(CandidatStatus nodeStatus) {
        SKOSStatus skosStatus = new SKOSStatus();
        skosStatus.setDate(new SimpleDateFormat("yyyy-MM-dd").format(nodeStatus.getDate()));
        skosStatus.setIdConcept(nodeStatus.getIdConcept());
        skosStatus.setIdStatus(String.valueOf(nodeStatus.getStatus().getIdStatus()));
        skosStatus.setMessage(nodeStatus.getMessage());
        skosStatus.setIdThesaurus(nodeStatus.getIdThesaurus());
        skosStatus.setIdUser(String.valueOf(nodeStatus.getIdUser()));
        return skosStatus;
    }

    private ArrayList<String> getPathFromArray(ArrayList<ArrayList<String>> paths) {
        String pathFromArray = "";
        ArrayList<String> allPath = new ArrayList<>();
        for (ArrayList<String> path : paths) {
            for (String string1 : path) {
                if(pathFromArray.isEmpty())
                    pathFromArray = string1;
                else
                    pathFromArray = pathFromArray + "##" + string1;
            }
            allPath.add(pathFromArray);
            pathFromArray = "";
        }
        return allPath;
    }
    
    private void addNoteGiven(List<NodeNote> nodeNotes, SKOSResource resource) {
        for (NodeNote note : nodeNotes) {
            int prop = switch (note.getNoteTypeCode()) {
                case "note" -> SKOSProperty.NOTE;
                case "scopeNote" -> SKOSProperty.SCOPE_NOTE;
                case "historyNote" -> SKOSProperty.HISTORY_NOTE;
                case "example" -> SKOSProperty.EXAMPLE;
                case "definition" -> SKOSProperty.DEFINITION;
                case "editorialNote" -> SKOSProperty.EDITORIAL_NOTE;
                case "changeNote" -> SKOSProperty.CHANGE_NOTE;
                default -> SKOSProperty.NOTE;
            };
            resource.addDocumentation(note.getLexicalValue(), note.getLang(), prop);
        }
    }


    private void addDiscussions(List<MessageDto> messages, SKOSResource resource) {
        for (MessageDto message : messages) {
            SKOSDiscussion skosDiscussion = new SKOSDiscussion();
            skosDiscussion.setMsg(message.getMsg());
            skosDiscussion.setIdUser(message.getIdUser());
            skosDiscussion.setDate(message.getDate());
            resource.addMessage(skosDiscussion);
        }
    }


    private void addVotes(List<VoteDto> votes, SKOSResource resource) {
        for (VoteDto vote : votes) {
            SKOSVote skosVote = new SKOSVote();
            skosVote.setIdNote(vote.getIdNote());
            skosVote.setIdUser(vote.getIdUser());
            skosVote.setIdThesaurus(vote.getIdThesaurus());
            skosVote.setIdConcept(vote.getIdConcept());
            skosVote.setTypeVote(vote.getTypeVote());
            if (!StringUtils.isEmpty(vote.getIdNote()) && !"null".equalsIgnoreCase(vote.getIdNote())) {
                String htmlTagsRegEx = "<[^>]*>";
                NodeNote nodeNote = noteService.getNoteByIdNote(Integer.parseInt(vote.getIdNote()));
                if (nodeNote != null) {
                    String str = conceptService.formatLinkTag(nodeNote.getLexicalValue());
                    skosVote.setValueNote(str.replaceAll(htmlTagsRegEx, ""));
                }
            }
            resource.addVote(skosVote);
        }
    }

    private void addGPSGiven(List<NodeGps> gps, SKOSResource resource) {
        if (gps == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(gps)) {
            List<SKOSGPSCoordinates> elements = new ArrayList<>();
            for (NodeGps element : gps) {
                elements.add(new SKOSGPSCoordinates(element.getLatitude(), element.getLongitude()));
            }
            resource.setGpsCoordinates(elements);
        }
    }


    private void addAlignementGiven(List<NodeAlignmentSmall> nodeAlignments, SKOSResource resource) {
        for (NodeAlignmentSmall alignment : nodeAlignments) {

            int prop = -1;
            prop = switch (alignment.getAlignement_id_type()) {
                case 1 -> SKOSProperty.EXACT_MATCH;
                case 2 -> SKOSProperty.CLOSE_MATCH;
                case 3 -> SKOSProperty.BROAD_MATCH;
                case 4 -> SKOSProperty.RELATED_MATCH;
                case 5 -> SKOSProperty.NARROWER_MATCH;
                default -> prop;
            };
            resource.addMatch(alignment.getUri_target(), prop);
        }
    }
    private void addRelationGiven(List<NodeHieraRelation> btList, List<NodeHieraRelation> ntList,
                                  List<NodeHieraRelation> rtList, SKOSResource resource, String idTheso) {
        for (NodeHieraRelation rt : rtList) {
            int prop = switch (rt.getRole()) {
                case "RHP" -> SKOSProperty.RELATED_HAS_PART;
                case "RPO" -> SKOSProperty.RELATED_PART_OF;
                default -> SKOSProperty.RELATED;
            };

            resource.addRelation(rt.getUri().getIdConcept(), getUriFromNodeUri(rt.getUri(), idTheso), prop);
        }
        for (NodeHieraRelation nt : ntList) {
            int prop = switch (nt.getRole()) {
                case "NTG" -> SKOSProperty.NARROWER_GENERIC;
                case "NTP" -> SKOSProperty.NARROWER_PARTITIVE;
                case "NTI" -> SKOSProperty.NARROWER_INSTANTIAL;
                default -> SKOSProperty.NARROWER;
            };
            resource.addRelation(nt.getUri().getIdConcept(), getUriFromNodeUri(nt.getUri(), idTheso), prop);
        }
        for (NodeHieraRelation bt : btList) {
            int prop = switch (bt.getRole()) {
                case "BTG" -> SKOSProperty.BROADER_GENERIC;
                case "BTP" -> SKOSProperty.BROADER_PARTITIVE;
                case "BTI" -> SKOSProperty.BROADER_INSTANTIAL;
                default -> SKOSProperty.BROADER;
            };
            resource.addRelation(bt.getUri().getIdConcept(), getUriFromNodeUri(bt.getUri(), idTheso), prop);
        }
    }    
    
    // ajoute les données de remplacement des concepts dépréciés dans les 2 sens (Replaced et Replace)
    private void addReplaces(List<NodeHieraRelation> replaces, SKOSResource resource, String idTheso) {
        for (NodeHieraRelation uriReplace : replaces) {
            int prop = switch (uriReplace.getRole()) {
                case "replacedBy" -> SKOSProperty.IS_REPLACED_BY;
                case "replace" -> SKOSProperty.REPLACES;
                default -> -1;
            };
            resource.addReplaces(getUriFromNodeUri(uriReplace.getUri(), idTheso), prop);
        }
    }    

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /////////////////////Fin des nouvelles fonctions ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////// 
    public String getUriFromId(String id) {
        String uriBase = getPath();
        if (uriBase.endsWith("/")) {
            uriBase = uriBase.substring(0, uriBase.length() - 1);
        }
        if(nodePreference.isOriginalUriIsArk()){
            if(!StringUtils.isEmpty((nodePreference.getOriginalUri()))){
                return nodePreference.getOriginalUri()+ "/" + nodePreference.getIdNaan() + "/" + id;
            } else {
                return uriBase + "/?idt=" + id;
            }
        } else {
            return uriBase + "/?idt=" + id;
        }
    }

    /**
     * Cette fonction permet de retourner l'URI du concept avec identifiant Ark
     * : si renseigné sinon l'URL du Site
     */
    private String getUri(NodeConceptExport nodeConceptExport) {
        String uri = "";
        if (nodeConceptExport == null) {
            return uri;
        }
        if (nodeConceptExport.getConcept() == null) {
            return uri;
        }
        String path = getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        // Choix de l'URI pour l'export : 
        // Si Handle est actif, on le prend en premier 
        // sinon,  on vérifie si Ark est actif, 
        // en dernier, on prend l'URL basique d'Opentheso
        // 1 seule URI est possible pour l'export par concept
        // URI de type Ark
        if(nodePreference.isOriginalUriIsArk()) {
            if (nodeConceptExport.getConcept().getIdArk() != null && !nodeConceptExport.getConcept().getIdArk().isEmpty()) {
                uri = nodePreference.getOriginalUri()+ "/" +nodeConceptExport.getConcept().getIdArk();
                return uri;
            } else {
                uri = path + "/?idc=" + nodeConceptExport.getConcept().getIdConcept().trim()
                            + "&idt=" + nodeConceptExport.getConcept().getIdThesaurus();                
                return uri;
            }
        }
        
        if(nodePreference.isOriginalUriIsHandle()) {
            // URI de type Handle
            if (nodeConceptExport.getConcept().getIdHandle() != null && !nodeConceptExport.getConcept().getIdHandle().isEmpty()) {
                if (!nodeConceptExport.getConcept().getIdHandle().trim().isEmpty()) {
                    uri = "https://hdl.handle.net/" + nodeConceptExport.getConcept().getIdHandle();
                    return uri;
                }
            }
        }
        if(nodePreference.isOriginalUriIsDoi()) {
            // URI de type Doi
            if (nodeConceptExport.getConcept().getIdDoi() != null) {
                if (!nodeConceptExport.getConcept().getIdDoi().trim().isEmpty()) {
                    uri = "https://doi.org/" + nodeConceptExport.getConcept().getIdDoi();
                    return uri;
                }
            }
        }        
        // si on ne trouve pas ni Handle, ni Ark
        //    uri = nodePreference.getCheminSite() + nodeConceptExport.getConcept().getIdConcept();
        if(nodePreference.getOriginalUri() != null && !nodePreference.getOriginalUri().isEmpty()) {
            uri = nodePreference.getOriginalUri()+ "/?idc=" + nodeConceptExport.getConcept().getIdConcept().trim()
                        + "&idt=" + nodeConceptExport.getConcept().getIdThesaurus();
        } else {
            uri = path + "/?idc=" + nodeConceptExport.getConcept().getIdConcept().trim()
                        + "&idt=" + nodeConceptExport.getConcept().getIdThesaurus();
        }

        return uri;
    }
    
    private String getLocalUri(NodeConceptExport nodeConceptExport){

        String uri = "";
        String path = getPath();
        if(path == null) return uri;
        if (nodeConceptExport == null) {
            return uri;
        }
        if (nodeConceptExport.getConcept() == null) {
            return uri;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        uri = path+ "/?idc=" + nodeConceptExport.getConcept().getIdConcept().trim()
                    + "&idt=" + nodeConceptExport.getConcept().getIdThesaurus();
        return uri;
    }

    /**
     * Cette fonction permet de retourner l'URI du concept avec identifiant Ark : si renseigné sinon l'URL du Site
    */
    public String getUriFromGroup(NodeGroupLabel nodeGroupLabel) {
        String uri = "";
        if (nodeGroupLabel == null) {
            return uri;
        }
        if (nodeGroupLabel.getIdGroup() == null) {
            return uri;
        }
        String uriBase = getPath();
        if (uriBase.endsWith("/")) {
            uriBase = uriBase.substring(0, uriBase.length() - 1);
        }

        // Choix de l'URI pour l'export : 
        // Si Handle est actif, on le prend en premier 
        // sinon,  on vérifie si Ark est actif, 
        // en dernier, on prend l'URL basique d'Opentheso
        // 1 seule URI est possible pour l'export par concept
        // URI de type Ark
        if(nodePreference.isOriginalUriIsArk()) {
            if (nodeGroupLabel.getIdArk() != null && !nodeGroupLabel.getIdArk().trim().isEmpty()) {
                uri = nodePreference.getUriArk() + nodeGroupLabel.getIdArk();
                return uri;
            } else {
                uri = uriBase + "/?idg=" + nodeGroupLabel.getIdGroup()
                            + "&idt=" + nodeGroupLabel.getIdThesaurus();
                return uri;
            }
            
        }
        if(nodePreference.isOriginalUriIsHandle()) {        
            // URI de type Handle
            if (nodeGroupLabel.getIdHandle() != null) {
                if (!nodeGroupLabel.getIdHandle().trim().isEmpty()) {
                    uri = "https://hdl.handle.net/" + nodeGroupLabel.getIdHandle();
                    return uri;
                }
            }
        }
        if(nodePreference.isOriginalUriIsDoi()) {        
            // URI de type Doi
            if (nodeGroupLabel.getIdDoi() != null) {
                if (!nodeGroupLabel.getIdDoi().trim().isEmpty()) {
                    uri = "https://doi.org/" + nodeGroupLabel.getIdDoi();
                    return uri;
                }
            }
        }        
        // si on ne trouve pas ni Handle, ni Ark
//        uri = nodePreference.getCheminSite() + nodeGroupLabel.getIdGroup();
        if(nodePreference.getOriginalUri() != null && !nodePreference.getOriginalUri().isEmpty()) {
            uri = nodePreference.getOriginalUri() + "/?idg=" + nodeGroupLabel.getIdGroup()
                        + "&idt=" + nodeGroupLabel.getIdThesaurus();
        } else {
            uri = uriBase + "/?idg=" + nodeGroupLabel.getIdGroup()
                        + "&idt=" + nodeGroupLabel.getIdThesaurus();
        }

    //    uri = nodePreference.getCheminSite() + nodeGroupLabel.getIdGroup();
        return uri;
    }

    /**
     * Cette fonction permet de retourner l'URI du concept avec identifiant Ark
     * : si renseigné sinon l'URL du Site
     */
    private String getUriGroupFromNodeUri(NodeUri nodeUri, String idTheso) {
        String uri = "";
        if (nodeUri == null) {
            return uri;
        }
        String uriBase = getPath();
        if (uriBase.endsWith("/")) {
            uriBase = uriBase.substring(0, uriBase.length() - 1);
        }

        // Choix de l'URI pour l'export : 
        // Si Handle est actif, on le prend en premier 
        // sinon,  on vérifie si Ark est actif, 
        // en dernier, on prend l'URL basique d'Opentheso
        // 1 seule URI est possible pour l'export par concept
        // URI de type Ark
        if(nodePreference.isOriginalUriIsArk()) { 
            if (nodeUri.getIdArk() != null && !nodeUri.getIdArk().isEmpty()) {
                uri = nodePreference.getOriginalUri()+ "/" + nodeUri.getIdArk();
                return uri;
            } else {
                uri = uriBase + "/?idg=" + nodeUri.getIdConcept()
                                + "&idt=" + idTheso;              
                return uri;
            }
        }
        // URI de type Handle
        if (nodeUri.getIdHandle() != null) {
            if (!nodeUri.getIdHandle().trim().isEmpty()) {
                uri = "https://hdl.handle.net/" + nodeUri.getIdHandle();
                return uri;
            }
        }

        // si on ne trouve pas ni Handle, ni Ark
        //    uri = nodePreference.getCheminSite() + nodeUri.getIdConcept();
//        uri = nodePreference.getCheminSite() + "?idg=" + nodeUri.getIdConcept()
//                        + "&idt=" + idTheso;
        if(nodePreference.getOriginalUri() != null && !nodePreference.getOriginalUri().isEmpty()) {
            uri = nodePreference.getOriginalUri() + "/?idg=" + nodeUri.getIdConcept()
                            + "&idt=" + idTheso;
        } else {
            uri = uriBase + "/?idg=" + nodeUri.getIdConcept()
                            + "&idt=" + idTheso;
        }

        return uri;
    }

    /**
     * Cette fonction permet de retourner l'URI du concept avec identifiant Ark
     * : si renseigné sinon l'URL du Site
     */
    private String getUriFromNodeUri(NodeUri nodeUri, String idTheso) {
        String uri = "";
        if (nodeUri == null) {
            return uri;
        }
        String uriBase = getPath();
        if (uriBase.endsWith("/")) {
            uriBase = uriBase.substring(0, uriBase.length() - 1);
        }

        // Choix de l'URI pour l'export : 
        // Si Handle est actif, on le prend en premier 
        // sinon,  on vérifie si Ark est actif, 
        // en dernier, on prend l'URL basique d'Opentheso
        // 1 seule URI est possible pour l'export par concept
        // URI de type Ark

        if(nodePreference.isOriginalUriIsArk()) {
            if (nodeUri.getIdArk() != null && !nodeUri.getIdArk().trim().isEmpty()) {
                uri = nodePreference.getOriginalUri()+ "/" + nodeUri.getIdArk();
                return uri;
            } else {
                uri = uriBase + "/?idc=" + nodeUri.getIdConcept().trim()
                        + "&idt=" + idTheso;
                return uri;
            }
        }
        if(nodePreference.isOriginalUriIsHandle()) {
            // URI de type Handle
            if (nodeUri.getIdHandle() != null) {
                if (!nodeUri.getIdHandle().trim().isEmpty()) {
                    uri = "https://hdl.handle.net/" + nodeUri.getIdHandle();
                    return uri;
                }
            }
        }
        if(nodePreference.isOriginalUriIsDoi()) {
            // URI de type Doi
            if (nodeUri.getIdDoi() != null) {
                if (!nodeUri.getIdDoi().trim().isEmpty()) {
                    uri = "https://doi.org/" + nodeUri.getIdDoi();
                    return uri;
                }
            }
        }        

        // si on ne trouve pas ni Handle, ni Ark
        if(nodePreference.getOriginalUri() != null && !nodePreference.getOriginalUri().isEmpty()) {
            uri = nodePreference.getOriginalUri() + "/?idc=" + nodeUri.getIdConcept().trim()
                        + "&idt=" + idTheso;
        } else {
            uri = uriBase + "/?idc=" + nodeUri.getIdConcept().trim()
                        + "&idt=" + idTheso;
        }

                        //+ "&amp;idt=" + idTheso;
    //    uri = nodePreference.getCheminSite() + nodeUri.getIdConcept();
        return uri;
    }

    
    private String getUriForFacette(String idFacet, String idTheso){
        String uri = "";
        if (idFacet == null) {
            return uri;
        }
        String uriBase = getPath();
        if (uriBase.endsWith("/")) {
            uriBase = uriBase.substring(0, uriBase.length() - 1);
        }
        uri =uriBase + "/?idf=" + idFacet + "&idt=" +idTheso;
        return uri;
    }    
    
    /**
     * permet de retourner le Path de l'application
     * exp:  //http://localhost:8082/opentheso2
     * @return
     */
    private String getPath(){
        if(StringUtils.isNotEmpty(nodePreference.getCheminSite())){
            return nodePreference.getCheminSite();
        }
        if(FacesContext.getCurrentInstance() == null) {
            return nodePreference.getOriginalUri();
        }
        String path = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("origin");
        if(path == null)
            return nodePreference.getOriginalUri();
        return path + FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

    public void getMessages() {
        if(messages != null) {
            if (!messages.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info :", messages));
            }
        }
    }
  
}
