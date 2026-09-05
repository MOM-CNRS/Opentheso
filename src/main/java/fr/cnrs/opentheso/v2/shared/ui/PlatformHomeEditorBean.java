package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.shared.service.PlatformHomeWriteService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;

/**
 * Édition de la page d'accueil plateforme + code Google Analytics
 * (équivalent legacy {@code viewEditorHomeBean} / {@code main_thesaurus.xhtml}).
 */
@Getter
@Setter
@ViewScoped
@Named("v2PlatformHomeEditorBean")
@RequiredArgsConstructor
public class PlatformHomeEditorBean implements Serializable {

    private static final String OPTION_NONE = "Option1";
    private static final String OPTION_HOME = "Option2";
    private static final String OPTION_ANALYTICS = "Option3";

    private final transient PlatformHomeWriteService platformHomeWriteService;
    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient ObjectProvider<ConsultationShellBean> consultationShellBean;

    private String selectedOption = OPTION_NONE;
    private boolean editingHomePage;
    private boolean editingGoogleAnalytics;
    private boolean plainTextView;
    private String text;
    private String codeGoogleAnalytics;

    public boolean isActionsAvailable() {
        return userSession.isSuperAdmin();
    }

    public boolean isTextVisible() {
        return !editingHomePage && !editingGoogleAnalytics;
    }

    public void onOptionChange() {
        if (!isActionsAvailable()) {
            reset();
            return;
        }
        switch (selectedOption) {
            case OPTION_HOME -> startEditHomePage();
            case OPTION_ANALYTICS -> startEditGoogleAnalytics();
            default -> reset();
        }
    }

    public void startEditHomePage() {
        if (!isActionsAvailable()) {
            return;
        }
        text = platformHomeWriteService.loadHomePageHtml(v2LocaleBean.getIdLangue());
        editingHomePage = true;
        editingGoogleAnalytics = false;
        plainTextView = false;
        selectedOption = OPTION_HOME;
    }

    public void startEditGoogleAnalytics() {
        if (!isActionsAvailable()) {
            return;
        }
        codeGoogleAnalytics = platformHomeWriteService.loadGoogleAnalyticsCode();
        editingGoogleAnalytics = true;
        editingHomePage = false;
        plainTextView = false;
        selectedOption = OPTION_ANALYTICS;
    }

    public void setViewPlainTextTo(boolean status) {
        plainTextView = status;
    }

    public void cancel() {
        reset();
    }

    public void saveHomePage() {
        if (!isActionsAvailable()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        if (!platformHomeWriteService.saveHomePageHtml(v2LocaleBean.getIdLangue(), text)) {
            MessageUtils.showErrorMessage("L'ajout a échoué !");
            return;
        }
        MessageUtils.showInformationMessage("Texte ajouté avec succès");
        refreshShellHomeHtml();
        reset();
    }

    public void saveGoogleAnalytics() {
        if (!isActionsAvailable()) {
            MessageUtils.showErrorMessage(WriteUiMessages.UNAUTHORIZED_FALLBACK);
            return;
        }
        platformHomeWriteService.saveGoogleAnalyticsCode(codeGoogleAnalytics);
        MessageUtils.showInformationMessage("Code Analytics ajouté avec succès !");
        reset();
    }

    public void reset() {
        selectedOption = OPTION_NONE;
        editingHomePage = false;
        editingGoogleAnalytics = false;
        plainTextView = false;
        text = null;
        codeGoogleAnalytics = null;
    }

    private void refreshShellHomeHtml() {
        ConsultationShellBean shell = consultationShellBean.getIfAvailable();
        if (shell != null) {
            shell.refreshPlatformHomeHtml();
        }
    }
}
