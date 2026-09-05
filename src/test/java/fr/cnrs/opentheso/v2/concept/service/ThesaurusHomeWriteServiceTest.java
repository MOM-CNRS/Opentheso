package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.entites.ThesaurusHomePage;
import fr.cnrs.opentheso.repositories.ThesaurusHomePageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusHomeWriteServiceTest {

    @Mock
    private ThesaurusHomePageRepository repository;

    private ThesaurusHomeWriteService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusHomeWriteService(repository);
        ReflectionTestUtils.setField(service, "defaultWorkLanguage", "fr");
    }

    @Test
    void loadHtml_returnsEmptyWhenThesaurusBlank() {
        assertEquals("", service.loadHtml(" ", "fr"));
    }

    @Test
    void loadHtml_usesStoredPage() {
        var page = ThesaurusHomePage.builder().idTheso("TH1").lang("fr").htmlCode("<p>OK</p>").build();
        when(repository.findByIdThesoAndLang("TH1", "fr")).thenReturn(Optional.of(page));

        assertEquals("<p>OK</p>", service.loadHtml("TH1", null));
    }

    @Test
    void saveHtml_returnsFalseWhenThesaurusBlank() {
        assertFalse(service.saveHtml("", "fr", "<p>x</p>"));
    }

    @Test
    void saveHtml_upsertsNormalizedHtml() {
        when(repository.upsertHtmlCode(eq("TH1"), eq("en"), anyString())).thenReturn(1);

        assertTrue(service.saveHtml("TH1", "en", "<p>Hello</p>"));
        verify(repository).upsertHtmlCode(eq("TH1"), eq("en"), anyString());
    }
}
