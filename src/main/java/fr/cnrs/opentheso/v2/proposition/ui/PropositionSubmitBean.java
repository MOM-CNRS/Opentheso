package fr.cnrs.opentheso.v2.proposition.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSubmission;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;

@Getter
@Setter
@ViewScoped
@Named("v2PropositionSubmitBean")
@RequiredArgsConstructor
public class PropositionSubmitBean implements Serializable {

    private final PropositionMutationService propositionMutationService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;

    private String authorName;
    private String authorEmail;
    private String comment;

    public void prepare() {
        comment = "";
        if (userSession.isLoggedIn()) {
            authorName = userSession.getCurrentUsername();
            authorEmail = userSession.getCurrentUserEmail();
        } else {
            authorName = "";
            authorEmail = "";
        }
    }

    public void submit() {
        if (!conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Aucun concept sélectionné");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        if (StringUtils.isBlank(comment)) {
            MessageUtils.showWarnMessage("Veuillez saisir votre proposition");
            return;
        }
        if (StringUtils.isBlank(authorEmail)) {
            MessageUtils.showWarnMessage("Veuillez saisir votre adresse email");
            return;
        }

        var submission = new PropositionSubmission(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.getCurrentThesaurusTitle(),
                summary.conceptId(),
                summary.preferredLabel(),
                thesaurusContext.resolveWorkLanguage(),
                authorName,
                authorEmail,
                comment
        );

        boolean saved = propositionMutationService.submit(submission);
        if (!saved) {
            MessageUtils.showWarnMessage("Vous avez déjà une proposition en cours pour ce concept");
            return;
        }
        comment = "";
        MessageUtils.showInformationMessage("Votre proposition a bien été envoyée");
        PrimeFaces.current().executeScript("PF('v2SuggestImprovement').hide();");
    }
}
