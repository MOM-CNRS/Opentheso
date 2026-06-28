package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageFlag;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.LanguageFlagService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@ViewScoped
@Named("v2FlagManagementBean")
public class FlagManagementBean implements Serializable {

    private final UserSession userSession;
    private final LanguageFlagService languageFlagService;

    private List<LanguageFlag> languages = Collections.emptyList();

    public FlagManagementBean(UserSession userSession, LanguageFlagService languageFlagService) {
        this.userSession = userSession;
        this.languageFlagService = languageFlagService;
    }

    public boolean isScreenAvailable() {
        return ToolboxAccessPolicy.canManageLanguageFlags(userSession);
    }

    public void load() {
        if (!isScreenAvailable()) {
            languages = Collections.emptyList();
            return;
        }
        languages = languageFlagService.listAll();
    }

    public void onCellEdit(LanguageFlag language) {
        if (!isScreenAvailable()) {
            return;
        }
        languageFlagService.updateCountryCode(language.getIso6391(), language.getCountryCode());
        languages = languageFlagService.listAll();
        MessageUtils.showInformationMessage("Drapeau mis à jour");
    }
}
