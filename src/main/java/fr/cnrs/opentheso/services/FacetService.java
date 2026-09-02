package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.ConceptFacet;
import fr.cnrs.opentheso.entites.NodeLabel;
import fr.cnrs.opentheso.entites.ThesaurusArray;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.facets.NodeFacet;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.NodeLabelRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@AllArgsConstructor
public class FacetService {

    private final ConceptFacetRepository facetRepository;
    private final ThesaurusArrayRepository thesaurusArrayRepository;
    private final NodeLabelRepository nodeLabelRepository;
    private final ConceptFacetRepository conceptFacetRepository;
    private final ConceptRepository conceptRepository;
    private final TermRepository termRepository;


    public NodeFacet getFacet(String idFacet, String idThesaurus, String lang) {

        var facet = thesaurusArrayRepository.findFacetMetadata(idFacet, idThesaurus);
        if (facet.isEmpty()) {
            log.error("Aucune facet n'est trouvée avec l'id {}", idFacet);
            return null;
        }

        var lexicalValue = nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang(idFacet, idThesaurus, lang)
                .map(NodeLabel::getLexicalValue)
                .orElse("");

        return NodeFacet.builder()
                .idFacet(idFacet)
                .idThesaurus(idThesaurus)
                .idConceptParent(facet.get().getIdConceptParent())
                .lexicalValue(lexicalValue)
                .lang(lang)
                .build();
    }

    public List<NodeIdValue> getAllIdValueFacetsOfConcept(String idConcept, String idThesaurus, String idLang) {

        log.debug("Recherche des facettes appartenant au concept id {}", idConcept);
        var thesaurusArrays = thesaurusArrayRepository.findAllByIdThesaurusAndIdConceptParent(idThesaurus, idConcept);
        if (CollectionUtils.isEmpty(thesaurusArrays)) {
            log.debug("Aucune facette n'est trouvée pour le concept id {}", idConcept);
            return Collections.emptyList();
        }

        log.debug("{} facettes appartenant au concept id {}", thesaurusArrays.size(), idConcept);
        var listFacets = new java.util.ArrayList<>(thesaurusArrays.stream()
                .map(facet -> {
                    var lexicalValue = nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang(facet.getIdFacet(), idThesaurus, idLang)
                            .map(NodeLabel::getLexicalValue)
                            .orElse("");

                    return NodeIdValue.builder()
                            .id(facet.getIdFacet())
                            .value(lexicalValue)
                            .build();
                }).collect(Collectors.toCollection(ArrayList::new)));
        Collections.sort(listFacets);
        return listFacets;
    }

    public List<ConceptFacet> getFacetsByConceptAndThesaurus(String idConcept, String idThesaurus) {

        log.debug("Recherche de facet à rattaché au concept id {} et thésaurus id {}", idConcept, idThesaurus);
        var facets = facetRepository.findAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (CollectionUtils.isEmpty(facets)) {
            log.debug("Aucune facet n'est trouvé avec le concept id {}", idConcept);
            return List.of();
        }

        log.debug("{} facets trouvées avec le concept id {}", facets.size(), idConcept);
        return facets;
    }

