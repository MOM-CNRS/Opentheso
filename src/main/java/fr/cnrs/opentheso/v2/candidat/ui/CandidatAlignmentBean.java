package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteAlignmentType;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
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
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2CandidatAlignmentBean")
@RequiredArgsConstructor
public class CandidatAlignmentBean implements Serializable {

    private final ConceptAlignmentMutationService conceptAlignmentMutationService;
    private final CandidatMutationService candidatMutationService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;

    private List<ConceptWriteAlignmentType> alignmentTypes = Collections.emptyList();
    private int manualAlignmentType = -1;
    private String manualAlignmentUri = "";
    private String manualAlignmentSource = "";

    public void loadAlignmentTypes() {
        alignmentTypes = conceptAlignmentMutationService.listAlignmentTypes();
    }

    public void reset() {
        loadAlignmentTypes();
        manualAlignmentUri = "";
        manualAlignmentSource = "";
        manualAlignmentType = alignmentTypes.isEmpty() ? -1 : alignmentTypes.get(0).getId();
    }

    public void showManualAlignmentDialog() {
        reset();
        PrimeFaces.current().executeScript("PF('addManualAlignment').show();");
    }

    public void addManualAlignment(CandidatDto candidat) {
        if (candidat == null || StringUtils.isBlank(candidat.getIdConcepte())) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        if (StringUtils.isBlank(manualAlignmentUri)) {
            MessageUtils.showErrorMessage("Veuillez saisir une valeur !");
            return;
        }
        if (!fr.cnrs.opentheso.utils.StringUtils.urlValidator(manualAlignmentUri)) {
            MessageUtils.showErrorMessage("L'URL n'est pas valide !");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }

        String thesaurusId = thesaurusContext.resolveThesaurusId();
        var command = new AddManualAlignmentCommand(
                thesaurusId,
                candidat.getIdConcepte(),
                manualAlignmentType,
                manualAlignmentUri,
                manualAlignmentSource,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        MutationResult result = conceptAlignmentMutationService.addManualAlignment(command);
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return;
        }

        candidat.setAlignments(candidatMutationService.loadAlignments(candidat.getIdConcepte(), thesaurusId));
        MessageUtils.showInformationMessage(result.message());
        PrimeFaces.current().ajax().update("tabViewCandidat messageIndex");
        PrimeFaces.current().executeScript("PF('addManualAlignment').hide();");
    }
}
