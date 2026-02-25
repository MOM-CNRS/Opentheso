package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.bean.importexport.outils.HTMLLinkElement;
import fr.cnrs.opentheso.bean.importexport.outils.HtmlLinkExtraction;
import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.entites.ConceptFacet;
import fr.cnrs.opentheso.entites.ConceptReplacedBy;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.models.ConceptIdView;
import fr.cnrs.opentheso.models.NodeDeprecatedProjection;
import fr.cnrs.opentheso.models.TopConceptProjection;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.candidats.VoteDto;
import fr.cnrs.opentheso.models.concept.NodeConcept;
import fr.cnrs.opentheso.models.concept.NodeConceptExport;
import fr.cnrs.opentheso.models.concept.NodeConceptSearch;
import fr.cnrs.opentheso.models.concept.NodeConceptTree;
import fr.cnrs.opentheso.models.concept.NodeFullConcept;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.nodes.NodeGps;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.models.relations.NodeHieraRelation;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.CandidatMessageRepository;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.CandidatVoteRepository;
import fr.cnrs.opentheso.repositories.ConceptCandidatRepository;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptReplacedByRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptTermCandidatRepository;
import fr.cnrs.opentheso.repositories.ConceptTreeRepository;
import fr.cnrs.opentheso.repositories.ConceptTypeRepository;
import fr.cnrs.opentheso.repositories.CorpusLinkRepository;
import fr.cnrs.opentheso.repositories.ExternalImageRepository;
import fr.cnrs.opentheso.repositories.ExternalResourceRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipHistoriqueRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.NoteHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PermutedRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationDetailRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationRepository;
import fr.cnrs.opentheso.repositories.PropositionRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.ws.api.NodeDatas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptService {

    private final ConceptDcTermRepository conceptDcTermRepository;
    private final ConceptHistoriqueRepository conceptHistoriqueRepository;
    private final ConceptReplacedByRepository conceptReplacedByRepository;
    private final CorpusLinkRepository corpusLinkRepository;
    private final ConceptFacetRepository conceptFacetRepository;
    private final ConceptRepository conceptRepository;
    private final PermutedRepository permutedRepository;
    private final PreferenceService preferenceService;
    private final TermService termService;
    private final NonPreferredTermService nonPreferredTermService;
    private final GroupService groupService;
    private final ResourceService resourceService;
    private final FacetService facetService;
    private final PreferredTermRepository preferredTermRepository;
    private final AlignmentService alignmentService;
    private final HandleConceptService handleConceptService;
    private final RelationService relationService;
    private final NoteService noteService;
    private final HandleService handleHelper;
    private final HandleService handleService;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final ImageService imageService;
    private final GpsService gpsService;
    private final ConceptTypeRepository conceptTypeRepository;
    private final ConceptTreeRepository conceptTreeRepository;
    private final ExternalResourcesRepository externalResourcesRepository;
    private final UserRepository userRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final TermRepository termRepository;
    private final NoteRepository noteRepository;
    private final NoteHistoriqueRepository noteHistoriqueRepository;
    private final ConceptCandidatRepository conceptCandidatRepository;
    private final CandidatStatusRepository candidatStatusRepository;
    private final CandidatVoteRepository candidatVoteRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final HierarchicalRelationshipHistoriqueRepository hierarchicalRelationshipHistoriqueRepository;
    private final ConceptTermCandidatRepository conceptTermCandidatRepository;
    private final AlignementRepository alignementRepository;
    private final PropositionRepository propositionRepository;
    private final GpsRepository gpsRepository;
    private final ExternalResourceRepository externalResourceRepository;
    private final ThesaurusArrayRepository thesaurusArrayRepository;
    private final CandidatMessageRepository candidatMessageRepository;
    private final ExternalImageRepository externalImageRepository;
    private final ConceptAddService conceptAddService;
    private final ThesaurusRepository thesaurusRepository;
    private final PropositionModificationRepository propositionModificationRepository;
    private final PropositionModificationDetailRepository propositionModificationDetailRepository;
    private final ConceptGroupHistoriqueRepository conceptGroupHistoriqueRepository;
    private final ConceptGroupLabelHistoriqueRepository conceptGroupLabelHistoriqueRepository;


    public NodeFullConcept getConcept(String idConcept, String idThesaurus, String idLang, int offset, int step, boolean isPrivate) {
        if(StringUtils.isEmpty(idThesaurus) || StringUtils.isEmpty(idConcept)) return null;
        return resourceService.getFullConcept(idThesaurus, idConcept, idLang, offset, step, isPrivate);
    }

    public void updateConcept(Concept concept) {

        log.debug("Mise à jour du concept id {}", concept.getId());
        conceptRepository.save(concept);
    }

    public List<Concept> getConceptByThesaurusAndTopConceptAndStatusNotLike(String idThesaurus, boolean isTopConcept, String status) {

        log.debug("Recherche des concepts par thésaurus {}, topConcept {} et status {}", idThesaurus, isTopConcept, status);
        return conceptRepository.findAllByIdThesaurusAndTopConceptAndStatusNotLike(idThesaurus, isTopConcept, status);
    }

    public List<Concept> getConceptByThesaurusAndStatus(String idThesaurus, String status) {

        return conceptRepository.findAllByIdThesaurusAndStatus(idThesaurus, status);
    }

    public List<String> getAllIdConceptOfThesaurus(String idThesaurus) {

        log.debug("Recherche de tous les concepts présent dans le thésaurus id {}", idThesaurus);
        var concepts = conceptRepository.findAllByIdThesaurusAndStatusNot(idThesaurus, "CA");
        if (CollectionUtils.isEmpty(concepts)) {
            log.debug("Aucun concept n'est trouvé dans le thésaurus id {}", idThesaurus);
            return List.of();
        }
        log.debug("{} concepts trouvés !", concepts.size());
        return concepts.stream().map(Concept::getIdConcept).toList();
    }

    public boolean isTopConcept(String idConcept, String idThesaurus) {

        log.debug("Vérifier si le concept id {} est un top concept {}", idConcept, idThesaurus);
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (concept.isEmpty()) {
            log.debug("Aucun concept n'est trouvé avec l'id {}", idConcept);
            return false;
        }

        return concept.get().getTopConcept();
    }

    public Concept getConcept(String idConcept) {

        log.debug("Recherche du concept avec l'id {}", idConcept);
        var concept = conceptRepository.findByIdConcept(idConcept);
        if (concept.isEmpty()) {
            log.debug("Aucun concept n'est trouvé avec l'id {}", idConcept);
            return null;
        }

        return concept.get(0);
    }

    public Concept getConcept(String idConcept, String idThesaurus) {

        log.debug("Recherche du concept avec l'id {} dans le thésaurus id {}", idConcept, idThesaurus);
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (concept.isEmpty()) {
            log.debug("Aucun concept n'est trouvé avec l'id {}", idConcept);
            return null;
        }

        return concept.get();
    }

    public void cleanConcept() {

        log.debug("Nettoyage des concepts");
        conceptRepository.cleanConcept();
    }

    public void setConceptType(String idThesaurus, String idConcept, String type) {

        log.debug("Mise à jour du type de concept {} pour le concept id {}", type, idConcept);
        conceptRepository.setConceptType(type, new Date(), idConcept, idThesaurus);
        log.debug("Mise à jour du type de concept {} est terminée pour le concept id {}", type, idConcept);
    }

    public void setTopConceptTag(boolean status, String idConcept, String idThesaurus) {

        log.debug("Mise à jour du Flag 'topConcept' du concept id {}", idConcept);
        conceptRepository.setTopConceptTag(status, idConcept, idThesaurus);
    }

    @Transactional
    public void deleteByThesaurus(String idThesaurus) {

        log.debug("Suppression de tous les concepts présents dans le thésaurus id {}", idThesaurus);
        permutedRepository.deleteAllByIdThesaurus(idThesaurus);
        conceptReplacedByRepository.deleteAllByIdThesaurus(idThesaurus);
        corpusLinkRepository.deleteAllByIdThesaurus(idThesaurus);
        conceptDcTermRepository.deleteAllByIdThesaurus(idThesaurus);
        conceptHistoriqueRepository.deleteAllByIdThesaurus(idThesaurus);
        conceptTypeRepository.deleteAllByIdThesaurus(idThesaurus);
        conceptFacetRepository.deleteAllByIdThesaurus(idThesaurus);
        try {
            log.debug("Suppression des historiques des groups du thésaurus id {}", idThesaurus);
            conceptGroupHistoriqueRepository.deleteAllByIdThesaurus(idThesaurus);

            log.debug("Suppression de tous les labels des groups du thésaurus {}", idThesaurus);
            conceptGroupLabelHistoriqueRepository.deleteAllByIdThesaurus(idThesaurus);

            log.debug("Suppression de tous les relations entre les groups et les concepts du thésaurus {}", idThesaurus);
            conceptGroupConceptRepository.deleteAllByIdThesaurus(idThesaurus);

            conceptRepository.deleteAllByIdThesaurus(idThesaurus);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    public void updateThesaurusId(String oldThesaurusId, String newThesaurusId) {

        log.debug("Modification du thesaurus id dans tous les concepts (du l'id {} vers l'id {})", oldThesaurusId, newThesaurusId);
        permutedRepository.updateThesaurusId(newThesaurusId, oldThesaurusId);
        conceptReplacedByRepository.updateThesaurusId(newThesaurusId, oldThesaurusId);
        corpusLinkRepository.updateThesaurusId(newThesaurusId, oldThesaurusId);
        conceptDcTermRepository.updateThesaurusId(newThesaurusId, oldThesaurusId);
        conceptHistoriqueRepository.updateThesaurusId(newThesaurusId, oldThesaurusId);
        conceptRepository.updateThesaurusId(newThesaurusId, oldThesaurusId);
        conceptTypeRepository.updateThesaurusId(newThesaurusId, oldThesaurusId);
        conceptFacetRepository.updateThesaurusId(newThesaurusId, oldThesaurusId);
    }

    public NodeConceptSearch getConceptForSearch(String idConcept, String idThesaurus, String idLang) {
        log.debug("Chargement du concept '{}' dans le thésaurus '{}' pour la langue '{}'", idConcept, idThesaurus, idLang);
        return buildConceptSearch(idConcept, idThesaurus, idLang);
    }

    public NodeConceptSearch getConceptForSearchFromLabel(String label, String idThesaurus, String idLang) {
        log.debug("Recherche d'un concept depuis le label '{}' (langue '{}', thésaurus '{}')", label, idLang, idThesaurus);

        String conceptId = getOneIdConceptFromLabel(idThesaurus, label, idLang);

        if (StringUtils.isEmpty(conceptId)) {
            String normalizedLabel = fr.cnrs.opentheso.utils.StringUtils.convertString(label);
            log.debug("Aucun concept trouvé via label exact. Tentative via altLabel normalisé : '{}'", normalizedLabel);
            conceptId = conceptRepository.findConceptIdFromAltLabel(idThesaurus, normalizedLabel, idLang).orElse(null);
        }

        if (StringUtils.isEmpty(conceptId)) {
            log.warn("Aucun concept trouvé pour le label '{}'", label);
            return null;
        }

        log.debug("Concept trouvé : '{}'", conceptId);
        return buildConceptSearch(conceptId, idThesaurus, idLang);
    }

    private NodeConceptSearch buildConceptSearch(String idConcept, String idThesaurus, String idLang) {
        NodeConceptSearch node = new NodeConceptSearch();
        node.setIdConcept(idConcept);
        node.setIdTheso(idThesaurus);
        node.setCurrentLang(idLang);

        node.setDeprecated(isDeprecated(idConcept, idThesaurus));

        log.debug("Récupération du prefLabel...");
        node.setPrefLabel(termService.getLexicalValueOfConcept(idConcept, idThesaurus, idLang));

        log.debug("Récupération des traductions...");
        node.setNodeTermTraductions(termService.getTraductionsOfConcept(idConcept, idThesaurus, idLang));

        log.debug("Récupération des termes génériques (BT)...");
        node.setNodeBT(relationService.getListBT(idConcept, idThesaurus, idLang));

        log.debug("Récupération des termes spécifiques (NT)...");
        node.setNodeNT(relationService.getListNT(idConcept, idThesaurus, idLang, 21, 0));

        log.debug("Récupération des termes associés (RT)...");
        node.setNodeRT(relationService.getListRT(idConcept, idThesaurus, idLang));

        log.debug("Récupération des termes non préférés (EM)...");
        node.setNodeEM(nonPreferredTermService.getNonPreferredTerms(idConcept, idThesaurus, idLang));

        log.debug("Récupération des groupes de concept...");
        node.setNodeConceptGroup(groupService.getListGroupOfConcept(idThesaurus, idConcept, idLang));

        log.debug("Concept chargé avec succès : '{}'", idConcept);
        return node;
    }

    public boolean isDeprecated(String idConcept, String idThesaurus) {

        log.debug("Vérifier si le concept id {} est déprécié", idConcept);
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        log.debug("Le concept id {} est déprécié : {}", idConcept, concept.isPresent() && "dep".equalsIgnoreCase(concept.get().getStatus()));
        return concept.isPresent() && "dep".equalsIgnoreCase(concept.get().getStatus());
    }

    public String getOneIdConceptFromLabel(String idTheso, String label, String idLang) {

        log.debug("Recherche d'un idConcept depuis un label exact dans le thésaurus '{}', langue '{}', valeur '{}'", idTheso, idLang, label);
        var normalizedLabel = fr.cnrs.opentheso.utils.StringUtils.convertString(label);
        return conceptRepository.findConceptIdFromLabel(idTheso, normalizedLabel, idLang).orElse(null);
    }

    @Transactional
    public boolean deleteConcept(String idConcept, String idThesaurus) {

        log.debug("Suppression du Concept id {} avec ses relations et traductions", idConcept);
        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(idThesaurus, idConcept);
        if (preferredTerm.isPresent()) {
            termService.deleteTerm(preferredTerm.get().getIdTerm(), idThesaurus);
        }

        relationService.deleteAllRelationOfConcept(idConcept, idThesaurus);
        noteService.deleteNotes(idConcept, idThesaurus);
        alignmentService.deleteAlignmentOfConcept(idConcept, idThesaurus);
        conceptRepository.deleteAllByIdThesaurusAndIdConcept(idThesaurus, idConcept);
        facetService.deleteFacetsByConceptAndThesaurus(idConcept, idThesaurus);
        groupService.deleteAllGroupOfConcept(idConcept, idThesaurus);
        gpsRepository.deleteByIdConceptAndIdTheso(idConcept, idThesaurus);
        externalImageRepository.deleteAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        externalResourceRepository.deleteAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        propositionModificationDetailRepository.deleteByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        propositionModificationRepository.deleteAllByIdConceptAndIdTheso(idConcept, idThesaurus);
        propositionRepository.deleteAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);

        deleteConceptReplacedby(idThesaurus, idConcept);

        var preferences = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preferences != null && preferences.isUseHandle()) {
            var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
            if (concept.isPresent() && StringUtils.isNotEmpty(concept.get().getIdHandle())) {
                handleConceptService.deleteIdHandle(idConcept, concept.get().getIdHandle(), idThesaurus);
            }
        }
        return true;
    }

    public boolean deleteBranchConcept(String idConceptTop, String idThesaurus) {

        log.debug("Suppression du concept (id {}) avec ses relations et traductions", idConceptTop);
        var idConcepts = getIdsOfBranch2(idThesaurus, idConceptTop);

        // test si les concepts fils ont une poly-hiérarchie, on refuse la suppression (qui peut supprimer plusieurs branches
        for (String idConcept : idConcepts) {
            if (relationService.isConceptHaveManyRelationBT(idConcept, idThesaurus)) {
                log.error("Le concept id {} dispose de plusieurs relation de type BT", idConcept);
                return false;
            }
        }

        // supprimer les concepts
        for (String idConcept : idConcepts) {
            deleteConcept(idConcept, idThesaurus);
        }
        return true;
    }

    public List<String> getIdsOfBranch2(String idThesaurus, String idConceptDeTete) {

        List<String> lisIds = new ArrayList<>();
        return getIdsOfBranch2__(idThesaurus, idConceptDeTete, lisIds);
    }

    private List<String> getIdsOfBranch2__(String idTheso, String idConceptDeTete, List<String> lisIds) {

        // pour éviter les boucles à l'infini
        if (lisIds.contains(idConceptDeTete)) {
            return lisIds;
        }

        lisIds.add(idConceptDeTete);
        List<String> listIdsOfConceptChildren = resourceService.getConceptsTT(idTheso, idConceptDeTete);

        for (String listIdsOfConceptChildren1 : listIdsOfConceptChildren) {
            getIdsOfBranch2__(idTheso, listIdsOfConceptChildren1, lisIds);
        }
        return lisIds;
    }

    public List<NodeIdValue> getListConceptsOfGroup(String idThesaurus, String idLang, String idGroup, boolean isSortByNotation) {
        return getConceptsByGroup(idThesaurus, idLang, idGroup, isSortByNotation, false);
    }

    public List<NodeIdValue> getListTopConceptsOfGroup(String idThesaurus, String idLang, String idGroup, boolean isSortByNotation) {
        return getConceptsByGroup(idThesaurus, idLang, idGroup, isSortByNotation, true);
    }

    private List<NodeIdValue> getConceptsByGroup(String idThesaurus, String idLang, String idGroup,
                                                 boolean isSortByNotation, boolean topOnly) {
        String typeLabel = topOnly ? "Top Concepts" : "Concepts";
        log.debug("Chargement des {} du groupe '{}' dans le thésaurus '{}' (tri par notation: {})", typeLabel, idGroup, idThesaurus, isSortByNotation);

        List<Object[]> rawConcepts = topOnly
                ? conceptRepository.findTopConceptsByGroup(idThesaurus, idGroup)
                : conceptRepository.findConceptsByGroup(idThesaurus, idGroup);

        List<NodeIdValue> result = new ArrayList<>(rawConcepts.stream()
                .map(row -> {
                    String idConcept = (String) row[0];
                    String notation = (String) row[1];
                    String lexicalValue = termService.getLexicalValueOfConcept(idConcept, idThesaurus, idLang);

                    NodeIdValue node = new NodeIdValue();
                    node.setId(idConcept);
                    node.setNotation(notation);
                    node.setValue(StringUtils.isEmpty(lexicalValue) ? "__" + idConcept : lexicalValue);
                    return node;
                })
                .collect(Collectors.toCollection(ArrayList::new)));

        if (!isSortByNotation) {
            Collections.sort(result);
        }

        log.debug("{} {} récupérés pour le groupe '{}'", result.size(), typeLabel, idGroup);
        return result;
    }

    public void deleteConceptReplacedby(String idThesaurus, String idConcept) {

        log.debug("Supprimer le concept id {} de la table concept_replacedby", idConcept);
        conceptReplacedByRepository.deleteAllByIdConcept1AndIdThesaurus(idConcept, idThesaurus);
        conceptReplacedByRepository.deleteAllByIdConcept2AndIdThesaurus(idConcept, idThesaurus);
    }

    public boolean updateDateOfConcept(String idThesaurus, String idConcept, int contributor) {
        log.debug("Mise à jour de la date du concept id {}", idConcept);
        int rows = conceptRepository.updateDateOfConcept(idThesaurus, idConcept, contributor);
        if (rows > 0) {
            log.debug("Mise à jour réussie pour le concept {}", idConcept);
            return true;
        } else {
            log.warn("Aucun concept trouvé pour idConcept={} et thesaurus={}", idConcept, idThesaurus);
            return false;
        }
    }

    public List<String> getIdsOfBranchWithoutLoop(String idConceptDeTete, String idThesaurus) {
        log.debug("Rechercher tous les identifiants d'une branche en partant du concept id {}", idConceptDeTete);
        List<String> lisIds = new ArrayList<>();
        return getIdsOfBranchWithoutLoop__(idConceptDeTete, idThesaurus, lisIds);
    }

    private List<String> getIdsOfBranchWithoutLoop__(String idConceptDeTete, String idThesaurus, List<String> lisIds) {

        if (lisIds.contains(idConceptDeTete)) {
            return lisIds;
        }
        lisIds.add(idConceptDeTete);
        var listIdsOfConceptChildren = getListChildrenOfConcept(idConceptDeTete, idThesaurus);
        for (String listIdsOfConceptChildren1 : listIdsOfConceptChildren) {
            getIdsOfBranchWithoutLoop__(listIdsOfConceptChildren1, idThesaurus, lisIds);
        }
        return lisIds;
    }

    /**
     * Cette fonction permet de récupérer les Ids des concepts suivant l'id du
     * Concept-Père et le thésaurus sous forme de classe tableau pas de tri
     */
    public List<String> getListChildrenOfConcept(String idConcept, String idThesaurus) {

        log.debug("Recherche des concept id {} de la table concept_children", idConcept);
        var relations = relationService.getListConceptRelationParRole(idConcept, idThesaurus, "NT");

        if (CollectionUtils.isEmpty(relations)) return List.of();
        return relations.stream().map(HierarchicalRelationship::getIdConcept2).toList();
    }

    /**
     * Cette fonction permet de retrouver tous tes identifiants d'une branche en
     * partant du concept en paramètre avec limit pour le nombre de résultat
     */
    public List<String> getIdsOfBranchLimited(String idConceptDeTete, String idThesaurus, int limit) {
        List<String> lisIds = new ArrayList<>();
        lisIds = getIdsOfBranchLimited__(idConceptDeTete, idThesaurus, lisIds, limit);
        return lisIds;
    }

    private List<String> getIdsOfBranchLimited__(String idConceptDeTete, String idThesaurus, List<String> lisIds, int limit) {

        if (lisIds.size() > limit) {
            return lisIds;
        }
        lisIds.add(idConceptDeTete);

        var listIdsOfConceptChildren = getListChildrenOfConcept(idConceptDeTete, idThesaurus);
        for (String listIdsOfConceptChildren1 : listIdsOfConceptChildren) {
            getIdsOfBranchLimited__(listIdsOfConceptChildren1, idThesaurus, lisIds, limit);
        }
        return lisIds;
    }

    /**
     * Cette fonction permet de retrouver tous tes identifiants d'une branche en partant du concept en paramètre
     */
    public List<String> getIdsOfBranch(String idConceptDeTete, String idTheso) {
        List<String> lisIds = new ArrayList<>();
        lisIds = getIdsOfBranch__(idConceptDeTete, idTheso, lisIds);
        return lisIds;
    }

    private List<String> getIdsOfBranch__(String idConceptDeTete, String idTheso, List<String> lisIds) {

        if (lisIds.contains(idConceptDeTete)) {
            return lisIds;
        }
        lisIds.add(idConceptDeTete);
        var listIdsOfConceptChildren = getListChildrenOfConcept(idConceptDeTete, idTheso);
        for (String listIdsOfConceptChildren1 : listIdsOfConceptChildren) {
            getIdsOfBranch__(listIdsOfConceptChildren1, idTheso, lisIds);
        }
        return lisIds;
    }

    public Date getLastModification(String idThesaurus) {
        try {
            var dates = conceptRepository.findLastModifiedDates(idThesaurus, PageRequest.of(0, 1));
            if (!dates.isEmpty()) {
                log.debug("Dernière modification du thésaurus {} : {}", idThesaurus, dates.get(0));
                return dates.get(0);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la dernière date de modification pour le thésaurus : " + idThesaurus, e);
        }
        return null;
    }

    public NodeDatas getConceptForGraph(String idConcept, String idTheso, String idLang) {

        var label = termService.getLexicalValueOfConcept(idConcept, idTheso, idLang);
        var definitionNotes = noteService.getNoteByConceptAndThesaurusAndLangAndType(idConcept, idTheso, idLang, "definition");
        var labels = nonPreferredTermRepository.findAltLabelsByConceptAndThesaurusAndLang(idConcept, idTheso, idLang);

        return NodeDatas.builder()
                .name(StringUtils.isEmpty(label) ? "(" + idConcept + ")" : label)
                .url(getUri(idConcept, idTheso))
                .definition(definitionNotes.stream().map(NodeNote::getLexicalValue).toList())
                .synonym(labels)
                .build();
    }

    public List<NodeDeprecated> getAllDeprecatedConceptOfThesaurus(String idTheso, String idLang) {

        var results = conceptRepository.findAllDeprecatedConcepts(idTheso, idLang);
        List<NodeDeprecated> deprecatedList = new ArrayList<>();

        for (NodeDeprecatedProjection projection : results) {
            NodeDeprecated node = new NodeDeprecated();
            node.setDeprecatedId(projection.getIdConcept());
            node.setDeprecatedLabel(projection.getLexicalValue());
            node.setModified(projection.getModified());
            node.setUserName(projection.getUsername());

            List<NodeIdValue> replacesValues = getAllReplacedBy(idTheso, node.getDeprecatedId(), idLang);

            if (!replacesValues.isEmpty()) {
                node.setReplacedById(replacesValues.stream().map(NodeIdValue::getId).collect(Collectors.joining("##")));
                node.setReplacedByLabel(replacesValues.stream().map(NodeIdValue::getValue).collect(Collectors.joining("##")));
            }

            deprecatedList.add(node);
        }

        return deprecatedList;
    }


    /**
     * Cette fonction permet de retourner l'URI du concept en s'adaptant au
     * format défini pour le thésaurus
     */
    private String getUri(String idConcept, String idThesaurus) {

        if (idConcept == null || idThesaurus == null) {
            return "";
        }

        var preferences = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preferences == null) {
            return "";
        }

        return preferences.getCheminSite() + "?idc=" + idConcept + "&idt=" + idThesaurus;
    }

    public List<NodeIdValue> getLastModifiedConcepts(String idThesaurus, String idLang) {

        List<Object[]> results = conceptRepository.findLastModifiedConcepts(idThesaurus, idLang);
        List<NodeIdValue> nodeIdValues = new ArrayList<>();
        for (Object[] row : results) {
            nodeIdValues.add(NodeIdValue.builder()
                    .id((String) row[0])
                    .value((String) row[1])
                    .build());
        }

        return nodeIdValues;
    }



    /**
     * Permet de supprimer tous les identifiants Handle de la table Concept et
     * de la plateforme (handle.net) via l'API REST pour un thésaurus donné
     * suite à une suppression d'un thésaurus
     */
    public void deleteAllIdHandle(String idThesaurus) {

        var preferences = preferenceService.getThesaurusPreferences(idThesaurus);
        if (preferences == null || !preferences.isUseHandle()) {
            return;
        }

        var tabIdHandle = conceptRepository.findAllNonEmptyIdHandleByThesaurus(idThesaurus);
        if (preferences.isUseHandleWithCertificat()) {
            if (!handleHelper.deleteAllIdHandle(tabIdHandle, preferences)) {
                log.error("Erreur pendant la suppression du handle : " + handleHelper.getMessage());
            }
        } else {
            // HandleService hs = HandleService.getInstance();
            handleService.applyNodePreference(preferences);
            handleService.connectHandle();
            for (String idHandle : tabIdHandle) {
                try {
                    handleService.deleteHandle(idHandle);
                } catch (Exception ex) {
                    log.error("Erreur pendant la suppression du handle : " + ex.toString());
                }
            }
        }
    }

    public boolean isNotationExist(String idThesaurus, String notation) {

        log.debug("Vérification si l'id du concept {} existe dans le thésaurus {}", notation, idThesaurus);
        var concept = conceptRepository.findAllByIdThesaurusAndNotationLike(idThesaurus, notation);
        return CollectionUtils.isNotEmpty(concept);
    }

    @Transactional
    public boolean setTopConcept(String idConcept, String idThesaurus, boolean isTopConcept) {
        log.debug("Mise à jour du status 'TopConcept' pour le concept {}", idConcept);
        int updated = conceptRepository.updateTopConcept(idConcept, idThesaurus, isTopConcept);
        return updated > 0;
    }

    @Transactional
    public boolean moveBranchFromRootToConcept(String idConcept, String idNewConceptBT, String idThesaurus, int idUser) {

        relationService.addRelationBT(idConcept, idThesaurus, idNewConceptBT, idUser);
        return setTopConcept(idConcept, idThesaurus, false);
    }

    public boolean moveBranchFromConceptToRoot(String idConcept, String idOldConceptBT, String idThesaurus, int idUser) {

        relationService.deleteRelationBT(idConcept, idThesaurus, idOldConceptBT, idUser);
        return setTopConcept(idConcept, idThesaurus, true);
    }

    public boolean updateNotation(String idConcept, String idThesaurus, String notation) {

        log.debug("Mise à jour du notation pour le concept {}", idConcept);
        conceptRepository.updateNotation(idConcept, idThesaurus, notation);

        int rowsAffected = conceptRepository.updateNotation(idConcept, idThesaurus, notation);

        if (rowsAffected > 0) {
            log.debug("Aucun concept trouvé avec cet idConcept/idThesaurus.");
            return true;
        } else {
            log.debug("Aucun concept trouvé avec cet idConcept/idThesaurus.");
            return false;
        }
    }

    public boolean moveBranchFromConceptToConcept(String idConcept, List<String> idOldBTsToDelete, String idNewConceptBT,
                                                  String idThesaurus, int idUser) {

        if (idOldBTsToDelete.size() == 1 && idOldBTsToDelete.get(0).equalsIgnoreCase(idNewConceptBT)) {
            return true;
        }

        for (String idOldBT : idOldBTsToDelete) {
            relationService.deleteRelationBT(idConcept, idThesaurus, idOldBT, idUser);
        }

        relationService.addRelationBT(idConcept, idThesaurus, idNewConceptBT, idUser);
        return true;
    }

    public boolean haveChildren(String idThesaurus, String idConcept) {

        int childCount = conceptRepository.countChildren(idThesaurus, idConcept);
        if (childCount > 0) {
            return true;
        }

        int facetCount = conceptFacetRepository.countFacets(idThesaurus, idConcept);
        return facetCount > 0;
    }

    public List<NodeConceptTree> getTopConcepts(String idThesaurus, String idLang, boolean sortByNotation, boolean isPrivate) {
        var projections = isPrivate
                ? conceptRepository.findTopConceptsPrivate(idThesaurus)
                : conceptRepository.findTopConcepts(idThesaurus);

        List<NodeConceptTree> list = new ArrayList<>();

        for (TopConceptProjection proj : projections) {
            NodeConceptTree node = new NodeConceptTree();
            node.setIdConcept(proj.getIdConcept());
            node.setNotation(proj.getNotation());
            node.setStatusConcept(proj.getStatus());
            node.setIdThesaurus(idThesaurus);
            node.setIdLang(idLang);
            node.setTopTerm(true);
            node.setTitle(termService.getLexicalValueOfConcept(proj.getIdConcept(), idThesaurus, idLang));
            node.setHaveChildren(haveChildren(idThesaurus, proj.getIdConcept()));
            list.add(node);
        }

        if (!sortByNotation) {
            Collections.sort(list);
        }
        return list;
    }

    public List<NodeConceptTree> getListConcepts(String idConcept, String idThesaurus, String idLang, boolean isSortByNotation) {

        var rows = isSortByNotation
                ? conceptTreeRepository.findConceptsByNotation(idConcept, idThesaurus)
                : conceptTreeRepository.findConceptsAlphabetically(idConcept, idThesaurus);

        List<NodeConceptTree> trees = new ArrayList<>();

        for (Object[] row : rows) {
            NodeConceptTree node = new NodeConceptTree();
            node.setIdConcept((String) row[0]);
            node.setIdThesaurus(idThesaurus);
            node.setIdLang(idLang);
            node.setTerm(true);
            if (isSortByNotation && row.length > 1) {
                node.setNotation((String) row[1]);
            }

            var titleStatus = conceptTreeRepository.findLexicalValueAndStatus(node.getIdConcept(), idThesaurus, idLang);
            if (titleStatus.isPresent()) {
                node.setTitle((String) titleStatus.get()[0]);
                node.setStatusConcept((String) titleStatus.get()[1]);
            } else {
                node.setTitle("");
                node.setStatusConcept("");
            }

            node.setHaveChildren(haveChildren(idThesaurus, node.getIdConcept()));
            trees.add(node);
        }

        if (!isSortByNotation) {
            Collections.sort(trees);
        }

        return trees;
    }

    public List<NodeTree> getTopConceptsWithTermByTheso(String idTheso, String idLang) {
        return conceptRepository.findTopConceptsWithTermByThesaurusAndLang(idTheso, idLang);
    }

    @Transactional
    public boolean moveConceptToAnotherThesaurus(String idConceptToMove, String idThesoFrom, String idThesoTarget) {
        try {
            conceptRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            thesaurusArrayRepository.updateThesaurusByParent(idConceptToMove, idThesoFrom, idThesoTarget);
            conceptHistoriqueRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            termRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            nonPreferredTermRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            noteRepository.updateThesaurusByConcept(idConceptToMove, idThesoFrom, idThesoTarget);
            noteRepository.updateThesaurusByTerm(idConceptToMove, idThesoFrom, idThesoTarget);
            noteHistoriqueRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            preferredTermRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            conceptCandidatRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            candidatStatusRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            candidatMessageRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            candidatVoteRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            conceptGroupConceptRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            hierarchicalRelationshipRepository.updateThesaurusByConcept1(idConceptToMove, idThesoFrom, idThesoTarget);
            hierarchicalRelationshipRepository.updateThesaurusByConcept2(idConceptToMove, idThesoFrom, idThesoTarget);
            hierarchicalRelationshipHistoriqueRepository.updateThesaurusByConcept1(idConceptToMove, idThesoFrom, idThesoTarget);
            hierarchicalRelationshipHistoriqueRepository.updateThesaurusByConcept2(idConceptToMove, idThesoFrom, idThesoTarget);
            conceptTermCandidatRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            alignementRepository.updateInternalThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            propositionRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            conceptReplacedByRepository.updateThesaurusByConcept1(idConceptToMove, idThesoFrom, idThesoTarget);
            conceptReplacedByRepository.updateThesaurusByConcept2(idConceptToMove, idThesoFrom, idThesoTarget);
            gpsRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            conceptFacetRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            externalResourceRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            externalImageRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            propositionRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            conceptDcTermRepository.updateThesaurus(idConceptToMove, idThesoFrom, idThesoTarget);
            return true;
        } catch (Exception e) {
            log.error("Error while moving concept: " + idConceptToMove, e);
            return false;
        }
    }



    /**
     * Cette fonction permet de récupérer toutes les informations concernant un
     * Concept par son id et son thésaurus et la langue On récupère aussi les
     * IdArk si Ark est actif
     */
    public NodeConceptExport getConceptForExport(String idConcept, String idThesaurus, boolean isCandidatExport) {
        NodeConceptExport nodeConceptExport = new NodeConceptExport();

        String htmlTagsRegEx = "<[^>]*>";

        // les relations BT, NT, RT
        List<NodeHieraRelation> nodeListRelations = relationService.getAllRelationsOfConcept(idConcept, idThesaurus);
        nodeConceptExport.setNodeListOfBT(getRelations(nodeListRelations, nodeConceptExport.getRelationsBT()));
        nodeConceptExport.setNodeListOfNT(getRelations(nodeListRelations, nodeConceptExport.getRelationsNT()));
        nodeConceptExport.setNodeListIdsOfRT(getRelations(nodeListRelations, nodeConceptExport.getRelationsRT()));

        //récupération du Concept
        fr.cnrs.opentheso.models.concept.Concept concept = getThisConcept(idConcept, idThesaurus);
        if (concept == null) {
            return null;
        }
        nodeConceptExport.setConcept(concept);

        //récupération les aligenemnts
        nodeConceptExport.setNodeAlignmentsList(alignmentService.getAllAlignmentsOfConcept(idConcept, idThesaurus));

        //récupération des traductions
        nodeConceptExport.setNodeTermTraductions(termService.getTraductionByConcept(idThesaurus, idConcept));

        //récupération des Non Prefered Term
        nodeConceptExport.setNodeEM(nonPreferredTermService.getAllNonPreferredTerms(idConcept, idThesaurus));

        //récupération des Groupes ou domaines
        nodeConceptExport.setNodeListIdsOfConceptGroup(groupService.getListGroupOfConceptArk(idThesaurus, idConcept));

        var nodeNotes = noteService.getListNotesAllLang(idConcept, idThesaurus);
        if (isCandidatExport) {
            for (NodeNote note : nodeNotes) {
                String str = formatLinkTag(note.getLexicalValue());
                note.setLexicalValue(str.replaceAll(htmlTagsRegEx, ""));
            }
        }
        nodeConceptExport.setNodeNotes(nodeNotes);

        //récupération des coordonnées GPS
        List<Gps> nodeGps = gpsService.findByIdConceptAndIdThesoOrderByPosition(idConcept, idThesaurus);
        if (CollectionUtils.isNotEmpty(nodeGps)) {
            nodeConceptExport.setNodeGps(nodeGps.stream().map(element -> NodeGps.builder()
                            .position(element.getPosition() != null ? element.getPosition() : 0)
                            .longitude(element.getLongitude())
                            .latitude(element.getLatitude())
                            .build())
                    .toList());
        }

        List<NodeImage> nodeImages = imageService.getAllExternalImages(idConcept, idThesaurus);
        if (nodeImages != null) {
            nodeConceptExport.setNodeImages(nodeImages);
        }

        if (isCandidatExport) {
            var messages = candidatMessageRepository.findMessagesByConceptAndThesaurus(idConcept, idThesaurus);
            if (CollectionUtils.isNotEmpty(messages)) {
                nodeConceptExport.setMessages(messages.stream().map(element -> MessageDto.builder()
                                .msg(element.getValue())
                                .date(element.getDate())
                                .idUser(element.getIdUser())
                                .build())
                        .toList());
            }

            var votes = candidatVoteRepository.findAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);
            if (CollectionUtils.isNotEmpty(votes)) {
                nodeConceptExport.setVotes(votes.stream().map(element -> VoteDto.builder()
                                .idConcept(element.getIdConcept())
                                .idThesaurus(element.getIdThesaurus())
                                .idUser(element.getIdUser())
                                .idNote(element.getIdNote())
                                .typeVote(element.getTypeVote())
                                .build())
                        .toList());
            }
        }

        // pour les facettes
        var facetList = facetService.getFacetsByConceptAndThesaurus(idConcept, idThesaurus);
        nodeConceptExport.setListFacetsOfConcept(facetList.stream().map(ConceptFacet::getIdFacet).toList());

        /// pour les concepts dépréciés
        nodeConceptExport.setReplacedBy(getAllReplacedByWithArk(idThesaurus, idConcept));
        nodeConceptExport.setReplaces(getAllReplacesWithArk(idThesaurus, idConcept));

        return nodeConceptExport;
    }

    private List<NodeHieraRelation> getRelations(List<NodeHieraRelation> nodeHieraRelations, List<String> relations) {

        List<NodeHieraRelation> nodeHieraRelations1 = new ArrayList<>();
        for (NodeHieraRelation nodeHieraRelation : nodeHieraRelations) {
            if (relations.contains(nodeHieraRelation.getRole())) {
                nodeHieraRelations1.add(nodeHieraRelation);
            }
        }
        return nodeHieraRelations1;
    }

    public String formatLinkTag(String initialStr) {
        Pattern MY_PATTERN = Pattern.compile("<a(.*?)a>");
        Matcher m = MY_PATTERN.matcher(initialStr);
        while (m.find()) {
            String link = "<a" + m.group(1) + "a>";
            ArrayList<HTMLLinkElement> result = new HtmlLinkExtraction().extractHTMLLinks(link);
            if (CollectionUtils.isNotEmpty(result)) {
                initialStr = initialStr.replace(link, result.get(0).getLinkElement()
                        + " (" + result.get(0).getLinkAddress() + ")");
            }
        }
        return initialStr;
    }



    public List<List<String>> getPathOfConceptWithoutGroup(String idConcept, String idThesaurus, List<String> path,
                                                           List<List<String>> tabId) {

        List<String> firstPath = new ArrayList<>();

        var tabIdInvert = getInvertPathOfConceptWithoutGroup(idConcept, idThesaurus, firstPath, path, tabId);
        for (int i = 0; i < tabIdInvert.size(); i++) {
            List<String> pathTemp = new ArrayList<>();
            for (int j = tabIdInvert.get(i).size(); j > 0; j--) {
                pathTemp.add(tabIdInvert.get(i).get(j - 1));
            }
            tabIdInvert.remove(i);
            tabIdInvert.add(i, pathTemp);
        }
        return tabIdInvert;
    }

    /**
     * Focntion récursive pour trouver le chemin complet d'un concept en partant
     * du Concept lui même pour arriver à la tête TT on peut rencontrer
     * plusieurs têtes en remontant, alors on construit à chaque fois un chemin
     * complet.
     */
    private List<List<String>> getInvertPathOfConceptWithoutGroup(String idConcept, String idThesaurus, List<String> firstPath,
                                                                  List<String> path, List<List<String>> tabId) {

        var resultat = relationService.getListIdBT(idConcept, idThesaurus);
        if (resultat.size() > 1) {
            firstPath.addAll(path);
        }
        if (resultat.isEmpty()) {
            List<String> pathTemp = new ArrayList<>(firstPath);
            for (String path1 : path) {
                if (!pathTemp.contains(path1)) {
                    pathTemp.add(path1);
                }
            }
            tabId.add(pathTemp);
            path.clear();
        }

        for (String resultat1 : resultat) {
            path.add(resultat1);
            getInvertPathOfConceptWithoutGroup(resultat1, idThesaurus, firstPath, path, tabId);
        }

        return tabId;
    }

    /**
     * Cette fonction permet de récupérer toutes les informations concernant un
     * Concept par son id et son thésaurus et la langue ##MR ajout de limit NT
     * qui permet de définir la taille maxi des NT à récupérer, si = -1, pas de
     * limit offset 42 fetch next 21 rows only
     */
    public NodeConcept getConceptOldVersion(String idConcept, String idThesaurus, String idLang, int step, int offset) {
        NodeConcept nodeConcept = new NodeConcept();

        // récupération des BT
        var nodeListBT = relationService.getListBT(idConcept, idThesaurus, idLang);
        nodeConcept.setNodeBT(nodeListBT);

        //récupération du Concept
        fr.cnrs.opentheso.models.concept.Concept concept = getThisConcept(idConcept, idThesaurus);
        if (concept == null) {
            return null;
        }
        if ("dep".equalsIgnoreCase(concept.getStatus())) {
            concept.setDeprecated(true);
        }
        nodeConcept.setConcept(concept);

        //récupération du Terme
        Term term = termService.getThisTerm(idConcept, idThesaurus, idLang);
        nodeConcept.setTerm(term);

        //récupération des termes spécifiques
        nodeConcept.setNodeNT(relationService.getListNT(idConcept, idThesaurus, idLang, step, offset));

        //récupération des termes associés
        nodeConcept.setNodeRT(relationService.getListRT(idConcept, idThesaurus, idLang));

        //récupération des Non Prefered Term
        nodeConcept.setNodeEM(nonPreferredTermService.getNonPreferredTerms(idConcept, idThesaurus, idLang));

        //récupération des traductions
        nodeConcept.setNodeTermTraductions(termService.getTraductionsOfConcept(idConcept, idThesaurus, idLang));

        //récupération des notes du term
        nodeConcept.setNodeNotes(noteService.getListNotes(idConcept, idThesaurus, idLang));

        nodeConcept.setNodeConceptGroup(groupService.getListGroupOfConcept(idThesaurus, idConcept, idLang));

        nodeConcept.setNodeAlignments(alignmentService.getAllAlignmentOfConcept(idConcept, idThesaurus));

        nodeConcept.setNodeimages(imageService.getAllExternalImages(idConcept, idThesaurus));

        //gestion des ressources externes
        var externalResources = externalResourcesRepository.findAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (CollectionUtils.isNotEmpty(externalResources)) {
            nodeConcept.setNodeExternalResources(externalResources.stream()
                    .map(element -> NodeImage.builder()
                            .idConcept(element.getIdConcept())
                            .idThesaurus(element.getIdThesaurus())
                            .imageName(element.getDescription())
                            .uri(element.getExternalUri())
                            .build())
                    .toList());
        }

        // concepts qui remplacent un concept déprécié
        nodeConcept.setReplacedBy(getAllReplacedBy(idThesaurus, idConcept, idLang));
        // les concepts dépécés que ce concept remplace
        nodeConcept.setReplaces(getAllReplaces(idThesaurus, idConcept, idLang));

        /// récupération des Méta-données DC_terms
        var conceptDcTerms = conceptDcTermRepository.findAllByIdThesaurusAndIdConcept(idThesaurus, idConcept);
        if (CollectionUtils.isNotEmpty(conceptDcTerms)) {
            nodeConcept.setDcElements(conceptDcTerms.stream()
                    .map(element -> {
                        DcElement dcElement = new DcElement();
                        dcElement.setName(element.getName());
                        dcElement.setValue(element.getValue());
                        dcElement.setLanguage(element.getLanguage());
                        dcElement.setType(element.getDataType());
                        return dcElement;
                    })
                    .toList());
        }

        return nodeConcept;
    }

    public List<String> getListChildrenOfConceptByArk(String idArk) {
        String idThesaurus = thesaurusRepository.findIdThesaurusByArkId(idArk).orElse(null);
        String idConcept = getIdConceptFromArkId(idArk, idThesaurus);

        if (idThesaurus == null || idConcept == null) return Collections.emptyList();

        return conceptRepository.findArkIdsOfChildren(idThesaurus, idConcept);
    }

    public String getIdConceptFromArkId(String arkId, String idThesaurus) {
        return conceptRepository.findConceptIdByArkIgnoreCase(arkId, idThesaurus).orElse(null);
    }

    public String getIdConceptFromHandleId(String handleId) {
        return conceptRepository.findConceptIdByHandleIgnoreCase(handleId).orElse(null);
    }

    public fr.cnrs.opentheso.models.concept.Concept getThisConcept(String idConcept, String idThesaurus) {

        var optionalConcept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (optionalConcept.isEmpty()) {
            return null;
        }

        var concept = new fr.cnrs.opentheso.models.concept.Concept();

        concept.setIdConcept(idConcept);
        concept.setIdThesaurus(idThesaurus);
        concept.setIdArk(optionalConcept.get().getIdArk());
        concept.setIdHandle(optionalConcept.get().getIdHandle());
        concept.setIdDoi(optionalConcept.get().getIdDoi());
        concept.setCreated(optionalConcept.get().getCreated());
        concept.setModified(optionalConcept.get().getModified());
        concept.setStatus(optionalConcept.get().getStatus());
        concept.setNotation(optionalConcept.get().getNotation());
        concept.setTopConcept(optionalConcept.get().getTopConcept());
        concept.setCreator(optionalConcept.get().getCreator());
        concept.setContributor(optionalConcept.get().getContributor());
        concept.setIdGroup("");
        concept.setConceptType(optionalConcept.get().getConceptType().toLowerCase());

        var contributor = userRepository.findById(concept.getContributor());
        concept.setContributorName(contributor.isPresent() ? contributor.get().getUsername() : "");

        var creator = userRepository.findById(concept.getCreator());
        concept.setCreatorName(creator.isPresent() ? creator.get().getUsername() : "");

        return concept;
    }

    public List<String> getAllIdConceptOfThesaurusWithoutArk(String idThesaurus) {
        return conceptRepository.findAllIdConceptsWithoutArk(idThesaurus);
    }

    public List<NodeUri> getAllTopConcepts(String idThesaurus) {
        return conceptRepository.findAllTopConceptsWithUris(idThesaurus);
    }

    public List<String> getIdConceptFromDate(String idThesaurus, String dateStr) {
        try {
            LocalDate localDate = LocalDate.parse(dateStr);
            Date startDate = Date.from(
                    localDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            );
            return conceptRepository.findConceptsModifiedSince(idThesaurus, startDate);
        } catch (DateTimeParseException e) {
            log.error("Format de date invalide : {}", dateStr, e);
            return Collections.emptyList();
        }
    }

    @Transactional
    public void insertConcept(fr.cnrs.opentheso.models.concept.Concept concept) {

        if (conceptRepository.existsByIdConceptAndIdThesaurus(concept.getIdConcept(), concept.getIdThesaurus())) {
            return;
        }

        if (concept.getCreatorName() != null) {
            userRepository.findAllByUsername(concept.getCreatorName()).ifPresent(user -> concept.setCreator(user.getId()));
        }

        if (concept.getContributorName() != null) {
            userRepository.findAllByUsername(concept.getContributorName()).ifPresent(user -> concept.setContributor(user.getId()));
        }
        conceptRepository.save(Concept.builder()
                .idConcept(concept.getIdConcept())
                .idThesaurus(concept.getIdThesaurus())
                .topConcept(concept.isTopConcept())
                .gps(false)
                .conceptType("concept")
                .status(concept.getStatus() == null ? "D" : concept.getStatus())
                .idArk(concept.getIdArk() == null ? "" : concept.getIdArk())
                .idHandle(concept.getIdHandle() == null ? "" : concept.getIdHandle())
                .idDoi(concept.getIdDoi() == null ? "" : concept.getIdDoi())
                .notation(concept.getNotation() == null ? "" : concept.getNotation())
                .creator(concept.getCreator())
                .created(new Date())
                .modified(new Date())
                .contributor(concept.getContributor())
                .build());
    }

    public List<NodeTree> getListChildrenOfConceptWithTerm(String idConcept, String idLang, String idThesaurus) {

        return hierarchicalRelationshipRepository.findChildrenWithPreferredTerm(idConcept, idLang, idThesaurus).stream()
                .map(view -> {
                    NodeTree node = new NodeTree();
                    node.setIdConcept(view.getIdConcept());
                    node.setPreferredTerm(view.getPreferredTerm());
                    return node;
                })
                .collect(Collectors.toList());
    }

    public List<String> getAllConceptIdsByMultiGroup(String idThesaurus, String[] idGroups) {

        if (idGroups == null || idGroups.length == 0) {
            return List.of(); // ou lever une exception
        }

        var groupIds = Arrays.stream(idGroups).map(String::toLowerCase).toList();
        return conceptRepository.findAllByThesaurusAndGroups(idThesaurus, groupIds).stream()
                .map(ConceptIdView::getIdConcept)
                .toList();
    }

    /**
     * Cette fonction permet de récupérer l'identifiant du Concept d'après l'id
     * du concept !!!! ATTENTION !!!! l'id du concept peut se trouver dans
     * plusieurs thésaurus différents donc on ne retourne que le premier.
     */
    public String getIdThesaurusFromIdConcept(String idConcept) {

        var concepts = conceptRepository.findByIdConcept(idConcept);
        if (CollectionUtils.isEmpty(concepts)) {
            return null;
        }

        return concepts.get(0).getIdThesaurus();
    }

    /**
     * Cette fonction permet de récupérer les identifiants d'un concept idArk,
     * idHandle, idConcept sous forme de nodeUri
     */
    public NodeUri getNodeUriOfConcept(String idConcept, String idThesaurus) {

        NodeUri nodeUri = new NodeUri();
        nodeUri.setIdConcept(idConcept);

        var concept = getConcept(idConcept, idThesaurus);
        if (concept == null) {
            return nodeUri;
        }
        nodeUri.setIdArk(concept.getIdArk());
        nodeUri.setIdHandle(concept.getIdHandle());
        return nodeUri;
    }

    public List<NodeUri> getListConceptsOfGroup(String idThesaurus, String idGroup) {
        return conceptRepository.findConceptsByThesaurusAndGroup(idThesaurus, idGroup);
    }

    public List<String> getAllIdConceptOfThesaurusByGroup(String idThesaurus, String idGroup) {
        return conceptRepository.findAllConceptIdsByGroup(idThesaurus, idGroup);
    }

    public List<String> getAllIdConceptsWithoutHandle(String idThesaurus) {
        return conceptRepository.findAllIdsWithoutHandle(idThesaurus);
    }

    public List<String> getAllTopConceptIds(String idThesaurus) {
        return conceptRepository.findAllTopConceptIdsByThesaurus(idThesaurus);
    }

    public String getIdThesaurusFromArkId(String arkId) {
        return conceptRepository.findIdThesaurusListByArkId(arkId);
    }

    public String getIdThesaurusFromHandleId(String handleId) {

        log.debug("Recherche du thésaurus id avec l'id handle {}", handleId);
        var concept = conceptRepository.findByIdHandle(handleId);
        if (concept.isEmpty()){
            log.error("Aucun concept trouvé avec l'id Handle {}", handleId);
            return null;
        }
        return concept.get().getIdThesaurus();
    }

    public void setStatus(String newStatus, String idConcept, String idThesaurus) {

        conceptRepository.setStatus(newStatus, idConcept, idThesaurus);
    }

    public void addHistory(fr.cnrs.opentheso.models.concept.Concept concept, int idUser) {
        conceptAddService.addConceptHistorique(concept, idUser);
    }

    public List<NodeIdValue> getAllReplacedBy(String idThesaurus, String idConcept, String idLang) {

        log.debug("Recherche des concepts qui remplacent le concept déprécié {}", idConcept);
        var concepts = conceptReplacedByRepository.findAllByIdConcept1AndIdThesaurus(idConcept, idThesaurus);
        if (concepts.isEmpty()) {
            log.debug("Aucun concept replaceBy avec l'id {}", idConcept);
            return List.of();
        }

        return concepts.stream().map(element -> {
            var label = termService.getLexicalValueOfConcept(element.getIdConcept2(), idThesaurus, idLang);
            return NodeIdValue.builder()
                    .id(element.getIdConcept2())
                    .value(StringUtils.isEmpty(label) ? "" : label)
                    .build();
        }).toList();
    }

    public List<NodeIdValue> getAllReplaces(String idThesaurus, String idConcept, String idLang) {

        log.debug("Recherche des concepts dépréciés et remplacée par le concept {}", idConcept);
        var concepts = conceptReplacedByRepository.findAllByIdConcept2AndIdThesaurus(idConcept, idThesaurus);
        if (concepts.isEmpty()) {
            log.debug("Aucun concept Replaces avec l'id {}", idConcept);
            return List.of();
        }

        return concepts.stream().map(element -> {
            var label = termService.getLexicalValueOfConcept(element.getIdConcept1(), idThesaurus, idLang);
            return NodeIdValue.builder()
                    .id(element.getIdConcept1())
                    .value(StringUtils.isEmpty(label) ? "" : label)
                    .build();
        }).toList();
    }

    public List<NodeHieraRelation> getAllReplacedByWithArk(String idTheso, String idConcept) {

        var projections = conceptReplacedByRepository.findReplacedByWithUri(idTheso, idConcept);

        return projections.stream().map(proj -> {
            NodeUri uri = new NodeUri();
            uri.setIdConcept(proj.getIdConcept());
            uri.setIdArk(Optional.ofNullable(proj.getIdArk()).orElse(""));
            uri.setIdHandle(Optional.ofNullable(proj.getIdHandle()).orElse(""));
            uri.setIdDoi(Optional.ofNullable(proj.getIdDoi()).orElse(""));

            return NodeHieraRelation.builder()
                    .role("replacedBy")
                    .uri(uri)
                    .build();
        }).toList();
    }

    @Transactional
    public boolean deprecateConcept(String idConcept, String idThesaurus, int idUser) {
        try {
            log.debug("Dépréciation du concept {}", idConcept);
            setStatus("DEP", idConcept, idThesaurus);
            fr.cnrs.opentheso.models.concept.Concept concept = getThisConcept(idConcept, idThesaurus);
            addHistory(concept, idUser);
        } catch (Exception exception) {
            log.error("Error during désactivation of Concept : " + idConcept);
            return false;
        }
        return true;
    }

    @Transactional
    public boolean approveConcept(String idConcept, String idThesaurus, int idUser) {
        try {
            log.debug("Activation du concept id {}", idConcept);
            setStatus("D", idConcept, idThesaurus);
            var concept = getThisConcept(idConcept, idThesaurus);
            addHistory(concept, idUser);
            log.debug("Suppression du concept déprécié {}", idConcept);
            conceptReplacedByRepository.deleteAllByIdConcept1AndIdThesaurus(idConcept, idThesaurus);
            return true;
        } catch (Exception sqle) {
            log.error("Error during activation of Concept : " + idConcept, sqle);
            return false;
        }
    }

    public void deleteReplacedBy(String idConcept, String idThesaurus, String idConceptReplaceBy) {

        log.debug("Suppression du concept {} qui remplace le concept déprécié {}", idConceptReplaceBy, idConcept);
        conceptReplacedByRepository.deleteAllByIdConcept1AndIdConcept2AndIdThesaurus(idConcept, idConceptReplaceBy, idThesaurus);
    }

    public void addReplacedBy(String idConcept, String idThesaurus, String idConceptReplaceBy, int idUser) {

        log.debug("Remplacement du concept déprécie par le concept {}", idConcept);
        conceptReplacedByRepository.save(ConceptReplacedBy.builder()
                .idConcept1(idConcept)
                .idConcept2(idConceptReplaceBy)
                .idThesaurus(idThesaurus)
                .idUser(idUser)
                .modified(new Date())
                .build());
    }

    public List<NodeHieraRelation> getAllReplacesWithArk(String idThesaurus, String idConcept) {

        var projections = conceptReplacedByRepository.findReplacesWithUri(idThesaurus, idConcept);

        return projections.stream().map(proj -> {
            NodeUri uri = new NodeUri();
            uri.setIdConcept(proj.getIdConcept());
            uri.setIdArk(Optional.ofNullable(proj.getIdArk()).orElse(""));
            uri.setIdHandle(Optional.ofNullable(proj.getIdHandle()).orElse(""));
            uri.setIdDoi(Optional.ofNullable(proj.getIdDoi()).orElse(""));

            NodeHieraRelation relation = new NodeHieraRelation();
            relation.setRole("replace");
            relation.setUri(uri);
            return relation;
        }).toList();
    }

    public Map<String, String> getIdConceptsFromArkIds(Set<String> arkIds, String idTheso) {
        if (arkIds == null || arkIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> list = conceptRepository.findArkIds(arkIds, idTheso);

        Map<String, String> result = new HashMap<>();
        for (Object[] row : list) {
            String arkId = (String) row[0];
            String idConcept = (String) row[1];
            result.put(arkId, idConcept);
        }

        return result;
    }

    public Map<String, String> getArkIdsFromIdConcepts(Set<String> idConcepts, String idTheso) {
        if (idConcepts == null || idConcepts.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> list = conceptRepository.findArkFromIdConcepts(idConcepts, idTheso);
        Map<String, String> result = new HashMap<>();
        for (Object[] row : list) {
            String idConcept = (String) row[0];
            String idArk = (String) row[1];
            result.put(idConcept, idArk);
        }
        return result;
    }
}
