package fr.cnrs.opentheso.v2.rights;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnrs.opentheso.v2.shared.auth.UserCapabilityService;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache central des droits (profil session, rôles projet, rôles thésaurus).
 * TTL : {@code opentheso.auth.rights-cache-ttl} ({@code 0s} = pas d'expiration auto).
 */
@Service
@RequiredArgsConstructor
public class UserRightsCache {

    private final UserCapabilityService userCapabilityService;
    private final ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;

    @Value("${opentheso.auth.rights-cache-ttl:5m}")
    private Duration rightsCacheTtl;

    @Value("${opentheso.auth.rights-cache-max-size:10000}")
    private long rightsCacheMaxSize;

    private Cache<Integer, CachedRights> cache;

    @PostConstruct
    void initCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(Math.max(1, rightsCacheMaxSize));
        if (rightsCacheTtl != null && !rightsCacheTtl.isZero() && !rightsCacheTtl.isNegative()) {
            builder.expireAfterWrite(rightsCacheTtl);
        }
        cache = builder.build();
    }

    public SessionUser getSessionUser(int userId) {
        return getOrLoad(userId).sessionUser();
    }

    public Optional<Integer> getEffectiveRoleOnThesaurus(int userId, String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Optional.empty();
        }
        CachedRights rights = getOrLoad(userId);
        return rights.rolesByThesaurus().computeIfAbsent(
                thesaurusId,
                id -> thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(userId, id)
        );
    }

    public Optional<Integer> getRoleOnProject(int userId, int projectId) {
        CachedRights rights = getOrLoad(userId);
        return rights.rolesByProject().computeIfAbsent(
                projectId,
                id -> projectAdminQueryRepository.findCallerRoleOnProject(userId, id)
        );
    }

    public void invalidate(int userId) {
        if (cache != null) {
            cache.invalidate(userId);
        }
    }

    public void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    private CachedRights getOrLoad(int userId) {
        return cache.get(userId, id -> new CachedRights(
                userCapabilityService.loadSessionUser(id),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>()
        ));
    }

    record CachedRights(
            SessionUser sessionUser,
            ConcurrentHashMap<String, Optional<Integer>> rolesByThesaurus,
            ConcurrentHashMap<Integer, Optional<Integer>> rolesByProject
    ) {
    }
}
