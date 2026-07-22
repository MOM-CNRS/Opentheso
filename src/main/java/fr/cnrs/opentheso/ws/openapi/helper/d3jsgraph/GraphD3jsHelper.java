package fr.cnrs.opentheso.ws.openapi.helper.d3jsgraph;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.exports.UriHelper;
import fr.cnrs.opentheso.models.thesaurus.NodeThesaurus;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.ThesaurusService;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class GraphD3jsHelper {

    private static final int MAX_CONCEPTS_LIMITED = 800;
    private static final int MAX_CONCEPTS_ABSOLUTE = 2000;

    private final UriHelper uriHelper;
    private final PreferenceService preferenceService;
    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final GraphD3jsQueryRepository graphD3jsQueryRepository;

    private NodeGraphD3js nodeGraphD3js;
    private Preferences nodePreference;

    public void initGraph() {
        nodeGraphD3js = new NodeGraphD3js();
        nodeGraphD3js.setNodes(new ArrayList<>());
        nodeGraphD3js.setRelationships(new ArrayList<>());
    }

    public void getGraphByTheso(String idTheso, String idLang, boolean limit) {
        if (!prepareThesaurusContext(idTheso)) {
            return;
        }

        var nodeTTs = conceptService.getAllTopConcepts(idTheso);
        nodeGraphD3js.getRelationships().addAll(getRelationshipOfTheso(nodeTTs, idTheso));

        int maxConcepts = limit ? MAX_CONCEPTS_LIMITED : MAX_CONCEPTS_ABSOLUTE;
        List<Object[]> conceptRows = graphD3jsQueryRepository.findConceptIdsLimited(idTheso, maxConcepts);
        appendGraphForConcepts(idTheso, idLang, conceptRows);
    }

    public void getGraphByConcept(String idTheso, String idConcept, String idLang) {
        getGraphByConcept(idTheso, idConcept, idLang, true);
    }

    public void getGraphByConcept(String idTheso, String idConcept, String idLang, boolean limit) {
        if (!prepareThesaurusContext(idTheso)) {
            return;
        }

        if (!conceptAddService.isIdExiste(idConcept, idTheso)) {
            return;
        }

        List<String> listIdConcept = conceptService.getIdsOfBranch2(idTheso, idConcept);
        if (listIdConcept == null || listIdConcept.isEmpty()) {
            return;
        }
        int maxConcepts = limit ? MAX_CONCEPTS_LIMITED : MAX_CONCEPTS_ABSOLUTE;
        if (listIdConcept.size() > maxConcepts) {
            listIdConcept = listIdConcept.subList(0, maxConcepts);
        }

        List<Object[]> conceptRows = graphD3jsQueryRepository.findConceptsByIds(idTheso, listIdConcept);
        appendGraphForConcepts(idTheso, idLang, conceptRows);
    }

    private boolean prepareThesaurusContext(String idTheso) {
        nodePreference = preferenceService.getThesaurusPreferences(idTheso);
        uriHelper.setIdTheso(idTheso);
        uriHelper.setNodePreference(nodePreference);

        NodeThesaurus nodeThesaurus = thesaurusService.getNodeThesaurus(idTheso);
        if (nodeThesaurus == null) {
            return false;
        }
        nodeGraphD3js.addNewNode(getDatasOfThesaurus(nodeThesaurus));
        return true;
    }

    private void appendGraphForConcepts(String idTheso, String idLang, List<Object[]> conceptRows) {
        if (conceptRows == null || conceptRows.isEmpty()) {
            return;
        }

        Map<String, ConceptGraphData> concepts = new LinkedHashMap<>();
        for (Object[] row : conceptRows) {
            String idConcept = asString(row[0]);
            if (StringUtils.isBlank(idConcept)) {
                continue;
            }
            concepts.put(idConcept, new ConceptGraphData(
                    uriHelper.getUriForConcept(idConcept, asString(row[1]), asString(row[2]))
            ));
        }
        if (concepts.isEmpty()) {
            return;
        }

        Set<String> conceptIds = concepts.keySet();
        for (Object[] labelRow : graphD3jsQueryRepository.findPrefLabels(idTheso, conceptIds)) {
            ConceptGraphData concept = concepts.get(asString(labelRow[0]));
            if (concept == null) {
                continue;
            }
            String lang = asString(labelRow[1]);
            String label = asString(labelRow[2]);
            if (StringUtils.isBlank(label)) {
                continue;
            }
            concept.prefLabels.add(label + "@" + lang);
        }

        for (ConceptGraphData concept : concepts.values()) {
            nodeGraphD3js.addNewNode(toConceptNode(concept, idLang));
        }

        String thesoUri = uriHelper.getUriForTheso(idTheso, "", "");
        List<Relationship> relationships = new ArrayList<>();

        for (Object[] relationRow : graphD3jsQueryRepository.findHierarchicalRelations(idTheso, conceptIds)) {
            ConceptGraphData start = concepts.get(asString(relationRow[0]));
            if (start == null) {
                continue;
            }
            String endUri = uriHelper.getUriForConcept(
                    asString(relationRow[1]),
                    asString(relationRow[3]),
                    asString(relationRow[4]));
            String role = asString(relationRow[2]);
            String relationLabel = mapHierarchicalRole(role);
            if (relationLabel == null || StringUtils.isBlank(endUri)) {
                continue;
            }
            relationships.add(relationship(start.uri, endUri, relationLabel));
        }

        Map<String, CollectionGraphData> collections = new HashMap<>();
        for (Object[] membershipRow : graphD3jsQueryRepository.findMemberships(idTheso, conceptIds)) {
            ConceptGraphData concept = concepts.get(asString(membershipRow[0]));
            String groupId = asString(membershipRow[1]);
            if (concept == null || StringUtils.isBlank(groupId)) {
                continue;
            }
            CollectionGraphData collection = collections.computeIfAbsent(groupId, id -> new CollectionGraphData(
                    uriHelper.getUriForGroup(id, asString(membershipRow[2]), asString(membershipRow[3]))
            ));
            relationships.add(relationship(concept.uri, collection.uri, "ns2__memberOf"));
        }

        if (!collections.isEmpty()) {
            for (Object[] groupLabelRow : graphD3jsQueryRepository.findGroupLabels(idTheso, collections.keySet())) {
                CollectionGraphData collection = collections.get(asString(groupLabelRow[0]));
                if (collection == null) {
                    continue;
                }
                String label = asString(groupLabelRow[2]);
                String lang = asString(groupLabelRow[1]);
                if (StringUtils.isNotBlank(label)) {
                    collection.prefLabels.add(label + "@" + lang);
                }
            }
            for (CollectionGraphData collection : collections.values()) {
                nodeGraphD3js.addNewNode(toCollectionNode(collection));
            }
        }

        Set<String> externalUris = new HashSet<>();
        for (Object[] exactMatchRow : graphD3jsQueryRepository.findExactMatches(idTheso, conceptIds)) {
            ConceptGraphData concept = concepts.get(asString(exactMatchRow[0]));
            String targetUri = asString(exactMatchRow[1]);
            if (concept == null || StringUtils.isBlank(targetUri)) {
                continue;
            }
            relationships.add(relationship(concept.uri, targetUri, "skos__exactMatch"));
            if (externalUris.add(targetUri)) {
                nodeGraphD3js.addNewNode(getDatasOfExternalLink(targetUri));
            }
        }

        for (Object[] replacedByRow : graphD3jsQueryRepository.findReplacedBy(idTheso, conceptIds)) {
            ConceptGraphData concept = concepts.get(asString(replacedByRow[0]));
            if (concept == null) {
                continue;
            }
            String endUri = uriHelper.getUriForConcept(
                    asString(replacedByRow[1]),
                    asString(replacedByRow[2]),
                    asString(replacedByRow[3]));
            if (StringUtils.isNotBlank(endUri)) {
                relationships.add(relationship(concept.uri, endUri, "ns0__isReplacedBy"));
            }
        }

        for (Object[] replacesRow : graphD3jsQueryRepository.findReplaces(idTheso, conceptIds)) {
            ConceptGraphData concept = concepts.get(asString(replacesRow[0]));
            if (concept == null) {
                continue;
            }
            String endUri = uriHelper.getUriForConcept(
                    asString(replacesRow[1]),
                    asString(replacesRow[2]),
                    asString(replacesRow[3]));
            if (StringUtils.isNotBlank(endUri)) {
                relationships.add(relationship(concept.uri, endUri, "ns0__replace"));
            }
        }

        for (ConceptGraphData concept : concepts.values()) {
            relationships.add(relationship(concept.uri, thesoUri, "skos__inScheme"));
        }

        nodeGraphD3js.getRelationships().addAll(relationships);
    }

    private static String mapHierarchicalRole(String role) {
        if (role == null) {
            return null;
        }
        if (role.startsWith("NT")) {
            return "skos__narrower";
        }
        if (role.startsWith("BT")) {
            return "skos__broader";
        }
        if ("RT".equals(role)) {
            return "skos__related";
        }
        return null;
    }

    private static Relationship relationship(String start, String end, String relation) {
        Relationship relationship = new Relationship();
        relationship.setStart(start);
        relationship.setEnd(end);
        relationship.setRelation(relation);
        return relationship;
    }

    private Node toConceptNode(ConceptGraphData concept, String idLang) {
        Node node = new Node();
        node.setId(concept.uri);
        node.setLabels(List.of("Resource", "skos__Concept"));

        Properties properties = new Properties();
        properties.setUri(concept.uri);
        properties.setPropertiesLabel("skos__prefLabel");

        List<String> prefLabels = new ArrayList<>(concept.prefLabels);
        if (prefLabels.isEmpty()) {
            prefLabels.add("@" + (idLang != null ? idLang : ""));
        }
        properties.setPrefLabels(prefLabels);
        node.setProperties(properties);
        return node;
    }

    private Node toCollectionNode(CollectionGraphData collection) {
        Node node = new Node();
        node.setId(collection.uri);
        node.setLabels(List.of("Resource", "skos__Collection"));

        Properties properties = new Properties();
        properties.setUri(collection.uri);
        properties.setPropertiesLabel("skos__prefLabel");
        properties.setPrefLabels(new ArrayList<>(collection.prefLabels));
        node.setProperties(properties);
        return node;
    }

    private Node getDatasOfThesaurus(NodeThesaurus nodeThesaurus) {
        Node node = new Node();

        node.setId(uriHelper.getUriForTheso(nodeThesaurus.getIdThesaurus(), nodeThesaurus.getIdArk(), ""));
        List<String> labels = new ArrayList<>();
        labels.add("Resource");
        labels.add("skos__ConceptScheme");
        node.setLabels(labels);

        Properties properties = new Properties();
        properties.setUri(uriHelper.getUriForTheso(nodeThesaurus.getIdThesaurus(), "", ""));
        properties.setPropertiesLabel("skos__prefLabel");

        List<String> prefLabels = new ArrayList<>();
        for (Thesaurus thesaurus : nodeThesaurus.getListThesaurusTraduction()) {
            prefLabels.add(thesaurus.getTitle() + "@" + thesaurus.getLanguage());
        }
        properties.setPrefLabels(prefLabels);
        node.setProperties(properties);
        return node;
    }

    private List<Relationship> getRelationshipOfTheso(List<NodeUri> nodeUri, String idTheso) {
        List<Relationship> relationships = new ArrayList<>();
        for (NodeUri nodeUri1 : nodeUri) {
            Relationship relationship = new Relationship();
            relationship.setStart(uriHelper.getUriForTheso(idTheso, "", ""));
            relationship.setEnd(uriHelper.getUriForConcept(nodeUri1.getIdConcept(), nodeUri1.getIdArk(), null));
            relationship.setRelation("skos__hasTopConcept");
            relationships.add(relationship);
        }
        return relationships;
    }

    private Node getDatasOfExternalLink(String id) {
        Node node = new Node();
        node.setId(id);
        node.setLabels(List.of("Resource"));
        Properties properties = new Properties();
        properties.setUri(id);
        node.setProperties(properties);
        return node;
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    public String getJsonFromNodeGraphD3js() {
        if (nodeGraphD3js == null) {
            return null;
        }

        JsonObjectBuilder nodeRoot = Json.createObjectBuilder();
        JsonArrayBuilder jsonArrayNodes = Json.createArrayBuilder();

        for (Node node : nodeGraphD3js.getNodes()) {
            if (node == null || node.getId() == null) {
                continue;
            }
            JsonObjectBuilder nodeDatas = Json.createObjectBuilder();
            nodeDatas.add("id", node.getId());

            JsonArrayBuilder nodeLables = Json.createArrayBuilder();
            if (node.getLabels() != null) {
                for (String label : node.getLabels()) {
                    if (label != null) {
                        nodeLables.add(label);
                    }
                }
            }
            nodeDatas.add("labels", nodeLables.build());

            JsonObjectBuilder nodeProperties = Json.createObjectBuilder();
            JsonArrayBuilder jsonArrayPrefLabels = Json.createArrayBuilder();

            if (node.getProperties() != null) {
                if (node.getProperties().getPrefLabels() != null) {
                    for (String prefLabel : node.getProperties().getPrefLabels()) {
                        if (prefLabel != null) {
                            jsonArrayPrefLabels.add(prefLabel);
                        }
                    }
                    String propertiesLabel = node.getProperties().getPropertiesLabel();
                    if (propertiesLabel != null) {
                        nodeProperties.add(propertiesLabel, jsonArrayPrefLabels.build());
                    }
                }
                if (node.getProperties().getUri() != null) {
                    nodeProperties.add("uri", node.getProperties().getUri());
                } else {
                    nodeProperties.add("uri", node.getId());
                }
            } else {
                nodeProperties.add("uri", node.getId());
            }

            nodeDatas.add("properties", nodeProperties.build());
            jsonArrayNodes.add(nodeDatas.build());
        }

        nodeRoot.add("nodes", jsonArrayNodes.build());

        JsonArrayBuilder jsonArrayRelationships = Json.createArrayBuilder();
        for (Relationship relationship : nodeGraphD3js.getRelationships()) {
            if (relationship == null || relationship.getStart() == null || relationship.getEnd() == null
                    || relationship.getRelation() == null) {
                continue;
            }
            JsonObjectBuilder nodeRelation = Json.createObjectBuilder();
            nodeRelation.add("start", relationship.getStart());
            nodeRelation.add("end", relationship.getEnd());
            nodeRelation.add("label", relationship.getRelation());
            jsonArrayRelationships.add(nodeRelation.build());
        }
        nodeRoot.add("relationships", jsonArrayRelationships.build());
        return nodeRoot.build().toString();
    }

    private static final class ConceptGraphData {
        private final String uri;
        private final List<String> prefLabels = new ArrayList<>();

        private ConceptGraphData(String uri) {
            this.uri = uri;
        }
    }

    private static final class CollectionGraphData {
        private final String uri;
        private final List<String> prefLabels = new ArrayList<>();

        private CollectionGraphData(String uri) {
            this.uri = uri;
        }
    }
}
