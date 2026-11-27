package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.shared.auth.ApiKeyLookupService;
import fr.cnrs.opentheso.utils.ToolsHelper;
import java.time.LocalDate;
import java.util.Optional;

import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Helper permettant de vérifier l'existence d'une clé API dans la table useres
 */
@Service
@Slf4j
public class ApiKeyService {

    private final UserRepository userRepository;
    private final ApiKeyLookupService apiKeyLookupService;

    public ApiKeyService(UserRepository userRepository, ApiKeyLookupService apiKeyLookupService) {
        this.userRepository = userRepository;
        this.apiKeyLookupService = apiKeyLookupService;
    }

    /**
     * Retourne l'utilisateur associé à la clé API, ou Optional.empty() si elle est inconnue ou expirée.
     * La recherche (empreinte indexée, contrôle d'expiration) est celle de {@link ApiKeyLookupService}.
     */
    public Optional<User> findUserByApiKey(String apiKeyHeader) {
        return lookup(apiKeyHeader).filter(ApiKeyLookupService.ApiKeyMatch::valid).map(match -> {
            User user = new User();
            user.setId(match.userId());
            user.setUsername(match.username());
            return user;
        });
    }

    /** Compte propriétaire de la clé et état de la clé (pour distinguer une clé expirée d'une clé inconnue). */
    public Optional<ApiKeyLookupService.ApiKeyMatch> lookup(String apiKeyHeader) {
        return apiKeyLookupService.lookup(apiKeyHeader);
    }

    /**
     * Génère une clé API d'une longueur choisie avec le header voulu
     * @param header le header de la clé API
     * @param keyLength la longueur de la clé API
     * @return la clé API
     */
    public String generateApiKey(String header, int keyLength) {

        final String timestamp = String.valueOf(System.currentTimeMillis());
        final String randomKey = header.length() + timestamp.length() < keyLength ? ToolsHelper.getNewId(keyLength-header.length()-timestamp.length(), false, false) : "";
        String apiKey= header + timestamp + randomKey;
        if (apiKey.length() > keyLength) {
            apiKey = apiKey.substring(0, keyLength);
        }
        return apiKey;
    }

    /**
     * @param apiKey Clé d'API à sauvegarder
     * @param idUser id de l'utilisateur propriétaire de la clé
     * @return True si la clé a bien été sauvegardé, False sinon
     */
    public boolean saveApiKey(String apiKey, int idUser) {

        log.debug("Mise à jour du l'API Key de l'utilisateur {}", idUser);
        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            log.error("Aucun utilisateur n'est trouvé avec l'id {}", idUser);
            return false;
        }

        user.get().setApiKey(apiKey);
        user.get().setKeyNeverExpire(true);
        userRepository.save(user.get());
        log.debug("Mise à jour du APiKey terminé avec sucée");
        return true;
    }

    /**
     * Vérifie si la clé est valide, existe et n'est pas expirée
     */
    public ApiKeyState checkApiKey(String apiKey){

        if (apiKey == null || apiKey.isEmpty()) return ApiKeyState.EMPTY;

        var user = userRepository.findByApiKey(MD5Password.getEncodedPassword(apiKey));
        if (user.isEmpty()) {
            // Jamais la clé dans le journal : une clé presque juste y serait lisible
            log.warn("Clé API inconnue ({} caractères)", apiKey.length());
            return ApiKeyState.INVALID;
        }

        if (!user.get().getKeyNeverExpire()) {
            if (user.get().getKeyExpiresAt() == null)
                return ApiKeyState.INVALID;

            if (LocalDate.now().isAfter(user.get().getKeyExpiresAt()))
                return ApiKeyState.EXPIRED;
        }
        return ApiKeyState.VALID;
    }

    /**
     * Renvoie une réponse adaptée si l'état de la clé n'est pas VALID
     * @param state Etat de la clé API
     * @return Response avec le message erreur correspondant
     */
    public ResponseEntity<Object> errorResponse(ApiKeyState state) {
        int code = 0;
        String msg = switch (state) {
            case EMPTY -> {
                code = Response.Status.UNAUTHORIZED.getStatusCode();
                yield "No API key given";
            }
            case DATABASE_UNAVAILABLE -> {
                code = Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
                yield "Database unavailable";
            }
            case INVALID -> {
                code = Response.Status.FORBIDDEN.getStatusCode();
                yield "API key is invalid";
            }
            case SQL_ERROR -> {
                code = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
                yield "Server internal error";
            }
            case EXPIRED -> {
                code = Response.Status.UNAUTHORIZED.getStatusCode();
                yield "API key is expired";
            }
            default -> null;
        };

        return ResponseEntity.status(code).contentType(MediaType.APPLICATION_JSON).body(msg);
    }
    
}
