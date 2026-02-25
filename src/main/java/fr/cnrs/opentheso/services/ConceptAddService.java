package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.bean.leftbody.TreeNodeData;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.entites.*;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.concept.NodeMetaData;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.*;
import fr.cnrs.opentheso.services.security.CryptoService;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.ws.ark.ArkHelper2;

import jakarta.faces.application.FacesMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.beans.Transient;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptAddService {

    private final TermService termService;
    private final GroupService groupService;
    private final PreferenceService preferenceService;

    private final Tree tree;
    private final ArkService arkService;
    private final ConceptView conceptBean;
    private final SelectedTheso selectedTheso;
    private final RoleOnThesaurusBean roleOnThesaurusBean;
    private final ConceptRepository conceptRepository;
    private final RelationService relationService;
    private final UserRepository userRepository;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final ConceptHistoriqueRepository conceptHistoriqueRepository;
    private final HandleConceptService handleConceptService;
    private final FacetService facetService;
    private final ThesaurusDcTermRepository thesaurusDcTermRepository;
    private final CurrentUser currentUser;
    private final CryptoService cryptoService;


    public boolean addNewConcept(String idThesaurus, String idNewConcept, String idGroup, String idLang, String prefLabel,
                                 String status, String source, String idFacet, String idConceptParent, String notation,
                                 String idBTfacet, String relationType, int idUser, boolean isConceptUnderFacet,
                                 CurrentUser currentUser) {

        log.debug("Début de l'ajout du nouveau concept");
        if (roleOnThesaurusBean.getNodePreference() == null) {
            MessageUtils.showMessage(FacesMessage.SEVERITY_ERROR, "Erreur !", "Le thésaurus n'a pas de préférences !");
            return false;
        }

        if (StringUtils.isNotEmpty(idNewConcept)) {
            if (isIdExiste(idNewConcept, idThesaurus)) {
                MessageUtils.showMessage(FacesMessage.SEVERITY_ERROR, "Attention !", "Identifiant déjà attribué, veuillez choisir un autre ou laisser vide !!");
                return false;
            }
        } else {
            idNewConcept = null;
        }

        // si le group est à null ou vide, on créé le concept sans l'ajouter à aucun groupe
        // c'est dans ConceptHelper que ca se passe.
        var concept = Concept.builder()
                .idGroup(idGroup)
                .idThesaurus(idThesaurus)
                .idConcept(idNewConcept)
                .topConcept(false)
                .status(status)
                .notation(notation)
                .build();

        var terme = Term.builder()
                .idThesaurus(idThesaurus)
                .lang(idLang)
                .status(status)
                .lexicalValue(prefLabel.trim())
                .source(StringUtils.isEmpty(source) ? "" : source)
                .build();


        log.debug("Vérification : le nouveau concept est sous une Facette (le BT est celui du parent de la Facette)");
        if (isConceptUnderFacet) {
            log.debug("Le concept est sous une Facette");
            idConceptParent = idBTfacet;
        }

        log.debug("Enregistrement du concept dans la base");
        idNewConcept = addConcept(idConceptParent, relationType, concept, terme, idUser);

        if (idNewConcept == null) {
            MessageUtils.showErrorMessage("Erreur pendant l'enregistrement du nouveau concept !");
            return false;
        }

        log.debug("Enregistrement du trace de l'action");
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CREATOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(idNewConcept)
                .idThesaurus(idThesaurus)
                .build());
        // ajout de l'info de la dernière modification du concept dans les DC du thésaurus
        createAndSaveDcTerm(idThesaurus, DCMIResource.MODIFIED,
                new SimpleDateFormat("yyyy-MM-dd").format(new Date()), "", "date");

        if (isConceptUnderFacet) {
            facetService.addConceptToFacet(idFacet, idThesaurus, idNewConcept);

            var data = new TreeNodeData(idNewConcept, prefLabel, "", false, false, true, false, "term");
            data.setIdFacetParent(idFacet);

            tree.getDataService().addNodeWithoutChild("file", data, tree.getSelectedNode());
            tree.initialise(selectedTheso.getCurrentIdTheso(), selectedTheso.getSelectedLang());
            tree.expandTreeToPath2(idBTfacet, selectedTheso.getCurrentIdTheso(), selectedTheso.getSelectedLang(), idFacet);

            PrimeFaces.current().ajax().update("containerIndex");
            MessageUtils.showMessage(FacesMessage.SEVERITY_INFO, "", "Concept ajouté avec succès !");
            return true;
        }

        if (tree.getSelectedNode() != null) {
            // si le concept en cours n'est pas celui sélectionné dans l'arbre, on se positionne sur le concept en cours dans l'arbre
            if (!((TreeNodeData) tree.getSelectedNode().getData()).getNodeId().equalsIgnoreCase(idConceptParent)) {
                tree.expandTreeToPath(idConceptParent, idThesaurus, idLang);
            }

            // cas où l'arbre est déjà déplié ou c'est un concept sans fils
            /// attention, cette condition permet d'éviter une erreur dans l'arbre si :
            // un concept est sélectionné dans l'arbre mais non déployé, puis, on ajoute un TS, alors ca produit une erreur
            if (tree.getSelectedNode().getChildCount() == 0) {
                tree.getSelectedNode().setType("concept");
            }
            if (tree.getSelectedNode().isExpanded() || tree.getSelectedNode().getChildCount() == 0) {
                PrimeFaces.current().executeScript("srollToSelected()");
                tree.addNewChild(tree.getSelectedNode(), idNewConcept, idThesaurus, idLang, notation);
                tree.getSelectedNode().setExpanded(true);
            }
        }

        conceptBean.getConcept(idThesaurus, idConceptParent, idLang, currentUser);

        MessageUtils.showInformationMessage("Le concept a bien été ajouté");

        log.debug("Fin de l'ajout du nouveau concept");
        return true;
    }

    @Transient
    public String addConcept(String idParent, String relationType, Concept concept, Term term, int idUser) {

        if (idParent == null) {
            concept.setTopConcept(true);
        }

        var idConcept = addConceptInTable(concept, idUser);
        if (idConcept == null) {
            return null;
        }

        if (StringUtils.isNotEmpty(concept.getIdGroup())) {
            groupService.addConceptGroupConcept(concept.getIdGroup(), concept.getIdConcept(), concept.getIdThesaurus());
        }

        var idTerm = termService.addTerm(term, idConcept, idUser);
        if (idTerm == null) {
            return null;
        }
        term.setIdTerm(idTerm);
        /**
         * ajouter le lien hiérarchique avec le concept partent sauf si ce
         * n'est pas un TopConcept
         */
        if (!concept.isTopConcept()) {
            String inverseRelation = "BT";
            if (relationType == null) {
                relationType = "NT";
            }
            inverseRelation = switch (relationType) {
                case "NT" -> "BT";
                case "NTG" -> "BTG";
                case "NTP" -> "BTP";
                case "NTI" -> "BTI";
                default -> inverseRelation;
            };

            relationService.addLinkHierarchicalRelation(HierarchicalRelationship.builder()
                    .idConcept1(idParent)
                    .idConcept2(idConcept)
                    .idThesaurus(concept.getIdThesaurus())
                    .role(relationType)
                    .build(), idUser);

            relationService.addLinkHierarchicalRelation(HierarchicalRelationship.builder()
                    .idConcept1(idConcept)
                    .idConcept2(idParent)
                    .idThesaurus(concept.getIdThesaurus())
                    .role(inverseRelation)
                    .build(), idUser);
        }

        var preferences = preferenceService.getThesaurusPreferences(concept.getIdThesaurus());
        if (preferences != null) {
            // création de l'identifiant Handle
            if (preferences.isUseHandle()) {
                if (!handleConceptService.addIdHandle(idConcept, concept.getIdThesaurus())) {
                    MessageUtils.showErrorMessage("La création de Handle a échoué");
                }
            }
            if (preferences.isUseOpenArk()) {
                ArrayList<String> idConcepts = new ArrayList<>();
                idConcepts.add(idConcept);
                String apiKeyOpenArk = cryptoService.decrypt(preferences.getApiKeyOpenArk());
                if(generateOpenArkId(concept.getIdThesaurus(), idConcepts,
                        selectedTheso.getCurrentLang(), preferences, apiKeyOpenArk) == null){
                    MessageUtils.showErrorMessage("La création du Ark a échoué");
                }
            }

            // Si on arrive ici, c'est que tout va bien
            // alors c'est le moment de récupérer le code ARK
            if (preferences.isUseArk()) {
                var result = generateArkId(concept.getIdThesaurus(), List.of(idConcept), term.getLang(), preferences);
                if (CollectionUtils.isEmpty(result)) {
                    MessageUtils.showErrorMessage("La création du Ark local a échoué");
                    log.error("La création du Ark local a échoué");
                }
            }
            if (preferences.isUseArkLocal()) {
                ArrayList<String> idConcepts = new ArrayList<>();
                idConcepts.add(idConcept);
                if (!arkService.generateArkIdLocal(concept.getIdThesaurus(), idConcepts)) {
                    MessageUtils.showErrorMessage("La création du Ark a échouée");
                    log.error("La création du Ark local a échouée");
                }
            }
        }
        return idConcept;
    }

    public String addConceptInTable(Concept concept, int idUser) {

        log.debug("Ajouter un nouveau concept dans le thésaurus id {}", concept.getIdThesaurus());
        int idSequenceConcept = -1;
        concept.setNotation(concept.getNotation() == null ? ""  : concept.getNotation());
        concept.setIdArk(concept.getIdArk() == null ? ""  : concept.getIdArk());

        var preference = preferenceService.getThesaurusPreferences(concept.getIdThesaurus());
        if (concept.getIdConcept() == null) {
            if (preference != null && preference.getIdentifierType() != null && preference.getIdentifierType() == 1) {
                concept.setIdConcept(getAlphaNumericId());
            } else {
                concept.setIdConcept(getNumericConceptId());
                if (StringUtils.isNotEmpty(concept.getIdConcept())) {
                    idSequenceConcept = Integer.parseInt(concept.getIdConcept());
                }
            }
        }

        if (StringUtils.isEmpty(concept.getIdConcept())) {
            return null;
        }

        conceptRepository.save(fr.cnrs.opentheso.entites.Concept.builder()
                .idConcept(concept.getIdConcept())
                .idThesaurus(concept.getIdThesaurus())
                .idArk(concept.getIdArk())
                .created(new Date())
                .modified(new Date())
                .status(concept.getStatus())
                .notation(concept.getNotation())
                .topConcept(concept.isTopConcept())
                .creator(idUser)
                .conceptType("concept")
                .contributor(idUser)
                .gps(false)
                .idDoi("")
                .idHandle("")
                .build());

        addConceptHistorique(concept, idUser);

        return concept.getIdConcept();
    }

    private void createAndSaveDcTerm(String idThesaurus, String name, String value, String language, String type) {
        DcElement dcElement = new DcElement(name, value, language, type);

        boolean exists = thesaurusDcTermRepository.existsByIdThesaurusAndNameAndValue(idThesaurus, name, value);
        if (exists) {
            log.debug("DC Term déjà existant, insertion ignorée : {} {} {}", idThesaurus, name, value);
            return;
        }

        ThesaurusDcTerm tmp = thesaurusDcTermRepository.save(
                ThesaurusDcTerm.builder()
                        .idThesaurus(idThesaurus)
                        .name(dcElement.getName())
                        .value(dcElement.getValue())
                        .language(dcElement.getLanguage())
                        .dataType(dcElement.getType())
                        .build()
        );
        dcElement.setId(tmp.getId().intValue());
    }


    public void addConceptHistorique(Concept concept, int idUser) {

        log.debug("Ajout d'une trace dans la table historique des concepts (concept id {})", concept.getIdThesaurus());
        conceptHistoriqueRepository.save(ConceptHistorique.builder()
                .idConcept(concept.getIdConcept())
                .idThesaurus(concept.getIdThesaurus())
                .idArk(concept.getIdArk())
                .status(concept.getStatus())
                .notation(concept.getNotation())
                .topConcept(concept.isTopConcept())
                .idGroup(concept.getIdGroup() == null ? "" : concept.getIdGroup())
                .idUser(idUser)
                .modified(new Date())
                .build());
    }


    // nouvelle version pour OpenArk
    public List<NodeIdValue> generateOpenArkId(String idThesaurus, List<String> idConcepts, String idLang, Preferences preferences, String apiKey) {
        if (preferences.isUseOpenArk()) {
            if(!arkService.generateArkWithOpenArk(idThesaurus, idConcepts, idLang,
                    currentUser.getNodeUser().getName(), apiKey, preferenceService.getThesaurusPreferences(idThesaurus))){
                return null;
            }
            return new ArrayList<>();
        }
        return null;
    }

    public List<NodeIdValue> deleteArkWithOpenArk(String idThesaurus, List<String> idConcepts, String idLang, Preferences preferences, String apiKey) {
        if (preferences.isUseOpenArk()) {
            if(!arkService.deleteArkWithOpenArk(idThesaurus, idConcepts, apiKey, preferenceService.getThesaurusPreferences(idThesaurus))){
                return null;
            }
            return new ArrayList<>();
        }
        return null;
    }

    public List<NodeIdValue> generateArkId(String idThesaurus, List<String> idConcepts, String idLang, Preferences preferences) {

        log.debug("Génération d'identifiant Ark pour une liste des concepts");

        List<NodeIdValue> nodeIdValues = new ArrayList<>();
        if (preferences == null) {
            preferences = preferenceService.getThesaurusPreferences(idThesaurus);
        }

        if (preferences == null) {
            MessageUtils.showErrorMessage("Erreur: Veuillez paramétrer les préférences pour ce thésaurus !!");
            log.error("Veuillez paramétrer les préférences pour ce thésaurus !!");
            return nodeIdValues;
        }

        if (!preferences.isUseArk() && !preferences.isUseArkLocal()) {
            MessageUtils.showErrorMessage("Veuillez activer Ark dans les préférences !!");
            log.error("Il faut activer Ark pour le thésaurus id {}", idThesaurus);
            return nodeIdValues;
        }

        if (preferences.isUseArkLocal()) {
            arkService.generateArkIdLocal(idThesaurus, idConcepts);
            return null;
        }

        var arkHelper2 = new ArkHelper2(preferences);
        if (!arkHelper2.login()) {
            log.error("Erreur pendant la connexion avec le serveur Ark");
            return nodeIdValues;
        }

        for (String idConcept : idConcepts) {

            var concept = conceptRepository.findByIdConceptAndIdThesaurus(idConcept, idThesaurus);
            if (concept.isEmpty()) {
                log.error("Aucun concept n'est trouvé avec l'id {} dans le thésaurus id {}", idConcept, idThesaurus);

                NodeIdValue nodeIdValue = new NodeIdValue();
                nodeIdValue.setId(idConcept);
                nodeIdValue.setValue("Erreur: ce concept n'existe pas");
                nodeIdValues.add(nodeIdValue);
                continue;
            }

            var contributor = userRepository.findById(concept.get().getContributor());
            var creatorName = contributor.isPresent() ? contributor.get().getUsername() : "";

            var nodeMetaData = new NodeMetaData();
            nodeMetaData.setDcElementsList(new ArrayList<>());
            nodeMetaData.setTitle(termService.getLexicalValueOfConcept(idConcept, idThesaurus, idLang));
            nodeMetaData.setSource(preferences.getPreferredName());
            nodeMetaData.setCreator(creatorName);

            var privateUri = "?idc=" + idConcept + "&idt=" + idThesaurus;

            /// test de tous les cas de figure pour la création d'un idArk
            if (StringUtils.isEmpty(concept.get().getIdArk())) {
                // cas où on a déja un identifiant Ark en local, donc on doit vérifier :
                // - si l'idArk est présent sur le serveur, on applique une mise à jour de l'URL
                // - si l'idArk n'est pas présent sur le serveur, il y a 2 cas :
                //      - on vérifie si l'URL liée au Ark fourni existe sur le serveur, alors on retourne une erreur (il y a confusion)
                //      - si l'URL n'existe pas sur le serveur, alors on procède à une création d'un identifiant Ark
                //
                if (!arkHelper2.addArk(privateUri, nodeMetaData)) {
                    log.error("La création Ark a échoué ici : " + idConcept);
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId(idConcept);
                    nodeIdValue.setValue("Erreur: La création Ark a échoué: " + arkHelper2.getMessage());
                    nodeIdValues.add(nodeIdValue);
                    continue;
                }
                if (!arkService.updateArkIdOfConcept(idConcept, idThesaurus, arkHelper2.getIdArk())) {
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId(idConcept);
                    nodeIdValue.setValue("Erreur: La mise à jour du concept dans Opentheso a échoué");
                    nodeIdValues.add(nodeIdValue);
                    continue;
                }
                if (preferences.isGenerateHandle()) {
                    if (!handleConceptService.updateHandleIdOfConcept(idConcept, idThesaurus, arkHelper2.getIdHandle())) {
                        NodeIdValue nodeIdValue = new NodeIdValue();
                        nodeIdValue.setId(idConcept);
                        nodeIdValue.setValue("Erreur: La mise à jour Handle du concept dans Opentheso a échoué");
                        nodeIdValues.add(nodeIdValue);
                    }
                }
            } else {
                // ark existe dans Opentheso, on vérifie si Ark est présent sur le serveur Ark
                if (arkHelper2.isArkExistOnServer(concept.get().getIdArk())) {
                    // ark existe sur le serveur, alors on applique une mise à jour
                    // pour l'URL et les métadonnées

                    if (!arkHelper2.updateArk(concept.get().getIdArk(), privateUri, nodeMetaData)) {
                        log.error("Erreur de mise à jour de l'Ark avec l'erreur : " + arkHelper2.getMessage() + "  idConcept = " + idConcept);
                        NodeIdValue nodeIdValue = new NodeIdValue();
                        nodeIdValue.setId(idConcept);
                        nodeIdValue.setValue("Erreur: Ark existe sur le serveur OpenArk, mais la mise à jour a échoué : " + arkHelper2.getMessage());
                        nodeIdValues.add(nodeIdValue);
                        continue;
                    }
                    if (preferences.isGenerateHandle()) {
                        if (!handleConceptService.updateHandleIdOfConcept(idConcept, idThesaurus, arkHelper2.getIdHandle())) {
                            NodeIdValue nodeIdValue = new NodeIdValue();
                            nodeIdValue.setId(idConcept);
                            nodeIdValue.setValue("Erreur: La mise à jour Handle du concept dans Opentheso a échoué");
                            nodeIdValues.add(nodeIdValue);
                        }
                    }
                } else {
                    // création d'un identifiant Ark avec en paramètre l'ID Ark existant sur Opentheso
                    // + (création de l'ID Handle avec le serveur Ark de la MOM)
                    if (!arkHelper2.addArkWithProvidedId(concept.get().getIdArk(), privateUri, nodeMetaData)) {
                        log.error("Erreur de création d'un nouveau Ark avec l'erreur : " + arkHelper2.getMessage() + "  idConcept = " + idConcept);
                        NodeIdValue nodeIdValue = new NodeIdValue();
                        nodeIdValue.setId(idConcept);
                        nodeIdValue.setValue("Erreur: Ark n'existe pas sur le serveur OpenArk, mais la mise à jour a échoué : " + arkHelper2.getMessage());
                        nodeIdValues.add(nodeIdValue);
                        continue;
                    }
                    if (arkService.updateArkIdOfConcept(idConcept, idThesaurus, arkHelper2.getIdArk())) {
                        NodeIdValue nodeIdValue = new NodeIdValue();
                        nodeIdValue.setId(idConcept);
                        nodeIdValue.setValue("Erreur: La mise à jour du concept dans Opentheso a échoué");
                        nodeIdValues.add(nodeIdValue);
                        continue;
                    }
                    if (preferences.isGenerateHandle()) {
                        if (!handleConceptService.updateHandleIdOfConcept(idConcept, idThesaurus, arkHelper2.getIdHandle())) {
                            NodeIdValue nodeIdValue = new NodeIdValue();
                            nodeIdValue.setId(idConcept);
                            nodeIdValue.setValue("Erreur: La mise à jour Handle du concept dans Opentheso a échoué");
                            nodeIdValues.add(nodeIdValue);
                        }
                    }
                }
            }
            NodeIdValue nodeIdValue = new NodeIdValue();
            nodeIdValue.setId(idConcept);
            nodeIdValue.setValue("OK");
            nodeIdValues.add(nodeIdValue);
        }

        return nodeIdValues;
    }

    private String getAlphaNumericId() {
        String id = ToolsHelper.getNewId(15, false, false);
        while (isIdExiste(id)) {
            id = ToolsHelper.getNewId(15, false, false);
        }
        return id;
    }

    private String getNumericConceptId() {
        var idNumerique = conceptRepository.getNextConceptNumericId();
        if (idNumerique == null) {
            throw new IllegalStateException("Impossible de récupérer un ID depuis la séquence concept__id_seq");
        }

        String idConcept = String.valueOf(idNumerique);
        while (!conceptRepository.findByIdConcept(idConcept).isEmpty()) {
            idConcept = String.valueOf(++idNumerique);
        }

        return idConcept;
    }

    public boolean isIdExiste(String idNewConcept, String idThesaurus) {

        log.debug("Vérifier l'existence de l'id concept {} dans le thésaurus id {}", idNewConcept, idThesaurus);
        var concept = conceptRepository.findByIdConceptAndIdThesaurus(idNewConcept, idThesaurus);
        log.debug("Le concept id {} existe ? {}", idNewConcept, concept.isPresent());
        return concept.isPresent();
    }

    public boolean isIdExiste(String idConcept) {

        log.debug("Vérifier l'existence de l'id concept {}", idConcept);
        var concepts = conceptRepository.findByIdConcept(idConcept);
        log.debug("Le concept id {} existe ? {}", idConcept, CollectionUtils.isNotEmpty(concepts));
        return CollectionUtils.isNotEmpty(concepts);
    }

}
