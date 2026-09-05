package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.entites.ConceptType;
import fr.cnrs.opentheso.models.concept.NodeConceptType;
import fr.cnrs.opentheso.repositories.ConceptTypeRepository;
import fr.cnrs.opentheso.services.ConceptTypeService;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
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

    private final transient ConceptTypeService conceptTypeService;
    private final transient ConceptTypeRepository conceptTypeRepository;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient ThesaurusBrowseBean thesaurusBrowseBean;
    private final transient V2LocaleBean v2LocaleBean;

    private final DialogRunState run = new DialogRunState();

    private List<NodeConceptType> conceptTypes = new ArrayList<>();
    private NodeConceptType conceptTypeToAdd = new NodeConceptType();
    private NodeConceptType conceptTypeToDelete;
    private boolean dirty;

    public String getErrorMessage() {
        return run.getErrorMessage();
    }

    public String getFlashMessage() {
        return run.getFlashMessage();
    }

    public String getFlashToken() {
        return run.getFlashToken();
    }

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
        run.reset();
        dirty = false;
        conceptTypeToDelete = null;
        conceptTypeToAdd = new NodeConceptType();
        if (!isManageAvailable()) {
            run.setErrorMessage(unauthorized());
            conceptTypes = new ArrayList<>();
            return;
        }
        reloadTypes();
    }

    public void applyChange(NodeConceptType nodeConceptType) {
        run.setErrorMessage(null);
        if (!isManageAvailable() || nodeConceptType == null || StringUtils.isBlank(nodeConceptType.getCode())) {
            run.setErrorMessage(unauthorized());
            return;
        }
        if (nodeConceptType.isPermanent()) {
            run.setErrorMessage(msg("v2.type.systemLocked", "Ce type système n'est pas modifiable"));
            return;
        }
        if (StringUtils.isAllBlank(nodeConceptType.getLabelFr(), nodeConceptType.getLabelEn())) {
            run.setErrorMessage(msg("v2.type.labelRequired", "Indiquez au moins un libellé"));
            return;
        }
        conceptTypeToDelete = null;
        if (!conceptTypeService.updateConceptType(thesaurusContext.resolveThesaurusId(), nodeConceptType)) {
            run.setErrorMessage(msg("v2.type.updateFailed", "La mise à jour a échoué"));
            return;
        }
        dirty = true;
        run.flash(msg("v2.type.saved", "Type « {0} » enregistré", nodeConceptType.getCode()));
        reloadTypes();
    }

    public void prepareDelete(NodeConceptType nodeConceptType) {
        run.setErrorMessage(null);
        if (nodeConceptType == null || nodeConceptType.isPermanent()) {
            run.setErrorMessage(msg("v2.type.notDeletable", "Ce type ne peut pas être supprimé"));
            conceptTypeToDelete = null;
            return;
        }
        conceptTypeToDelete = nodeConceptType;
    }

    public void cancelDelete() {
        conceptTypeToDelete = null;
        run.setErrorMessage(null);
    }

    public void deleteCustomRelationship() {
        run.setErrorMessage(null);
        if (!isManageAvailable() || conceptTypeToDelete == null || StringUtils.isBlank(conceptTypeToDelete.getCode())) {
            run.setErrorMessage(unauthorized());
            return;
        }
        if (conceptTypeToDelete.isPermanent()) {
            run.setErrorMessage(msg("v2.type.systemNotDeletable", "Ce type système n'est pas supprimable"));
            return;
        }
        String code = conceptTypeToDelete.getCode();
        conceptTypeService.deleteConceptType(thesaurusContext.resolveThesaurusId(), conceptTypeToDelete);
        conceptTypeToDelete = null;
        dirty = true;
        run.flash(msg("v2.type.deleted", "Type « {0} » supprimé", code));
        reloadTypes();
    }

    public void addNewConceptType() {
        run.setErrorMessage(null);
        if (!isManageAvailable() || conceptTypeToAdd == null) {
            run.setErrorMessage(unauthorized());
            return;
        }
        if (StringUtils.isBlank(conceptTypeToAdd.getCode())) {
            run.setErrorMessage(msg("v2.type.codeRequired", "Le code est obligatoire"));
            return;
        }
        String code = fr.cnrs.opentheso.utils.StringUtils.unaccentLowerString(conceptTypeToAdd.getCode())
                .replace(" ", "");
        conceptTypeToAdd.setCode(code);
        if (StringUtils.isBlank(code)) {
            run.setErrorMessage(msg("v2.type.codeRequired", "Le code est obligatoire"));
            return;
        }
        if (StringUtils.isAllBlank(conceptTypeToAdd.getLabelFr(), conceptTypeToAdd.getLabelEn())) {
            run.setErrorMessage(msg("v2.type.labelRequired", "Indiquez au moins un libellé"));
            return;
        }
        if (codeExists(code)) {
            run.setErrorMessage(msg("v2.type.exists", "Le type « {0} » existe déjà", code));
            return;
        }
        conceptTypeToDelete = null;
        conceptTypeService.addNewConceptType(thesaurusContext.resolveThesaurusId(), conceptTypeToAdd);
        conceptTypeToAdd = new NodeConceptType();
        dirty = true;
        run.flash(msg("v2.type.added", "Type « {0} » ajouté", code));
        reloadTypes();
    }

    public void finishAfterClose() {
        run.reset();
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


    private String unauthorized() {
        return WriteUiMessages.unauthorized(v2LocaleBean);
    }

    private String msg(String key, String fallback) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback);
    }

    private String msg(String key, String fallback, Object... args) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback, args);
    }
}
