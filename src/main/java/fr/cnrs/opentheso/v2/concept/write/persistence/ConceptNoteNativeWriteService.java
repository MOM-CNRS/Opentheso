package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.utils.StringUtils;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptNoteNativeWriteService {

    private final ConceptNoteWriteRepository conceptNoteWriteRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @Transactional
    public MutationResult upsertNote(UpsertNoteCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.value())) {
            return MutationResult.validationError("La note ne doit pas être vide !");
        }
        String lang = normalizeLang(command.lang());
        String cleanedValue = StringUtils.clearNoteFromP(StringUtils.clearValue(command.value()));
        cleanedValue = StringEscapeUtils.unescapeXml(cleanedValue);
        String cleanedSource = StringUtils.clearValue(org.apache.commons.lang3.StringUtils.defaultString(command.source()));
        String storedValue = StringUtils.convertString(cleanedValue);

        var existingId = conceptNoteWriteRepository.findNoteId(
                command.conceptId(), command.thesaurusId(), lang, command.typeCode());
        if (existingId.isPresent()) {
            if (!conceptNoteWriteRepository.updateNote(
                    existingId.get(), command.thesaurusId(), cleanedValue, cleanedSource)) {
                return MutationResult.failure("Erreur de modification !");
            }
            conceptNoteWriteRepository.insertNoteHistory(
                    command.conceptId(), command.thesaurusId(), lang, command.typeCode(),
                    storedValue, "update", command.userId());
        } else {
            if (conceptNoteWriteRepository.existsWithValue(
                    command.conceptId(), command.thesaurusId(), lang, command.typeCode(), storedValue)) {
                return MutationResult.validationError("Cette note existe déjà !");
            }
            conceptNoteWriteRepository.insertNote(
                    command.conceptId(), command.thesaurusId(), lang, command.typeCode(),
                    cleanedValue, cleanedSource, command.userId());
        }
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Note enregistrée avec succès");
    }

    @Transactional
    public MutationResult deleteNote(DeleteNoteCommand command) {
        if (command.noteId() <= 0) {
            return MutationResult.validationError("Aucune note sélectionnée !");
        }
        String lang = normalizeLang(command.lang());
        String oldValue = conceptNoteWriteRepository.findNoteLexicalValue(
                command.conceptId(), command.thesaurusId(), lang, command.typeCode())
                .orElse("");
        conceptNoteWriteRepository.deleteNote(command.noteId(), command.thesaurusId());
        conceptNoteWriteRepository.insertNoteHistory(
                command.conceptId(), command.thesaurusId(), lang, command.typeCode(),
                StringUtils.convertString(oldValue),
                "delete",
                command.userId());
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Note supprimée avec succès");
    }

    private void finalizeMutation(String thesaurusId, String conceptId, int userId, String contributorName) {
        conceptWritePostMutationRepository.touchConcept(thesaurusId, conceptId, userId);
        conceptWritePostMutationRepository.saveContributorDcTerm(
                thesaurusId, conceptId, org.apache.commons.lang3.StringUtils.defaultString(contributorName));
    }

    private String normalizeLang(String lang) {
        return switch (lang) {
            case "en-GB", "en-US" -> "en";
            case "pt-BR", "pt-PT" -> "pt";
            default -> lang;
        };
    }
}
