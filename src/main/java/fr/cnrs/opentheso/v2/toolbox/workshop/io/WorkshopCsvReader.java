package fr.cnrs.opentheso.v2.toolbox.workshop.io;

import fr.cnrs.opentheso.models.alignment.NodeAlignmentImport;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentSmall;
import fr.cnrs.opentheso.models.concept.NodeCompareTheso;
import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.relations.NodeReplaceValueByValue;
import fr.cnrs.opentheso.models.notes.NodeNote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.logging.Level;

import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.utils.ToolsHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author miled.rousset
 */
@Data
@Slf4j
public class WorkshopCsvReader {

    private String message = "";
    private char delimiter = ',';

    private ArrayList<String> langs;
    private ArrayList<String> customRelations;    
    private String idLang;

    private ArrayList<ConceptObject> conceptObjects;

    private ArrayList<NodeAlignmentImport> nodeAlignmentImports;
    private ArrayList<NodeNote> nodeNotes;
    private ArrayList<NodeIdValue> nodeIdValues;
    private ArrayList<NodeCompareTheso> nodeCompareThesos;    
    
    
    private ArrayList<NodeDeprecated> nodeDeprecateds;

    private ArrayList<NodeReplaceValueByValue> nodeReplaceValueByValues;    
    
    public WorkshopCsvReader(char delimiter) {
        this.delimiter = delimiter;
        conceptObjects = new ArrayList<>();
    }
        
