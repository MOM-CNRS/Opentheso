package fr.cnrs.opentheso.v2.proposition.service;

import fr.cnrs.opentheso.entites.PropositionModificationDetail;
import fr.cnrs.opentheso.repositories.PropositionModificationDetailRepository;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
import fr.cnrs.opentheso.v2.proposition.model.PropositionAcceptance;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropositionDraftService {

    private static final List<PropositionFieldCategory> NOTE_CATEGORIES = List.of(
            PropositionFieldCategory.NOTE,
            PropositionFieldCategory.DEFINITION,
            PropositionFieldCategory.CHANGE_NOTE,
            PropositionFieldCategory.SCOPE,
            PropositionFieldCategory.EDITORIAL_NOTE,
            PropositionFieldCategory.EXAMPLE,
            PropositionFieldCategory.HISTORY
    );

    private final PropositionModificationDetailRepository propositionModificationDetailRepository;
    private final ConceptReadService conceptReadService;
    private final ConceptLifecycleMutationService conceptLifecycleMutationService;
    private final ConceptLexicalMutationService conceptLexicalMutationService;
    private final ConceptNoteMutationService conceptNoteMutationService;

    @Transactional
    public void saveDraftDetails(int propositionId, PropositionDraft draft) {
        if (draft == null) {
            return;
        }
        if (draft.getPreferredLabelChange() != null) {
            saveDetail(propositionId, draft.getPreferredLabelChange());
        }
        draft.getSynonymChanges().forEach(change -> saveDetail(propositionId, change));
        draft.getTranslationChanges().forEach(change -> saveDetail(propositionId, change));
        draft.getNoteChanges().values().forEach(change -> saveDetail(propositionId, change));
    }

    @Transactional(readOnly = true)
    public PropositionDraft loadDraftChanges(int propositionId) {
        var draft = new PropositionDraft();
        for (var detail : propositionModificationDetailRepository.findAllByIdProposition(propositionId)) {
            var category = PropositionFieldCategory.valueOf(detail.getCategorie());
            var change = new PropositionFieldChange(
                    category,
                    PropositionFieldAction.valueOf(detail.getAction()),
                    detail.getLang(),
                    detail.getValue(),
                    detail.getOldValue(),
                    detail.isHiden()
            );
            switch (category) {
                case NOM -> draft.setPreferredLabelChange(change);
                case SYNONYME -> draft.getSynonymChanges().add(change);
                case TRADUCTION -> draft.getTranslationChanges().add(change);
                default -> draft.setNoteChange(change);
            }
        }
        return draft;
    }

    /**
     * Applique les catégories acceptées de la proposition au concept, en s'appuyant sur les
     * services d'écriture concept v2 déjà existants (indépendant de la logique legacy).
     * Retourne la liste des messages d'erreur rencontrés (vide si tout a réussi).
     */
    @Transactional
    public List<String> applyAcceptedChanges(
            PropositionDraft draft,
            String thesaurusId,
            String conceptId,
            String lang,
            int userId,
            String contributorName,
            PropositionAcceptance acceptance
    ) {
        List<String> errors = new ArrayList<>();
        if (draft == null || acceptance == null) {
            return errors;
        }

        if (acceptance.preferredLabel() && draft.getPreferredLabelChange() != null) {
            var result = conceptLifecycleMutationService.renamePreferredLabel(new RenamePreferredLabelCommand(
                    thesaurusId, conceptId, lang, userId, contributorName,
                    draft.getPreferredLabelChange().value(), "proposition", false));
            collectError(result, errors);
        }

        if (acceptance.synonyms()) {
            for (var change : draft.getSynonymChanges()) {
                applySynonymChange(thesaurusId, conceptId, userId, contributorName, change, errors);
            }
        }

        if (acceptance.translations()) {
            for (var change : draft.getTranslationChanges()) {
                applyTranslationChange(thesaurusId, conceptId, userId, contributorName, change, errors);
            }
        }

        for (var category : NOTE_CATEGORIES) {
            if (!acceptance.isAccepted(category)) {
                continue;
            }
            var change = draft.getNoteChange(category.noteTypeCode());
            if (change != null) {
                applyNoteChange(thesaurusId, conceptId, userId, contributorName, category, change, errors);
            }
        }

        return errors;
    }

    private void saveDetail(int propositionId, PropositionFieldChange change) {
        propositionModificationDetailRepository.save(PropositionModificationDetail.builder()
                .idProposition(propositionId)
                .categorie(change.category().name())
                .action(change.action().name())
                .lang(change.lang())
                .value(change.value())
                .oldValue(change.oldValue())
                .hiden(change.hidden())
                .build());
    }

    private void applySynonymChange(String thesaurusId, String conceptId, int userId, String contributorName,
                                     PropositionFieldChange change, List<String> errors) {
        MutationResult result = switch (change.action()) {
            case ADD -> conceptLexicalMutationService.addSynonym(new AddSynonymCommand(
                    thesaurusId, conceptId, change.lang(), change.value(), change.hidden(), userId, contributorName, false));
            case UPDATE -> conceptLexicalMutationService.updateSynonym(new UpdateSynonymCommand(
                    thesaurusId, conceptId, change.lang(), change.oldValue(), change.value(), change.hidden(), userId, contributorName, false));
            case DELETE -> conceptLexicalMutationService.deleteSynonym(new DeleteSynonymCommand(
                    thesaurusId, conceptId, change.lang(), change.value(), userId, contributorName));
        };
        collectError(result, errors);
    }

    private void applyTranslationChange(String thesaurusId, String conceptId, int userId, String contributorName,
                                         PropositionFieldChange change, List<String> errors) {
        MutationResult result = switch (change.action()) {
            case ADD -> conceptLexicalMutationService.addTranslation(new AddTranslationCommand(
                    thesaurusId, conceptId, change.lang(), change.value(), userId, contributorName));
            case UPDATE -> conceptLexicalMutationService.updateTranslation(new UpdateTranslationCommand(
                    thesaurusId, conceptId, change.lang(), change.value(), userId, contributorName));
            case DELETE -> conceptLexicalMutationService.deleteTranslation(new DeleteTranslationCommand(
                    thesaurusId, conceptId, change.lang(), userId, contributorName));
        };
        collectError(result, errors);
    }

    private void applyNoteChange(String thesaurusId, String conceptId, int userId, String contributorName,
                                  PropositionFieldCategory category, PropositionFieldChange change, List<String> errors) {
        String typeCode = category.noteTypeCode();
        if (change.action() == PropositionFieldAction.DELETE) {
            int noteId = resolveNoteId(thesaurusId, conceptId, change.lang(), typeCode);
            if (noteId <= 0) {
                return;
            }
            collectError(conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                    thesaurusId, conceptId, noteId, change.lang(), typeCode, userId, contributorName)), errors);
            return;
        }
        collectError(conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                thesaurusId, conceptId, change.lang(), typeCode, change.value(), "proposition", userId, contributorName)), errors);
    }

    private int resolveNoteId(String thesaurusId, String conceptId, String lang, String typeCode) {
        return conceptReadService.loadDetail(thesaurusId, conceptId, lang)
                .flatMap(detail -> detail.notes().stream()
                        .filter(note -> typeCode.equals(note.typeCode()))
                        .map(ConceptNote::id)
                        .findFirst())
                .map(this::safeParse)
                .orElse(-1);
    }

    private int safeParse(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void collectError(MutationResult result, List<String> errors) {
        if (result != null && !result.success()) {
            errors.add(result.message());
        }
    }
}
