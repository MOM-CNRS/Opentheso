package fr.cnrs.opentheso.v2.toolbox.edition.io.csv;

import fr.cnrs.opentheso.models.alignment.NodeAlignmentImport;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
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
import java.util.function.Consumer;
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
public class ThesaurusCsvReader {

    private static final String COL_IDENTIFIER = "identifier";
    private static final String COL_LOCAL_ID = "localid";
    private static final String COL_SKOS_MEMBER = "skos:member";

    private String message = "";
    private char delimiter = ',';

    private ArrayList<String> langs;
    private ArrayList<String> customRelations;    
    private String idLang;

    private ArrayList<ThesaurusCsvConceptObject> conceptObjects;

    private ArrayList<NodeAlignmentImport> nodeAlignmentImports;
    private ArrayList<NodeNote> nodeNotes;
    private ArrayList<NodeIdValue> nodeIdValues;
    private ArrayList<NodeCompareTheso> nodeCompareThesos;    
    
    
    private ArrayList<NodeDeprecated> nodeDeprecateds;

    private ArrayList<NodeReplaceValueByValue> nodeReplaceValueByValues;    
    
    public ThesaurusCsvReader(char delimiter) {
        this.delimiter = delimiter;
        conceptObjects = new ArrayList<>();
    }

    protected CSVFormat headerFormat() {
        return headerFormat(true);
    }

    protected CSVFormat headerFormat(boolean withDelimiter) {
        var builder = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setIgnoreEmptyLines(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true);
        if (withDelimiter) {
            builder.setDelimiter(delimiter);
        }
        return builder.build();
    }

    protected void forHashTokens(CSVRecord csvRecord, String column, Consumer<String> consumer) {
        try {
            String value = csvRecord.get(column);
            if (value == null) {
                return;
            }
            for (String token : value.split("##")) {
                if (!StringUtils.isEmpty(token)) {
                    consumer.accept(token.trim());
                }
            }
        } catch (Exception ignored) {
            // colonne optionnelle absente
        }
    }

    protected void forHashTokensAllowEmpty(CSVRecord csvRecord, String column, boolean readEmptyData,
            Consumer<String> consumer) {
        try {
            String value = csvRecord.get(column);
            if (value == null) {
                return;
            }
            for (String token : value.split("##")) {
                String trimmed = token.trim();
                if (readEmptyData || !trimmed.isEmpty()) {
                    consumer.accept(trimmed);
                }
            }
        } catch (Exception ignored) {
            // colonne optionnelle absente
        }
    }

    protected void forMappedIdOrUriTokens(CSVRecord csvRecord, String idColumn, String uriColumn,
            Consumer<String> consumer) {
        if (csvRecord.isMapped(idColumn)) {
            forHashTokens(csvRecord, idColumn, consumer);
        } else {
            forHashTokens(csvRecord, uriColumn, token -> consumer.accept(getId(token)));
        }
    }

    private void addHashLabels(CSVRecord csvRecord, String column, String lang, boolean readEmptyData,
            boolean emptyCheckOnToken, java.util.List<ThesaurusCsvConceptLabel> target) {
        try {
            String value = csvRecord.get(column);
            if (value == null) {
                return;
            }
            for (String token : value.split("##")) {
                boolean keep = readEmptyData
                        || (emptyCheckOnToken ? !token.isEmpty() : !value.isEmpty());
                if (keep) {
                    ThesaurusCsvConceptLabel label = new ThesaurusCsvConceptLabel();
                    label.setLabel(token);
                    label.setLang(lang);
                    target.add(label);
                }
            }
        } catch (Exception ignored) {
            // colonne optionnelle absente
        }
    }

