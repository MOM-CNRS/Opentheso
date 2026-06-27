package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorpusSettingsBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private ThesaurusContext thesaurusContext;
    @Mock
    private ThesaurusAccessService thesaurusAccessService;
    @Mock
    private ThesaurusCorpusService thesaurusCorpusService;

    private CorpusSettingsBean bean;

    @BeforeEach
    void setUp() {
        bean = new CorpusSettingsBean(
                userSession,
                thesaurusContext,
                thesaurusAccessService,
                thesaurusCorpusService
        );
    }

    @Test
    void load_listsCorpusWhenUserCanManageThesaurus() {
        grantAccess();
        when(thesaurusCorpusService.listCorpus("TH1")).thenReturn(List.of(SettingTestFixtures.sampleCorpus()));

        try (MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces()) {
            bean.load();
        }

        assertEquals(1, bean.getCorpusList().size());
        assertTrue(bean.isScreenAvailable());
        verify(thesaurusContext).syncFromViewParams();
    }

    @Test
    void load_clearsListWhenAccessDenied() {
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");
        when(thesaurusAccessService.canManageThesaurus(anyInt(), eq(false), eq("TH1"))).thenReturn(false);

        bean.load();

        assertTrue(bean.getCorpusList().isEmpty());
        assertFalse(bean.isScreenAvailable());
        assertNull(bean.getEditingCorpusName());
        verify(thesaurusCorpusService, never()).listCorpus(any());
    }

    @Test
    void prepareCreate_resetsEditor() {
        bean.prepareCreate();

        assertNull(bean.getCorpusEditor().getCorpusName());
        assertNull(bean.getEditingCorpusName());
    }

    @Test
    void prepareEdit_setsEditorAndEditingName() {
        bean.prepareEdit(SettingTestFixtures.sampleCorpus());

        assertEquals("Corpus A", bean.getCorpusEditor().getCorpusName());
        assertEquals("Corpus A", bean.getEditingCorpusName());
    }

    @Test
    void create_persistsCorpusWhenAllowed() {
        grantAccess();
        bean.setCorpusEditor(CorpusEditor.from(
                new ThesaurusCorpus("New", "http://link", null, true, true, false, null)
        ));
        when(thesaurusCorpusService.listCorpus("TH1")).thenReturn(List.of());

        try (MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces();
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.create();

            verify(thesaurusCorpusService).createCorpus(eq("TH1"), any(ThesaurusCorpus.class));
            messageUtils.verify(() -> MessageUtils.showInformationMessage("Corpus créé avec succès"));
        }
    }

    @Test
    void create_showsErrorWhenValidationFails() {
        grantAccess();
        bean.setCorpusEditor(CorpusEditor.from(
                new ThesaurusCorpus("New", "http://link", null, true, true, false, null)
        ));
        when(thesaurusCorpusService.createCorpus(eq("TH1"), any(ThesaurusCorpus.class)))
                .thenThrow(new InvalidSettingDataException("nom obligatoire"));

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.create();
            messageUtils.verify(() -> MessageUtils.showErrorMessage("nom obligatoire"));
        }
    }

    @Test
    void update_persistsCorpusWhenAllowed() {
        grantAccess();
        bean.prepareEdit(SettingTestFixtures.sampleCorpus());
        bean.getCorpusEditor().setUriLink("http://updated");
        when(thesaurusCorpusService.listCorpus("TH1")).thenReturn(List.of());

        try (MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces();
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.update();

            verify(thesaurusCorpusService).updateCorpus(eq("TH1"), eq("Corpus A"), any(ThesaurusCorpus.class));
            messageUtils.verify(() -> MessageUtils.showInformationMessage("Corpus modifié avec succès"));
        }
    }

    @Test
    void update_skipsWhenEditingNameMissing() {
        grantAccess();

        bean.update();

        verify(thesaurusCorpusService, never()).updateCorpus(any(), any(), any());
    }

    @Test
    void delete_removesCorpusWhenAllowed() {
        grantAccess();
        bean.prepareEdit(SettingTestFixtures.sampleCorpus());
        when(thesaurusCorpusService.listCorpus("TH1")).thenReturn(List.of());

        try (MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces();
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.delete();

            verify(thesaurusCorpusService).deleteCorpus("TH1", "Corpus A");
            messageUtils.verify(() -> MessageUtils.showInformationMessage("Corpus supprimé avec succès"));
        }
    }

    @Test
    void delete_showsErrorWhenServiceFails() {
        grantAccess();
        bean.prepareEdit(SettingTestFixtures.sampleCorpus());
        doThrow(new InvalidSettingDataException("introuvable"))
                .when(thesaurusCorpusService).deleteCorpus("TH1", "Corpus A");

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.delete();
            messageUtils.verify(() -> MessageUtils.showErrorMessage("introuvable"));
        }
    }

    private void grantAccess() {
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(userSession.isSuperAdmin()).thenReturn(false);
        when(thesaurusContext.getCurrentThesaurusId()).thenReturn("TH1");
        when(thesaurusAccessService.canManageThesaurus(2, false, "TH1")).thenReturn(true);
    }

    private MockedStatic<PrimeFaces> mockPrimeFaces() {
        MockedStatic<PrimeFaces> primeFaces = mockStatic(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        PrimeFaces primeFacesInstance = mock(PrimeFaces.class);
        primeFaces.when(PrimeFaces::current).thenReturn(primeFacesInstance);
        when(primeFacesInstance.ajax()).thenReturn(ajax);
        return primeFaces;
    }
}