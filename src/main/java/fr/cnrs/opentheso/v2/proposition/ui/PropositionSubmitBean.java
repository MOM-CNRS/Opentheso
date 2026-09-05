package fr.cnrs.opentheso.v2.proposition.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraftMapper;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionNoteOption;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSubmission;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSynonymOption;
import fr.cnrs.opentheso.v2.proposition.model.PropositionTranslationOption;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ViewScoped
@Named("v2PropositionSubmitBean")
@RequiredArgsConstructor
public class PropositionSubmitBean implements Serializable {

    private static final List<String> NOTE_TYPE_CODES = List.of(
            "note", "definition", "scopeNote", "changeNote", "editorialNote", "example", "historyNote");

    private final transient PropositionMutationService propositionMutationService;
    private final transient PropositionDraftService propositionDraftService;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ConceptReadService conceptReadService;
    private final transient ConceptLexicalMutationService conceptLexicalMutationService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;

    private String authorName;
    private String authorEmail;
    private String comment;

    private String currentPreferredLabel;
    private String proposedPreferredLabel;
    private String renameDraftLabel;

    private List<ConceptWriteLanguage> availableLanguages = new ArrayList<>();

    private List<PropositionSynonymOption> synonymOptions = new ArrayList<>();
    private String newSynonymLang;
    private String newSynonymValue;
    private boolean newSynonymHidden;

    private List<PropositionTranslationOption> translationOptions = new ArrayList<>();
    private String newTranslationLang;
    private String newTranslationValue;

    private List<PropositionNoteOption> noteOptions = new ArrayList<>();
    private PropositionNoteOption noteBeingEdited;
    private String newNoteTypeCode;
    private String newNoteLang;
    private String newNoteValue;

    public void prepare() {
        comment = "";
        if (userSession.isLoggedIn()) {
            authorName = userSession.getCurrentUsername();
            authorEmail = userSession.getCurrentUserEmail();
        } else {
            authorName = "";
            authorEmail = "";
        }

        proposedPreferredLabel = null;
        renameDraftLabel = null;
        synonymOptions = new ArrayList<>();
        translationOptions = new ArrayList<>();
        noteOptions = new ArrayList<>();
        noteBeingEdited = null;
        newNoteTypeCode = "note";
        newNoteLang = null;
        newNoteValue = null;
        availableLanguages = conceptLexicalMutationService.listUsedLanguages(
                thesaurusContext.resolveThesaurusId(), thesaurusContext.resolveWorkLanguage());

        if (!conceptSelectionContext.hasSelection()) {
            currentPreferredLabel = null;
            return;
        }

        ConceptDetail detail = conceptReadService.loadDetail(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                conceptSelectionContext.getSummary().lang()
        ).orElse(null);

        if (detail == null) {
            currentPreferredLabel = conceptSelectionContext.getSummary().preferredLabel();
            proposedPreferredLabel = currentPreferredLabel;
            return;
        }

        currentPreferredLabel = detail.summary().preferredLabel();
        proposedPreferredLabel = currentPreferredLabel;

        detail.synonyms().forEach(value -> synonymOptions.add(seedSynonym(value, false)));
        detail.hiddenSynonyms().forEach(value -> synonymOptions.add(seedSynonym(value, true)));

        detail.translations().stream()
                .filter(label -> !label.isPreferred())
                .forEach(label -> {
                    var option = new PropositionTranslationOption();
                    option.setLang(label.getLang());
                    option.setValue(label.getValue());
                    option.setOldValue(label.getValue());
                    translationOptions.add(option);
                });

        for (String typeCode : NOTE_TYPE_CODES) {
            var option = new PropositionNoteOption();
            option.setTypeCode(typeCode);
            option.setLabel(PropositionFieldCategory.forNoteType(typeCode).name());
            String existingValue = detail.notes().stream()
                    .filter(note -> typeCode.equals(note.typeCode()))
                    .map(note -> note.value())
                    .findFirst()
                    .orElse(null);
            option.setValue(existingValue);
            option.setOldValue(existingValue);
            noteOptions.add(option);
        }

        if (!availableLanguages.isEmpty()) {
            newSynonymLang = availableLanguages.get(0).code();
            newTranslationLang = availableLanguages.get(0).code();
            newNoteLang = conceptSelectionContext.getSummary().lang();
        }
    }

    public boolean isPreferredLabelChanged() {
        return StringUtils.isNotBlank(proposedPreferredLabel)
                && !proposedPreferredLabel.trim().equals(StringUtils.defaultString(currentPreferredLabel).trim());
    }

    public boolean isAuthorNameEditable() {
        return !userSession.isLoggedIn() || StringUtils.isBlank(userSession.getCurrentUsername());
    }

    public boolean isAuthorEmailEditable() {
        return !userSession.isLoggedIn() || StringUtils.isBlank(userSession.getCurrentUserEmail());
    }

