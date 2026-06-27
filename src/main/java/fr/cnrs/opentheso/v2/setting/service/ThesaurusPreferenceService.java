package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.utils.SimpleCrypto;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.mapper.SettingMapper;
import fr.cnrs.opentheso.v2.setting.model.IdentifierServerType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;
import fr.cnrs.opentheso.v2.shared.repository.PreferencesJpaRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public ThesaurusPreferences loadPreferences(String thesaurusId, String workLang) {
        PreferencesEntity entity = requirePreferences(thesaurusId);
        var languages = thesaurusSettingsQueryRepository.findUsedLanguages(thesaurusId, workLang).stream()
                .map(SettingMapper::toLanguage)
                .toList();
        return SettingMapper.toPreferences(entity, languages);
    }

    @Transactional
    public ThesaurusPreferences savePreferences(String thesaurusId, ThesaurusPreferences preferences, String workLang) {
        return savePreferences(thesaurusId, preferences, null, null, null, null, workLang);
    }

    @Transactional
    public ThesaurusPreferences savePreferences(
            String thesaurusId,
            ThesaurusPreferences preferences,
            String newPassArk,
            String newPassHandle,
            String newDeeplApiKey,
            String newApiKeyOpenArk,
            String workLang
    ) {
        PreferencesEntity entity = requirePreferences(thesaurusId);
        SettingMapper.applyPreferences(entity, preferences);
        if (StringUtils.isNotBlank(newPassArk)) {
            entity.setPassArk(newPassArk);
        }
        if (StringUtils.isNotBlank(newPassHandle)) {
            entity.setPassHandle(newPassHandle);
        }
        if (StringUtils.isNotBlank(newDeeplApiKey)) {
            entity.setDeeplApiKey(newDeeplApiKey);
        }
        if (StringUtils.isNotBlank(newApiKeyOpenArk)) {
            entity.setApiKeyOpenArk(crypto.encrypt(newApiKeyOpenArk));
        }
        normalizePaths(entity);
        preferencesJpaRepository.save(entity);
        log.info("Préférences enregistrées pour le thésaurus {}", thesaurusId);
        return loadPreferences(thesaurusId, workLang);
    }

    @Transactional
    public ThesaurusPreferences updateIdentifierServer(
            String thesaurusId,
            IdentifierServerType serverType,
            String workLang
    ) {
        PreferencesEntity entity = requirePreferences(thesaurusId);
        SettingMapper.applyIdentifierServerType(entity, serverType);
        preferencesJpaRepository.save(entity);
        log.info("Serveur d'identifiants mis à jour pour le thésaurus {}", thesaurusId);
        return loadPreferences(thesaurusId, workLang);
    }

    @Transactional
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
        return loadPreferences(thesaurusId, workLang);
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
