package fr.cnrs.opentheso.v2.publicapi.graphql.api;

import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptLabel;
import fr.cnrs.opentheso.v2.publicapi.graphql.api.dto.PublicConceptNode;
import fr.cnrs.opentheso.v2.publicapi.graphql.service.PublicGraphQlConceptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(PublicConceptGraphQlController.class)
class PublicConceptGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
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

        graphQlTester.document("""
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
                .path("publicConcept.conceptId").entity(String.class).isEqualTo("C1")
                .path("publicConcept.prefLabel").entity(String.class).isEqualTo("Chat")
                .path("publicConcept.synonyms").entityList(String.class).containsExactly("Minou")
                .path("publicConcept.translations[0].lang").entity(String.class).isEqualTo("en");
    }

    @Test
    void publicConcept_returnsNullWhenConceptNotFound() {
        when(publicGraphQlConceptService.getConcept("TH1", "C9", "fr")).thenReturn(Optional.empty());

        graphQlTester.document("""
                query {
                  publicConcept(thesaurusId: "TH1", conceptId: "C9", lang: "fr") {
                    conceptId
                  }
                }
                """)
                .execute()
                .path("publicConcept").valueIsNull();
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

        graphQlTester.document("""
                query {
                  publicConceptSearch(thesaurusId: "TH1", value: "chat") {
                    conceptId
                    prefLabel
                  }
                }
                """)
                .execute()
                .path("publicConceptSearch[0].conceptId").entity(String.class).isEqualTo("C1");
    }
}
