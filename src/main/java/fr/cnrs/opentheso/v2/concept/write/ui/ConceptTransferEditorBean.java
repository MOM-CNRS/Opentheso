package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptsToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptTransferMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptTransferEditorBean")
@RequiredArgsConstructor
public class ConceptTransferEditorBean implements Serializable {

    private final transient ConceptTransferMutationService conceptTransferMutationService;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ConceptNavigationSupport conceptNavigationSupport;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient BranchConceptSupport branchConceptSupport;
    private final transient ThesaurusViewBean thesaurusViewBean;

    private String sourceThesaurusLabel;
    private String sourceLabel;
    private String targetThesaurusId;
    private String destMode = "";
    private String parentConceptId;
    private String parentLabel;
    private String errorMessage;
    private String flashMessage;
    private String flashToken;
    private List<String> branchConceptIds = Collections.emptyList();
    @Getter(AccessLevel.NONE)
    private List<ConceptWriteThesaurusOption> availableThesauri = Collections.emptyList();
    private boolean thesauriLoaded;
    private String bulkTargetThesaurusId = "";
    private String bulkParentConceptId = "";
    private String bulkConceptIds = "";
    private String bulkXferMessage = "";
    private boolean bulkXferOk;

    public boolean isTransferActionsAvailable() {
        return conceptWritePolicy.canTransferConcept(userSession);
    }

    public List<ConceptWriteThesaurusOption> getAvailableThesauri() {
        ensureThesauriLoaded();
        return availableThesauri;
    }

    public String getAvailableThesauriJson() {
        ensureThesauriLoaded();
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (ConceptWriteThesaurusOption option : availableThesauri) {
            if (option == null || StringUtils.isBlank(option.id())) {
                continue;
            }
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append("{\"id\":").append(jsonString(option.id()))
                    .append(",\"name\":").append(jsonString(StringUtils.defaultIfBlank(option.title(), option.id())))
                    .append('}');
        }
        return json.append(']').toString();
    }

