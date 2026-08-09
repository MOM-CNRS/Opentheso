package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.entites.ConceptType;
import fr.cnrs.opentheso.models.concept.NodeConceptType;
import fr.cnrs.opentheso.repositories.ConceptTypeRepository;
import fr.cnrs.opentheso.services.ConceptTypeService;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
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
import java.util.Collections;
import java.util.List;

/**
 * CRUD des types de concept (menu fil d'Ariane legacy « Gérer le type de concept »).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptTypeManagerBean")
@RequiredArgsConstructor
public class ConceptTypeManagerBean implements Serializable {

    private final ConceptTypeService conceptTypeService;
    private final ConceptTypeRepository conceptTypeRepository;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private List<NodeConceptType> conceptTypes = Collections.emptyList();
    private NodeConceptType conceptTypeToAdd = new NodeConceptType();
    private NodeConceptType conceptTypeToDelete;

    public boolean isManageAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession)
                && thesaurusBrowseBean.isCustomRelationVisible();
    }

    public void prepareManage() {
        if (!isManageAvailable()) {
            return;
        }
        reloadTypes();
        conceptTypeToAdd = new NodeConceptType();
        conceptTypeToDelete = null;
    }

    public void prepareAdd() {
        conceptTypeToAdd = new NodeConceptType();
    }

    public void applyChange(NodeConceptType nodeConceptType) {
        if (!isManageAvailable() || nodeConceptType == null || StringUtils.isBlank(nodeConceptType.getCode())) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        if (nodeConceptType.isPermanent()) {
            MessageUtils.showErrorMessage("Type permanent non modifiable");
            return;
        }
        if (!conceptTypeService.updateConceptType(thesaurusContext.resolveThesaurusId(), nodeConceptType)) {
            MessageUtils.showErrorMessage("Mise à jour impossible");
            return;
        }
        reloadTypes();
        MessageUtils.showInformationMessage("Type mis à jour");
    }

    public void prepareDelete(NodeConceptType nodeConceptType) {
        conceptTypeToDelete = nodeConceptType;
    }

    public void deleteCustomRelationship() {
        if (!isManageAvailable() || conceptTypeToDelete == null || StringUtils.isBlank(conceptTypeToDelete.getCode())) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        if (conceptTypeToDelete.isPermanent()) {
            MessageUtils.showErrorMessage("Type permanent non supprimable");
            return;
        }
        conceptTypeService.deleteConceptType(thesaurusContext.resolveThesaurusId(), conceptTypeToDelete);
        conceptTypeToDelete = null;
        reloadTypes();
        MessageUtils.showInformationMessage("Type supprimé");
        PrimeFaces.current().executeScript("PF('v2ConfirmDeleteConceptType').hide();");
    }

    public void addNewConceptType() {
        if (!isManageAvailable() || conceptTypeToAdd == null || StringUtils.isBlank(conceptTypeToAdd.getCode())) {
            MessageUtils.showErrorMessage("Code obligatoire");
            return;
        }
        String code = fr.cnrs.opentheso.utils.StringUtils.unaccentLowerString(conceptTypeToAdd.getCode())
                .replaceAll(" ", "");
        conceptTypeToAdd.setCode(code);
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (conceptTypeService.isConceptTypeExist(thesaurusId, conceptTypeToAdd)) {
            MessageUtils.showErrorMessage("Ce type existe déjà");
            return;
        }
        conceptTypeService.addNewConceptType(thesaurusId, conceptTypeToAdd);
        conceptTypeToAdd = new NodeConceptType();
        reloadTypes();
        MessageUtils.showInformationMessage("Type ajouté");
        PrimeFaces.current().executeScript("PF('v2AddNewConceptTypeDlg').hide();");
    }

    private void reloadTypes() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            conceptTypes = Collections.emptyList();
            return;
        }
        List<ConceptType> types = conceptTypeRepository.findAllByIdThesaurusIn(List.of(thesaurusId, "all"));
        if (CollectionUtils.isEmpty(types)) {
            conceptTypes = Collections.emptyList();
            return;
        }
        conceptTypes = types.stream()
                .map(type -> NodeConceptType.builder()
                        .code(type.getCode())
                        .labelFr(type.getLabelFr())
                        .labelEn(type.getLabelEn())
                        .reciprocal(type.isReciprocal())
                        .permanent("all".equalsIgnoreCase(type.getIdThesaurus()))
                        .build())
                .toList();
    }
}
