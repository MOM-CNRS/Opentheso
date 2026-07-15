package fr.cnrs.opentheso.models.skosapi;


import fr.cnrs.opentheso.models.terms.ConceptPreferredTermLookup;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.concept.NodeConceptTree;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.utils.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import fr.cnrs.opentheso.utils.ToolsHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Miled Rousset
 *
 */
@Slf4j
@Data
public class SKOSResource {

    private String uri;
    private String localUri;
    private String identifier;
    private String arkId;
    private SKOSdc sdc;
    private int property;

    /// le status du concept : CA=candidat, DEP=déprécié, autre=concept
    private int status;

    private ArrayList<SKOSReplaces> sKOSReplaces;

    private SKOSStatus skosStatus;
    private ArrayList<SKOSDiscussion> messages;
    private ArrayList<SKOSVote> votes;
    private ArrayList<SKOSLabel> labelsList;
    private ArrayList<SKOSRelation> relationsList;
    private ArrayList<SKOSDocumentation> documentationsList;
    private ArrayList<SKOSDate> dateList;
    private ArrayList<SKOSAgent> agentList;
    private List<SKOSGPSCoordinates> gpsCoordinates;
    private ArrayList<SKOSNotation> notationList;
    private ArrayList<SKOSMatch> matchList;
    private ArrayList<String> dcRelations;
    private ArrayList<String> externalResources;
    
    // Autopostage the complete path to the concept
    private ArrayList<String> paths;

    // images
    private ArrayList<NodeImage> nodeImages;
    
    private FoafImage foafImage;    

    // pour les labels et le DC du thésaurus
    private Thesaurus thesaurus;

    /**
     *
     */
    public SKOSResource() {
        skosStatus = null;
        labelsList = new ArrayList<>();
        relationsList = new ArrayList<>();
        documentationsList = new ArrayList<>();
        dateList = new ArrayList<>();
        agentList = new ArrayList<>();
        gpsCoordinates = new ArrayList<>();
        notationList = new ArrayList<>();
        matchList = new ArrayList<>();
        nodeImages = new ArrayList<>();
        messages = new ArrayList<>();
        votes = new ArrayList<>();
        status = 80;
        sKOSReplaces = new ArrayList<>();
        thesaurus = new Thesaurus();
        foafImage = new FoafImage();
        dcRelations = new ArrayList<>();
        externalResources = new ArrayList<>();
    }

    public SKOSResource(String uri, int property) {

        skosStatus = null;
        localUri = "";
        labelsList = new ArrayList<>();
        relationsList = new ArrayList<>();
        documentationsList = new ArrayList<>();
        dateList = new ArrayList<>();
        agentList = new ArrayList<>();
        gpsCoordinates = new ArrayList<>();
        notationList = new ArrayList<>();
        matchList = new ArrayList<>();
        nodeImages = new ArrayList<>();
        messages = new ArrayList<>();
        votes = new ArrayList<>();
        status = 80;
        sKOSReplaces = new ArrayList<>();
        thesaurus = new Thesaurus();

        this.property = property;
        this.uri = uri;
    }

    public void clear(){
        skosStatus = null;

        if(labelsList != null)
            labelsList.clear();
        if(relationsList != null)
            relationsList.clear();
        if(documentationsList != null)
            documentationsList.clear();
        if(dateList != null)
            dateList.clear();
        if(agentList != null)
            agentList.clear();
        gpsCoordinates = null;
        if(notationList != null)
            notationList.clear();
        if(matchList != null)
            matchList.clear();
        if(nodeImages != null)
            nodeImages.clear();
        if(messages != null)
            messages.clear();
        if(votes != null)
            votes.clear();
        if(sKOSReplaces != null)
            sKOSReplaces.clear();
        thesaurus = null;
        foafImage = null;
        dcRelations = null;
        externalResources = null;
    }

    public void addDcRelations(String dcRelation) {
        this.dcRelations.add(dcRelation);
    }

    public void addAgent(String agent, int prop) {
        agentList.add(new SKOSAgent(agent, prop));
    }

    public void addMatch(String v, int prop) {
        if (new ToolsHelper().isValidURI(v)) {
            matchList.add(new SKOSMatch(v, prop));
        } else {
            log.debug("URI invalide : " + v);
        }
    }


