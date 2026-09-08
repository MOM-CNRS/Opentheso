package fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.parser;

import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class ConceptReader {

    public void readGeneric(SKOSXmlDocument sKOSXmlDocument, SKOSResource skosConcept, IRI predicate, Literal literal, String lang) {
        String localName = predicate.getLocalName();
        readGenericLabels(skosConcept, localName, literal, lang);
        readGenericNotes(skosConcept, localName, literal, lang);
        readGenericOther(sKOSXmlDocument, skosConcept, localName, literal);
    }

    private void readGenericLabels(SKOSResource skosConcept, String localName, Literal literal, String lang) {
        switch (localName) {
            case "prefLabel":
                skosConcept.addLabel(literal.getLabel(), lang, SKOSProperty.PREF_LABEL);
                break;
            case "altLabel":
                skosConcept.addLabel(literal.getLabel(), lang, SKOSProperty.ALT_LABEL);
                break;
            case "hiddenLabel":
                skosConcept.addLabel(literal.getLabel(), lang, SKOSProperty.HIDDEN_LABEL);
                break;
            default:
                break;
        }
    }

    private void readGenericNotes(SKOSResource skosConcept, String localName, Literal literal, String lang) {
        switch (localName) {
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
            case "source":
                skosConcept.addDcRelations(literal.getLabel());
                break;
            default:
                break;
        }
    }

    private void readGenericOther(SKOSXmlDocument sKOSXmlDocument, SKOSResource skosConcept, String localName, Literal literal) {
        switch (localName) {
            case "Image":
                NodeImage nodeImage = new NodeImage();
                nodeImage.setImageName("");
                nodeImage.setCopyRight("");
                nodeImage.setUri(literal.getLabel());
                skosConcept.addNodeImage(nodeImage);
                break;
            case "replaces":
                skosConcept.addReplaces(literal.getLabel(), SKOSProperty.REPLACES);
                break;
            case "isReplacedBy":
                skosConcept.addReplaces(literal.getLabel(), SKOSProperty.IS_REPLACED_BY);
                break;
            case "deprecated":
                skosConcept.setStatus(SKOSProperty.DEPRECATED);
                break;
            case "P625":
                readGenericGpsPoint(skosConcept, literal.getLabel());
                break;
            case "lat":
                applyGenericLatitude(skosConcept, literal.getLabel());
                break;
            case "long":
                applyGenericLongitude(skosConcept, literal.getLabel());
                break;
            case "notation":
                skosConcept.addNotation(literal.getLabel());
                break;
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
            default:
                break;
        }
    }

    private void readGenericGpsPoint(SKOSResource skosConcept, String label) {
        if (label.startsWith("Point")) {
            Matcher matcher = Pattern.compile("Point\\((-?\\d+\\.\\d+) (-?\\d+\\.\\d+)\\)").matcher(label);
            if (matcher.find()) {
                SKOSGPSCoordinates element = new SKOSGPSCoordinates();
                element.setLat(matcher.group(1));
                element.setLon(matcher.group(2));
                skosConcept.getGpsCoordinates().add(element);
            }
            return;
        }
        Matcher matcher = Pattern.compile("\\(([^)]+)\\)").matcher(label);
        while (matcher.find()) {
            addGenericGpsParts(skosConcept, matcher.group(1).substring(1).split(", "));
        }
    }

    private void addGenericGpsParts(SKOSResource skosConcept, String[] parts) {
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

    private void applyGenericLatitude(SKOSResource skosConcept, String latitude) {
        if (skosConcept.getGpsCoordinates().stream().anyMatch(element -> element.getLat() == null)) {
            for (SKOSGPSCoordinates element : skosConcept.getGpsCoordinates()) {
                if (element.getLat() == null) {
                    element.setLat(latitude);
                    return;
                }
            }
            return;
        }
        SKOSGPSCoordinates element = new SKOSGPSCoordinates();
        element.setLat(latitude);
        skosConcept.getGpsCoordinates().add(element);
    }

    private void applyGenericLongitude(SKOSResource skosConcept, String longitude) {
        if (skosConcept.getGpsCoordinates().stream().anyMatch(element -> element.getLon() == null)) {
            for (SKOSGPSCoordinates element : skosConcept.getGpsCoordinates()) {
                if (element.getLon() == null) {
                    element.setLon(longitude);
                    return;
                }
            }
            return;
        }
        SKOSGPSCoordinates element = new SKOSGPSCoordinates();
        element.setLon(longitude);
        skosConcept.getGpsCoordinates().add(element);
    }


    public void readConcept(SKOSXmlDocument sKOSXmlDocument, SKOSResource skosConcept, IRI predicate, Value value, Literal literal) {
        try {
            String localName = predicate.getLocalName();
            readConceptDates(skosConcept, localName, literal);
            readConceptRelations(skosConcept, localName, value);
            readConceptMetadata(sKOSXmlDocument, skosConcept, localName, value, literal);
        } catch (Exception e) {
            log.error("Error while reading concept " + skosConcept.getUri() + value + " : " + e.getMessage());
            throw e;
        }


        if (Strings.CS.contains(predicate.getNamespace(), "purl.org/dc/terms") && predicate.isResource()) {
            skosConcept.getThesaurus().addDcElement(new DcElement(predicate.getLocalName(), value.stringValue(), null, DCMIResource.TYPE_RESOURCE));
        }
    }

    private void readConceptDates(SKOSResource skosConcept, String localName, Literal literal) {
        switch (localName) {
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
            default:
                break;
        }
    }

    private void readConceptRelations(SKOSResource skosConcept, String localName, Value value) {
        switch (localName) {
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
            default:
                break;
        }
    }

    private void readConceptMetadata(SKOSXmlDocument sKOSXmlDocument, SKOSResource skosConcept, String localName, Value value, Literal literal) {
        switch (localName) {
            case "creator":
                // Attention au Null
                if (literal != null)
                    skosConcept.addAgent(literal.getLabel(), SKOSProperty.CREATOR);
                break;
            case "contributor":
                if (literal != null)
                    skosConcept.addAgent(literal.getLabel(), SKOSProperty.CONTRIBUTOR);
                break;

            case "notation":
                if (literal != null)
                    skosConcept.addNotation(literal.getLabel());
                break;

            case "identifier":
                if (literal != null) {
                    skosConcept.setIdentifier(literal.getLabel());
                    sKOSXmlDocument.getEquivalenceUriArkHandle().put(skosConcept.getUri(), literal.getLabel());
                }
                break;

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

            case "Image":
                NodeImage nodeImage = new NodeImage();
                nodeImage.setImageName("");
                nodeImage.setCopyRight("");
                nodeImage.setUri(value.stringValue());
                skosConcept.addNodeImage(nodeImage);
                break;
            case "image":
                NodeImage nodeImage2 = new NodeImage();
                nodeImage2.setImageName("");
                nodeImage2.setCopyRight("");
                nodeImage2.setUri(value.stringValue());
                skosConcept.addNodeImage(nodeImage2);
                break;

            case "replaces":
                skosConcept.addReplaces(value.stringValue(), SKOSProperty.REPLACES);
                break;
            case "isReplacedBy":
                skosConcept.addReplaces(value.stringValue(), SKOSProperty.IS_REPLACED_BY);
                break;

            case "deprecated":
                skosConcept.setStatus(SKOSProperty.DEPRECATED);
                break;
            default:
                break;
        }
    }
}
