package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.EditionMetadata;
import fr.cnrs.opentheso.v2.toolbox.service.ModifyThesaurusService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusHomeMetadataBeanTest {

    @Mock private UserSession userSession;
    @Mock private RightsService rightsService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private ModifyThesaurusService modifyThesaurusService;
    @Mock private ObjectProvider<ThesaurusViewBean> thesaurusViewBeanProvider;
    @Mock private ThesaurusViewBean thesaurusViewBean;

    private ThesaurusHomeMetadataBean bean;
    private MockedStatic<MessageUtils> messageUtils;

    @BeforeEach
    void setUp() {
        messageUtils = mockStatic(MessageUtils.class);
        lenient().when(thesaurusViewBeanProvider.getIfAvailable()).thenReturn(thesaurusViewBean);
        bean = new ThesaurusHomeMetadataBean(
                userSession, rightsService, thesaurusContext, modifyThesaurusService, thesaurusViewBeanProvider);
    }

    @AfterEach
    void tearDown() {
        messageUtils.close();
    }

    @Test
    void isEditable_requiresAdminOnThesaurus() {
        stubAdmin("TH1", 9, true);

        assertTrue(bean.isEditable());
    }

    @Test
    void isEditable_deniesAnonymous() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        assertFalse(bean.isEditable());
    }

    @Test
    void isEditable_deniesContributor() {
        stubAdmin("TH1", 9, false);

        assertFalse(bean.isEditable());
    }

    @Test
    void sectionVisible_whenThesaurusSelected() {
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        assertTrue(bean.isSectionVisible());
    }

    @Test
    void startEditing_loadsRowsWhenAllowed() {
        stubAdmin("TH1", 9, true);
        EditionMetadata existing = EditionMetadata.emptyRow();
        existing.setId(4);
        existing.setName("title");
        existing.setValue("Pactols");
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of(existing));
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("title", "creator"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string", "date"));

        bean.startEditing();

        assertTrue(bean.isEditing());
        assertEquals(2, bean.getRows().size());
        assertEquals("maitre", bean.getRows().get(0).getName());
        assertEquals("boolean", bean.getRows().get(0).getType());
        assertEquals("false", bean.getRows().get(0).getValue());
        assertEquals("title", bean.getRows().get(1).getName());
        assertTrue(bean.getDcmiResources().contains("maitre"));
        assertTrue(bean.getDcmiTypes().contains("boolean"));
    }

    @Test
    void startEditing_ignoredWhenDenied() {
        stubAdmin("TH1", 9, false);

        bean.startEditing();

        assertFalse(bean.isEditing());
        verify(modifyThesaurusService, never()).loadMetadata("TH1");
    }

    @Test
    void addRow_appendsDefaultProperty() {
        stubAdmin("TH1", 9, true);
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("creator", "title"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string"));

        bean.startEditing();
        bean.addRow();

        assertEquals(2, bean.getRows().size());
        assertEquals("creator", bean.getRows().get(1).getName());
        assertEquals(-1, bean.getRows().get(1).getId());
    }

    @Test
    void saveRow_persistsAndRefreshesHome() {
        stubAdmin("TH1", 9, true);
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("title"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string"));
        bean.startEditing();
        EditionMetadata row = EditionMetadata.emptyRow();
        row.setName("title");
        row.setValue("Pactols");
        EditionMetadata saved = EditionMetadata.emptyRow();
        saved.setId(12);
        saved.setName("title");
        saved.setValue("Pactols");
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of(saved));

        bean.saveRow(row);

        verify(modifyThesaurusService).saveMetadata("TH1", row);
        verify(thesaurusViewBean).refreshHomeOverview();
        assertEquals(2, bean.getRows().size());
        assertEquals("maitre", bean.getRows().get(0).getName());
        assertEquals(12, bean.getRows().get(1).getId());
        messageUtils.verify(() -> MessageUtils.showInformationMessage("Métadonnée enregistrée"));
    }

    @Test
    void saveRow_showsValidationError() {
        stubAdmin("TH1", 9, true);
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("title"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string"));
        bean.startEditing();
        EditionMetadata row = EditionMetadata.emptyRow();
        row.setName("title");
        doThrow(new InvalidToolboxDataException("La valeur est obligatoire"))
                .when(modifyThesaurusService).saveMetadata("TH1", row);

        bean.saveRow(row);

        verify(thesaurusViewBean, never()).refreshHomeOverview();
        messageUtils.verify(() -> MessageUtils.showErrorMessage("La valeur est obligatoire"));
    }

    @Test
    void deleteRow_removesUnsavedRowLocally() {
        stubAdmin("TH1", 9, true);
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("title"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string"));
        bean.startEditing();
        bean.addRow();
        EditionMetadata unsaved = bean.getRows().get(1);

        bean.deleteRow(unsaved);

        assertEquals(1, bean.getRows().size());
        assertEquals("maitre", bean.getRows().get(0).getName());
        verify(modifyThesaurusService, never()).deleteMetadata("TH1", -1);
    }

    @Test
    void deleteRow_deletesPersistedRow() {
        stubAdmin("TH1", 9, true);
        EditionMetadata existing = EditionMetadata.emptyRow();
        existing.setId(7);
        existing.setName("creator");
        existing.setValue("FRANTIQ");
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of(existing));
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("creator"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string"));
        bean.startEditing();
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());

        bean.deleteRow(existing);

        verify(modifyThesaurusService).deleteMetadata("TH1", 7);
        verify(thesaurusViewBean).refreshHomeOverview();
        assertEquals(1, bean.getRows().size());
        assertEquals("maitre", bean.getRows().get(0).getName());
        messageUtils.verify(() -> MessageUtils.showInformationMessage("Métadonnée supprimée"));
    }

    @Test
    void cancelEditing_leavesEditMode() {
        stubAdmin("TH1", 9, true);
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("title"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of());
        bean.startEditing();
        bean.addRow();

        bean.cancelEditing();

        assertFalse(bean.isEditing());
        assertTrue(bean.getRows().isEmpty());
    }

    @Test
    void clearLanguageIfTyped_clearsLanguage() {
        EditionMetadata row = EditionMetadata.emptyRow();
        row.setLanguage("fr");
        row.setType("date");

        bean.clearLanguageIfTyped(row);

        assertEquals("", row.getLanguage());
        assertEquals("date", row.getType());
    }

    @Test
    void clearTypeIfLanguaged_clearsType() {
        EditionMetadata row = EditionMetadata.emptyRow();
        row.setLanguage("fr");
        row.setType("date");

        bean.clearTypeIfLanguaged(row);

        assertEquals("fr", row.getLanguage());
        assertEquals("", row.getType());
    }

    @Test
    void getLanguages_delegatesToViewBean() {
        when(thesaurusViewBean.getLanguages()).thenReturn(List.of(new ThesaurusLanguage(1L, "fr", "", "", "français")));

        assertEquals("fr", bean.getLanguages().get(0).getCode());
    }

    @Test
    void saveRow_updatesMasterRole() {
        stubAdmin("TH1", 9, true);
        when(modifyThesaurusService.isMasterThesaurus("TH1")).thenReturn(false);
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("title"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string"));
        bean.startEditing();
        EditionMetadata master = bean.getRows().get(0);
        master.setValue("true");

        bean.saveRow(master);

        verify(modifyThesaurusService).updateMasterRole("TH1", true);
        verify(modifyThesaurusService, never()).saveMetadata("TH1", master);
        assertTrue(bean.isMasterThesaurus());
        messageUtils.verify(() -> MessageUtils.showInformationMessage("Rôle du thésaurus enregistré"));
    }

    @Test
    void deleteRow_keepsMasterProperty() {
        stubAdmin("TH1", 9, true);
        when(modifyThesaurusService.loadMetadata("TH1")).thenReturn(List.of());
        when(modifyThesaurusService.loadDcmiResources()).thenReturn(List.of("title"));
        when(modifyThesaurusService.loadDcmiTypes()).thenReturn(List.of("string"));
        bean.startEditing();
        EditionMetadata master = bean.getRows().get(0);

        bean.deleteRow(master);

        assertEquals(1, bean.getRows().size());
        verify(modifyThesaurusService, never()).deleteMetadata("TH1", ThesaurusHomeMetadataBean.MASTER_ROW_ID);
        messageUtils.verify(() -> MessageUtils.showErrorMessage("La propriété maitre ne peut pas être supprimée"));
    }

    @Test
    void elAliases_matchBooleanHelpers() {
        EditionMetadata master = EditionMetadata.emptyRow();
        master.setName("maitre");
        master.setType("boolean");
        EditionMetadata title = EditionMetadata.emptyRow();
        title.setName("title");
        title.setType("string");

        assertTrue(bean.masterRow(master));
        assertTrue(bean.booleanRow(master));
        assertFalse(bean.masterRow(title));
        assertFalse(bean.booleanRow(title));
    }

    @Test
    void onPropertyChanged_forcesBooleanForMaster() {
        EditionMetadata row = EditionMetadata.emptyRow();
        row.setName("maitre");
        row.setValue("Pactols");
        row.setLanguage("fr");
        row.setType("string");

        bean.onPropertyChanged(row);

        assertEquals("boolean", row.getType());
        assertEquals("", row.getLanguage());
        assertEquals("false", row.getValue());
    }

    private void stubAdmin(String thesaurusId, int userId, boolean allowed) {
        when(userSession.getCurrentUserId()).thenReturn(userId);
        when(thesaurusContext.resolveThesaurusId()).thenReturn(thesaurusId);
        when(rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId)).thenReturn(allowed);
        lenient().when(modifyThesaurusService.isMasterThesaurus(thesaurusId)).thenReturn(false);
    }
}
