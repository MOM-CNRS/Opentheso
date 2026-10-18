package fr.cnrs.opentheso.shared.repository.projection;

public record CandidatNoteDetailRow(
        int id,
        String noteTypeCode,
        String idConcept,
        String lang,
        String lexicalValue,
        Integer idUser
) {
}
