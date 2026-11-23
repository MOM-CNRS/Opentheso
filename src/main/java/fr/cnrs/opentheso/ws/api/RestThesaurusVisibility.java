package fr.cnrs.opentheso.ws.api;

import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.services.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Visibilité des thésaurus dans les API REST, décidée uniquement par la clé API.
 * Sans clé : thésaurus publics. Clé d'un profil non super-administrateur : publics
 * plus les thésaurus des projets de ce profil. Clé d'un super-administrateur : tous.
 */
@Service
public class RestThesaurusVisibility {

    private final RestThesaurusVisibilityRepository repository;
    private final ThesaurusRepository thesaurusRepository;
    private final RightsService rightsService;
    private final ApiKeyService apiKeyService;

    public RestThesaurusVisibility(
            RestThesaurusVisibilityRepository repository,
            ThesaurusRepository thesaurusRepository,
            RightsService rightsService,
            ApiKeyService apiKeyService
    ) {
        this.repository = repository;
        this.thesaurusRepository = thesaurusRepository;
        this.rightsService = rightsService;
        this.apiKeyService = apiKeyService;
    }

    public Integer currentUserId() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return currentUserId(servlet.getRequest());
        }
        return null;
    }

    public Integer currentUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String apiKey = firstHeader(request, "X-API-KEY", "API-KEY");
        if (StringUtils.isBlank(apiKey)) {
            return null;
        }
        Optional<User> user = apiKeyService.findUserByApiKey(apiKey.trim());
        return user.map(User::getId).orElse(null);
    }

    public boolean canSee(Integer userId, String thesaurusId) {
        return explain(userId, thesaurusId) == RestThesaurusAccess.ALLOWED;
    }

    public RestThesaurusAccess explain(Integer userId, String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return RestThesaurusAccess.ALLOWED;
        }
        if (isSuperAdmin(userId)) {
            return RestThesaurusAccess.ALLOWED;
        }
        return repository.describe(userId == null ? -1 : userId, thesaurusId.trim())
                .map(facts -> RestThesaurusAccess.decide(
                        true,
                        facts.isPrivate(),
                        facts.attachedToProject(),
                        facts.memberOfProject()))
                .orElse(RestThesaurusAccess.NOT_FOUND);
    }

    public List<String> visibleIds(Integer userId) {
        if (isSuperAdmin(userId)) {
            return thesaurusRepository.findAllOrderByCreatedDesc().stream()
                    .map(Thesaurus::getIdThesaurus)
                    .toList();
        }
        return repository.visibleIds(userId == null ? -1 : userId);
    }

    private boolean isSuperAdmin(Integer userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        try {
            return rightsService.capabilities(userId).superAdmin();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String firstHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
