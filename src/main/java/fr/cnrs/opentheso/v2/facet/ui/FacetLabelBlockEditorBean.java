package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.v2.facet.write.model.command.RenameFacetLabelCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.Serializable;

/**
 * Édition inline du libellé de la fiche facette (langue courante).
 */
@Getter
@Setter
@ViewScoped
@Named("v2FacetLabelBlockEditorBean")
@RequiredArgsConstructor
public class FacetLabelBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "f-label";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient FacetMutationService facetMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingFacetId;
    private String editingLang;
    private String preferredLabel;
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedFacet() != null
                && conceptWritePolicy.canMutateLexicalContent(userSession, false);
    }

    public boolean isEditing() {
        if (editing && !matchesCurrentFacet()) {
            resetForm(false);
        }
        return editing && FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard());
    }

    public void startEditing() {
        if (!isEditable()) {
            return;
        }
        FacetDetailOverview facet = thesaurusViewBean.getSelectedFacet();
        if (facet == null || StringUtils.isBlank(facet.facetId())) {
            return;
        }
        editingFacetId = facet.facetId();
        editingLang = resolveLang(facet);
        preferredLabel = StringUtils.defaultString(facet.label());
        errorMessage = "";
        flashMessage = "";
        flashToken = "";
        editing = true;
        thesaurusViewBean.setFicheEditCard(FICHE_CARD);
    }

    public void cancel() {
        resetForm(false);
    }

    public void save() {
        errorMessage = "";
        if (!isEditable() || !isEditing()) {
            return;
        }
        if (userSession.getCurrentUserId() == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        FacetDetailOverview current = thesaurusViewBean.getSelectedFacet();
        if (current == null || StringUtils.isBlank(current.facetId())) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        String value = StringUtils.trimToEmpty(preferredLabel);
        if (value.isEmpty()) {
            errorMessage = "Le libellé est obligatoire.";
            return;
        }
        if (Strings.CS.equals(StringUtils.trimToEmpty(current.label()), value)) {
            finishSuccess();
            return;
        }
        MutationResult result = facetMutationService.renamePreferredLabel(new RenameFacetLabelCommand(
                thesaurusViewBean.getId(),
                current.facetId(),
                resolveLang(current),
                value
        ));
        if (result == null || result.outcome() != MutationOutcome.OK) {
            errorMessage = result == null
                    ? "L'enregistrement a échoué."
                    : StringUtils.defaultIfBlank(result.message(), "L'enregistrement a échoué.");
            return;
        }
        finishSuccess();
    }

    private void finishSuccess() {
        editing = false;
        if (FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        errorMessage = "";
        flashMessage = "Libellé enregistré";
        flashToken = String.valueOf(System.currentTimeMillis());
        thesaurusViewBean.reloadSelectedConcept();
    }

    private void resetForm(boolean keepFlash) {
        editing = false;
        if (FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        editingFacetId = null;
        editingLang = null;
        preferredLabel = "";
        errorMessage = "";
        if (!keepFlash) {
            flashMessage = "";
            flashToken = "";
        }
    }

    private boolean matchesCurrentFacet() {
        FacetDetailOverview facet = thesaurusViewBean.getSelectedFacet();
        return facet != null && Strings.CS.equals(editingFacetId, facet.facetId());
    }

    private String resolveLang(FacetDetailOverview facet) {
        if (facet != null && StringUtils.isNotBlank(facet.lang())) {
            return facet.lang();
        }
        return StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
    }
}
