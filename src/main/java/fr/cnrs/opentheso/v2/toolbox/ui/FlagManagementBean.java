package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageFlag;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.LanguageFlagService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.CellEditEvent;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@ViewScoped
@Named("v2FlagManagementBean")
public class FlagManagementBean implements Serializable {

    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;
    private final transient LanguageFlagService languageFlagService;

    private List<LanguageFlag> languages = Collections.emptyList();

    public FlagManagementBean(
            UserSession userSession,
            ToolboxAccessPolicy toolboxAccessPolicy,
            LanguageFlagService languageFlagService
    ) {
        this.userSession = userSession;
        this.toolboxAccessPolicy = toolboxAccessPolicy;
        this.languageFlagService = languageFlagService;
    }

    public boolean isScreenAvailable() {
        return toolboxAccessPolicy.canManageLanguageFlags(userSession);
    }

    public void load() {
        if (!isScreenAvailable()) {
            languages = Collections.emptyList();
            return;
        }
        languages = languageFlagService.listAll();
    }

    /**
     * Aligné legacy {@code FlagBean.updateLang} : persiste puis recharge la page
     * pour rafraîchir l'image du drapeau.
     */
    public void onCellEdit(CellEditEvent<LanguageFlag> event) {
        if (!isScreenAvailable() || event == null) {
            return;
        }
        LanguageFlag language = resolveEditedLanguage(event);
        if (language == null || StringUtils.isBlank(language.getIso6391())) {
            return;
        }
        languageFlagService.updateCountryCode(
                language.getIso6391(),
                StringUtils.trimToEmpty(language.getCountryCode())
        );
        load();
        PrimeFaces.current().executeScript("window.location.reload();");
    }

    private LanguageFlag resolveEditedLanguage(CellEditEvent<LanguageFlag> event) {
        Object rowData = event.getRowData();
        if (rowData instanceof LanguageFlag languageFlag) {
            Object newValue = event.getNewValue();
            if (newValue != null) {
                languageFlag.setCountryCode(String.valueOf(newValue));
            }
            return languageFlag;
        }
        int rowIndex = event.getRowIndex();
        if (rowIndex >= 0 && rowIndex < languages.size()) {
            LanguageFlag language = languages.get(rowIndex);
            Object newValue = event.getNewValue();
            if (newValue != null) {
                language.setCountryCode(String.valueOf(newValue));
            }
            return language;
        }
        return null;
    }
}
