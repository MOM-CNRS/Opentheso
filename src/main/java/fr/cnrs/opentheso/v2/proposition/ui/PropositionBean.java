package fr.cnrs.opentheso.v2.proposition.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDetail;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionReadService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@SessionScoped
@Named("v2PropositionBean")
@RequiredArgsConstructor
public class PropositionBean implements Serializable {

    private final PropositionReadService propositionReadService;
    private final PropositionMutationService propositionMutationService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;

    private List<PropositionSummary> propositions = Collections.emptyList();
    private int pendingCount;
    private boolean showAll;

    private PropositionDetail selectedProposition;
    private String reviewComment;

    public void refresh() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        pendingCount = propositionReadService.countPending(thesaurusId);
        if (isManagerOnCurrentThesaurus()) {
            propositions = showAll
                    ? propositionReadService.listAll(thesaurusId)
                    : propositionReadService.listPending(thesaurusId);
        } else {
            propositions = Collections.emptyList();
        }
    }

    public boolean isManagerOnCurrentThesaurus() {
        return userSession.isLoggedIn() && userSession.isManager();
    }

    public void showPropositionDrawer() {
        refresh();
        PrimeFaces.current().ajax().update(":v2ListPropositionsPanel", ":containerIndex:header:v2NotificationProp");
        PrimeFaces.current().executeScript("showV2PropositionListBar();");
    }

    public void toggleShowAll() {
        refresh();
    }

    public void openReview(PropositionSummary proposition) {
        if (proposition == null) {
            return;
        }
        selectedProposition = propositionReadService.findDetail(proposition.id());
        reviewComment = "";
        if (selectedProposition != null && "ENVOYER".equals(selectedProposition.status())) {
            propositionMutationService.markRead(selectedProposition.id());
        }
    }

    public void approveSelected() {
        if (selectedProposition == null) {
            return;
        }
        propositionMutationService.approve(
                selectedProposition.id(),
                userSession.getCurrentUsername(),
                reviewComment,
                selectedProposition.conceptLabel(),
                thesaurusContext.getCurrentThesaurusTitle()
        );
        MessageUtils.showInformationMessage("Proposition approuvée");
        finishReview();
    }

    public void refuseSelected() {
        if (selectedProposition == null) {
            return;
        }
        propositionMutationService.refuse(
                selectedProposition.id(),
                userSession.getCurrentUsername(),
                reviewComment,
                selectedProposition.conceptLabel(),
                thesaurusContext.getCurrentThesaurusTitle()
        );
        MessageUtils.showInformationMessage("Proposition refusée");
        finishReview();
    }

    public void deleteSelected() {
        if (selectedProposition == null) {
            return;
        }
        propositionMutationService.delete(selectedProposition.id());
        MessageUtils.showInformationMessage("Proposition supprimée");
        finishReview();
    }

    private void finishReview() {
        selectedProposition = null;
        reviewComment = "";
        refresh();
        PrimeFaces.current().ajax().update(":v2ListPropositionsPanel", ":containerIndex:header:v2NotificationProp");
    }

    public void openProposition(PropositionSummary proposition) throws IOException {
        if (proposition == null || StringUtils.isAnyBlank(proposition.thesaurusId(), proposition.conceptId())) {
            return;
        }
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath()
                + "/v2/thesaurus?idt=" + proposition.thesaurusId().trim()
                + "&idc=" + proposition.conceptId().trim());
    }
}
