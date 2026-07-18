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

@Getter
@Setter
@ViewScoped
@Named("v2PropositionSubmitBean")
@RequiredArgsConstructor
public class PropositionSubmitBean implements Serializable {

    private static final List<String> NOTE_TYPE_CODES = List.of(
            "note", "definition", "scopeNote", "changeNote", "editorialNote", "example", "historyNote");

    private final PropositionMutationService propositionMutationService;
    private final PropositionDraftService propositionDraftService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptReadService conceptReadService;
    private final ConceptLexicalMutationService conceptLexicalMutationService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;

    private String authorName;
    private String authorEmail;
    private String comment;

    private String currentPreferredLabel;
    private String proposedPreferredLabel;

    private List<ConceptWriteLanguage> availableLanguages = new ArrayList<>();

    private List<PropositionSynonymOption> synonymOptions = new ArrayList<>();
    private String newSynonymLang;
    private String newSynonymValue;
    private boolean newSynonymHidden;

    private List<PropositionTranslationOption> translationOptions = new ArrayList<>();
    private String newTranslationLang;
    private String newTranslationValue;

    private List<PropositionNoteOption> noteOptions = new ArrayList<>();

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
        synonymOptions = new ArrayList<>();
        translationOptions = new ArrayList<>();
        noteOptions = new ArrayList<>();
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
    }

    private PropositionSynonymOption seedSynonym(String value, boolean hidden) {
        var option = new PropositionSynonymOption();
        option.setLang(conceptSelectionContext.getSummary().lang());
        option.setValue(value);
        option.setOldValue(value);
        option.setHidden(hidden);
        return option;
    }

    public void addSynonymOption() {
        if (StringUtils.isBlank(newSynonymValue)) {
            MessageUtils.showWarnMessage("Veuillez saisir une valeur");
            return;
        }
        var option = new PropositionSynonymOption();
        option.setLang(StringUtils.defaultIfBlank(newSynonymLang, conceptSelectionContext.getSummary().lang()));
        option.setValue(newSynonymValue.trim());
        option.setHidden(newSynonymHidden);
        option.setToAdd(true);
        synonymOptions.add(option);
        newSynonymValue = null;
        newSynonymHidden = false;
    }

    public void removeSynonymOption(PropositionSynonymOption option) {
        if (option.getOldValue() == null) {
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
        if (option.getOldValue() != null && !option.getOldValue().equals(option.getValue())) {
            option.setToUpdate(true);
        } else {
            option.setToUpdate(false);
        }
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
    }

    public void removeTranslationOption(PropositionTranslationOption option) {
        if (option.getOldValue() == null) {
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
        if (option.getOldValue() != null && !option.getOldValue().equals(option.getValue())) {
            option.setToUpdate(true);
        } else {
            option.setToUpdate(false);
        }
    }

    public boolean hasStructuredChanges() {
        boolean labelChanged = StringUtils.isNotBlank(proposedPreferredLabel)
                && !proposedPreferredLabel.trim().equals(StringUtils.defaultString(currentPreferredLabel).trim());
        boolean synonymChanged = synonymOptions.stream().anyMatch(o -> o.isToAdd() || o.isToUpdate() || o.isToRemove());
        boolean translationChanged = translationOptions.stream().anyMatch(o -> o.isToAdd() || o.isToUpdate() || o.isToRemove());
        boolean noteChanged = noteOptions.stream().anyMatch(PropositionNoteOption::hasChanged);
        return labelChanged || synonymChanged || translationChanged || noteChanged;
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
            return;
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
        PrimeFaces.current().executeScript("PF('v2SuggestImprovement').hide();");
    }
}