    public void deleteFacetsByConceptAndThesaurus(String idConcept, String idThesaurus) {

        log.debug("Suppression de tous les facette rattachées au concept id {} dans le thésaurus id {}", idConcept, idThesaurus);
        facetRepository.deleteAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);
    }

    public boolean isConceptHaveFacet(String idConcept, String idThesaurus) {

        log.debug("Vérifier si le concept id {} contient des facets", idConcept);
        var thesaurusArray = thesaurusArrayRepository.findAllByIdThesaurusAndIdConceptParent(idThesaurus, idConcept);
        return CollectionUtils.isNotEmpty(thesaurusArray);
    }

    public boolean isFacetHaveThisMember(String idFacet, String idConcept, String idThesaurus) {

        log.debug("Vérifier si la facet id {} contient des facets", idFacet);
        var facets = facetRepository.findAllByIdConceptAndIdThesaurusAndIdFacet(idConcept, idThesaurus, idFacet);
        return CollectionUtils.isNotEmpty(facets);
    }

    public boolean isIdFacetExist(String idFacet) {

        log.debug("Vérifier si une facet avec l'id {} existe", idFacet);
        var facet = facetRepository.findByIdFacet(idFacet);
        return facet.isPresent();
    }

    public boolean isFacetHaveMembers(String idFacet, String idThesaurus) {

        log.debug("Vérifier si la facet avec l'id {}a des concepts membres", idThesaurus);
        var facet = conceptFacetRepository.findByIdFacetAndIdThesaurus(idFacet, idThesaurus);
        return facet.isPresent();
    }

    public String addNewFacet(String idFacet, String idThesaurus, String idConceptParent, String lexicalValue, String idLang) {

        log.debug("Ajout d'une nouvelle facette");
        lexicalValue = fr.cnrs.opentheso.utils.StringUtils.convertString(lexicalValue);

        if (StringUtils.isEmpty(idFacet)) {
            idFacet = generateNewFacetId();
            while (isIdFacetExist(idFacet)) {
                idFacet = generateNewFacetId();
            }
        }

        nodeLabelRepository.save(NodeLabel.builder()
                .idFacet(idFacet)
                .idThesaurus(idThesaurus)
                .lexicalValue(lexicalValue)
                .created(new Date())
                .modified(new Date())
                .lang(idLang)
                .build());

        thesaurusArrayRepository.save(ThesaurusArray.builder()
                .idThesaurus(idThesaurus)
                .idConceptParent(idConceptParent)
                .idFacet(idFacet)
                .build());

        return idFacet;
    }

    private String generateNewFacetId() {
        Long nextId = thesaurusArrayRepository.getNextFacetSequenceId();
        if (nextId == null) {
            log.error("La séquence 'thesaurus_array_facet_id_seq' n'a pas retourné de valeur.");
            throw new IllegalStateException("Impossible de générer un nouvel ID de facette.");
        }
        return "F" + nextId;
    }

    public List<NodeFacet> getAllFacetsDetailsOfThesaurus(String idThesaurus) {

        log.debug("Recherche des facettes du thésaurus id {}", idThesaurus);
        var facets = nodeLabelRepository.findAllFacetsWithConceptParent(idThesaurus);
        if (CollectionUtils.isNotEmpty(facets)) {
            log.debug("Aucune facet n'est trouvée");
            return List.of();
        }

        log.debug("{} facettes récupérées pour le thésaurus '{}'", facets.size(), idThesaurus);
        return facets.stream().map(facet -> {

            var created = (Date) facet[3];
            var modified = (Date) facet[4];
            var idConceptParent = (String) facet[5];

            var nodeFacet = new NodeFacet();
            nodeFacet.setIdFacet((String) facet[1]);
            nodeFacet.setIdThesaurus(idThesaurus);
            nodeFacet.setLexicalValue((String) facet[5]);
            nodeFacet.setLang((String) facet[2]);
            nodeFacet.setCreated(created != null ? created.toString() : null);
            nodeFacet.setModified(modified != null ? modified.toString() : null);
            nodeFacet.setIdConceptParent(idConceptParent);

            // Infos URI
            var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConceptParent, idThesaurus);
            NodeUri nodeUri = new NodeUri();
            nodeUri.setIdConcept(idConceptParent);
            nodeUri.setIdArk(concept.map(Concept::getIdArk).orElse(null));
            nodeUri.setIdHandle(concept.map(Concept::getIdHandle).orElse(null));
            nodeFacet.setNodeUri(nodeUri);

            return nodeFacet;
        }).toList();
    }

    public boolean updateFacetTraduction(String idFacet, String idThesaurus, String idLang, String lexicalValue) {

        log.debug("Mise à jour de la valeur de la facette avec id {}", idFacet);
        var facet = nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang(idFacet, idThesaurus, idLang);
        if (facet.isEmpty()) {
            log.error("Aucune facette n'est trouvée avec l'id {}", idFacet);
            return false;
        }

        lexicalValue = fr.cnrs.opentheso.utils.StringUtils.convertString(lexicalValue);
        facet.get().setLexicalValue(lexicalValue);
        nodeLabelRepository.save(facet.get());
        log.debug("Mise à jour terminée de la facet id {}", idFacet);
        return true;
    }

    public void updateFacetParent(String idConceptParent, String idFacet, String idThesaurus) {
        log.debug("Mise à jour du concept parent pour la facet id {}", idFacet);
        thesaurusArrayRepository.updateConceptParent(idConceptParent, idThesaurus, idFacet);
    }

    public void deleteFacet(String idFacet, String idThesaurus) {

        log.debug("Suppression de la facet id {} avec ses relations", idFacet);
        thesaurusArrayRepository.deleteAllByIdThesaurusAndIdFacet(idThesaurus, idFacet);
        conceptFacetRepository.deleteAllByIdThesaurusAndIdFacet(idThesaurus, idFacet);
        nodeLabelRepository.deleteAllByIdThesaurusAndIdFacet(idThesaurus, idFacet);
    }

    public void deleteTraductionFacet(String idFacet, String idThesaurus, String idLang) {

        log.debug("Suppression de la traduction en langue {} de la facette avec id {}", idLang, idFacet);
        nodeLabelRepository.deleteAllByIdThesaurusAndIdFacetAndLang(idThesaurus, idFacet, idLang);
    }

    public void deleteConceptFromFacet(String idFacet, String idConcept, String idThesaurus) {

        log.debug("Suppression de la facette avec id {}", idFacet);
        conceptFacetRepository.deleteAllByIdConceptAndIdThesaurusAndIdFacet(idConcept, idThesaurus, idFacet);
    }

    public boolean checkExistenceFacetByNameAndLangAndThesaurus(String name, String lang, String idThesaurus) {

        log.debug("Vérifier l'existence d'une facet par sa valeur {} (langue {})", name, lang);
        return nodeLabelRepository.existsByIdThesaurusAndLexicalValueAndLang(idThesaurus, name, lang);
    }


    public boolean isTraductionExistOfFacet(String idFacet, String idThesaurus, String idLang) {

        log.debug("Vérifier si une traduction existe pour la facet id {} en lang {}", idFacet, idLang);
        var nodeLabel = nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang(idFacet, idThesaurus, idLang);
        return nodeLabel.isPresent();
    }

    public void addConceptToFacet(String idFacet, String idThesaurus, String idConcept) {

        log.debug("Ajout d'un concept dans la facet id {}", idFacet);
        conceptFacetRepository.save(ConceptFacet.builder()
                .idFacet(idFacet)
                .idThesaurus(idThesaurus)
                .idConcept(idConcept)
                .build());
    }

    public void addFacetTraduction(String idFacet, String idThesaurus, String lexicalValue, String idLang) {

        log.debug("Ajout d'une nouvelle traduction pour la facet id {} en lang {}", idFacet, idLang);
        lexicalValue = fr.cnrs.opentheso.utils.StringUtils.convertString(lexicalValue);
        nodeLabelRepository.save(NodeLabel.builder()
                .idFacet(idFacet)
                .idThesaurus(idThesaurus)
                .lexicalValue(lexicalValue)
                .lang(idLang)
                .created(new Date())
                .modified(new Date())
                .build());
    }

    public void updateDateOfFacet(String idThesaurus, String idFacet, int contributor) {

        log.debug("Mise à jour de la date de modification de la facet avec id {}", idFacet);
        var thesaurusArray = thesaurusArrayRepository.findAllByIdThesaurusAndIdFacet(idThesaurus, idFacet);
        if (thesaurusArray.isEmpty()) {
            log.debug("Aucune facette n'est trouvée avec l'id {}", idFacet);
            return;
        }

        thesaurusArray.get().setContributor(contributor);
        thesaurusArray.get().setModified(new Date());
        thesaurusArrayRepository.save(thesaurusArray.get());
    }

    public String getIdConceptParentOfFacet(String idFacet, String idThesaurus) {

        log.debug("Recherche de l'id du concept Parent de la facet id {}", idFacet);
        var thesaurusArray = thesaurusArrayRepository.findAllByIdThesaurusAndIdFacet(idThesaurus, idFacet);
        if (thesaurusArray.isEmpty()) {
            log.debug("Aucune facette n'est trouvée avec l'id {}", idFacet);
            return null;
        }

        return thesaurusArray.get().getIdConceptParent();
    }

    public List<String> getAllMembersOfFacet(String idFacet, String idThesaurus) {

        log.debug("Recherche des concepts membre à la facet id {}", idFacet);
        var facettes = conceptFacetRepository.findAllByIdFacetAndIdThesaurus(idFacet, idThesaurus);
        if (CollectionUtils.isEmpty(facettes)) {
           log.debug("Aucun concept membre à la facet id {}", idFacet);
           return Collections.emptyList();
        }
        return facettes.stream().map(ConceptFacet::getIdConcept).toList();
    }

    public List<NodeFacet> getAllTraductionsFacet(String idFacet, String idThesaurus, String lang) {

        var facettes = nodeLabelRepository.findByIdFacetAndIdThesaurusAndLangNot(idFacet, idThesaurus, lang);
        if (CollectionUtils.isEmpty(facettes)) {
            log.debug("Aucune traduction n'est trouvée pour la facet id {}", idFacet);
            return Collections.emptyList();
        }
        return facettes.stream()
                .map(facet -> NodeFacet.builder()
                        .idFacet(facet.getIdFacet())
                        .idThesaurus(facet.getIdThesaurus())
                        .lexicalValue(facet.getLexicalValue())
                        .lang(facet.getLang())
                        .build())
                .toList();
    }

    public boolean isFacetInGroups(String idThesaurus, String idFacet, List<String> groups) {

        log.debug("Vérifier si le parent de la Facette est dans les collections indiquées");
        if (CollectionUtils.isEmpty(groups)) {
            log.error("La liste des groups est vide");
            return false;
        }

        var lowerGroups = groups.stream().filter(StringUtils::isNotEmpty).map(String::toLowerCase).toList();
        var result = thesaurusArrayRepository.findConceptParentInGroups(idFacet, idThesaurus, lowerGroups);
        log.debug("Vérification d'appartenance de la facette '{}' aux groupes {} dans le thésaurus '{}': {}",
                idFacet, lowerGroups, idThesaurus, result.isPresent());

        return result.isPresent();
    }

    public List<NodeIdValue> searchFacet(String name, String lang, String idThesaurus) {

        log.debug("Recherche des facettes contenant '{}' (lang='{}', thésaurus='{}')", name, lang, idThesaurus);
        var normalized = fr.cnrs.opentheso.utils.StringUtils.convertString(name);
        var rawResults = nodeLabelRepository.searchFacetsByName(normalized, lang, idThesaurus);

        log.debug("{} facettes trouvées pour la recherche sur '{}'", rawResults.size(), name);
        return rawResults.stream().map(row -> {
            NodeIdValue node = new NodeIdValue();
            node.setId((String) row[0]);
            node.setValue((String) row[1]);
            return node;
        }).toList();
    }

    public List<NodeIdValue> getAllMembersOfFacetSorted(String idFacet, String idLang, String idTheso) {
        log.debug("Recherche des membres de la facette '{}' dans le thésaurus '{}'", idFacet, idTheso);
        try {
            var conceptIds = conceptFacetRepository.findConceptIdsByFacet(idTheso, idFacet);

            var results = conceptIds.stream()
                    .map(conceptId -> NodeIdValue.builder()
                            .id(conceptId)
                            .value(termRepository.getLexicalValueOfConcept(conceptId, idTheso, idLang).orElse(""))
                            .build())
                    .collect(Collectors.toCollection(ArrayList::new));

            Collections.sort(results);
            log.debug("{} concepts trouvés pour la facette '{}'", results.size(), idFacet);
            return results;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des membres de la facette : " + idFacet, e);
            return Collections.emptyList();
        }
    }
}
