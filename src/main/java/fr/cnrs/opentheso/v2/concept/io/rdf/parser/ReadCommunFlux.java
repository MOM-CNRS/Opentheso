package fr.cnrs.opentheso.v2.concept.io.rdf.parser;

import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.parser.ConceptReader;
import fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.parser.GenericReader;
import fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.parser.ThesaurusManager;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

public class ReadCommunFlux extends AbstractRDFHandler {

    private final static String RESOURCE_TAG = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private final static String CONCEPT_SCHEME_TAG = "http://www.w3.org/2004/02/skos/core#ConceptScheme";
    private final static String COLLECTION_TAG = "http://www.w3.org/2004/02/skos/core#Collection";
    private final static String FACET_TAG = "http://purl.org/iso25964/skos-thes#ThesaurusArray";
    private final static String CONCEPT_TAG = "http://www.w3.org/2004/02/skos/core#Concept";
    private final static String IMAGE_TAG = "http://xmlns.com/foaf/0.1/Image";

    private SKOSXmlDocument skosXmlDocument;
    private Resource subject, subjectTmp;
    private IRI predicate;
    private Value value;
    private boolean isConceptScheme;
    private boolean isCollection;
    private boolean isFacet;
    private boolean isConcept;
    private boolean isImage;
    private String defaultLang;
    private SKOSResource skosResource;
    private ThesaurusManager fileManager = new ThesaurusManager();
    private ConceptReader conceptReader = new ConceptReader();
    private GenericReader genericReader = new GenericReader();

    public ReadCommunFlux(SKOSXmlDocument skosXmlDocument, String defaultLang) {
        this.defaultLang = defaultLang;
        this.skosXmlDocument = skosXmlDocument;
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        // On commence par lire la première ligne d'un bloc de type "rdf:Description"
        predicate = st.getPredicate();
        value = st.getObject();
        subject = st.getSubject();

        // On teste si on est au niveau du début/fin d'une balise de type "rdf:Description"
        if ((subjectTmp == null || (subjectTmp != null && !subject.stringValue().equals(subjectTmp.stringValue()))) && RESOURCE_TAG.equalsIgnoreCase(predicate.stringValue())) {
        //if (RESOURCE_TAG.equalsIgnoreCase(predicate.stringValue())) {
            // Ajouter l'objet SKOSResource dans SKOSXmlDocument
            // Ce code est executé à la fin de chaque balise "rdf:Description"
            addNewResources();
            // Définir la nature de l'élément "rdf:Description"
            // Ce code est executé au début de chaque balise "rdf:Description"
            // initialisation de l'objet Bloc au début d'un nouveau bloc
            if (!isConceptScheme && !isFacet && !isConcept && !isCollection && !isImage) {
                skosResource = new SKOSResource();
                skosResource.setUri(subject.stringValue());
                switch(value.stringValue()) {
                    case CONCEPT_SCHEME_TAG:
                        isConceptScheme = true;
                        break;
                    case COLLECTION_TAG:
                        isCollection = true;
                        break;
                    case FACET_TAG:
                        isFacet = true;
                        break;
                    case CONCEPT_TAG:
                        isConcept = true;
                        break;
                    case IMAGE_TAG:
                        isImage = true;
                }
            }
        }

        String lang = defaultLang;
        Literal literal = null;
        if (value instanceof Literal) {
            // Si la ligne en cours contient une langue spécifique, on récupère la langue dans
            literal = (Literal) value;
            if (ObjectUtils.isNotEmpty(literal) && literal.getLanguage().isPresent()) {
                lang = literal.getLanguage().get();
            }

            if (isConceptScheme) {
                // Dans le cas d'un conceptScheme
                genericReader.setThesaurusData(skosResource, predicate, literal, lang);
            } else if (isImage) {
                // Dans le cas d'une image
                genericReader.setFoatImageDatas(skosResource, literal, predicate);
            } else {
                // Dans le cas d'un Concept, Collection, ..
                conceptReader.readGeneric(skosXmlDocument, skosResource, predicate, literal, lang);
            }
        } else {
            // Pour traiter les lignes d'un bloc qui ne contiennent pas une langue, le bloc peut être de plusieurs type
            conceptReader.readConcept(skosXmlDocument, skosResource, predicate, value, literal);
        }
        subjectTmp = subject;
    }

    @Override
    public void endRDF() throws RDFHandlerException {
        // On execute ce cas à la fin de la lecture du fichier RDF :
        // on doit enregistrer le dernier object skosResource (la dernière balise lu)
        addNewResources();
    }

    private void addNewResources() {
        // finalisation de l'objet lu suivant le type de l'objet et démarrage d'un nouvel objet
        if (isConceptScheme) {
            isConceptScheme = false;
            skosXmlDocument.setTitle(subject.stringValue());
            fileManager.addStructData(skosResource, skosXmlDocument, SKOSProperty.CONCEPT_SCHEME);
        } else if (isFacet) {
            isFacet = false;
            fileManager.addStructData(skosResource, skosXmlDocument, SKOSProperty.FACET);
        } else if (isConcept) {
            isConcept = false;
            fileManager.addStructData(skosResource, skosXmlDocument, SKOSProperty.CONCEPT);
        } else if (isCollection) {
            isCollection = false;
            fileManager.addStructData(skosResource, skosXmlDocument, SKOSProperty.COLLECTION);
        } else if (isImage) {
            isImage = false;
            fileManager.addStructData(skosResource, skosXmlDocument, SKOSProperty.FOAF_IMAGE);
        }
    }
}
