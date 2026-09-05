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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformHomeReadServiceTest {

    @Mock
    private PlatformHomeQueryRepository repository;

    private PlatformHomeReadService service;

    @BeforeEach
    void setUp() {
        service = new PlatformHomeReadService(repository);
        ReflectionTestUtils.setField(service, "defaultWorkLanguage", "fr");
        ReflectionTestUtils.setField(service, "applicationVersion", "26.07.02");
    }

    @Test
    void loadHomePageHtml_fallsBackToDefaultLanguage() {
        when(repository.findHomePageHtml("en")).thenReturn(Optional.empty());
        when(repository.findHomePageHtml("fr")).thenReturn(Optional.of("<p>FR</p>"));

        assertEquals("<p>FR</p>", service.loadHomePageHtml("en"));
    }

    @Test
    void loadHomePageHtml_usesRequestedLanguage() {
        when(repository.findHomePageHtml("de")).thenReturn(Optional.of("<p>DE</p>"));

        assertEquals("<p>DE</p>", service.loadHomePageHtml("DE"));
    }

    @Test
    void getGoogleAnalyticsCode_returnsEmptyWhenMissing() {
        when(repository.findGoogleAnalyticsCode()).thenReturn(Optional.empty());

        assertEquals("", service.getGoogleAnalyticsCode());
    }

    @Test
    void getApplicationVersion_returnsConfiguredValue() {
        assertEquals("26.07.02", service.getApplicationVersion());
    }
}
