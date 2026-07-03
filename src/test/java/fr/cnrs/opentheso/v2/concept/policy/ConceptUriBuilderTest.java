package fr.cnrs.opentheso.v2.concept.policy;

import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConceptUriBuilderTest {

    @Test
    void buildConceptUri_usesArkWhenConfigured() {
        var preferences = mock(ThesaurusPreferences.class);
        when(preferences.exportUriType()).thenReturn(ExportUriType.ARK);
        when(preferences.originalUri()).thenReturn("https://ark.example.com");
        when(preferences.cheminSite()).thenReturn("https://site.example.com");

        String uri = ConceptUriBuilder.buildConceptUri(
                preferences,
                "http://localhost/app",
                "C1",
                "TH1",
                "ark:/123",
                "",
                ""
        );

        assertEquals("https://ark.example.com/ark:/123", uri);
    }

    @Test
    void buildConceptUri_usesHandleWhenConfigured() {
        var preferences = mock(ThesaurusPreferences.class);
        when(preferences.exportUriType()).thenReturn(ExportUriType.HANDLE);
        when(preferences.cheminSite()).thenReturn("https://site.example.com");

        String uri = ConceptUriBuilder.buildConceptUri(
                preferences,
                "http://localhost/app",
                "C1",
                "TH1",
                "",
                "12345/abc",
                ""
        );

        assertEquals("https://hdl.handle.net/12345/abc", uri);
    }

    @Test
    void resolvePermanentId_prefersHandleForHandleExport() {
        var preferences = mock(ThesaurusPreferences.class);
        when(preferences.exportUriType()).thenReturn(ExportUriType.HANDLE);

        assertEquals("12345/abc", ConceptUriBuilder.resolvePermanentId(preferences, "ark:/1", "12345/abc", "10.123/xyz"));
    }
}
