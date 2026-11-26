package fr.cnrs.opentheso.ws.api;

import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.setting.service.ThesaurusApiSwitch;
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
    private final ThesaurusApiSwitch thesaurusApiSwitch;

    public RestThesaurusVisibility(
            RestThesaurusVisibilityRepository repository,
            ThesaurusRepository thesaurusRepository,
            RightsService rightsService,
            ApiKeyService apiKeyService,
            ThesaurusApiSwitch thesaurusApiSwitch
    ) {
        this.repository = repository;
        this.thesaurusRepository = thesaurusRepository;
        this.rightsService = rightsService;
        this.apiKeyService = apiKeyService;
        this.thesaurusApiSwitch = thesaurusApiSwitch;
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

    /** Utilisateur d'une clé API reçue hors requête HTTP (contexte GraphQL). */
    public Integer userIdFromApiKey(String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            return null;
        }
        return apiKeyService.findUserByApiKey(apiKey.trim()).map(User::getId).orElse(null);
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

    /** Thésaurus listés par l'API : visibles pour l'appelant et dont l'API n'est pas désactivée. */
    public List<String> visibleIds(Integer userId) {
        var disabled = thesaurusApiSwitch.disabledIds();
        return authorizedIds(userId).stream()
                .filter(id -> !disabled.contains(ThesaurusApiSwitch.normalize(id)))
                .toList();
    }

    private List<String> authorizedIds(Integer userId) {
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
