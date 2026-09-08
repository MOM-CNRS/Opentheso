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
import org.apache.commons.lang3.Strings;

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

    private static String optionalColumn(CSVRecord rec, String name) {
        try {
            return rec.get(name);
        } catch (Exception ignored) {
            // colonne optionnelle absente
            return null;
        }
    }

    private String localIdOrWarn(CSVRecord csvRecord) {
        try {
            return csvRecord.get(COL_LOCAL_ID);
        } catch (Exception e) {
            log.warn("Unable to read 'localid' column: {}", e.getMessage());
            return null;
        }
    }

    private int parseAlignmentTypeId(String typeToken) {
        try {
            return Integer.parseInt(typeToken);
        } catch (Exception e) {
            return 1;
        }
    }

    private String identifierOrKeep(CSVRecord csvRecord, String current) {
        try {
            return csvRecord.get(COL_IDENTIFIER);
        } catch (Exception e) {
            // colonne identifier optionnelle absente
            return current;
        }
    }

    private void assignListFileConceptId(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord, String uri1) {
        try {
            if (uri1 == null || uri1.isEmpty()) {
                uri1 = csvRecord.get("URI");
                uri1 = getId(uri1);
            }
            conceptObject.setIdConcept(uri1);
        } catch (Exception e) {
            // identifiant non déductible pour cette ligne
        }
    }

    private void assignIdFromIdentifierColumn(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord, String uri1) {
        try {
            String uriForId = csvRecord.get(COL_IDENTIFIER);
            if (uriForId == null || uriForId.isEmpty()) {
                conceptObject.setIdConcept(getId(uri1));
            } else {
                conceptObject.setIdConcept(uriForId);
            }
        } catch (Exception e) {
            // colonne identifier illisible
        }
    }

    private void assignIdFromUriColumn(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        try {
            String uriForId = csvRecord.get("URI");
            conceptObject.setIdConcept(getId(uriForId));
        } catch (Exception e) {
            // identifiant non déductible depuis l'URI
        }
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
            String[] values;
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
            String[] values;
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
                value = optionalColumn(csvRecord, "deprecated");
                if (value == null) {
                    continue;
                }
                nodeDeprecated.setDeprecatedId(value);
                value = optionalColumn(csvRecord, "isReplacedBy");
                if (value != null) {
                    nodeDeprecated.setReplacedById(value);
                }
                value = optionalColumn(csvRecord, "skos:note@" + lang);
                if (value != null) {
                    nodeDeprecated.setNote(value);
                    nodeDeprecated.setNoteLang(lang);
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
                value = optionalColumn(csvRecord, "skos:prefLabel@" + idLang);
                if (value == null) {
                    continue;
                }
                nodeCompareTheso.setOriginalPrefLabel(value);
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
                value = optionalColumn(csvRecord, COL_LOCAL_ID);
                if (value == null) {
                    continue;
                }
                nodeIdValue.setId(value);
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
                String id = optionalColumn(csvRecord, COL_IDENTIFIER);
                if (id == null || id.isEmpty()) {
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
            nodeIdValues = new ArrayList<>();
            for (CSVRecord csvRecord : cSVParser) {
                addArkRecord(csvRecord);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private void addArkRecord(CSVRecord csvRecord) {
        String value = optionalColumn(csvRecord, COL_LOCAL_ID);
        if (value == null) {
            return;
        }
        NodeIdValue nodeIdValue = new NodeIdValue();
        nodeIdValue.setId(value);
        value = optionalColumn(csvRecord, "arkId");
        if (value == null) {
            return;
        }
        nodeIdValue.setValue(value.trim());
        nodeIdValues.add(nodeIdValue);
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
                value = optionalColumn(csvRecord, COL_LOCAL_ID);
                if (StringUtils.isEmpty(value)) {
                    continue;
                }
                conceptObject.setLocalId(value);

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

                for (CSVRecord csvRecord : csvParser) {
                    addNotationRecord(csvRecord);
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

    private void addNotationRecord(CSVRecord csvRecord) {
        String value = csvRecord.get(COL_LOCAL_ID);
        if (value == null || value.isBlank()) {
            return;
        }
        NodeIdValue nodeIdValue = new NodeIdValue();
        nodeIdValue.setId(value);
        value = csvRecord.get("skos:notation");
        if (value == null || value.isBlank()) {
            return;
        }
        nodeIdValue.setValue(value.trim());
        nodeIdValues.add(nodeIdValue);
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
            nodeIdValues = new ArrayList<>();
            for (CSVRecord csvRecord : cSVParser) {
                addCollectionRecord(csvRecord);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private void addCollectionRecord(CSVRecord csvRecord) {
        String value = optionalColumn(csvRecord, COL_LOCAL_ID);
        if (value == null) {
            return;
        }
        NodeIdValue nodeIdValue = new NodeIdValue();
        nodeIdValue.setId(value);
        value = optionalColumn(csvRecord, COL_SKOS_MEMBER);
        if (value == null) {
            return;
        }
        nodeIdValue.setValue(value.trim());
        nodeIdValues.add(nodeIdValue);
    }
    
    public List<String> readHeadersFileAlignment(Reader in){
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
        return List.of();
    }

    public List<String> readHeadersFileRelated(Reader in){
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
        return List.of();
    }

    /**
     * permet de lire un fichier CSV complet pour importer les RT
     *
     * @param in
     * @return
     */
    public boolean readFileRelated(Reader in) {
        try {
            CSVParser parser = headerFormat().parse(in);

            // id -> valeurs related uniques
            Map<String, Set<String>> relatedById = new HashMap<>();

            for (CSVRecord csvRecord : parser) {
                addRelatedRecord(relatedById, csvRecord);
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

    private void addRelatedRecord(Map<String, Set<String>> relatedById, CSVRecord csvRecord) {
        String id = optionalColumn(csvRecord, COL_LOCAL_ID);
        String related = optionalColumn(csvRecord, "skos:related");
        if (id == null || related == null) {
            return;
        }
        if (StringUtils.isBlank(id) || StringUtils.isBlank(related)) {
            return;
        }
        relatedById.computeIfAbsent(id, k -> new HashSet<>()).add(related);
    }

    /**
     * permet de lire un fichier CSV complet pour importer les alignements
     *
     * @param in
     * @param headerSourceAlign
     * @return
     */
    public boolean readFileAlignment(Reader in, List<String> headerSourceAlign) {
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
                value = optionalColumn(csvRecord, COL_LOCAL_ID);
                if (value == null) {
                    continue;
                }
                nodeAlignmentImport.setLocalId(value);

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
            CSVRecord csvRecord, List<String> headerSourceAlign) {
        String uri1;
        ToolsHelper toolsHelper = new ToolsHelper();

        // types alignements 1=exactMatch ; 2=closeMatch ; 3=broadMatch ; 4=relatedMatch ; 5=narrowMatch
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
                // colonne d'alignement optionnelle absente
            }            
        }
        return nodeAlignmentImport;
    }

    private NodeAlignmentImport getAlignmentSource(NodeAlignmentImport nodeAlignmentImport, String source, String uri) {
        String[] valueType;
        // types alignements 1=exactMatch ; 2=closeMatch ; 3=broadMatch ; 4=relatedMatch ; 5=narrowMatch
        try {
            if (source != null && !source.isEmpty()) {
                NodeAlignmentSmall nodeAlignmentSmall = new NodeAlignmentSmall();
                nodeAlignmentSmall.setSource(source);

                //on récupère le type d'alignement (url##1)
                if (uri.contains("##")) {
                    valueType = uri.split("##");
                    if (valueType.length == 2) {
                        nodeAlignmentSmall.setUri_target(valueType[0]);
                        nodeAlignmentSmall.setAlignement_id_type(parseAlignmentTypeId(valueType[1]));
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
            // source d'alignement mal formée : ignorée
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
                idConcept = optionalColumn(csvRecord, COL_LOCAL_ID);
                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                conceptObject.setIdConcept(idConcept);

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
                idConcept = optionalColumn(csvRecord, COL_LOCAL_ID);
                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                nodeIdValue.setId(idConcept);

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
            // prefLabel optionnel absent
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
                idConcept = optionalColumn(csvRecord, COL_LOCAL_ID);
                if (idConcept == null || idConcept.isEmpty()) {
                    continue;
                }
                conceptObject.setIdConcept(idConcept);

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

            nodeReplaceValueByValues = new ArrayList<>();
            for (CSVRecord csvRecord : cSVParser) {
                addReplaceValueRecord(csvRecord, usedLangs);
            }
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private void addReplaceValueRecord(CSVRecord csvRecord, List<String> usedLangs) {
        String idConcept = localIdOrWarn(csvRecord);
        if (idConcept == null || idConcept.isEmpty()) {
            return;
        }
        for (String idLang1 : usedLangs) {
            addReplaceValueIfPresent(getValueAndPropertyPrefLabel(new NodeReplaceValueByValue(), csvRecord, idLang1), idConcept);
            addReplaceValueIfPresent(getValueAndPropertyAltLabel(new NodeReplaceValueByValue(), csvRecord, idLang1), idConcept);
            addReplaceValueIfPresent(getValueAndPropertyDefinition(new NodeReplaceValueByValue(), csvRecord, idLang1), idConcept);
        }
        addReplaceValueIfPresent(getValueAndPropertyBT(new NodeReplaceValueByValue(), csvRecord), idConcept);
    }

    private void addReplaceValueIfPresent(NodeReplaceValueByValue nodeReplaceValueByValue, String idConcept) {
        if (nodeReplaceValueByValue == null) {
            return;
        }
        nodeReplaceValueByValue.setIdConcept(idConcept);
        nodeReplaceValueByValues.add(nodeReplaceValueByValue);
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
            // ancienne valeur optionnelle absente
            nodeReplaceValueByValue.setOldValue(optionalColumn(csvRecord, "skos:altlabel@"+ idLang));
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
            // ancienne valeur optionnelle absente
            nodeReplaceValueByValue.setOldValue(optionalColumn(csvRecord, "skos:definition@"+ idLang)); 
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
    

    public boolean setLangs(Reader in) {
        langs = new ArrayList<>();
        customRelations = new ArrayList<>();        
        try {
            CSVParser cSVParser = headerFormat().parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            for (String columnName : headers.keySet()) {
                collectLangFromHeader(columnName);
                collectCustomRelationFromHeader(columnName);
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return !langs.isEmpty();
    }

    private void collectLangFromHeader(String columnName) {
        if (!columnName.contains("@")) {
            return;
        }
        String[] values = columnName.split("@");
        if (values[1] != null && !langs.contains(values[1])) {
            langs.add(values[1]);
        }
    }

    private void collectCustomRelationFromHeader(String columnName) {
        if (!columnName.contains("customRelationId")) {
            return;
        }
        String[] values = columnName.split(":");
        if (values.length < 2) {
            return;
        }
        if (values[1] != null && !customRelations.contains(values[1])) {
            customRelations.add(values[1]);
        }
    }

    public String getLangOfValue(Reader in) {
        String lang = null;
        try {
            CSVParser cSVParser = headerFormat().parse(in);
            Map<String, Integer> headers = cSVParser.getHeaderMap();

            String[] values;
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

            for (CSVRecord csvRecord : cSVParser) {
                ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();

                // setId, si l'identifiant n'est pas renseigné, on récupère un NULL 
                // puis on génère un nouvel identifiant
                uri1 = optionalColumn(csvRecord, "URI");
                conceptObject.setUri(uri1);
                uri1 = identifierOrKeep(csvRecord, uri1);
                assignListFileConceptId(conceptObject, csvRecord, uri1);

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

            StringBuilder missingIds = new StringBuilder(message);
            for (CSVRecord csvRecord : cSVParser) {
                addThesaurusRecord(csvRecord, readEmptyData, missingIds);
            }
            message = missingIds.toString();
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ThesaurusCsvReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private void addThesaurusRecord(CSVRecord csvRecord, boolean readEmptyData, StringBuilder missingIds) {
        ThesaurusCsvConceptObject conceptObject = new ThesaurusCsvConceptObject();
        String uri1 = optionalColumn(csvRecord, "URI");
        conceptObject.setUri(uri1);

        if (csvRecord.isMapped(COL_IDENTIFIER)) {
            assignIdFromIdentifierColumn(conceptObject, csvRecord, uri1);
        } else {
            assignIdFromUriColumn(conceptObject, csvRecord);
        }
        if (StringUtils.isEmpty(conceptObject.getIdConcept())) {
            missingIds.append("\nconcept sans Id : ").append(csvRecord);
            return;
        }

        getArkId(conceptObject, csvRecord);
        getLabels(conceptObject, csvRecord, readEmptyData);
        getNotes(conceptObject, csvRecord, readEmptyData);
        conceptObject.setType(getType(csvRecord));
        conceptObject.setConceptType(getConceptType(csvRecord));
        conceptObject.setNotation(getNotation(csvRecord));
        getRelations(conceptObject, csvRecord);
        getCustomRelations(conceptObject, csvRecord);
        getAlignments(conceptObject, csvRecord, readEmptyData);
        getGps(conceptObject, csvRecord);
        getGeoLocalisation(conceptObject, csvRecord, readEmptyData);
        populateTypedFields(conceptObject, csvRecord);
        getExternalResources(conceptObject, csvRecord);
        getDates(conceptObject, csvRecord);
        getFoafImages(conceptObject, csvRecord);
        conceptObjects.add(conceptObject);
    }

    private void populateTypedFields(ThesaurusCsvConceptObject conceptObject, CSVRecord csvRecord) {
        if ("skos:Concept".equalsIgnoreCase(conceptObject.getType())) {
            getMembers(conceptObject, csvRecord);
        }
        if ("skos:collection".equalsIgnoreCase(conceptObject.getType())) {
            getMembers(conceptObject, csvRecord);
        }
        if ("skos:Collection".equalsIgnoreCase(conceptObject.getType())) {
            getSubGroups(conceptObject, csvRecord);
        }
        if ("skos-thes:ThesaurusArray".equalsIgnoreCase(conceptObject.getType())) {
            getMembersOfFacet(conceptObject, csvRecord);
            getSuperOrdinate(conceptObject, csvRecord);
        }
        if ("skos:Concept".equalsIgnoreCase(conceptObject.getType())) {
            setDeprecatedConcept(conceptObject, csvRecord);
        }
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
        String[] values;
        
        NodeImage nodeImage = new NodeImage();
        try {
            values = value.split("@@");
            for (String value1 : values) {
                if (!StringUtils.isEmpty(value1)) {
                    applyImageToken(nodeImage, value1);
                }
            }
        } catch (Exception e) {
            // jeton d'image mal formé : ignoré
        }
        if(StringUtils.isEmpty(nodeImage.getUri())) return null;
        return nodeImage;
    }

    private void applyImageToken(NodeImage nodeImage, String value1) {
        if(Strings.CS.startsWith(value1, "rdf:about=")){
            nodeImage.setUri(StringUtils.substringAfter(value1, "rdf:about="));
        }
        if(Strings.CS.startsWith(value1, "dcterms:rights=")){
            nodeImage.setCopyRight(StringUtils.substringAfter(value1, "dcterms:rights="));
        }
        if(Strings.CS.startsWith(value1, "dcterms:title=")){
            nodeImage.setImageName(StringUtils.substringAfter(value1, "dcterms:title="));
        }
        if(Strings.CS.startsWith(value1, "dcterms:creator=")){
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
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        String id = extractIdFromQuery(uri, "idf=");
        if (id == null) {
            id = extractIdFromQuery(uri, "idg=");
        }
        if (id == null) {
            id = extractIdFromQuery(uri, "idc=");
        }
        if (id == null) {
            id = extractIdFromPath(uri);
        }
        return fr.cnrs.opentheso.utils.StringUtils.normalizeStringForIdentifier(id);
    }

    private String extractIdFromQuery(String uri, String key) {
        if (!uri.contains(key)) {
            return null;
        }
        if (uri.contains("&")) {
            return uri.substring(uri.indexOf(key) + key.length(), uri.indexOf("&"));
        }
        return uri.substring(uri.indexOf(key) + key.length(), uri.length());
    }

    private String extractIdFromPath(String uri) {
        if (uri.contains("#")) {
            return uri.substring(uri.indexOf("#") + 1, uri.length());
        }
        if (uri.contains("ark:/")) {
            return uri.substring(uri.indexOf("ark:/") + 5, uri.length());
        }
        return uri.substring(uri.lastIndexOf("/") + 1, uri.length());
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
            // date de création optionnelle absente
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
            // date de modification optionnelle absente
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
        return getMembersOfFacet(conceptObject, csvRecord);
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
            // arkId optionnel absent
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
