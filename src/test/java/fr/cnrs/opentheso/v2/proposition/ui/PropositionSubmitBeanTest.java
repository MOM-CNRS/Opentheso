package fr.cnrs.opentheso.v2.proposition.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSynonymOption;
import fr.cnrs.opentheso.v2.proposition.model.PropositionTranslationOption;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.test.support.PrimeFacesTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropositionSubmitBeanTest {

    @Mock private PropositionMutationService propositionMutationService;
    @Mock private PropositionDraftService propositionDraftService;
    @Mock private ConceptSelectionContext conceptSelectionContext;
    @Mock private ConceptReadService conceptReadService;
    @Mock private ConceptLexicalMutationService conceptLexicalMutationService;
    @Mock private ThesaurusContext thesaurusContext;
    @Mock private UserSession userSession;

    private PropositionSubmitBean bean;
    private MockedStatic<MessageUtils> messageUtilsStatic;
    private PrimeFacesTestSupport.PrimeFacesContext primeFacesContext;

    private static final ConceptSummary SUMMARY =
            new ConceptSummary("C1", "TH1", "Concept 1", "fr", "valid", null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        messageUtilsStatic = mockStatic(MessageUtils.class);
        primeFacesContext = PrimeFacesTestSupport.open();
        bean = new PropositionSubmitBean(
                propositionMutationService, propositionDraftService, conceptSelectionContext,
                conceptReadService, conceptLexicalMutationService, thesaurusContext, userSession);
        lenient().when(conceptLexicalMutationService.listUsedLanguages(any(), any()))
                .thenReturn(List.of(new ConceptWriteLanguage("fr", "Français")));
    }

    @AfterEach
    void tearDown() {
        messageUtilsStatic.close();
        primeFacesContext.close();
    }

    @Test
    void prepare_resetsFieldsWhenNoConceptSelected() {
        when(conceptSelectionContext.hasSelection()).thenReturn(false);

        bean.prepare();

        assertEquals("", bean.getComment());
        assertTrue(bean.getSynonymOptions().isEmpty());
    }

    @Test
    void prepare_seedsOptionsFromCurrentConceptState() {
        when(conceptSelectionContext.hasSelection()).thenReturn(true);
        when(conceptSelectionContext.getSummary()).thenReturn(SUMMARY);
        when(conceptSelectionContext.getConceptId()).thenReturn("C1");
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");

        var detail = new ConceptDetail(SUMMARY, List.of(), List.of(), List.of(), List.of(),
                List.of("Synonym 1"), List.of("Hidden synonym"),
                List.of(new ConceptLabel("en", "English label", false, false)),
                List.of(new ConceptNote("1", "definition", "fr", "Existing definition")),
                List.of(), List.of(), List.of(), List.of(), List.of());
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(detail));

        bean.prepare();

        assertEquals("Concept 1", bean.getCurrentPreferredLabel());
        assertEquals(2, bean.getSynonymOptions().size());
        assertEquals(1, bean.getTranslationOptions().size());
        assertEquals("English label", bean.getTranslationOptions().get(0).getValue());
        assertEquals(7, bean.getNoteOptions().size());
        assertEquals("Existing definition", bean.getNoteOptions().stream()
                .filter(o -> "definition".equals(o.getTypeCode())).findFirst().orElseThrow().getValue());
    }

    @Test
    void addSynonymOption_rejectsBlankValue() {
        bean.setNewSynonymValue(" ");

        bean.addSynonymOption();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Veuillez saisir une valeur"));
        assertTrue(bean.getSynonymOptions().isEmpty());
    }

    @Test
    void addSynonymOption_addsNewOptionMarkedToAdd() {
        when(conceptSelectionContext.getSummary()).thenReturn(SUMMARY);
        bean.setNewSynonymValue("New synonym");
        bean.setNewSynonymHidden(true);

        bean.addSynonymOption();

        assertEquals(1, bean.getSynonymOptions().size());
        assertTrue(bean.getSynonymOptions().get(0).isToAdd());
        assertTrue(bean.getSynonymOptions().get(0).isHidden());
        assertEquals(null, bean.getNewSynonymValue());
    }

    @Test
    void removeSynonymOption_deletesFreshlyAddedOption() {
        var option = new PropositionSynonymOption();
        option.setValue("New");
        bean.getSynonymOptions().add(option);

        bean.removeSynonymOption(option);

        assertTrue(bean.getSynonymOptions().isEmpty());
    }

    @Test
    void removeSynonymOption_marksExistingOptionForRemoval() {
        var option = new PropositionSynonymOption();
        option.setValue("Existing");
        option.setOldValue("Existing");
        bean.getSynonymOptions().add(option);

        bean.removeSynonymOption(option);

        assertTrue(bean.getSynonymOptions().contains(option));
        assertTrue(option.isToRemove());
    }

    @Test
    void markSynonymUpdated_setsFlagWhenValueChanged() {
        var option = new PropositionSynonymOption();
        option.setOldValue("Old");
        option.setValue("New");

        bean.markSynonymUpdated(option);

        assertTrue(option.isToUpdate());
    }

    @Test
    void markSynonymUpdated_clearsFlagWhenValueReverted() {
        var option = new PropositionSynonymOption();
        option.setOldValue("Old");
        option.setValue("Old");
        option.setToUpdate(true);

        bean.markSynonymUpdated(option);

        assertFalse(option.isToUpdate());
    }

    @Test
    void undoRemoveSynonymOption_clearsRemovalFlag() {
        var option = new PropositionSynonymOption();
        option.setToRemove(true);

        bean.undoRemoveSynonymOption(option);

        assertFalse(option.isToRemove());
    }

    @Test
    void addTranslationOption_rejectsMissingLangOrValue() {
        bean.setNewTranslationLang(null);
        bean.setNewTranslationValue("Value");

        bean.addTranslationOption();

        assertTrue(bean.getTranslationOptions().isEmpty());
    }

    @Test
    void addTranslationOption_addsNewOption() {
        bean.setNewTranslationLang("en");
        bean.setNewTranslationValue("New value");

        bean.addTranslationOption();

        assertEquals(1, bean.getTranslationOptions().size());
        assertTrue(bean.getTranslationOptions().get(0).isToAdd());
    }

    @Test
    void removeTranslationOption_marksExistingForRemoval() {
        var option = new PropositionTranslationOption();
        option.setOldValue("Existing");
        bean.getTranslationOptions().add(option);

        bean.removeTranslationOption(option);

        assertTrue(option.isToRemove());
        assertTrue(bean.getTranslationOptions().contains(option));
    }

    @Test
    void undoRemoveTranslationOption_clearsRemovalFlag() {
        var option = new PropositionTranslationOption();
        option.setToRemove(true);

        bean.undoRemoveTranslationOption(option);

        assertFalse(option.isToRemove());
    }

    @Test
    void markTranslationUpdated_setsFlagWhenValueChanged() {
        var option = new PropositionTranslationOption();
        option.setOldValue("Old");
        option.setValue("New");

        bean.markTranslationUpdated(option);

        assertTrue(option.isToUpdate());
    }

    @Test
    void markTranslationUpdated_clearsFlagWhenValueReverted() {
        var option = new PropositionTranslationOption();
        option.setOldValue("Old");
        option.setValue("Old");
        option.setToUpdate(true);

        bean.markTranslationUpdated(option);

        assertFalse(option.isToUpdate());
    }

    @Test
    void hasStructuredChanges_falseWhenNothingProposed() {
        bean.setProposedPreferredLabel(null);

        assertFalse(bean.hasStructuredChanges());
    }

    @Test
    void hasStructuredChanges_trueWhenPreferredLabelProposed() {
        bean.setCurrentPreferredLabel("Old label");
        bean.setProposedPreferredLabel("New label");

        assertTrue(bean.hasStructuredChanges());
    }

    @Test
    void hasStructuredChanges_trueWhenSynonymFlaggedAsChanged() {
        var option = new PropositionSynonymOption();
        option.setToAdd(true);
        bean.getSynonymOptions().add(option);

        assertTrue(bean.hasStructuredChanges());
    }

    @Test
    void hasStructuredChanges_trueWhenTranslationFlaggedAsChanged() {
        var option = new PropositionTranslationOption();
        option.setToUpdate(true);
        bean.getTranslationOptions().add(option);

        assertTrue(bean.hasStructuredChanges());
    }

    @Test
    void hasStructuredChanges_trueWhenNoteHasChanged() {
        var note = new fr.cnrs.opentheso.v2.proposition.model.PropositionNoteOption();
        note.setValue("New value");
        note.setOldValue("Old value");
        bean.getNoteOptions().add(note);

        assertTrue(bean.hasStructuredChanges());
    }

    @Test
    void submit_rejectsWhenNoConceptSelected() {
        when(conceptSelectionContext.hasSelection()).thenReturn(false);

        bean.submit();

        messageUtilsStatic.verify(() -> MessageUtils.showErrorMessage("Aucun concept sélectionné"));
        verify(propositionMutationService, never()).submitDraft(any());
    }

    @Test
    void submit_rejectsBlankComment() {
        when(conceptSelectionContext.hasSelection()).thenReturn(true);
        bean.setComment(" ");

        bean.submit();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Veuillez saisir votre proposition"));
        verify(propositionMutationService, never()).submitDraft(any());
    }

    @Test
    void submit_rejectsBlankAuthorEmail() {
        when(conceptSelectionContext.hasSelection()).thenReturn(true);
        bean.setComment("A comment");
        bean.setAuthorEmail(" ");

        bean.submit();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Veuillez saisir votre adresse email"));
        verify(propositionMutationService, never()).submitDraft(any());
    }

    @Test
    void submit_warnsWhenDuplicateProposition() {
        when(conceptSelectionContext.hasSelection()).thenReturn(true);
        when(conceptSelectionContext.getSummary()).thenReturn(SUMMARY);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        bean.setComment("A comment");
        bean.setAuthorEmail("a@b.fr");
        when(propositionMutationService.submitDraft(any())).thenReturn(Optional.empty());

        bean.submit();

        messageUtilsStatic.verify(() -> MessageUtils.showWarnMessage("Vous avez déjà une proposition en cours pour ce concept"));
        verify(propositionDraftService, never()).saveDraftDetails(anyInt(), any());
    }

    @Test
    void submit_savesSubmissionAndDraftOnSuccess() {
        when(conceptSelectionContext.hasSelection()).thenReturn(true);
        when(conceptSelectionContext.getSummary()).thenReturn(SUMMARY);
        when(thesaurusContext.resolveThesaurusId()).thenReturn("TH1");
        bean.setComment("A comment");
        bean.setAuthorEmail("a@b.fr");
        when(propositionMutationService.submitDraft(any())).thenReturn(Optional.of(42));

        bean.submit();

        verify(propositionDraftService).saveDraftDetails(eq(42), any());
        messageUtilsStatic.verify(() -> MessageUtils.showInformationMessage("Votre proposition a bien été envoyée"));
        assertEquals("", bean.getComment());
    }
}
