package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.TraductionDto;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ViewScoped
@RequiredArgsConstructor
@Named("v2TraductionCandidatBean")
public class CandidatTranslationBean implements Serializable {

    private static final String CONTAINER_INDEX = "containerIndex";

    private final transient CandidatBean candidatBean;
    private final transient CandidatMutationService candidatMutationService;
    private final transient UserSession userSession;
    private final transient V2LocaleBean localeBean;

    private String langage, traduction, langageOld, traductionOld, newLangage, newTraduction;
    private List<NodeLangTheso> nodeLanguesFiltered;

    public void init(TraductionDto traductionDto) {
        langage = traductionDto.getLangue();
        traduction = traductionDto.getTraduction();

        langageOld = langage;
        traductionOld = traduction;
    }

    public void init() {
        newLangage = "";
        newTraduction = "";
        initLanguages();
        if (CollectionUtils.isEmpty(nodeLanguesFiltered)) {
            MessageUtils.showWarnMessage("Le candidat est traduit dans toutes les langues du thésaurus");
        } else {
            PrimeFaces.current().executeScript("PF('newTraduction').show();");
        }
    }

    private void initLanguages() {
        nodeLanguesFiltered = new ArrayList<>(candidatBean.getLanguagesOfTheso());

        List<String> languesToRemove = new ArrayList<>();
        languesToRemove.add(candidatBean.getCandidatSelected().getLang());
        for (TraductionDto traductionDto : candidatBean.getCandidatSelected().getTraductions()) {
            languesToRemove.add(traductionDto.getLangue());
        }

        nodeLanguesFiltered.removeIf(nodeLang -> languesToRemove.contains(nodeLang.getCode()));
    }

    public void deleteTraduction() {
        candidatMutationService.deleteCandidateTranslation(
                candidatBean.getCandidatSelected().getIdThesaurus(), candidatBean.getCandidatSelected().getIdTerm(), langage);

        loadTraductionList();
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.traduction.msg2"));
        PrimeFaces.current().ajax().update(CONTAINER_INDEX);
    }

    public void updateTraduction() {
        candidatMutationService.updateTermLabel(traduction, candidatBean.getCandidatSelected().getIdThesaurus(),
                langage, candidatBean.getCandidatSelected().getIdTerm());

        loadTraductionList();
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.traduction.msg3"));
        PrimeFaces.current().ajax().update(CONTAINER_INDEX);
    }

    public void addTraductionCandidat() {
        if (candidatMutationService.isLabelExistIgnoreCase(
                newTraduction, candidatBean.getCandidatSelected().getIdThesaurus(), newLangage)) {
            MessageUtils.showErrorMessage("Un label existe dans le thésaurus pour : "
                    + candidatBean.getCandidatSelected().getIdConcepte() + "#" + newTraduction + "(" + langage + ")");
            return;
        }

        Term term = new Term();
        term.setStatus("D");
        term.setSource("Candidat");
        term.setLang(newLangage);
        term.setLexicalValue(newTraduction);
        term.setIdThesaurus(candidatBean.getCandidatSelected().getIdThesaurus());
        term.setContributor(candidatBean.getCandidatSelected().getUserId());
        term.setCreator(candidatBean.getCandidatSelected().getUserId());
        term.setIdTerm(candidatBean.getCandidatSelected().getIdTerm());
        candidatMutationService.addCandidateTranslation(term, requireUserId());

        loadTraductionList();
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.traduction.msg1"));
    }

    private void loadTraductionList() {
        var traductions = candidatMutationService.loadCandidateTranslations(
                candidatBean.getCandidatSelected().getIdConcepte(),
                candidatBean.getCandidatSelected().getIdThesaurus(),
                candidatBean.getCandidatSelected().getLang());
        candidatBean.getCandidatSelected().setTraductions(traductions);
        PrimeFaces.current().ajax().update(CONTAINER_INDEX);
    }

    private int requireUserId() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }
        return userId;
    }
}