    private void addPrefLabel(CSVRecord csvRecord, String column, String lang, boolean readEmptyData,
            java.util.List<ThesaurusCsvConceptLabel> target) {
        try {
            String value = csvRecord.get(column);
            if (value == null) {
                return;
            }
            if (readEmptyData || !value.isEmpty()) {
                ThesaurusCsvConceptLabel label = new ThesaurusCsvConceptLabel();
                label.setLabel(value);
                label.setLang(lang);
                target.add(label);
            }
        } catch (Exception ignored) {
            // colonne optionnelle absente
        }
    }

        
    public boolean readFileCsvForGetIdFromPrefLabelSetLang(Reader in) {
        try {
            CSVParser cSVParser = headerFormat(false).parse(in);
           
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
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }              
    
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
            for (CSVRecord csvRecord : cSVParser) {
                NodeDeprecated nodeDeprecated = new NodeDeprecated();
                try {
                    value = csvRecord.get("deprecated");
                    if (value == null) {
                        continue;
                    }
                    nodeDeprecated.setDeprecatedId(value);
                } catch (Exception e) {
                    continue;
                }
                try {
                    value = csvRecord.get("isReplacedBy");
                    if (value == null) {
                    } else {
                        nodeDeprecated.setReplacedById(value);
                    }
                } catch (Exception e) {
                }
                try {
                    value = csvRecord.get("skos:note@" + lang);
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
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
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
            for (CSVRecord csvRecord : cSVParser) {
                NodeCompareTheso nodeCompareTheso = new NodeCompareTheso();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = csvRecord.get("skos:prefLabel@" + idLang);
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
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean readFileConceptId(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader()
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();
            CSVParser cSVParser = cSVFormat.parse(in);
            String value;
            nodeIdValues = new ArrayList<>();
            for (CSVRecord csvRecord : cSVParser) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL
                try {
                    value = csvRecord.get(COL_LOCAL_ID);
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
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

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

            for (CSVRecord csvRecord : cSVParser) {
                String id;
                try {
                    id = csvRecord.get(COL_IDENTIFIER);
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
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean readFileArk(Reader in) {
        try {
            CSVFormat cSVFormat = CSVFormat.DEFAULT.builder().setHeader().setDelimiter(delimiter)
                    .setIgnoreEmptyLines(true).setIgnoreHeaderCase(true).setTrim(true).build();

            CSVParser cSVParser = cSVFormat.parse(in);
            String value;
            nodeIdValues = new ArrayList<>();
            for (CSVRecord csvRecord : cSVParser) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = csvRecord.get(COL_LOCAL_ID);
                    if (value == null) {
                        continue;
                    }
                    nodeIdValue.setId(value);
                } catch (Exception e) {
                    continue;
                }
                // on récupère les uris à supprimer
                try {
                    value = csvRecord.get("arkId");
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
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

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
            CSVParser csvParser = headerFormat().parse(br);

            for (CSVRecord csvRecord : csvParser) {
                ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();

                // Lecture du localId (gestion de la casse et BOM)
                String localId = getSafe(csvRecord, "localId");
                if (localId == null || localId.isEmpty()) {
                    localId = getSafe(csvRecord, COL_LOCAL_ID); // fallback
                }

                if (localId == null || localId.isEmpty()) {
                    continue;
                }
                conceptObject.setLocalId(localId.trim());

                // Lecture de l'URI à supprimer
                String uri = getSafe(csvRecord, "Uri");
                if (uri == null || uri.isEmpty()) {
                    uri = getSafe(csvRecord, "uri");
                }

                if (uri != null && !uri.isEmpty()) {
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId("");
                    nodeIdValue.setValue(uri.trim());
                    conceptObject.getAlignments().add(nodeIdValue);
                }

                conceptObjects.add(conceptObject);
            }

            return true;

        } catch (IOException ex) {
            log.error(ThesaurusCsvReader.class.getName() +  ex.getMessage());
            message = "Erreur lors de la lecture du fichier CSV : " + ex.getMessage();
            return false;
        }
    }
    /**
     * Récupère la valeur d'une colonne de façon sécurisée,
     * en gérant les BOM éventuels dans le nom de colonne.
     */
    private String getSafe(CSVRecord csvRecord, String header) {
        try {
            // Essai direct
            if (csvRecord.isMapped(header)) {
                return csvRecord.get(header);
            }
            // Essai avec un éventuel BOM
            String withBom = "\uFEFF" + header;
            if (csvRecord.isMapped(withBom)) {
                return csvRecord.get(withBom);
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
            CSVParser cSVParser = headerFormat().parse(in);
            String value;
            for (CSVRecord csvRecord : cSVParser) {
                ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = csvRecord.get(COL_LOCAL_ID);
                    if (StringUtils.isEmpty(value)) {
                        continue;
                    }
                    conceptObject.setLocalId(value);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les images 
                conceptObject = getImages(conceptObject, csvRecord);

                conceptObjects.add(conceptObject);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
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
            try (CSVParser csvParser = headerFormat().parse(in)) {
                nodeIdValues = new ArrayList<>();

                // Parcourir les enregistrements CSV
                for (CSVRecord csvRecord : csvParser) {
                    NodeIdValue nodeIdValue = new NodeIdValue();

                    // Récupérer l'identifiant "localId"
                    String value = csvRecord.get(COL_LOCAL_ID);
                    if (value == null || value.isBlank()) {
                        continue; // Si vide, passer à l'enregistrement suivant
                    }
                    nodeIdValue.setId(value);

                    // Récupérer la notation "skos:notation"
                    value = csvRecord.get("skos:notation");
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
            CSVParser cSVParser = headerFormat().parse(in);
            String value;
            nodeIdValues = new ArrayList<>();
            for (CSVRecord csvRecord : cSVParser) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = csvRecord.get(COL_LOCAL_ID);
                    if (value == null) {
                        continue;
                    }
                    nodeIdValue.setId(value);
                } catch (Exception e) {
                    continue;
                }
                // on récupère les ids des collections à ajouter au concept
                try {
                    value = csvRecord.get(COL_SKOS_MEMBER);
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
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }       
    
    public ArrayList<String > readHeadersFileAlignment (Reader in){
        try {
            CSVParser cSVParser = headerFormat().parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            ArrayList<String> headerSourceAlign = new ArrayList<>();
            for (String columnName : headers.keySet()) {
                if (columnName.equalsIgnoreCase(COL_LOCAL_ID)) {
                    continue;
                }
                headerSourceAlign.add(columnName);
            }
            return headerSourceAlign;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;        
    }

    public ArrayList<String > readHeadersFileRelated (Reader in){
        try {
            CSVParser cSVParser = headerFormat().parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            ArrayList<String> headersRelated = new ArrayList<>();
            for (String columnName : headers.keySet()) {
                headersRelated.add(columnName);
            }
            return headersRelated;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
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
            CSVParser parser = headerFormat().parse(in);

            // id -> valeurs related uniques
            Map<String, Set<String>> relatedById = new HashMap<>();

            for (CSVRecord csvRecord : parser) {

                String id;
                String related;

                try {
                    id = csvRecord.get(COL_LOCAL_ID);
                    related = csvRecord.get("skos:related");
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
            CSVParser cSVParser = headerFormat().parse(in);
            String value;
            if (nodeAlignmentImports == null) {
                nodeAlignmentImports = new ArrayList<>();
            } else {
                nodeAlignmentImports.clear();
            }
            for (CSVRecord csvRecord : cSVParser) {
                NodeAlignmentImport nodeAlignmentImport = new NodeAlignmentImport();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    value = csvRecord.get(COL_LOCAL_ID);
                    if (value == null) {
                        continue;
                    }
                    nodeAlignmentImport.setLocalId(value);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les alignements 
                nodeAlignmentImport = getNewAlignment(nodeAlignmentImport, csvRecord, headerSourceAlign);
                if (nodeAlignmentImport != null) {
                    nodeAlignmentImports.add(nodeAlignmentImport);
                }
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private NodeAlignmentImport getNewAlignment(
            NodeAlignmentImport nodeAlignmentImport,
            CSVRecord csvRecord, ArrayList<String> headerSourceAlign) {
        String uri1;
        ToolsHelper toolsHelper = new ToolsHelper();

        /// types alignements 1=exactMatch ; 2=closeMatch ; 3=broadMatch ; 4=relatedMatch ; 5=narrowMatch
        for (String alignSource : headerSourceAlign) {
            try {
                uri1 = csvRecord.get(alignSource);
                if (StringUtils.isBlank(uri1)) {
                    continue;
                }
                String uriForValidation = uri1.contains("##") ? uri1.split("##", 2)[0] : uri1;
                if (!toolsHelper.isValidURI(uriForValidation)) {
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
            CSVParser cSVParser = headerFormat().parse(in);

            String idConcept;
            for (CSVRecord csvRecord : cSVParser) {
                ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    idConcept = csvRecord.get(COL_LOCAL_ID);
                    if (idConcept == null || idConcept.isEmpty()) {
                        continue;
                    }
                    conceptObject.setIdConcept(idConcept);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les labels
                conceptObject = getLabels(conceptObject, csvRecord, false);

                conceptObjects.add(conceptObject);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
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
            CSVParser cSVParser = headerFormat().parse(in);

            String idConcept;
            nodeIdValues = new ArrayList<>();

            for (CSVRecord csvRecord : cSVParser) {
                NodeIdValue nodeIdValue = new NodeIdValue();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL
                try {
                    idConcept = csvRecord.get(COL_LOCAL_ID);
                    if (idConcept == null || idConcept.isEmpty()) {
                        continue;
                    }
                    nodeIdValue.setId(idConcept);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les labels
                nodeIdValue = getPrefLabel(nodeIdValue, csvRecord, lang);

                if (nodeIdValue != null) {
                    nodeIdValues.add(nodeIdValue);
                }
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private NodeIdValue getPrefLabel(NodeIdValue nodeIdValue, CSVRecord csvRecord, String lang) {
        String value;
        try {
            value = csvRecord.get("skos:prefLabel@" + lang.trim());
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
            CSVParser cSVParser = headerFormat().parse(in);

            String idConcept;
            for (CSVRecord csvRecord : cSVParser) {
                ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    idConcept = csvRecord.get(COL_LOCAL_ID);
                    if (idConcept == null || idConcept.isEmpty()) {
                        continue;
                    }
                    conceptObject.setIdConcept(idConcept);
                } catch (Exception e) {
                    continue;
                }

                // on récupère les notes 
                conceptObject = getNotes(conceptObject, csvRecord, false);

                conceptObjects.add(conceptObject);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
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
            CSVParser cSVParser = headerFormat().parse(in);

            String idConcept;
            nodeReplaceValueByValues = new ArrayList<>();
            for (CSVRecord csvRecord : cSVParser) {
                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                try {
                    idConcept = csvRecord.get(COL_LOCAL_ID);
                    if (idConcept == null || idConcept.isEmpty()) {
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("Unable to read 'localid' column: {}", e.getMessage());
                    continue;
                }
                for (String idLang1 : usedLangs) {
                    NodeReplaceValueByValue nodeReplaceValueByValue = new NodeReplaceValueByValue();
                    // on récupère les prefLabels 
                    nodeReplaceValueByValue = getValueAndPropertyPrefLabel(nodeReplaceValueByValue, csvRecord, idLang1);     
                    if (nodeReplaceValueByValue != null) {
                        nodeReplaceValueByValue.setIdConcept(idConcept);
                        nodeReplaceValueByValues.add(nodeReplaceValueByValue);
                    }
                    NodeReplaceValueByValue nodeReplaceValueByValue2 = new NodeReplaceValueByValue();
                    // on récupère les altLabels 
                    nodeReplaceValueByValue2 = getValueAndPropertyAltLabel(nodeReplaceValueByValue2, csvRecord, idLang1);     
                    if (nodeReplaceValueByValue2 != null) {
                        nodeReplaceValueByValue2.setIdConcept(idConcept);
                        nodeReplaceValueByValues.add(nodeReplaceValueByValue2);
                    }   
                    NodeReplaceValueByValue nodeReplaceValueByValue3 = new NodeReplaceValueByValue();
                    // on récupère les définitions
                    nodeReplaceValueByValue3 = getValueAndPropertyDefinition(nodeReplaceValueByValue3, csvRecord, idLang1);     
                    if (nodeReplaceValueByValue3 != null) {
                        nodeReplaceValueByValue3.setIdConcept(idConcept);
                        nodeReplaceValueByValues.add(nodeReplaceValueByValue3);
                    }                     
                    
                }
                 // on récupère les BTs 
                NodeReplaceValueByValue nodeReplaceValueByValue = new NodeReplaceValueByValue();
                nodeReplaceValueByValue = getValueAndPropertyBT(nodeReplaceValueByValue, csvRecord);     
                if (nodeReplaceValueByValue != null) {
                    nodeReplaceValueByValue.setIdConcept(idConcept);
                    nodeReplaceValueByValues.add(nodeReplaceValueByValue);
                }                  
                 
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }    
    
    private NodeReplaceValueByValue getValueAndPropertyPrefLabel(NodeReplaceValueByValue nodeReplaceValueByValue, CSVRecord csvRecord,
            String idLang) {
        String value;
        try {
            //récupère les prefLabels
            value = csvRecord.get("new_skos:preflabel@"+ idLang);
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setIdLang(idLang);
                nodeReplaceValueByValue.setNewValue(value);
                nodeReplaceValueByValue.setSKOSProperty(SKOSProperty.PREF_LABEL);
            } else {
                return null;
            }
            value = csvRecord.get("skos:preflabel@"+ idLang);
            nodeReplaceValueByValue.setOldValue(value);
            return nodeReplaceValueByValue;
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
        }      
        return null;
    }
    private NodeReplaceValueByValue getValueAndPropertyAltLabel(NodeReplaceValueByValue nodeReplaceValueByValue, CSVRecord csvRecord,
            String idLang) {
        String value;
        try {
            //récupère les altLabels
            value = csvRecord.get("new_skos:altLabel@"+ idLang);
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setIdLang(idLang);
                nodeReplaceValueByValue.setNewValue(value);
                nodeReplaceValueByValue.setSKOSProperty(SKOSProperty.ALT_LABEL);
            } else {
                return null;
            }
            try {
                value = csvRecord.get("skos:altlabel@"+ idLang);
                nodeReplaceValueByValue.setOldValue(value);
            } catch (Exception e) {
            }
            return nodeReplaceValueByValue;
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
        }      
        return null;
    }   
    private NodeReplaceValueByValue getValueAndPropertyDefinition(NodeReplaceValueByValue nodeReplaceValueByValue, CSVRecord csvRecord,
            String idLang) {
        String value;
        try {
            //récupère les définitons
            value = csvRecord.get("new_skos:definition@"+ idLang);
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setIdLang(idLang);
                nodeReplaceValueByValue.setNewValue(value);
                nodeReplaceValueByValue.setSKOSProperty(SKOSProperty.DEFINITION);
            } else {
                return null;
            }
            try { 
                value = csvRecord.get("skos:definition@"+ idLang);
                nodeReplaceValueByValue.setOldValue(value);
            } catch (Exception e) {
            }                
            return nodeReplaceValueByValue;
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
        }      
        return null;
    }     
    
    private NodeReplaceValueByValue getValueAndPropertyBT(NodeReplaceValueByValue nodeReplaceValueByValue, CSVRecord csvRecord) {
        String value;
        try {
            value = csvRecord.get("new_skos:broader");
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setNewValue(value);
                nodeReplaceValueByValue.setSKOSProperty(SKOSProperty.BROADER);
            } else {
                return null;
            }
            value = csvRecord.get("skos:broader");
            if(value != null && !value.isEmpty()) {
                nodeReplaceValueByValue.setOldValue(value);
                return nodeReplaceValueByValue;
            } else {
                nodeReplaceValueByValue.setOldValue(null);
                return nodeReplaceValueByValue;
            }
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
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
            CSVParser cSVParser = headerFormat().parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            String values[];
            for (String columnName : headers.keySet()) {
                if (columnName.contains("@")) {
                    values = columnName.split("@");
                    if (values[1] != null && !langs.contains(values[1])) {
                        langs.add(values[1]);
                    }
                }
                if (columnName.contains("customRelationId")) {
                    values = columnName.split(":");
                    if(values.length < 2) continue;
                    if (values[1] != null && !customRelations.contains(values[1])) {
                        customRelations.add(values[1]);
                    }
                }                
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return !langs.isEmpty();
    }

    public String getLangOfValue(Reader in) {
        String lang = null;
        try {
            CSVParser cSVParser = headerFormat().parse(in);
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
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
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
            CSVParser cSVParser = headerFormat().parse(in);  
            String uri1 = null;
            //          boolean first = true;

            for (CSVRecord csvRecord : cSVParser) {
                ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();

                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                // puis on génère un nouvel identifiant
                try {
                    uri1 = csvRecord.get("URI");
                    conceptObject.setUri(uri1);
                } catch (Exception e) {
                }

                try {
                    uri1 = csvRecord.get(COL_IDENTIFIER);
                } catch (Exception e) {
                }

                try {
                    if (uri1 == null || uri1.isEmpty()) {
                        uri1 = csvRecord.get("URI");
                        uri1 = getId(uri1);
                    }
                    conceptObject.setIdConcept(uri1);
                } catch (Exception e) {
                }

                // on récupère l'id Ark s'il existe
                conceptObject = getArkId(conceptObject, csvRecord);

                // on récupère les labels
                conceptObject = getLabels(conceptObject, csvRecord, false);

                // on récupère les notes
                conceptObject = getNotes(conceptObject, csvRecord, false);

                // on récupère le type
                conceptObject.setType(getType(csvRecord));

                // on récupère la notation
                conceptObject.setNotation(getNotation(csvRecord));

                // on récupère les relations (BT, NT, RT)
                conceptObject = getRelations(conceptObject, csvRecord);

                // on récupère les alignements 
                conceptObject = getAlignments(conceptObject, csvRecord, false);

                // on récupère les images
                conceptObject = getImages(conceptObject, csvRecord);
                
                // on récupère la localisation
                conceptObject = getGps(conceptObject, csvRecord);
                conceptObject = getGeoLocalisation(conceptObject, csvRecord, false);

                // on récupère les membres (l'appartenance du concept à un groupe, collection ...
                conceptObject = getMembers(conceptObject, csvRecord);

                // on récupère la date
                conceptObject = getDates(conceptObject, csvRecord);

                // on récupère l'appartenance du concept à une facette
                conceptObject = getMemberOfFacet(conceptObject, csvRecord);

                conceptObjects.add(conceptObject);
                uri1 = null;
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
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
            CSVParser cSVParser = headerFormat().parse(in);            

            String uri1 = null;
            String uri_forId;
            for (CSVRecord csvRecord : cSVParser) {
                ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();

                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                // puis on génère un nouvel identifiant
                try {
                    uri1 = csvRecord.get("URI");
                    conceptObject.setUri(uri1);
                } catch (Exception e) {
                }

                if (csvRecord.isMapped(COL_IDENTIFIER)) {
                    try {
                        uri_forId = csvRecord.get(COL_IDENTIFIER);
                        if (uri_forId == null || uri_forId.isEmpty()) {
                            conceptObject.setIdConcept(getId(uri1));
                        } else {
                            conceptObject.setIdConcept(uri_forId);
                        }
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        uri_forId = csvRecord.get("URI");
                        conceptObject.setIdConcept(getId(uri_forId));
                    } catch (Exception e) {
                    }
                }
                if(StringUtils.isEmpty(conceptObject.getIdConcept())){
                    message = message + "\n" + "concept sans Id : " + csvRecord.toString();
                    continue;
                }

                // on récupère l'id Ark s'il existe
                conceptObject = getArkId(conceptObject, csvRecord);

                // on récupère les labels
                conceptObject = getLabels(conceptObject, csvRecord, readEmptyData);

                // on récupère les notes
                conceptObject = getNotes(conceptObject, csvRecord, readEmptyData);

                // on récupère le type de l'enregistrement (concept, collection)
                conceptObject.setType(getType(csvRecord));
                
                // on récupérer du concept (People, qualifier ...)
                conceptObject.setConceptType(getConceptType(csvRecord));

                // on récupère la notation
                conceptObject.setNotation(getNotation(csvRecord));

                // on récupère les relations (BT, NT, RT)
                conceptObject = getRelations(conceptObject, csvRecord);

                // on récupère les relations (BT, NT, RT)
                conceptObject = getCustomRelations(conceptObject, csvRecord);                
                
                // on récupère les alignements 
                conceptObject = getAlignments(conceptObject, csvRecord, readEmptyData);

                // on récupère la localisation
                conceptObject = getGps(conceptObject, csvRecord);
                conceptObject = getGeoLocalisation(conceptObject, csvRecord, readEmptyData);

                
                
                // on récupère les membres (l'appartenance du concept à un groupe, collection ...
                if("skos:Concept".equalsIgnoreCase(conceptObject.getType())){                
                    conceptObject = getMembers(conceptObject, csvRecord);
                }
                if("skos:collection".equalsIgnoreCase(conceptObject.getType())){                
                    conceptObject = getMembers(conceptObject, csvRecord);
                }                
                
                // récupération des sous groupes
                if("skos:Collection".equalsIgnoreCase(conceptObject.getType())){
                    conceptObject = getSubGroups(conceptObject, csvRecord);
                }
                
                // récupération des membres d'une Facette
                if("skos-thes:ThesaurusArray".equalsIgnoreCase(conceptObject.getType())){
                    conceptObject = getMembersOfFacet(conceptObject, csvRecord);
                    
                    // récupération du parent de la facette
                    conceptObject = getSuperOrdinate(conceptObject, csvRecord);
                }                
                
                // définir si le concept est déprécié (Obsolète) et s'il a un concept de remplacement 
                if("skos:Concept".equalsIgnoreCase(conceptObject.getType())){
                    conceptObject = setDeprecatedConcept(conceptObject, csvRecord);
                }                
                
                // récupération des resources Externes
                conceptObject = getExternalResources(conceptObject, csvRecord);
                
                
                // on récupère la date
                conceptObject = getDates(conceptObject, csvRecord);
                
                // on récupère les images 
                conceptObject = getFoafImages(conceptObject, csvRecord);

                conceptObjects.add(conceptObject);
                uri1 = null;
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * permet de récupérer les resources externes
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getExternalResources(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        forHashTokens(csvRecord, "dcterms:source", conceptObject.getExternalResources()::add);
        return conceptObject;
    }    
    
    /**
     * permet de récupérer les URI des images
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getFoafImages(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        forHashTokens(csvRecord, "foaf:Image", value -> conceptObject.getImages().add(getNodeImage(value)));
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
                    applyImageToken(nodeImage, value1);
                }
            }
        } catch (Exception e) {
        }
        if(StringUtils.isEmpty(nodeImage.getUri())) return null;
        return nodeImage;
    }

    private void applyImageToken(NodeImage nodeImage, String value1) {
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
    
    
    /**
     * permet de savoir si le concept est déprécié et s'il a des concepts de remplacement
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject setDeprecatedConcept(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        String value;
        try {
            value = csvRecord.get("owl:deprecated");
            if (!StringUtils.isEmpty(value)) {
                if("true".equalsIgnoreCase(value)) {
                    conceptObject.setDeprecated(true);
                    getReplacedByOfDeprecatedConcept(conceptObject, csvRecord);
                }
                else
                    conceptObject.setDeprecated(false);
            }
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
        }

        return conceptObject;
    }      
    
    /**
     * permet de récupérer des dcterms:isReplacedBy les concepts de rempalacement
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getReplacedByOfDeprecatedConcept(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        forHashTokens(csvRecord, "dcterms:isReplacedBy", value -> conceptObject.getReplacedBy().add(getId(value)));
        return conceptObject;
    }      
    
    /**
     * permet de récupérer le parent de la Facette
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getSuperOrdinate(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        String value;
        if (csvRecord.isMapped("superOrdinateId")) {
            try {
                value = csvRecord.get("superOrdinateId");
                if (StringUtils.isNotEmpty(value)) {
                    conceptObject.setSuperOrdinate(value.trim());
                }
            } catch (Exception e) {
                // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
            }
        } else {
            try {
                value = csvRecord.get("iso-thes:superOrdinate");
                if (StringUtils.isNotEmpty(value)) {
                    conceptObject.setSuperOrdinate(getId(value.trim()));
                }
            } catch (Exception e) {
                // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
            }
        }

        return conceptObject;
    }      
    
    
    /**
     * permet de récupérer les concepts qui sont membre de cette Facette
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getMembersOfFacet(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        forMappedIdOrUriTokens(csvRecord, "memberid", COL_SKOS_MEMBER,
                conceptObject.getMembers()::add);
        return conceptObject;
    }       
    
    /**
     * permet de récupérer les sous groupes d'un groupe
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getSubGroups(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        forHashTokens(csvRecord, "iso-thes:subGroup", value -> conceptObject.getSubGroups().add(getId(value)));
        return conceptObject;
    }    
    
    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getGps(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {

        try {
            String value = csvRecord.get("geo:gps");
            if (!value.isEmpty()) {
                conceptObject.setGps(value.trim());
            }
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
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
     * @param csvRecord
     * @return
     */
    private String getType(CSVRecord csvRecord) {
        String type = "";
        try {
            type = csvRecord.get("rdf:type");
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
        }
  
        return type.trim().toLowerCase();
    }
    
    /**
     * permet de récupérer le type du concept (People, qualifier, place ...)
     *
     * @param csvRecord
     * @return
     */
    private String getConceptType(CSVRecord csvRecord) {
        String conceptType = "";
        try {
            conceptType = csvRecord.get("dct:type");
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
        }
        return conceptType.trim().toLowerCase();
    }    

    /**
     * permet de récupérer la notation du concept
     *
     * @param csvRecord
     * @return
     */
    private String getNotation(CSVRecord csvRecord) {
        String notation = "";
        try {
            notation = csvRecord.get("skos:notation");
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
        }
        return notation.trim();
    }

    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getDates(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {

        // dct:created
        String value;
        try {
            value = csvRecord.get("dcterms:created");
            if (!value.isEmpty()) {
                conceptObject.setCreated(value.trim());
            } else {
                value = csvRecord.get("dct:created");
                if (!value.isEmpty()) {
                    conceptObject.setCreated(value.trim());
                }
            }
            
        } catch (Exception e) {
        }

        // dct:modified
        try {
            value = csvRecord.get("dcterms:modified");
            if (!value.isEmpty()) {
                conceptObject.setModified(value.trim());
            } else {
                value = csvRecord.get("dct:modified");
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
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getMembers(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        forMappedIdOrUriTokens(csvRecord, "memberid", COL_SKOS_MEMBER,
                conceptObject.getMembers()::add);
        return conceptObject;
    }

    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getMemberOfFacet(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        if (csvRecord.isMapped("skos:facet")) {
            forHashTokens(csvRecord, "skos:facet", conceptObject.getMemberOfFacets()::add);
        }
        return conceptObject;
    }


    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getArkId(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        try {
            String arkId = csvRecord.get("arkId");
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
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getAlignments(
            ThesaurusCsvConceptObject conceptObject,
            CSVRecord csvRecord, boolean readEmptyData) {
        forHashTokensAllowEmpty(csvRecord, "skos:exactMatch", readEmptyData, conceptObject.getExactMatchs()::add);
        forHashTokensAllowEmpty(csvRecord, "skos:closeMatch", readEmptyData, conceptObject.getCloseMatchs()::add);
        forHashTokensAllowEmpty(csvRecord, "skos:broadMatch", readEmptyData, conceptObject.getBroadMatchs()::add);
        forHashTokensAllowEmpty(csvRecord, "skos:narrowMatch", readEmptyData, conceptObject.getNarrowMatchs()::add);
        forHashTokensAllowEmpty(csvRecord, "skos:relatedMatch", readEmptyData, conceptObject.getRelatedMatchs()::add);
        return conceptObject;
    }

    /**
     * permet de charger toutes les relations d'un concept
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getRelations(
            ThesaurusCsvConceptObject conceptObject,
            CSVRecord csvRecord) {
        forMappedIdOrUriTokens(csvRecord, "narrowerid", "skos:narrower",
                conceptObject.getNarrowers()::add);
        forMappedIdOrUriTokens(csvRecord, "broaderid", "skos:broader",
                conceptObject.getBroaders()::add);
        forMappedIdOrUriTokens(csvRecord, "relatedid", "skos:related",
                conceptObject.getRelateds()::add);
        return conceptObject;
    }
    
    /**
     * permet de charger toutes les relations d'un concept
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getCustomRelations(
            ThesaurusCsvConceptObject conceptObject,
            CSVRecord csvRecord) {
        if (customRelations == null) {
            return conceptObject;
        }
        for (String customRelation : customRelations) {
            forHashTokens(csvRecord, "customRelationId:" + customRelation, value1 -> {
                NodeIdValue nodeIdValue = new NodeIdValue();
                nodeIdValue.setId(value1);
                nodeIdValue.setValue(customRelation);
                conceptObject.getCustomRelations().add(nodeIdValue);
            });
        }
        return conceptObject;
    }    

    /**
     * permet de charger tous les labels d'un concept dans toutes les langues
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getLabels(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord, boolean readEmptyData) {
        for (String idLang2 : langs) {
            addPrefLabel(csvRecord, "skos:preflabel@" + idLang2.trim(), idLang2, readEmptyData,
                    conceptObject.getPrefLabels());
            addHashLabels(csvRecord, "skos:altLabel@" + idLang2.trim(), idLang2, readEmptyData, false,
                    conceptObject.getAltLabels());
            addHashLabels(csvRecord, "skos:hiddenLabel@" + idLang2.trim(), idLang2, readEmptyData, false,
                    conceptObject.getHiddenLabels());
        }
        return conceptObject;
    }

    private ThesaurusCsvConceptObject getNotes(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord, boolean readEmptyData) {
        for (String idLang1 : langs) {
            addHashLabels(csvRecord, "skos:note@" + idLang1.trim(), idLang1, readEmptyData, true,
                    conceptObject.getNote());
            addHashLabels(csvRecord, "skos:definition@" + idLang1.trim(), idLang1, readEmptyData, true,
                    conceptObject.getDefinitions());
            addHashLabels(csvRecord, "skos:scopeNote@" + idLang1.trim(), idLang1, readEmptyData, true,
                    conceptObject.getScopeNotes());
            addHashLabels(csvRecord, "skos:example@" + idLang1.trim(), idLang1, readEmptyData, true,
                    conceptObject.getExamples());
            addHashLabels(csvRecord, "skos:historyNote@" + idLang1.trim(), idLang1, readEmptyData, true,
                    conceptObject.getHistoryNotes());
            addHashLabels(csvRecord, "skos:changeNote@" + idLang1.trim(), idLang1, readEmptyData, true,
                    conceptObject.getChangeNotes());
            addHashLabels(csvRecord, "skos:editorialNote@" + idLang1.trim(), idLang1, readEmptyData, true,
                    conceptObject.getEditorialNotes());
        }
        return conceptObject;
    }
    
    private ThesaurusCsvConceptObject getImages(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        forHashTokens(csvRecord, "foaf:image", image -> conceptObject.getImages().add(getNodeImage(image)));
        return conceptObject;
    }

    /**
     * permet de charger tous les alignements d'un concept
     *
     * @param conceptObject
     * @param csvRecord
     * @return
     */
    private ThesaurusCsvConceptObject getGeoLocalisation(
            ThesaurusCsvConceptObject conceptObject,
            CSVRecord csvRecord, boolean readEmptyData) {
        String lat;
        String longitude;
        // geo:lat
        try {
            lat = csvRecord.get("geo:lat");
            longitude = csvRecord.get("geo:long");
            if(readEmptyData) {
                conceptObject.setLatitude(lat.replace(",", ".").trim());
                conceptObject.setLongitude(longitude.replace(",", ".").trim());
            } else {
                if (!lat.isEmpty() && !longitude.isEmpty()) {
                    conceptObject.setLatitude(lat.replace(",", ".").trim());
                    conceptObject.setLongitude(longitude.replace(",", ".").trim());
                }
            }
        } catch (Exception e) {
            // colonne/valeur optionnelle absente pour cette langue : ignorée volontairement
        }
        return conceptObject;
    }

}
