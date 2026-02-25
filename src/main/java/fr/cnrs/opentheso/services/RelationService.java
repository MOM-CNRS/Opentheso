package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.entites.HierarchicalRelationshipHistorique;
import fr.cnrs.opentheso.models.BroaderRelationProjection;
import fr.cnrs.opentheso.models.RelatedRelationProjection;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.relations.NodeCustomRelation;
import fr.cnrs.opentheso.models.relations.NodeHieraRelation;
import fr.cnrs.opentheso.models.relations.NodeRelation;
import fr.cnrs.opentheso.models.relations.NodeTypeRelation;
import fr.cnrs.opentheso.models.terms.NodeBT;
import fr.cnrs.opentheso.models.terms.NodeNT;
import fr.cnrs.opentheso.models.terms.NodeRT;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipHistoriqueRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.NtTypeRepository;
import fr.cnrs.opentheso.repositories.TermRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@AllArgsConstructor
public class RelationService {

    private final TermRepository termRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final HierarchicalRelationshipHistoriqueRepository hierarchicalRelationshipHistoriqueRepository;
    private final ConceptTypeService conceptTypeService;
    private final TermService termService;
    private final NtTypeRepository ntTypeRepository;


    public HierarchicalRelationship addHierarchicalRelation(String idConcept1, String idThesaurus, String role, String idConcept2) {

        var hierarchicalRelationship = HierarchicalRelationship.builder()
                .idConcept1(idConcept1)
                .idConcept2(idConcept2)
                .idThesaurus(idThesaurus)
                .role(role)
                .build();

        return hierarchicalRelationshipRepository.save(hierarchicalRelationship);
    }

    public void deleteAllByThesaurus(String idThesaurus) {

        log.debug("Suppression des relation des concepts présents dans le thésaurus id {}", idThesaurus);
        hierarchicalRelationshipRepository.deleteAllByIdThesaurus(idThesaurus);
        hierarchicalRelationshipHistoriqueRepository.deleteAllByIdThesaurus(idThesaurus);
    }

    public void deleteAllRelationOfConcept(String idConcept, String idThesaurus) {

        log.debug("Suppression des toutes relation avec le concept id {} présents dans le thésaurus id {}", idConcept, idThesaurus);
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1(idThesaurus, idConcept);
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept2(idThesaurus, idConcept);
    }

    public void updateThesaurusId(String oldIdThesaurus, String newIdThesaurus) {

        log.debug("Mise à jour du thésaurus id pour les relation entre les concepts présents dans le thésaurus id {}", oldIdThesaurus);
        hierarchicalRelationshipRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        hierarchicalRelationshipHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    public void addLinkHierarchicalRelation(HierarchicalRelationship hierarchicalRelationship, int idUser) {

        log.debug("Ajouter une relation d'hiérarchique entre les deux concepts {} et {}",
                hierarchicalRelationship.getIdConcept1(), hierarchicalRelationship.getIdConcept2());

        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(hierarchicalRelationship.getIdConcept1())
                .idConcept2(hierarchicalRelationship.getIdConcept2())
                .role(hierarchicalRelationship.getRole())
                .idThesaurus(hierarchicalRelationship.getIdThesaurus())
                .build());

        addRelationHistorique(hierarchicalRelationship.getIdConcept1(), hierarchicalRelationship.getIdThesaurus(),
                hierarchicalRelationship.getIdConcept2(), hierarchicalRelationship.getRole(), idUser, "ADD");
    }

