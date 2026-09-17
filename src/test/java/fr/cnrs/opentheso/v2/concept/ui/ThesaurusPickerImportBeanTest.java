package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.v2.concept.service.ThesaurusPickerImportProgressTracker;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvImportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvStructuredImportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionSkosImportService;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusPickerImportBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private ToolboxAccessPolicy toolboxAccessPolicy;
    @Mock
    private NewThesaurusService newThesaurusService;
    @Mock
    private ThesaurusEditionSkosImportService skosImportService;
    @Mock
    private ThesaurusEditionCsvImportService csvImportService;
    @Mock
    private ThesaurusEditionCsvStructuredImportService csvStructuredImportService;
    @Mock
    private ThesaurusPickerImportProgressTracker importProgressTracker;

    private ThesaurusPickerImportBean bean;

    @BeforeEach
    void setUp() {
        lenient().when(v2LocaleBean.getMsg(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        lenient().when(importProgressTracker.snapshot(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new ThesaurusPickerImportProgressTracker.ProgressSnapshot(
                        false, false, 0, 0, "idle", "", 0, 0, -1L, 0d));
        bean = new ThesaurusPickerImportBean(
                userSession,
                v2LocaleBean,
                toolboxAccessPolicy,
                newThesaurusService,
                skosImportService,
                csvImportService,
                csvStructuredImportService,
                importProgressTracker
        );
    }

    @Test
    void open_deniedWhenNoToolboxPermission() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(false);

        bean.open();

        assertFalse(bean.isImportMode());
        verify(newThesaurusService, never()).loadFormOptions(anyInt(), anyBoolean());
    }

    @Test
    void open_initializesFormForAllowedUser() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(newThesaurusService.loadFormOptions(5, true)).thenReturn(new NewThesaurusFormOptions(
                List.of(new LanguageOption("fr", "fr", "Français", "French"), new LanguageOption("en", "gb", "Anglais", "English")),
                List.of(new ProjectOption(3, "Frantiq")),
                true
        ));

        bean.open();

        assertTrue(bean.isImportMode());
        assertTrue(bean.isFormat(ThesaurusPickerImportBean.FORMAT_SKOS));
        assertEquals("fr", bean.getSelectedLang());
        assertEquals(2, bean.getLanguages().size());
    }

    @Test
    void selectFormat_resetsLoadedFile() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(newThesaurusService.loadFormOptions(5, false)).thenReturn(new NewThesaurusFormOptions(
                List.of(new LanguageOption("fr", "fr", "Français", "French")),
                List.of(new ProjectOption(3, "Frantiq")),
                false
        ));
        bean.open();
        bean.setLoadDone(true);
        bean.setTotalConcepts(12);

        bean.selectFormat(ThesaurusPickerImportBean.FORMAT_CSV);

        assertTrue(bean.isFormat(ThesaurusPickerImportBean.FORMAT_CSV));
        assertFalse(bean.isLoadDone());
        assertEquals(0, bean.getTotalConcepts());
        assertEquals("3", bean.getSelectedProjectId());
    }

    @Test
    void cancel_leavesImportMode() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(newThesaurusService.loadFormOptions(5, true)).thenReturn(new NewThesaurusFormOptions(
                List.of(), List.of(), true
        ));
        bean.open();

        bean.cancel();

        assertFalse(bean.isImportMode());
    }

    @Test
    void importReady_afterLoadDoneWithTempFile_evenIfParsedGraphIsNull() {
        bean.setFormat(ThesaurusPickerImportBean.FORMAT_SKOS);
        bean.setLoadDone(true);
        bean.setUploadTempPath("/tmp/opentheso-import/picker-test");
        bean.setUploadedFileSize(3L);
        bean.setSkosDocument(null);

        assertTrue(bean.isImportReady());
        assertTrue(bean.isHasUpload());
    }

    @Test
    void importReady_csvRequiresThesaurusName() {
        bean.setFormat(ThesaurusPickerImportBean.FORMAT_CSV);
        bean.setLoadDone(true);
        bean.setUploadTempPath("/tmp/opentheso-import/picker-csv");
        bean.setUploadedFileSize(12L);

        assertFalse(bean.isImportReady());

        bean.setThesaurusName("  PACTOLS  ");
        assertTrue(bean.isImportReady());
    }

    @Test
    void submitImport_deniedWithoutPermission() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(false);

        bean.submitImport();

        assertEquals("v2.picker.import.denied", bean.getError());
        verify(skosImportService, never()).importNewThesaurus(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submitImport_deniedWithoutUserId() {
        when(toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(null);

        bean.submitImport();

        assertEquals("v2.picker.import.denied", bean.getError());
    }

    @Test
    void clearUploadedFile_resetsFileState() throws Exception {
        var dir = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "opentheso-import");
        java.nio.file.Files.createDirectories(dir);
        var temp = java.nio.file.Files.createTempFile(dir, "picker-test-", null);
        java.nio.file.Files.write(temp, new byte[]{1, 2, 3, 4});
        bean.setUploadTempPath(temp.toAbsolutePath().toString());
        bean.setUploadedFileSize(4L);
        bean.setUploadedFileName("demo.rdf");
        bean.setLoadDone(true);
        bean.setTotalConcepts(9);

        bean.clearUploadedFile();

        assertFalse(bean.isLoadDone());
        assertEquals(0, bean.getTotalConcepts());
        assertFalse(bean.isHasUpload());
        assertTrue(bean.getUploadedFileName() == null || bean.getUploadedFileName().isBlank());
        assertTrue(java.nio.file.Files.notExists(temp));
    }
}
