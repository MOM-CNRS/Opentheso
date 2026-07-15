package fr.cnrs.opentheso.v2.collection.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.collection.read.CollectionReadService;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.GroupDetailOverview;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteDraft;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionNoteEditorBeanTest {

    @Mock private ConceptNoteMutationService conceptNoteMutationService;
    @Mock private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock private CollectionReadService collectionReadService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private ThesaurusBrowseBean thesaurusBrowseBean;

    private CollectionNoteEditorBean bean;

    private static final GroupDetailOverview GROUP = new GroupDetailOverview(
            "g1", "Collection", "fr", "", "", 0, "", "", "",
            List.of(), List.of(new ConceptNote("1", "note", "fr", "Existing")), List.of());

    @BeforeEach
    void setUp() {
        bean = new CollectionNoteEditorBean(
                conceptNoteMutationService, conceptWriteMetadataService, collectionReadService,
                thesaurusContext, userSession, thesaurusBrowseBean);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        lenient().when(thesaurusBrowseBean.getSelectedGroup()).thenReturn(GROUP);
    }

    @Test
    void isNoteActionsAvailable_trueForManager() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertTrue(bean.isNoteActionsAvailable());
    }

    @Test
    void isNoteActionsAvailable_falseForGuest() {
        when(userSession.isLoggedIn()).thenReturn(false);

        assertFalse(bean.isNoteActionsAvailable());
    }

    @Test
    void prepareManageNote_loadsMetadataAndDraft() {
        when(conceptNoteMutationService.listNoteTypes()).thenReturn(List.of(new ConceptWriteNoteType("note")));
        when(conceptWriteMetadataService.listUsedLanguages("TH1", "fr"))
                .thenReturn(List.of(new ConceptWriteLanguage("fr", "Français")));
        when(conceptWriteMetadataService.loadNoteDraft("TH1", "g1", "fr", "note"))
                .thenReturn(Optional.of(new ConceptWriteNoteDraft(5, "Draft", "src")));

        bean.prepareManageNote();

        assertEquals("Collection", bean.getCurrentLabel());
        assertEquals("Draft", bean.getNoteValue());
        assertEquals("src", bean.getNoteSource());
    }

    @Test
    void submitSaveNote_delegatesToMutationService() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        bean.setSelectedLang("fr");
        bean.setSelectedTypeCode("note");
        bean.setNoteValue("New note");
        bean.setNoteSource("source");
        when(conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                "TH1", "g1", "fr", "note", "New note", "source", 7, "admin")))
                .thenReturn(MutationResult.ok("Note enregistrée"));
        when(collectionReadService.loadDetail("TH1", "g1", "fr")).thenReturn(Optional.of(GROUP));

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(primeFaces.ajax()).thenReturn(ajax);
        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.submitSaveNote();

            verify(conceptNoteMutationService).upsertNote(any(UpsertNoteCommand.class));
            messageUtils.verify(() -> MessageUtils.showInformationMessage("Note enregistrée"));
        }
    }

    @Test
    void submitDeleteNote_rejectsInvalidNoteId() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.submitDeleteNote(new ConceptNote("bad", "note", "fr", "x"));
            messageUtils.verify(() -> MessageUtils.showErrorMessage("Aucune note sélectionnée !"));
        }
        verify(conceptNoteMutationService, never()).deleteNote(any());
    }

    @Test
    void submitDeleteNote_deletesNote() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                "TH1", "g1", 12, "fr", "note", 7, "admin")))
                .thenReturn(MutationResult.ok("Note supprimée"));
        when(collectionReadService.loadDetail("TH1", "g1", "fr")).thenReturn(Optional.of(GROUP));

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(primeFaces.ajax()).thenReturn(ajax);
        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.submitDeleteNote(new ConceptNote("12", "note", "fr", "x"));

            verify(conceptNoteMutationService).deleteNote(any(DeleteNoteCommand.class));
        }
    }
}