    public List<NodeIdValue> getCandidatRelationsBT(String idConceptSelected, String idThesaurus, String lang) {

        var nodeBTs = getListBT(idConceptSelected, idThesaurus, lang);

        if(CollectionUtils.isNotEmpty(nodeBTs)) {
            return nodeBTs.stream()
                    .map(element -> NodeIdValue.builder()
                            .id(element.getIdConcept())
                            .value(element.getTitle())
                            .build())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public List<NodeBT> getListBT(String idConcept, String idThesaurus, String idLang) {

        log.debug("Chargement des relations BT pour le concept '{}' dans le thésaurus '{}'", idConcept, idThesaurus);
        var broaderRelations = hierarchicalRelationshipRepository.findBroaderConcepts(idConcept, idThesaurus);

        List<NodeBT> result = new ArrayList<>();
        for (BroaderRelationProjection relation : broaderRelations) {
            NodeBT nodeBT = new NodeBT();
            nodeBT.setIdConcept(relation.getIdConcept2());
            nodeBT.setRole(relation.getRole());
            nodeBT.setStatus(relation.getStatus());

            var lexicalOpt = termRepository.getLexicalValueOfConcept(relation.getIdConcept2(), idThesaurus, idLang);
            if (lexicalOpt.isEmpty() || StringUtils.isBlank(lexicalOpt.get())) {
                nodeBT.setTitle("");
            } else {
                nodeBT.setTitle(lexicalOpt.get());
            }

            // Si besoin de redéfinir le statut avec celui du term (prioritaire)
            termRepository.findByIdTermAndIdThesaurusAndLang(relation.getIdConcept2(), idThesaurus, idLang)
                    .ifPresent(term -> {
                        if (StringUtils.isNotBlank(term.getStatus())) {
                            nodeBT.setStatus(term.getStatus());
                        }
                    });

            result.add(nodeBT);
        }

        result.sort(Comparator.naturalOrder());
        log.debug("Nombre de termes BT trouvés : {}", result.size());
        return result;
    }

    public List<NodeIdValue> getCandidatRelationsRT(String idConceptSelected, String idThesaurus, String lang) {

        var nodeRTs = getListRT(idConceptSelected, idThesaurus, lang);

        if(CollectionUtils.isNotEmpty(nodeRTs)) {
            return nodeRTs.stream()
                    .map(element -> NodeIdValue.builder()
                            .id(element.getIdConcept())
                            .value(element.getTitle())
                            .build())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public List<NodeRT> getListRT(String idConcept, String idThesaurus, String idLang) {
        log.debug("Chargement des relations RT pour le concept '{}' dans le thésaurus '{}'", idConcept, idThesaurus);

        var rawRelations = hierarchicalRelationshipRepository.findRelatedConcepts(idConcept, idThesaurus);
        List<NodeRT> result = new ArrayList<>();

        for (RelatedRelationProjection relation : rawRelations) {
            NodeRT nodeRT = new NodeRT();
            nodeRT.setIdConcept(relation.getIdConcept2());
            nodeRT.setRole(relation.getRole());
            nodeRT.setStatus(relation.getStatus());

            String lexicalValue = termRepository
                    .getLexicalValueOfConcept(relation.getIdConcept2(), idThesaurus, idLang)
                    .orElse("__" + relation.getIdConcept2());

            nodeRT.setTitle(lexicalValue);
            result.add(nodeRT);
        }

        result.sort(Comparator.naturalOrder());
        log.debug("{} relations RT trouvées pour le concept '{}'", result.size(), idConcept);
        return result;
    }

    public List<HierarchicalRelationship> getListConceptRelationParRole(String idConcept, String idThesaurus, String role) {

        log.debug("Recherche des concepts en relation avec le concept {} et avec le rôle {}", idConcept, role);
        return hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(idThesaurus, idConcept, role);
    }

    public boolean isConceptHaveRelationBT(String idConcept, String idThesaurus) {

        log.debug("Vérifier si un concept a une relation BT (term générique");
        var result = hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(idThesaurus, idConcept, "BT");
        return CollectionUtils.isNotEmpty(result);
    }

    public List<String> getListIdBT(String idConcept, String idThesaurus) {

        log.debug("Recherche des id des terms génériques du concept id {}", idConcept);
        var listIdBT = hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(idThesaurus, idConcept, "BT");
        return listIdBT.stream().map(HierarchicalRelationship::getIdConcept2).toList();
    }

    public List<HierarchicalRelationship> getListLoopRelations(String role, String idThesaurus) {

        log.debug("Recherche de relation dans le thésaurus {} et avec le rôle {}", idThesaurus, role);
        var result = hierarchicalRelationshipRepository.getListLoopRelations(idThesaurus, role);
        if (CollectionUtils.isEmpty(result)) {
            log.debug("Aucune relation trouvée dans le thésaurus {} et avec le rôle {}", idThesaurus, role);
            return List.of();
        }
        return result;
    }

    public void deleteThisRelation(String idConcept1, String idThesaurus, String role, String idConcept2) {

        log.debug("Suppression de la relation entre le concept {} et le concept {}", idConcept1, idThesaurus);
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus, idConcept1, idConcept2, role);
    }


    public List<String> getListIdWhichHaveNt(String idConcept, String idThesaurus) {

        var result = hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept2AndRoleLike(idThesaurus, idConcept, "NT");
        if (CollectionUtils.isEmpty(result)) {
            log.debug("Aucune relation trouvée avec le concept le concept {} et avec le role NR", idConcept);
            return List.of();
        }
        return result.stream().map(HierarchicalRelationship::getIdConcept1).toList();
    }

    public NodeRelation getLoopRelation(String idTheso, String idConcept) {

        log.debug("Recherche d'une relation en boucle pour le concept '{}' dans le thésaurus '{}'", idConcept, idTheso);
        return hierarchicalRelationshipRepository.findBtRelation(idTheso, idConcept)
                .flatMap(rel1 -> {
                    log.debug("Relation BT trouvée : {} → {}", rel1.getIdConcept1(), rel1.getIdConcept2());
                    return hierarchicalRelationshipRepository.findLoopBtRelation(idTheso, rel1.getIdConcept1(), rel1.getIdConcept2());
                })
                .map(rel -> {
                    log.debug("Relation en boucle détectée : {} → {}", rel.getIdConcept1(), rel.getIdConcept2());
                    NodeRelation nodeRelation = new NodeRelation();
                    nodeRelation.setIdConcept1(rel.getIdConcept1());
                    nodeRelation.setIdConcept2(rel.getIdConcept2());
                    nodeRelation.setRelation(rel.getRole());
                    return nodeRelation;
                })
                .orElseGet(() -> {
                    log.debug("Aucune relation en boucle trouvée pour le concept '{}'", idConcept);
                    return null;
                });
    }

    public List<String> getListIdOfTopTermForRepair(String idThesaurus) {
        try {
            List<String> topConcepts = hierarchicalRelationshipRepository.findTopConceptsWithNTOnly(idThesaurus);
            List<String> isolatedConcepts = hierarchicalRelationshipRepository.findIsolatedConcepts(idThesaurus);

            Set<String> merged = new LinkedHashSet<>();
            merged.addAll(topConcepts);
            merged.addAll(isolatedConcepts);

            log.debug("Top terms à réparer pour le thésaurus '{}': {} concepts", idThesaurus, merged.size());
            return new ArrayList<>(merged);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des TopTerms pour réparation du thésaurus '{}'", idThesaurus, e);
            return new ArrayList<>();
        }
    }

    @Transactional
    public void deleteRelationBT(String idConceptNT, String idThesaurus, String idConceptBT, int idUser) {

        log.debug("Suppression la relatio, entre le terme générique {} au concept {}", idConceptNT, idConceptBT);
        addRelationHistorique(idConceptNT, idThesaurus, idConceptBT, "BT", idUser, "DEL");

        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus,
                idConceptNT, idConceptBT, "BT");

        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus,
                idConceptBT, idConceptNT, "NT");
    }

    @Transactional
    public void deleteRelationRT(String idConcept1, String idThesaurus, String idConcept2, int idUser) {

        log.debug("Suppression la relation entre le terme associé {} au concept {}", idConcept1, idConcept2);
        addRelationHistorique(idConcept1, idThesaurus, idConcept2, "RT", idUser, "DEL");

        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus,
                idConcept1, idConcept2, "RT");

        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus,
                idConcept2, idConcept1, "RT");
    }

    private void addRelationHistorique(String idConcept1, String idThesaurus, String idConcept2, String role, int idUser, String action) {

        log.debug("Enregistrement un nouveau historique des relations");
        hierarchicalRelationshipHistoriqueRepository.save(HierarchicalRelationshipHistorique.builder()
                .idConcept1(idConcept1)
                .idConcept2(idConcept2)
                .idThesaurus(idThesaurus)
                .modified(new Date())
                .idUser(idUser)
                .action(action)
                .role(role)
                .build());
    }

    @Transactional
    public void addRelationBT(String idConceptNT, String idThesaurus, String idConceptBT, int idUser) {

        log.debug("Ajouter une relation de type terme générique au concept {}", idConceptNT);
        addRelationHistorique(idConceptNT, idThesaurus, idConceptBT, "BT", idUser, "ADD");

        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(idConceptNT)
                .idConcept2(idConceptBT)
                .idThesaurus(idThesaurus)
                .role("BT")
                .build());

        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(idConceptBT)
                .idConcept2(idConceptNT)
                .idThesaurus(idThesaurus)
                .role("NT")
                .build());
    }

    @Transactional
    public void addRelationNT(String idConcept, String idThesaurus, String idConceptNT, int idUser) {

        log.debug("Ajouter une relation entre le terme spécifique {} et le concept {}", idConcept, idConceptNT);
        addRelationHistorique(idConcept, idThesaurus, idConceptNT, "NT", idUser, "ADD");

        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(idConcept)
                .idConcept2(idConceptNT)
                .idThesaurus(idThesaurus)
                .role("NT")
                .build());

        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(idConceptNT)
                .idConcept2(idConcept)
                .idThesaurus(idThesaurus)
                .role("BT")
                .build());
    }

    /**
     * permet de changer la relation entre deux concepts concept1 = concept de
     * départ concept2 = concept d'arriver directRelation = la relation à mettre
     * en place exp NT, NTI ...inverseRelation = la relation reciproque qu'il
     * faut ajouter exp : BT, BTI ...
     */
    @Transactional
    public boolean updateRelationNT(String idConcept1, String idConcept2, String idThesaurus, String directRelation, String inverseRelation, int idUser) {

        hierarchicalRelationshipRepository.updateRole(directRelation, idConcept1, idConcept2, idThesaurus);
        hierarchicalRelationshipRepository.updateRole(inverseRelation, idConcept2, idConcept1, idThesaurus);

        addRelationHistorique(idConcept1, idThesaurus, idConcept2, directRelation, idUser, "UPDATE");
        return true;
    }

    @Transactional
    public boolean addRelationRT(String idConcept1, String idThesaurus, String idConcept2, int idUser) {
        try {
            log.debug("Ajouter une relation associative entre les deux concepts {} et {}", idConcept1, idConcept2);
            addRelationHistorique(idConcept1, idThesaurus, idConcept2, "RT", idUser, "ADD");
            hierarchicalRelationshipRepository.save(
                    HierarchicalRelationship.builder()
                            .idConcept1(idConcept1)
                            .idConcept2(idConcept2)
                            .idThesaurus(idThesaurus)
                            .role("RT")
                            .build()
            );
            hierarchicalRelationshipRepository.save(
                    HierarchicalRelationship.builder()
                            .idConcept1(idConcept2)
                            .idConcept2(idConcept1)
                            .idThesaurus(idThesaurus)
                            .role("RT")
                            .build()
            );

            return true;
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de la relation RT", e);
            return false;
        }
    }


    @Transactional
    public void deleteCustomRelationship(String idConcept1, String idThesaurus, String idConcept2, int idUser,
                                         String conceptType, boolean isReciprocal) {

        log.debug("Supprimer une relation qualificatif au concept {}", idConcept1);
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus,
                idConcept1, idConcept2, conceptType);

        if(isReciprocal){
            hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus,
                    idConcept2, idConcept1, conceptType);
        }

        addRelationHistorique(idConcept1, idThesaurus, idConcept2, "QUALIFIER", idUser, "DEL");;
    }

    @Transactional
    public void deleteRelationNT(String idConcept1, String idThesaurus, String idConcept2, int idUser) {

        log.debug("Supprimer une relation entre un terme spécifique au concept {}", idConcept1);
        addRelationHistorique(idConcept1, idThesaurus, idConcept2, "RT", idUser, "DELETE");

        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus,
                idConcept1, idConcept2, "NT");

        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(idThesaurus,
                idConcept2, idConcept1, "BT");
    }

    @Transactional
    public void addCustomRelationship(String idConcept1, String idThesaurus, String idConcept2, int idUser,
                                         String relationType, boolean isReciprocal) {

        log.debug("Ajouter une relation personnalisée entre le concept {} et le concept {}", idConcept1, idConcept2);
        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(idConcept1)
                .idConcept2(idConcept2)
                .idThesaurus(idThesaurus)
                .role(relationType)
                .build());

        if(isReciprocal) {
            hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                    .idConcept1(idConcept2)
                    .idConcept2(idConcept1)
                    .idThesaurus(idThesaurus)
                    .role(relationType)
                    .build());
        }

        addRelationHistorique(idConcept1, idThesaurus, idConcept2, relationType, idUser, "ADD");
    }


    public boolean isConceptHaveRelationNTorBT(String idConcept1, String idConcept2, String idThesaurus) {

        log.debug("Verifier si le Concept {} a une relation NT avec le concept {}", idConcept1, idConcept2);
        var result = hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRole(idThesaurus, idConcept1, idConcept2);
        return CollectionUtils.isNotEmpty(result);
    }

    public boolean isConceptHaveRelationRT(String idConcept1, String idConcept2, String idThesaurus) {

        log.debug("Vérification si le concept {} dispose d'une relation de type RT", idConcept1);
        return hierarchicalRelationshipRepository.existsRelationRT(idConcept1, idConcept2, idThesaurus);
    }

    public boolean isConceptHaveBrother(String idConcept1, String idConcept2, String idThesaurus) {

        log.debug("Vérification si le concept {} a le frère {}", idConcept1, idConcept2);
        return hierarchicalRelationshipRepository.existsBrotherRelation(idConcept1, idConcept2, idThesaurus);
    }

    public List<String> getListIdOfBT(String idConcept, String idThesaurus) {

        log.debug("Recherche de la liste des BT pour le concept {}", idConcept);
        return hierarchicalRelationshipRepository.findIdsOfBroaderConcepts(idConcept, idThesaurus);
    }

    public boolean isConceptHaveManyRelationBT(String idConcept, String idThesaurus) {

        log.debug("Vérification si le concept {} contient plusieurs relation de type BT", idConcept);
        return hierarchicalRelationshipRepository.countBroaderRelations(idConcept, idThesaurus) > 1;
    }

    public List<NodeCustomRelation> getAllNodeCustomRelation(String idConcept, String idThesaurus, String idLang, String interfaceLang) {

        log.debug("Recherche de toutes les relations client avec le concept {}", idConcept);
        var projections = hierarchicalRelationshipRepository.findCustomRelations(idConcept, idThesaurus);
        if (CollectionUtils.isEmpty(projections)) {
            log.debug("Aucune relation personnalisée n'est trouvée !");
            return List.of();
        }

        return projections.stream()
                .map(element -> {
                    var conceptType = conceptTypeService.getNodeTypeConcept(element.getRole(), idThesaurus);
                    return NodeCustomRelation.builder()
                            .targetConcept(element.getIdConcept2())
                            .relation(element.getRole())
                            .targetLabel(termService.getLexicalValueOfConcept(element.getIdConcept2(), idThesaurus, idLang))
                            .relationLabel("fr".equalsIgnoreCase(interfaceLang) ? conceptType.getLabelFr() : conceptType.getLabelEn())
                            .reciprocal(conceptType.isReciprocal())
                            .build();
                })
                .toList();
    }

    public List<NodeNT> getListNT(String idConcept, String idThesaurus, String idLang, int step, int offset) {

        var relations = (step == -1)
                ? hierarchicalRelationshipRepository.findNTByConceptNoLimit(idConcept, idThesaurus)
                : hierarchicalRelationshipRepository.findNTByConceptWithPagination(idConcept, idThesaurus, step, offset);

        var nodeListNT = relations.stream()
                .map(p -> NodeNT.builder()
                        .idConcept(p.getIdConcept2())
                        .role(p.getRole())
                        .title(termService.getLexicalValueOfConcept(p.getIdConcept2(), idThesaurus, idLang))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.sort(nodeListNT);
        return nodeListNT;
    }

    public List<NodeHieraRelation> getAllRelationsOfConcept(String idConcept, String idThesaurus) {

        return hierarchicalRelationshipRepository.getRelationsWithIdentifiers(idConcept, idThesaurus).stream()
                .map(proj ->
                    NodeHieraRelation.builder()
                            .role(proj.getRole())
                            .uri(NodeUri.builder()
                                    .idConcept(proj.getIdConcept())
                                    .idArk(proj.getIdArk() != null ? proj.getIdArk() : "")
                                    .idHandle(proj.getIdHandle() != null ? proj.getIdHandle() : "")
                                    .idDoi(proj.getIdDoi() != null ? proj.getIdDoi() : "")
                                    .build())
                            .build()
                ).toList();
    }

    public List<NodeTypeRelation> getTypesRelationsNT() {
        return ntTypeRepository.findAll()
                .stream()
                .map(ntType -> {
                    NodeTypeRelation relation = new NodeTypeRelation();
                    relation.setRelationType(ntType.getRelation());
                    relation.setDescriptionFr(ntType.getDescriptionFr());
                    relation.setDescriptionEn(ntType.getDescriptionEn());
                    return relation;
                })
                .collect(Collectors.toList());
    }
}
