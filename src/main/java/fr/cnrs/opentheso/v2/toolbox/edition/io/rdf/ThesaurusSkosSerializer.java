package fr.cnrs.opentheso.v2.toolbox.edition.io.rdf;

import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.nodes.NodeImage;

import fr.cnrs.opentheso.models.skosapi.SKOSAgent;
import fr.cnrs.opentheso.models.skosapi.SKOSDate;
import fr.cnrs.opentheso.models.skosapi.SKOSDiscussion;
import fr.cnrs.opentheso.models.skosapi.SKOSDocumentation;
import fr.cnrs.opentheso.models.skosapi.SKOSGPSCoordinates;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSMatch;
import fr.cnrs.opentheso.models.skosapi.SKOSNotation;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSRelation;
import fr.cnrs.opentheso.models.skosapi.SKOSReplaces;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSVote;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;

import fr.cnrs.opentheso.utils.ToolsHelper;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.AbstractNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.List;

@Data
public class ThesaurusSkosSerializer {

    public final static String DELIMINATE = "##";
    public final static String STATUS_TAG = "status";
    public final static String DISCUSSION_TAG = "message";
    public final static String VOTE_TAG = "vote";

    private Model model;
    private ModelBuilder builder;
    private SKOSXmlDocument xmlDocument;
    private ValueFactory vf;

    
    public ThesaurusSkosSerializer(SKOSXmlDocument xmlDocument) {
        this.xmlDocument = xmlDocument;
        vf = SimpleValueFactory.getInstance();
        loadModel();
        writeModel();
        model = builder.build();
    }

    public void closeCache() {
        model.clear();
        model = null;
        vf = null;
        builder = null;
    }

