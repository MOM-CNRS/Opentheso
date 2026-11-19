package fr.cnrs.opentheso.ws.graphql.exception;

import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {

        // Ici on gère les exceptions liées à l'API Key
        if (ex instanceof IllegalArgumentException) {
            return GraphqlErrorBuilder.newError(env)
                    .message(ex.getMessage())   // message clair
                    .errorType(ErrorType.UNAUTHORIZED) // classification
                    .build();
        }
        // 🔹 Clé API invalide (utilisateur inexistant)
        if (ex instanceof ApiKeyInvalidException apiEx) {
            return GraphqlErrorBuilder.newError(env)
                    .message(apiEx.getMessage())
                    .errorType(ErrorType.UNAUTHORIZED)
                    .build();
        }
        if (ex instanceof UserCantWriteOnThesaurusException
                || ex instanceof fr.cnrs.opentheso.ws.openapi.exception.UserCantWriteOnThesaurusException) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = "Cet utilisateur ne peut pas modifier ce thésaurus.";
            }
            return GraphqlErrorBuilder.newError(env)
                    .message(message)
                    .errorType(ErrorType.UNAUTHORIZED)
                    .build();
        }
        // Retourne null pour les autres exceptions → elles seront gérées par défaut
        return null;
    }
}
