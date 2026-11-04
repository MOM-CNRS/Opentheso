package fr.cnrs.opentheso.shared.ui;

import fr.cnrs.opentheso.shared.model.LegalPageContent;
import fr.cnrs.opentheso.shared.service.LegalPageService;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * Pages du pied de page : lecture publique, édition réservée au super-administrateur,
 * pour la langue d'interface en cours.
 */
@Named("legalPageBean")
@ViewScoped
@RequiredArgsConstructor
public class LegalPageBean implements Serializable {

    private final transient LegalPageService legalPageService;
    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;

    @Getter
    @Setter
    private String code;
    @Getter
    @Setter
    private String title = "";
    @Getter
    @Setter
    private String html = "";
    @Getter
    private boolean known;
    @Getter
    private boolean editing;
    @Getter
    private String saveMessage;
    @Getter
    private boolean saveError;

    private Map<String, String> storedTitles;

    public void load() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces != null && faces.isPostback()) {
            return;
        }
        if (faces != null && StringUtils.isBlank(code)) {
            code = faces.getExternalContext().getRequestParameterMap().get("code");
        }
        editing = false;
        saveMessage = null;
        saveError = false;
        applyStored();
    }

    public boolean isCanEdit() {
        return known && userSession.isSuperAdmin();
    }

    public boolean isContentPresent() {
        return StringUtils.isNotBlank(html);
    }

    public String getMentionsTitle() {
        return displayTitle(LegalPageService.MENTIONS);
    }

    public String getPrivacyTitle() {
        return displayTitle(LegalPageService.PRIVACY);
    }

    public String getAccessibilityTitle() {
        return displayTitle(LegalPageService.ACCESSIBILITY);
    }

    public void startEditing() {
        if (!isCanEdit()) {
            return;
        }
        editing = true;
        saveMessage = null;
        saveError = false;
    }

    public void cancelEditing() {
        load();
    }

    public void save() {
        if (!isCanEdit()) {
            fail("legal.saveError");
            return;
        }
        if (StringUtils.isBlank(title)) {
            fail("legal.nameRequired");
            return;
        }
        if (!legalPageService.save(code, localeBean.getIdLangue(), title, html)) {
            fail("legal.saveError");
            return;
        }
        storedTitles = null;
        editing = false;
        applyStored();
        saveError = false;
        saveMessage = localeBean.getMsg("legal.saved");
    }

    private void applyStored() {
        if (!legalPageService.isKnown(code)) {
            known = false;
            title = localeBean.getMsg("legal.unknown");
            html = "";
            return;
        }
        known = true;
        Optional<LegalPageContent> stored = legalPageService.find(code, localeBean.getIdLangue());
        title = stored.map(LegalPageContent::title)
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> localeBean.getMsg(LegalPageService.titleKey(code)));
        html = stored.map(LegalPageContent::html).orElse("");
    }

    private String displayTitle(String pageCode) {
        if (storedTitles == null) {
            storedTitles = legalPageService.findTitles(localeBean.getIdLangue());
        }
        String stored = storedTitles.get(pageCode);
        if (StringUtils.isNotBlank(stored)) {
            return stored;
        }
        return localeBean.getMsg(LegalPageService.titleKey(pageCode));
    }

    private void fail(String key) {
        saveError = true;
        saveMessage = localeBean.getMsg(key);
    }
}
