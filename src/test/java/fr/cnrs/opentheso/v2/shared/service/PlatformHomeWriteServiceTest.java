package fr.cnrs.opentheso.v2.shared.service;

import fr.cnrs.opentheso.v2.shared.repository.PlatformHomeQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformHomeWriteServiceTest {

    @Mock
    private PlatformHomeQueryRepository repository;
    @Mock
    private PlatformHomeReadService readService;

    private PlatformHomeWriteService service;

    @BeforeEach
    void setUp() {
        service = new PlatformHomeWriteService(repository, readService);
        ReflectionTestUtils.setField(service, "defaultWorkLanguage", "fr");
    }

    @Test
    void loadHomePageHtml_delegatesToReadService() {
        when(readService.loadHomePageHtml("en")).thenReturn("<p>Hi</p>");

        assertEquals("<p>Hi</p>", service.loadHomePageHtml("en"));
    }

    @Test
    void saveHomePageHtml_usesDefaultLanguageWhenBlank() {
        when(repository.upsertHomePageHtml(eq("fr"), anyString())).thenReturn(true);

        assertTrue(service.saveHomePageHtml(" ", "<p>Accueil</p>"));
        verify(repository).upsertHomePageHtml(eq("fr"), anyString());
    }

    @Test
    void googleAnalytics_roundTrip() {
        when(repository.findGoogleAnalyticsCode()).thenReturn(Optional.of("G-TEST"));

        assertEquals("G-TEST", service.loadGoogleAnalyticsCode());
        service.saveGoogleAnalyticsCode("G-NEW");
        verify(repository).saveGoogleAnalyticsCode("G-NEW");
    }
}
