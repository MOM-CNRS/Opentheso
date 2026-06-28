package fr.cnrs.opentheso.v2.candidat.mapper;

import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatConceptRelationRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatNoteDetailRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidatDetailMapperTest {

    @Test
    void groupBroaderRelationsByConcept_groupsRowsByConceptId() {
        var grouped = CandidatDetailMapper.groupBroaderRelationsByConcept(List.of(
                new CandidatConceptRelationRow("C1", "BT1", "Label 1"),
                new CandidatConceptRelationRow("C1", "BT2", "Label 2"),
                new CandidatConceptRelationRow("C2", "BT3", "Label 3")
        ));

        assertEquals(2, grouped.get("C1").size());
        assertEquals("BT3", grouped.get("C2").get(0).getId());
    }

    @Test
    void toNodeNotes_marksVotedNotes() {
        var notes = CandidatDetailMapper.toNodeNotes(
                List.of(new CandidatNoteDetailRow(10, "note", "C1", "fr", "text", 1)),
                Set.of("10")
        );

        assertTrue(notes.get(0).isVoted());
    }
}
