package fr.cnrs.opentheso.suggestion.ui;

import fr.cnrs.opentheso.suggestion.service.InstanceFeatureService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * Lecture des interrupteurs de la barre de gauche. Chaque appel relit le cache du service,
 * mis à jour dès l'enregistrement dans Paramètres de l'instance.
 */
@Named("instanceFeatureBean")
@ApplicationScoped
@RequiredArgsConstructor
public class InstanceFeatureBean implements Serializable {

    private final transient InstanceFeatureService instanceFeatureService;

    public boolean isSuggestionEnabled() {
        return instanceFeatureService.current().suggestionEnabled();
    }

    public boolean isLanguageVisible() {
        return instanceFeatureService.current().languageVisible();
    }

    public boolean isRestApiVisible() {
        return instanceFeatureService.current().restApiVisible();
    }

    public boolean isGraphApiVisible() {
        return instanceFeatureService.current().graphApiVisible();
    }
}
