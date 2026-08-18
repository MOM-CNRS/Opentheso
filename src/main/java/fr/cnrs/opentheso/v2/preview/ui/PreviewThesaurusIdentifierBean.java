package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.setting.ui.PreferenceEditor;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Named("v2PreviewIdentifierBean")
@ViewScoped
@RequiredArgsConstructor
public class PreviewThesaurusIdentifierBean implements Serializable {

    private final PreviewThesaurusPreferenceBean preferenceBean;

    public PreferenceEditor getPreference() {
        return preferenceBean.getPreference();
    }

    /**
     * Un seul serveur d'identifiants à la fois (Ark, Ark local, Handle, OpenArk).
     * Ne persiste pas : l'enregistrement se fait via {@link PreviewThesaurusPreferenceBean#savePreferences()}.
     */
    public void selectIdentifierServer(AjaxBehaviorEvent event) {
        PreferenceEditor editor = getPreference();
        if (editor == null || event == null || event.getComponent() == null) {
            return;
        }
        IdentifierServerSelection.applyExclusive(editor, event.getComponent().getId());
    }
}
