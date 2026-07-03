package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelection;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;

@Getter
@Setter
@SessionScoped
@Named("v2ThesaurusContext")
public class ThesaurusContext implements Serializable {

    private final ThesaurusSelectionService thesaurusSelectionService;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    private String idConceptFromUri;
    private String idGroupFromUri;
    private String idThesoFromUri;

    private String currentThesaurusId;
    private String currentThesaurusTitle;
    private String currentLanguage;

    private boolean fromUrl;

    public ThesaurusContext(
            ThesaurusSelectionService thesaurusSelectionService,
            ThesaurusWorkLanguageService thesaurusWorkLanguageService
    ) {
        this.thesaurusSelectionService = thesaurusSelectionService;
        this.thesaurusWorkLanguageService = thesaurusWorkLanguageService;
    }

    public void syncFromViewParams() {
        if (StringUtils.isNotBlank(idThesoFromUri)) {
            fromUrl = true;
            applyThesaurus(idThesoFromUri.trim());
            idThesoFromUri = null;
            idConceptFromUri = null;
            idGroupFromUri = null;
            return;
        }
        fromUrl = false;
    }

    public String resolveThesaurusId() {
        return StringUtils.isNotBlank(currentThesaurusId) ? currentThesaurusId : null;
    }

    public String resolveWorkLanguage() {
        if (StringUtils.isNotBlank(currentLanguage)) {
            return currentLanguage;
        }
        return thesaurusWorkLanguageService.resolveForThesaurus(resolveThesaurusId());
    }

    public boolean matchesCurrentThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId)
                && thesaurusId.equalsIgnoreCase(resolveThesaurusId());
    }

    public void clearSelection() {
        currentThesaurusId = null;
        currentThesaurusTitle = null;
        currentLanguage = null;
        fromUrl = false;
    }

    public void changeWorkLanguage(String language) {
        if (StringUtils.isBlank(language)) {
            return;
        }
        currentLanguage = language;
    }

    public void selectThesaurus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            clearSelection();
            return;
        }
        applyThesaurus(thesaurusId.trim());
    }

    private void applyThesaurus(String thesaurusId) {
        ThesaurusSelection selection = thesaurusSelectionService.resolve(thesaurusId);
        if (selection == null) {
            return;
        }
        currentThesaurusId = selection.thesaurusId();
        currentThesaurusTitle = selection.title();
        currentLanguage = thesaurusWorkLanguageService.resolveForThesaurus(selection.thesaurusId());
    }
}
