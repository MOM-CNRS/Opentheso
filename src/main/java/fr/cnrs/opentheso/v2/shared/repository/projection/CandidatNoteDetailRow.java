package fr.cnrs.opentheso.v2.shared.repository.projection;

public record CandidatNoteDetailRow(
        int id,
        String noteTypeCode,
        String idConcept,
        String lang,
        String lexicalValue,
        Integer idUser
) {
}
