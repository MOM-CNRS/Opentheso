package fr.cnrs.opentheso.collection.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.collection.read.CollectionReadService;
import fr.cnrs.opentheso.collection.write.model.command.AddCollectionTranslationCommand;
import fr.cnrs.opentheso.collection.write.model.command.AddMemberToCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.RemoveMemberFromCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.CreateCollectionCommand;
import fr.cnrs.opentheso.collection.write.model.command.CreateSubgroupCommand;
import fr.cnrs.opentheso.collection.write.model.command.RenameCollectionLabelCommand;
import fr.cnrs.opentheso.collection.write.model.command.UpdateCollectionTypeCommand;
import fr.cnrs.opentheso.collection.write.service.CollectionMutationService;
import fr.cnrs.opentheso.concept.model.GroupDetailOverview;
import fr.cnrs.opentheso.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.concept.model.ConceptNote;
import fr.cnrs.opentheso.concept.model.FacetMemberItem;
import fr.cnrs.opentheso.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.concept.write.model.MutationResult;
import fr.cnrs.opentheso.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.repositories.ConceptGroupTypeRepository;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.shared.ui.UserSession;
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
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private ThesaurusBrowseBean thesaurusBrowseBean;
    @Mock private ConceptNoteMutationService conceptNoteMutationService;

    private CollectionDetailEditorBean bean;

    private static final GroupDetailOverview GROUP = new GroupDetailOverview(
            "g1", "Collection", "fr", "MT", "Microthesaurus", "skos:Collection", 1, "N1", "", "",
            List.of(), List.of(), List.of());

    @BeforeEach
    void setUp() {
        bean = new CollectionDetailEditorBean(
                collectionMutationService, collectionReadService, conceptWriteSearchService,
                conceptWriteMetadataService, conceptGroupTypeRepository, thesaurusContext,
                userSession,
                conceptWritePolicy, thesaurusBrowseBean, conceptNoteMutationService);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        lenient().when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(true);
    }

    @Test
    void isManagerActionsAvailable_falseForContributor() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(false);

        assertFalse(bean.isManagerActionsAvailable());
    }

    @Test
    void isManagerActionsAvailable_trueForManager() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(true);

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
    void submitCreateFromView_rejectsBlankName() {
        bean.setLabel("  ");

        bean.submitCreateFromView();

        assertFalse(bean.isCreateOk());
        assertEquals("Le nom de la collection est obligatoire.", bean.getCreateMessage());
        verify(collectionMutationService, never()).createCollection(any());
    }

    @Test
    void submitCreateFromView_createsRootCollection() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        bean.setLabel(" Animaux ");
        bean.setNotation(" A1 ");
        when(collectionMutationService.createCollection(any(CreateCollectionCommand.class)))
                .thenReturn(MutationResult.ok("La collection a bien été créée", "g99"));

        bean.submitCreateFromView();

        assertTrue(bean.isCreateOk());
        assertEquals("g99", bean.getCreatedGroupId());
        verify(collectionMutationService).createCollection(new CreateCollectionCommand(
                "TH1", "fr", "Animaux", "A1", "MT", 7
        ));
        verify(thesaurusBrowseBean).invalidateCollectionTree();
        verify(thesaurusBrowseBean).focusGroup("g99");
        verify(collectionMutationService, never()).createSubgroup(any());
    }

    @Test
    void submitCreateFromView_createsUnderSelectedCollection() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        bean.setLabel("Félin");
        bean.setParentCollectionId(" g1 ");
        when(collectionMutationService.createSubgroup(any(CreateSubgroupCommand.class)))
                .thenReturn(MutationResult.ok("La sous-collection a bien été créée", "g100"));

        bean.submitCreateFromView();

        assertTrue(bean.isCreateOk());
        assertEquals("g100", bean.getCreatedGroupId());
        assertEquals("", bean.getParentCollectionId());
        verify(collectionMutationService).createSubgroup(new CreateSubgroupCommand(
                "TH1", "g1", "fr", "Félin", "", "MT", 7
        ));
        verify(collectionMutationService, never()).createCollection(any());
        verify(thesaurusBrowseBean).focusGroup("g100");
    }

    @Test
    void submitCreateRoot_focusesCreatedCollection() {
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
    void submitCreateFromSelection_rejectsBlankName() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        bean.setBulkLabel("  ");
        bean.setBulkConceptIds("C1");

        bean.submitCreateFromSelection();

        assertFalse(bean.isBulkOk());
        assertEquals("Le nom de la collection est obligatoire.", bean.getBulkMessage());
        verify(collectionMutationService, never()).createCollectionWithMembers(any(), any());
    }

    @Test
    void submitCreateFromSelection_rejectsUnauthorized() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(false);
        bean.setBulkLabel("Architecture");
        bean.setBulkConceptIds("C1");

        bean.submitCreateFromSelection();

        assertFalse(bean.isBulkOk());
        assertEquals("Action non autorisée", bean.getBulkMessage());
        verify(collectionMutationService, never()).createCollectionWithMembers(any(), any());
    }

    @Test
    void submitCreateFromSelection_createsWithMembers() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        bean.setBulkLabel("Architecture");
        bean.setBulkConceptIds("C1\nC2,C1");
        when(collectionMutationService.createCollectionWithMembers(any(), any()))
                .thenReturn(MutationResult.ok("Collection « Architecture » créée · 2 concepts", "g9"));

        bean.submitCreateFromSelection();

        assertTrue(bean.isBulkOk());
        assertEquals("Collection « Architecture » créée · 2 concepts", bean.getBulkMessage());
        verify(collectionMutationService).createCollectionWithMembers(
                new CreateCollectionCommand("TH1", "fr", "Architecture", "", "MT", 7),
                List.of("C1", "C2"));
        verify(thesaurusBrowseBean).invalidateCollectionTree();
    }

    @Test
    void submitAddSelectionToCollection_addsConceptsToExistingCollection() {
        bean.setBulkCollectionId("arch");
        bean.setBulkConceptIds("C1\nC2,C1");
        when(collectionMutationService.addConceptsToCollection(eq("TH1"), eq("arch"), eq(List.of("C1", "C2"))))
                .thenReturn(MutationResult.ok("2 concepts ajoutés à la collection"));

        bean.submitAddSelectionToCollection();

        assertTrue(bean.isBulkOk());
        assertEquals("2 concepts ajoutés à la collection", bean.getBulkMessage());
        assertEquals("", bean.getBulkCollectionId());
        verify(thesaurusBrowseBean).invalidateCollectionTree();
    }

    @Test
    void submitAddSelectionToCollection_requiresACollection() {
        bean.setBulkCollectionId("  ");
        bean.setBulkConceptIds("C1");

        bean.submitAddSelectionToCollection();

        assertFalse(bean.isBulkOk());
        assertEquals("Choisissez une collection.", bean.getBulkMessage());
        verify(collectionMutationService, never()).addConceptsToCollection(any(), any(), any());
    }

    @Test
    void submitModify_updatesTypeAndLabel() {
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

    @Test
    void submitFiche_renamesLabel() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(collectionMutationService.renamePreferredLabel(
                new RenameCollectionLabelCommand("TH1", "g1", "fr", "Amphore", 7)))
                .thenReturn(MutationResult.ok("La collection a bien été modifiée"));
        bean.setFicheGroupId("g1");
        bean.setFicheMode("label");
        bean.setFichePayload("Amphore");

        bean.submitFiche();

        assertTrue(bean.isFicheOk());
        assertEquals("La collection a bien été modifiée", bean.getFicheMessage());
    }

    @Test
    void submitFiche_refusesWhenTheUserCannotEdit() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(false);
        bean.setFicheGroupId("g1");
        bean.setFicheMode("label");
        bean.setFichePayload("Amphore");

        bean.submitFiche();

        assertFalse(bean.isFicheOk());
        verify(collectionMutationService, never()).renamePreferredLabel(any());
    }

    @Test
    void submitFiche_addsTranslationAndDropsRemovedNote() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        GroupDetailOverview current = new GroupDetailOverview(
                "g1", "Collection", "fr", "MT", "Microthesaurus", "skos:Collection", 1, "N1", "", "",
                List.of(new GroupTranslationItem("en", "Collection")),
                List.of(new ConceptNote("12", "definition", "fr", "Ancienne")),
                List.of());
        when(collectionReadService.loadDetail("TH1", "g1", "fr")).thenReturn(Optional.of(current));
        when(collectionMutationService.updateTranslation(any()))
                .thenReturn(MutationResult.ok("Traduction modifiée avec succès"));
        when(collectionMutationService.addTranslation(new AddCollectionTranslationCommand("TH1", "g1", "de", "Sammlung")))
                .thenReturn(MutationResult.ok("Traduction ajoutée avec succès"));
        when(collectionReadService.loadNotes("TH1", "g1"))
                .thenReturn(List.of(new ConceptNote("12", "definition", "fr", "Ancienne", "src")));
        when(conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                "TH1", "g1", 12, "fr", "definition", 7, "admin")))
                .thenReturn(MutationResult.ok("Note supprimée avec succès"));
        when(conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                "TH1", "g1", "fr", "scopeNote", "Usage", "manuel", 7, "admin")))
                .thenReturn(MutationResult.ok("Note enregistrée avec succès"));

        bean.setFicheGroupId("g1");
        bean.setFicheMode("translations");
        bean.setFichePayload("en\tCollection\nde\tSammlung");
        bean.submitFiche();
        assertTrue(bean.isFicheOk());

        bean.setFicheMode("notes");
        bean.setFichePayload("scopeNote\tfr\tUsage\tmanuel");
        bean.submitFiche();

        assertTrue(bean.isFicheOk());
        verify(conceptNoteMutationService).deleteNote(new DeleteNoteCommand(
                "TH1", "g1", 12, "fr", "definition", 7, "admin"));
        verify(conceptNoteMutationService).upsertNote(new UpsertNoteCommand(
                "TH1", "g1", "fr", "scopeNote", "Usage", "manuel", 7, "admin"));
    }

    @Test
    void submitFiche_replacesCollectionMembers() {
        when(userSession.getCurrentUserId()).thenReturn(7);
        GroupDetailOverview current = new GroupDetailOverview(
                "g1", "Collection", "fr", "MT", "Microthesaurus", "skos:Collection", 1, "N1", "", "",
                List.of(), List.of(), List.of(new FacetMemberItem("c1", "Ancien")));
        when(collectionReadService.loadDetail("TH1", "g1", "fr")).thenReturn(Optional.of(current));
        when(collectionMutationService.removeMember(new RemoveMemberFromCollectionCommand("TH1", "g1", "c1", false)))
                .thenReturn(MutationResult.ok("Le concept a bien été enlevé de la collection"));
        when(collectionMutationService.addMember(new AddMemberToCollectionCommand("TH1", "g1", "c2", false)))
                .thenReturn(MutationResult.ok("Le concept a été ajouté à la collection"));

        bean.setFicheGroupId("g1");
        bean.setFicheMode("members");
        bean.setFichePayload("c2\tAmphore");
        bean.submitFiche();

        assertTrue(bean.isFicheOk());
        assertEquals("Le concept a été ajouté à la collection", bean.getFicheMessage());
        verify(collectionMutationService).removeMember(new RemoveMemberFromCollectionCommand("TH1", "g1", "c1", false));
        verify(collectionMutationService).addMember(new AddMemberToCollectionCommand("TH1", "g1", "c2", false));
    }
}
