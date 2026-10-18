package fr.cnrs.opentheso.facet.ui;

import fr.cnrs.opentheso.concept.model.ConceptTreeNodeKinds;
import fr.cnrs.opentheso.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.concept.write.model.MutationResult;
import fr.cnrs.opentheso.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.facet.write.model.command.DeleteFacetCommand;
import fr.cnrs.opentheso.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Suppression de la facette ouverte dans la fiche V2.
 */
@Getter
@Setter
@ViewScoped
@Named("facetCardDeleteBean")
@RequiredArgsConstructor
public class FacetCardDeleteBean implements Serializable {

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient FacetMutationService facetMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;

    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isDeletable() {
        return thesaurusViewBean.getSelectedFacet() != null
                && conceptWritePolicy.canMutateHierarchicalRelations(userSession, false);
    }

    public void deleteFacet() {
        errorMessage = "";
        if (!isDeletable() || userSession.getCurrentUserId() == null) {
            errorMessage = WriteUiMessages.unauthorized(localeBean);
            return;
        }
        FacetDetailOverview facet = thesaurusViewBean.getSelectedFacet();
        if (facet == null || StringUtils.isBlank(facet.facetId())) {
            errorMessage = WriteUiMessages.unauthorized(localeBean);
            return;
        }
        MutationResult result = facetMutationService.deleteFacet(new DeleteFacetCommand(
                thesaurusViewBean.getId(),
                facet.facetId()
        ));
        if (result == null || !result.success()) {
            errorMessage = result == null
                    ? WriteUiMessages.msg(localeBean, "facet.delete.failed", "La suppression de la facette a échoué")
                    : StringUtils.defaultIfBlank(result.message(),
                    WriteUiMessages.msg(localeBean, "facet.delete.failed", "La suppression de la facette a échoué"));
            return;
        }
        flashMessage = WriteUiMessages.msg(localeBean, "facet.delete.done", "Facette supprimée");
        flashToken = String.valueOf(System.currentTimeMillis());
        errorMessage = "";
        if (StringUtils.isNotBlank(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        thesaurusViewBean.reloadTree();
        String parentId = StringUtils.trimToEmpty(facet.parentConceptId());
        if (StringUtils.isNotBlank(parentId)) {
            thesaurusViewBean.openTreeNode(parentId, ConceptTreeNodeKinds.CONCEPT);
            return;
        }
        thesaurusViewBean.showThesaurusHome();
    }
}
