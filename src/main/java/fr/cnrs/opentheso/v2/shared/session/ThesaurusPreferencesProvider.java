package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.entites.Preferences;

import java.util.Optional;

public interface ThesaurusPreferencesProvider {

    Optional<Preferences> findPreferences(String thesaurusId);
}
