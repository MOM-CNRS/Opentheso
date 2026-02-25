package fr.cnrs.opentheso.bean.concept;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.proposition.PropositionBean;
import fr.cnrs.opentheso.bean.proposition.TraductionPropBean;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.TermService;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;


@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "traductionBean")
public class TraductionBean implements Serializable {

    private final PropositionBean propositionBean;
    private final ConceptView conceptBean;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final LanguageBean languageBean;
    private final TermService termService;
    private final ConceptService conceptService;
    private final ConceptDcTermRepository conceptDcTermRepository;

    private String selectedLang, traductionValue;
    private List<NodeLangTheso> nodeLangs, nodeLangsFiltered;
    private List<NodeTermTraduction> nodeTermTraductions, nodeTermTraductionsForEdit;


    public void reset() {
        nodeLangs = selectedTheso.getNodeLangs();
        nodeLangsFiltered = new ArrayList<>();
        nodeTermTraductions = conceptBean.getNodeConcept().getNodeTermTraductions();
        selectedLang = null;
        traductionValue = "";
    }

    public void setTraductionsForEdit() {

        log.debug("Préparation de la liste des traductions disponibles");
        reset();

        nodeTermTraductionsForEdit = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeTermTraductions)) {
            for (NodeTermTraduction nodeTermTraduction : nodeTermTraductions) {
                NodeTermTraduction nodeTermTraduction1 = new NodeTermTraduction();
                nodeTermTraduction1.setLexicalValue(nodeTermTraduction.getLexicalValue());
                nodeTermTraduction1.setLang(nodeTermTraduction.getLang());
                nodeTermTraductionsForEdit.add(nodeTermTraduction1);
            }
        }
    }

    public void setLangWithNoTraduction() {

        nodeLangsFiltered.addAll(nodeLangs);

        log.debug("Préparation de la liste des langues à ignorer");
        List<String> langsToRemove = new ArrayList<>();
        langsToRemove.add(conceptBean.getSelectedLang());
        if (CollectionUtils.isNotEmpty(conceptBean.getNodeConcept().getNodeTermTraductions())) {
            langsToRemove.addAll(conceptBean.getNodeConcept().getNodeTermTraductions().stream().map(NodeTermTraduction::getLang).toList());
        }

        if (CollectionUtils.isNotEmpty(nodeLangs)) {
            nodeLangsFiltered = nodeLangs.stream()
                    .filter(element -> !toRemove(element.getCode(), langsToRemove))
                    .toList();
        }

        if (CollectionUtils.isEmpty(nodeLangsFiltered)) {
            log.debug(languageBean.getMsg("concept.translate.isTranslatedIntoAllLang"));
            MessageUtils.showInformationMessage(languageBean.getMsg("concept.translate.isTranslatedIntoAllLang"));
        } else {
            PrimeFaces.current().executeScript("PF('addTraduction').show();");
        }
    }

    private boolean toRemove(String lang, List<String> langsToRemove) {

        for (String langToRemove : langsToRemove) {
            if (lang.equals(langToRemove)) {
                return true;
            }
        }
        return false;
    }

    public void setLangWithNoTraductionProp() {
        
        reset();

        nodeLangsFiltered.addAll(nodeLangs);

        List<String> langsToRemove = new ArrayList<>();
        langsToRemove.add(conceptBean.getSelectedLang());

        if (CollectionUtils.isNotEmpty(conceptBean.getNodeConcept().getNodeTermTraductions())) {
            for (TraductionPropBean nodeTermTraduction : propositionBean.getProposition().getTraductionsProp()) {
                langsToRemove.add(nodeTermTraduction.getLang());
            }
        }

        if (CollectionUtils.isNotEmpty(nodeLangs)) {
            nodeLangsFiltered.addAll(nodeLangs);
            for (NodeLangTheso nodeLang : nodeLangs) {
                if (langsToRemove.contains(nodeLang.getCode())) {
                    nodeLangsFiltered.remove(nodeLang);
                }
            }
        }

        if (CollectionUtils.isEmpty(nodeLangsFiltered)) {
            MessageUtils.showInformationMessage(languageBean.getMsg("concept.translate.isTranslatedIntoAllLang"));
            PrimeFaces.current().ajax().update("containerIndex:rightTab:idAddTraduction");
        } else {
            PrimeFaces.current().executeScript("PF('addTraductionProp').show();");
        }
    }

    public void infos() {
        MessageUtils.showInformationMessage("Rédiger une aide ici pour Add Concept !");
    }

    /**
     * permet d'ajouter une nouvelle traduction au concept
     */
    public void addNewTraduction(int idUser) {

        if (StringUtils.isEmpty(traductionValue)) {
            MessageUtils.showErrorMessage("La valeur est obligatoire !");
            return;
        }
        if (StringUtils.isEmpty(selectedLang)) {
            MessageUtils.showErrorMessage("Aucune langue sélectionnée !");
            return;
        }
        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage("Aucun thésaurus sélectionné !");
            return;
        }

        if (termService.isTermExistIgnoreCase(traductionValue, selectedTheso.getCurrentIdTheso(), selectedLang)) {
            MessageUtils.showErrorMessage("Un label identique existe dans cette langue !");
            return;
        }

        log.debug("Ajout de la nouvelle traduction dans la base de données");
        termService.addTermTraduction(fr.cnrs.opentheso.models.terms.Term.builder()
                .idTerm(conceptBean.getNodeFullConcept().getPrefLabel().getIdTerm())
                .lexicalValue(traductionValue)
                .lang(selectedLang)
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .creator(idUser)
                .source("")
                .status("")
                .build(), idUser);

        refreshConceptInformations(idUser);

        MessageUtils.showInformationMessage(languageBean.getMsg("concept.translate.success"));

        reset();
        setLangWithNoTraduction();

        PrimeFaces.current().ajax().update("containerIndex:rightTab:idAddTraduction");
        PrimeFaces.current().executeScript("PF('addTraduction').show();");
    }

    private void refreshConceptInformations(int idUser) {

        log.debug("Rafraîchissement des données du concept");
        conceptBean.getConcept(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getConcept().getIdConcept(),
                conceptBean.getSelectedLang(), currentUser);

        log.debug("Mise à jour de la date de mise à jour du concept");
        conceptService.updateDateOfConcept(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getConcept().getIdConcept(), idUser);

        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(currentUser.getNodeUser().getName())
                .idConcept(conceptBean.getNodeConcept().getConcept().getIdConcept())
                .idThesaurus(selectedTheso.getCurrentIdTheso())
                .build());
    }

    public void addNewTraductionProposition() {

        if (StringUtils.isEmpty(traductionValue)) {
            MessageUtils.showErrorMessage("Aucune valeur n'est saisie !");
            return;
        }
        if (selectedLang == null || selectedLang.isEmpty()) {
            MessageUtils.showErrorMessage("Aucune langue n'est sélectionnée !");
            return;
        }

        if (termService.isTermExistIgnoreCase(traductionValue, selectedTheso.getCurrentIdTheso(), selectedLang)) {
            MessageUtils.showErrorMessage("Un label identique existe dans cette langue !");
            return;
        }

        for (TraductionPropBean traductionPropBean : propositionBean.getProposition().getTraductionsProp()) {
            if (selectedLang.equalsIgnoreCase(traductionPropBean.getLang())
                    && traductionValue.equalsIgnoreCase(traductionPropBean.getLexicalValue())) {

                MessageUtils.showErrorMessage("Un label identique existe dans cette langue !");
                return;
            }
        }

        var traductionProp = new TraductionPropBean();
        traductionProp.setLang(selectedLang);
        traductionProp.setLexicalValue(traductionValue);
        traductionProp.setIdTerm(conceptBean.getNodeConcept().getTerm().getIdTerm());
        traductionProp.setToAdd(true);
        propositionBean.getProposition().getTraductionsProp().add(traductionProp);
        propositionBean.setTraductionAccepted(true);
    }

    /**
     * permet de modifier une traduction au concept
     *
     * @param nodeTermTraduction
     * @param idUser
     */
    public void updateTraduction(NodeTermTraduction nodeTermTraduction, int idUser) {

        if (nodeTermTraduction == null || nodeTermTraduction.getLexicalValue().isEmpty()) {
            MessageUtils.showErrorMessage("Veuillez saisir une valeur !");
            return;
        }

        if (termService.isTermExistIgnoreCase(nodeTermTraduction.getLexicalValue(), selectedTheso.getCurrentIdTheso(), nodeTermTraduction.getLang())) {
            MessageUtils.showErrorMessage("Un label identique existe dans cette langue !");
            return;
        }

        termService.updateTermTraduction(nodeTermTraduction.getLexicalValue(), conceptBean.getNodeConcept().getTerm().getIdTerm(),
                nodeTermTraduction.getLang(), selectedTheso.getCurrentIdTheso(), idUser);

        refreshConceptInformations(idUser);

        MessageUtils.showInformationMessage("Traduction modifiée avec succès");

        reset();

        PrimeFaces.current().ajax().update("containerIndex:rightTab:idRenameTraduction");
        PrimeFaces.current().executeScript("PF('renameTraduction').show();");
    }

    public void updateTraductionProp(TraductionPropBean traductionPropBean) {

        if (traductionPropBean.getLexicalValue().isEmpty()) {
            MessageUtils.showErrorMessage("Veuillez saisir une valeur !");
            return;
        }
        
        if (traductionPropBean.isToUpdate() && traductionPropBean.getLexicalValue().equalsIgnoreCase(traductionPropBean.getOldValue())){
            traductionPropBean.setToUpdate(false);
            return;
        }

        // Rechercher dans la base s'il existe un label identique
        if (termService.isTermExistIgnoreCase(traductionPropBean.getLexicalValue(), selectedTheso.getCurrentIdTheso(), traductionPropBean.getLang())) {
            MessageUtils.showErrorMessage("Un label identique existe dans cette langue !");
            return;
        }

        for (int i = 0; i < propositionBean.getProposition().getTraductionsProp().size(); i++) {
            if (propositionBean.getProposition().getTraductionsProp().get(i).getLang()
                    .equals(traductionPropBean.getLang())) {
                if (propositionBean.getProposition().getTraductionsProp().get(i).isToRemove()) {
                    propositionBean.getProposition().getTraductionsProp().get(i).setToRemove(false);
                    propositionBean.getProposition().getTraductionsProp().get(i).setToUpdate(true);
                    propositionBean.getProposition().getTraductionsProp().get(i).setLexicalValue(traductionPropBean.getLexicalValue());
                } else if (propositionBean.getProposition().getTraductionsProp().get(i).isToAdd()) {
                    propositionBean.getProposition().getTraductionsProp().get(i).setLexicalValue(traductionPropBean.getLexicalValue());
                } else {
                    propositionBean.getProposition().getTraductionsProp().get(i).setToUpdate(true);
                    propositionBean.getProposition().getTraductionsProp().get(i).setLexicalValue(traductionPropBean.getLexicalValue());
                }
                propositionBean.setTraductionAccepted(propositionBean.getProposition().getTraductionsProp().get(i).isToUpdate());
            }
        }
        
        propositionBean.checkTraductionPropositionStatus();
    }

    /**
     * permet de modifier toutes les traductions du concept multiple corrections
     *
     * @param idUser
     */
    public void updateAllTraduction(int idUser) {

        if (nodeTermTraductionsForEdit == null || nodeTermTraductionsForEdit.isEmpty()) {
            MessageUtils.showInformationMessage("Veuillez saisir une valeur !");
            return;
        }

        boolean isModified = false;

        for (NodeTermTraduction nodeTermTraduction : nodeTermTraductionsForEdit) {
            var toModify = false;
            isModified = false;

            log.debug("Vérification si le terme {} a changé !", nodeTermTraduction.getLexicalValue());
            for (NodeTermTraduction nodeTermTraductionOld : nodeTermTraductions) {
                if (nodeTermTraduction.getLang().equalsIgnoreCase(nodeTermTraductionOld.getLang())) {
                    toModify = !nodeTermTraduction.getLexicalValue().equalsIgnoreCase(nodeTermTraductionOld.getLexicalValue());
                    break;
                }
            }
            if (toModify) {
                if (termService.isTermExistIgnoreCase(nodeTermTraduction.getLexicalValue(), selectedTheso.getCurrentIdTheso(), nodeTermTraduction.getLang())) {
                    MessageUtils.showErrorMessage("Un label identique existe dans cette langue : " + nodeTermTraduction.getLang());
                    continue;
                }

                log.debug("Mise à jour du terme {} ({}) dans la base de donnée", nodeTermTraduction.getLexicalValue(), nodeTermTraduction.getLang());
                termService.updateTermTraduction(nodeTermTraduction.getLexicalValue(), conceptBean.getNodeConcept().getTerm().getIdTerm(),
                        nodeTermTraduction.getLang(), selectedTheso.getCurrentIdTheso(), idUser);
                isModified = true;
            }
        }

        if (isModified) {
            refreshConceptInformations(idUser);

            MessageUtils.showInformationMessage("Traduction modifiée avec succès");
            reset();

            PrimeFaces.current().ajax().update("containerIndex:rightTab:idRenameTraduction");
            PrimeFaces.current().executeScript("PF('renameTraduction').show();");
        } else {
            MessageUtils.showInformationMessage("Aucune modification à faire");
        }
    }

    /**
     * permet de supprimer une traduction au concept
     *
     * @param nodeTermTraduction
     * @param idUser
     */
    public void deleteTraduction(NodeTermTraduction nodeTermTraduction, int idUser) {

        if (nodeTermTraduction == null || nodeTermTraduction.getLang().isEmpty()) {
            MessageUtils.showErrorMessage("Erreur de sélection de traduction !");
            return;
        }

        log.debug("Suppression de la traduction dans la base de données");
        termService.deleteTerm(selectedTheso.getCurrentIdTheso(), conceptBean.getNodeConcept().getTerm().getIdTerm(), nodeTermTraduction.getLang());

        refreshConceptInformations(idUser);

        MessageUtils.showInformationMessage("Traduction supprimée avec succès");

        reset();

        PrimeFaces.current().ajax().update("containerIndex:rightTab:idDeleteTraduction");
        PrimeFaces.current().executeScript("PF('deleteTraduction').show();");
        log.debug("Suppression de la traduction du terme {} ({}) terminée",
                conceptBean.getNodeConcept().getTerm().getIdTerm(), nodeTermTraduction.getLang());
    }

    public void deleteTraductionProp(TraductionPropBean traductionPropBean) {

        for (int i = 0; i < propositionBean.getProposition().getTraductionsProp().size(); i++) {
            if (propositionBean.getProposition().getTraductionsProp().get(i).getLexicalValue()
                    .equals(traductionPropBean.getLexicalValue())) {
                if (propositionBean.getProposition().getTraductionsProp().get(i).isToUpdate()) {
                    propositionBean.getProposition().getTraductionsProp().get(i).setToRemove(true);
                    propositionBean.getProposition().getTraductionsProp().get(i).setToUpdate(false);
                    propositionBean.getProposition().getTraductionsProp().get(i).setLexicalValue(
                        propositionBean.getProposition().getTraductionsProp().get(i).getOldValue());
                } else if (propositionBean.getProposition().getTraductionsProp().get(i).isToAdd()) {
                    propositionBean.getProposition().getTraductionsProp().remove(i);
                } else {
                    propositionBean.getProposition().getTraductionsProp().get(i).setToRemove(
                            !propositionBean.getProposition().getTraductionsProp().get(i).isToRemove());
                }
            }
        }
        
        propositionBean.checkTraductionPropositionStatus();
    }

}
