package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.bean.proposition.SynonymPropBean;
import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.terms.NodeEM;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.proposition.PropositionBean;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.NonPreferredTermService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.utils.MessageUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.util.CollectionUtils;

/**
 *
 * @author miledrousset
 */
@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "synonymBean")
public class SynonymBean implements Serializable {

    private final CurrentUser currentUser;
    private final ConceptView conceptBean;
    private final SelectedTheso selectedTheso;
    private final PropositionBean propositionBean;
    private final NonPreferredTermService nonPreferredTermService;
    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;
    private final TermService termService;
    private final ConceptDcTermRepository conceptDcTermRepository;

    private NodeEM nodeEM;
    private List<NodeLangTheso> nodeLangues;
    private List<NodeEM> nodeEMs, nodeEMsForEdit;
    private String selectedLang, selectedValue, value;
    private boolean hidden, duplicate;


    public void reset() {
        hidden = false;
        selectedLang = conceptBean.getSelectedLang();
        nodeLangues = thesaurusService.getAllUsedLanguagesOfThesaurusNode(selectedTheso.getCurrentIdTheso(), selectedLang);

        nodeEMs = conceptBean.getNodeConcept().getNodeEM();
        
        prepareNodeEMForEdit();
        
        value = "";
        duplicate = false;
        this.nodeEM = null;
    }

    private void init() {
        value = "";
        duplicate = false;
        this.nodeEM = null;
    }
   
        public void prepareNodeEMForEdit() {

        log.debug("Charger la liste des synonymes disponibles pour le concept {}", conceptBean.getNodeConcept().getConcept().getIdConcept());
        nodeEMsForEdit = new ArrayList<>();
        if (!CollectionUtils.isEmpty(conceptBean.getNodeConcept().getNodeEM())) {
            for (var nodeEM1 : conceptBean.getNodeConcept().getNodeEM()) {
                nodeEM1.setOldValue(nodeEM1.getLexicalValue());
                nodeEM1.setOldHiden(nodeEM1.isHiden());
                nodeEMsForEdit.add(nodeEM1);
            }
        }
    }

    public void infos() {
        MessageUtils.showInformationMessage("Rédiger une aide ici pour Add Concept !");
    }

    /**
     * permet d'ajouter un synonyme sans controler le nom en doublon
     */
    public void addForced(int idUser) {

        log.debug("Recherche de preferred term");
        var preferredTerm = termService.getPreferenceTermByThesaurusAndConcept(selectedTheso.getCurrentIdTheso(),
                conceptBean.getNodeConcept().getConcept().getIdConcept());

        log.debug("Id du term {}", preferredTerm.getIdTerm());

        log.debug("Enregistrement du synonyme dans la base de données");
        nonPreferredTermService.addNonPreferredTerm(Term.builder()
                .idTerm(preferredTerm.getIdTerm())
                .lexicalValue(value)
                .lang(selectedLang)
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .hidden(hidden)
                .status(hidden ? "Hidden" : "USE")
                .build(), idUser);

        refreshConceptDatas(idUser);

        conceptBean.getConcept(selectedTheso.getCurrentIdTheso(),
                conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentLang(), currentUser);

        MessageUtils.showInformationMessage("Synonyme ajouté avec succès");

        init();

        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
        PrimeFaces.current().ajax().update("containerIndex:formLeftTab");
    }

    /**
     * permet de modifier un synonyme
     */
    public void updateSynonym(NodeEM nodeEMLocal, int idUser, boolean isMultiple) {

        log.debug("Début de la modification du synonyme {}", nodeEMLocal.getLexicalValue());

        // save de la valeur pour une modification forcée
        this.nodeEM = nodeEMLocal;

        if (!nodeEMLocal.getOldValue().equals(nodeEMLocal.getLexicalValue())) {
            log.debug("Modification de la valeur de la synonyme");
            if (termService.existsPrefLabel(nodeEMLocal.getLexicalValue(), nodeEMLocal.getLang(), selectedTheso.getCurrentIdTheso())) {
                MessageUtils.showWarnMessage("Un label identique existe déjà !");
                duplicate = true;
                return;
            }
            if (termService.isAltLabelExist(nodeEMLocal.getLexicalValue(), selectedTheso.getCurrentIdTheso(), nodeEMLocal.getLang())) {
                MessageUtils.showErrorMessage("Un label identique existe déjà !");
                duplicate = true;
                return;
            }
            updateSynonymForced(idUser);
        } else {
            if (nodeEMLocal.isOldHiden() != nodeEMLocal.isHiden()) {
                updateStatus(nodeEMLocal, idUser);
            }
        }

        if (!isMultiple) {
            reset();
            prepareNodeEMForEdit();
        }
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }

