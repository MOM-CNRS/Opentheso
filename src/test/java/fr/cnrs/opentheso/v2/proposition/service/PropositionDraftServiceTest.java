package fr.cnrs.opentheso.v2.proposition.service;

import fr.cnrs.opentheso.entites.PropositionModificationDetail;
import fr.cnrs.opentheso.repositories.PropositionModificationDetailRepository;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptSummary;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.proposition.model.PropositionAcceptance;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropositionDraftServiceTest {

    @Mock private PropositionModificationDetailRepository propositionModificationDetailRepository;
    @Mock private ConceptReadService conceptReadService;
    @Mock private ConceptLifecycleMutationService conceptLifecycleMutationService;
    @Mock private ConceptLexicalMutationService conceptLexicalMutationService;
    @Mock private ConceptNoteMutationService conceptNoteMutationService;

    private PropositionDraftService service;

    @BeforeEach
    void setUp() {
        service = new PropositionDraftService(
                propositionModificationDetailRepository,
                conceptReadService,
                conceptLifecycleMutationService,
                conceptLexicalMutationService,
                conceptNoteMutationService
        );
    }

    @Test
    void saveDraftDetails_savesOneRowPerChange() {
        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New", "Old", false));
        draft.getSynonymChanges().add(new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Syn", null, false));
        draft.setNoteChange(new PropositionFieldChange(
                PropositionFieldCategory.DEFINITION, PropositionFieldAction.ADD, "fr", "Def", null, false));

        service.saveDraftDetails(7, draft);

        ArgumentCaptor<PropositionModificationDetail> captor = ArgumentCaptor.forClass(PropositionModificationDetail.class);
        verify(propositionModificationDetailRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(d -> d.getIdProposition() == 7));
    }

    @Test
    void loadDraftChanges_reconstructsDraftFromStoredDetails() {
        when(propositionModificationDetailRepository.findAllByIdProposition(7)).thenReturn(List.of(
                PropositionModificationDetail.builder().idProposition(7).categorie("NOM").action("UPDATE")
                        .lang("fr").value("New label").oldValue("Old label").build(),
                PropositionModificationDetail.builder().idProposition(7).categorie("SYNONYME").action("ADD")
                        .lang("fr").value("Syn1").build(),
                PropositionModificationDetail.builder().idProposition(7).categorie("TRADUCTION").action("DELETE")
                        .lang("en").build(),
                PropositionModificationDetail.builder().idProposition(7).categorie("DEFINITION").action("ADD")
                        .lang("fr").value("Def value").build()
        ));

        var draft = service.loadDraftChanges(7);

        assertEquals("New label", draft.getPreferredLabelChange().value());
        assertEquals(1, draft.getSynonymChanges().size());
        assertEquals(1, draft.getTranslationChanges().size());
        assertEquals(PropositionFieldAction.DELETE, draft.getTranslationChanges().get(0).action());
        assertEquals("Def value", draft.getNoteChange("definition").value());
    }

    @Test
    void applyAcceptedChanges_appliesOnlyAcceptedCategories() {
        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New label", "Old label", false));
        draft.getSynonymChanges().add(new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Syn", null, false));

        when(conceptLifecycleMutationService.renamePreferredLabel(any(RenamePreferredLabelCommand.class)))
                .thenReturn(MutationResult.ok("ok"));

        var acceptance = new PropositionAcceptance(
                true, false, false, false, false, false, false, false, false, false);

        var errors = service.applyAcceptedChanges(draft, "TH1", "C1", "fr", 7, "admin", acceptance);

        assertTrue(errors.isEmpty());
        verify(conceptLifecycleMutationService).renamePreferredLabel(any(RenamePreferredLabelCommand.class));
        verify(conceptLexicalMutationService, never()).addSynonym(any());
    }

    @Test
    void applyAcceptedChanges_appliesSynonymAndTranslationChanges() {
        var draft = new PropositionDraft();
        draft.getSynonymChanges().add(new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.ADD, "fr", "Syn", null, false));
        draft.getSynonymChanges().add(new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME, PropositionFieldAction.DELETE, "fr", "Old syn", "Old syn", false));
        draft.getTranslationChanges().add(new PropositionFieldChange(
                PropositionFieldCategory.TRADUCTION, PropositionFieldAction.ADD, "en", "New value", null, false));

        when(conceptLexicalMutationService.addSynonym(any(AddSynonymCommand.class))).thenReturn(MutationResult.ok("ok"));
        when(conceptLexicalMutationService.deleteSynonym(any(DeleteSynonymCommand.class))).thenReturn(MutationResult.ok("ok"));
        when(conceptLexicalMutationService.addTranslation(any(AddTranslationCommand.class))).thenReturn(MutationResult.ok("ok"));

        var acceptance = new PropositionAcceptance(
                false, true, true, false, false, false, false, false, false, false);

        var errors = service.applyAcceptedChanges(draft, "TH1", "C1", "fr", 7, "admin", acceptance);

        assertTrue(errors.isEmpty());
        verify(conceptLexicalMutationService).addSynonym(any(AddSynonymCommand.class));
        verify(conceptLexicalMutationService).deleteSynonym(any(DeleteSynonymCommand.class));
        verify(conceptLexicalMutationService).addTranslation(any(AddTranslationCommand.class));
    }

    @Test
    void applyAcceptedChanges_upsertsAcceptedNoteAddition() {
        var draft = new PropositionDraft();
        draft.setNoteChange(new PropositionFieldChange(
                PropositionFieldCategory.DEFINITION, PropositionFieldAction.ADD, "fr", "New definition", null, false));

        when(conceptNoteMutationService.upsertNote(any(UpsertNoteCommand.class))).thenReturn(MutationResult.ok("ok"));

        var acceptance = new PropositionAcceptance(
                false, false, false, false, true, false, false, false, false, false);

        var errors = service.applyAcceptedChanges(draft, "TH1", "C1", "fr", 7, "admin", acceptance);

        assertTrue(errors.isEmpty());
        ArgumentCaptor<UpsertNoteCommand> captor = ArgumentCaptor.forClass(UpsertNoteCommand.class);
        verify(conceptNoteMutationService).upsertNote(captor.capture());
        assertEquals("definition", captor.getValue().typeCode());
        assertEquals("New definition", captor.getValue().value());
    }

    @Test
    void applyAcceptedChanges_deletesNoteByResolvingCurrentNoteId() {
        var draft = new PropositionDraft();
        draft.setNoteChange(new PropositionFieldChange(
                PropositionFieldCategory.SCOPE, PropositionFieldAction.DELETE, "fr", "", "Old scope", false));

        var summary = new ConceptSummary("C1", "TH1", "Label", "fr", "valid", null, null, null, null, null, null);
        var note = new ConceptNote("15", "scopeNote", "fr", "Old scope");
        var detail = new ConceptDetail(summary, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(note), List.of(), List.of(), List.of(), List.of(), List.of());
        when(conceptReadService.loadDetail("TH1", "C1", "fr")).thenReturn(Optional.of(detail));
        when(conceptNoteMutationService.deleteNote(any(DeleteNoteCommand.class))).thenReturn(MutationResult.ok("ok"));

        var acceptance = new PropositionAcceptance(
                false, false, false, false, false, false, true, false, false, false);

        var errors = service.applyAcceptedChanges(draft, "TH1", "C1", "fr", 7, "admin", acceptance);

        assertTrue(errors.isEmpty());
        ArgumentCaptor<DeleteNoteCommand> captor = ArgumentCaptor.forClass(DeleteNoteCommand.class);
        verify(conceptNoteMutationService).deleteNote(captor.capture());
        assertEquals(15, captor.getValue().noteId());
    }

    @Test
    void applyAcceptedChanges_collectsErrorMessagesFromFailedMutations() {
        var draft = new PropositionDraft();
        draft.setPreferredLabelChange(new PropositionFieldChange(
                PropositionFieldCategory.NOM, PropositionFieldAction.UPDATE, "fr", "New label", "Old label", false));

        when(conceptLifecycleMutationService.renamePreferredLabel(any(RenamePreferredLabelCommand.class)))
                .thenReturn(new MutationResult(false, MutationOutcome.DUPLICATE_LABEL, "Le libellé existe déjà", null));

        var acceptance = new PropositionAcceptance(
                true, false, false, false, false, false, false, false, false, false);

        var errors = service.applyAcceptedChanges(draft, "TH1", "C1", "fr", 7, "admin", acceptance);

        assertEquals(1, errors.size());
        assertEquals("Le libellé existe déjà", errors.get(0));
    }

    @Test
    void applyAcceptedChanges_returnsEmptyForNullDraftOrAcceptance() {
        assertTrue(service.applyAcceptedChanges(null, "TH1", "C1", "fr", 7, "admin", PropositionAcceptance.none()).isEmpty());
        assertTrue(service.applyAcceptedChanges(new PropositionDraft(), "TH1", "C1", "fr", 7, "admin", null).isEmpty());
    }
}
