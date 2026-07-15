package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusScopedAuthSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptAuthSupportTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;
    @Mock
    private ThesaurusScopedAuthSupport thesaurusScopedAuthSupport;

    private ConceptAuthSupport support;

    @BeforeEach
    void setUp() {
        support = new ConceptAuthSupport(apiKeyAuthenticationService, thesaurusScopedAuthSupport);
    }

    @Test
    void resolveUserId_delegatesToAuthenticationService() {
        when(apiKeyAuthenticationService.resolveUserId("key", "legacy")).thenReturn(9);

        support.resolveUserId("key", "legacy");

        verify(apiKeyAuthenticationService).resolveUserId("key", "legacy");
    }

    @Test
    void requireThesaurusContributor_delegatesToScopedAuthSupport() {
        support.requireThesaurusContributor(9, "TH1");

        verify(thesaurusScopedAuthSupport).requireThesaurusContributor(9, "TH1");
    }
}
