package fr.cnrs.opentheso.v2.publicapi.graphql.api;

import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptLabel;
import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptNode;
import fr.cnrs.opentheso.v2.publicapi.graphql.service.PublicGraphQlConceptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(PublicConceptGraphQlController.class)
class PublicConceptGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private PublicGraphQlConceptService publicGraphQlConceptService;

    @Test
    void publicConcept_resolvesConceptThroughSchema() {
        var node = new PublicConceptNode(
                "C1", "TH1", "Chat", "N1", "concept", "C", "ark1", "2024", "2025",
                List.of(new PublicConceptLabel("en", "Cat")),
                List.of("Minou"), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of()
        );
        when(publicGraphQlConceptService.getConcept("TH1", "C1", "fr")).thenReturn(Optional.of(node));

        String conceptId = graphQlTester.document("""
                query {
                  publicConcept(thesaurusId: "TH1", conceptId: "C1", lang: "fr") {
                    conceptId
                    prefLabel
                    synonyms
                    translations { lang value }
                  }
                }
                """)
                .execute()
                .path("publicConcept.conceptId").entity(String.class).get();
        assertEquals("C1", conceptId);
    }

    @Test
    void publicConcept_returnsNullWhenConceptNotFound() {
        when(publicGraphQlConceptService.getConcept("TH1", "C9", "fr")).thenReturn(Optional.empty());

        var response = graphQlTester.document("""
                query {
                  publicConcept(thesaurusId: "TH1", conceptId: "C9", lang: "fr") {
                    conceptId
                  }
                }
                """)
                .execute();
        response.path("publicConcept").valueIsNull();
        assertNotNull(response);
    }

    @Test
    void publicConceptSearch_resolvesListThroughSchema() {
        var node = new PublicConceptNode(
                "C1", "TH1", "Chat", "N1", "concept", "C", "ark1", "2024", "2025",
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of()
        );
        when(publicGraphQlConceptService.searchConcepts(any(), any(), any(), any())).thenReturn(List.of(node));

        String conceptId = graphQlTester.document("""
                query {
                  publicConceptSearch(thesaurusId: "TH1", value: "chat") {
                    conceptId
                    prefLabel
                  }
                }
                """)
                .execute()
                .path("publicConceptSearch[0].conceptId").entity(String.class).get();
        assertEquals("C1", conceptId);
    }
}
