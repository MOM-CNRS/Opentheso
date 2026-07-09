package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionSkosImportSupport;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionSkosImportServiceTest {

    @Mock
    private ThesaurusEditionSkosImportSupport thesaurusEditionSkosImportSupport;
    @Mock
    private NewThesaurusService newThesaurusService;

    private ThesaurusEditionSkosImportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionSkosImportService(thesaurusEditionSkosImportSupport, newThesaurusService);
    }

    @Test
    void loadSkosFile_delegatesToSupport() throws Exception {
        var document = sampleDocument();
        var error = new StringBuffer();
        var input = new ByteArrayInputStream("<rdf/>".getBytes(StandardCharsets.UTF_8));
        when(thesaurusEditionSkosImportSupport.readSkos(any(), eq(RDFFormat.RDFXML), eq("fr"), eq(error)))
                .thenReturn(document);

        var result = service.loadSkosFile(input, 0, "fr", error);

        assertEquals(document, result.document());
        assertEquals(1, result.totalConcepts());
    }

    @Test
    void importNewThesaurus_delegatesToSupport() throws Exception {
        var document = sampleDocument();
        when(thesaurusEditionSkosImportSupport.importNewThesaurus(
                eq(document),
                eq("yyyy-MM-dd"),
                eq(7),
                eq(12),
                eq("fr"),
                eq("ark"),
                eq(""),
                eq(""),
                any(Preferences.class)
        )).thenReturn("TH99");

        String thesaurusId = service.importNewThesaurus(
                document,
                "yyyy-MM-dd",
                7,
                true,
                12,
                "fr",
                "ark",
                "",
                ""
        );

        assertEquals("TH99", thesaurusId);
    }

    @Test
    void importNewThesaurus_usesSingleProjectForNonSuperAdmin() throws Exception {
        var document = sampleDocument();
        when(newThesaurusService.loadFormOptions(7, false))
                .thenReturn(new NewThesaurusFormOptions(List.of(), List.of(new ProjectOption(5, "Projet A")), false));
        when(thesaurusEditionSkosImportSupport.importNewThesaurus(
                eq(document),
                any(),
                eq(7),
                eq(5),
                any(),
                any(),
                any(),
                any(),
                any(Preferences.class)
        )).thenReturn("TH5");

        String thesaurusId = service.importNewThesaurus(
                document,
                "yyyy-MM-dd",
                7,
                false,
                null,
                "fr",
                "sans",
                "",
                ""
        );

        assertEquals("TH5", thesaurusId);
        ArgumentCaptor<Integer> projectCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(thesaurusEditionSkosImportSupport).importNewThesaurus(
                eq(document),
                any(),
                eq(7),
                projectCaptor.capture(),
                any(),
                any(),
                any(),
                any(),
                any(Preferences.class)
        );
        assertEquals(5, projectCaptor.getValue());
    }

    @Test
    void importNewThesaurus_surfacesSupportError() throws Exception {
        var document = sampleDocument();
        when(thesaurusEditionSkosImportSupport.importNewThesaurus(
                eq(document),
                anyString(),
                anyInt(),
                nullable(Integer.class),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(Preferences.class)
        )).thenReturn(null);
        when(thesaurusEditionSkosImportSupport.getLastErrorMessage()).thenReturn("Erreur SKOS");

        assertThrows(IllegalStateException.class, () -> service.importNewThesaurus(
                document, "yyyy-MM-dd", 7, true, null, "fr", "sans", "", ""
        ));
    }

    private SKOSXmlDocument sampleDocument() throws Exception {
        var document = new SKOSXmlDocument();
        document.setTitle("https://example.com/theso");
        var concept = new SKOSResource();
        concept.getLabelsList().add(new SKOSLabel("Chat", "fr", SKOSProperty.PREF_LABEL));
        document.setConceptList(new ArrayList<>(List.of(concept)));
        return document;
    }
}
