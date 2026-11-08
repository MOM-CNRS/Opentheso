package fr.cnrs.opentheso.concept.api;

import fr.cnrs.opentheso.concept.model.ConceptCorpusLinkItem;
import fr.cnrs.opentheso.concept.model.CorpusSearchContext;
import fr.cnrs.opentheso.concept.policy.ThesaurusConsultationAccessPolicy;
import fr.cnrs.opentheso.concept.service.ConceptReadService;
import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.shared.session.AuthenticatedUserSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptExtrasApiControllerTest {

    @Mock
    private ConceptReadService conceptReadService;
    @Mock
    private ThesaurusConsultationAccessPolicy consultationAccessPolicy;
    @Mock
    private RightsService rightsService;
    @Mock
    private AuthenticatedUserSource authenticatedUserSource;

    private ConceptExtrasApiController controller;

    @BeforeEach
    void setUp() {
        controller = new ConceptExtrasApiController(
                conceptReadService, consultationAccessPolicy, rightsService, authenticatedUserSource);
    }

    @Test
    void branchCount_rejectsBlankIds() {
        var response = controller.branchCount(" ", "c1");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(conceptReadService, never()).countBranchConcepts(" ", "c1");
    }

    @Test
    void branchCount_hidesPrivateThesaurusFromGuest() {
        when(consultationAccessPolicy.canConsult("th1", false)).thenReturn(false);
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.empty());

        var response = controller.branchCount("th1", "c1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(conceptReadService, never()).countBranchConcepts("th1", "c1");
    }

    @Test
    void branchCount_allowsMemberOfPrivateThesaurus() {
        when(consultationAccessPolicy.canConsult("th1", false)).thenReturn(false);
        when(authenticatedUserSource.getUserId()).thenReturn(Optional.of(9));
        when(rightsService.can(9, Permission.SUPER_ADMIN)).thenReturn(false);
        when(rightsService.roleOnThesaurus(9, "th1")).thenReturn(Optional.of(2));
        when(conceptReadService.countBranchConcepts("th1", "c1")).thenReturn(14);

        var response = controller.branchCount("th1", "c1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(14, response.getBody().count());
    }

    @Test
    void corpusLinks_returnsResolvedLinksForPublicThesaurus() {
        when(consultationAccessPolicy.canConsult("th1", false)).thenReturn(true);
        when(conceptReadService.loadCorpusSearchContext("th1", "c1", "fr"))
                .thenReturn(Optional.of(new CorpusSearchContext("c1", "ark:/1", "Bronze")));
        when(conceptReadService.loadCorpusLinks("th1", new CorpusSearchContext("c1", "ark:/1", "Bronze")))
                .thenReturn(List.of(new ConceptCorpusLinkItem("Pactols", "https://example.test/1", 3, false, true)));

        var response = controller.corpusLinks("th1", "c1", "fr");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().links().size());
        var link = response.getBody().links().get(0);
        assertEquals("Pactols", link.name());
        assertEquals("https://example.test/1", link.uri());
        assertEquals(3, link.count());
        assertTrue(!link.onlyUri());
    }

    @Test
    void corpusLinks_returnsNotFoundWhenConceptIsMissing() {
        when(consultationAccessPolicy.canConsult("th1", false)).thenReturn(true);
        when(conceptReadService.loadCorpusSearchContext("th1", "missing", "fr")).thenReturn(Optional.empty());

        var response = controller.corpusLinks("th1", "missing", "fr");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
