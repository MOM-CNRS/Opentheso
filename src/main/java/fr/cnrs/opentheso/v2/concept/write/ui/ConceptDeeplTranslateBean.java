package fr.cnrs.opentheso.v2.concept.write.ui;

import com.deepl.api.Language;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.deepl.DeeplClient;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Traduction DeepL des notes concept — équivalent V2 de {@code deeplTranslate}.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptDeeplTranslateBean")
@RequiredArgsConstructor
public class ConceptDeeplTranslateBean implements Serializable {

    private final DeeplClient deeplClient;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;
    private final ConceptNoteMutationService conceptNoteMutationService;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;

    private String fromLang;
    private String fromLangLabel;
    private String textToTranslate;
    private String existingTranslatedText;
    private String sourceTranslatedText;
    private String translatingText;
    private String toLang = "en-GB";
    private String noteTypeCode;
    private String conceptId;
    private String conceptLabel;

    private List<Language> sourceLangs = Collections.emptyList();
    private List<Language> targetLangs = Collections.emptyList();

    public boolean isDeeplAvailable() {
        if (!conceptWritePolicy.canMutateLexicalContent(userSession, isSelectedDeprecated())) {
            return false;
        }
        var prefs = thesaurusPreferenceService.loadPreferencesOrNull(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
        return prefs != null && prefs.useDeeplTranslation() && StringUtils.isNotBlank(prefs.deeplApiKey());
    }

    /**
     * Initialise les langues DeepL puis charge la note source (comme legacy {@code init} + {@code initNoteToTranslateByLang}).
     */
    public void prepareFromNote(ConceptNote note) {
        clearTranslationFields();
        if (note == null || StringUtils.isAnyBlank(note.value(), note.lang(), note.typeCode())) {
            MessageUtils.showErrorMessage("Aucune note sélectionnée !");
            return;
        }
        if (!conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }

        String apiKey = resolveApiKey();
        if (StringUtils.isBlank(apiKey)) {
            MessageUtils.showErrorMessage("Clé API DeepL manquante");
            return;
        }

        sourceLangs = deeplClient.listSourceLanguages(apiKey);
        targetLangs = deeplClient.listTargetLanguages(apiKey);

        conceptId = conceptSelectionContext.getSummary().conceptId();
        conceptLabel = conceptSelectionContext.getSummary().preferredLabel();
        noteTypeCode = note.typeCode();
        textToTranslate = note.value();
        fromLang = note.lang();
        fromLangLabel = resolveLanguageLabel(fromLang);

        if (StringUtils.equalsIgnoreCase(fromLang, normalizeIdLang(toLang))) {
            toLang = "fr";
        }
        retrieveExistingTranslatedText();
    }

    public void translate() {
        String apiKey = resolveApiKey();
        if (StringUtils.isBlank(apiKey)) {
            MessageUtils.showErrorMessage("Clé API DeepL manquante");
            return;
        }
        if (StringUtils.isBlank(textToTranslate)) {
            MessageUtils.showWarnMessage("Aucun texte à traduire");
            return;
        }
        translatingText = deeplClient.translate(apiKey, textToTranslate, fromLang, toLang);
        if (StringUtils.isBlank(translatingText)) {
            MessageUtils.showErrorMessage("Échec de la traduction DeepL");
        }
    }

    public void retrieveExistingTranslatedText() {
        existingTranslatedText = null;
        sourceTranslatedText = null;
        if (StringUtils.isAnyBlank(conceptId, noteTypeCode, toLang)) {
            return;
        }
        var draft = conceptWriteMetadataService.loadNoteDraft(
                thesaurusContext.resolveThesaurusId(),
                conceptId,
                normalizeIdLang(toLang),
                noteTypeCode
        );
        if (draft.isEmpty()) {
            // DeepL peut renvoyer en-GB ; tenter aussi le code brut
            if (!normalizeIdLang(toLang).equalsIgnoreCase(toLang)) {
                draft = conceptWriteMetadataService.loadNoteDraft(
                        thesaurusContext.resolveThesaurusId(),
                        conceptId,
                        toLang,
                        noteTypeCode
                );
            }
        }
        draft.ifPresent(existing -> {
            existingTranslatedText = StringUtils.defaultString(existing.value());
            sourceTranslatedText = StringUtils.defaultString(existing.source());
        });
    }

    public void saveTranslatedText() {
        if (StringUtils.isBlank(translatingText)) {
            MessageUtils.showWarnMessage("Aucune traduction à enregistrer");
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            return;
        }
        String source = "traduit par Deepl le " + LocalDate.now();
        var result = conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptId,
                normalizeIdLang(toLang),
                noteTypeCode,
                translatingText,
                source,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        ));
        if (result != null && result.outcome() == MutationOutcome.OK) {
            conceptNavigationSupport.openConcept(conceptId);
            translatingText = "";
            retrieveExistingTranslatedText();
            PrimeFaces.current().ajax().update(":containerIndex:formRightTab", ":messageIndex");
            MessageUtils.showInformationMessage("Note ajoutée avec succès");
        } else if (result != null) {
            MessageUtils.showErrorMessage(result.message());
        }
    }

    public void saveExistingTranslatedText() {
        if (existingTranslatedText == null) {
            MessageUtils.showWarnMessage("Aucune traduction existante");
            return;
        }
        Integer userId = requireUserId();
        if (userId == null) {
            return;
        }
        var result = conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptId,
                normalizeIdLang(toLang),
                noteTypeCode,
                existingTranslatedText,
                StringUtils.defaultIfBlank(sourceTranslatedText, null),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        ));
        if (result != null && result.outcome() == MutationOutcome.OK) {
            conceptNavigationSupport.openConcept(conceptId);
            PrimeFaces.current().ajax().update(":containerIndex:formRightTab", ":messageIndex");
            MessageUtils.showInformationMessage("Note mis à jour avec succès");
        } else if (result != null) {
            MessageUtils.showErrorMessage(result.message());
        }
    }

    private void clearTranslationFields() {
        textToTranslate = null;
        existingTranslatedText = null;
        sourceTranslatedText = null;
        translatingText = null;
        fromLang = null;
        fromLangLabel = null;
        noteTypeCode = null;
        conceptId = null;
        conceptLabel = null;
    }

    private String resolveApiKey() {
        var prefs = thesaurusPreferenceService.loadPreferencesOrNull(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
        return prefs == null ? null : prefs.deeplApiKey();
    }

    private String resolveLanguageLabel(String idLang) {
        if (sourceLangs != null) {
            for (Language language : sourceLangs) {
                if (idLang != null && idLang.equalsIgnoreCase(language.getCode())) {
                    return language.getName();
                }
            }
        }
        return idLang;
    }

    private static String normalizeIdLang(String idLang) {
        if (idLang == null) {
            return null;
        }
        return switch (idLang) {
            case "en-GB", "en-US" -> "en";
            case "pt-BR", "pt-PT" -> "pt";
            default -> idLang;
        };
    }

    private Integer requireUserId() {
        if (!isDeeplAvailable()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return null;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
        }
        return userId;
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(StringUtils.trimToEmpty(conceptSelectionContext.getSummary().status()));
    }
}
