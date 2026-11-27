package fr.cnrs.opentheso.ws.openapi.v1.routes.users;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.users.NodeUserResource;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.services.UserRoleGroupService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;



@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/openapi/v1/users")
@CrossOrigin(methods = { RequestMethod.GET , RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT })
@Tag(name = "User", description = "Gestion des utilisateurs")
public class UserController {

    private final UserRepository userRepository;
    private final ApiKeyService apiKeyHelper;
    private final UserRoleGroupService userRoleGroupService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptLimiter loginAttemptLimiter;


    @PostMapping("/authentification")
    @Operation(summary = "Authentification d'un utilisateur")
    public ResponseEntity createUser(@RequestBody @Valid AuthentificationDto authentificationDto) {

        String login = authentificationDto.getLogin();
        if (loginAttemptLimiter.isBlocked(login)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Trop de tentatives de connexion. Réessayez plus tard.");
        }
        // Mot de passe vérifié en BCrypt (MD5 historique migré), comme la connexion de l'interface
        var user = userService.getUserByLoginAndPassword(login, authentificationDto.getPassword());
        if (ObjectUtils.isEmpty(user)) {
            loginAttemptLimiter.recordFailure(login);
            return ResponseEntity.notFound().build();
        }
        loginAttemptLimiter.recordSuccess(login);

        if (!user.isSuperAdmin()) {
            user.setThesorusList(userService.getThesaurusOfUser(user.getIdUser()));
        }
        if (StringUtils.isEmpty(user.getApiKey())) {
            var apiKeyValue = apiKeyHelper.generateApiKey("ot_", 64);
            if(!apiKeyHelper.saveApiKey(MD5Password.getEncodedPassword(apiKeyValue), user.getIdUser())){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur pendant la génération du API Key !!!");
            }
            user.setApiKey(apiKeyValue);
        }
        return ResponseEntity.ok().body(user);
    }


