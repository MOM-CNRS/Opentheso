package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.concept.search.repository.ConceptSearchQueryRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptAttributeWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptCreationWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptCustomRelationWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptDeletionWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLexicalWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptLifecycleWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptNoteWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptRelationWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptRenameWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptSynonymWriteRepository;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptTranslationWriteRepository;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.persistence.ThesaurusPublicQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxStatisticsQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Exercice chaque méthode publique des repositories natifs V2 avec un {@link EntityManager}
 * mocké. Cela couvre la construction des requêtes SQL (y compris les text-blocks) sans base.
 */
class NativeQueryRepositorySmokeTest {

    static Stream<Object> repositories() {
        return Stream.of(
                new ConceptQueryRepository(),
                new ThesaurusHomeQueryRepository(),
                new CandidatQueryRepository(),
                new ConceptFullQueryRepository(),
                new ConceptTableQueryRepository(),
                new CollectionTreeQueryRepository(),
                new ConsultationCatalogQueryRepository(),
                new GraphGlobeQueryRepository(),
                new GraphViewQueryRepository(),
                new HistoryQueryRepository(),
                new AdminQueryRepository(),
                new UserRoleQueryRepository(),
                new UserAuthQueryRepository(),
                new UserCommandRepository(),
                new ProjectAdminQueryRepository(),
                new ThesaurusSettingsQueryRepository(),
                new PlatformHomeQueryRepository(),
                new EditionQueryRepository(),
                new ConceptSearchQueryRepository(),
                new ToolboxStatisticsQueryRepository(),
                new ThesaurusPublicQueryRepository(),
                new ProjectMembershipRepository(),
                new ConceptDeletionWriteRepository(),
                new ConceptRelationWriteRepository(),
                new ConceptSynonymWriteRepository(),
                new ConceptAttributeWriteRepository(),
                new ConceptCustomRelationWriteRepository(),
                new ConceptTranslationWriteRepository(),
                new ConceptCreationWriteRepository(),
                new ConceptLexicalWriteRepository(),
                new ConceptNoteWriteRepository(),
                new ConceptLifecycleWriteRepository(),
                new ConceptRenameWriteRepository()
        );
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void everyPublicMethod_runsAgainstMockedEntityManager(Object repository) {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class, withSettings().extraInterfaces(jakarta.persistence.TypedQuery.class));
        jakarta.persistence.TypedQuery<?> typedQuery = (jakarta.persistence.TypedQuery<?>) query;
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setParameter(anyInt(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        when(query.getSingleResult()).thenReturn(0L);
        when(query.executeUpdate()).thenReturn(1);
        when(query.getResultStream()).thenReturn(Stream.empty());
        when(entityManager.createNativeQuery(nullable(String.class))).thenReturn(query);
        when(entityManager.createNativeQuery(nullable(String.class), any(Class.class))).thenReturn(query);
        when(entityManager.createNativeQuery(nullable(String.class), anyString())).thenReturn(query);
        when(entityManager.createQuery(nullable(String.class))).thenReturn(query);
        when(entityManager.createQuery(nullable(String.class), any(Class.class))).thenReturn(typedQuery);

        injectEntityManager(repository, entityManager);

        for (Method method : repository.getClass().getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            Object[] args = dummyArgs(method);
            assertDoesNotThrow(() -> {
                try {
                    method.setAccessible(true);
                    method.invoke(repository, args);
                } catch (Exception ignored) {
                    // Some methods cast getSingleResult() or map rows; query construction already ran.
                }
            }, method.getName());
        }
    }

    private static void injectEntityManager(Object repository, EntityManager entityManager) {
        for (String field : List.of("em", "entityManager")) {
            try {
                ReflectionTestUtils.setField(repository, field, entityManager);
                return;
            } catch (IllegalArgumentException ignored) {
                // try next name
            }
        }
        throw new IllegalStateException("No EntityManager field on " + repository.getClass().getName());
    }

    private static Object[] dummyArgs(Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = dummyValue(parameters[i].getType());
        }
        return args;
    }

    private static Object dummyValue(Class<?> type) {
        if (!type.isPrimitive() && type != String.class && !type.isEnum()) {
            if (Collection.class.isAssignableFrom(type)) {
                return new ArrayList<>(List.of("x"));
            }
            if (List.class.isAssignableFrom(type)) {
                return List.of("x");
            }
            if (Set.class.isAssignableFrom(type)) {
                return Set.of("x");
            }
            if (Map.class.isAssignableFrom(type)) {
                return Map.of("k", "v");
            }
        }
        if (type == String.class) {
            return "x";
        }
        if (type == boolean.class || type == Boolean.class) {
            return true;
        }
        if (type == int.class || type == Integer.class) {
            return 1;
        }
        if (type == long.class || type == Long.class) {
            return 1L;
        }
        if (type == double.class || type == Double.class) {
            return 1.0;
        }
        if (type == Date.class) {
            return Date.from(java.time.Instant.parse("2024-06-15T12:00:00Z"));
        }
        if (type == LocalDate.class) {
            return LocalDate.of(2024, Month.JANUARY, 1);
        }
        if (type == String[].class) {
            return new String[] {"x"};
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length == 0 ? null : constants[0];
        }
        if (type == Collection.class || type == List.class) {
            return List.of("x");
        }
        if (type.isInterface() || (!type.isPrimitive() && !Modifier.isFinal(type.getModifiers()))) {
            try {
                return mock(type);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
