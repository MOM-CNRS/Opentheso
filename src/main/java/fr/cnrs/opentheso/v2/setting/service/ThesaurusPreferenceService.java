package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.config.CacheConfig;
import fr.cnrs.opentheso.utils.SimpleCrypto;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.mapper.SettingMapper;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;
import fr.cnrs.opentheso.v2.shared.repository.PreferencesJpaRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ThesaurusPreferenceService {

    private final PreferencesJpaRepository preferencesJpaRepository;
    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    private final SimpleCrypto crypto;

    public ThesaurusPreferenceService(
            PreferencesJpaRepository preferencesJpaRepository,
            ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository,
            @Value("${crypto.openark.key}") String secretKey
    ) {
        if (secretKey.length() != 32) {
            throw new IllegalStateException("La clé AES doit faire 32 caractères");
        }
        this.preferencesJpaRepository = preferencesJpaRepository;
        this.thesaurusSettingsQueryRepository = thesaurusSettingsQueryRepository;
        this.crypto = new SimpleCrypto(secretKey);
    }

    public boolean isPreferredNameExist(String idThesaurus, String preferredName) {
        return preferencesJpaRepository.existsInAnotherThesaurus(idThesaurus, preferredName);
    }

    @Transactional(readOnly = true)
    public ThesaurusPreferences loadPreferences(String thesaurusId, String workLang) {
        return toPreferences(thesaurusId, workLang);
    }

    /**
     * Same as {@link #loadPreferences(String, String)} but returns null instead of throwing
     * when the thesaurus has no preferences row configured yet (consultation read paths).
     * Cached because consultation pages (concept detail, thesaurus home, tree loading) call
     * this on every request while preferences change rarely; evicted by the save* methods
     * and after thesaurus language list changes.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.THESAURUS_PREFERENCES_CACHE, key = "#thesaurusId + '|' + #workLang")
    public ThesaurusPreferences loadPreferencesOrNull(String thesaurusId, String workLang) {
        try {
            return toPreferences(thesaurusId, workLang);
        } catch (InvalidSettingDataException noPreferencesConfigured) {
            return null;
        }
    }

    /**
     * Langues du thésaurus ({@code thesaurus_label}) — non mis en cache, pour le sélecteur
     * de recherche qui doit refléter immédiatement un ajout / une suppression.
     */
    @Transactional(readOnly = true)
    public List<ThesaurusLanguage> loadUsedLanguages(String thesaurusId, String workLang) {
        return usedLanguages(thesaurusId, workLang);
    }

    @CacheEvict(cacheNames = CacheConfig.THESAURUS_PREFERENCES_CACHE, allEntries = true)
    public void evictPreferencesCache() {
        // Intentional no-op: annotation drives cache eviction after language list changes.
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.THESAURUS_PREFERENCES_CACHE, allEntries = true)
    public ThesaurusPreferences savePreferences(String thesaurusId, ThesaurusPreferences preferences, String workLang) {
        return persistPreferences(thesaurusId, preferences, null, null, null, null, workLang);
    }

    /**
     * Persiste uniquement le tri de l'arbre (α / notation), sans toucher aux autres préférences.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.THESAURUS_PREFERENCES_CACHE, allEntries = true)
    public ThesaurusPreferences updateSortByNotation(String thesaurusId, boolean sortByNotation, String workLang) {
        PreferencesEntity entity = requirePreferences(thesaurusId);
        entity.setSortByNotation(sortByNotation);
        preferencesJpaRepository.save(entity);
        log.info("Tri par notation={} enregistré pour le thésaurus {}", sortByNotation, thesaurusId);
        return toPreferences(thesaurusId, workLang);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.THESAURUS_PREFERENCES_CACHE, allEntries = true)
    public ThesaurusPreferences savePreferences(
            String thesaurusId,
            ThesaurusPreferences preferences,
            String newPassArk,
            String newPassHandle,
            String newDeeplApiKey,
            String newApiKeyOpenArk,
            String workLang
    ) {
        return persistPreferences(
                thesaurusId, preferences, newPassArk, newPassHandle, newDeeplApiKey, newApiKeyOpenArk, workLang);
    }

    private ThesaurusPreferences persistPreferences(
            String thesaurusId,
            ThesaurusPreferences preferences,
            String newPassArk,
            String newPassHandle,
            String newDeeplApiKey,
            String newApiKeyOpenArk,
            String workLang
    ) {
        PreferencesEntity entity = requirePreferences(thesaurusId);
        String existingPassArk = entity.getPassArk();
        String existingPassHandle = entity.getPassHandle();
        String existingDeeplApiKey = entity.getDeeplApiKey();
        String existingApiKeyOpenArk = entity.getApiKeyOpenArk();

        SettingMapper.applyPreferences(entity, preferences);

        // Comme le legacy : ne pas écraser les secrets si aucun nouveau n'est saisi.
        entity.setPassArk(StringUtils.isNotBlank(newPassArk) ? newPassArk : existingPassArk);
        entity.setPassHandle(StringUtils.isNotBlank(newPassHandle) ? newPassHandle : existingPassHandle);
        entity.setDeeplApiKey(StringUtils.isNotBlank(newDeeplApiKey) ? newDeeplApiKey : existingDeeplApiKey);
        if (StringUtils.isNotBlank(newApiKeyOpenArk)) {
            entity.setApiKeyOpenArk(crypto.encrypt(newApiKeyOpenArk));
        } else {
            entity.setApiKeyOpenArk(existingApiKeyOpenArk);
        }
        normalizePaths(entity);
        preferencesJpaRepository.save(entity);
        log.info("Préférences enregistrées pour le thésaurus {}", thesaurusId);
        return toPreferences(thesaurusId, workLang);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.THESAURUS_PREFERENCES_CACHE, allEntries = true)
    public ThesaurusPreferences updateIdentifierServer(
            String thesaurusId,
            IdentifierServerType serverType,
            String workLang
    ) {
        PreferencesEntity entity = requirePreferences(thesaurusId);
        SettingMapper.applyIdentifierServerType(entity, serverType);
        preferencesJpaRepository.save(entity);
        log.info("Serveur d'identifiants mis à jour pour le thésaurus {}", thesaurusId);
        return toPreferences(thesaurusId, workLang);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.THESAURUS_PREFERENCES_CACHE, allEntries = true)
    public ThesaurusPreferences saveIdentifierSettings(
            String thesaurusId,
            ThesaurusPreferences preferences,
            String newApiKeyOpenArk,
            String workLang
    ) {
        PreferencesEntity entity = requirePreferences(thesaurusId);
        SettingMapper.applyPreferences(entity, preferences);
        normalizePaths(entity);
        if (StringUtils.isNotBlank(newApiKeyOpenArk)) {
            entity.setApiKeyOpenArk(crypto.encrypt(newApiKeyOpenArk));
        }
        preferencesJpaRepository.save(entity);
        log.info("Paramètres d'identifiants enregistrés pour le thésaurus {}", thesaurusId);
        return toPreferences(thesaurusId, workLang);
    }

    private ThesaurusPreferences toPreferences(String thesaurusId, String workLang) {
        PreferencesEntity entity = requirePreferences(thesaurusId);
        return SettingMapper.toPreferences(entity, usedLanguages(thesaurusId, workLang));
    }

    private List<ThesaurusLanguage> usedLanguages(String thesaurusId, String workLang) {
        return thesaurusSettingsQueryRepository.findUsedLanguages(thesaurusId, workLang).stream()
                .map(SettingMapper::toLanguage)
                .toList();
    }

    private PreferencesEntity requirePreferences(String thesaurusId) {
        return preferencesJpaRepository.findByIdThesaurus(thesaurusId)
                .orElseThrow(() -> new InvalidSettingDataException(
                        "Aucun paramètre n'est trouvé pour le thésaurus " + thesaurusId + "."
                ));
    }

    private void normalizePaths(PreferencesEntity entity) {
        if (StringUtils.isNotEmpty(entity.getCheminSite())
                && !entity.getCheminSite().endsWith("/")) {
            entity.setCheminSite(entity.getCheminSite() + "/");
        }
        if (StringUtils.isNotEmpty(entity.getServerArk())
                && !entity.getServerArk().endsWith("/")) {
            entity.setServerArk(entity.getServerArk() + "/");
        }
    }
}
