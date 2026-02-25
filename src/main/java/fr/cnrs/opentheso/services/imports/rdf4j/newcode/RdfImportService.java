package fr.cnrs.opentheso.services.imports.rdf4j.newcode;

import fr.cnrs.opentheso.models.skos.ResourceType;
import fr.cnrs.opentheso.models.skos.SkosConceptDto;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class RdfImportService {

    private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

    /**
     * Lit un fichier RDF (TTL ou RDF/XML) et retourne la liste des concepts DTO.
     */
    public List<SkosConceptDto> importRdf(InputStream rdfInput) throws Exception {
        // 1️⃣ Parse le RDF en mémoire
        Model model = parseRdf(rdfInput);

        // 2️⃣ Map URI -> DTO
        Map<String, SkosConceptDto> dtoMap = new HashMap<>();

        for (Resource subject : model.subjects()) {
            String uri = subject.stringValue();
            SkosConceptDto dto = dtoMap.computeIfAbsent(uri, u -> new SkosConceptDto());
            dto.setUri(uri);

            // Type de ressource
            List<String> types = model.filter(subject, RDF.TYPE, null)
                    .stream()
                    .map(st -> st.getObject().stringValue())
                    .toList();
            for (String typeUri : types) {
                ResourceType rt = ResourceType.fromRdfName(typeUri);
                if (rt != null) {
                    dto.setResourceType(rt);
                    break;
                }
            }

            // Créateur / contributeur
            model.filter(subject, VF.createIRI("http://purl.org/dc/terms/creator"), null)
                    .forEach(st -> dto.setCreatorName(st.getObject().stringValue()));

            model.filter(subject, VF.createIRI("http://purl.org/dc/terms/contributor"), null)
                    .forEach(st -> dto.getContributorName().add(st.getObject().stringValue()));

            // Identifier
            model.filter(subject, VF.createIRI("http://purl.org/dc/terms/identifier"), null)
                    .forEach(st -> dto.setIdentifier(st.getObject().stringValue()));

            // Dates
            model.filter(subject, VF.createIRI("http://purl.org/dc/terms/created"), null)
                    .forEach(st -> dto.setCreated(st.getObject().stringValue()));

            model.filter(subject, VF.createIRI("http://purl.org/dc/terms/modified"), null)
                    .forEach(st -> dto.setModified(st.getObject().stringValue()));

            // Notation
            model.filter(subject, VF.createIRI("http://www.w3.org/2004/02/skos/core#notation"), null)
                    .forEach(st -> dto.setNotation(st.getObject().stringValue()));

            // PrefLabel / AltLabel / HiddenLabel
            populateLabels(model, subject, dto);

            // Notes / definitions / scopeNote
            populateNotes(model, subject, dto);

            // Relations SKOS
            populateRelations(model, subject, dto);

            // Coordonnées GPS
            handleGps(model, subject, dto);

            // Images / foaf:Image
            populateImages(model, subject, dto);
        }

        // Retourne tous les DTO
        return new ArrayList<>(dtoMap.values());
    }

    /* ==========================
       Helpers
       ========================== */

    private Model parseRdf(InputStream rdfInput) throws Exception {
        RDFParser parser = Rio.createParser(RDFFormat.RDFXML);
        Model model = new LinkedHashModel();
        parser.setRDFHandler(new StatementCollector(model));
        parser.parse(rdfInput, "");
        return model;
    }

    /**
     * Remplit les labels multilingues (prefLabel, altLabel, hiddenLabel) depuis le RDF.
     * Les valeurs sont stockées dans dto.getLabels() sous forme Map<labelType, Map<lang, List<String>>>.
     */
    private void populateLabels(Model model, Resource subject, SkosConceptDto dto) {
        String[] labelTypes = {"prefLabel", "altLabel", "hiddenLabel"};

        for (String labelType : labelTypes) {
            IRI predicate = VF.createIRI("http://www.w3.org/2004/02/skos/core#" + labelType);

            for (Statement st : model.filter(subject, predicate, null)) {
                if (st.getObject() instanceof Literal lit) {
                    String lang = lit.getLanguage().orElse("und");

                    // récupère ou crée la liste des labels pour ce type et cette langue
                    List<String> values = dto.getLabels()
                            .computeIfAbsent(labelType, k -> new HashMap<>())
                            .computeIfAbsent(lang, k -> new ArrayList<>());

                    values.add(lit.getLabel());
                }
            }
        }
    }

    private void populateNotes(Model model, Resource subject, SkosConceptDto dto) {
        String[] noteTypes = {"definition", "note", "scopeNote", "historyNote"};
        for (String type : noteTypes) {
            IRI predicate = VF.createIRI("http://www.w3.org/2004/02/skos/core#" + type);
            model.filter(subject, predicate, null).forEach(st -> {
                if (st.getObject() instanceof Literal lit) {
                    dto.setNote(type, lit.getLanguage().orElse("und"), lit.getLabel());
                }
            });
        }
    }

    private void populateRelations(Model model, Resource subject, SkosConceptDto dto) {
        Map<String, String> relationMap = Map.of(
                "skos:broader", "broader",
                "skos:narrower", "narrower",
                "skos:related", "related",
                "skos:member", "member"
        );
        relationMap.forEach((rdfPred, relationType) -> {
            IRI pred = VF.createIRI("http://www.w3.org/2004/02/skos/core#" + rdfPred.substring(5));
            model.filter(subject, pred, null).forEach(st -> dto.addRelation(relationType, st.getObject().stringValue()));
        });
    }

    private void handleGps(Model model, Resource subject, SkosConceptDto dto) {
        IRI lat = VF.createIRI("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
        IRI lon = VF.createIRI("http://www.w3.org/2003/01/geo/wgs84_pos#long");
        model.filter(subject, lat, null)
                .forEach(st -> dto.setLatitude(Double.parseDouble(st.getObject().stringValue())));
        model.filter(subject, lon, null)
                .forEach(st -> dto.setLongitude(Double.parseDouble(st.getObject().stringValue())));
        IRI point = VF.createIRI("http://www.opengis.net/ont/geosparql#P625");
        model.filter(subject, point, null).forEach(st -> {
            String wkt = st.getObject().stringValue();
            if (wkt.startsWith("Point(") && wkt.endsWith(")")) {
                String[] coords = wkt.substring(6, wkt.length() - 1).split(" ");
                dto.setLatitude(Double.parseDouble(coords[1]));
                dto.setLongitude(Double.parseDouble(coords[0]));
            }
        });
    }

    private void populateImages(Model model, Resource subject, SkosConceptDto dto) {
        IRI foafImage = VF.createIRI("http://xmlns.com/foaf/0.1/Image");
        model.filter(subject, RDF.TYPE, foafImage)
                .forEach(st -> {
                    // Ici tu peux remplir rawColumns ou créer une Map spécifique images
                    dto.setRawColumn("foaf:Image", st.getSubject().stringValue());
                });
    }
}