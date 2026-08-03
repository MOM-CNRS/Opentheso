package fr.cnrs.opentheso.services.imports.rdf4j;

import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.utils.DateUtils;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import org.apache.commons.lang3.StringUtils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;



public class GenericReader {

    public void setThesaurusData(SKOSResource skosConcept, IRI predicate, Literal literal, String lang) {

        switch (predicate.getLocalName()) {
            case "prefLabel":
                skosConcept.addLabel(literal.getLabel(), lang, SKOSProperty.PREF_LABEL);
                break;
            case "title":
                skosConcept.getThesaurus().setTitle(literal.getLabel());
                break;
            case "creator":
                skosConcept.getThesaurus().setCreator(literal.getLabel());
                break;
            case "contributor":
                skosConcept.getThesaurus().setContributor(literal.getLabel());
                break;
            case "publisher":
                skosConcept.getThesaurus().setPublisher(literal.getLabel());
                break;
            case "description":
                skosConcept.getThesaurus().setDescription(literal.getLabel());
                break;
            case "type":
                skosConcept.getThesaurus().setType(literal.getLabel());
                break;
            case "rights":
                skosConcept.getThesaurus().setRights(literal.getLabel());
                break;
            case "subject":
                skosConcept.getThesaurus().setSubject(literal.getLabel());
                break;
            case "coverage":
                skosConcept.getThesaurus().setCoverage(literal.getLabel());
                break;
            case "language":
                skosConcept.getThesaurus().setLanguage(literal.getLabel());
                break;
            case "relation":
                skosConcept.getThesaurus().setRelation(literal.getLabel());
                break;
            case "source":
                skosConcept.getThesaurus().setSource(literal.getLabel());
                break;
            case "created":
                skosConcept.getThesaurus().setCreated(new DateUtils().getDateFromString(literal.getLabel()));
                break;
            case "modified":
                skosConcept.getThesaurus().setModified(new DateUtils().getDateFromString(literal.getLabel()));
                break;
            case "thesaurusRole":
                skosConcept.getThesaurus().setThesaurusRole(literal.getLabel());
                break;
                
            /// ajout des créativecommons
            /// ajout des dcterms
        }
        if(StringUtils.contains(predicate.getNamespace(), "purl.org/dc/terms")){
            String dcLang;
            try {
                switch (literal.getDatatype().getLocalName()) {
                    case DCMIResource.TYPE_DATE:
                        skosConcept.getThesaurus().addDcElement(new DcElement(predicate.getLocalName(), literal.getLabel(), null, DCMIResource.TYPE_DATE));                        
                        break;
                    case DCMIResource.TYPE_LANGUE:
                        dcLang = literal.getLanguage().get(); literal.getDatatype().getLocalName();
                        skosConcept.getThesaurus().addDcElement(new DcElement(predicate.getLocalName(), literal.getLabel(), dcLang, null));
                        break;       
                    case DCMIResource.TYPE_STRING:
                        skosConcept.getThesaurus().addDcElement(new DcElement(predicate.getLocalName(), literal.getLabel(), null, DCMIResource.TYPE_STRING));                         
                        break;                         
                    default:
                        break;
                }
                
            } catch (Exception e) {
            }
        }
    }

    public void setFoatImageDatas(SKOSResource skosResource, Literal literal, IRI predicate) {

        switch(predicate.getLocalName()) {
            case "identifier":
                skosResource.setIdentifier(literal.getLabel());
                break;
            case "title":
                skosResource.getFoafImage().setImageName(literal.getLabel());
                break;
            case "creator":
                skosResource.getFoafImage().setCreator(literal.getLabel());
                break;                
            case "rights":
                skosResource.getFoafImage().setCopyRight(literal.getLabel());
                break;
        }
    }

}
