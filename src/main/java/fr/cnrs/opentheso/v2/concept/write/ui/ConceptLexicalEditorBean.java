package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.mapper.ConceptMapper;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
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
import java.util.stream.Collectors;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptLexicalEditorBean")
@RequiredArgsConstructor
public class ConceptLexicalEditorBean implements Serializable {

    private final transient ConceptLexicalMutationService conceptLexicalMutationService;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ConceptNavigationSupport conceptNavigationSupport;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient ConceptReadService conceptReadService;
    private final transient ConceptWriteMetadataService conceptWriteMetadataService;

    private String currentConceptLabel;
    private String synonymValue;
    private String selectedLang;
    private boolean synonymHidden;
    private boolean duplicateLabelWarning;
    private SynonymEditRow pendingSynonymEditRow;
    private List<ConceptWriteLanguage> availableLanguages = Collections.emptyList();
    private List<ConceptWriteLanguage> availableTranslationLanguages = Collections.emptyList();
    private String translationValue;
    private String translationLang;
    private List<SynonymEditRow> synonymEdits = Collections.emptyList();
    private List<TranslationEditRow> translationEdits = Collections.emptyList();

    public boolean isLexicalActionsAvailable() {
        return conceptWritePolicy.canMutateLexicalContent(userSession, isSelectedDeprecated());
    }

    public void prepareAddSynonym() {
        resetSynonymForm();
        loadLanguages();
        selectedLang = thesaurusContext.resolveWorkLanguage();
    }

    public void prepareEditSynonyms() {
        refreshCurrentConceptLabel();
        duplicateLabelWarning = false;
        pendingSynonymEditRow = null;
        synonymEdits = loadSynonymEdits();
    }

    public void prepareDeleteSynonyms() {
        refreshCurrentConceptLabel();
        synonymEdits = loadSynonymEdits();
    }

    public void prepareAddTranslation() {
        refreshCurrentConceptLabel();
        translationValue = "";
        loadLanguages();
        loadAvailableTranslationLanguages();
        translationLang = availableTranslationLanguages.isEmpty() ? null : availableTranslationLanguages.get(0).code();
    }

    public void prepareEditTranslations() {
        refreshCurrentConceptLabel();
        translationEdits = loadTranslationEdits();
    }

    public void prepareDeleteTranslations() {
        prepareEditTranslations();
    }

    public void submitAddSynonym() {
        submitAddSynonymInternal(false);
    }

    public void submitAddSynonymForced() {
        submitAddSynonymInternal(true);
    }

    public void submitUpdateSynonym(SynonymEditRow row) {
        submitUpdateSynonymInternal(row, false);
    }

    public void submitUpdateSynonymForced(SynonymEditRow row) {
        submitUpdateSynonymInternal(row, true);
    }

    public void submitDeleteSynonym(SynonymEditRow row) {
        Integer userId = requireUserId();
        if (userId == null || row == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        if (submitMutation(conceptLexicalMutationService.deleteSynonym(new DeleteSynonymCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                row.getLang(),
                row.getOldValue(),
                userId,
                contributorName()
        )), null)) {
            synonymEdits = loadSynonymEdits();
            PrimeFaces.current().executeScript("PF('v2DeleteSynonymDlg').show();");
        }
    }

    public void submitAddTranslation() {
        Integer userId = requireUserId();
        if (userId == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        submitMutation(conceptLexicalMutationService.addTranslation(new AddTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                translationLang,
                translationValue,
                userId,
                contributorName()
        )), "PF('v2AddTranslationDlg').hide();");
    }

    public void submitUpdateTranslation(TranslationEditRow row) {
        Integer userId = requireUserId();
        if (userId == null || row == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        submitMutation(conceptLexicalMutationService.updateTranslation(new UpdateTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                row.getLang(),
                row.getValue(),
                userId,
                contributorName()
        )), null);
        translationEdits = loadTranslationEdits();
    }

    public void submitDeleteTranslation(String lang) {
        Integer userId = requireUserId();
        if (userId == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        submitMutation(conceptLexicalMutationService.deleteTranslation(new DeleteTranslationCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                lang,
                userId,
                contributorName()
        )), null);
        translationEdits = loadTranslationEdits();
    }

    public void cancelDuplicate() {
        duplicateLabelWarning = false;
        pendingSynonymEditRow = null;
    }

    private void submitAddSynonymInternal(boolean forced) {
        Integer userId = requireUserId();
        if (userId == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        var command = new AddSynonymCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                selectedLang,
                synonymValue,
                synonymHidden,
                userId,
                contributorName(),
                forced
        );
        MutationResult result = conceptLexicalMutationService.addSynonym(command);
        if (result.outcome() == MutationOutcome.DUPLICATE_LABEL) {
            duplicateLabelWarning = true;
            MessageUtils.showWarnMessage(result.message());
            return;
        }
        duplicateLabelWarning = false;
        // Comme legacy : garder le dialogue ouvert et réinitialiser le formulaire
        if (submitMutation(result, null)) {
            resetSynonymForm();
            PrimeFaces.current().executeScript("PF('v2AddSynonymDlg').show();");
        }
    }

