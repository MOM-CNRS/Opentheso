package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolboxPreferencePersistence {

    private final PreferencesRepository preferencesRepository;

    public void initPreferences(String thesaurusId, String workLanguage) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        // Idempotent : un thésaurus n'a qu'une ligne preferences (contrainte id_thesaurus).
        if (preferencesRepository.findByIdThesaurus(thesaurusId).isPresent()) {
            log.debug("Préférences déjà présentes pour le thésaurus {}, initialisation ignorée", thesaurusId);
            return;
        }

        String basePreferredName = thesaurusId;
        String preferredName = basePreferredName;
        int index = 1;
        while (preferencesRepository.existsInAnotherThesaurus(thesaurusId, preferredName)) {
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
                .master(false)
                .build());
    }

    public void updateMasterRole(String thesaurusId, boolean master) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        var preference = preferencesRepository.findByIdThesaurus(thesaurusId);
        if (preference.isEmpty()) {
            return;
        }
        preference.get().setMaster(master);
        preferencesRepository.save(preference.get());
    }

    public boolean isMaster(String thesaurusId) {
        var preference = preferencesRepository.findByIdThesaurus(thesaurusId);
        return preference.map(Preferences::isMaster).orElse(false);
    }

    @Transactional
    public void updateMasterLink(
            String thesaurusId,
            String masterServerUrl,
            String masterThesaurusId,
            String masterApiKey
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        String url = StringUtils.trimToNull(masterServerUrl);
        String masterId = StringUtils.trimToNull(masterThesaurusId);
        if (masterApiKey != null) {
            preferencesRepository.updateMasterLink(
                    thesaurusId, url, masterId, StringUtils.trimToNull(masterApiKey));
        } else {
            preferencesRepository.updateMasterLinkKeepApiKey(thesaurusId, url, masterId);
        }
    }

    public void updateLastSyncAt(String thesaurusId, java.time.LocalDateTime lastSyncAt) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        var preference = preferencesRepository.findByIdThesaurus(thesaurusId);
        if (preference.isEmpty()) {
            return;
        }
        preference.get().setLastSyncAt(lastSyncAt);
        preferencesRepository.save(preference.get());
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
        while (preferencesRepository.existsInAnotherThesaurus(thesaurusId, uniqueName)
                && !uniqueName.equals(preference.get().getPreferredName())) {
            uniqueName = candidate + "_" + index;
            index++;
        }
        preference.get().setPreferredName(uniqueName);
        preferencesRepository.save(preference.get());
    }
}
