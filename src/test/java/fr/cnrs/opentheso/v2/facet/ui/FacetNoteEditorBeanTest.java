package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.facet.read.FacetReadService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacetNoteEditorBeanTest {

    @Mock private ConceptNoteMutationService conceptNoteMutationService;
    @Mock private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock private FacetReadService facetReadService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private ThesaurusBrowseBean thesaurusBrowseBean;

    private FacetNoteEditorBean bean;

    private static final FacetDetailOverview FACET = new FacetDetailOverview(
            "F1", "Facet", "fr", "C1", "Parent", List.of(), List.of(),
            List.of(new ConceptNote("2", "definition", "fr", "Def")));

    @BeforeEach
    void setUp() {
        bean = new FacetNoteEditorBean(
                conceptNoteMutationService, conceptWriteMetadataService, facetReadService,
                thesaurusContext, userSession, thesaurusBrowseBean);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        lenient().when(thesaurusBrowseBean.getSelectedFacet()).thenReturn(FACET);
    }

    @Test
    void isNoteActionsAvailable_trueForManager() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertTrue(bean.isNoteActionsAvailable());
    }

    @Test
    void prepareDeleteNotes_loadsFacetNotes() {
        bean.prepareDeleteNotes();

        assertEquals("Facet", bean.getCurrentLabel());
        assertEquals(1, bean.getNotesToDelete().size());
    }

    @Test
    void submitSaveNote_usesFacetIdAsIdentifier() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(conceptNoteMutationService.listNoteTypes()).thenReturn(List.of(new ConceptWriteNoteType("note")));
        when(conceptWriteMetadataService.listUsedLanguages("TH1", "fr"))
                .thenReturn(List.of(new ConceptWriteLanguage("fr", "Français")));
        bean.prepareManageNote();
        bean.setNoteValue("Facet note");
        when(conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                "TH1", "F1", "fr", "note", "Facet note", "", 7, "admin")))
                .thenReturn(MutationResult.ok("Note enregistrée"));
        when(facetReadService.loadDetail("TH1", "F1", "fr")).thenReturn(Optional.of(FACET));

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
}
