package fr.cnrs.opentheso.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void cacheManager_returnsNonNull() {
        CacheManager manager = cacheConfig.cacheManager();
        assertNotNull(manager);
    }

    @Test
    void cacheManager_isCaffeineBacked() {
        assertTrue(cacheConfig.cacheManager() instanceof CaffeineCacheManager);
    }

    @Test
    void cacheManager_exposesThesaurusPreferencesCache() {
        CacheManager manager = cacheConfig.cacheManager();
        assertNotNull(manager.getCache(CacheConfig.THESAURUS_PREFERENCES_CACHE));
    }

    @Test
    void thesaurusPreferencesCacheName_isStable() {
        // Guards against accidental rename that would silently invalidate @Cacheable keys
        assertEquals("thesaurusPreferences", CacheConfig.THESAURUS_PREFERENCES_CACHE);
    }
}