    public void submitMoveFromSelection() {
        bulkXferOk = false;
        bulkXferMessage = "";
        if (!isTransferActionsAvailable()) {
            bulkXferMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            bulkXferMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        List<String> conceptIds = parseBulkConceptIds(bulkConceptIds);
        if (conceptIds.isEmpty()) {
            bulkXferMessage = "Aucun concept à déplacer.";
            return;
        }
        String targetId = StringUtils.trimToEmpty(bulkTargetThesaurusId);
        if (targetId.isEmpty()) {
            bulkXferMessage = "Choisissez un thésaurus de destination";
            return;
        }
        String parentId = StringUtils.trimToEmpty(bulkParentConceptId);
        if (parentId.isEmpty()) {
            bulkXferMessage = "Choisissez un emplacement";
            return;
        }
        String newParent = "__root".equalsIgnoreCase(parentId) ? null : parentId;
        MutationResult result = conceptTransferMutationService.moveConceptsToThesaurus(
                new MoveConceptsToThesaurusCommand(
                        thesaurusContext.resolveThesaurusId(),
                        targetId,
                        conceptIds,
                        thesaurusContext.resolveWorkLanguage(),
                        userId,
                        StringUtils.defaultString(userSession.getCurrentUsername()),
                        newParent
                )
        );
        if (result == null || !result.success()) {
            bulkXferMessage = result != null ? result.message() : "Le déplacement a échoué";
            return;
        }
        conceptNavigationSupport.invalidateConceptTree();
        thesaurusViewBean.reloadTree();
        bulkXferOk = true;
        bulkXferMessage = result.message();
        bulkTargetThesaurusId = "";
        bulkParentConceptId = "";
        bulkConceptIds = "";
    }

    public boolean isTargetThesaurusSelected() {
        return StringUtils.isNotBlank(targetThesaurusId);
    }

    public boolean isParentSelected() {
        return StringUtils.isNotBlank(parentConceptId);
    }

    public boolean isSubmitReady() {
        if (!isTargetThesaurusSelected()) {
            return false;
        }
        if ("root".equals(destMode)) {
            return true;
        }
        return "parent".equals(destMode) && isParentSelected();
    }

    public String getTargetThesaurusLabel() {
        if (StringUtils.isBlank(targetThesaurusId)) {
            return "";
        }
        return availableThesauri.stream()
                .filter(th -> targetThesaurusId.equalsIgnoreCase(th.id()))
                .map(th -> StringUtils.defaultIfBlank(th.title(), th.id()) + " (" + th.id() + ")")
                .findFirst()
                .orElse(targetThesaurusId);
    }

    public void prepareMoveToAnotherThesaurus() {
        errorMessage = null;
        destMode = "";
        parentConceptId = "";
        parentLabel = "";
        targetThesaurusId = null;
        if (!conceptSelectionContext.hasSelection()) {
            branchConceptIds = Collections.emptyList();
            availableThesauri = Collections.emptyList();
            sourceLabel = "";
            sourceThesaurusLabel = "";
            return;
        }
        sourceThesaurusLabel = StringUtils.defaultIfBlank(
                thesaurusContext.getCurrentThesaurusTitle(),
                thesaurusContext.resolveThesaurusId());
        sourceLabel = conceptSelectionContext.getSummary().preferredLabel();
        branchConceptIds = branchConceptSupport.collectBranchConceptIds(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId());
        loadAvailableThesauri();
        thesauriLoaded = true;
    }

    public void onTargetThesaurusChange() {
        destMode = "";
        parentConceptId = "";
        parentLabel = "";
        errorMessage = null;
    }

    public boolean submitMoveToAnotherThesaurus() {
        errorMessage = null;
        if (!isTransferActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return false;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || StringUtils.isBlank(targetThesaurusId) || branchConceptIds.isEmpty()) {
            errorMessage = "Choisissez un thésaurus de destination";
            return false;
        }
        if (!"root".equals(destMode) && !"parent".equals(destMode)) {
            errorMessage = "Choisissez un emplacement";
            return false;
        }
        boolean toRoot = "root".equals(destMode);
        String parentId = toRoot ? null : parentConceptId;
        if (!toRoot && StringUtils.isBlank(parentId)) {
            errorMessage = "Choisissez un concept parent, ou la racine";
            return false;
        }
        var command = new MoveConceptToThesaurusCommand(
                thesaurusContext.resolveThesaurusId(),
                targetThesaurusId,
                conceptSelectionContext.getConceptId(),
                branchConceptIds,
                thesaurusContext.resolveWorkLanguage(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()),
                parentId
        );
        try {
            MutationResult result = conceptTransferMutationService.moveConceptToThesaurus(command);
            if (result == null || !result.success()) {
                errorMessage = result != null ? result.message() : "Le déplacement a échoué";
                return false;
            }
            conceptNavigationSupport.invalidateConceptTree();
            conceptNavigationSupport.openThesaurusHome();
            flashSuccess(StringUtils.defaultIfBlank(result.message(),
                    sourceLabel + " → " + getTargetThesaurusLabel()));
            return true;
        } catch (RuntimeException exception) {
            errorMessage = "Le déplacement a échoué";
            return false;
        }
    }

    private void ensureThesauriLoaded() {
        if (thesauriLoaded) {
            return;
        }
        loadAvailableThesauri();
        thesauriLoaded = true;
    }

    private void loadAvailableThesauri() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            availableThesauri = Collections.emptyList();
            return;
        }
        availableThesauri = conceptTransferMutationService.listAdminThesauri(
                userId,
                userSession.isSuperAdmin(),
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    private static List<String> parseBulkConceptIds(String raw) {
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String part : raw.split("[\\n\\r\\t,;]+")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private static String jsonString(String value) {
        String raw = StringUtils.defaultString(value);
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '<' -> out.append("\\u003c");
                case '>' -> out.append("\\u003e");
                case '&' -> out.append("\\u0026");
                default -> out.append(c);
            }
        }
        return out.append('"').toString();
    }

    private void flashSuccess(String message) {
        flashMessage = message;
        flashToken = String.valueOf(System.currentTimeMillis());
    }
}