    private void submitUpdateSynonymInternal(SynonymEditRow row, boolean forced) {
        Integer userId = requireUserId();
        if (userId == null || row == null) {
            return;
        }
        var summary = requireSummary();
        if (summary == null) {
            return;
        }
        var command = new UpdateSynonymCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                row.getLang(),
                row.getOldValue(),
                row.getValue(),
                row.isHidden(),
                userId,
                contributorName(),
                forced
        );
        MutationResult result = conceptLexicalMutationService.updateSynonym(command);
        if (result.outcome() == MutationOutcome.DUPLICATE_LABEL) {
            duplicateLabelWarning = true;
            pendingSynonymEditRow = row;
            MessageUtils.showWarnMessage(result.message());
            PrimeFaces.current().ajax().update(":v2EditSynonymForm");
            return;
        }
        duplicateLabelWarning = false;
        pendingSynonymEditRow = null;
        if (submitMutation(result, null)) {
            synonymEdits = loadSynonymEdits();
            PrimeFaces.current().executeScript("PF('v2EditSynonymDlg').show();");
        }
    }

    public void submitUpdateSynonymForcedFromEdit() {
        if (pendingSynonymEditRow != null) {
            submitUpdateSynonymInternal(pendingSynonymEditRow, true);
        }
    }

    private boolean submitMutation(MutationResult result, String hideDialogScript) {
        if (result == null) {
            return false;
        }
        switch (result.outcome()) {
            case OK -> {
                conceptNavigationSupport.openConcept(conceptSelectionContext.getSummary().conceptId());
                PrimeFaces.current().ajax().update(":containerIndex:formRightTab :messageIndex");
                MessageUtils.showInformationMessage(result.message());
                if (StringUtils.isNotBlank(hideDialogScript)) {
                    PrimeFaces.current().executeScript(hideDialogScript);
                }
                return true;
            }
            case VALIDATION_ERROR, FAILURE, FORBIDDEN, DUPLICATE_LABEL -> {
                MessageUtils.showErrorMessage(result.message());
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private List<SynonymEditRow> loadSynonymEdits() {
        if (!conceptSelectionContext.hasSelection()) {
            return Collections.emptyList();
        }
        var summary = conceptSelectionContext.getSummary();
        return conceptReadService.loadDetailWithSource(
                        thesaurusContext.resolveThesaurusId(),
                        summary.conceptId(),
                        thesaurusContext.resolveWorkLanguage())
                .map(result -> ConceptMapper.mapSynonymLabels(
                        result.fullConcept(), thesaurusContext.resolveWorkLanguage(), true))
                .map(labels -> labels.stream()
                        .map(label -> new SynonymEditRow(label.lang(), label.value(), label.hidden()))
                        .toList())
                .orElseGet(Collections::emptyList);
    }

    private List<TranslationEditRow> loadTranslationEdits() {
        if (!conceptSelectionContext.hasSelection()) {
            return Collections.emptyList();
        }
        var summary = conceptSelectionContext.getSummary();
        return conceptReadService.loadDetail(
                        thesaurusContext.resolveThesaurusId(),
                        summary.conceptId(),
                        thesaurusContext.resolveWorkLanguage())
                .map(detail -> detail.translations().stream()
                        .filter(ConceptLabel::preferred)
                        .map(label -> new TranslationEditRow(label.lang(), label.value()))
                        .toList())
                .orElseGet(Collections::emptyList);
    }

    private void loadLanguages() {
        availableLanguages = conceptWriteMetadataService.listUsedLanguages(
                thesaurusContext.resolveThesaurusId(), thesaurusContext.resolveWorkLanguage());
    }

    private void loadAvailableTranslationLanguages() {
        loadLanguages();
        var usedLangs = loadTranslationEdits().stream()
                .map(TranslationEditRow::getLang)
                .collect(Collectors.toSet());
        usedLangs.add(thesaurusContext.resolveWorkLanguage());
        availableTranslationLanguages = availableLanguages.stream()
                .filter(lang -> !usedLangs.contains(lang.code()))
                .toList();
    }

    private void resetSynonymForm() {
        refreshCurrentConceptLabel();
        synonymValue = "";
        synonymHidden = false;
        duplicateLabelWarning = false;
        loadLanguages();
    }

    private void refreshCurrentConceptLabel() {
        currentConceptLabel = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().preferredLabel()
                : "";
    }

    private Integer requireUserId() {
        if (!isLexicalActionsAvailable()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return null;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
        }
        return userId;
    }

    private fr.cnrs.opentheso.v2.concept.model.ConceptSummary requireSummary() {
        if (!conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return null;
        }
        return conceptSelectionContext.getSummary();
    }

    private String contributorName() {
        return StringUtils.defaultString(userSession.getCurrentUsername());
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(StringUtils.trimToEmpty(conceptSelectionContext.getSummary().status()));
    }
}
