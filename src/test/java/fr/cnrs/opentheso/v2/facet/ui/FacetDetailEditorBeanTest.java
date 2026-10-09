package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptSearchSuggestion;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import fr.cnrs.opentheso.v2.facet.read.FacetReadService;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.CreateFacetCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
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
class FacetDetailEditorBeanTest {

    @Mock private FacetMutationService facetMutationService;
    @Mock private FacetReadService facetReadService;
    @Mock private ConceptWriteSearchService conceptWriteSearchService;
    @Mock private ConceptWriteMetadataService conceptWriteMetadataService;
    @Mock private ConceptLifecycleMutationService conceptLifecycleMutationService;
    @Mock private ConceptNoteMutationService conceptNoteMutationService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;
    @Mock private ConceptWritePolicy conceptWritePolicy;
    @Mock private ThesaurusBrowseBean thesaurusBrowseBean;

    private FacetDetailEditorBean bean;

    private static final FacetDetailOverview FACET = new FacetDetailOverview(
            "F1", "Facet", "fr", "C1", "Parent", List.of(), List.of(), List.of());

    @BeforeEach
    void setUp() {
        bean = new FacetDetailEditorBean(
                facetMutationService, facetReadService, conceptWriteSearchService,
                conceptWriteMetadataService, conceptLifecycleMutationService, conceptNoteMutationService,
                thesaurusContext, userSession, conceptWritePolicy, thesaurusBrowseBean);
        lenient().when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        lenient().when(thesaurusContext.resolveWorkLanguage()).thenReturn("fr");
        lenient().when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(true);
    }

    @Test
    void isManagerActionsAvailable_trueForManager() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(true);

