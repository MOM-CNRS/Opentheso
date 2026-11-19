package fr.cnrs.opentheso.ws.graphql;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.DcTermsService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.services.security.AuthorizationService;
import fr.cnrs.opentheso.ws.graphql.dto.MutationResponse;
import fr.cnrs.opentheso.ws.graphql.dto.PrefLabelInput;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.UserCantWriteOnThesaurusException;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import graphql.GraphQLContext;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class ConceptMutationController {

    private final TermService termService;
    private final ApiKeyService apiKeyService;
    private final AuthorizationService authorizationService;
    private final ConceptService conceptService;
    private final DcTermsService dcTermsService;

    public ConceptMutationController(TermService termService, ApiKeyService apiKeyService,
                                     AuthorizationService authorizationService, ConceptService conceptService,
                                     DcTermsService dcTermsService) {
        this.termService = termService;
        this.apiKeyService = apiKeyService;
        this.authorizationService = authorizationService;
        this.conceptService = conceptService;
        this.dcTermsService = dcTermsService;
    }

    @MutationMapping
    public MutationResponse setPrefLabel(
            @Argument String idConcept,
            @Argument String idThesaurus,
            @Argument PrefLabelInput input,
            GraphQLContext context) {

        String lang = input.lang();
        String value = input.value();

        String apiKey = context.get("X-API-KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Clé API obligatoire pour cette mutation");
        }
        Optional<User> userOpt = apiKeyService.findUserByApiKey(apiKey);
        if (userOpt.isEmpty()) {
            throw new ApiKeyInvalidException(ApiKeyState.INVALID);
        }

        if (!authorizationService.canUserWrite(userOpt.get().getId(), idThesaurus)) {
            throw new UserCantWriteOnThesaurusException();
        }
        if (!termService.isTraductionExistOfConcept(idConcept, idThesaurus, lang)) {
            return new MutationResponse(false, "Label inexistant pour cette langue", idConcept, idThesaurus);
        }

        termService.updatePrefLabel(idConcept, idThesaurus, lang, value, userOpt.get().getId());
        conceptService.updateDateOfConcept(idThesaurus, idConcept, userOpt.get().getId());
        dcTermsService.updateConceptDcTerms(idConcept, idThesaurus, userOpt.get().getUsername());
        return new MutationResponse(true, "Label mis à jour avec succès", idConcept, idThesaurus);
    }
}