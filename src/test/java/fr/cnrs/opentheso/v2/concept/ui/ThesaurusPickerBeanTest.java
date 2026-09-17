package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusPickerRow;
import fr.cnrs.opentheso.v2.concept.service.ConsultationCatalogService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.shared.ui.V2NavigationBean;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusDetails;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.service.ModifyThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusPickerBeanTest {

    @Mock
    private ConsultationCatalogService consultationCatalogService;
    @Mock
    private ConsultationShellBean consultationShellBean;
    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean v2LocaleBean;
    @Mock
    private V2NavigationBean v2NavigationBean;
    @Mock
    private NewThesaurusService newThesaurusService;
    @Mock
    private ThesaurusPickerImportBean thesaurusPickerImportBean;
    @Mock
    private ThesaurusPickerExportBean thesaurusPickerExportBean;
    @Mock
    private ThesaurusAccessService thesaurusAccessService;
    @Mock
    private ModifyThesaurusService modifyThesaurusService;
    @Mock
    private EditionThesaurusService editionThesaurusService;

    private ThesaurusPickerBean bean;

    @BeforeEach
    void setUp() {
        lenient().when(v2LocaleBean.getMsg(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(v2LocaleBean.getIdLangue()).thenReturn("fr");
        lenient().when(userSession.isLoggedIn()).thenReturn(true);
        lenient().when(userSession.getCurrentUserId()).thenReturn(1);
        lenient().when(userSession.isSuperAdmin()).thenReturn(false);
        lenient().when(consultationCatalogService.listPickerThesauri(anyInt(), anyBoolean(), anyString()))
                .thenReturn(sampleRows());

        bean = new ThesaurusPickerBean(
                consultationCatalogService,
                consultationShellBean,
                userSession,
                v2LocaleBean,
                v2NavigationBean,
                newThesaurusService,
                thesaurusPickerImportBean,
                thesaurusPickerExportBean,
                thesaurusAccessService,
                modifyThesaurusService,
                editionThesaurusService
        );
        bean.load();
    }

    @Test
    void load_fetchesPickerRows() {
        verify(consultationCatalogService).listPickerThesauri(1, false, "fr");
        assertEquals(3, bean.getRows().size());
    }

    @Test
    void selectTab_filtersMemberAndPublic() {
        bean.selectTab(ThesaurusPickerBean.TAB_MEMBER);
        assertEquals(2, bean.getFilteredRows().size());
        assertTrue(bean.getFilteredRows().stream().allMatch(ThesaurusPickerRow::isMember));

        bean.selectTab(ThesaurusPickerBean.TAB_PUBLIC);
        assertEquals(2, bean.getFilteredRows().size());
        assertTrue(bean.getFilteredRows().stream().allMatch(ThesaurusPickerRow::isPublicAccess));
    }

    @Test
    void toggleSort_andQuery_filterRows() {
        bean.setQuery("Alpha");
        assertEquals(1, bean.getFilteredRows().size());
        assertEquals("th-a", bean.getFilteredRows().get(0).id());

        bean.toggleSort("terms");
        assertTrue(bean.isSortedBy("terms"));
        bean.toggleSort("terms");
        assertFalse(bean.isSortAscending());
    }

    @Test
    void openThesaurus_setsShellSelection() throws Exception {
        bean.openThesaurus("th-a");
        verify(consultationShellBean).setSelectedThesaurusId("th-a");
        verify(consultationShellBean).onThesaurusChange();
    }

    @Test
    void createThesaurus_deniedWhenGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);
        bean.createThesaurus();
        assertFalse(bean.isCreateMode());
        verify(newThesaurusService, never()).loadFormOptions(anyInt(), anyBoolean());
    }

    @Test
    void createThesaurus_opensFormWhenLoggedIn() {
        when(newThesaurusService.loadFormOptions(1, false)).thenReturn(new NewThesaurusFormOptions(
                List.of(new LanguageOption("fr", "fr", "Français", "French")),
                List.of(new ProjectOption(3, "Frantiq")),
                false
        ));

        bean.createThesaurus();

        assertTrue(bean.isCreateMode());
        assertEquals("fr", bean.getCreateEditor().getSelectedLanguage());
        assertEquals("3", bean.getCreateEditor().getSelectedProjectId());
    }

    @Test
    void openImportThesaurus_delegatesWhenAllowed() {
        when(thesaurusPickerImportBean.isCanImportThesaurus()).thenReturn(true);

        bean.openImportThesaurus();

        verify(thesaurusPickerImportBean).open();
    }

    @Test
    void openExportThesaurus_delegatesToExportBean() {
        bean.openExportThesaurus("th-a");

        verify(thesaurusPickerExportBean).open("th-a", "Alpha", 10L);
        verify(thesaurusPickerImportBean).cancel();
    }

    @Test
    void openEditThesaurus_deniedWithoutManageRight() {
        when(thesaurusAccessService.canManageThesaurus(1, false, "th-a")).thenReturn(false);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.openEditThesaurus("th-a");
            assertFalse(bean.isEditMode());
            messages.verify(() -> MessageUtils.showErrorMessage("v2.picker.edit.denied"));
        }
        verify(modifyThesaurusService, never()).loadDetails(anyString());
    }

    @Test
    void prepareAndDeleteThesaurus_whenAllowed() {
        when(thesaurusAccessService.canManageThesaurus(1, false, "th-a")).thenReturn(true);
        when(modifyThesaurusService.loadDetails("th-a")).thenReturn(
                new EditionThesaurusDetails("th-a", "Alpha", "", false, "fr")
        );

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            bean.prepareDeleteThesaurus("th-a");
            assertTrue(bean.isDeleteReady());
            assertEquals("Alpha", bean.getDeleteThesaurusTitle());

            bean.deleteThesaurus();

            verify(editionThesaurusService).deleteThesaurus("th-a", false);
            messages.verify(() -> MessageUtils.showInformationMessage(org.mockito.ArgumentMatchers.contains("v2.picker.delete.success")));
        }
        assertFalse(bean.isDeleteReady());
    }

    @Test
    void canManageRow_respectsRoleAndLogin() {
        ThesaurusPickerRow managerRow = sampleRows().get(0);
        ThesaurusPickerRow guestRole = sampleRows().get(2);

        assertTrue(bean.canManageRow(managerRow));
        assertFalse(bean.canManageRow(guestRole));

        when(userSession.isLoggedIn()).thenReturn(false);
        assertFalse(bean.canManageRow(managerRow));
    }

    private static List<ThesaurusPickerRow> sampleRows() {
        return List.of(
                new ThesaurusPickerRow(
                        "th-a", "Alpha", "member", "Manager", "manager",
                        10, LocalDate.of(2024, 1, 1), "P1", "Org", "Dom", "2020",
                        List.of("fr"), false
                ),
                new ThesaurusPickerRow(
                        "th-b", "Beta", "member", "Viewer", "viewer",
                        5, LocalDate.of(2024, 2, 1), "P1", "Org", "Dom", "2021",
                        List.of("en"), true
                ),
                new ThesaurusPickerRow(
                        "th-c", "Gamma", "public", "—", "",
                        20, LocalDate.of(2023, 1, 1), "", "Org", "Dom", "",
                        List.of("fr", "en"), false
                )
        );
    }
}
