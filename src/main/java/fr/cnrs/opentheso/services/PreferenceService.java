package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.PreferencesRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class PreferenceService {

    private final PreferencesRepository preferencesRepository;

    public void initPreferences(String idThesaurus, String workLanguage) {

        // contrôle du (preferredname s'il existe avant), si oui, il faut l'incrémenter
        String basePreferredName = idThesaurus;
        String preferredName = basePreferredName;

        int index = 1;

        // Vérifie les doublons et incrémente
        while (preferencesRepository.existsByPreferredName(preferredName)) {
            preferredName = basePreferredName + "_" + index;
            index++;
        }


        log.debug("Initialisation des préférences pour le thésaurus {}", idThesaurus);
        preferencesRepository.save(Preferences.builder()
                .idThesaurus(idThesaurus)
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

    public void addPreference(Preferences preference, String idThesaurus) {

        log.debug("Formatage des données de preférence");
        var preferenceToSave = normalizePreferencesDatas(preference);

        log.debug("Ajout des préférences du thésaurus {} dans la base de données", idThesaurus);
        preferencesRepository.save(preferenceToSave);
    }

    public boolean isPreferredNameExist(String preferredName) {
        return preferencesRepository.existsByPreferredName(preferredName);
    }

    public String getThesaurusIdFromPersistentName(String persistentThesaurusName) {
        return preferencesRepository.findIdThesaurusByPreferredName(persistentThesaurusName);
    }

    public void updateAllPreferenceUser(Preferences preference) {

        log.debug("Mise à jour des preferences du thésaurus {}", preference.getIdThesaurus());
        preferencesRepository.save(preference);
    }

    private Preferences normalizePreferencesDatas(Preferences preference) {

        if (StringUtils.isNotEmpty(preference.getCheminSite())) {
            log.debug("Formatage de la valeur de chemin du site {}", preference.getCheminSite());
            if (!preference.getCheminSite().substring(preference.getCheminSite().length() - 1).equalsIgnoreCase("/")) {
                preference.setCheminSite(preference.getCheminSite() + "/");
            }
        }
        if (StringUtils.isNotEmpty(preference.getServerArk())) {
            log.debug("Formatage de la valeur du serveur Ark {}", preference.getServerArk());
            if (!preference.getServerArk().substring(preference.getServerArk().length() - 1).equalsIgnoreCase("/")) {
                preference.setServerArk(preference.getServerArk() + "/");
            }
        }

        return preference;
    }

    public Preferences getThesaurusPreferences(String idThesaurus) {

        log.debug("Rechercher des paramètres du thésaurus {}", idThesaurus);
        var preference = preferencesRepository.findByIdThesaurus(idThesaurus);

        if (preference.isEmpty()) {
            log.error("Aucun paramètre n'est trouvé pour le thésaurus id {}", idThesaurus);
            return null;
        }

        log.debug("Paramètres trouvés pour le thésaurus id {}", idThesaurus);
        return preference.get();
    }

    public void setIdentifierFlags(String idThesaurus, boolean useArk, boolean useArkLocal, boolean useHandle, boolean useOpenArk) {

        log.debug("Mise à jour des flags d'identifier du thésaurus {}", idThesaurus);
        var preference = preferencesRepository.findByIdThesaurus(idThesaurus);

        if (preference.isEmpty()) {
            log.error("Aucun paramètre n'est trouvé pour le thésaurus id {}", idThesaurus);
            return;
        }

        preference.get().setUseArk(useArk);
        preference.get().setUseArkLocal(useArkLocal);
        preference.get().setUseHandle(useHandle);
        preference.get().setUseOpenArk(useOpenArk);
        preferencesRepository.save(preference.get());
    }

    public String getWorkLanguageOfThesaurus(String idThesaurus) {

        log.debug("Rechercher de la langue source du thésaurus {}", idThesaurus);
        var preference = preferencesRepository.findByIdThesaurus(idThesaurus);

        if (preference.isEmpty()) {
            log.debug("Aucun paramètre n'est trouvé pour le thésaurus id {}", idThesaurus);
            initPreferences(idThesaurus, "fr");
            return null ;
        }

        log.debug("La langue source du thésaurus {}  est {}", idThesaurus, preference.get().getSourceLang());
        return preference.get().getSourceLang();
    }

    public boolean setWorkLanguageOfThesaurus(String idLang, String idThesaurus) {

        log.debug("Début de la mise à jour de la source language pour le thésaurus {}", idThesaurus);
        var preference = preferencesRepository.findByIdThesaurus(idThesaurus);

        if (preference.isEmpty()) {
            log.debug("Aucun paramètre n'est trouvé pour le thésaurus id {}", idThesaurus);
            return false;
        }

        preference.get().setSourceLang(idLang);
        preferencesRepository.save(preference.get());
        log.debug("Mise à jour de la source language terminé pour le thésaurus {}", idThesaurus);
        return true;
    }

    public void deletePreferenceThesaurus(String idThesaurus) {

        log.debug("Suppression des préférences du thésaurus id {}", idThesaurus);
        preferencesRepository.deleteByIdThesaurus(idThesaurus);
    }

    public void updateThesaurusId(String oldIdThesaurus, String newIdThesaurus) {

        log.debug("Mise à jour du thésaurus id pour les préférences du thésaurus id {}", oldIdThesaurus);
        preferencesRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }
}