    public void addNotation(String notation) {
        notationList.add(new SKOSNotation(notation));
    }

    public void addExternalResource(String externalResource) {
        this.externalResources.add(externalResource);
    }

    /**
     * Méthode d'ajout des labels à la ressource, dans une ArrayList
     *
     * @param lab un String label
     * @param lang un String langue conforme xml (fr, eng...)
     * @param prop un int SKOSProperty
     */
    public void addLabel(String lab, String lang, int prop) {
        try {
            SKOSLabel label = new SKOSLabel(lab, lang, prop);
            labelsList.add(label);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Méthode d'ajout des relations pour les concepts dépréciés
     *
     * @param uri un String URI
     * @param prop un int SKOSProperty
     */
    public void addReplaces(String uri, int prop) {
        try {
            SKOSReplaces sKOSReplace = new SKOSReplaces(uri, prop);
            sKOSReplaces.add(sKOSReplace);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * Méthode d'ajout des relations à la ressource, dans une ArrayList
     *
     * @param uri un String URI
     * @param prop un int SKOSProperty
     */
    public void addRelation(String localIdentifier, String uri, int prop) {
        try {
            SKOSRelation relation = new SKOSRelation(localIdentifier, uri, prop);
            relationsList.add(relation);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * Méthode d'ajout des documentations à la ressource, dans une ArrayList
     *
     * @param text un String texte de la documentation
     * @param lang un String langue conforme xml (fr, en...)
     * @param prop un int SKOSProperty
     */
    public void addDocumentation(String text, String lang, int prop) {
        try {
            SKOSDocumentation documentation = new SKOSDocumentation(text, lang, prop);
            documentationsList.add(documentation);
        } catch (Exception e) {
            e.getMessage();
            System.out.println(e.getMessage());
        }
    }

    /**
     * Méthode d'ajout des dates de création et de modification à la ressource,
     * dans une ArrayList
     *
     * @param sDate
     * @param prop un int SKOSProperty
     */
    public void addDate(String sDate, int prop) {
        try {
            SKOSDate date = new SKOSDate(sDate, prop);
            dateList.add(date);
        } catch (Exception e) {
            e.getMessage();
        }
    }
    
    /**
     * Méthode d'ajout des Idetifier type DC
     *
     * @param identifier
     * @param prop un int SKOSProperty
     */
    public void addIdentifier(String identifier, int prop) {
        this.identifier = identifier;
        try {
            SKOSdc dc = new SKOSdc(identifier, prop);
            this.sdc = dc;
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public void addNodeImage(NodeImage nodeImage) {
        this.nodeImages.add(nodeImage);
    }

    /**
     * Surcharge de la méthode toString() afin de mettre au format xml la
     * ressource
     *
     * @return
     */
    public String toString() {
        String xmlRessource;
        Iterator<SKOSLabel> itLab = labelsList.iterator();
        Iterator<SKOSRelation> itRel = relationsList.iterator();
        Iterator<SKOSDocumentation> itDoc = documentationsList.iterator();
        Iterator<SKOSDate> itDate = dateList.iterator();

        xmlRessource = "<skos:Concept rdf:about=\"" + uri + "\">\n";

        while (itLab.hasNext()) {
            xmlRessource += "        " + itLab.next().toString();
        }

        while (itRel.hasNext()) {
            xmlRessource += "        " + itRel.next().toString();
        }

        while (itDoc.hasNext()) {
            xmlRessource += "        " + itDoc.next().toString();
        }
        while (itDate.hasNext()) {
            xmlRessource += "        " + itDate.next().toString();
        }
        if (sdc != null) {
            if (sdc.getIdentifier() != null) {
                if (!sdc.getIdentifier().isEmpty()) {
                    xmlRessource += "        " + sdc.toString();
                }
            }
        }
        if (arkId != null && !arkId.isEmpty()) {
            xmlRessource += "        " + "<arkId>" + arkId + "<arkId>\n";
        }

        xmlRessource += "    </skos:Concept>\n";

        return xmlRessource;
    }

    /**
     * sort par ordre alphabetique par raport a une langue donnée et assosi les
     * noms au ID ne change pas l'ordre si isTrad est a true
     *
     * @param isTrad
     * @param langCode
     * @param langCode2
     * @param idToNameHashMap
     * @param idToIsTrad
     * @param resourceChecked
     * @return
     */
    public static Comparator<SKOSResource> sortAlphabeticInLang(boolean isTrad, String langCode, String langCode2, HashMap<String, String> idToNameHashMap, HashMap<String, List<Integer>> idToIsTrad, List<String> resourceChecked) {
        return new AlphabeticComparator(isTrad, langCode, langCode2, idToNameHashMap, idToIsTrad, resourceChecked);
    }

    /**
     * sort par ordre alphabetique par raport a une langue donnée et assosi les
     * noms au ID et rempli les assosiation des IDs toutes les information du
     * term sont stocké dans les hashmaps ne change pas l'ordre si isTrad est a
     * true
     *
     * @param isTrad
     * @param langCode
     * @param langCode2
     * @param idToNameHashMap
     * @param idToChildId
     * @param idToDocumentation
     * @param idToMatch
     * @param idToGPS
     * @param idToImg
     * @param resourceChecked
     * @param idToIsTradDiff
     * @return
     */
    public static Comparator<SKOSResource> sortForHiera(boolean isTrad, String langCode,
            String langCode2, HashMap<String, String> idToNameHashMap, HashMap<String, List<String>> idToChildId, HashMap<String, ArrayList<String>> idToDocumentation,
            HashMap<String, ArrayList<String>> idToMatch, HashMap<String, List<String>> idToGPS,
            HashMap<String, ArrayList<NodeImage>> idToImg, ArrayList<String> resourceChecked,
            HashMap<String, ArrayList<Integer>> idToIsTradDiff, TermService termService) {
        return sortForHiera(isTrad, langCode, langCode2, idToNameHashMap, idToChildId, idToDocumentation,
                idToMatch, idToGPS, idToImg, resourceChecked, idToIsTradDiff, termService::getThisTerm);
    }

    public static Comparator<SKOSResource> sortForHiera(boolean isTrad, String langCode,
            String langCode2, HashMap<String, String> idToNameHashMap, HashMap<String, List<String>> idToChildId, HashMap<String, ArrayList<String>> idToDocumentation,
            HashMap<String, ArrayList<String>> idToMatch, HashMap<String, List<String>> idToGPS,
            HashMap<String, ArrayList<NodeImage>> idToImg, ArrayList<String> resourceChecked,
            HashMap<String, ArrayList<Integer>> idToIsTradDiff, ConceptPreferredTermLookup termLookup) {

        return new HieraComparator(isTrad, langCode, langCode2, idToNameHashMap, idToChildId, idToDocumentation,
                idToMatch, idToGPS, idToImg, resourceChecked, idToIsTradDiff, termLookup);
    }

    private static class HieraComparator implements Comparator<SKOSResource> {

        String langCode;
        String langCode2;
        HashMap<String, String> idToNameHashMap;
        HashMap<String, List<String>> idToChildId;
        HashMap<String, ArrayList<String>> idToDocumentation;
        HashMap<String, ArrayList<String>> idToMatch;
        HashMap<String, List<String>> idToGPS;
        HashMap<String, ArrayList<NodeImage>> idToImg;
        boolean isTrad;
        ArrayList<String> resourceChecked;
        ConceptPreferredTermLookup termLookup;
        HashMap<String, ArrayList<Integer>> idToIsTradDiff;

        public HieraComparator(boolean isTrad, String langCode, String langCode2,
                HashMap<String, String> idToNameHashMap, HashMap<String, List<String>> idToChildId,
                HashMap<String, ArrayList<String>> idToDocumentation, HashMap<String, ArrayList<String>> idToMatch,
                HashMap<String, List<String>> idToGPS, HashMap<String, ArrayList<NodeImage>> idToImg, ArrayList<String> resourceChecked,
                HashMap<String, ArrayList<Integer>> idToIsTradDiff, ConceptPreferredTermLookup termLookup) {

            this.termLookup = termLookup;
            this.langCode = langCode;
            this.langCode2 = langCode2;
            this.idToNameHashMap = idToNameHashMap;
            this.idToChildId = idToChildId;
            this.idToDocumentation = idToDocumentation;
            this.idToMatch = idToMatch;
            this.idToGPS = idToGPS;
            this.idToImg = idToImg;
            this.isTrad = isTrad;
            this.resourceChecked = resourceChecked;
            this.idToIsTradDiff = idToIsTradDiff;
            this.idToNameHashMap.clear();
        }

        @Override
        public int compare(SKOSResource r1, SKOSResource r2) {
            String r1_name = null;
            String r2_name = null;

            String id1 = r1.getIdentifier();//getIdFromUri(r1.getUri());
            String id2 = r2.getIdentifier();//getIdFromUri(r2.getUri());

            if (!resourceChecked.contains(id1)) {

                writeIdToChild(r1);
                writeIdToMatch(r1);
                writeIdToGPS(r1);
                writeIdToImg(r1);
                checkTrad(r1);
                resourceChecked.add(id1);
            }
            writeIdToDocumentation(r1);

            if (!resourceChecked.contains(id2)) {

                writeIdToChild(r2);
                writeIdToMatch(r2);
                writeIdToGPS(r2);
                writeIdToImg(r2);
                checkTrad(r2);
                resourceChecked.add(id2);
            }
            writeIdToDocumentation(r2);

            for (SKOSLabel label : r1.getLabelsList()) {
                if (label.getProperty() == SKOSProperty.PREF_LABEL && label.getLanguage().equals(langCode)) {

                    r1_name = label.getLabel();
                    idToNameHashMap.put(id1, r1_name);

                }
            }

            for (SKOSLabel label2 : r2.getLabelsList()) {
                if (label2.getProperty() == SKOSProperty.PREF_LABEL && label2.getLanguage().equals(langCode)) {
                    r2_name = label2.getLabel();

                    idToNameHashMap.put(id2, r2_name);

                }
            }

            if (isTrad) {
                return 0;
            }

            if (r1_name == null) {
                r1_name = "";
            }
            if (r2_name == null) {
                r2_name = "";
            }

            if (r1_name.length() == 0) {
                if (r2_name.length() == 0) {
                    return 0;               // Both empty - so indicate
                }
                return 1;                   // empty string sorts last
            }
            if (r2_name.length() == 0) {
                return -1;                  // empty string sorts last                  
            }

            return r1_name.compareTo(r2_name);

        }

        private void writeIdToGPS(SKOSResource resource) {
            if (CollectionUtils.isNotEmpty(resource.getGpsCoordinates())) {
                idToGPS.put(resource.getIdentifier(),
                        resource.getGpsCoordinates().stream()
                                .map(element -> element.getLat() + " " + element.getLon())
                                .collect(Collectors.toList()));
            }
        }

        private void writeIdToImg(SKOSResource resource) {
            String key = resource.getIdentifier();//getIdFromUri(resource.getUri());

            if (resource.getNodeImages() != null) {
                idToImg.put(key, resource.getNodeImages());
            }
        }

        private void writeIdToMatch(SKOSResource resource) {
            for (SKOSMatch match : resource.getMatchList()) {
                String key = resource.getIdentifier();//getIdFromUri(resource.getUri());
                String matchTypeName = null;
                int prop = match.getProperty();
                switch (prop) {
                    case SKOSProperty.EXACT_MATCH:
                        matchTypeName = "exactMatch";
                        break;
                    case SKOSProperty.CLOSE_MATCH:
                        matchTypeName = "closeMatch";
                        break;
                    case SKOSProperty.BROAD_MATCH:
                        matchTypeName = "broadMatch";
                        break;
                    case SKOSProperty.RELATED_MATCH:
                        matchTypeName = "relatedMatch";
                        break;
                    case SKOSProperty.NARROWER_MATCH:
                        matchTypeName = "narrowMatch";
                        break;
                }

                ArrayList<String> mat = idToMatch.get(key);

                if (mat == null) {
                    ArrayList<String> temp = new ArrayList<>();
                    temp.add(matchTypeName + ": " + match.getValue());
                    idToMatch.put(key, temp);
                } else {
                    String matToAdd = matchTypeName + ": " + match.getValue();
                    if (!mat.contains(matToAdd)) {
                        mat.add(matToAdd);
                    }
                }

            }

        }

        private void writeIdToDocumentation(SKOSResource resource) {
            for (SKOSDocumentation documentation : resource.getDocumentationsList()) {
                String documentationText = null;

                if (documentation.getLanguage().equals(langCode)) {
                    documentationText = documentation.getText();
                } else {
                    continue;
                }

                String key = resource.getIdentifier();//getIdFromUri(resource.getUri());
                String docTypeName = null;
                int prop = documentation.getProperty();

                switch (prop) {
                    case SKOSProperty.DEFINITION:
                        docTypeName = "definition";
                        break;
                    case SKOSProperty.SCOPE_NOTE:
                        docTypeName = "scopeNote";
                        break;
                    case SKOSProperty.EXAMPLE:
                        docTypeName = "example";
                        break;
                    case SKOSProperty.HISTORY_NOTE:
                        docTypeName = "historyNote";
                        break;
                    case SKOSProperty.EDITORIAL_NOTE:
                        docTypeName = "editorialNote";
                        break;
                    case SKOSProperty.CHANGE_NOTE:
                        docTypeName = "changeNote";
                        break;
                    case SKOSProperty.NOTE:
                        docTypeName = "note";
                        break;
                    default:
                        docTypeName = "note";
                        break;
                }
                ArrayList<String> doc = idToDocumentation.get(key);

                if (doc == null) {
                    ArrayList<String> temp = new ArrayList<>();
                    temp.add(docTypeName + ": " + documentationText);
                    idToDocumentation.put(key, temp);
                } else {
                    String docToAdd = docTypeName + ": " + documentationText;
                    if (!doc.contains(docToAdd)) {
                        doc.add(docToAdd);
                    }
                }
            }
        }

        private class TermTemp {

            public String term;
            public String idConcept;
            public String idArk;
        }

        private void writeIdToChild(SKOSResource resource) {

            String key = resource.getIdentifier();//getIdFromUri(resource.getUri());
            for (SKOSRelation relation : resource.getRelationsList()) {
                if (relation.getProperty() == SKOSProperty.NARROWER
                        || relation.getProperty() == SKOSProperty.NARROWER_GENERIC
                        || relation.getProperty() == SKOSProperty.NARROWER_INSTANTIAL
                        || relation.getProperty() == SKOSProperty.NARROWER_PARTITIVE) {

                    if (idToChildId.get(key) == null) {
                        ArrayList<String> temp = new ArrayList<>();
                        temp.add(relation.getLocalIdentifier());
                        idToChildId.put(key, temp);
                    } else {
                        String childId = relation.getLocalIdentifier();
                        if (!idToChildId.get(key).contains(childId)) {
                            idToChildId.get(key).add(childId);
                        }
                    }

                }
            }

            List<String> childs = idToChildId.get(key);
            if (childs != null) {
                ArrayList<TermTemp> conceptIdsTemps = new ArrayList<>();
                for (String child : childs) {
                    String idTheso = resource.getLocalUri().substring(resource.getLocalUri().indexOf("idt=") + 4);
                    Term term = termLookup.getThisTerm(child, idTheso, langCode);
                    if (term != null) {
                        TermTemp termTemp = new TermTemp();
                        termTemp.idConcept = child;
                        termTemp.term = term.getLexicalValue();
                        conceptIdsTemps.add(termTemp);
                    }
                }
                
                NodeConceptTree nodeConceptTree = new NodeConceptTree();
                conceptIdsTemps.sort((o1, o2) -> nodeConceptTree.naturalCompare(o1.term, o2.term, true));
                idToChildId.put(key, conceptIdsTemps.stream().map(c -> c.idConcept).collect(Collectors.toList()));
            }
        }

        private void checkTrad(SKOSResource resource) {

            String key = resource.getIdentifier();//getIdFromUri(resource.getUri());

            int lang1Doc = 0;
            int lang2Doc = 0;

            for (SKOSDocumentation doc : resource.getDocumentationsList()) {

                if (doc.getLanguage().equals(langCode)) {
                    lang1Doc++;
                }
                if (doc.getLanguage().equals(langCode2)) {
                    lang2Doc++;
                }

                int diff = Math.abs(lang1Doc - lang2Doc);

                ArrayList<Integer> newIsTrad = new ArrayList<>();

                for (int i = 0; i < diff; i++) {
                    newIsTrad.add(SKOSProperty.NOTE);
                }
                idToIsTradDiff.put(key, newIsTrad);
            }
        }
    }

    private static class AlphabeticComparator implements Comparator<SKOSResource> {

        String langCode;
        String langCode2;
        HashMap<String, String> idToNameHashMap;
        boolean isTrad;
        HashMap<String, List<Integer>> idToIsTrad;
        List<String> resourceChecked;

        public AlphabeticComparator(boolean isTrad, String langCode, String langCode2, HashMap<String, String> idToNameHashMap, HashMap<String, List<Integer>> idToIsTrad, List<String> resourceChecked) {
            this.langCode = langCode;
            this.idToNameHashMap = idToNameHashMap;
            this.isTrad = isTrad;
            this.langCode2 = langCode2;
            this.idToIsTrad = idToIsTrad;
            this.resourceChecked = resourceChecked;

            this.idToNameHashMap.clear();
        }

        @Override
        public int compare(SKOSResource r1, SKOSResource r2) {

            String r1_name = null;
            String r2_name = null;

            String id1 = r1.getIdentifier();//getIdFromUri(r1.getUri());
            String id2 = r2.getIdentifier();//getIdFromUri(r2.getUri());

            if (!resourceChecked.contains(id1)) {
                checkTrad(r1);
                resourceChecked.add(id1);
            }
            if (!resourceChecked.contains(id2)) {
                checkTrad(r2);
                resourceChecked.add(id2);

            }

            for (SKOSLabel label : r1.getLabelsList()) {
                if (label.getProperty() == SKOSProperty.PREF_LABEL && label.getLanguage().equals(langCode)) { // to test
                    r1_name = label.getLabel();
                    idToNameHashMap.put(id1, r1_name);

                }
            }
            for (SKOSLabel label2 : r2.getLabelsList()) {
                if (label2.getProperty() == SKOSProperty.PREF_LABEL && label2.getLanguage().equals(langCode)) {
                    r2_name = label2.getLabel();
                    idToNameHashMap.put(id2, r2_name);

                }

            }
            if (isTrad) {
                return 0;
            }

            if (r1_name == null) {
                r1_name = "";
            }
            if (r2_name == null) {
                r2_name = "";
            }

            if (r1_name.length() == 0) {
                if (r2_name.length() == 0) {
                    return 0;               // Both empty - so indicate
                }
                return 1;                   // empty string sorts last
            }
            if (r2_name.length() == 0) {
                return -1;                  // empty string sorts last                  
            }
            
            return new NodeConceptTree().naturalCompare(r1_name, r2_name, true);
        }

        private void checkTrad(SKOSResource resource) {

            int lang1Pref = 0;
            int lang2Pref = 0;

            int lang1Alt = 0;
            int lang2Alt = 0;

            int lang1Doc = 0;
            int lang2Doc = 0;

            //prefLabel
            for (SKOSLabel label : resource.getLabelsList()) {
                if (label.getLanguage().equals(langCode) && label.getProperty() == SKOSProperty.PREF_LABEL) {
                    lang1Pref++;
                }
                if (label.getLanguage().equals(langCode2) && label.getProperty() == SKOSProperty.PREF_LABEL) {
                    lang2Pref++;
                }

                if (lang1Pref > 0 && lang2Pref > 0) {
                    lang1Pref--;
                    lang2Pref--;

                    String key = getIdFromUri(resource.getUri());
                    List<Integer> trad = idToIsTrad.get(key);
                    if (trad == null) {
                        ArrayList<Integer> newIsTrad = new ArrayList<>();
                        newIsTrad.add(SKOSProperty.PREF_LABEL);
                        idToIsTrad.put(key, newIsTrad);
                    } else {
                        trad.add(SKOSProperty.PREF_LABEL);
                    }
                }
            }
            //altLabel
            for (SKOSLabel label : resource.getLabelsList()) {

                if (label.getLanguage().equals(langCode) && label.getProperty() == SKOSProperty.ALT_LABEL) {
                    lang1Alt++;
                }
                if (label.getLanguage().equals(langCode2) && label.getProperty() == SKOSProperty.ALT_LABEL) {
                    lang2Alt++;
                }

                if (lang1Alt > 0 && lang2Alt > 0) {
                    lang1Alt--;
                    lang2Alt--;

                    String key = getIdFromUri(resource.getUri());
                    List<Integer> trad = idToIsTrad.get(key);
                    if (trad == null) {
                        ArrayList<Integer> newIsTrad = new ArrayList<>();
                        newIsTrad.add(SKOSProperty.ALT_LABEL);
                        idToIsTrad.put(key, newIsTrad);
                    } else {
                        trad.add(SKOSProperty.ALT_LABEL);
                    }
                }

            }

            for (SKOSDocumentation doc : resource.getDocumentationsList()) {

                if (doc.getLanguage().equals(langCode)) {
                    lang1Doc++;
                }
                if (doc.getLanguage().equals(langCode2)) {
                    lang2Doc++;
                }

                if (lang1Doc > 0 && lang2Doc > 0) {
                    lang1Doc--;
                    lang2Doc--;

                    String key = getIdFromUri(resource.getUri());
                    List<Integer> trad = idToIsTrad.get(key);
                    if (trad == null) {
                        ArrayList<Integer> newIsTrad = new ArrayList<>();
                        newIsTrad.add(SKOSProperty.NOTE);
                        idToIsTrad.put(key, newIsTrad);
                    } else {
                        trad.add(SKOSProperty.NOTE);
                    }
                }

            }
        }
    }

    public static String getIdFromUri(String uri) {
        if (uri.contains("idg=")) {
            if (uri.contains("&")) {
                uri = uri.substring(uri.indexOf("idg=") + 4, uri.indexOf("&"));
            } else {
                uri = uri.substring(uri.indexOf("idg=") + 4);
            }
        } else {
            if (uri.contains("idc=")) {
                if (uri.contains("&")) {
                    uri = uri.substring(uri.indexOf("idc=") + 4, uri.indexOf("&"));
                } else {
                    uri = uri.substring(uri.indexOf("idc=") + 4, uri.length());
                }
            } else {
                if (uri.contains("#")) {
                    uri = uri.substring(uri.indexOf("#") + 1, uri.length());
                } else {
                    uri = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
                }
            }
        }

        return StringUtils.normalizeStringForIdentifier(uri);
    }

    public SKOSStatus getSkosStatus() {
        return skosStatus;
    }

    public void setSkosStatus(String[] statusDatas) {
        skosStatus = new SKOSStatus();
        skosStatus.setIdStatus(statusDatas[0]);
        skosStatus.setMessage(statusDatas[1]);
        skosStatus.setIdUser(statusDatas[2]);
        skosStatus.setDate(statusDatas[3]);
        skosStatus.setIdConcept(identifier);
        skosStatus.setIdThesaurus(uri.substring(uri.indexOf("idt=") + 4, uri.length()));
    }

    public void setSkosStatus(SKOSStatus skosStatus) {
        this.skosStatus = skosStatus;
    }

    public ArrayList<SKOSDiscussion> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<SKOSDiscussion> messages) {
        this.messages = messages;
    }

    public void addMessage(String[] voteDatas) {
        SKOSDiscussion skosDiscussion = new SKOSDiscussion();
        skosDiscussion.setDate(voteDatas[0]);
        skosDiscussion.setIdUser(voteDatas[1] != null ? Integer.parseInt(voteDatas[1]) : null);
        skosDiscussion.setMsg(voteDatas[2]);
        messages.add(skosDiscussion);
    }

    public void addMessage(SKOSDiscussion skosDiscussion) {
        messages.add(skosDiscussion);
    }

    public ArrayList<SKOSVote> getVotes() {
        return votes;
    }

    public void addVote(String[] voteDatas) {
        SKOSVote skosVote = new SKOSVote();
        skosVote.setIdThesaurus(voteDatas[0]);
        skosVote.setIdConcept(voteDatas[1]);
        skosVote.setIdNote(voteDatas[2]);
        skosVote.setIdUser(voteDatas[3] != null ? Integer.parseInt(voteDatas[3]) : null);
        skosVote.setTypeVote(voteDatas[4]);
        votes.add(skosVote);
    }

    public void addVote(SKOSVote skosVote) {
        votes.add(skosVote);
    }

    public ArrayList<SKOSReplaces> getsKOSReplaces() {
        return sKOSReplaces;
    }

    public void setsKOSReplaces(ArrayList<SKOSReplaces> sKOSReplaces) {
        this.sKOSReplaces = sKOSReplaces;
    }

}
