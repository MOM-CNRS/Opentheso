package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.entites.ConceptType;
import fr.cnrs.opentheso.models.concept.NodeConceptType;
import fr.cnrs.opentheso.repositories.ConceptTypeRepository;
import fr.cnrs.opentheso.services.ConceptTypeService;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD des types de concept (menu « Gérer les types de concept »).
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

    private List<NodeConceptType> conceptTypes = new ArrayList<>();
    private NodeConceptType conceptTypeToAdd = new NodeConceptType();
    private NodeConceptType conceptTypeToDelete;
    private String errorMessage;
    private String flashMessage;
    private String flashToken;
    private boolean dirty;

    public boolean isManageAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession)
                && thesaurusBrowseBean.isCustomRelationVisible();
    }

    public boolean isPendingDelete(NodeConceptType type) {
        return conceptTypeToDelete != null
                && type != null
                && StringUtils.equals(conceptTypeToDelete.getCode(), type.getCode());
    }

    public void prepareManage() {
        errorMessage = null;
        flashMessage = null;
        flashToken = null;
        dirty = false;
        conceptTypeToDelete = null;
        conceptTypeToAdd = new NodeConceptType();
        if (!isManageAvailable()) {
            errorMessage = "Action non autorisée";
            conceptTypes = new ArrayList<>();
            return;
        }
        reloadTypes();
    }

    public void applyChange(NodeConceptType nodeConceptType) {
        errorMessage = null;
        if (!isManageAvailable() || nodeConceptType == null || StringUtils.isBlank(nodeConceptType.getCode())) {
            errorMessage = "Action non autorisée";
            return;
        }
        if (nodeConceptType.isPermanent()) {
            errorMessage = "Ce type système n'est pas modifiable";
            return;
        }
        if (StringUtils.isAllBlank(nodeConceptType.getLabelFr(), nodeConceptType.getLabelEn())) {
            errorMessage = "Indiquez au moins un libellé";
            return;
        }
        conceptTypeToDelete = null;
        if (!conceptTypeService.updateConceptType(thesaurusContext.resolveThesaurusId(), nodeConceptType)) {
            errorMessage = "La mise à jour a échoué";
            return;
        }
        dirty = true;
        flashSuccess("Type « " + nodeConceptType.getCode() + " » enregistré");
        reloadTypes();
    }

    public void prepareDelete(NodeConceptType nodeConceptType) {
        errorMessage = null;
        if (nodeConceptType == null || nodeConceptType.isPermanent()) {
            errorMessage = "Ce type ne peut pas être supprimé";
            conceptTypeToDelete = null;
            return;
        }
        conceptTypeToDelete = nodeConceptType;
    }

    public void cancelDelete() {
        conceptTypeToDelete = null;
        errorMessage = null;
    }

    public void deleteCustomRelationship() {
        errorMessage = null;
        if (!isManageAvailable() || conceptTypeToDelete == null || StringUtils.isBlank(conceptTypeToDelete.getCode())) {
            errorMessage = "Action non autorisée";
            return;
        }
        if (conceptTypeToDelete.isPermanent()) {
            errorMessage = "Ce type système n'est pas supprimable";
            return;
        }
        String code = conceptTypeToDelete.getCode();
        conceptTypeService.deleteConceptType(thesaurusContext.resolveThesaurusId(), conceptTypeToDelete);
        conceptTypeToDelete = null;
        dirty = true;
        flashSuccess("Type « " + code + " » supprimé");
        reloadTypes();
    }

    public void addNewConceptType() {
        errorMessage = null;
        if (!isManageAvailable() || conceptTypeToAdd == null) {
            errorMessage = "Action non autorisée";
            return;
        }
        if (StringUtils.isBlank(conceptTypeToAdd.getCode())) {
            errorMessage = "Le code est obligatoire";
            return;
        }
        String code = fr.cnrs.opentheso.utils.StringUtils.unaccentLowerString(conceptTypeToAdd.getCode())
                .replaceAll(" ", "");
        conceptTypeToAdd.setCode(code);
        if (StringUtils.isBlank(code)) {
            errorMessage = "Le code est obligatoire";
            return;
        }
        if (StringUtils.isAllBlank(conceptTypeToAdd.getLabelFr(), conceptTypeToAdd.getLabelEn())) {
            errorMessage = "Indiquez au moins un libellé";
            return;
        }
        if (codeExists(code)) {
            errorMessage = "Le type « " + code + " » existe déjà";
            return;
        }
        conceptTypeToDelete = null;
        conceptTypeService.addNewConceptType(thesaurusContext.resolveThesaurusId(), conceptTypeToAdd);
        conceptTypeToAdd = new NodeConceptType();
        dirty = true;
        flashSuccess("Type « " + code + " » ajouté");
        reloadTypes();
    }

    public void finishAfterClose() {
        flashMessage = null;
        flashToken = null;
        errorMessage = null;
        conceptTypeToDelete = null;
        dirty = false;
    }

    private boolean codeExists(String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }
        return conceptTypes.stream().anyMatch(type -> code.equalsIgnoreCase(type.getCode()))
                || conceptTypeService.isConceptTypeExist(thesaurusContext.resolveThesaurusId(), conceptTypeToAdd);
    }

    private void reloadTypes() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            conceptTypes = new ArrayList<>();
            return;
        }
        List<ConceptType> types = conceptTypeRepository.findAllByIdThesaurusIn(List.of(thesaurusId, "all"));
        if (CollectionUtils.isEmpty(types)) {
            conceptTypes = new ArrayList<>();
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
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void flashSuccess(String message) {
        flashMessage = message;
        flashToken = String.valueOf(System.currentTimeMillis());
    }
}
