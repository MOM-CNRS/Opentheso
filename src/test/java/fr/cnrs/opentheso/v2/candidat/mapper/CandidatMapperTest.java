package fr.cnrs.opentheso.v2.candidat.mapper;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatListRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidatMapperTest {

    @Test
    void toCandidatDto_mapsListRowFields() {
        var row = new CandidatListRow(
                "C1",
                LocalDateTime.of(2024, Month.JANUARY, 2, 10, 0),
                LocalDateTime.of(2024, Month.JANUARY, 3, 11, 0),
                7,
                9,
                "admin msg",
                "Label",
                "creator",
                "admin",
                3,
                2,
                5,
                1
        );

        var dto = CandidatMapper.toCandidatDto(row, "TH1", CandidatStatusCode.PENDING);

        assertEquals("C1", dto.getIdConcepte());
        assertEquals("TH1", dto.getIdThesaurus());
        assertEquals("Label", dto.getNomPref());
        assertEquals("creator", dto.getCreatedBy());
        assertEquals("admin", dto.getCreatedByAdmin());
        assertEquals(7, dto.getCreatedById());
        assertEquals(9, dto.getCreatedByIdAdmin());
        assertEquals(3, dto.getNbrParticipant());
        assertEquals(2, dto.getNbrDemande());
        assertEquals(5, dto.getNbrVote());
        assertEquals(1, dto.getNbrNoteVote());
        assertEquals(String.valueOf(CandidatStatusCode.PENDING), dto.getStatut());
        assertTrue(dto.getAlignments().isEmpty());
    }

    @Test
    void toCandidatDtos_mapsAllRows() {
        var rows = List.of(
                new CandidatListRow("C1", null, null, 1, null, "", "A", "u1", "Utilisateur inconnu", 0, 0, 0, 0),
                new CandidatListRow("C2", null, null, 2, null, "", "B", "u2", "Utilisateur inconnu", 0, 0, 0, 0)
        );

        var dtos = CandidatMapper.toCandidatDtos(rows, "TH1", CandidatStatusCode.ACCEPTED);

        assertEquals(2, dtos.size());
        assertEquals("C2", dtos.get(1).getIdConcepte());
        assertEquals(String.valueOf(CandidatStatusCode.ACCEPTED), dtos.get(0).getStatut());
    }
}
