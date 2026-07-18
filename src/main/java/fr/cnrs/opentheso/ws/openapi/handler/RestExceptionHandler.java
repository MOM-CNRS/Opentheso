package fr.cnrs.opentheso.ws.openapi.handler;

import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.exception.SettingAccessDeniedException;
import fr.cnrs.opentheso.v2.shared.exception.ModuleAccessDeniedException;
import fr.cnrs.opentheso.v2.candidat.exception.CandidateNotFoundException;
import fr.cnrs.opentheso.v2.graph.exception.GraphViewNotFoundException;
import fr.cnrs.opentheso.v2.graph.exception.InvalidGraphDataException;
import fr.cnrs.opentheso.v2.toolbox.exception.ToolboxAccessDeniedException;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.user.exception.ApiKeyRegenerationException;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.v2.admin.exception.AdminAccessDeniedException;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.exception.ProjectNotFoundException;
import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.ws.openapi.exception.*;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;

@RestControllerAdvice
@Order(1)
public class RestExceptionHandler {

    @ExceptionHandler(ThesaurusNotFoundException.class)
    public ProblemDetail handleThesaurusNotFound(ThesaurusNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND); // 404
        problem.setTitle("Thesaurus Not Found");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "THESAURUS_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(ConceptAlreadyExistsException.class)
    public ProblemDetail handleConceptNotFound(ConceptAlreadyExistsException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT); // 404
        problem.setTitle("Concept Conflit");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "CONCEPT_ALREADY_EXISTS");
        return problem;
    }

    @ExceptionHandler(LabelAlreadyExistsException.class)
    public ProblemDetail handleLabelConflict(LabelAlreadyExistsException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT); // 404
        problem.setTitle("Label Conflit");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "LABEL_ALREADY_EXISTS");
        return problem;
    }

    @ExceptionHandler(NotationAlreadyExistsException.class)
    public ProblemDetail handleNotationConflict(NotationAlreadyExistsException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT); // 409
        problem.setTitle("Notation Conflict");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "NOTATION_ALREADY_EXISTS");
        return problem;
    }

    @ExceptionHandler(ApiKeyMissingException.class)
    public ProblemDetail handleMissingApiKey(ApiKeyMissingException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "API_KEY_MISSING");
        return problem;
    }

    @ExceptionHandler(ApiKeyInvalidException.class)
    public ProblemDetail handleInvalidApiKey(ApiKeyInvalidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(
                ex.getState() == ApiKeyState.EXPIRED ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN
        );
        problem.setTitle("Unauthorized");
        problem.setDetail("Clé API " + ex.getState().name().toLowerCase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "API_KEY_" + ex.getState().name());
        return problem;
    }

    @ExceptionHandler(UserCantWriteOnThesaurusException.class)
    public ProblemDetail handleFailPermission(UserCantWriteOnThesaurusException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("The user does not have write access to this thesaurus.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "No right");
        return problem;
    }

    @ExceptionHandler(InvalidProfileDataException.class)
    public ProblemDetail handleInvalidProfile(InvalidProfileDataException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Invalid profile data");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "INVALID_PROFILE_DATA");
        return problem;
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(InvalidPasswordException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid password");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "INVALID_PASSWORD");
        return problem;
    }

    @ExceptionHandler(ApiKeyRegenerationException.class)
    public ProblemDetail handleApiKeyRegeneration(ApiKeyRegenerationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("API key regeneration refused");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "API_KEY_REGENERATION_REFUSED");
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("User not found");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "USER_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ProblemDetail handleProjectNotFound(ProjectNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Project not found");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "PROJECT_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(ProjectAccessDeniedException.class)
    public ProblemDetail handleProjectAccessDenied(ProjectAccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Project access denied");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "PROJECT_ACCESS_DENIED");
        return problem;
    }

    @ExceptionHandler(InvalidProjectDataException.class)
    public ProblemDetail handleInvalidProjectData(InvalidProjectDataException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid project data");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "INVALID_PROJECT_DATA");
        return problem;
    }

    @ExceptionHandler(AdminAccessDeniedException.class)
    public ProblemDetail handleAdminAccessDenied(AdminAccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Admin access denied");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "ADMIN_ACCESS_DENIED");
        return problem;
    }

    @ExceptionHandler(SettingAccessDeniedException.class)
    public ProblemDetail handleSettingAccessDenied(SettingAccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Setting access denied");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "SETTING_ACCESS_DENIED");
        return problem;
    }

    @ExceptionHandler(InvalidSettingDataException.class)
    public ProblemDetail handleInvalidSettingData(InvalidSettingDataException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid setting data");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "INVALID_SETTING_DATA");
        return problem;
    }

    @ExceptionHandler(ModuleAccessDeniedException.class)
    public ProblemDetail handleModuleAccessDenied(ModuleAccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Module access denied");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "MODULE_ACCESS_DENIED");
        return problem;
    }

    @ExceptionHandler(GraphViewNotFoundException.class)
    public ProblemDetail handleGraphViewNotFound(GraphViewNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Graph view not found");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "GRAPH_VIEW_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(InvalidGraphDataException.class)
    public ProblemDetail handleInvalidGraphData(InvalidGraphDataException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Invalid graph data");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "INVALID_GRAPH_DATA");
        return problem;
    }

    @ExceptionHandler(CandidateNotFoundException.class)
    public ProblemDetail handleCandidateNotFound(CandidateNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Candidate not found");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "CANDIDATE_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(ToolboxAccessDeniedException.class)
    public ProblemDetail handleToolboxAccessDenied(ToolboxAccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Toolbox access denied");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "TOOLBOX_ACCESS_DENIED");
        return problem;
    }

    @ExceptionHandler(InvalidToolboxDataException.class)
    public ProblemDetail handleInvalidToolboxData(InvalidToolboxDataException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid toolbox data");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "INVALID_TOOLBOX_DATA");
        return problem;
    }

    @ExceptionHandler(PublicResourceNotFoundException.class)
    public ProblemDetail handlePublicResourceNotFound(PublicResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Resource not found");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "PUBLIC_RESOURCE_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((first, second) -> first + "; " + second)
                .orElse("Données invalides");
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation error");
        problem.setDetail(detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        problem.setProperty("errorCode", "VALIDATION_ERROR");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        problem.setDetail("Une erreur interne est survenue");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now());
        return problem;
    }
}