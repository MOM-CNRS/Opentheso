package fr.cnrs.opentheso.v2.candidat.api.mapper;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
