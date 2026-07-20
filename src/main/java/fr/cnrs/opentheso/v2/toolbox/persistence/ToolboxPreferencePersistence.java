package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolboxPreferencePersistence {

    private final PreferencesRepository preferencesRepository;

    public void initPreferences(String thesaurusId, String workLanguage) {
        String basePreferredName = thesaurusId;
        String preferredName = basePreferredName;
        int index = 1;
        while (preferencesRepository.existsByPreferredName(preferredName)) {
            preferredName = basePreferredName + "_" + index;
            index++;
        }

        log.debug("Initialisation des préférences pour le thésaurus {}", thesaurusId);
        preferencesRepository.save(Preferences.builder()
                .idThesaurus(thesaurusId)
                .sourceLang(workLanguage)
                .idNaan("66666")
                .prefixIdHandle("66.666.66666")
                .privatePrefixHandle("crt")
                .prefixArk("crt")
                .urlApiHandle("https://handle.mom.fr:8000/api/handles/")
                .uriArk("https://ark.mom.fr/ark:/")
                .cheminSite("http://mondomaine.fr/")
                .originalUri("http://mondomaine.fr")
                .preferredName(preferredName)
                .identifierType(2)
                .autoExpandTree(true)
                .webservices(true)
                .breadcrumb(true)
                .build());
    }

    public String getWorkLanguage(String thesaurusId) {
        var preference = preferencesRepository.findByIdThesaurus(thesaurusId);
        if (preference.isEmpty()) {
            initPreferences(thesaurusId, "fr");
            return null;
        }
        return preference.get().getSourceLang();
    }

    public boolean setWorkLanguage(String languageCode, String thesaurusId) {
        var preference = preferencesRepository.findByIdThesaurus(thesaurusId);
        if (preference.isEmpty()) {
            return false;
        }
        preference.get().setSourceLang(languageCode);
        preferencesRepository.save(preference.get());
        return true;
    }

    public void deletePreferences(String thesaurusId) {
        preferencesRepository.deleteByIdThesaurus(thesaurusId);
    }

    public void updateThesaurusId(String oldIdThesaurus, String newIdThesaurus) {
        preferencesRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    public Preferences findPreferences(String thesaurusId) {
        return preferencesRepository.findByIdThesaurus(thesaurusId).orElse(null);
    }

    public void updatePreferredName(String thesaurusId, String preferredName) {
        if (StringUtils.isBlank(thesaurusId) || StringUtils.isBlank(preferredName)) {
            return;
        }
        var preference = preferencesRepository.findByIdThesaurus(thesaurusId);
        if (preference.isEmpty()) {
            return;
        }
        String candidate = preferredName.trim();
        String uniqueName = candidate;
        int index = 1;
        while (preferencesRepository.existsByPreferredName(uniqueName)
                && !uniqueName.equals(preference.get().getPreferredName())) {
            uniqueName = candidate + "_" + index;
            index++;
        }
        preference.get().setPreferredName(uniqueName);
        preferencesRepository.save(preference.get());
    }
}
