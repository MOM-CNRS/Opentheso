/*
{
  "nodes": [
    {
      "id": "th1.300",
      "labels": ["skos__Concept", "Resource"],
      "properties": {
          "skos__prefLabel": [
              "noeud 1@fr",
              "node 1@en"
          ],
          "uri": "http://example.com/th1.300"
      }
    },
    {
      "id": "th1.301",
      "labels": ["skos__Concept", "Resource"],
      "properties": {
          "skos__prefLabel": [
              "noeud 2@fr",
              "node 2@en"
          ],
          "uri": "http://example.com/th1.301"
      }
    }
  ],
  "relationships": [
    { "start": "th1.300", "end": "th1.301", "label": "skos__narrower" }
  ]
}

                <label>Relations affichées: </label>
                <select id="select-links" multiple style="min-width: 500px">
                    <option value="skos__broader">Terme générique</option>
                    <option value="skos__narrower">Terme spécifique</option>
                    <option value="skos__related">Terme associé</option>
                    <option value="skos__exactMatch">Alignement exact</option>
                    <option value="skos__inScheme">Dans thésaurus</option>
                    <option value="skos__hasTopConcept">Top Concept</option>
                    <option value="ns0__isReplacedBy">Est replacé par</option>
                    <option value="ns0__replace">Remplace</option>
                    <option value="ns2__memberOf">Membre de</option>
                </select>
            </div>
            <div class="legend-row-container">
                <label>Noeuds affichés: </label>
                <select id="select-nodes" multiple style="min-width: 500px">
                    <option value="skos__Concept" selected>Concept</option>
                    <option value="skos__Collection">Collection</option>
                    <option value="skos__ConceptScheme" selected>Thésaurus</option>
                    <option value="Resource" selected>Resource</option>
                </select>
            </div>
 */

package fr.cnrs.opentheso.ws.openapi.helper.d3jsgraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

/**
 *
 * @author miledrousset
 */
@Data
public class NodeGraphD3js {
    private List<Relationship> relationships;
    
    private List<Node> nodes;
    private Set<Node> nodeSet = new HashSet<>();    
    
    public boolean addNewNode(Node node) {
        if (node == null || node.getId() == null) {
            return false;
        }
        if (nodeSet.add(node)) {
            nodes.add(node);
            return true; 
        }
        // Node with the same id exists, find and replace it
        if(node.getLabels() != null && node.getLabels().size() > 1){
            if("skos__Concept".equalsIgnoreCase(node.getLabels().get(1))){
                int index = findNodeIndexById(node.getId());
                if (index != -1) {
                    nodes.set(index, node);
                    return true;
                }        
            }
        }
        return false;
    }    
    
    private int findNodeIndexById(String id) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) != null && id.equals(nodes.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }    
    
}
