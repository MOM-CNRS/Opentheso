package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LegacyThesaurusPreferencesProvider {

    private final PreferencesRepository preferencesRepository;

    public Optional<Preferences> findPreferences(String thesaurusId) {
        return preferencesRepository.findByIdThesaurus(thesaurusId);
    }
}
