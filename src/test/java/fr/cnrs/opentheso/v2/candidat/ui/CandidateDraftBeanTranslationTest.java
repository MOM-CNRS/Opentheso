package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.TraductionDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateDraftBeanTranslationTest {

    @Test
    void parseTranslationsPayloadReadsLangValueLines() {
        List<TraductionDto> rows = CandidateDraftBean.parseTranslationsPayload("en\tForest\nde\tWald\n");

        assertEquals(2, rows.size());
        assertEquals("en", rows.get(0).getLangue());
        assertEquals("Forest", rows.get(0).getTraduction());
        assertEquals("de", rows.get(1).getLangue());
        assertEquals("Wald", rows.get(1).getTraduction());
    }

    @Test
    void parseTranslationsPayloadSkipsBlankAndIncompleteLines() {
        List<TraductionDto> rows = CandidateDraftBean.parseTranslationsPayload("\nfr\nes\tBosque\n  \t \n");

        assertEquals(1, rows.size());
        assertEquals("es", rows.get(0).getLangue());
        assertEquals("Bosque", rows.get(0).getTraduction());
    }

    @Test
    void parseTranslationsPayloadIgnoresBlankValues() {
        assertTrue(CandidateDraftBean.parseTranslationsPayload("en\t   ").isEmpty());
        assertTrue(CandidateDraftBean.parseTranslationsPayload(null).isEmpty());
    }

    @Test
    void parseNotesPayloadReadsEncodedRows() {
        List<CandidateDraftBean.DraftNote> rows = CandidateDraftBean.parseNotesPayload(
                "scopeNote\tfr\tNote%20d%27application\tsource%20A\nexample\ten\tAn%20example\t");

        assertEquals(2, rows.size());
        assertEquals("scopeNote", rows.get(0).type());
        assertEquals("fr", rows.get(0).lang());
        assertEquals("Note d'application", rows.get(0).value());
        assertEquals("source A", rows.get(0).source());
        assertEquals("example", rows.get(1).type());
        assertEquals("An example", rows.get(1).value());
    }
}
