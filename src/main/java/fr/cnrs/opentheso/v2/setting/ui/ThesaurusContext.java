package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
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

    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    private final SelectedTheso selectedTheso;

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private String idConceptFromUri;
    private String idGroupFromUri;
    private String idThesoFromUri;

    private String currentThesaurusId;
    private String currentThesaurusTitle;

    public ThesaurusContext(
            ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository,
            SelectedTheso selectedTheso
    ) {
        this.thesaurusSettingsQueryRepository = thesaurusSettingsQueryRepository;
        this.selectedTheso = selectedTheso;
    }

    public void syncFromViewParams() {
        if (StringUtils.isNotBlank(idThesoFromUri)) {
            applyThesaurus(idThesoFromUri.trim());
            idThesoFromUri = null;
            idConceptFromUri = null;
            idGroupFromUri = null;
            return;
        }
        if (StringUtils.isBlank(currentThesaurusId)
                && selectedTheso != null
                && StringUtils.isNotBlank(selectedTheso.getCurrentIdTheso())) {
            applyThesaurus(selectedTheso.getCurrentIdTheso());
        }
    }

    private void applyThesaurus(String thesaurusId) {
        currentThesaurusId = thesaurusId;
        currentThesaurusTitle = thesaurusSettingsQueryRepository
                .findThesaurusTitle(currentThesaurusId, workLanguage)
                .orElse(currentThesaurusId);
    }
}
