package fr.cnrs.opentheso.shared.service;

import fr.cnrs.opentheso.shared.repository.LegalPageQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LegalPageServiceTest {

    @Mock
    private LegalPageQueryRepository repository;

    private LegalPageService service;

    @BeforeEach
    void setUp() {
        service = new LegalPageService(repository);
    }

    @Test
    void save_keepsRichTextAndDropsScripts() {
        assertTrue(service.save(
                "Mentions",
                "FR",
                "  Mentions   légales  ",
                "<h2>Éditeur</h2><p>Texte <strong>libre</strong></p><script>alert(1)</script><ul><li>un</li></ul>"));

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(repository).upsert(
                org.mockito.ArgumentMatchers.eq("mentions"),
                org.mockito.ArgumentMatchers.eq("fr"),
                org.mockito.ArgumentMatchers.eq("Mentions légales"),
                html.capture());
        assertTrue(html.getValue().contains("Éditeur"));
        assertTrue(html.getValue().contains("<strong>libre</strong>"));
        assertTrue(html.getValue().contains("<li>un</li>"));
        assertFalse(html.getValue().toLowerCase().contains("script"));
    }

    @Test
    void save_rejectsAnUnknownPageOrABlankTitle() {
        assertFalse(service.save("autre", "fr", "Titre", "<p>x</p>"));
        assertFalse(service.save("mentions", "fr", "   ", "<p>x</p>"));
        verify(repository, never()).upsert(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void titleKey_followsThePage() {
        assertEquals("legal.privacy", LegalPageService.titleKey("donnees"));
        assertEquals("legal.accessibility", LegalPageService.titleKey("accessibilite"));
    }
}
