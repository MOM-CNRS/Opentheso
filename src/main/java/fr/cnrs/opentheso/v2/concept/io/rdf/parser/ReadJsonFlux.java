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

import java.util.regex.Pattern;

public class ReadJsonFlux extends AbstractRDFHandler {

    private String FACET_PATTERN = "^http://localhost:8080/opentheso2/\\?idf=F\\d+&idt=th\\d+$";
    private String CONCEPT_PATTERN = "^https://opentheso2\\.mom\\.fr/ark:/66666/th\\d+/\\?idg=G\\d+&idt=th\\d+/\\?idc=\\d+&idt=th\\d+$";
    private String COLLECTION_PATTERN = "^https://opentheso2\\.mom\\.fr/ark:/66666/th\\d+/\\?idg=G\\d+&idt=th\\d+/\\?idg=G\\d+&idt=th\\d+$";
    private String THESORUS_PATTERN = "^https://opentheso2\\.mom\\.fr/ark:/66666/th\\d+/\\?idg=G\\d+&idt=th\\d+/66666/th\\d+$";
    private final static String IMAGE_TAG = "http://xmlns.com/foaf/0.1/Image";

    private SKOSXmlDocument skosXmlDocument;
    private Resource subject;
    private IRI predicate;
    private Value value;
    private boolean isConceptScheme;
    private boolean isCollection;
    private boolean isFacet;
    private boolean isConcept;
    private boolean isImage;
    private String currentSubject;
    private String defaultLang;
    private SKOSResource skosResource;
    private ThesaurusManager fileManager = new ThesaurusManager();
    private ConceptReader conceptReader = new ConceptReader();
    private GenericReader genericReader = new GenericReader();
    private Pattern thesorusRegex, facetRegex, collectionRegex, conceptRegex;

    public ReadJsonFlux(SKOSXmlDocument skosXmlDocument, String defaultLang) {
        this.defaultLang = defaultLang;
        this.skosXmlDocument = skosXmlDocument;

        facetRegex = Pattern.compile(FACET_PATTERN);
        thesorusRegex = Pattern.compile(THESORUS_PATTERN);
        conceptRegex = Pattern.compile(CONCEPT_PATTERN);
        collectionRegex = Pattern.compile(COLLECTION_PATTERN);
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {

        predicate = st.getPredicate();
        value = st.getObject();
        subject = st.getSubject();

        if (!subject.stringValue().equals(currentSubject)) {
            addNewResources();

            skosResource = new SKOSResource();
            skosResource.setUri(subject.stringValue());

            if (thesorusRegex.matcher(subject.stringValue()).matches()) {
                isConceptScheme = true;
            } else if (collectionRegex.matcher(subject.stringValue()).matches()) {
                isCollection = true;
            } else if (facetRegex.matcher(subject.stringValue()).matches()) {
                isFacet = true;
            } else if (conceptRegex.matcher(subject.stringValue()).matches()) {
                isConcept = true;
            } else if (subject.stringValue().startsWith("https://") || subject.stringValue().startsWith("http://")) {
                isImage = true;
            }
        }

        currentSubject = subject.stringValue();

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
            // Pour traiter les lignes d'un concept qui ne contiennent pas une langue
            conceptReader.readConcept(skosXmlDocument, skosResource, predicate, value, literal);
        }
    }

    @Override
    public void endRDF() throws RDFHandlerException {
        // On execute ce cas à la fin de la lecture du fichier RDF :
        // on doit enregistrer le dernier object skosResource (la dernière balise lu)
        addNewResources();
    }

    private void addNewResources() {
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
        } else  if (isCollection) {
            isCollection = false;
            fileManager.addStructData(skosResource, skosXmlDocument, SKOSProperty.COLLECTION);
        } else  if (isImage) {
            isImage = false;
            fileManager.addStructData(skosResource, skosXmlDocument, SKOSProperty.FOAF_IMAGE);
        }
    }
}