    /**
     * permet de modifier un synonyme sans controle avec doublon
     */
    public void updateSynonymForced(int idUser) {

        duplicate = false;

        if (!nonPreferredTermService.updateNonPreferredTerm(nodeEM.getOldValue(), nodeEM.getLexicalValue(),
                conceptBean.getNodeConcept().getTerm().getIdTerm(), nodeEM.getLang(), selectedTheso.getCurrentIdTheso(),
                nodeEM.isHiden(), idUser)) {
            MessageUtils.showErrorMessage("La modification a échoué !");
            return;
        }

        refreshConceptDatas(idUser);
        conceptBean.getConcept(selectedTheso.getCurrentIdTheso(),
                conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentLang(), currentUser);

        MessageUtils.showInformationMessage("Synonyme modifié avec succès");
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }

    /**
     * permet d'ajouter une nouvelle traduction au concept
     */
    public void addNewSynonym(int idUser) {

        duplicate = false;

        if (checkData()) {
            addForced(idUser);
        }
    }

    private boolean checkData() {

        if (StringUtils.isEmpty(value)) {
            MessageUtils.showErrorMessage("La valeur est obligatoire !");
            return false;
        }

        if (StringUtils.isEmpty(selectedLang)) {
            MessageUtils.showErrorMessage("Pas de langue choisie !");
            return false;
        }

        if (termService.existsPrefLabel(value, selectedLang, selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage("Un label identique existe déjà !");
            duplicate = true;
            return false;
        }

        if (termService.isAltLabelExist(value, selectedTheso.getCurrentIdTheso(), selectedLang)) {
            MessageUtils.showErrorMessage("Un label identique existe déjà !");
            duplicate = true;
            return false;
        }

        return true;
    }

    /**
     * permet d'ajouter une nouvelle traduction au concept
     */
    public void addPropSynonym() {

        log.debug("Ajout d'une proposition synonyme !");
        checkData();

        if (CollectionUtils.isEmpty(propositionBean.getProposition().getSynonymsProp())) {
            propositionBean.getProposition().setSynonymsProp(new ArrayList<>());
        }

        for (SynonymPropBean synonym : propositionBean.getProposition().getSynonymsProp()) {
            if (synonym.getLexicalValue().equalsIgnoreCase(value)
                    && synonym.getLang().equalsIgnoreCase(selectedLang)) {
                MessageUtils.showErrorMessage("Un label identique existe déjà !");
                return;
            }
        }

        SynonymPropBean synonymPropBean = new SynonymPropBean();
        synonymPropBean.setToAdd(true);
        synonymPropBean.setHiden(hidden);
        synonymPropBean.setLang(selectedLang);
        synonymPropBean.setLexicalValue(value);
        synonymPropBean.setOldValue(value);
        propositionBean.getProposition().getSynonymsProp().add(synonymPropBean);
        propositionBean.setVarianteAccepted(true);
    }

    public boolean isVarianteMenuDisable() {
        return CollectionUtils.isEmpty(propositionBean.getProposition().getSynonymsProp());
    }

    /**
     * permet de modifier un synonyme sans controle avec doublon
     */
    public void updateStatus(NodeEM nodeEM, int idUser) {

        log.debug("Modification du status de la synonyme {}", nodeEM.getLexicalValue());
        if (!nonPreferredTermService.updateStatusNonPreferredTerm(conceptBean.getNodeConcept().getTerm().getIdTerm(),
                nodeEM.getLexicalValue(), nodeEM.getLang(), selectedTheso.getCurrentIdTheso(), nodeEM.isHiden(), idUser)) {
            MessageUtils.showErrorMessage("La modification a échoué !");
            return;
        }

        refreshConceptDatas(idUser);

        MessageUtils.showInformationMessage("Synonyme modifié avec succès");
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
    }

    /**
     * permet de modifier tous les synonymes
     */
    public void updateAllSynonyms(int idUser) {

        if (nodeEMsForEdit == null) {
            MessageUtils.showInformationMessage("Aucune sélection !");
            return;
        }

        log.debug("Début de la modification de la liste des synonyms !");
        for (NodeEM nodeEM1 : nodeEMsForEdit) {
            updateSynonym(nodeEM1, idUser, true);
        }

        conceptBean.getConcept(selectedTheso.getCurrentIdTheso(),
                conceptBean.getNodeConcept().getConcept().getIdConcept(),
                selectedTheso.getCurrentLang(), currentUser);
        reset();
        prepareNodeEMForEdit();
        PrimeFaces.current().executeScript("PF('renameSynonym').hide();");
    }


    public void updateSynonymProp(SynonymPropBean synonymPropBean) {

        if (synonymPropBean == null) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }

        if (StringUtils.isEmpty(synonymPropBean.getLexicalValue())) {
            MessageUtils.showErrorMessage("Le champs valeur est obligatoire !");
            return;
        }

        if (synonymPropBean.isToUpdate() && synonymPropBean.getLexicalValue().equals(synonymPropBean.getOldValue())) {
            synonymPropBean.setToUpdate(false);
            return;
        }

        if (!synonymPropBean.getOldValue().equals(synonymPropBean.getLexicalValue())) {
            if (termService.existsPrefLabel(synonymPropBean.getLexicalValue(), synonymPropBean.getLang(), selectedTheso.getCurrentIdTheso())) {
                MessageUtils.showErrorMessage("Un label identique existe déjà !");
                return;
            }

            if (termService.isAltLabelExist(synonymPropBean.getLexicalValue(), selectedTheso.getCurrentIdTheso(), synonymPropBean.getLang())) {
                MessageUtils.showErrorMessage("Un label identique existe déjà !");
                return;
            }

            if (synonymPropBean.isToRemove()) {
                synonymPropBean.setLexicalValue(synonymPropBean.getOldValue());
            } else if (!synonymPropBean.isToAdd()) {
                synonymPropBean.setToUpdate(true);
            }
        } else {
            if (synonymPropBean.isOldHiden() != synonymPropBean.isHiden()) {
                if (synonymPropBean.isToRemove()) {
                    synonymPropBean.setLexicalValue(synonymPropBean.getOldValue());
                } else if (!synonymPropBean.isToAdd()) {
                    synonymPropBean.setToUpdate(true);
                }
            }
        }

        propositionBean.checkSynonymPropositionStatus();
    }


