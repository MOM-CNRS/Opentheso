package fr.cnrs.opentheso.v2.candidat.api.mapper;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidatApiMapperTest {

    @Test
    void toSummary_mapsCandidateFields() {
        CandidatDto candidat = new CandidatDto();
        candidat.setIdConcepte("C1");
        candidat.setNomPref("Label");
        candidat.setLang("fr");
        candidat.setStatut("1");
        candidat.setCreatedBy("alice");
        candidat.setCreationDate(new Date(1_700_000_000_000L));

        var response = CandidatApiMapper.toSummary(candidat);

        assertEquals("C1", response.conceptId());
        assertEquals("Label", response.preferredLabel());
        assertEquals("alice", response.createdBy());
    }

    @Test
    void toSummaries_mapsWholeList() {
        CandidatDto candidat1 = new CandidatDto();
        candidat1.setIdConcepte("C1");
        candidat1.setNomPref("Label 1");
        CandidatDto candidat2 = new CandidatDto();
        candidat2.setIdConcepte("C2");
        candidat2.setNomPref("Label 2");

        var responses = CandidatApiMapper.toSummaries(List.of(candidat1, candidat2));

        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(r -> "C1".equals(r.conceptId())));
        assertTrue(responses.stream().anyMatch(r -> "C2".equals(r.conceptId())));
    }
}
