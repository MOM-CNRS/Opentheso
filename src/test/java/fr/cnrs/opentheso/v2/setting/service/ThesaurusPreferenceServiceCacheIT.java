package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.config.CacheConfig;
import fr.cnrs.opentheso.v2.shared.persistence.PreferencesEntity;
import fr.cnrs.opentheso.v2.shared.repository.PreferencesJpaRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie que @Cacheable et @CacheEvict fonctionnent réellement via le proxy Spring AOP.
 * Les tests unitaires Mockito contournent le proxy — seul un vrai contexte Spring
 * permet de garantir que le cache est effectivement utilisé entre deux appels.
 */
@SpringBootTest(
        classes = {CacheConfig.class, ThesaurusPreferenceService.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestPropertySource(properties = {
        "crypto.openark.key=12345678901234567890123456789012",
        "spring.main.allow-bean-definition-overriding=true"
})
class ThesaurusPreferenceServiceCacheIT {

    @MockBean
    private PreferencesJpaRepository preferencesJpaRepository;

    @MockBean
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    @Autowired
    private ThesaurusPreferenceService thesaurusPreferenceService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void evictCacheBeforeEachTest() {
        var cache = cacheManager.getCache(CacheConfig.THESAURUS_PREFERENCES_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void loadPreferencesOrNull_secondCallReturnsCachedResult_repositoryCalledOnce() {
        when(preferencesJpaRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(sampleEntity("TH1")));
        when(thesaurusSettingsQueryRepository.findUsedLanguages("TH1", "fr")).thenReturn(List.of());

        thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr");
        thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr");

        // Repository must be called exactly once — second call hits the cache
        verify(preferencesJpaRepository, times(1)).findByIdThesaurus("TH1");
    }

    @Test
    void loadPreferencesOrNull_differentKeys_repositoryCalledForEach() {
        when(preferencesJpaRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(sampleEntity("TH1")));
        when(preferencesJpaRepository.findByIdThesaurus("TH2")).thenReturn(Optional.of(sampleEntity("TH2")));
        when(thesaurusSettingsQueryRepository.findUsedLanguages("TH1", "fr")).thenReturn(List.of());
        when(thesaurusSettingsQueryRepository.findUsedLanguages("TH2", "fr")).thenReturn(List.of());

        thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr");
        thesaurusPreferenceService.loadPreferencesOrNull("TH2", "fr");

        verify(preferencesJpaRepository, times(1)).findByIdThesaurus("TH1");
        verify(preferencesJpaRepository, times(1)).findByIdThesaurus("TH2");
    }

    @Test
    void savePreferences_evictsCache_nextCallHitsRepository() {
        PreferencesEntity entity = sampleEntity("TH1");
        when(preferencesJpaRepository.findByIdThesaurus("TH1")).thenReturn(Optional.of(entity));
        when(preferencesJpaRepository.save(entity)).thenReturn(entity);
        when(thesaurusSettingsQueryRepository.findUsedLanguages("TH1", "fr")).thenReturn(List.of());

        // Prime the cache with one call
        thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr");
        // Second call must hit cache, not the repository
        thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr");
        verify(preferencesJpaRepository, times(1)).findByIdThesaurus("TH1");

        // Evict via save (savePreferences internally calls requirePreferences)
        var prefs = thesaurusPreferenceService.loadPreferences("TH1", "fr");
        thesaurusPreferenceService.savePreferences("TH1", prefs, "fr");

        // Record how many calls happened before the post-eviction check
        org.mockito.Mockito.clearInvocations(preferencesJpaRepository);

        // After eviction, next call to loadPreferencesOrNull must go to the repository
        thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr");
        verify(preferencesJpaRepository, times(1)).findByIdThesaurus("TH1");

        // And second call hits cache again
        thesaurusPreferenceService.loadPreferencesOrNull("TH1", "fr");
        verify(preferencesJpaRepository, times(1)).findByIdThesaurus("TH1");
    }

    @Test
    void loadPreferencesOrNull_returnsNullAndCachesNull_whenEntityMissing() {
        when(preferencesJpaRepository.findByIdThesaurus("MISSING")).thenReturn(Optional.empty());

        var first = thesaurusPreferenceService.loadPreferencesOrNull("MISSING", "fr");
        var second = thesaurusPreferenceService.loadPreferencesOrNull("MISSING", "fr");

        assertNull(first);
        assertNull(second);
        // null result is also cached — repository called only once
        verify(preferencesJpaRepository, times(1)).findByIdThesaurus("MISSING");
    }

    @Test
    void cacheManager_isAvailableInContext() {
        assertNotNull(cacheManager.getCache(CacheConfig.THESAURUS_PREFERENCES_CACHE));
    }

    private static PreferencesEntity sampleEntity(String thesaurusId) {
        PreferencesEntity e = new PreferencesEntity();
        e.setIdThesaurus(thesaurusId);
        e.setSourceLang("fr");
        return e;
    }
}
