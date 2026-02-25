package fr.cnrs.opentheso.services.imports.rdf4j;

import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class ConceptReader {

    public void readGeneric(SKOSXmlDocument sKOSXmlDocument, SKOSResource skosConcept, IRI predicate, Literal literal, String lang) {

        switch (predicate.getLocalName()) {
            case "prefLabel":
                skosConcept.addLabel(literal.getLabel(), lang, SKOSProperty.PREF_LABEL);
                break;
            case "altLabel":
                skosConcept.addLabel(literal.getLabel(), lang, SKOSProperty.ALT_LABEL);
                break;
            case "hiddenLabel":
                skosConcept.addLabel(literal.getLabel(), lang, SKOSProperty.HIDDEN_LABEL);
                break;

            case "definition":
                skosConcept.addDocumentation(literal.getLabel(), lang, SKOSProperty.DEFINITION);
                break;
            case "scopeNote":
                skosConcept.addDocumentation(literal.getLabel(), lang, SKOSProperty.SCOPE_NOTE);
                break;
            case "example":
                skosConcept.addDocumentation(literal.getLabel(), lang, SKOSProperty.EXAMPLE);
                break;
            case "historyNote":
                skosConcept.addDocumentation(literal.getLabel(), lang, SKOSProperty.HISTORY_NOTE);
                break;
            case "editorialNote":
                skosConcept.addDocumentation(literal.getLabel(), lang, SKOSProperty.EDITORIAL_NOTE);
                break;
            case "changeNote":
                skosConcept.addDocumentation(literal.getLabel(), lang, SKOSProperty.CHANGE_NOTE);
                break;
            case "note":
                skosConcept.addDocumentation(literal.getLabel(), lang, SKOSProperty.NOTE);
                break;
            //DcSource
            case "source":
                skosConcept.addDcRelations(literal.getLabel());
                break;

            //Image
            case "Image":
                NodeImage nodeImage = new NodeImage();
                nodeImage.setImageName("");
                nodeImage.setCopyRight("");
                nodeImage.setUri(literal.getLabel());
                skosConcept.addNodeImage(nodeImage);
                break;

            //Replaces
            case "replaces":
                skosConcept.addReplaces(literal.getLabel(), SKOSProperty.REPLACES);
                break;
            case "isReplacedBy":
                skosConcept.addReplaces(literal.getLabel(), SKOSProperty.IS_REPLACED_BY);
                break;

            //deprecated
            case "deprecated":
                skosConcept.setStatus(SKOSProperty.DEPRECATED);
                break;

            //GPSCoordinates
            case "P625":
                if (literal.getLabel().startsWith("Point")) {
                    Pattern pattern = Pattern.compile("Point\\((-?\\d+\\.\\d+) (-?\\d+\\.\\d+)\\)");
                    Matcher matcher = pattern.matcher(literal.getLabel());

                    if (matcher.find()) {
                        SKOSGPSCoordinates element = new SKOSGPSCoordinates();
                        element.setLat(matcher.group(1));
                        element.setLon(matcher.group(2));
                        skosConcept.getGpsCoordinates().add(element);
                    }
                } else {
                    Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
                    Matcher matcher = pattern.matcher(literal.getLabel());

                    while (matcher.find()) {
                        String coordinates = matcher.group(1).substring(1);
                        String[] parts = coordinates.split(", ");

                        for (String part : parts) {
                            String[] values = part.split(" ");
                            if (values.length == 2) {
                                SKOSGPSCoordinates element = new SKOSGPSCoordinates();
                                element.setLat(values[0]);
                                element.setLon(values[1]);
                                skosConcept.getGpsCoordinates().add(element);
                            }
                        }
                    }
                }
                break;
            case "lat":
                if (skosConcept.getGpsCoordinates().stream().anyMatch(element -> element.getLat() == null)) {
                    for (SKOSGPSCoordinates element : skosConcept.getGpsCoordinates()) {
                        if (element.getLat() == null) {
                            element.setLat(literal.getLabel());
                            break;
                        }
                    }
                } else {
                    SKOSGPSCoordinates element = new SKOSGPSCoordinates();
                    element.setLat(literal.getLabel());
                    skosConcept.getGpsCoordinates().add(element);
                }
                break;

            case "long":
                if (skosConcept.getGpsCoordinates().stream().anyMatch(element -> element.getLon() == null)) {
                    for (SKOSGPSCoordinates element : skosConcept.getGpsCoordinates()) {
                        if (element.getLon() == null) {
                            element.setLon(literal.getLabel());
                            break;
                        }
                    }
                } else {
                    SKOSGPSCoordinates element = new SKOSGPSCoordinates();
                    element.setLon(literal.getLabel());
                    skosConcept.getGpsCoordinates().add(element);
                }
                break;
            //Notation
            case "notation":
                skosConcept.addNotation(literal.getLabel());
                break;
            //identifier
            case "identifier":
                skosConcept.setIdentifier(literal.getLabel());
                sKOSXmlDocument.getEquivalenceUriArkHandle().put(skosConcept.getUri(), literal.getLabel());
                break;

            case "created":
                skosConcept.addDate(literal.getLabel(), SKOSProperty.CREATED);
                break;
            case "modified":
                skosConcept.addDate(literal.getLabel(), SKOSProperty.MODIFIED);
                break;

            case "creator":
                skosConcept.addAgent(literal.getLabel(), SKOSProperty.CREATOR);
                break;
            case "contributor":
                skosConcept.addAgent(literal.getLabel(), SKOSProperty.CONTRIBUTOR);
                break;
        }
    }


    public void readConcept(SKOSXmlDocument sKOSXmlDocument, SKOSResource skosConcept, IRI predicate, Value value, Literal literal) {
        try {


            switch (predicate.getLocalName()) {
                //readDate
                case "created":
                    if (literal != null)
                        skosConcept.addDate(literal.getLabel(), SKOSProperty.CREATED);
                    break;
                case "modified":
                    if (literal != null)
                        skosConcept.addDate(literal.getLabel(), SKOSProperty.MODIFIED);
                    break;
                case "date":
                    if (literal != null)
                        skosConcept.addDate(literal.getLabel(), SKOSProperty.DATE);
                    break;

                //readRelationships
                case "broader":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.BROADER);
                    break;
                case "broaderGeneric":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.BROADER_GENERIC);
                    break;
                case "broaderInstantial":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.BROADER_INSTANTIAL);
                    break;
                case "broaderPartitive":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.BROADER_PARTITIVE);
                    break;
                case "narrower":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.NARROWER);
                    break;
                case "narrowerGeneric":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.NARROWER_GENERIC);
                    break;
                case "narrowerInstantial":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.NARROWER_INSTANTIAL);
                    break;
                case "narrowerPartitive":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.NARROWER_PARTITIVE);
                    break;
                case "related":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.RELATED);
                    break;
                case "relatedHasPart":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.RELATED_HAS_PART);
                    break;
                case "relatedPartOf":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.RELATED_PART_OF);
                    break;
                case "hasTopConcept":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.HAS_TOP_CONCEPT);
                    break;
                case "inScheme":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.INSCHEME);
                    break;
                case "member":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.MEMBER);
                    break;
                case "topConceptOf":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.TOP_CONCEPT_OF);
                    break;
                case "microThesaurusOf":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.MICROTHESAURUS_OF);
                    break;
                case "subGroup":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.SUBGROUP);
                    break;
                case "superGroup":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.SUPERGROUP);
                    break;
                case "hasMainConcept":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.HAS_MAIN_CONCEPT);
                    break;
                case "memberOf":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.MEMBER_OF);
                    break;
                case "mainConceptOf":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.MAIN_CONCEPT_OF);
                    break;
                case "superOrdinate":
                    skosConcept.addRelation("", value.stringValue(), SKOSProperty.SUPER_ORDINATE);
                    break;

                //Creator
                case "creator":
                    // Attention au Null
                    if (literal != null)
                        skosConcept.addAgent(literal.getLabel(), SKOSProperty.CREATOR);
                    break;
                case "contributor":
                    if (literal != null)
                        skosConcept.addAgent(literal.getLabel(), SKOSProperty.CONTRIBUTOR);
                    break;

                //Notation
                case "notation":
                    if (literal != null)
                        skosConcept.addNotation(literal.getLabel());
                    break;

                //identifier
                case "identifier":
                    if (literal != null) {
                        skosConcept.setIdentifier(literal.getLabel());
                        sKOSXmlDocument.getEquivalenceUriArkHandle().put(skosConcept.getUri(), literal.getLabel());
                    }
                    break;

                //Match
                case "exactMatch":
                    skosConcept.addMatch(value.stringValue(), SKOSProperty.EXACT_MATCH);
                    break;
                case "closeMatch":
                    skosConcept.addMatch(value.stringValue(), SKOSProperty.CLOSE_MATCH);
                    break;
                case "broadMatch":
                    skosConcept.addMatch(value.stringValue(), SKOSProperty.BROAD_MATCH);
                    break;
                case "relatedMatch":
                    skosConcept.addMatch(value.stringValue(), SKOSProperty.RELATED_MATCH);
                    break;
                case "narrowMatch":
                    skosConcept.addMatch(value.stringValue(), SKOSProperty.NARROWER_MATCH);
                    break;

                //Image
                case "Image":
                    NodeImage nodeImage = new NodeImage();
                    nodeImage.setImageName("");
                    nodeImage.setCopyRight("");
                    nodeImage.setUri(value.stringValue());
                    skosConcept.addNodeImage(nodeImage);
                    break;

                //Replaces
                case "replaces":
                    skosConcept.addReplaces(value.stringValue(), SKOSProperty.REPLACES);
                    break;
                case "isReplacedBy":
                    skosConcept.addReplaces(value.stringValue(), SKOSProperty.IS_REPLACED_BY);
                    break;

                //deprecated
                case "deprecated":
                    skosConcept.setStatus(SKOSProperty.DEPRECATED);
                    break;
            }

        } catch (Exception e) {
            log.error("Error while reading concept " + skosConcept.getUri() + value + " : " + e.getMessage());
            throw e;
        }


        if (StringUtils.contains(predicate.getNamespace(), "purl.org/dc/terms")) {
            if (predicate.isResource()) {
                skosConcept.getThesaurus().addDcElement(new DcElement(predicate.getLocalName(), value.stringValue(), null, DCMIResource.TYPE_RESOURCE));
            }
        }
    }
}
