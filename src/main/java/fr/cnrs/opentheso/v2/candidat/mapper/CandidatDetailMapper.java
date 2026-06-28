package fr.cnrs.opentheso.v2.candidat.mapper;

import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.candidats.TraductionDto;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatConceptRelationRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatDiscussionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatIdValueRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatNoteDetailRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatTranslationRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CandidatDetailMapper {

    private CandidatDetailMapper() {
    }

    public static List<NodeIdValue> toNodeIdValues(List<CandidatIdValueRow> rows) {
        return rows.stream()
                .map(row -> NodeIdValue.builder().id(row.id()).value(row.value()).build())
                .toList();
    }

    public static Map<String, List<NodeIdValue>> groupBroaderRelationsByConcept(List<CandidatConceptRelationRow> rows) {
        Map<String, List<NodeIdValue>> grouped = new HashMap<>();
        for (CandidatConceptRelationRow row : rows) {
            grouped.computeIfAbsent(row.conceptId(), key -> new ArrayList<>())
                    .add(NodeIdValue.builder().id(row.relatedConceptId()).value(row.label()).build());
        }
        return grouped;
    }

    public static List<NodeNote> toNodeNotes(List<CandidatNoteDetailRow> rows, Set<String> votedNoteIds) {
        return rows.stream()
                .map(row -> NodeNote.builder()
                        .idNote(row.id())
                        .noteTypeCode(row.noteTypeCode())
                        .idConcept(row.idConcept())
                        .lang(row.lang())
                        .lexicalValue(row.lexicalValue())
                        .idUser(row.idUser())
                        .voted(votedNoteIds.contains(String.valueOf(row.id())))
                        .build())
                .toList();
    }

    public static List<TraductionDto> toTraductions(List<CandidatTranslationRow> rows) {
        return rows.stream()
                .map(row -> TraductionDto.builder()
                        .langue(row.lang())
                        .traduction(row.lexicalValue())
                        .codePays(row.countryCode())
                        .build())
                .toList();
    }

    public static List<MessageDto> toMessages(List<CandidatDiscussionRow> rows, int currentUserId) {
        return rows.stream()
                .map(row -> MessageDto.builder()
                        .msg(row.value())
                        .nom(row.username())
                        .idUser(row.idUser())
                        .mine(currentUserId == row.idUser())
                        .date(row.date())
                        .build())
                .toList();
    }

    public static void applyBroaderTerms(CandidatDto candidat, Map<String, List<NodeIdValue>> broaderByConcept) {
        candidat.setTermesGenerique(broaderByConcept.getOrDefault(candidat.getIdConcepte(), List.of()));
    }

    public static void applyDetails(
            CandidatDto candidat,
            String preferredTermId,
            List<CandidatIdValueRow> collections,
            List<CandidatIdValueRow> broaderRelations,
            List<CandidatIdValueRow> relatedRelations,
            List<String> synonyms,
            List<CandidatNoteDetailRow> notes,
            Set<String> votedNoteIds,
            List<CandidatTranslationRow> translations,
            List<CandidatDiscussionRow> messages,
            boolean voted,
            List<NodeAlignment> alignments,
            List<NodeImage> images,
            int currentUserId
    ) {
        candidat.setIdTerm(preferredTermId);
        candidat.setCollections(toNodeIdValues(collections));
        candidat.setTermesGenerique(toNodeIdValues(broaderRelations));
        candidat.setTermesAssocies(toNodeIdValues(relatedRelations));
        candidat.setEmployePourList(new ArrayList<>(synonyms));
        candidat.setNodeNotes(toNodeNotes(notes, votedNoteIds));
        candidat.setTraductions(toTraductions(translations));
        candidat.setMessages(toMessages(messages, currentUserId));
        candidat.setVoted(voted);
        candidat.setAlignments(alignments);
        candidat.setImages(images);
    }

    public static List<String> extractConceptIds(List<CandidatDto> candidates) {
        return candidates.stream()
                .map(CandidatDto::getIdConcepte)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