    public boolean isSynonymMenuDisabled() {
        return synonymOptions == null || synonymOptions.isEmpty();
    }

    public boolean isTranslationMenuDisabled() {
        return translationOptions == null || translationOptions.isEmpty();
    }

    public List<ConceptWriteLanguage> getLanguagesWithoutTranslation() {
        Set<String> used = translationOptions.stream()
                .filter(option -> !option.isToRemove())
                .map(PropositionTranslationOption::getLang)
                .filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return availableLanguages.stream()
                .filter(lang -> !used.contains(StringUtils.defaultString(lang.code()).toLowerCase()))
                .toList();
    }

    public List<PropositionNoteOption> getVisibleNoteOptions() {
        return noteOptions.stream()
                .filter(option -> StringUtils.isNotBlank(option.getValue())
                        || StringUtils.isNotBlank(option.getOldValue())
                        || option.hasChanged())
                .toList();
    }

    public void prepareRenamePreferredLabel() {
        renameDraftLabel = isPreferredLabelChanged() ? proposedPreferredLabel : currentPreferredLabel;
    }

    public void confirmRenamePreferredLabel() {
        if (StringUtils.isBlank(renameDraftLabel)) {
            proposedPreferredLabel = currentPreferredLabel;
            MessageUtils.showErrorMessage("Le libellé ne peut pas être vide");
            return;
        }
        proposedPreferredLabel = renameDraftLabel.trim();
        PrimeFaces.current().executeScript("PF('v2PropRenameLabel').hide();");
    }

    public void prepareAddSynonym() {
        newSynonymValue = "";
        newSynonymHidden = false;
        if (StringUtils.isBlank(newSynonymLang) && !availableLanguages.isEmpty()) {
            newSynonymLang = availableLanguages.get(0).code();
        }
    }

    public void prepareAddTranslation() {
        newTranslationValue = "";
        List<ConceptWriteLanguage> free = getLanguagesWithoutTranslation();
        newTranslationLang = free.isEmpty() ? null : free.get(0).code();
    }

    public void prepareAddNote() {
        newNoteTypeCode = "note";
        newNoteValue = "";
        newNoteLang = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().lang()
                : (availableLanguages.isEmpty() ? null : availableLanguages.get(0).code());
    }

    public void prepareEditNote(PropositionNoteOption option) {
        noteBeingEdited = option;
    }

    public void confirmEditNote() {
        noteBeingEdited = null;
        PrimeFaces.current().executeScript("PF('v2PropEditNote').hide();");
    }

    public void clearNote(PropositionNoteOption option) {
        if (option == null) {
            return;
        }
        option.setValue(null);
    }

