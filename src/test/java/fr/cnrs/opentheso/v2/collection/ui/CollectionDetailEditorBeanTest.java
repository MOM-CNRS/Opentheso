package fr.cnrs.opentheso.v2.collection.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.collection.read.CollectionReadService;
import fr.cnrs.opentheso.v2.collection.write.model.command.AddMemberToCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.CreateCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionTypeCommand;
import fr.cnrs.opentheso.v2.collection.write.service.CollectionMutationService;
import fr.cnrs.opentheso.v2.concept.model.GroupDetailOverview;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.repositories.ConceptGroupTypeRepository;
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
class CollectionDetailEditorBeanTest {

    @Mock private CollectionMutationService collectionMutationService;
    @Mock private CollectionReadService collectionReadService;
    @Mock private ConceptWriteSearchService conceptWriteSearchService;
    @Mock private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock private ConceptGroupTypeRepository conceptGroupTypeRepository;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private ThesaurusBrowseBean thesaurusBrowseBean;

    private CollectionDetailEditorBean bean;

    private static final GroupDetailOverview GROUP = new GroupDetailOverview(
            "g1", "Collection", "fr", "MT", "skos:Collection", 1, "N1", "", "",
            List.of(), List.of(), List.of());

    @BeforeEach
    void setUp() {
        bean = new CollectionDetailEditorBean(
                collectionMutationService, collectionReadService, conceptWriteSearchService,
                conceptWriteMetadataService, conceptGroupTypeRepository, thesaurusContext,
                userSession, thesaurusBrowseBean);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
    }

    @Test
    void isManagerActionsAvailable_falseForContributor() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(false);
        when(userSession.isSuperAdmin()).thenReturn(false);

        assertFalse(bean.isManagerActionsAvailable());
    }

    @Test
    void isManagerActionsAvailable_trueForManager() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);

        assertTrue(bean.isManagerActionsAvailable());
    }

    @Test
    void prepareModify_loadsSelectedGroupFields() {
        when(thesaurusBrowseBean.getSelectedGroup()).thenReturn(GROUP);
        when(conceptGroupTypeRepository.findAll()).thenReturn(List.of());

        bean.prepareModify();

        assertEquals("Collection", bean.getLabel());
        assertEquals("N1", bean.getNotation());
        assertEquals("MT", bean.getTypeCode());
    }

    @Test
    void submitAddMember_rejectsMissingConcept() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(thesaurusBrowseBean.getSelectedGroup()).thenReturn(GROUP);

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.submitAddMember();
            messageUtils.verify(() -> MessageUtils.showErrorMessage("Sélection invalide !"));
        }
        verify(collectionMutationService, never()).addMember(any());
    }

    @Test
    void submitAddMember_delegatesToService() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(thesaurusBrowseBean.getSelectedGroup()).thenReturn(GROUP);
        bean.setSelectedConcept(new ConceptSearchSuggestion("C1", "Concept", null, false));
        when(collectionMutationService.addMember(new AddMemberToCollectionCommand("TH1", "g1", "C1", false)))
                .thenReturn(MutationResult.ok("Le concept a été ajouté à la collection"));
        when(collectionReadService.loadDetail("TH1", "g1", "fr")).thenReturn(Optional.of(GROUP));

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(primeFaces.ajax()).thenReturn(ajax);
        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.submitAddMember();

            verify(collectionMutationService).addMember(new AddMemberToCollectionCommand("TH1", "g1", "C1", false));
            messageUtils.verify(() -> MessageUtils.showInformationMessage("Le concept a été ajouté à la collection"));
        }
    }

    @Test
    void submitCreateRoot_focusesCreatedCollection() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        bean.setLabel("Root");
        when(collectionMutationService.createCollection(any(CreateCollectionCommand.class)))
                .thenReturn(MutationResult.ok("La collection a bien été créée", "g99"));

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(primeFaces.ajax()).thenReturn(ajax);
        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.submitCreateRoot();

            verify(thesaurusBrowseBean).invalidateCollectionTree();
            verify(thesaurusBrowseBean).focusGroup("g99");
        }
    }

    @Test
    void submitModify_updatesTypeAndLabel() {
        when(userSession.isLoggedIn()).thenReturn(true);
        when(userSession.isManager()).thenReturn(true);
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(thesaurusBrowseBean.getSelectedGroup()).thenReturn(GROUP);
        bean.setLabel("Updated");
        bean.setTypeCode("CC");
        bean.setNotation("N2");
        when(collectionMutationService.renamePreferredLabel(any()))
                .thenReturn(MutationResult.ok("La collection a bien été modifiée"));
        when(collectionMutationService.updateType(new UpdateCollectionTypeCommand("TH1", "g1", "CC")))
                .thenReturn(MutationResult.ok("Le type a bien été modifié"));
        when(collectionMutationService.updateNotation(any()))
                .thenReturn(MutationResult.ok("La notation a bien été modifiée"));
        when(collectionReadService.loadDetail("TH1", "g1", "fr")).thenReturn(Optional.of(GROUP));

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(primeFaces.ajax()).thenReturn(ajax);
        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> ignored = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.submitModify();

            verify(collectionMutationService).updateType(new UpdateCollectionTypeCommand("TH1", "g1", "CC"));
        }
    }
}
