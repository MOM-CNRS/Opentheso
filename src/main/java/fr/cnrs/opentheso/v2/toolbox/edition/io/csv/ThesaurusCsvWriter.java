package fr.cnrs.opentheso.v2.toolbox.edition.io.csv;

import fr.cnrs.opentheso.models.concept.NodeCompareTheso;
import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvAlignmentRow;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvByIdRow;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvExportPersistence;
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
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class ThesaurusCsvWriter {

    private static final String CSV_EXPORT_ERROR = "Erreur lors de l'export CSV";
    private static final String COL_ARK_ID = "arkId";


    private final ThesaurusEditionCsvExportPersistence csvExportQuerySupport;
    private final String delim_multi_datas = "##";

    /**
     * Export en CSV avec tous les champs
     */
    public byte[] writeCsv(SKOSXmlDocument xmlDocument, List<NodeLangTheso> selectedLanguages, char delimiter) {
        if (selectedLanguages == null || selectedLanguages.isEmpty()) {
            return null;
        }
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (OutputStreamWriter out = new OutputStreamWriter(os, StandardCharsets.UTF_8); CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.RFC4180.builder().setDelimiter(delimiter).build())) {

                // write Headers
                ArrayList<String> header = new ArrayList<>();
                header.add("URI");
                header.add("rdf:type");
                header.add("localURI");
                header.add("identifier");
                header.add(COL_ARK_ID);

                List<String> langs = selectedLanguages.stream().map(lang -> lang.getCode()).collect(Collectors.toList());
                //skos:prefLabel
                langs.forEach((lang) -> {
                    header.add("skos:prefLabel@" + lang);
                });

                //skos:altLabel
                langs.forEach((lang) -> {
                    header.add("skos:altLabel@" + lang);
                });

                //skos:hiddenLabel
                langs.forEach((lang) -> {
                    header.add("skos:hiddenLabel@" + lang);
                });

                //skos:definition
                langs.forEach((lang) -> {
                    header.add("skos:definition@" + lang);
                });

                //skos:scopeNote
                langs.forEach((lang) -> {
                    header.add("skos:scopeNote@" + lang);
                });

                //skos:note
                langs.forEach((lang) -> {
                    header.add("skos:note@" + lang);
                });

                //skos:historyNote
                langs.forEach((lang) -> {
                    header.add("skos:historyNote@" + lang);
                });

                //skos:editorialNote
                langs.forEach((lang) -> {
                    header.add("skos:editorialNote@" + lang);
                });

                //skos:changeNote
                langs.forEach((lang) -> {
                    header.add("skos:changeNote@" + lang);
                });

                //skos:example
                langs.forEach((lang) -> {
                    header.add("skos:example@" + lang);
                });

                header.add("skos:notation");
                header.add("skos:narrower");
                header.add("narrowerId");
                header.add("skos:broader");
                header.add("broaderId");
                header.add("skos:related");
                header.add("relatedId");
                header.add("skos:exactMatch");
                header.add("skos:closeMatch");
                header.add("geo:lat");
                header.add("geo:long");
                header.add("geo:gps");
                header.add("skos:member");
                header.add("memberId");                
                
                // pour gérer les sous_collections
                header.add("iso-thes:subGroup"); 
                
                // pour les Facettes qui appartiennent au concept 
                header.add("iso-thes:superOrdinate");            
                header.add("superOrdinateId");

                // pour signaler que le concept est déprécié
                header.add("owl:deprecated");
                // pour signaler que le concept est remplacé par un autre concept
                header.add("dcterms:isReplacedBy");
                
                // pour les images
                header.add("foaf:Image");                
                
                header.add("dcterms:source");                
                header.add("dcterms:created");
                header.add("dcterms:modified");
                csvFilePrinter.printRecord(header);

                ArrayList<Object> csvRow = new ArrayList<>();
                // write concepts and collections
                xmlDocument.getGroupList().forEach(groupe -> {
                    try {
                        writeResource(csvRow, csvFilePrinter, groupe, "skos:Collection", langs);
                    } catch (IOException e) {
                        log.warn(CSV_EXPORT_ERROR, e);
                    }
                });
                // write Facettes
                xmlDocument.getFacetList().forEach(facet -> {
                    try {
                        writeResource(csvRow, csvFilePrinter, facet, "skos-thes:ThesaurusArray", langs);
                    } catch (IOException e) {
                        log.warn(CSV_EXPORT_ERROR, e);
                    }
                });                
                // write all concepts
                xmlDocument.getConceptList().forEach(concept -> {
                    try {
                        writeResource(csvRow, csvFilePrinter, concept, "skos:Concept", langs);
                    } catch (IOException e) {
                        log.warn(CSV_EXPORT_ERROR, e);
                    }
                });
            }
            return os.toByteArray();
        } catch (IOException e) {
            log.warn(CSV_EXPORT_ERROR, e);
            return null;
        }
    }

    private void writeResource(ArrayList<Object> csvRow, CSVPrinter csvFilePrinter,
            SKOSResource skosResource, String type, List<String> langs) throws IOException {

        //URI + rdf:type
        csvRow.add(skosResource.getUri());
        csvRow.add(type);

        //localURI
        csvRow.add(skosResource.getLocalUri());

        // identifier and arkId
        if (StringUtils.isNoneEmpty(skosResource.getIdentifier())) {
            csvRow.add(skosResource.getIdentifier());
        } else {
            csvRow.add("");
        }

        if (skosResource.getArkId() != null && !skosResource.getArkId().isEmpty()) {
            csvRow.add(skosResource.getArkId());
        } else {
            csvRow.add("");
        }

        //skos:prefLabel
        for (String lang : langs) {
            csvRow.add(getPrefLabelValue(skosResource.getLabelsList(), lang, SKOSProperty.PREF_LABEL));
        }

        //skos:altLabel
        for (String lang : langs) {
            csvRow.add(getAltLabelValue(skosResource.getLabelsList(), lang, SKOSProperty.ALT_LABEL));
        }

        //skos:hiddenLabel
        for (String lang : langs) {
            csvRow.add(getAltLabelValue(skosResource.getLabelsList(), lang, SKOSProperty.HIDDEN_LABEL));
        }

        addDocumentationColumns(csvRow, skosResource, langs, SKOSProperty.DEFINITION);
        addDocumentationColumns(csvRow, skosResource, langs, SKOSProperty.SCOPE_NOTE);
        addDocumentationColumns(csvRow, skosResource, langs, SKOSProperty.NOTE);
        addDocumentationColumns(csvRow, skosResource, langs, SKOSProperty.HISTORY_NOTE);
        addDocumentationColumns(csvRow, skosResource, langs, SKOSProperty.EDITORIAL_NOTE);
        addDocumentationColumns(csvRow, skosResource, langs, SKOSProperty.CHANGE_NOTE);
        addDocumentationColumns(csvRow, skosResource, langs, SKOSProperty.EXAMPLE);

        // notation
        csvRow.add(getNotation(skosResource.getNotationList()));

        //narrower 
        csvRow.add(getRelationGivenValue(skosResource.getRelationsList(), SKOSProperty.NARROWER));
        //narrowerId
        csvRow.add(getRelationGivenValueId(skosResource.getRelationsList(), SKOSProperty.NARROWER));

        //broader
        var broader = getRelationGivenValue(skosResource.getRelationsList(), SKOSProperty.BROADER);
        if (StringUtils.isEmpty(broader)) {
            //broader = getRelationGivenValue(skosResource.getRelationsList(), SKOSProperty.TOP_CONCEPT_OF);
            csvRow.add("");
        } else {
            csvRow.add(broader);
        }
        //broaderId
        var broaderId = getRelationGivenValueId(skosResource.getRelationsList(), SKOSProperty.BROADER);
        if (StringUtils.isEmpty(broaderId)) {
            broaderId = ""; //getRelationGivenValueId(skosResource.getRelationsList(), SKOSProperty.TOP_CONCEPT_OF);
        }
        csvRow.add(broaderId);

        //related
        csvRow.add(getRelationGivenValue(skosResource.getRelationsList(), SKOSProperty.RELATED));
        //relatedId 
        csvRow.add(getRelationGivenValueId(skosResource.getRelationsList(), SKOSProperty.RELATED));

        //exactMatch
        csvRow.add(getAlligementValue(skosResource.getMatchList(), SKOSProperty.EXACT_MATCH));
        //closeMatch
        csvRow.add(getAlligementValue(skosResource.getMatchList(), SKOSProperty.CLOSE_MATCH));

        if (CollectionUtils.isNotEmpty(skosResource.getGpsCoordinates()) && skosResource.getGpsCoordinates().size() == 1) {
            //geo:lat
            csvRow.add(getLatValue(skosResource.getGpsCoordinates().get(0)));
            //geo:long
            csvRow.add(getLongValue(skosResource.getGpsCoordinates().get(0)));
        } else {
            //geo:lat
            csvRow.add("");
            //geo:long
            csvRow.add("");
        }
        //GPS
        if (CollectionUtils.isNotEmpty(skosResource.getGpsCoordinates()) && skosResource.getGpsCoordinates().size() > 1) {
            csvRow.add(getGpsValue(skosResource.getGpsCoordinates()));
        } else {
            csvRow.add("");
        }
        
        //skos:member (pour les concepts pour ajouter l'info de l'appartenance du concept à une collection)
        //skos:member (pour les Facettes et collections pour ajouter qui sont les membres)        
        csvRow.add(getMemberValue(skosResource.getRelationsList()));
        csvRow.add(getMemberId(skosResource.getRelationsList()));        
          
        // iso-thes:subGroup pour référencer l'URI des sous groupes 
        csvRow.add(getSubGroup(skosResource.getRelationsList()));        
        
        // iso-thes:superOrdinate pour référencer les Facettes du Concept
        csvRow.add(getFacettesOfConceptParent(skosResource.getRelationsList()));

        csvRow.add(getFacettesOfConceptParentId(skosResource.getRelationsList()));

        // owl:deprecated pour les concepts dépréciés
        if(skosResource.getStatus() == SKOSProperty.DEPRECATED)
            csvRow.add("true");
        else
            csvRow.add("false");
        // dcterms:isReplacedBy pour référencer les concepts qui remplacent celui qui est déprécié 
        csvRow.add(getReplaceBy(skosResource.getsKOSReplaces()));    
        
        // foaf:Image pour les images
        csvRow.add(getImages(skosResource.getNodeImages()));
        
        //dcterms:source
        if (CollectionUtils.isNotEmpty(skosResource.getExternalResources())) {
            csvRow.add(getExternalReources(skosResource.getExternalResources()));
        } else {
            csvRow.add("");
        }        
        
        
        //sdct:created
        csvRow.add(getDateValue(skosResource.getDateList(), SKOSProperty.CREATED));
        //dct:modified
        csvRow.add(getDateValue(skosResource.getDateList(), SKOSProperty.MODIFIED));

        csvFilePrinter.printRecord(csvRow);
        csvRow.clear();
    }
    
    private String getExternalReources(ArrayList<String> externalResources) {
        if(externalResources == null) return null;
        String value = "";
        for (String externalImage : externalResources) {
            if(StringUtils.isEmpty(value)){
                value = externalImage;
            } else {
                value = value + delim_multi_datas +  externalImage;
            }
        }
        return value;
    }       
    
    private String getImages(ArrayList<NodeImage> nodeImages) {
        String value = "";
        for (NodeImage nodeImage : nodeImages) {
            if(StringUtils.isEmpty(value)){
                value = "rdf:about=" + nodeImage.getUri();
            } else {
                value = value + delim_multi_datas +  "rdf:about=" + nodeImage.getUri();
            }
            if(!StringUtils.isEmpty(nodeImage.getCopyRight())) {
                value = value + "@@dcterms:rights=" +  nodeImage.getCopyRight();
            }      
            if(!StringUtils.isEmpty(nodeImage.getImageName())) {
                value = value + "@@dcterms:title=" +  nodeImage.getImageName();
            }
            if(!StringUtils.isEmpty(nodeImage.getCreator())) {
                value = value + "@@dcterms:creator=" +  nodeImage.getCreator();
            }              
        }
        return value;
    }     

    private String getReplaceBy(ArrayList<SKOSReplaces> sKOSReplaceses) {
        return sKOSReplaceses.stream()
                .filter(sKOSReplace -> (sKOSReplace.getProperty() == SKOSProperty.IS_REPLACED_BY))
                .map(sKOSReplace -> sKOSReplace.getTargetUri())
                .collect(Collectors.joining(delim_multi_datas));
    }                
    
    private String getFacettesOfConceptParent(ArrayList<SKOSRelation> sKOSRelations) {
        return sKOSRelations.stream()
                .filter(sKOSRelation -> (sKOSRelation.getProperty() == SKOSProperty.SUPER_ORDINATE))
                .map(sKOSRelation -> sKOSRelation.getTargetUri())
                .collect(Collectors.joining(delim_multi_datas));
    }
    private String getFacettesOfConceptParentId(ArrayList<SKOSRelation> sKOSRelations) {
        return sKOSRelations.stream()
                .filter(sKOSRelation -> (sKOSRelation.getProperty() == SKOSProperty.SUPER_ORDINATE))
                .map(sKOSRelation -> sKOSRelation.getLocalIdentifier())
                .collect(Collectors.joining(delim_multi_datas));
    }

    private String getSubGroup(ArrayList<SKOSRelation> sKOSRelations) {
        return sKOSRelations.stream()
                .filter(sKOSRelation -> (sKOSRelation.getProperty() == SKOSProperty.SUBGROUP))
                .map(sKOSRelation -> sKOSRelation.getTargetUri())
                .collect(Collectors.joining(delim_multi_datas));
    }    

    private String getPrefLabelValue(List<SKOSLabel> labels, String lang, int propertie) {
        String value = "";
        for (SKOSLabel label : labels) {
            if (label.getProperty() == propertie && label.getLanguage().equals(lang)) {
                value = label.getLabel();
                break;
            }
        }
        return value;
    }

    private String getAltLabelValue(List<SKOSLabel> labels, String lang, int propertie) {

        return labels.stream()
                .filter(label -> label.getProperty() == propertie && label.getLanguage().equals(lang))
                .map(label -> label.getLabel())
                .collect(Collectors.joining(delim_multi_datas));
    }

    private void addDocumentationColumns(
            ArrayList<Object> csvRow,
            SKOSResource skosResource,
            List<String> langs,
            int property
    ) {
        for (String lang : langs) {
            csvRow.add(sanitizeCsvNote(getDocumentationValue(skosResource.getDocumentationsList(), lang, property)));
        }
    }

    private String sanitizeCsvNote(String def) {
        return def.replace("amp;", "").replace(";", ",");
    }

    private String getDocumentationValue(ArrayList<SKOSDocumentation> documentations, String lang, int propertie) {

        return documentations.stream()
                .filter(document -> document.getProperty() == propertie && document.getLanguage().equals(lang))
                .map(document -> document.getText())
                .collect(Collectors.joining(delim_multi_datas));
    }

    private String getLatValue(SKOSGPSCoordinates coordinates) {
        if (coordinates != null) {
            if (coordinates.getLat() == null) {
                return "";
            }
            return coordinates.getLat();
        }
        return "";
    }

    private String getLongValue(SKOSGPSCoordinates coordinates) {
        if (coordinates != null) {
            if (coordinates.getLon() == null) {
                return "";
            }
            return coordinates.getLon();
        }
        return "";
    }

    private String getMemberValue(ArrayList<SKOSRelation> sKOSRelations) {
        return sKOSRelations.stream()
                .filter(sKOSRelation -> (sKOSRelation.getProperty() == SKOSProperty.MEMBER_OF) || (sKOSRelation.getProperty() == SKOSProperty.MEMBER))
                .map(sKOSRelation -> sKOSRelation.getTargetUri())
                .collect(Collectors.joining(delim_multi_datas));
    }   
    
    private String getMemberId(ArrayList<SKOSRelation> sKOSRelations) {
        return sKOSRelations.stream()
                .filter(sKOSRelation -> (sKOSRelation.getProperty() == SKOSProperty.MEMBER_OF) || (sKOSRelation.getProperty() == SKOSProperty.MEMBER))
                .map(sKOSRelation -> sKOSRelation.getLocalIdentifier())
                .collect(Collectors.joining(delim_multi_datas));
    }      
    

    private String getRelationGivenValue(List<SKOSRelation> relations, int propertie) {
        return relations.stream()
                .filter(relation -> relation.getProperty() == propertie)
                .map(relation -> relation.getTargetUri())
                .collect(Collectors.joining(delim_multi_datas));
    }

    private String getRelationGivenValueId(List<SKOSRelation> relations, int propertie) {
        return relations.stream()
                .filter(relation -> relation.getProperty() == propertie)
                .map(relation -> relation.getLocalIdentifier())
                .collect(Collectors.joining(delim_multi_datas));
    }

    private String getAlligementValue(List<SKOSMatch> matchs, int propertie) {
        return matchs.stream()
                .filter(alignment -> alignment.getProperty() == propertie)
                .map(alignment -> alignment.getValue())
                .collect(Collectors.joining(delim_multi_datas));
    }

    private String getGpsValue(List<SKOSGPSCoordinates> gpsList) {
        if (CollectionUtils.isNotEmpty(gpsList)) {
            StringBuilder resultat = new StringBuilder();
            for (SKOSGPSCoordinates gps : gpsList) {
                resultat.append(gps.toString()).append(", ");
            }
            return "(" + resultat.substring(0, resultat.length() - 2) + ")";
        } else {
            return "";
        }
    }

    public String getNotation(List<SKOSNotation> notations) {
        if (!CollectionUtils.isEmpty(notations)) {
            return notations.get(0).getNotation();
        }
        return "";
    }

    private String getDateValue(List<SKOSDate> dates, int propertie) {
        String value = "";
        for (SKOSDate date : dates) {
            if (date.getProperty() == propertie) {
                value = date.getDate();
                break;
            }
        }
        return value;
    }
    
    /**
     * Export des données limitées en CSV
     *
     * @param idTheso
     * @param idLang
     * @param idGroups
     * @param delimiter
     * @return
     */
    public byte[] writeCsvById(String idTheso, String idLang, List<String> idGroups, char delimiter) {
        return writeCsvById(idTheso, idLang, idGroups, delimiter, null);
    }

    public byte[] writeCsvById(
            String idTheso,
            String idLang,
            List<String> idGroups,
            char delimiter,
            Collection<String> restrictConceptIds
    ) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (OutputStreamWriter out = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                 CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.RFC4180.builder().setDelimiter(delimiter).build())) {

                ArrayList<String> header = new ArrayList<>();
                header.add("conceptId");
                header.add(COL_ARK_ID);
                header.add("handleId");
                header.add("prefLabel");
                header.add("altLabel");
                header.add("definition");
                header.add("alignment");
                csvFilePrinter.printRecord(header);

                List<String> idConcepts = csvExportQuerySupport.listConceptIds(idTheso, idGroups);
                if (restrictConceptIds != null && !restrictConceptIds.isEmpty()) {
                    Set<String> keep = new HashSet<>(restrictConceptIds);
                    idConcepts = idConcepts.stream().filter(keep::contains).toList();
                }
                List<ThesaurusCsvByIdRow> conceptRows = csvExportQuerySupport.loadConceptsForCsvById(idTheso, idLang, idConcepts);

                ArrayList<Object> csvRow = new ArrayList<>();
                for (ThesaurusCsvByIdRow conceptRow : conceptRows) {
                    appendCsvByIdRow(csvRow, csvFilePrinter, conceptRow);
                }
            }
            return os.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Export des données des candidats insérés ou refusés en CSV* 
     *
     * @param candidatDtos
     * @param delimiter
     * @return
     */
    public byte[] writeProcessedCandidates( List<CandidatDto> candidatDtos, char delimiter) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (OutputStreamWriter out = new OutputStreamWriter(os, StandardCharsets.UTF_8); CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.RFC4180.builder().setDelimiter(delimiter).build())) {
                /// écriture des headers
                ArrayList<String> header = new ArrayList<>();
                header.add("Id");
                header.add("Candidat");
                header.add("Créé par");
                header.add("Date de création");
                header.add("Traité par");
                header.add("Date de traitement");
                header.add("Message de l'admin");                
                header.add("Votes");
                header.add("Votes de notes");
                header.add("Nombre de participants");
                
                csvFilePrinter.printRecord(header);

                if (candidatDtos == null || candidatDtos.isEmpty()) {
                    return null;
                }

                /// écritures des données
                ArrayList<Object> csvRow = new ArrayList<>();
                for (CandidatDto candidatDto : candidatDtos) {
                    try {
                        csvRow.add(candidatDto.getIdConcepte());
                        csvRow.add(candidatDto.getNomPref());
                        csvRow.add(candidatDto.getCreatedBy());
                        csvRow.add(candidatDto.getCreationDate());
                        csvRow.add(candidatDto.getCreatedByAdmin());
                        csvRow.add(candidatDto.getInsertionDate());
                        csvRow.add(candidatDto.getAdminMessage());
                        csvRow.add(candidatDto.getNbrVote());
                        csvRow.add(candidatDto.getNbrNoteVote());
                        csvRow.add(candidatDto.getNbrParticipant());

                        csvFilePrinter.printRecord(csvRow);
                        csvRow.clear();
                    } catch (IOException e) {
                        log.warn(CSV_EXPORT_ERROR, e);
                    }
                }
            }
            return os.toByteArray();
        } catch (IOException e) {
            log.warn(CSV_EXPORT_ERROR, e);
            return null;
        }
    }

    /**
     * Export des données Id valeur en CSV
     *
     * @param nodeIdValues
     * @param header1
     * @param header2
     * @return
     */
    public byte[] writeCsvResultProcess(List<NodeIdValue> nodeIdValues, String header1, String header2) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (OutputStreamWriter out = new OutputStreamWriter(os, StandardCharsets.UTF_8); CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.RFC4180.builder().build())) {

                /// écriture des headers
                ArrayList<String> header = new ArrayList<>();
                header.add(header1);
                header.add(header2);
                csvFilePrinter.printRecord(header);

                ArrayList<Object> csvRow = new ArrayList<>();
                for (NodeIdValue nodeIdValue : nodeIdValues) {
                    try {
                        csvRow.add(nodeIdValue.getId());
                        csvRow.add(nodeIdValue.getValue());
                        csvFilePrinter.printRecord(csvRow);
                        csvRow.clear();
                    } catch (IOException e) {
                        log.warn(CSV_EXPORT_ERROR, e);
                    }
                }
            }
            return os.toByteArray();
        } catch (IOException e) {
            log.warn(CSV_EXPORT_ERROR, e);
            return null;
        }
    }

    /**
     * Export des données Id valeur en CSV
     *
     * @param listAlignments
     * @param alignmentSource
     * @return
     */
    public byte[] writeCsvForAlignment(ArrayList<NodeIdValue> listAlignments, String alignmentSource) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (OutputStreamWriter out = new OutputStreamWriter(os, StandardCharsets.UTF_8); CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.RFC4180.builder().build())) {

                /// écriture des headers
                ArrayList<String> header = new ArrayList<>();
                header.add("localId");
                header.add("URI");

                csvFilePrinter.printRecord(header);
                ArrayList<Object> csvRow = new ArrayList<>();
                try {
                    for (NodeIdValue listAlignment : listAlignments) {
                        if(StringUtils.containsIgnoreCase(listAlignment.getValue(), "." + alignmentSource + ".")) {
                            csvRow.add(0, listAlignment.getId());
                            csvRow.add(1, listAlignment.getValue());
                            csvFilePrinter.printRecord(csvRow);
                            csvRow.clear();
                        }
                    }
                } catch (IOException e) {
                    log.warn(CSV_EXPORT_ERROR, e);
                }

            }
            return os.toByteArray();
        } catch (IOException e) {
            log.warn(CSV_EXPORT_ERROR, e);
            return null;
        }
    }

    /**
     * Export des données Id valeur en CSV
     *
     * @param nodeCompareThesos
     * @param idLang
     * @return
     */
    public byte[] writeCsvFromNodeCompareTheso(List<NodeCompareTheso> nodeCompareThesos, String idLang) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (OutputStreamWriter out = new OutputStreamWriter(os, StandardCharsets.UTF_8); CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.RFC4180.builder().build())) {

                /// écriture des headers
                ArrayList<String> header = new ArrayList<>();
                header.add("originalPrefLabel@" + idLang);
                header.add("conceptId");
                header.add(COL_ARK_ID);
                header.add("skos:prefLabel@" + idLang);
                header.add("skos:altLabel@" + idLang);

                csvFilePrinter.printRecord(header);
                ArrayList<Object> csvRow = new ArrayList<>();
                for (NodeCompareTheso nodeCompareTheso : nodeCompareThesos) {
                    try {
                        csvRow.add(nodeCompareTheso.getOriginalPrefLabel());
                        csvRow.add(nodeCompareTheso.getIdConcept());
                        csvRow.add(nodeCompareTheso.getIdArk());
                        csvRow.add(nodeCompareTheso.getPrefLabel());
                        csvRow.add(nodeCompareTheso.getAltLabel());
                        csvFilePrinter.printRecord(csvRow);
                        csvRow.clear();
                    } catch (IOException e) {
                        log.warn(CSV_EXPORT_ERROR, e);
                    }
                }
            }
            return os.toByteArray();
        } catch (IOException e) {
            log.warn(CSV_EXPORT_ERROR, e);
            return null;
        }
    }

    /**
     * permet d'exporter les concepts dépréciés
     *
     * @param ds
     * @param idTheso
     * @param idLang
     * @param delimiter
     * @return
     */
    public byte[] writeCsvByDeprecated(String idTheso, String idLang, char delimiter) {
        return writeCsvByDeprecated(idTheso, idLang, delimiter, null);
    }

    public byte[] writeCsvByDeprecated(
            String idTheso,
            String idLang,
            char delimiter,
            Collection<String> restrictConceptIds
    ) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try (OutputStreamWriter out = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                 CSVPrinter csvFilePrinter = new CSVPrinter(out, CSVFormat.RFC4180.builder().setDelimiter(delimiter).build())) {

                ArrayList<String> header = new ArrayList<>();
                header.add("deprecatedId");
                header.add("deprecatedLabel");
                header.add("replacedBy");
                header.add("replacedByLabel");
                header.add("lastModification");
                header.add("userName");
                csvFilePrinter.printRecord(header);

                var nodeDeprecateds = csvExportQuerySupport.listDeprecatedConcepts(idTheso, idLang);
                Set<String> keep = restrictConceptIds == null || restrictConceptIds.isEmpty()
                        ? null
                        : new HashSet<>(restrictConceptIds);
                ArrayList<Object> csvRow = new ArrayList<>();
                for (NodeDeprecated nodeDeprecated : nodeDeprecateds) {
                    if (keep != null && !keep.contains(nodeDeprecated.getDeprecatedId())) {
                        continue;
                    }
                    csvRow.add(nodeDeprecated.getDeprecatedId());
                    csvRow.add(nodeDeprecated.getDeprecatedLabel());
                    csvRow.add(nodeDeprecated.getReplacedById());
                    csvRow.add(nodeDeprecated.getReplacedByLabel());
                    csvRow.add(nodeDeprecated.getModified());
                    csvRow.add(nodeDeprecated.getUserName());
                    csvFilePrinter.printRecord(csvRow);
                    csvRow.clear();
                }
            }
            return os.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private void appendCsvByIdRow(ArrayList<Object> csvRow, CSVPrinter csvFilePrinter, ThesaurusCsvByIdRow conceptRow)
            throws IOException {
        csvRow.add(conceptRow.conceptId());
        csvRow.add(conceptRow.arkId());
        csvRow.add(conceptRow.handleId());
        csvRow.add(conceptRow.prefLabel());
        csvRow.add(joinValues(conceptRow.altLabels()));
        csvRow.add(joinValues(conceptRow.definitions()));
        csvRow.add(joinAlignments(conceptRow.alignments()));
        csvFilePrinter.printRecord(csvRow);
        csvRow.clear();
    }

    private String joinValues(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return "";
        }
        return String.join(delim_multi_datas, values);
    }

    private String joinAlignments(List<ThesaurusCsvAlignmentRow> alignments) {
        if (CollectionUtils.isEmpty(alignments)) {
            return "";
        }
        return alignments.stream()
                .map(alignment -> alignment.typeLabel() + ":" + alignment.uri())
                .collect(Collectors.joining(delim_multi_datas));
    }

    public byte[] importTreeCsv(String[][] tab, char seperate) {
        try ( ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var writer = new BufferedWriter(new OutputStreamWriter(output));
            for (int ii = 0; ii < tab.length; ii++) {
                StringBuilder stringBuffer = new StringBuilder();
                for (int jj = 0; jj < tab[ii].length; jj++) {
                    if (StringUtils.isNotEmpty(tab[ii][jj])) {
                        stringBuffer.append(tab[ii][jj]).append(seperate);
                    } else {
                        stringBuffer.append("").append(seperate);
                    }
                }
                writer.write(stringBuffer.toString());
                writer.newLine();
            }

            writer.close();
            return output.toByteArray();
        } catch (IOException ex) {
            return null;
        }
    }
}
