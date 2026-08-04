package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusHomeWriteService;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;

/**
 * Édition de la page d'accueil du thésaurus (équivalent legacy {@code viewEditorThesaurusHomeBean}).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusHomeEditorBean")
@RequiredArgsConstructor
public class ThesaurusHomeEditorBean implements Serializable {

    private final ThesaurusHomeWriteService thesaurusHomeWriteService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final RightsService rightsService;
    private final ObjectProvider<ThesaurusBrowseBean> thesaurusBrowseBean;

    private boolean editing;
    private boolean plainTextView;
    private String text;

    public boolean isCanEdit() {
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId);
    }

    public void reset() {
        editing = false;
        plainTextView = false;
        text = null;
    }

    public void startEditing() {
        if (!isCanEdit()) {
            return;
        }
        text = new ToolsHelper().normalizeHtml(thesaurusHomeWriteService.loadHtml(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        ));
        editing = true;
        plainTextView = false;
    }

    public void setViewPlainTextTo(boolean status) {
        plainTextView = status;
    }

    public void cancel() {
        reset();
    }

    public void save() {
        if (!isCanEdit()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        boolean ok = thesaurusHomeWriteService.saveHtml(
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage(),
                text
        );
        if (!ok) {
            MessageUtils.showErrorMessage("L'ajout a échoué !");
            return;
        }
        MessageUtils.showInformationMessage("Texte ajouté avec succès");
        reset();
        ThesaurusBrowseBean browseBean = thesaurusBrowseBean.getIfAvailable();
        if (browseBean != null) {
            browseBean.reloadThesaurusHomeAfterEdit();
        }
    }
}
