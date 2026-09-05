package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptTransferMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2CandidatTransferBean")
@RequiredArgsConstructor
public class CandidatTransferBean implements Serializable {

    private final transient ConceptTransferMutationService conceptTransferMutationService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient CandidatBean candidatBean;

    private String sourceThesaurusId;
    private String targetThesaurusId;
    private List<String> conceptIdsToMove = Collections.emptyList();
    private List<ConceptWriteThesaurusOption> availableThesauri = Collections.emptyList();

    public void initForCandidates(List<String> conceptIds, String sourceThesaurusId) {
        this.conceptIdsToMove = conceptIds == null ? new ArrayList<>() : new ArrayList<>(conceptIds);
        this.sourceThesaurusId = sourceThesaurusId;
        this.targetThesaurusId = null;
        loadAvailableThesauri();
    }

    public String getSourceThesaurusTitle() {
        return thesaurusContext.getCurrentThesaurusTitle();
    }

    public void onTargetThesaurusChange() {
        // Required to enable PrimeFaces ajax on selectOneMenu.
    }

    public void moveCandidates() {
        if (StringUtils.isAnyBlank(sourceThesaurusId, targetThesaurusId) || CollectionUtils.isEmpty(conceptIdsToMove)) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }

        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        String lang = thesaurusContext.resolveWorkLanguage();

        for (String conceptId : conceptIdsToMove) {
            var command = new MoveConceptToThesaurusCommand(
                    sourceThesaurusId,
                    targetThesaurusId,
                    conceptId,
                    List.of(conceptId),
                    lang,
                    userId,
                    contributor,
                    null
            );
            MutationResult result = conceptTransferMutationService.moveConceptToThesaurus(command);
            if (result == null || !result.success()) {
                MessageUtils.showErrorMessage(result != null ? result.message() : "Le déplacement a échoué !");
                return;
            }
        }

        candidatBean.initCandidatModule();
        PrimeFaces.current().ajax().update("tabViewCandidat:panelCandidateList");
        MessageUtils.showInformationMessage("Le déplacement a réussi");
        PrimeFaces.current().executeScript("PF('moveToAnotherThesoCA').hide();");
    }

    private void loadAvailableThesauri() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || StringUtils.isBlank(sourceThesaurusId)) {
            availableThesauri = Collections.emptyList();
            return;
        }
        availableThesauri = conceptTransferMutationService.listAdminThesauri(
                userId,
                userSession.isSuperAdmin(),
                sourceThesaurusId,
                thesaurusContext.resolveWorkLanguage()
        );
    }
}
