package fr.cnrs.opentheso.v2.concept.policy;

import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusConsultationAccessPolicyTest {

    @Mock
    private ThesaurusRepository thesaurusRepository;

    private ThesaurusConsultationAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ThesaurusConsultationAccessPolicy(thesaurusRepository);
    }

    @Test
    void canConsult_allowsWhenListedInCatalog() {
        assertTrue(policy.canConsult("TH1", true));
    }

    @Test
    void canConsult_allowsPublicWhenNotListed() {
        when(thesaurusRepository.isPrivateThesaurus("TH1")).thenReturn(false);

        assertTrue(policy.canConsult("TH1", false));
    }

    @Test
    void canConsult_deniesPrivateWhenNotListed() {
        when(thesaurusRepository.isPrivateThesaurus("TH1")).thenReturn(true);

        assertFalse(policy.canConsult("TH1", false));
    }

    @Test
    void canConsult_deniesUnknownThesaurus() {
        when(thesaurusRepository.isPrivateThesaurus("TH1")).thenReturn(null);

        assertFalse(policy.canConsult("TH1", false));
    }

    @Test
    void canConsult_deniesBlankId() {
        assertFalse(policy.canConsult(" ", false));
    }
}
