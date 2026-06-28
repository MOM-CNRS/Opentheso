package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelection;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@SessionScoped
@Named("v2ThesaurusContext")
public class ThesaurusContext implements Serializable {

    private final ThesaurusSelectionService thesaurusSelectionService;
    private final SelectedTheso selectedTheso;

    private String idConceptFromUri;
    private String idGroupFromUri;
    private String idThesoFromUri;

    private String currentThesaurusId;
    private String currentThesaurusTitle;

    public ThesaurusContext(
            ThesaurusSelectionService thesaurusSelectionService,
            SelectedTheso selectedTheso
    ) {
        this.thesaurusSelectionService = thesaurusSelectionService;
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
        ThesaurusSelection selection = thesaurusSelectionService.resolve(thesaurusId);
        if (selection == null) {
            return;
        }
        currentThesaurusId = selection.thesaurusId();
        currentThesaurusTitle = selection.title();
        if (selectedTheso != null) {
            selectedTheso.setCurrentIdTheso(selection.thesaurusId());
        }
    }
}