    @PostMapping
    @Operation(summary = "Créer un nouveau utilisateur")
    public ResponseEntity createUser(@RequestHeader(value = "API-KEY", required = false) String apiKey,
                                     @RequestBody @Valid UserDto userDto) {

        var requester = getUser(apiKey);
        if (requester == null) {
            return unauthorized();
        }
        if (!isSuperAdmin(requester)) {
            return forbidden();
        }
        if (StringUtils.isBlank(userDto.getPassword())) {
            return ResponseEntity.badRequest().body("Le mot de passe est obligatoire.");
        }

        var userCreated = userRepository.save(User.builder().username(userDto.getLogin())
                .mail(userDto.getMail())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .isSuperAdmin(userDto.isSuperAdmin())
                .alertMail(userDto.isAlertMail())
                .build());

        var apiKeyValue = apiKeyHelper.generateApiKey("ot_", 64);
        if(!apiKeyHelper.saveApiKey(MD5Password.getEncodedPassword(apiKeyValue), userCreated.getId())){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur pendant la génération du API Key !!!");
        }

        if (!ObjectUtils.isEmpty(userDto.getIdRole()) && !ObjectUtils.isEmpty(userDto.getIdThesaurus())) {
            var idGroup = userService.getGroupOfThisThesaurus(userDto.getIdThesaurus());
            userRoleGroupService.addUserRoleOnGroup(userCreated.getId(), userDto.getIdRole(), idGroup);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(publicView(userCreated, apiKeyValue));
    }


    @DeleteMapping("/{idUser}")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity deleteUser(@RequestHeader(value = "API-KEY", required = false) String apiKey,
                                     @PathVariable("idUser") Integer idUser) {

        var requester = getUser(apiKey);
        if (requester == null) {
            return unauthorized();
        }
        if (!isSuperAdmin(requester)) {
            return forbidden();
        }
        var userToRemove = userRepository.findById(idUser);
        if (userToRemove.isPresent()) {
            userRepository.delete(userToRemove.get());
            return ResponseEntity.status(HttpStatus.OK).body("");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
    }


    /**
     * Un super-administrateur modifie tout compte. Un utilisateur ne modifie que le sien, sans toucher
     * à son rôle ni à son état actif : sinon une simple clé suffisait à reprendre n'importe quel compte.
     */
    @PutMapping("/{idUser}")
    @Operation(summary = "Modifier un utilisateur")
    public ResponseEntity updateUser(@RequestHeader(value = "API-KEY", required = false) String apiKey,
                                     @PathVariable("idUser") Integer idUser,
                                     @RequestBody @Valid UserDto userDto) {

        var requester = getUser(apiKey);
        if (requester == null) {
            return unauthorized();
        }
        boolean superAdmin = isSuperAdmin(requester);
        if (!superAdmin && !isSelf(requester, idUser)) {
            return forbidden();
        }
        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }

        user.get().setMail(userDto.getMail());
        user.get().setUsername(userDto.getLogin());
        if (StringUtils.isNotBlank(userDto.getPassword())) {
            user.get().setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        user.get().setAlertMail(userDto.isAlertMail());
        if (superAdmin) {
            user.get().setActive(userDto.isActive());
        }
        userRepository.save(user.get());

        if (superAdmin && !ObjectUtils.isEmpty(userDto.getIdRole()) && !ObjectUtils.isEmpty(userDto.getIdProject())) {
            var idGroup = userService.getGroupOfThisThesaurus(userDto.getIdThesaurus());
            userRoleGroupService.updateUserRoleOnGroup(user.get().getId(), userDto.getIdRole(), idGroup);
        }
        return ResponseEntity.status(HttpStatus.OK).body("");
    }

    @GetMapping
    @Operation(summary = "Rechercher des utilisateurs")
    public ResponseEntity searchUser(@RequestHeader(value = "API-KEY", required = false) String apiKey,
                                     @ParameterObject NodeUserResource userResource) {

        var requester = getUser(apiKey);
        if (requester == null) {
            return unauthorized();
        }
        if (!isSuperAdmin(requester)) {
            return forbidden();
        }
        var users = userService.searchUserByCriteria(userResource.getMail(), userResource.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    /** Nouvelle clé : pour son propre compte, ou pour tout compte par un super-administrateur. */
    @PutMapping("/api-key/{idUser}")
    @Operation(summary = "Modifier un utilisateur")
    public ResponseEntity generateApiKey(@RequestHeader(value = "API-KEY", required = false) String apiKey,
                                         @PathVariable("idUser") Integer idUser) {

        var requester = getUser(apiKey);
        if (requester == null) {
            return unauthorized();
        }
        if (!isSuperAdmin(requester) && !isSelf(requester, idUser)) {
            return forbidden();
        }
        var user = userRepository.findById(idUser);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
        var apiKeyValue = apiKeyHelper.generateApiKey("ot_", 64);
        if(!apiKeyHelper.saveApiKey(MD5Password.getEncodedPassword(apiKeyValue), idUser)){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur pendant la génération du API Key !!!");
        }
        return ResponseEntity.status(HttpStatus.OK).body(publicView(user.get(), apiKeyValue));
    }

    private User getUser(String apiKey) {
        if (StringUtils.isBlank(apiKey)) {
            return null;
        }
        var keyState = apiKeyHelper.checkApiKey(apiKey);
        if (keyState != ApiKeyState.VALID){
            return null;
        }
        return userService.getUserByApiKey(MD5Password.getEncodedPassword(apiKey));
    }

    static boolean isSuperAdmin(User user) {
        return user != null && Boolean.TRUE.equals(user.getIsSuperAdmin());
    }

    static boolean isSelf(User user, Integer idUser) {
        return user != null && user.getId() != null && user.getId().equals(idUser);
    }

    /** Réponse sans l'empreinte du mot de passe ni la clé chiffrée ; la nouvelle clé n'est montrée qu'ici. */
    static Map<String, Object> publicView(User user, String newApiKey) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("mail", user.getMail());
        view.put("isSuperAdmin", Boolean.TRUE.equals(user.getIsSuperAdmin()));
        view.put("active", user.getActive());
        view.put("apiKey", newApiKey);
        return view;
    }

    private static ResponseEntity<String> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Clé API absente ou invalide");
    }

    private static ResponseEntity<String> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("");
    }
}