    public boolean readFileCsvForGetIdFromPrefLabelSetLang(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader()
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();

            CSVParser cSVParser = cSVFormat.parse(in);
           
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            if(headers.keySet().size()>1) {
                message = "Erreur, Une seule colonne est autorisée";
                return false;
            }
            String values[];
            idLang = null;
            for (String columnName : headers.keySet()) {
                if (columnName.contains("@")) {
                    values = columnName.split("@");
                    if (values[1] != null) {
                        idLang = values[1];
                    }
                } else {
                    message = "Erreur, La langue doit être précisée exemple : skos:prefLabel@fr";
                    return false;                    
                }
            }
            if(idLang == null){
                message = "Erreur, La langue n'a pas été trouvée";
                return false;                  
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }              
    
    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
    public boolean readFileCsvDeprecateConcepts(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();

            CSVParser cSVParser = cSVFormat.parse(in);
            String lang= "fr";
            Map<String, Integer> headers = cSVParser.getHeaderMap();
            String values[];
            for (String columnName : headers.keySet()) {
                if (columnName.contains("@")) {
                    values = columnName.split("@");
                    if (values[1] != null) {
                        lang = values[1];
                    }
                }
            }
            
            String value;
            nodeDeprecateds = new ArrayList<>();
            for (CSVRecord record : cSVParser) {
                NodeDeprecated nodeDeprecated = new NodeDeprecated();
                try {
                    value = record.get("deprecated");
                    if (value == null) {
                        continue;
                    }
                    nodeDeprecated.setDeprecatedId(value);
                } catch (Exception e) {
                    continue;
                }
                try {
                    value = record.get("isReplacedBy");
                    if (value == null) {
                    } else {
                        nodeDeprecated.setReplacedById(value);
                    }
                } catch (Exception e) {
                }
                try {
                    value = record.get("skos:note@" + lang);
                    if (value == null) {
                    } else {
                        nodeDeprecated.setNote(value);
                        nodeDeprecated.setNoteLang(lang);
                    }
                } catch (Exception e) {
                }                
                nodeDeprecateds.add(nodeDeprecated);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }         
    
    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
    public boolean readFileCsvForGetIdFromPrefLabel(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader()
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();

            CSVParser cSVParser = cSVFormat.parse(in);
            String value;
            nodeCompareThesos = new ArrayList<>();
            for (CSVRecord record : cSVParser) {
                NodeCompareTheso nodeCompareTheso = new NodeCompareTheso();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = record.get("skos:prefLabel@" + idLang);
                    if (value == null) {
                        continue;
                    }
                    nodeCompareTheso.setOriginalPrefLabel(value);
                } catch (Exception e) {
                    continue;
                }
                nodeCompareThesos.add(nodeCompareTheso);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
    public boolean readFileIdentifier(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader()
                    .setIgnoreEmptyLines(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser cSVParser = cSVFormat.parse(in);
            nodeIdValues = new ArrayList<>();
            Set<String> uniqueIds = new HashSet<>(); // pour éviter les doublons sur l'id

            for (CSVRecord record : cSVParser) {
                String id;
                try {
                    id = record.get("identifier");
                    if (id == null || id.isEmpty()) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }

                // Vérification doublon
                if (!uniqueIds.contains(id)) {
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId(id);
                    nodeIdValues.add(nodeIdValue);
                    uniqueIds.add(id); // marque comme déjà ajouté
                }
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        return false;
    }

     /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
    public boolean readFileConceptId(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader()
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);
            String value;
            nodeIdValues = new ArrayList<>();
            for (CSVRecord record : cSVParser) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL
                try {
                    value = record.get("localid");
                    if (value == null) {
                        continue;
                    }
                    nodeIdValue.setId(value);
                } catch (Exception e) {
                    continue;
                }
                nodeIdValues.add(nodeIdValue);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
    public boolean readFileArk(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();

            CSVParser cSVParser = cSVFormat.parse(in);
            String value;
            nodeIdValues = new ArrayList<>();
            for (CSVRecord record : cSVParser) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = record.get("localid");
                    if (value == null) {
                        continue;
                    }
                    nodeIdValue.setId(value);
                } catch (Exception e) {
                    continue;
                }
                // on récupère les uris à supprimer
                try {
                    value = record.get("arkId");
                    if (value == null) {
                        continue;
                    }
                    nodeIdValue.setValue(value.trim());
                } catch (Exception e) {
                    continue;
                }
                nodeIdValues.add(nodeIdValue);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
/*    public boolean readFileAlignmentToDelete(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);

            String value;
            for (CSVRecord record : cSVParser) {
                ConceptObject conceptObject = new ConceptObject();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = record.get("localid");
                    if (value == null) {
                        continue;
                    }
                    conceptObject.setLocalId(value);
                } catch (Exception e) {
                    continue;
                }
                // on récupère les uris à supprimer
                try {
                    value = record.get("Uri");
                    if (value == null) {
                        continue;
                    }
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId("");
                    nodeIdValue.setValue(value.trim());
                    conceptObject.alignments.add(nodeIdValue);
                } catch (Exception e) {
                    continue;
                }

                conceptObjects.add(conceptObject);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }*/

    public boolean readFileAlignmentToDelete(Reader in) {
        conceptObjects = new ArrayList<>();

        try {
            // Nettoyer le flux au cas où il contient un BOM
            BufferedReader br = new BufferedReader(in);
            br.mark(1);
            if (br.read() != '\uFEFF') {
                br.reset(); // pas de BOM, on revient au début
            }

            // Construction du format CSV
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader() // utilise la première ligne comme header
                    .setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser csvParser = csvFormat.parse(br);

            for (CSVRecord record : csvParser) {
                ConceptObject conceptObject = new ConceptObject();

                // Lecture du localId (gestion de la casse et BOM)
                String localId = getSafe(record, "localId");
                if (localId == null || localId.isEmpty()) {
                    localId = getSafe(record, "localid"); // fallback
                }

                if (localId == null || localId.isEmpty()) {
                    continue;
                }
                conceptObject.setLocalId(localId.trim());

                // Lecture de l'URI à supprimer
                String uri = getSafe(record, "Uri");
                if (uri == null || uri.isEmpty()) {
                    uri = getSafe(record, "uri");
                }

                if (uri != null && !uri.isEmpty()) {
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId("");
                    nodeIdValue.setValue(uri.trim());
                    conceptObject.alignments.add(nodeIdValue);
                }

                conceptObjects.add(conceptObject);
            }

            return true;

        } catch (IOException ex) {
            log.error(WorkshopCsvReader.class.getName() +  ex.getMessage());
            message = "Erreur lors de la lecture du fichier CSV : " + ex.getMessage();
            return false;
        }
    }
    /**
     * Récupère la valeur d'une colonne de façon sécurisée,
     * en gérant les BOM éventuels dans le nom de colonne.
     */
    private String getSafe(CSVRecord record, String header) {
        try {
            // Essai direct
            if (record.isMapped(header)) {
                return record.get(header);
            }
            // Essai avec un éventuel BOM
            String withBom = "\uFEFF" + header;
            if (record.isMapped(withBom)) {
                return record.get(withBom);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
    public boolean readFileImage(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);
            String value;
            for (CSVRecord record : cSVParser) {
                ConceptObject conceptObject = new ConceptObject();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = record.get("localid");
                    if (StringUtils.isEmpty(value)) {
                        continue;
                    }
                    conceptObject.setLocalId(value);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les images 
                conceptObject = getImages(conceptObject, record);

                if (conceptObject != null) {
                    conceptObjects.add(conceptObject);
                }
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
    public boolean readFileNotation(Reader in) {
        try {
            // Configuration du format CSV
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            // Lecture du fichier CSV
            try (CSVParser csvParser = csvFormat.parse(in)) {
                nodeIdValues = new ArrayList<>();

                // Parcourir les enregistrements CSV
                for (CSVRecord record : csvParser) {
                    NodeIdValue nodeIdValue = new NodeIdValue();

                    // Récupérer l'identifiant "localId"
                    String value = record.get("localid");
                    if (value == null || value.isBlank()) {
                        continue; // Si vide, passer à l'enregistrement suivant
                    }
                    nodeIdValue.setId(value);

                    // Récupérer la notation "skos:notation"
                    value = record.get("skos:notation");
                    if (value == null || value.isBlank()) {
                        continue; // Si vide, passer à l'enregistrement suivant
                    }
                    nodeIdValue.setValue(value.trim());

                    // Ajouter l'objet à la liste
                    nodeIdValues.add(nodeIdValue);
                }
            }
            return true;

        } catch (IOException ex) {
                log.error("Error reading CSV file", ex);
        } catch (IllegalArgumentException ex) {
            log.error("CSV file missing required headers", ex);
        }
        return false;

    }    

    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @return
     */
    public boolean readFileCollection(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();

            CSVParser cSVParser = cSVFormat.parse(in);
            String value;
            nodeIdValues = new ArrayList<>();
            for (CSVRecord record : cSVParser) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = record.get("localid");
                    if (value == null) {
                        continue;
                    }
                    nodeIdValue.setId(value);
                } catch (Exception e) {
                    continue;
                }
                // on récupère les ids des collections à ajouter au concept
                try {
                    value = record.get("skos:member");
                    if (value == null) {
                        continue;
                    }
                    nodeIdValue.setValue(value.trim());
                } catch (Exception e) {
                    continue;
                }
                nodeIdValues.add(nodeIdValue);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }       
    
    public ArrayList<String > readHeadersFileAlignment (Reader in){
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            ArrayList<String> headerSourceAlign = new ArrayList<>();
            for (String columnName : headers.keySet()) {
                if (columnName.equalsIgnoreCase("localid")) {
                    continue;
                }
                headerSourceAlign.add(columnName);
            }
            return headerSourceAlign;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;        
    }

    public ArrayList<String > readHeadersFileRelated (Reader in){
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            ArrayList<String> headersRelated = new ArrayList<>();
            for (String columnName : headers.keySet()) {
                headersRelated.add(columnName);
            }
            return headersRelated;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * permet de lire un fichier CSV complet pour importer les RT
     *
     * @param in
     * @param headerSourceAlign
     * @return
     */
    public boolean readFileRelated(Reader in, ArrayList<String> headerSourceAlign) {
        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(in);

            // id -> valeurs related uniques
            Map<String, Set<String>> relatedById = new HashMap<>();

            for (CSVRecord record : parser) {

                String id;
                String related;

                try {
                    id = record.get("localid");
                    related = record.get("skos:related");
                } catch (Exception e) {
                    continue;
                }

                if (StringUtils.isBlank(id) || StringUtils.isBlank(related)) {
                    continue;
                }

                relatedById
                        .computeIfAbsent(id, k -> new HashSet<>())
                        .add(related);
            }

            // Si tu as besoin d'une structure finale plate (id, value)
            nodeIdValues = new ArrayList<>();
            relatedById.forEach((id, values) ->
                    values.forEach(value ->
                            nodeIdValues.add(new NodeIdValue(id, value))
                    )
            );

            return true;

        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
        return false;
    }


    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @param headerSourceAlign
     * @return
     */
    public boolean readFileAlignment(Reader in, ArrayList<String> headerSourceAlign) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);
            String value;
            if (nodeAlignmentImports == null) {
                nodeAlignmentImports = new ArrayList<>();
            } else {
                nodeAlignmentImports.clear();
            }
            for (CSVRecord record : cSVParser) {
                NodeAlignmentImport nodeAlignmentImport = new NodeAlignmentImport();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = record.get("localid");
                    if (value == null) {
                        continue;
                    }
                    nodeAlignmentImport.setLocalId(value);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les alignements 
                nodeAlignmentImport = getNewAlignment(nodeAlignmentImport, record, headerSourceAlign);
                if (nodeAlignmentImport != null) {
                    nodeAlignmentImports.add(nodeAlignmentImport);
                }
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private NodeAlignmentImport getNewAlignment(
            NodeAlignmentImport nodeAlignmentImport,
            CSVRecord record, ArrayList<String> headerSourceAlign) {
        String uri1;
        ToolsHelper toolsHelper = new ToolsHelper();

        /// types alignements 1=exactMatch ; 2=closeMatch ; 3=broadMatch ; 4=relatedMatch ; 5=narrowMatch
        for (String alignSource : headerSourceAlign) {
            try {
                uri1 = record.get(alignSource);
                if(toolsHelper.isValidURI(uri1) == false) {
                    log.error("Erreur lors de la lecture du fichier CSV : l'URI " + uri1 + " n'est pas valide.");
                    message = "Erreur lors de la lecture du fichier CSV : l'URI " + uri1 + " n'est pas valide.";
                    return null;
                }
                nodeAlignmentImport = getAlignmentSource(nodeAlignmentImport, alignSource, uri1);
            } catch (Exception e) {
            }            
        }
        return nodeAlignmentImport;
    }

    private NodeAlignmentImport getAlignmentSource(NodeAlignmentImport nodeAlignmentImport, String source, String uri) {
        String[] valueType;
        /// types alignements 1=exactMatch ; 2=closeMatch ; 3=broadMatch ; 4=relatedMatch ; 5=narrowMatch
        try {
            if (source != null && !source.isEmpty()) {
                NodeAlignmentSmall nodeAlignmentSmall = new NodeAlignmentSmall();
                nodeAlignmentSmall.setSource(source);

                //on récupère le type d'alignement (url##1)
                if (uri.contains("##")) {
                    valueType = uri.split("##");
                    if (valueType.length == 2) {
                        nodeAlignmentSmall.setUri_target(valueType[0]);
                        try {
                            nodeAlignmentSmall.setAlignement_id_type(Integer.parseInt(valueType[1]));
                        } catch (Exception e) {
                            nodeAlignmentSmall.setAlignement_id_type(1);
                        }
                    } else {
                        nodeAlignmentSmall.setUri_target(uri);
                        nodeAlignmentSmall.setAlignement_id_type(1);
                    }
                } else {
                    nodeAlignmentSmall.setUri_target(uri);
                    nodeAlignmentSmall.setAlignement_id_type(1);
                }
                nodeAlignmentImport.getNodeAlignmentSmalls().add(nodeAlignmentSmall);
                return nodeAlignmentImport;
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    /**
     * permet de lire un fichier CSV complet pour importer les altLabels avec option
     * de vider les notes avant
     *
     * @param in
     * @return
     */
    public boolean readFileAltlabel(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);

            String idConcept;
            for (CSVRecord record : cSVParser) {
                ConceptObject conceptObject = new ConceptObject();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    idConcept = record.get("localid");
                    if (idConcept == null || idConcept.isEmpty()) {
                        continue;
                    }
                    conceptObject.setIdConcept(idConcept);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les labels
                conceptObject = getLabels(conceptObject, record, false);

                if (conceptObject != null) {
                    conceptObjects.add(conceptObject);
                }
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * permet de lire un fichier CSV complet pour importer les notes avec option
     * de vider les notes avant
     *
     * @param in
     * @return
     */
    public boolean readFileTraduction(Reader in, String lang) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);

            String idConcept;
            nodeIdValues = new ArrayList<>();

            for (CSVRecord record : cSVParser) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL
                try {
                    idConcept = record.get("localid");
                    if (idConcept == null || idConcept.isEmpty()) {
                        continue;
                    }
                    nodeIdValue.setId(idConcept);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les labels
                nodeIdValue = getPrefLabel(nodeIdValue, record, lang);

                if (nodeIdValue != null) {
                    nodeIdValues.add(nodeIdValue);
                }
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private NodeIdValue getPrefLabel(NodeIdValue nodeIdValue, CSVRecord record, String lang) {
        String value;
        try {
            value = record.get("skos:prefLabel@" + lang.trim());
            if(StringUtils.isNotEmpty(value)) {
                nodeIdValue.setValue(value);
                return nodeIdValue;
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * permet de lire un fichier CSV complet pour importer les notes avec option
     * de vider les notes avant
     *
     * @param in
     * @return
     */
    public boolean readFileNote(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);

            String idConcept;
            for (CSVRecord record : cSVParser) {
                ConceptObject conceptObject = new ConceptObject();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    idConcept = record.get("localid");
                    if (idConcept == null || idConcept.isEmpty()) {
                        continue;
                    }
                    conceptObject.setIdConcept(idConcept);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les notes 
                conceptObject = getNotes(conceptObject, record, false);

                if (conceptObject != null) {
                    conceptObjects.add(conceptObject);
                }
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * permet de lire un fichier CSV complet pour récupérer données
     * pour la valeur à remplacer par la nouvelle valeur 
     *
     * @param in
     * @param usedLangs
     * @return
     */
    public boolean readFileReplaceValueByNewValue(Reader in, List<String> usedLangs) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);

            String idConcept;
            nodeReplaceValueByValues = new ArrayList<>();
            for (CSVRecord record : cSVParser) {
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    idConcept = record.get("localid");
                    if (idConcept == null || idConcept.isEmpty()) {
                        continue;
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    continue;
                }                
                for (String idLang1 : usedLangs) {
                    NodeReplaceValueByValue nodeReplaceValueByValue = new NodeReplaceValueByValue();
                    // on récupère les prefLabels 
                    nodeReplaceValueByValue = getValueAndPropertyPrefLabel(nodeReplaceValueByValue, record, idLang1);     
                    if (nodeReplaceValueByValue != null) {
                        nodeReplaceValueByValue.setIdConcept(idConcept);
                        nodeReplaceValueByValues.add(nodeReplaceValueByValue);
                    }
                    NodeReplaceValueByValue nodeReplaceValueByValue2 = new NodeReplaceValueByValue();
                    // on récupère les altLabels 
                    nodeReplaceValueByValue2 = getValueAndPropertyAltLabel(nodeReplaceValueByValue2, record, idLang1);     
                    if (nodeReplaceValueByValue2 != null) {
                        nodeReplaceValueByValue2.setIdConcept(idConcept);
                        nodeReplaceValueByValues.add(nodeReplaceValueByValue2);
                    }   
                    NodeReplaceValueByValue nodeReplaceValueByValue3 = new NodeReplaceValueByValue();
                    // on récupère les définitions
                    nodeReplaceValueByValue3 = getValueAndPropertyDefinition(nodeReplaceValueByValue3, record, idLang1);     
                    if (nodeReplaceValueByValue3 != null) {
                        nodeReplaceValueByValue3.setIdConcept(idConcept);
                        nodeReplaceValueByValues.add(nodeReplaceValueByValue3);
                    }                     
                    
                }
                 // on récupère les BTs 
                NodeReplaceValueByValue nodeReplaceValueByValue = new NodeReplaceValueByValue();
                nodeReplaceValueByValue = getValueAndPropertyBT(nodeReplaceValueByValue, record);     
                if (nodeReplaceValueByValue != null) {
                    nodeReplaceValueByValue.setIdConcept(idConcept);
                    nodeReplaceValueByValues.add(nodeReplaceValueByValue);
                }                  
                 
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }    
    
    private NodeReplaceValueByValue getValueAndPropertyPrefLabel(NodeReplaceValueByValue nodeReplaceValueByValue, CSVRecord record,
            String idLang) {
        String value;
        try {
            //récupère les prefLabels
            value = record.get("new_skos:preflabel@"+ idLang);
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setIdLang(idLang);
                nodeReplaceValueByValue.setNewValue(value);
                nodeReplaceValueByValue.setSKOSProperty(SKOSProperty.PREF_LABEL);
            } else {
                return null;
            }
            value = record.get("skos:preflabel@"+ idLang);
            nodeReplaceValueByValue.setOldValue(value);
            return nodeReplaceValueByValue;
        } catch (Exception e) {
            //System.err.println("");
        }      
        return null;
    }
    private NodeReplaceValueByValue getValueAndPropertyAltLabel(NodeReplaceValueByValue nodeReplaceValueByValue, CSVRecord record,
            String idLang) {
        String value;
        try {
            //récupère les altLabels
            value = record.get("new_skos:altLabel@"+ idLang);
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setIdLang(idLang);
                nodeReplaceValueByValue.setNewValue(value);
                nodeReplaceValueByValue.setSKOSProperty(SKOSProperty.ALT_LABEL);
            } else {
                return null;
            }
            try {
                value = record.get("skos:altlabel@"+ idLang);
                nodeReplaceValueByValue.setOldValue(value);
            } catch (Exception e) {
            }
            return nodeReplaceValueByValue;
        } catch (Exception e) {
            //System.err.println("");
        }      
        return null;
    }   
    private NodeReplaceValueByValue getValueAndPropertyDefinition(NodeReplaceValueByValue nodeReplaceValueByValue, CSVRecord record,
            String idLang) {
        String value;
        try {
            //récupère les définitons
            value = record.get("new_skos:definition@"+ idLang);
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setIdLang(idLang);
                nodeReplaceValueByValue.setNewValue(value);
                nodeReplaceValueByValue.setSKOSProperty(SKOSProperty.DEFINITION);
            } else {
                return null;
            }
            try { 
                value = record.get("skos:definition@"+ idLang);
                nodeReplaceValueByValue.setOldValue(value);
            } catch (Exception e) {
            }                
            return nodeReplaceValueByValue;
        } catch (Exception e) {
            //System.err.println("");
        }      
        return null;
    }     
    
    private NodeReplaceValueByValue getValueAndPropertyBT(NodeReplaceValueByValue nodeReplaceValueByValue, CSVRecord record) {
        String value;
        try {
            value = record.get("new_skos:broader");
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setNewValue(value);
                nodeReplaceValueByValue.setSKOSProperty(SKOSProperty.BROADER);
            } else {
                return null;
            }
            value = record.get("skos:broader");
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setOldValue(value);
                return nodeReplaceValueByValue;
            } else {
                nodeReplaceValueByValue.setOldValue(null);
                return nodeReplaceValueByValue;
            }
        } catch (Exception e) {
            //System.err.println("");
        }      
        return null;
    }    
    

////////////////////////////////////////////////////////////////////////////////    
////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
    public boolean setLangs(Reader in) {
        langs = new ArrayList<>();
        customRelations = new ArrayList<>();        
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            String values[];
            for (String columnName : headers.keySet()) {
                if (columnName.contains("@")) {
                    values = columnName.split("@");
                    if (values[1] != null) {
                        if (!langs.contains(values[1])) {
                            langs.add(values[1]);//columnName.substring(columnName.indexOf("@"), columnName.indexOf("@" +2)));
                        }
                    }
                }
                if (columnName.contains("customRelationId")) {
                    values = columnName.split(":");
                    if(values.length < 2) continue;
                    if (values[1] != null) {
                        if (!customRelations.contains(values[1])) {
                            customRelations.add(values[1]);//.toLowerCase());//columnName.substring(columnName.indexOf("@"), columnName.indexOf("@" +2)));
                        }
                    }
                }                
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return !langs.isEmpty();
    }

    public String getLangOfValue(Reader in) {
        String lang = null;
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            String values[];
            for (String columnName : headers.keySet()) {
                if (columnName.contains("@")) {
                    values = columnName.split("@");
                    if (values[1] != null) {
                        lang = values[1];
                    }
                }
            }
            return lang;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * permet de lire une liste en CSV, la première colonne n'est pas
     * obligatoire pour charger une liste de concepts
     *
     * @param in
     * @return
     */
    public boolean readListFile(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);  
            String uri1 = null;
            //          boolean first = true;

            for (CSVRecord record : cSVParser) {
                ConceptObject conceptObject = new ConceptObject();

                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                // puis on génère un nouvel identifiant
                try {
                    uri1 = record.get("URI");
                    conceptObject.setUri(uri1);
                } catch (Exception e) {
                }

                try {
                    uri1 = record.get("identifier");
                } catch (Exception e) {
                }

                try {
                    if (uri1 == null || uri1.isEmpty()) {
                        uri1 = record.get("URI");
                        uri1 = getId(uri1);
                    }
                    conceptObject.setIdConcept(uri1);
                } catch (Exception e) {
                }

                // on récupère l'id Ark s'il existe
                conceptObject = getArkId(conceptObject, record);

                // on récupère les labels
                conceptObject = getLabels(conceptObject, record, false);

                // on récupère les notes
                conceptObject = getNotes(conceptObject, record, false);

                // on récupère le type
                conceptObject.setType(getType(record));

                // on récupère la notation
                conceptObject.setNotation(getNotation(record));

                // on récupère les relations (BT, NT, RT)
                conceptObject = getRelations(conceptObject, record);

                // on récupère les alignements 
                conceptObject = getAlignments(conceptObject, record, false);

                // on récupère les images
                conceptObject = getImages(conceptObject, record);
                
                // on récupère la localisation
                conceptObject = getGps(conceptObject, record);
                conceptObject = getGeoLocalisation(conceptObject, record, false);

                // on récupère les membres (l'appartenance du concept à un groupe, collection ...
                conceptObject = getMembers(conceptObject, record);

                // on récupère la date
                conceptObject = getDates(conceptObject, record);

                // on récupère l'appartenance du concept à une facette
                conceptObject = getMemberOfFacet(conceptObject, record);

                conceptObjects.add(conceptObject);
                uri1 = null;
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }    
    
    /**
     * permet de lire un fichier CSV complet pour charger un thésaurus
     *
     * @param in
     * @param readEmptyData
     * @return
     */
    public boolean readFile(Reader in, boolean readEmptyData) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);            

            String uri1 = null;
            String uri_forId;
            for (CSVRecord record : cSVParser) {
                ConceptObject conceptObject = new ConceptObject();

                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                // puis on génère un nouvel identifiant
                try {
                    uri1 = record.get("URI");
                    conceptObject.setUri(uri1);
                } catch (Exception e) {
                }

                if (record.isMapped("identifier")) {
                    try {
                        uri_forId = record.get("identifier");
                        if (uri_forId == null || uri_forId.isEmpty()) {
                            conceptObject.setIdConcept(getId(uri1));
                        } else {
                            conceptObject.setIdConcept(uri_forId);
                        }
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        uri_forId = record.get("URI");
                        conceptObject.setIdConcept(getId(uri_forId));
                    } catch (Exception e) {
                    }
                }
                if(StringUtils.isEmpty(conceptObject.getIdConcept())){
                    message = message + "\n" + "concept sans Id : " + record.toString();
                    continue;
                }

                // on récupère l'id Ark s'il existe
                conceptObject = getArkId(conceptObject, record);

                // on récupère les labels
                conceptObject = getLabels(conceptObject, record, readEmptyData);

                // on récupère les notes
                conceptObject = getNotes(conceptObject, record, readEmptyData);

                // on récupère le type de l'enregistrement (concept, collection)
                conceptObject.setType(getType(record));
                
                // on récupérer du concept (People, qualifier ...)
                conceptObject.setConceptType(getConceptType(record));

                // on récupère la notation
                conceptObject.setNotation(getNotation(record));

                // on récupère les relations (BT, NT, RT)
                conceptObject = getRelations(conceptObject, record);

                // on récupère les relations (BT, NT, RT)
                conceptObject = getCustomRelations(conceptObject, record);                
                
                // on récupère les alignements 
                conceptObject = getAlignments(conceptObject, record, readEmptyData);

                // on récupère la localisation
                conceptObject = getGps(conceptObject, record);
                conceptObject = getGeoLocalisation(conceptObject, record, readEmptyData);

                
                
                // on récupère les membres (l'appartenance du concept à un groupe, collection ...
                if("skos:Concept".equalsIgnoreCase(conceptObject.getType())){                
                    conceptObject = getMembers(conceptObject, record);
                }
                if("skos:collection".equalsIgnoreCase(conceptObject.getType())){                
                    conceptObject = getMembers(conceptObject, record);
                }                
                
                // récupération des sous groupes
                if("skos:Collection".equalsIgnoreCase(conceptObject.getType())){
                    conceptObject = getSubGroups(conceptObject, record);
                }
                
                // récupération des membres d'une Facette
                if("skos-thes:ThesaurusArray".equalsIgnoreCase(conceptObject.getType())){
                    conceptObject = getMembersOfFacet(conceptObject, record);
                    
                    // récupération du parent de la facette
                    conceptObject = getSuperOrdinate(conceptObject, record);
                }                
                
                // définir si le concept est déprécié (Obsolète) et s'il a un concept de remplacement 
                if("skos:Concept".equalsIgnoreCase(conceptObject.getType())){
                    conceptObject = setDeprecatedConcept(conceptObject, record);
                }                
                
                // récupération des resources Externes
                conceptObject = getExternalResources(conceptObject, record);
                
                
                // on récupère la date
                conceptObject = getDates(conceptObject, record);
                
                // on récupère les images 
                conceptObject = getFoafImages(conceptObject, record);

                conceptObjects.add(conceptObject);
                uri1 = null;
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WorkshopCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * permet de récupérer les resources externes
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getExternalResources(ConceptObject conceptObject, CSVRecord record) {
        String value;
        String values[];
        
        try {
            value = record.get("dcterms:source");
            values = value.split("##");
            for (String value1 : values) {
                if (!StringUtils.isEmpty(value1)) {
                    conceptObject.externalResources.add(value1.trim());
                }
            }
        } catch (Exception e) {
        }
        return conceptObject;
    }    
    
    /**
     * permet de récupérer les URI des images
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getFoafImages(ConceptObject conceptObject, CSVRecord record) {
        String value;
        String values[];
        
        try {
            value = record.get("foaf:Image");
            values = value.split("##");
            for (String value1 : values) {
                if (!StringUtils.isEmpty(value1)) {
                    conceptObject.images.add(getNodeImage(value1.trim()));
                }
            }
        } catch (Exception e) {
        }
        return conceptObject;
    }
    
    /**
     * Permet de récupérer les URI des images
     */
    private NodeImage getNodeImage(String value) {
        String values[];
        
        NodeImage nodeImage = new NodeImage();
        try {
            values = value.split("@@");
            for (String value1 : values) {
                if (!StringUtils.isEmpty(value1)) {
                    if(StringUtils.startsWith(value1, "rdf:about=")){
                        nodeImage.setUri(StringUtils.substringAfter(value1, "rdf:about="));
                    }
                    if(StringUtils.startsWith(value1, "dcterms:rights=")){
                        nodeImage.setCopyRight(StringUtils.substringAfter(value1, "dcterms:rights="));
                    }
                    if(StringUtils.startsWith(value1, "dcterms:title=")){
                        nodeImage.setImageName(StringUtils.substringAfter(value1, "dcterms:title="));
                    }          
                    if(StringUtils.startsWith(value1, "dcterms:creator=")){
                        nodeImage.setCreator(StringUtils.substringAfter(value1, "dcterms:creator="));
                    }                     
                }
            }
        } catch (Exception e) {
        }
        if(StringUtils.isEmpty(nodeImage.getUri())) return null;
        return nodeImage;
    }   
    
    
    /**
     * permet de savoir si le concept est déprécié et s'il a des concepts de remplacement
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject setDeprecatedConcept(ConceptObject conceptObject, CSVRecord record) {
        String value;
        try {
            value = record.get("owl:deprecated");
            if (!StringUtils.isEmpty(value)) {
                if("true".equalsIgnoreCase(value)) {
                    conceptObject.setDeprecated(true);
                    getReplacedByOfDeprecatedConcept(conceptObject, record);
                }
                else
                    conceptObject.setDeprecated(false);
            }
        } catch (Exception e) {
            //System.err.println("");
        }

        return conceptObject;
    }      
    
    /**
     * permet de récupérer des dcterms:isReplacedBy les concepts de rempalacement
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getReplacedByOfDeprecatedConcept(ConceptObject conceptObject, CSVRecord record) {
        String value;
        String values[];
        try {
            value = record.get("dcterms:isReplacedBy");
            values = value.split("##");
            for (String value1 : values) {
                if (!value1.isEmpty()) {
                    conceptObject.replacedBy.add(getId(value1.trim()));
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }

        return conceptObject;
    }      
    
    /**
     * permet de récupérer le parent de la Facette
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getSuperOrdinate(ConceptObject conceptObject, CSVRecord record) {
        String value;
        if (record.isMapped("superOrdinateId")) {
            try {
                value = record.get("superOrdinateId");
                if (StringUtils.isNotEmpty(value)) {
                    conceptObject.superOrdinate = value.trim();
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        } else {
            try {
                value = record.get("iso-thes:superOrdinate");
                if (StringUtils.isNotEmpty(value)) {
                    conceptObject.superOrdinate = getId(value.trim());
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        }

        return conceptObject;
    }      
    
    
    /**
     * permet de récupérer les concepts qui sont membre de cette Facette
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getMembersOfFacet(ConceptObject conceptObject, CSVRecord record) {
        String value;
        String values[];
        
        if (record.isMapped("memberid")) {
            try {
                value = record.get("memberid");
           //     value = value.toLowerCase();
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.members.add(value1.trim());
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        } else {        
            try {
                value = record.get("skos:member");
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.members.add(getId(value1.trim()));
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        }

        return conceptObject;
    }       
    
    /**
     * permet de récupérer les sous groupes d'un groupe
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getSubGroups(ConceptObject conceptObject, CSVRecord record) {
        String value;
        String values[];
        try {
            value = record.get("iso-thes:subGroup");
            values = value.split("##");
            for (String value1 : values) {
                if (!value1.isEmpty()) {
                    conceptObject.subGroups.add(getId(value1.trim()));
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }

        return conceptObject;
    }    
    
    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getGps(ConceptObject conceptObject, CSVRecord record) {

        try {
            String value = record.get("geo:gps");
            if (!value.isEmpty()) {
                conceptObject.setGps(value.trim());
            }
        } catch (Exception e) {
            //System.err.println("");
        }
        return conceptObject;
    }

    /**
     * permet de récupérer l'identifiant d'près une URI
     *
     * @return
     */
    private String getId(String uri) {
        String id;

    //    uri = uri.toLowerCase();
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        if (uri.contains("idf=")) {
            if (uri.contains("&")) {
                id = uri.substring(uri.indexOf("idf=") + 4, uri.indexOf("&"));
            } else {
                id = uri.substring(uri.indexOf("idf=") + 4, uri.length());
            }
        } else {
            if (uri.contains("idg=")) {
                if (uri.contains("&")) {
                    id = uri.substring(uri.indexOf("idg=") + 4, uri.indexOf("&"));
                } else {
                    id = uri.substring(uri.indexOf("idg=") + 4, uri.length());
                }
            } else {
                if (uri.contains("idc=")) {
                    if (uri.contains("&")) {
                        id = uri.substring(uri.indexOf("idc=") + 4, uri.indexOf("&"));
                    } else {
                        id = uri.substring(uri.indexOf("idc=") + 4, uri.length());
                    }
                } else {
                    if (uri.contains("#")) {
                        id = uri.substring(uri.indexOf("#") + 1, uri.length());
                    } else {
                        if(uri.contains("ark:/")){
                            id = uri.substring(uri.indexOf("ark:/")+5 , uri.length());
                        } else 
                            id = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
                    }
                }
            }
        }
        
        return fr.cnrs.opentheso.utils.StringUtils.normalizeStringForIdentifier(id);
    }

    /**
     * permet de récupérer le type de l'enregistrement (concept, collection, groupe ...)
     *
     * @param record
     * @return
     */
    private String getType(CSVRecord record) {
        String type = "";
        try {
            type = record.get("rdf:type");
        } catch (Exception e) {
            //System.err.println("");
        }
  
        return type.trim().toLowerCase();
    }
    
    /**
     * permet de récupérer le type du concept (People, qualifier, place ...)
     *
     * @param record
     * @return
     */
    private String getConceptType(CSVRecord record) {
        String conceptType = "";
        try {
            conceptType = record.get("dct:type");
        } catch (Exception e) {
            //System.err.println("");
        }
        return conceptType.trim().toLowerCase();
    }    

    /**
     * permet de récupérer la notation du concept
     *
     * @param record
     * @return
     */
    private String getNotation(CSVRecord record) {
        String notation = "";
        try {
            notation = record.get("skos:notation");
        } catch (Exception e) {
            //System.err.println("");
        }
        return notation.trim();
    }

    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getDates(ConceptObject conceptObject, CSVRecord record) {

        // dct:created
        String value;
        try {
            value = record.get("dcterms:created");
            if (!value.isEmpty()) {
                conceptObject.setCreated(value.trim());
            } else {
                value = record.get("dct:created");
                if (!value.isEmpty()) {
                    conceptObject.setCreated(value.trim());
                }
            }
            
        } catch (Exception e) {
        }

        // dct:modified
        try {
            value = record.get("dcterms:modified");
            if (!value.isEmpty()) {
                conceptObject.setModified(value.trim());
            } else {
                value = record.get("dct:modified");
                if (!value.isEmpty()) {
                    conceptObject.setModified(value.trim());
                }                
            }           
        } catch (Exception e) {
        }
        return conceptObject;
    }

    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getMembers(ConceptObject conceptObject, CSVRecord record) {

        String value;
        String values[];
        // skos:member
        if (record.isMapped("memberid")) {        
            try {
                value = record.get("memberid");
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.members.add(value1.trim());
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }            
        } else {
            try {
                value = record.get("skos:member");
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.members.add(getId(value1.trim()));
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        }

        return conceptObject;
    }

    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getMemberOfFacet(ConceptObject conceptObject, CSVRecord record) {

        String value;
        String values[];
        // skos:member
        if (record.isMapped("skos:facet")) {
            try {
                value = record.get("skos:facet");
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.memberOfFacets.add(value1.trim());
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        }
        return conceptObject;
    }


    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getArkId(ConceptObject conceptObject, CSVRecord record) {
        try {
            String arkId = record.get("arkId");
            if (arkId != null) {
                conceptObject.setArkId(arkId.trim());
            }
        } catch (Exception e) {
        }

        return conceptObject;
    }

    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getAlignments(
            ConceptObject conceptObject,
            CSVRecord record, boolean readEmptyData) {
        String value;
        String values[];

        // skos:exactMatch
        try {
            value = record.get("skos:exactMatch");
            values = value.split("##");
            for (String value1 : values) {
                if(readEmptyData){
                    conceptObject.exactMatchs.add(value1.trim());
                } else {
                    if (!value1.isEmpty()) {
                        conceptObject.exactMatchs.add(value1.trim());
                    }                    
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }

        // skos:closeMatch
        try {
            value = record.get("skos:closeMatch");
            values = value.split("##");
            for (String value1 : values) {
                if(readEmptyData){
                    conceptObject.closeMatchs.add(value1.trim());
                } else {
                    if (!value1.isEmpty()) {
                        conceptObject.closeMatchs.add(value1.trim());
                    }
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }
        // skos:broadMatch
        try {
            value = record.get("skos:broadMatch");
            values = value.split("##");
            for (String value1 : values) {
                if(readEmptyData){
                    conceptObject.broadMatchs.add(value1.trim());
                } else {
                    if (!value1.isEmpty()) {
                        conceptObject.broadMatchs.add(value1.trim());
                    }                    
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }
        // skos:narrowMatch
        try {
            value = record.get("skos:narrowMatch");
            values = value.split("##");
            for (String value1 : values) {
                if(readEmptyData){
                    conceptObject.narrowMatchs.add(value1.trim());
                } else {
                    if (!value1.isEmpty()) {
                        conceptObject.narrowMatchs.add(value1.trim());
                    }                    
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }
        // skos:relatedMatch
        try {
            value = record.get("skos:relatedMatch");
            values = value.split("##");
            for (String value1 : values) {
                if(readEmptyData){
                    conceptObject.relatedMatchs.add(value1.trim());
                } else {
                    if (!value1.isEmpty()) {
                        conceptObject.relatedMatchs.add(value1.trim());
                    }                    
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }

        return conceptObject;
    }
    
    /**
     * permet de charger toutes les images d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getImages(
            ConceptObject conceptObject,
            CSVRecord record, boolean readEmptyData) {
        String value;
        String values[];

        // foaf:Image
   /*     
 <foaf:Image rdf:about="http://www.peterparker.com/photos/spiderman/statue.jpg">

   <dc:title>Battle on the Statue Of Liberty</dc:title>

   

   <foaf:depicts rdf:resource="#spiderman"/>

   <foaf:depicts rdf:resource="#green-goblin"/>

   

   <foaf:maker rdf:resource="#peter"/>   

 </foaf:Image>        */
        
        
        
        try {
            value = record.get("foaf:Image");
            values = value.split("##");
            for (String value1 : values) {
                if(readEmptyData){
                    conceptObject.exactMatchs.add(value1.trim());
                } else {
                    if (!value1.isEmpty()) {
                        conceptObject.exactMatchs.add(value1.trim());
                    }                    
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }
        return conceptObject;
    }    
    
    

    /**
     * permet de charger toutes les relations d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getRelations(
            ConceptObject conceptObject,
            CSVRecord record) {
        String value;
        String values[];

        // skos:narrowerId (on vérifie si ce champs est renseigné, on le prend avant skos:narrower pour éviter de découper les identifiants
        // et surtout en cas de fichier avec des uris Ark pour retrouver les bons id internes
        // narrowerId 
        if (record.isMapped("narrowerid")) {
            try {
                value = record.get("narrowerid");
           //     value = value.toLowerCase();
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.narrowers.add(value1.trim());
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        } else {
            // skos:narrower
            try {
                value = record.get("skos:narrower");
            //    value = value.toLowerCase();
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.narrowers.add(getId(value1.trim()));
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        }

        // broaderId        
        if (record.isMapped("broaderid")) {
            try {
                value = record.get("broaderid");
            //    value = value.toLowerCase();
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.broaders.add(value1.trim());
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        } else {
            // skos:broader
            try {
                value = record.get("skos:broader");
            //    value = value.toLowerCase();
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.broaders.add(getId(value1.trim()));
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        }

        if (record.isMapped("relatedid")) {
            // relatedId        
            try {
                value = record.get("relatedid");
            //    value = value.toLowerCase();
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.relateds.add(value1.trim());
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        } else {
            // skos:related
            try {
                value = record.get("skos:related");
            //    value = value.toLowerCase();
                values = value.split("##");
                for (String value1 : values) {
                    if (!value1.isEmpty()) {
                        conceptObject.relateds.add(getId(value1.trim()));
                    }
                }
            } catch (Exception e) {
                //System.err.println("");
            }
        }
        return conceptObject;
    }
    
    /**
     * permet de charger toutes les relations d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getCustomRelations(
            ConceptObject conceptObject,
            CSVRecord record) {
        String value;
        String values[];
        
        // liste des relations personnalisées (Qualifier, Poeple, Place ...) 
        for (String customRelation : customRelations) {
            if (record.isMapped("customRelationId:"+ customRelation )) {
                try {
                    value = record.get("customRelationId:"+ customRelation);
                //    value = value.toLowerCase();
                    values = value.split("##");
                    for (String value1 : values) {
                        if (!value1.isEmpty()) {
                            NodeIdValue nodeIdValue = new NodeIdValue();
                            nodeIdValue.setId(value1.trim());
                            nodeIdValue.setValue(customRelation);
                            conceptObject.customRelations.add(nodeIdValue);
                        }
                    }
                } catch (Exception e) {
                    //System.err.println("");
                }
            }             
        }        

        return conceptObject;
    }    

    /**
     * permet de charger tous les labels d'un concept dans toutes les langues
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getLabels(ConceptObject conceptObject, CSVRecord record, boolean readEmptyData) {
        String value;
        Label label;
        String values[];

        for (String idLang2 : langs) {
            // prefLabel
            try {
                value = record.get("skos:preflabel@" + idLang2.trim());
                if(readEmptyData) {
                    label = new Label();
                    label.setLabel(value);
                    label.setLang(idLang2);
                    conceptObject.prefLabels.add(label);                    
                } else {
                    if (!value.isEmpty()) {
                        label = new Label();
                        label.setLabel(value);
                        label.setLang(idLang2);
                        conceptObject.prefLabels.add(label);
                    }
                }
            } catch (Exception e) {
                System.err.println("");
            }

            // altLabel
            try {
                value = record.get("skos:altLabel@" + idLang2.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang2);
                        conceptObject.altLabels.add(label);                        
                    } else {
                        if (!value.isEmpty()) {
                            label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang2);
                            conceptObject.altLabels.add(label);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("");
            }

            // hiddenLabel
            try {
                value = record.get("skos:hiddenLabel@" + idLang2.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang2);
                        conceptObject.hiddenLabels.add(label);                        
                    } else {
                        if (!value.isEmpty()) {
                            label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang2);
                            conceptObject.hiddenLabels.add(label);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        return conceptObject;
    }

    private ConceptObject getNotes(ConceptObject conceptObject, CSVRecord record, boolean readEmptyData) {

        String value;
        String values[];
        for (String idLang1 : langs) {
            // note
            try {
                value = record.get("skos:note@" + idLang1.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        Label label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang1);
                        conceptObject.note.add(label);                        
                    } else {
                        if (!value1.isEmpty()) {
                            Label label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang1);
                            conceptObject.note.add(label);
                        }
                    }
                }

            } catch (Exception e) {
                //System.err.println("");
            }
            // définition
            try {
                value = record.get("skos:definition@" + idLang1.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        Label label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang1);
                        conceptObject.definitions.add(label);                        
                    } else {                 
                        if (!value1.isEmpty()) {
                            Label label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang1);
                            conceptObject.definitions.add(label);
                        }
                    }
                }

            } catch (Exception e) {
                //System.err.println("");
            }

            // scopeNotes note d'application
            try {
                value = record.get("skos:scopeNote@" + idLang1.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        Label label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang1);
                        conceptObject.scopeNotes.add(label);                        
                    } else {
                        if (!value1.isEmpty()) {
                            Label label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang1);
                            conceptObject.scopeNotes.add(label);
                        }                        
                    }
                }

            } catch (Exception e) {
                //System.err.println("");
            }

            // example
            try {
                value = record.get("skos:example@" + idLang1.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        Label label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang1);
                        conceptObject.examples.add(label);                        
                    } else {
                        if (!value1.isEmpty()) {
                            Label label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang1);
                            conceptObject.examples.add(label);
                        }                        
                    }
                }

            } catch (Exception e) {
                //System.err.println("");
            }

            // historyNotes
            try {
                value = record.get("skos:historyNote@" + idLang1.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        Label label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang1);
                        conceptObject.historyNotes.add(label);                        
                    } else {
                        if (!value1.isEmpty()) {
                            Label label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang1);
                            conceptObject.historyNotes.add(label);
                        }                        
                    }
                }

            } catch (Exception e) {
                //System.err.println("");
            }

            // changeNotes
            try {
                value = record.get("skos:changeNote@" + idLang1.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        Label label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang1);
                        conceptObject.changeNotes.add(label);                        
                    } else {
                        if (!value1.isEmpty()) {
                            Label label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang1);
                            conceptObject.changeNotes.add(label);
                        }                        
                    }
                }

            } catch (Exception e) {
                //System.err.println("");
            }

            // editorialNotes
            try {
                value = record.get("skos:editorialNote@" + idLang1.trim());
                values = value.split("##");
                for (String value1 : values) {
                    if(readEmptyData) {
                        Label label = new Label();
                        label.setLabel(value1);
                        label.setLang(idLang1);
                        conceptObject.editorialNotes.add(label);                        
                    } else {
                        if (!value1.isEmpty()) {
                            Label label = new Label();
                            label.setLabel(value1);
                            label.setLang(idLang1);
                            conceptObject.editorialNotes.add(label);
                        }                        
                    }
                }

            } catch (Exception e) {
                //System.err.println("");
            }
        }
        return conceptObject;
    }
    
    private ConceptObject getImages(ConceptObject conceptObject, CSVRecord record) {
        String value;
        String values[];
        ArrayList<NodeImage> nodeImages = new ArrayList<>();

        try {
            value = record.get("foaf:image");
            values = value.split("##");

            for (String image : values) {
                if (!image.isEmpty()) {
                    NodeImage nodeImage = getNodeImage(image);
                    nodeImages.add(nodeImage);
                }
            }
            conceptObject.setImages(nodeImages);            
        } catch (Exception e) {
            //System.err.println("");
        }
        return conceptObject;
    }

    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param record
     * @return
     */
    private ConceptObject getGeoLocalisation(
            ConceptObject conceptObject,
            CSVRecord record, boolean readEmptyData) {
        String lat;
        String longitude;
        // geo:lat
        try {
            lat = record.get("geo:lat");
            longitude = record.get("geo:long");
            if(readEmptyData) {
                conceptObject.setLatitude(lat.replaceAll(",", ".").trim());
                conceptObject.setLongitude(longitude.replaceAll(",", ".").trim());
            } else {
                if (!lat.isEmpty()) {
                    if (!longitude.isEmpty()) {
                        conceptObject.setLatitude(lat.replaceAll(",", ".").trim());
                        conceptObject.setLongitude(longitude.replaceAll(",", ".").trim());
                    }
                }
            }
        } catch (Exception e) {
            //System.err.println("");
        }
        return conceptObject;
    }

    @Data
    public class ConceptObject {

        private String idConcept;
        private String uri;
        private String localId; //identifiant local
        private String arkId;
        private String idTerm;
        // rdf:type pour distinguer les concepts des collections, groupes ...
        private String type;
        private String conceptType;
        private boolean deprecated;

        // notation
        private String notation;

        // les labels
        private ArrayList<Label> prefLabels;
        private ArrayList<Label> altLabels;
        private ArrayList<Label> hiddenLabels;

        // Les notes
        private ArrayList<Label> note;
        private ArrayList<Label> definitions;
        private ArrayList<Label> scopeNotes;
        private ArrayList<Label> examples;
        private ArrayList<Label> historyNotes;
        private ArrayList<Label> changeNotes;
        private ArrayList<Label> editorialNotes;

        // les relations, broader, narrower, related
        private ArrayList<String> broaders;
        private ArrayList<String> narrowers;
        private ArrayList<String> relateds;
        
        // pour les relations personnalisées
        private ArrayList<NodeIdValue> customRelations;        

        // les aligenements 
        private ArrayList<String> exactMatchs;
        private ArrayList<String> closeMatchs;
        private ArrayList<String> broadMatchs;
        private ArrayList<String> narrowMatchs;
        private ArrayList<String> relatedMatchs;

        // géolocalisation
        // geo:lat  geo:long
        private String latitude;
        private String longitude;
        private String gps;

        // skos:member, l'appartenance du concept à un groupe ou collection ...
        private ArrayList<String> members;
        
        // les Facettes d'un Concept
        private String superOrdinate;         
        
        // iso-thes:subGroup
        private ArrayList<String> subGroups;
        private ArrayList<String> replacedBy;

        private ArrayList<NodeImage> images;
        private ArrayList<String> externalResources;

        // l'appartenance du concept à une facette
        private ArrayList<String> memberOfFacets;
        
        // dates 
        //dct:created, dct:modified
        private String created;
        private String modified;

        /// pour récupérer une liste d'alignements (à ajouter ou à supprimer)
        private ArrayList<NodeIdValue> alignments;

        public ConceptObject() {
            prefLabels = new ArrayList<>();
            altLabels = new ArrayList<>();
            hiddenLabels = new ArrayList<>();

            note = new ArrayList<>();
            definitions = new ArrayList<>();
            scopeNotes = new ArrayList<>();
            examples = new ArrayList<>();
            historyNotes = new ArrayList<>();
            changeNotes = new ArrayList<>();
            editorialNotes = new ArrayList<>();

            broaders = new ArrayList<>();
            narrowers = new ArrayList<>();
            relateds = new ArrayList<>();
            customRelations = new ArrayList<>();

            exactMatchs = new ArrayList<>();
            closeMatchs = new ArrayList<>();
            broadMatchs = new ArrayList<>();
            narrowMatchs = new ArrayList<>();
            relatedMatchs = new ArrayList<>();

            members = new ArrayList<>();
            alignments = new ArrayList<>();
            
            subGroups = new ArrayList<>();
            replacedBy = new ArrayList<>();
            images = new ArrayList<>();
            externalResources = new ArrayList<>();
            memberOfFacets = new ArrayList<>();
            conceptType = null;
        }

        public void clear() {
            if (prefLabels != null) {
                prefLabels.clear();
            }
            if (altLabels != null) {
                altLabels.clear();
            }
            if (hiddenLabels != null) {
                hiddenLabels.clear();
            }
            if (note != null) {
                note.clear();
            }
            if (definitions != null) {
                definitions.clear();
            }
            if (scopeNotes != null) {
                scopeNotes.clear();
            }
            if (examples != null) {
                examples.clear();
            }
            if (historyNotes != null) {
                historyNotes.clear();
            }
            if (changeNotes != null) {
                changeNotes.clear();
            }
            if (editorialNotes != null) {
                editorialNotes.clear();
            }
            if (broaders != null) {
                broaders.clear();
            }
            if (narrowers != null) {
                narrowers.clear();
            }
            if (relateds != null) {
                relateds.clear();
            }
            if(customRelations != null)
                customRelations.clear();
            
            if (exactMatchs != null) {
                exactMatchs.clear();
            }
            if (closeMatchs != null) {
                closeMatchs.clear();
            }
            if (broadMatchs != null) {
                broadMatchs.clear();
            }
            if (narrowMatchs != null) {
                narrowMatchs.clear();
            }
            if (relatedMatchs != null) {
                relatedMatchs.clear();
            }
            if (members != null) {
                members.clear();
            }
            if (alignments != null) {
                alignments.clear();
            }
            if (memberOfFacets != null) {
                memberOfFacets.clear();
            }

            conceptType = null;
            gps = null;
        }
    }

    public class Label {

        private String label;
        private String lang;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

    }

}
