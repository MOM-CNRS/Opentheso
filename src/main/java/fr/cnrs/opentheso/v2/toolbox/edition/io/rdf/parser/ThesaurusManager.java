package fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.parser;

import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import org.eclipse.rdf4j.model.Resource;


public class ThesaurusManager {


    public void addStructData(SKOSResource skosResource, SKOSXmlDocument sKOSXmlDocument, int prop) {

        skosResource.setProperty(prop);

        switch (prop) {
            case SKOSProperty.CONCEPT_SCHEME:
                sKOSXmlDocument.setConceptScheme(skosResource);
                break;
            case SKOSProperty.FACET:
                sKOSXmlDocument.addFacet(skosResource);
                break;
            case SKOSProperty.CONCEPT_GROUP:
            case SKOSProperty.COLLECTION:
            case SKOSProperty.THEME:
            case SKOSProperty.MICROTHESAURUS:
                sKOSXmlDocument.addGroup(skosResource);
                break;
            case SKOSProperty.CONCEPT:
                sKOSXmlDocument.addconcept(skosResource);
                break;
            case SKOSProperty.FOAF_IMAGE:
                sKOSXmlDocument.addFoafImage(skosResource);
                break;
        }
    }
}