        assertTrue(bean.isManagerActionsAvailable());
    }

    @Test
    void isManagerActionsAvailable_falseForGuest() {
        when(conceptWritePolicy.canMutateHierarchicalRelations(userSession, false)).thenReturn(false);

        assertFalse(bean.isManagerActionsAvailable());
    }

    @Test
    void prepareCreateUnderCurrentConcept_prefillParentFromSelectedConcept() {
        var summary = mock(fr.cnrs.opentheso.v2.concept.model.ConceptSummary.class);
        when(summary.conceptId()).thenReturn("C9");
        when(summary.preferredLabel()).thenReturn("Concept neuf");
        var detail = mock(fr.cnrs.opentheso.v2.concept.model.ConceptDetail.class);
        when(detail.summary()).thenReturn(summary);
        when(thesaurusBrowseBean.getSelectedConcept()).thenReturn(detail);

        bean.prepareCreateUnderCurrentConcept();

        assertEquals("Concept neuf", bean.getParentConceptLabel());
        assertEquals("C9", bean.getParentConceptId());
        assertEquals("C9", bean.getSelectedParentConcept().conceptId());
        assertEquals("", bean.getLabel());
        assertEquals("", bean.getCreateRunState());
        assertTrue(bean.isComposing());
    }

    @Test
    void isCreateReady_falseWhenLabelBlank() {
        bean.setParentConceptId("C9");
        bean.setLabel("  ");

        assertFalse(bean.isCreateReady());
    }

    @Test
    void submitCreate_closesComposerWithoutNavigating() {
        bean.setParentConceptId("C9");
        bean.setComposing(true);
        bean.setLabel("Par matériau");
        when(facetMutationService.createFacet(new CreateFacetCommand("TH1", "C9", "fr", "Par matériau")))
                .thenReturn(MutationResult.ok("La facette a bien été créée", "F12"));

        bean.submitCreate();

        assertEquals("done", bean.getCreateRunState());
        assertEquals("F12", bean.getCreatedFacetId());
        assertEquals("Facette « Par matériau » créée", bean.getCreateFlashMessage());
        assertFalse(bean.isComposing());
        verify(thesaurusBrowseBean, never()).focusFacet(any());
        verify(thesaurusBrowseBean, never()).invalidateConceptTree();
    }

    @Test
    void submitCreate_duplicateNameStaysInError() {
        bean.setParentConceptId("C9");
        bean.setComposing(true);
        bean.setLabel("Par matériau");
        when(facetMutationService.createFacet(new CreateFacetCommand("TH1", "C9", "fr", "Par matériau")))
                .thenReturn(MutationResult.duplicate("Le nom de la facette 'Par matériau' existe déjà !"));

        bean.submitCreate();

        assertEquals("error", bean.getCreateRunState());
        assertEquals("Le nom de la facette 'Par matériau' existe déjà !", bean.getCreateErrorMessage());
        assertTrue(bean.isComposing());
        verify(thesaurusBrowseBean, never()).focusFacet(any());
    }

    @Test
    void prepareFacetDraft_keepsParentAndOpensComposer() {
        bean.setParentConceptId("C9");
        bean.setParentConceptLabel("Concept neuf");
        bean.setLabel("Ancien nom");

        bean.prepareFacetDraft();

        assertEquals("C9", bean.getParentConceptId());
        assertEquals("Concept neuf", bean.getParentConceptLabel());
        assertEquals("C9", bean.getSelectedParentConcept().conceptId());
        assertEquals("", bean.getLabel());
        assertTrue(bean.isComposing());
        assertFalse(bean.isCreated());
        assertFalse(bean.isChainNext());
    }

    @Test
    void createFacetDraft_marksCreatedAndInvalidatesTree() {
        bean.setParentConceptId("C9");
        bean.setLabel("Par matériau");
        when(facetMutationService.createFacet(new CreateFacetCommand("TH1", "C9", "fr", "Par matériau")))
                .thenReturn(MutationResult.ok("La facette a bien été créée", "F12"));

        bean.createFacetDraft();

        assertTrue(bean.isCreated());
        assertFalse(bean.isChainNext());
        assertEquals("F12", bean.getCreatedFacetId());
        verify(thesaurusBrowseBean).invalidateConceptTree();
    }

    @Test
    void createFacetDraft_persistsTranslationsNotesAndMembers() {
        bean.setParentConceptId("C9");
        bean.setLabel("Par matériau");
        bean.setTranslationsPayload("fr\tIgnoré\nen\tBy material");
        bean.setNotesPayload("scopeNote\ten\tHello%20world\tsrc");
        bean.setMembersPayload("C2\tConcept deux");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(facetMutationService.createFacet(new CreateFacetCommand("TH1", "C9", "fr", "Par matériau")))
                .thenReturn(MutationResult.ok("La facette a bien été créée", "F12"));

        bean.createFacetDraft();

        verify(facetMutationService).addTranslation(new AddFacetTranslationCommand("TH1", "F12", "en", "By material"));
        verify(facetMutationService, never()).addTranslation(new AddFacetTranslationCommand("TH1", "F12", "fr", "Ignoré"));
        verify(conceptNoteMutationService).upsertNote(new UpsertNoteCommand(
                "TH1", "F12", "en", "scopeNote", "Hello world", "src", 7, "admin"));
        verify(facetMutationService).addMember(new AddFacetMemberCommand("TH1", "F12", "C2", false));
    }

    @Test
    void createFacetDraft_warnsWhenNotesCannotBeSavedWithoutUser() {
        bean.setParentConceptId("C9");
        bean.setLabel("Par matériau");
        bean.setDefinition("Une définition");
        when(userSession.getCurrentUserId()).thenReturn(7);
        when(userSession.getCurrentUsername()).thenReturn("admin");
        when(conceptNoteMutationService.upsertNote(any())).thenThrow(new RuntimeException("db"));
        when(facetMutationService.createFacet(new CreateFacetCommand("TH1", "C9", "fr", "Par matériau")))
                .thenReturn(MutationResult.ok("La facette a bien été créée", "F12"));

        bean.createFacetDraft();

        assertTrue(bean.isCreated());
        assertEquals("F12", bean.getCreatedFacetId());
        assertTrue(String.valueOf(bean.getCreateFlashMessage()).contains("certaines informations"),
                bean.getCreateFlashMessage());
    }

    @Test
    void cancelFacetDraft_resetsForm() {
        bean.setComposing(true);
        bean.setParentConceptId("C9");
        bean.setLabel("Par matériau");
        bean.setCreated(true);

        bean.cancelFacetDraft();

        assertFalse(bean.isComposing());
        assertFalse(bean.isCreated());
        assertEquals("", bean.getLabel());
        assertEquals("", bean.getParentConceptId());
    }

    @Test
    void cancelCreate_closesComposer() {
        bean.setComposing(true);
        bean.setLabel("Par matériau");
        bean.setCreatedFacetId("F12");

        bean.cancelCreate();

        assertFalse(bean.isComposing());
        assertEquals("", bean.getLabel());
        assertEquals("", bean.getCreatedFacetId());
        verify(thesaurusBrowseBean, never()).focusFacet(any());
    }

    @Test
    void prepareModify_loadsFacetLabel() {
        when(thesaurusBrowseBean.getSelectedFacet()).thenReturn(FACET);

        bean.prepareModify();

        assertEquals("Facet", bean.getLabel());
    }

    @Test
    void submitAddMember_rejectsMissingConcept() {
        when(thesaurusBrowseBean.getSelectedFacet()).thenReturn(FACET);

        try (MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            bean.submitAddMember();
            messageUtils.verify(() -> MessageUtils.showErrorMessage("Sélection invalide !"));
        }
        verify(facetMutationService, never()).addMember(any());
    }

    @Test
    void submitAddMember_withBranch_delegatesToService() {
        when(thesaurusBrowseBean.getSelectedFacet()).thenReturn(FACET);
        bean.setSelectedConcept(new ConceptSearchSuggestion("C1", "Concept", null, false));
        bean.setApplyToBranch(true);
        when(facetMutationService.addMember(new AddFacetMemberCommand("TH1", "F1", "C1", true)))
                .thenReturn(MutationResult.ok("La branche a bien été ajoutée à la facette"));
        when(facetReadService.loadDetail("TH1", "F1", "fr")).thenReturn(Optional.of(FACET));

        PrimeFaces primeFaces = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        lenient().when(primeFaces.ajax()).thenReturn(ajax);
        try (MockedStatic<PrimeFaces> primeFacesStatic = mockStatic(PrimeFaces.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class)) {
            primeFacesStatic.when(PrimeFaces::current).thenReturn(primeFaces);

            bean.submitAddMember();

            verify(facetMutationService).addMember(new AddFacetMemberCommand("TH1", "F1", "C1", true));
            messageUtils.verify(() -> MessageUtils.showInformationMessage("La branche a bien été ajoutée à la facette"));
        }
    }
}
