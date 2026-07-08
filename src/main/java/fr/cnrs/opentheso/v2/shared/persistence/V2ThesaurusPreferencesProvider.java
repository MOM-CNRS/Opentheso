package fr.cnrs.opentheso.v2.shared.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Primary
@Component
@RequiredArgsConstructor
public class V2ThesaurusPreferencesProvider implements ThesaurusPreferencesProvider {

    private final PreferencesRepository preferencesRepository;

    @Override
    public Optional<Preferences> findPreferences(String thesaurusId) {
        return preferencesRepository.findByIdThesaurus(thesaurusId);
    }
}
