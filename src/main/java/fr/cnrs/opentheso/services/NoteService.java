package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.NoteHistorique;
import fr.cnrs.opentheso.entites.NoteType;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.repositories.NoteHistoriqueRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.NoteTypeRepository;
import fr.cnrs.opentheso.utils.StringUtils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;


@Slf4j
@Service
@AllArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteTypeRepository noteTypeRepository;
    private final NoteHistoriqueRepository noteHistoriqueRepository;


    public NodeNote getNodeNote(String idConcept, String idThesaurus, String idLang, String noteType) {

        log.debug("Recherche d'une note avec id Concept {}, id Thésaurus {}, lang {}, noteType {}", idConcept, idThesaurus, idLang, noteType);
        idLang = normalizeIdLang(idLang);
        var note = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(idConcept, idThesaurus, noteType, idLang);
        if (note.isEmpty()) {
            log.debug("Aucune note n'est trouvée");
            return null;
        }

        return NodeNote.builder()
                .idConcept(idConcept)
                .idNote(note.get(0).getId())
                .lang(note.get(0).getLang())
                .lexicalValue(note.get(0).getLexicalValue())
                .modified(note.get(0).getModified())
                .created(note.get(0).getCreated())
                .noteTypeCode(note.get(0).getNoteTypeCode())
                .noteSource(note.get(0).getNoteSource())
                .identifier(note.get(0).getIdentifier())
                .build();
    }

    public boolean updateNote(int idNote, String idConcept, String idLang, String idThesaurus,
                              String note, String noteSource, String noteTypeCode, int idUser) {

        log.debug("Mise à jour de la note id {}", idNote);
        var noteValue = noteRepository.findByIdAndIdThesaurus(idNote, idThesaurus);
        if (noteValue.isEmpty()) {
            log.debug("Aucune note n'existe pas l'id {}", idNote);
            return false;
        }

        log.debug("Mise à jour de la note id {} ", idNote);
        noteValue.get().setLexicalValue(StringUtils.clearNoteFromP(note));
        noteValue.get().setNoteSource(noteSource);
        noteValue.get().setModified(new Date());
        noteRepository.save(noteValue.get());

        idLang = normalizeIdLang(idLang);
        addConceptNoteHistorique(idConcept, idLang, idThesaurus, note, noteTypeCode, "update", idUser);
        return true;
    }

    public void addConceptNoteHistorique(String idConcept, String idLang, String idThesausus, String note,
                                         String noteTypeCode, String actionPerformed, int idUser) {

        log.debug("Ajout de l'historique des actions sur les notes");
        noteHistoriqueRepository.save(NoteHistorique.builder()
                .idThesaurus(idThesausus)
                .idConcept(idConcept)
                .lang(idLang)
                .lexicalvalue(fr.cnrs.opentheso.utils.StringUtils.convertString(note))
                .actionPerformed(actionPerformed)
                .idUser(idUser)
                .notetypecode(noteTypeCode)
                .modified(new Date())
                .build());
    }

    public void deleteByThesaurus(String idThesaurus) {

        log.debug("Suppression des notes de thesaurus: " + idThesaurus);
        noteRepository.deleteAllByIdThesaurus(idThesaurus);
        noteHistoriqueRepository.deleteAllByIdThesaurus(idThesaurus);

    }

    public void updateThesaurusId(String oldIdThesaurus, String newIdThesaurus) {

        log.debug("Mise à jour des thésaurus id pour les notes présents dnas le thésaurus: " + oldIdThesaurus);
        noteRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        noteHistoriqueRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    public List<NodeNote> getNotesCandidat(String identifier, String idThesaurus) {

        log.debug("Recherche des notes du candidat {} (id thésaurus {})", identifier, idThesaurus);
        var notes = noteRepository.findAllByIdentifierAndIdThesaurus(identifier, idThesaurus);
        if (notes.isEmpty()) {
            log.debug("Aucune note trouvée pour le candidat {} (id thésaurus {})", identifier, idThesaurus);
        }

        log.debug("{} notes trouvées pour le candidat {} (id thésaurus {})", notes.size(), identifier, idThesaurus);
        return notes.stream()
                .map(note -> NodeNote.builder()
                        .idNote(note.getId())
                        .noteTypeCode(note.getNoteTypeCode())
                        .idConcept(note.getIdConcept())
                        .lang(note.getLang())
                        .lexicalValue(note.getLexicalValue())
                        .idUser(note.getIdUser())
                        .build())
                .toList();
    }

    public void addNote(String identifier, String idLang, String idThesaurus, String note, String noteTypeCode, String noteSource, int idUser) {

        idLang = normalizeIdLang(idLang);

        note = StringUtils.clearValue(note);
        note = StringUtils.clearNoteFromP(note);
        note = StringEscapeUtils.unescapeXml(note);
      //  note = StringUtils.convertString(note);
      //  noteSource = StringUtils.convertString(noteSource);

        if(isNoteExistInThatLang(identifier, idThesaurus, idLang, noteTypeCode)) {
            log.debug("Mise à jour d'une note existante");
            var noteToUpdate = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(identifier, idThesaurus, noteTypeCode, idLang);
            if (!noteToUpdate.isEmpty()) {
                noteToUpdate.get(0).setLexicalValue(note);
                noteToUpdate.get(0).setNoteSource(noteSource);
                noteRepository.save(noteToUpdate.get(0));
            }
        } else {
            log.debug("Enregistrement d'une nouvelle note");
            noteRepository.save(Note.builder()
                    .noteTypeCode(noteTypeCode)
                    .idThesaurus(idThesaurus)
                    .lang(idLang)
                    .lexicalValue(note)
                    .identifier(identifier)
                    .noteSource(noteSource)
                    .idUser(idUser)
                    .created(new Date())
                    .modified(new Date())
                    .build());
        }
    }

    public boolean isNoteExistInThatLang(String identifier, String idThesaurus, String idLang, String noteTypeCode) {

        log.debug("Vérification l'existence du note {} dans le thésaurus id {}", identifier, idThesaurus);
        idLang = normalizeIdLang(idLang);
        var note = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(identifier, idThesaurus, noteTypeCode, idLang);
        return !note.isEmpty();
    }

    public boolean isNoteExist(String identifier, String idThesaurus, String idLang, String note, String noteTypeCode) {

        log.debug("Vérification de l'existence da la note id {} avec la valeur {} ({})", identifier, note, idLang);
        idLang = normalizeIdLang(idLang);
        var noteFound = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLangAndLexicalValue(
                identifier, idThesaurus, noteTypeCode, idLang, fr.cnrs.opentheso.utils.StringUtils.convertString(note));
        log.debug("La note {} existe : {}", identifier, noteFound.isPresent());
        return noteFound.isPresent();
    }

    public void deleteNotes(String identifier, String idThesaurus) {

        log.debug("Suppression du note id {} contenu dans le thésaurus id {}", identifier, idThesaurus);
        noteRepository.deleteAllByIdentifierAndIdThesaurus(identifier, idThesaurus);
    }

    public void deleteNoteByLang(String identifier, String idThesaurus, String idLang, String notetypecode) {

        log.debug("Suppression de toutes les notes par langue {} dans le thésaurus {}", idLang, idThesaurus);
        noteRepository.deleteAllByIdThesaurusAndIdentifierAndLangAndNoteTypeCode(idThesaurus, identifier, idLang, notetypecode);
    }

    private String normalizeIdLang(String idLang){
        return switch (idLang) {
            case "en-GB" -> "en";
            case "en-US" -> "en";
            case "pt-BR" -> "pt";
            case "pt-PT" -> "pt";
            default -> idLang;
        };
    }

    public List<NodeNote> getListNotesAllLang(String identifier, String idThesaurus) {

        log.debug("Rechercher la liste des notes dans tous les langues avec l'identifier {}", identifier);
        var notes = noteRepository.findAllByIdentifierAndIdThesaurus(identifier, idThesaurus);
        if (notes.isEmpty()) {
            log.debug("Aucune note n'est trouvée");
            return List.of();
        }

        log.debug("{} notes trouvées pour le candidat {}", notes.size(), idThesaurus);
        return notes.stream()
                .map(element -> NodeNote.builder()
                        .idTerm(identifier)
                        .idNote(element.getId())
                        .lang(element.getLang())
                        .lexicalValue(element.getLexicalValue())
                        .modified(element.getModified())
                        .created(element.getCreated())
                        .noteTypeCode(element.getNoteTypeCode())
                        .noteSource(element.getNoteSource())
                        .identifier(element.getIdentifier())
                        .build())
                .toList();
    }

    public int getNoteByValueAndThesaurus(String value, String noteTypeCode, String idLang, String idThesaurus) {

        log.debug("Recherche de l'Id note par value {}, lang {}, idThesaurus {} et NoteType {}", value, idThesaurus, noteTypeCode, idLang);
        value = fr.cnrs.opentheso.utils.StringUtils.convertString(value);
        var note = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(value, idThesaurus, noteTypeCode, idLang);
        if (note.isEmpty()) {
            log.debug("Aucune note n'est trouvée");
            return -1;
        }

        return note.get(0).getId();
    }

    public List<NodeNote> getNoteByConceptAndThesaurusAndLangAndType(String idConcept, String idThesaurus, String idLang, String typeCode) {
        log.debug("Recherche des notes par concept {}, thésaurus {} et NoteType {}", idConcept, idThesaurus, typeCode);
        var notes = noteRepository.findAllByIdentifierAndIdThesaurusAndNoteTypeCodeAndLang(idConcept, idThesaurus, typeCode, idLang);
        if (CollectionUtils.isEmpty(notes)) {
            log.debug("Aucune note n'est trouvée !");
            return List.of();
        }

        log.debug("{} notes trouvée de type {} rattaché au concept id {}", notes.size(), typeCode, idConcept);
        return notes.stream()
                .map(note -> NodeNote.builder()
                    .idNote(note.getId())
                    .lexicalValue(note.getLexicalValue())
                    .modified(note.getModified())
                    .created(note.getCreated())
                    .noteTypeCode(note.getNoteTypeCode())
                    .lang(note.getLang())
                    .build())
                .toList();
    }

    public NodeNote getNoteByIdNote(int idNote) {

        log.debug("Recherche de la note avec id {}", idNote);
        var note = noteRepository.findById(idNote);
        if (note.isEmpty()) {
            log.debug("Aucune note n'existe avec l'id {}", idNote);
            return null;
        }

        return NodeNote.builder()
                .idNote(note.get().getId())
                .lexicalValue(note.get().getLexicalValue())
                .modified(note.get().getModified())
                .created(note.get().getCreated())
                .noteTypeCode(note.get().getNoteTypeCode())
                .lang(note.get().getLang())
                .build();
    }

    public NodeNote getNoteByValue(String noteValue) {

        log.debug("Recherche d'une note par sa valeur : {}", noteValue);
        var note = noteRepository.findByLexicalValue(noteValue);
        if (note.isEmpty()) {
            log.debug("Aucune note n'existe avec la valeur {}", noteValue);
            return null;
        }

        return NodeNote.builder()
                .idNote(note.get().getId())
                .lexicalValue(note.get().getLexicalValue())
                .modified(note.get().getModified())
                .created(note.get().getCreated())
                .noteTypeCode(note.get().getNoteTypeCode())
                .lang(note.get().getLang())
                .build();
    }

    public void deleteThisNote(int idNote, String identifier, String idLang, String idThesaurus, String noteTypeCode,
                                  String oldNote, int idUser) {

        log.debug("Suppression de la note id {}", idNote);
        noteRepository.deleteByIdAndIdThesaurus(idNote, idThesaurus);
        addConceptNoteHistorique(identifier, idLang, idThesaurus, oldNote, noteTypeCode, "delete", idUser);
    }

    public int getNbrNoteSansGroup(String idThesaurus, String idLang) {

        log.debug("Recherche du nombre de notes pour les concepts qui n'ont pas de collection dans le thésaurus id {}", idThesaurus);
        int nbrNoteConcepts = noteRepository.countNotesWithoutGroupByLangAndThesaurus(idThesaurus, idLang);
        int nbrNoteTerms = noteRepository.countNotesOfTermsWithoutGroup(idThesaurus, idLang);
        return nbrNoteConcepts + nbrNoteTerms;
    }

    public int getNbrNoteByGroup(String idGroup, String idThesaurus, String idLang) {

        log.debug("Recherche du nombre de notes dans les concepts qui appartiennent au groupe {}", idGroup);
        var notesCount = noteRepository.countNotesByGroupAndLangAndThesaurus(idGroup, idThesaurus, idLang);
        log.debug("{} notes trouvées", notesCount);
        return notesCount;
    }

    public List<NodeNote> getListNotes(String identifier, String idThesaurus, String idLang) {

        log.debug("Chargement des notes pour l'identifiant '{}' (thésaurus: {}, langue: {})", identifier, idThesaurus, idLang);
        var notes = noteRepository.findAllByIdentifierAndIdThesaurusAndLang(identifier, idThesaurus, idLang);
        return notes.stream().map(this::mapToNodeNote).toList();
    }

    private NodeNote mapToNodeNote(Note note) {
        return NodeNote.builder()
                .idNote(note.getId())
                .noteSource(note.getNoteSource())
                .idTerm(note.getIdTerm())
                .idConcept(note.getIdConcept())
                .lang(note.getLang())
                .lexicalValue(note.getLexicalValue())
                .created(note.getCreated())
                .modified(note.getModified())
                .idUser(note.getIdUser() != null ? note.getIdUser() : 0)
                .identifier(note.getIdentifier())
                .noteTypeCode(note.getNoteTypeCode())
                .build();
    }

    public List<NoteType> getNotesType() {

        log.debug("Recherche de tous les types note");

        List<NoteType> noteTypes = noteTypeRepository.findAll();
        List<NoteType> sorted = noteTypes.stream()
                .sorted(Comparator.comparingInt((NoteType n) -> {
                    // Priorité spéciale pour "definition" et "scopeNote" et ...
                    if ("definition".equals(n.getCode())) return 0;
                    if ("scopeNote".equals(n.getCode())) return 1;
                    if ("note".equals(n.getCode())) return 2;
                    if ("editorialNote".equals(n.getCode())) return 3;
                    return 4; // tous les autres après
                }).thenComparing(NoteType::getCode))
                .toList();
        return sorted;
    }

}
