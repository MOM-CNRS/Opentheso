package fr.cnrs.opentheso.publicapi.graphql.api;

import fr.cnrs.opentheso.publicapi.graphql.api.dto.PublicConceptLabel;
import fr.cnrs.opentheso.publicapi.graphql.api.dto.PublicConceptNode;
import fr.cnrs.opentheso.publicapi.graphql.service.PublicGraphQlConceptService;
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
    private fr.cnrs.opentheso.setting.service.ThesaurusApiSwitch thesaurusApiSwitch;

    @MockitoBean
    private fr.cnrs.opentheso.ws.api.RestThesaurusVisibility restThesaurusVisibility;

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

    @org.junit.jupiter.api.BeforeEach
    void allowVisibility() {
        org.mockito.Mockito.when(restThesaurusVisibility.explain(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(fr.cnrs.opentheso.ws.api.RestThesaurusAccess.ALLOWED);
    }

    @Test
    void publicConcept_isRefusedForPrivateThesaurusWithoutKey() {
        // Sans clé : Mockito renvoie 0 pour un Integer, d'où any()
        org.mockito.Mockito.when(restThesaurusVisibility.explain(any(), org.mockito.ArgumentMatchers.eq("th5")))
                .thenReturn(fr.cnrs.opentheso.ws.api.RestThesaurusAccess.PRIVATE);

        graphQlTester.document("{ publicConcept(thesaurusId: \"th5\", conceptId: \"1\") { __typename } }")
                .execute()
                .errors()
                .satisfy(errors -> org.junit.jupiter.api.Assertions.assertEquals(
                        fr.cnrs.opentheso.ws.api.RestThesaurusAccess.PRIVATE.message(), errors.get(0).getMessage()));
        org.mockito.Mockito.verifyNoInteractions(publicGraphQlConceptService);
    }

    @Test
    void publicConcept_isRefusedWhenThesaurusApiIsDisabled() {
        org.mockito.Mockito.when(thesaurusApiSwitch.anyDisabled(java.util.List.of("th17"))).thenReturn(true);

        graphQlTester.document("{ publicConcept(thesaurusId: \"th17\", conceptId: \"1\") { __typename } }")
                .execute()
                .errors()
                .satisfy(errors -> org.junit.jupiter.api.Assertions.assertEquals(
                        "L'API est désactivée pour ce thésaurus.", errors.get(0).getMessage()));
        org.mockito.Mockito.verifyNoInteractions(publicGraphQlConceptService);
    }
}