    private void loadModel() {
        builder = new ModelBuilder();
        builder.setNamespace("skos", "http://www.w3.org/2004/02/skos/core#");
        builder.setNamespace("dc", "http://purl.org/dc/elements/1.1/");
        builder.setNamespace("dcterms", "http://purl.org/dc/terms/");
        builder.setNamespace("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
        builder.setNamespace("iso-thes", "http://purl.org/iso25964/skos-thes#");
        builder.setNamespace("opentheso", "http://purl.org/umu/uneskos#");
        builder.setNamespace("foaf", "http://xmlns.com/foaf/0.1/");
        builder.setNamespace("owl", "http://www.w3.org/2002/07/owl#");
        builder.setNamespace("wdt", "http://www.opengis.net/ont/geosparql#");
    }

    private void writeModel() {
        writeConceptScheme();
        writeGroup();
        writeFacet();
        writeConcept();
    }

    private void writeConcept() {
        for (SKOSResource sResource : xmlDocument.getConceptList()) {
            builder.subject(vf.createIRI(sResource.getUri()));
            builder.add(RDF.TYPE, SKOS.CONCEPT);
            writeLabel(sResource);
            writeRelation(sResource);
            writeMatch(sResource);
            writeNotation(sResource);
            writeDate(sResource);
            writeIdentifier(sResource);
            writePath(sResource);
            writeAgent(sResource);
            writeDocumentation(sResource);
            writeGPS(sResource);
            writeExternalResources(sResource);
            writeStatusCandidat(sResource);
            writeDiscussions(sResource);
            writeVotes(sResource);
            writeStatus(sResource);
            writeReplaces(sResource);
            writeNodeImage(sResource);
        }
    }

    private void writeNodeImage(SKOSResource resource) {
        for (NodeImage nodeImage : resource.getNodeImages()) {
            if (StringUtils.isEmpty(nodeImage.getUri())) {
                continue;
            }

            builder.subject(vf.createIRI(nodeImage.getUri().replaceAll(" ", "%20")));
            builder.add(RDF.TYPE, FOAF.IMAGE);
            builder.add(DCTERMS.IDENTIFIER, resource.getSdc().getIdentifier());
            if (!StringUtils.isEmpty(nodeImage.getImageName())) {
                builder.add(DCTERMS.TITLE, nodeImage.getImageName());
            }
            if (!StringUtils.isEmpty(nodeImage.getCopyRight())) {
                builder.add(DCTERMS.RIGHTS, nodeImage.getCopyRight());
            }
            if (!StringUtils.isEmpty(nodeImage.getCreator())) {
                builder.add(DCTERMS.CREATOR, nodeImage.getCreator());
            }            
        }
    }

    private void writeReplaces(SKOSResource resource) {
        for (SKOSReplaces replace : resource.getsKOSReplaces()) {
            switch (replace.getProperty()) {
                case SKOSProperty.IS_REPLACED_BY:
                    builder.add(DCTERMS.IS_REPLACED_BY, vf.createIRI(replace.getTargetUri()));
                    break;
                case SKOSProperty.REPLACES:
                    builder.add(DCTERMS.REPLACES, vf.createIRI(replace.getTargetUri()));
                    break;
            }
        }
    }

    private void writeStatus(SKOSResource resource) {
        if (SKOSProperty.DEPRECATED == resource.getStatus()) {
            builder.add(OWL.DEPRECATED, true);
        }
    }

    private void writeFacet() {
        for (SKOSResource facet : xmlDocument.getFacetList()) {
            builder.subject(vf.createIRI(facet.getUri()));
            builder.add(RDF.TYPE, vf.createIRI("http://purl.org/iso25964/skos-thes#ThesaurusArray"));
            writeRelation(facet);
            writeLabel(facet);
            writeDate(facet);
            writeDocumentation(facet);
        }
    }

    private void writeGroup() {
        for (SKOSResource group : xmlDocument.getGroupList()) {
            builder.subject(vf.createIRI(group.getUri()));
            builder.add(RDF.TYPE, SKOS.COLLECTION);
            writeLabel(group);
            writeRelation(group);
            writeMatch(group);
            writeNotation(group);
            writeDate(group);
            writeIdentifier(group);
            writeAgent(group);
            writeDocumentation(group);
            writeGPS(group);
        }
    }

    private void writeConceptScheme() {
        if (xmlDocument.getConceptScheme() == null) {
            return;
        }
        SKOSResource conceptScheme = xmlDocument.getConceptScheme();

        if (new ToolsHelper().isValidURI(conceptScheme.getUri())) {
            builder.subject(vf.createIRI(conceptScheme.getUri()));
        } else {
            throw new IllegalArgumentException("URI invalide : " + conceptScheme.getUri() + "_ Concept : " +  conceptScheme.getUri());
        }
        builder.add(RDF.TYPE, SKOS.CONCEPT_SCHEME);

        writeLabel(conceptScheme);
        writeRelation(conceptScheme);
        writeMatch(conceptScheme);
        writeNotation(conceptScheme);
        writeDate(conceptScheme);
        writeIdentifier(conceptScheme);
        writeAgent(conceptScheme);
        writeDocumentation(conceptScheme);
        writeGPS(conceptScheme);
        writeDcTerms(conceptScheme);
        // À l'export, le thésaurus est toujours marqué esclave (copie).
        builder.add(
                vf.createIRI(fr.cnrs.opentheso.v2.toolbox.model.ThesaurusRole.SKOS_NAMESPACE,
                        fr.cnrs.opentheso.v2.toolbox.model.ThesaurusRole.SKOS_PROPERTY_LOCAL_NAME),
                vf.createLiteral(fr.cnrs.opentheso.v2.toolbox.model.ThesaurusRole.SLAVE.skosValue())
        );
    }

    private void writeGPS(SKOSResource resource) {
        if (CollectionUtils.isNotEmpty(resource.getGpsCoordinates())) {

            String str = "";
            String wktLiteralValue = "";
            switch(getGpsMode(resource.getGpsCoordinates())) {
                case "POLYGONE":
                    for (SKOSGPSCoordinates gps : resource.getGpsCoordinates()) {
                        str = str + ", " + gps.getLat() + " " + gps.getLon();
                    }
                    wktLiteralValue = "Polygon((" + str.substring(2) + "))";
                    break;
                case "POLYLINE":
                    for (SKOSGPSCoordinates gps : resource.getGpsCoordinates()) {
                        str = str + ", " + gps.getLat() + " " + gps.getLon();
                    }
                    wktLiteralValue = "MultiPoint((" + str.substring(2) + "))";
                    break;
                default:
                    wktLiteralValue = "Point(" + resource.getGpsCoordinates().get(0).getLat() + " "
                            + resource.getGpsCoordinates().get(0).getLon() + ")";
            }

            String wdtNamespace = "http://www.opengis.net/ont/geosparql#";
            builder.add(vf.createIRI(wdtNamespace, "P625"), vf.createLiteral(wktLiteralValue,
                            vf.createIRI(wdtNamespace, "wktLiteral")));

        }
    }

    private static class VocabularyNamespace extends AbstractNamespace {
        private final String prefix;
        private final String namespace;

        public VocabularyNamespace(String prefix, String namespace) {
            this.prefix = prefix;
            this.namespace = namespace;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public String getName() {
            return this.namespace;
        }
    }

    private String getGpsMode(List<SKOSGPSCoordinates> getGpsCoordinates) {
        if (CollectionUtils.isNotEmpty(getGpsCoordinates)) {
            var lastIndex = getGpsCoordinates.size() - 1;
            if (getGpsCoordinates.size() == 1) {
                return "POINT";
            } else if (getGpsCoordinates.get(0).getLon().equals(getGpsCoordinates.get(lastIndex).getLon())
                    && getGpsCoordinates.get(0).getLat().equals(getGpsCoordinates.get(lastIndex).getLat())) {
                return "POLYGONE";
            } else {
                return "POLYLINE";
            }
        }
        return "";
    }

    private void writeExternalResources(SKOSResource resource) {
        for (String externalResource : resource.getExternalResources()) {
            builder.add(DCTERMS.SOURCE, externalResource);
        }
    }

    private void writeDocumentation(SKOSResource resource) {
        for (SKOSDocumentation doc : resource.getDocumentationsList()) {
            switch (doc.getProperty()) {
                case SKOSProperty.DEFINITION:
                    builder.add(SKOS.DEFINITION, vf.createLiteral(doc.getText(), doc.getLanguage()));
                    break;
                case SKOSProperty.SCOPE_NOTE:
                    builder.add(SKOS.SCOPE_NOTE, vf.createLiteral(doc.getText(), doc.getLanguage()));
                    break;
                case SKOSProperty.EXAMPLE:
                    builder.add(SKOS.EXAMPLE, vf.createLiteral(doc.getText(), doc.getLanguage()));
                    break;
                case SKOSProperty.HISTORY_NOTE:
                    builder.add(SKOS.HISTORY_NOTE, vf.createLiteral(doc.getText(), doc.getLanguage()));
                    break;
                case SKOSProperty.EDITORIAL_NOTE:
                    builder.add(SKOS.EDITORIAL_NOTE, vf.createLiteral(doc.getText(), doc.getLanguage()));
                    break;
                case SKOSProperty.CHANGE_NOTE:
                    builder.add(SKOS.CHANGE_NOTE, vf.createLiteral(doc.getText(), doc.getLanguage()));
                    break;
                case SKOSProperty.NOTE:
                    builder.add(SKOS.NOTE, vf.createLiteral(doc.getText(), doc.getLanguage()));
                    break;
            }
        }
    }

    private void writeAgent(SKOSResource resource) {
        for (SKOSAgent agent : resource.getAgentList()) {
            if (ObjectUtils.isNotEmpty(agent)) {
                switch (agent.getProperty()) {
                    case SKOSProperty.CREATOR:
                        if(!StringUtils.isEmpty(agent.getAgent())){
                            builder.add(DCTERMS.CREATOR, agent.getAgent());
                        }
                        break;
                    case SKOSProperty.CONTRIBUTOR:
                        if(!StringUtils.isEmpty(agent.getAgent())){
                            builder.add(DCTERMS.CONTRIBUTOR, agent.getAgent());
                        }
                        break;                        
                    default:
                        throw new AssertionError();
                }
            }
        }
    }

    private void writeDate(SKOSResource resource) {
        for (SKOSDate date : resource.getDateList()) {
            switch (date.getProperty()) {
                case SKOSProperty.CREATED:
                    builder.add(DCTERMS.CREATED, vf.createLiteral(date.getDate(), XSD.DATE));
                    break;
                case SKOSProperty.MODIFIED:
                    builder.add(DCTERMS.MODIFIED, vf.createLiteral(date.getDate(), XSD.DATE));
                    break;
                case SKOSProperty.DATE:
                    builder.add(DCTERMS.DATE, vf.createLiteral(date.getDate(), XSD.DATE));
                    break;
            }
        }
    }

    /**
     * Pour écrire le chemin complet / autopostage
     */
    private void writePath(SKOSResource resource) {
        if (CollectionUtils.isNotEmpty(resource.getPaths())) {
            for (String path : resource.getPaths()) {
                builder.add(DCTERMS.DESCRIPTION, path.trim());
            }
        }
    }

    private void writeIdentifier(SKOSResource resource) {
        if (ObjectUtils.isNotEmpty(resource.getIdentifier())) {
            builder.add(DCTERMS.IDENTIFIER, resource.getIdentifier());
        }
    }

    private void writeNotation(SKOSResource resource) {
        for (SKOSNotation notation : resource.getNotationList()) {
            if (ObjectUtils.isNotEmpty(notation) && StringUtils.isNotEmpty(notation.getNotation())) {
                builder.add(SKOS.NOTATION, notation.getNotation());
            }
        }
    }

    private void writeDiscussions(SKOSResource resource) {
        for (SKOSDiscussion discussion : resource.getMessages()) {
            builder.add(SKOS.NOTE, vf.createLiteral(discussion.getDate() 
                    + DELIMINATE 
                    + discussion.getIdUser() 
                    + DELIMINATE 
                    + discussion.getMsg(), DISCUSSION_TAG));
        }
    }

    private void writeVotes(SKOSResource resource) {
        for (SKOSVote vote : resource.getVotes()) {
            builder.add(SKOS.NOTE, vf.createLiteral(vote.getIdThesaurus() + DELIMINATE + vote.getIdConcept() + DELIMINATE + vote.getValueNote()
                    + DELIMINATE + vote.getIdUser() + DELIMINATE + vote.getTypeVote(), VOTE_TAG));
        }
    }

    private void writeLabel(SKOSResource resource) {
        for (SKOSLabel label : resource.getLabelsList()) {
            switch (label.getProperty()) {
                case SKOSProperty.PREF_LABEL:
                    builder.add(SKOS.PREF_LABEL, vf.createLiteral(label.getLabel(), label.getLanguage()));
                    break;
                case SKOSProperty.ALT_LABEL:
                    builder.add(SKOS.ALT_LABEL, vf.createLiteral(label.getLabel(), label.getLanguage()));
                    break;
                case SKOSProperty.HIDDEN_LABEL:
                    builder.add(SKOS.HIDDEN_LABEL, vf.createLiteral(label.getLabel(), label.getLanguage()));
                    break;
            }
        }
    }

    private void writeDcTerms(SKOSResource resource) {
        if (StringUtils.isNotEmpty(resource.getThesaurus().getTitle())) {
            builder.add(DCTERMS.TITLE, resource.getThesaurus().getTitle());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getCreator())) {
            builder.add(DCTERMS.CREATOR, resource.getThesaurus().getCreator());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getContributor())) {
            builder.add(DCTERMS.CONTRIBUTOR, resource.getThesaurus().getContributor());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getPublisher())) {
            builder.add(DCTERMS.PUBLISHER, resource.getThesaurus().getPublisher());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getDescription())) {
            builder.add(DCTERMS.DESCRIPTION, resource.getThesaurus().getDescription());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getType())) {
            builder.add(DCTERMS.TYPE, resource.getThesaurus().getType());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getRights())) {
            builder.add(DCTERMS.RIGHTS, resource.getThesaurus().getRights());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getSubject())) {
            builder.add(DCTERMS.SUBJECT, resource.getThesaurus().getSubject());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getCoverage())) {
            builder.add(DCTERMS.COVERAGE, resource.getThesaurus().getCoverage());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getLanguage())) {
            builder.add(DCTERMS.LANGUAGE, resource.getThesaurus().getLanguage());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getRelation())) {
            builder.add(DCTERMS.RELATION, resource.getThesaurus().getRelation());
        }

        if (StringUtils.isNotEmpty(resource.getThesaurus().getSource())) {
            builder.add(DCTERMS.SOURCE, resource.getThesaurus().getSource());
        }
        
        /// Ecriture des DCMI, à terme, ca doit remplacer les lignes au dessus
        for (DcElement dcElement : resource.getThesaurus().getDcElement()) {
            switch (dcElement.getName()) {
                case "title":
                    writeDcElement(dcElement, DCTERMS.TITLE);
                case "creator":
                    writeDcElement(dcElement, DCTERMS.CREATOR);
                    break;
                case "contributor":
                    writeDcElement(dcElement, DCTERMS.CONTRIBUTOR);
                    break;
                case "subject":
                    writeDcElement(dcElement, DCTERMS.SUBJECT);
                    break;
                case "description":
                    writeDcElement(dcElement, DCTERMS.DESCRIPTION);
                    break;
                case "publisher":
                    writeDcElement(dcElement, DCTERMS.PUBLISHER);
                    break;
                case "date":
                    writeDcElement(dcElement, DCTERMS.DATE);
                    break;
                case "type":
                    writeDcElement(dcElement, DCTERMS.TYPE);
                    break;
                case "format":
                    writeDcElement(dcElement, DCTERMS.FORMAT);
                    break;
                case "identifier":
                    writeDcElement(dcElement, DCTERMS.IDENTIFIER);
                    break;
                case "source":
                    writeDcElement(dcElement, DCTERMS.SOURCE);
                    break;
                case "language":
                    writeDcElement(dcElement, DCTERMS.LANGUAGE);
                    break;
                case "relation":
                    writeDcElement(dcElement, DCTERMS.RELATION);
                    break;
                case "coverage":
                    writeDcElement(dcElement, DCTERMS.COVERAGE);
                    break;
                case "rights":
                    writeDcElement(dcElement, DCTERMS.RIGHTS);
                    break;
                case "rightsHolder":
                    writeDcElement(dcElement, DCTERMS.RIGHTS_HOLDER);
                    break;
                case "conformsTo":
                    writeDcElement(dcElement, DCTERMS.CONFORMS_TO);
                    break;
                case "created":
                    writeDcElement(dcElement, DCTERMS.CREATED);
                    break;
                case "modified":
                    writeDcElement(dcElement, DCTERMS.MODIFIED);
                    break;
                case "isRequiredBy":
                    writeDcElement(dcElement, DCTERMS.IS_REPLACED_BY);
                    break;
                case "license":
                    writeDcElement(dcElement, DCTERMS.LICENSE);
                    break;
                case "replaces":
                    writeDcElement(dcElement, DCTERMS.REPLACES);
                    break;
                case "alternative":
                    writeDcElement(dcElement, DCTERMS.ALTERNATIVE);
                default:
                    break;
            }
        }
        
    }

    private void writeDcElement(DcElement dcElement, IRI name){
        if(StringUtils.isEmpty(dcElement.getType())) {
            if(!StringUtils.isEmpty(dcElement.getLanguage())){
                builder.add(name, vf.createLiteral(dcElement.getValue(), dcElement.getLanguage()));
            } else {
                builder.add(name, dcElement.getValue());
            }
            return;
        }
        switch (dcElement.getType()) {
            case DCMIResource.TYPE_RESOURCE:
                builder.add(name, vf.createIRI(dcElement.getValue()));
                break;
            case DCMIResource.TYPE_STRING:
                builder.add(name, vf.createLiteral(dcElement.getValue(), XSD.STRING));
                break;                    
            case DCMIResource.TYPE_DATE:
                builder.add(name, vf.createLiteral(dcElement.getValue(), XSD.DATE));
                break;                            
            default:
                if(!StringUtils.isEmpty(dcElement.getLanguage())){
                    builder.add(name, vf.createLiteral(dcElement.getValue(), dcElement.getLanguage()));
                } else {
                    builder.add(name, dcElement.getValue());
                }
                break;
        }        
    }
            
            
            
    private void writeMatch(SKOSResource resource) {
        for (SKOSMatch match : resource.getMatchList()) {
            try {
                switch (match.getProperty()) {
                    case SKOSProperty.EXACT_MATCH:
                        builder.add(SKOS.EXACT_MATCH, vf.createIRI(match.getValue()));
                        break;
                    case SKOSProperty.CLOSE_MATCH:
                        builder.add(SKOS.CLOSE_MATCH, vf.createIRI(match.getValue()));
                        break;
                    case SKOSProperty.BROAD_MATCH:
                        builder.add(SKOS.BROAD_MATCH, vf.createIRI(match.getValue()));
                        break;
                    case SKOSProperty.RELATED_MATCH:
                        builder.add(SKOS.RELATED_MATCH, vf.createIRI(match.getValue()));
                        break;
                    case SKOSProperty.NARROWER_MATCH:
                        builder.add(SKOS.NARROW_MATCH, vf.createIRI(match.getValue()));
                        break;
                }
            } catch (Exception e) {
                System.out.println("Uri non valide = " + resource.getIdentifier() + "  " + match.getValue());
            }

        }
    }

    private void writeStatusCandidat(SKOSResource resource) {
        if (resource.getSkosStatus() != null && !StringUtils.isEmpty(resource.getSkosStatus().getIdStatus())) {
            builder.add(SKOS.NOTE, vf.createLiteral(resource.getSkosStatus().getIdStatus() + DELIMINATE + resource.getSkosStatus().getMessage()
                    + DELIMINATE + resource.getSkosStatus().getIdUser() + DELIMINATE + resource.getSkosStatus().getDate(), STATUS_TAG));
        }
    }

    private void writeRelation(SKOSResource resource) {

        for (SKOSRelation relation : resource.getRelationsList()) {
            switch (relation.getProperty()) {
                case SKOSProperty.MEMBER:
                    builder.add(SKOS.MEMBER, vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.BROADER:
                    builder.add(SKOS.BROADER, vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.BROADER_GENERIC:
                    builder.add("iso-thes:broaderGeneric", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.BROADER_INSTANTIAL:
                    builder.add("iso-thes:broaderInstantial", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.BROADER_PARTITIVE:
                    builder.add("iso-thes:broaderPartitive", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.NARROWER:
                    builder.add(SKOS.NARROWER, vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.NARROWER_GENERIC:
                    builder.add("iso-thes:narrowerGeneric", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.NARROWER_INSTANTIAL:
                    builder.add("iso-thes:narrowerInstantial", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.NARROWER_PARTITIVE:
                    builder.add("iso-thes:narrowerPartitive", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.RELATED:
                    builder.add(SKOS.RELATED, vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.RELATED_HAS_PART:
                    builder.add("iso-thes:relatedHasPart", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.RELATED_PART_OF:
                    builder.add("iso-thes:relatedPartOf", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.HAS_TOP_CONCEPT:
                    builder.add(SKOS.HAS_TOP_CONCEPT, vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.INSCHEME:
                    builder.add(SKOS.IN_SCHEME, vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.TOP_CONCEPT_OF:
                    builder.add(SKOS.TOP_CONCEPT_OF, vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.SUBGROUP:
                    builder.add("iso-thes:subGroup", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.MICROTHESAURUS_OF:
                    builder.add("iso-thes:microThesaurusOf", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.SUPERGROUP:
                    builder.add("iso-thes:superGroup", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.MEMBER_OF:
                    builder.add("opentheso:memberOf", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.SUPER_ORDINATE:
                    builder.add("iso-thes:superOrdinate", vf.createIRI(relation.getTargetUri()));
                    break;
                case SKOSProperty.SUB_ORDINATE_ARRAY:
                    builder.add("iso-thes:subordinateArray", vf.createIRI(relation.getTargetUri()));
            }
        }
    }

}
