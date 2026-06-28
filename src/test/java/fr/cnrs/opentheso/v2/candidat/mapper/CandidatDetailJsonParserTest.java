package fr.cnrs.opentheso.v2.candidat.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidatDetailJsonParserTest {

    @Test
    void parse_readsCollectionsNotesAndVotes() {
        var parsed = CandidatDetailJsonParser.parse(
                "[{\"id\":\"G1\",\"value\":\"Collection 1\"}]",
                "[{\"id\":\"BT1\",\"value\":\"Broader\"}]",
                "[]",
                "[\"synonyme\"]",
                "[{\"id\":10,\"noteTypeCode\":\"note\",\"idConcept\":\"C1\",\"lang\":\"fr\",\"lexicalValue\":\"text\",\"idUser\":2}]",
                "[\"10\",\"11\"]",
                "[{\"lang\":\"en\",\"lexicalValue\":\"Hello\",\"countryCode\":\"gb\"}]",
                "[{\"idUser\":3,\"username\":\"alice\",\"value\":\"msg\",\"date\":\"2024-01-01\"}]"
        );

        assertEquals(1, parsed.collections().size());
        assertEquals("G1", parsed.collections().get(0).id());
        assertEquals(1, parsed.broaderRelations().size());
        assertEquals("synonyme", parsed.synonyms().get(0));
        assertEquals(10, parsed.notes().get(0).id());
        assertTrue(parsed.votedNoteIds().contains("10"));
        assertEquals("en", parsed.translations().get(0).lang());
        assertEquals("alice", parsed.messages().get(0).username());
    }
}