    public void addNoteOption() {
        if (StringUtils.isBlank(newNoteValue) || StringUtils.isBlank(newNoteTypeCode)) {
            MessageUtils.showWarnMessage("Veuillez saisir une note");
            return;
        }
        PropositionNoteOption target = noteOptions.stream()
                .filter(option -> newNoteTypeCode.equals(option.getTypeCode()))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return;
        }
        if (StringUtils.isNotBlank(target.getValue())) {
            MessageUtils.showWarnMessage("Une note de ce type existe déjà — utilisez Modifier");
            return;
        }
        target.setValue(newNoteValue.trim());
        newNoteValue = null;
        PrimeFaces.current().executeScript("PF('v2PropAddNote').hide();");
    }

    private PropositionSynonymOption seedSynonym(String value, boolean hidden) {
        var option = new PropositionSynonymOption();
        option.setLang(conceptSelectionContext.getSummary().lang());
        option.setValue(value);
        option.setOldValue(value);
        option.setHidden(hidden);
        option.setOldHidden(hidden);
        return option;
    }

    public void addSynonymOption() {
        if (StringUtils.isBlank(newSynonymValue)) {
            MessageUtils.showWarnMessage("Veuillez saisir une valeur");
            return;
        }
        String lang = StringUtils.defaultIfBlank(newSynonymLang, conceptSelectionContext.getSummary().lang());
        String value = newSynonymValue.trim();
        boolean duplicate = synonymOptions.stream().anyMatch(option ->
                !option.isToRemove()
                        && StringUtils.equalsIgnoreCase(option.getLang(), lang)
                        && StringUtils.equalsIgnoreCase(option.getValue(), value));
        if (duplicate) {
            MessageUtils.showWarnMessage("Cette variante existe déjà");
            return;
        }
        var option = new PropositionSynonymOption();
        option.setLang(lang);
        option.setValue(value);
        option.setHidden(newSynonymHidden);
        option.setToAdd(true);
        synonymOptions.add(option);
        newSynonymValue = null;
        newSynonymHidden = false;
        PrimeFaces.current().executeScript("PF('v2PropAddSynonym').hide();");
    }

    public void applySynonymEdit(PropositionSynonymOption option) {
        if (option == null) {
            return;
        }
        if (option.isToAdd() || option.getOldValue() == null) {
            PrimeFaces.current().executeScript("PF('v2PropRenameSynonym').hide();");
            return;
        }
        boolean valueChanged = !StringUtils.equals(
                StringUtils.defaultString(option.getOldValue()).trim(),
                StringUtils.defaultString(option.getValue()).trim());
        boolean hiddenChanged = option.isHidden() != option.isOldHidden();
        option.setToUpdate(valueChanged || hiddenChanged);
        option.setToRemove(false);
        PrimeFaces.current().executeScript("PF('v2PropRenameSynonym').hide();");
    }

    public void removeSynonymOption(PropositionSynonymOption option) {
        if (option.getOldValue() == null || option.isToAdd()) {
            synonymOptions.remove(option);
        } else {
            option.setToRemove(true);
            option.setToUpdate(false);
        }
    }

    public void undoRemoveSynonymOption(PropositionSynonymOption option) {
        option.setToRemove(false);
    }

    public void markSynonymUpdated(PropositionSynonymOption option) {
        applySynonymEdit(option);
    }

    public void addTranslationOption() {
        if (StringUtils.isBlank(newTranslationLang) || StringUtils.isBlank(newTranslationValue)) {
            MessageUtils.showWarnMessage("Veuillez choisir une langue et saisir une valeur");
            return;
        }
        var option = new PropositionTranslationOption();
        option.setLang(newTranslationLang);
        option.setValue(newTranslationValue.trim());
        option.setToAdd(true);
        translationOptions.add(option);
        newTranslationLang = null;
        newTranslationValue = null;
        PrimeFaces.current().executeScript("PF('v2PropAddTraduction').hide();");
    }

    public void applyTranslationEdit(PropositionTranslationOption option) {
        if (option == null) {
            return;
        }
        if (option.isToAdd() || option.getOldValue() == null) {
            PrimeFaces.current().executeScript("PF('v2PropRenameTraduction').hide();");
            return;
        }
        boolean valueChanged = !StringUtils.equals(
                StringUtils.defaultString(option.getOldValue()).trim(),
                StringUtils.defaultString(option.getValue()).trim());
        option.setToUpdate(valueChanged);
        option.setToRemove(false);
        PrimeFaces.current().executeScript("PF('v2PropRenameTraduction').hide();");
    }

    public void removeTranslationOption(PropositionTranslationOption option) {
        if (option.getOldValue() == null || option.isToAdd()) {
            translationOptions.remove(option);
        } else {
            option.setToRemove(true);
            option.setToUpdate(false);
        }
    }

    public void undoRemoveTranslationOption(PropositionTranslationOption option) {
        option.setToRemove(false);
    }

    public void markTranslationUpdated(PropositionTranslationOption option) {
        applyTranslationEdit(option);
    }

    public boolean hasStructuredChanges() {
        boolean labelChanged = isPreferredLabelChanged();
        boolean synonymChanged = synonymOptions.stream().anyMatch(o -> o.isToAdd() || o.isToUpdate() || o.isToRemove());
        boolean translationChanged = translationOptions.stream().anyMatch(o -> o.isToAdd() || o.isToUpdate() || o.isToRemove());
        boolean noteChanged = noteOptions.stream().anyMatch(PropositionNoteOption::hasChanged);
        return labelChanged || synonymChanged || translationChanged || noteChanged;
    }

    public boolean submit() {
        if (!conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Aucun concept sélectionné");
            return false;
        }
        var summary = conceptSelectionContext.getSummary();
        if (StringUtils.isBlank(authorName)) {
            MessageUtils.showWarnMessage("Veuillez saisir votre nom");
            return false;
        }
        if (StringUtils.isBlank(authorEmail)) {
            MessageUtils.showWarnMessage("Veuillez saisir votre adresse email");
            return false;
        }
        if (StringUtils.isBlank(comment)) {
            MessageUtils.showWarnMessage("Veuillez saisir votre proposition");
            return false;
        }

        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String lang = summary.lang();

        var submission = new PropositionSubmission(
                thesaurusId,
                thesaurusContext.getCurrentThesaurusTitle(),
                summary.conceptId(),
                summary.preferredLabel(),
                lang,
                authorName,
                authorEmail,
                StringUtils.defaultString(comment)
        );

        var createdId = propositionMutationService.submitDraft(submission);
        if (createdId.isEmpty()) {
            MessageUtils.showWarnMessage("Vous avez déjà une proposition en cours pour ce concept");
            return false;
        }

        var draft = PropositionDraftMapper.toDraft(
                summary.conceptId(),
                thesaurusId,
                lang,
                currentPreferredLabel,
                proposedPreferredLabel,
                synonymOptions,
                translationOptions,
                noteOptions
        );
        propositionDraftService.saveDraftDetails(createdId.get(), draft);

        comment = "";
        MessageUtils.showInformationMessage("Votre proposition a bien été envoyée");
        return true;
    }
}