    /**
     * permet de supprimer un synonyme
     */
    public void deleteSynonym(NodeEM nodeEM, int idUser) {

        if (nodeEM == null) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }

        nonPreferredTermService.deleteNonPreferredTerm(conceptBean.getNodeConcept().getTerm().getIdTerm(),
                nodeEM.getLang(), nodeEM.getLexicalValue(), selectedTheso.getCurrentIdTheso(), idUser);

        refreshConceptDatas(idUser);

        MessageUtils.showInformationMessage("Synonyme supprimé avec succès");
        reset();
        PrimeFaces.current().ajax().update("containerIndex:formRightTab");
        PrimeFaces.current().ajax().update("conceptForm:listSynonimesToDelete");
    }

    public void deleteSynonymPropo(SynonymPropBean spb) {

        if (spb == null) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }

        if (!CollectionUtils.isEmpty(propositionBean.getProposition().getSynonymsProp())) {
            for (SynonymPropBean synonym : propositionBean.getProposition().getSynonymsProp()) {
                if (spb.getLexicalValue().equals(synonym.getLexicalValue())) {
                    if (synonym.isToAdd()) {
                        propositionBean.getProposition().getSynonymsProp().remove(synonym);
                    } else if (synonym.isToUpdate()) {
                        synonym.setToRemove(true);
                        synonym.setToUpdate(false);
                        synonym.setLexicalValue(synonym.getOldValue());
                    } else {
                        synonym.setToRemove(!synonym.isToRemove());
                    }
                }
            }
        } else {
            log.error("Aucune proposition d'un synonyme n'est présent !");
        }

        propositionBean.checkSynonymPropositionStatus();
    }

    public void cancel() {
        duplicate = false;
        this.nodeEM = null;
    }

    private void refreshConceptDatas(int idUser) {
        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(),conceptBean.getNodeConcept().getConcept().getIdConcept(), idUser);
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(conceptBean.getNodeConcept().getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());
    }
}
