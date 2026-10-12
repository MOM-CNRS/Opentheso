package fr.cnrs.opentheso.v2.concept.search.service;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ParsedSearchQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchQueryParserTest {

    @Test
    void parse_blank_returnsEmpty() {
        ParsedSearchQuery parsed = SearchQueryParser.parse("  ");
        assertTrue(parsed.isBlank());
        assertFalse(parsed.hasOperators());
    }

    @Test
    void parse_lexicalOnly_isFullText() {
        ParsedSearchQuery parsed = SearchQueryParser.parse("obsidienne");
        assertFalse(parsed.hasOperators());
        assertEquals(1, parsed.clauses().size());
        assertEquals(ConceptSearchMode.FULL_TEXT, parsed.clauses().get(0).mode());
        assertEquals("obsidienne", parsed.clauses().get(0).value());
    }

    @Test
    void parse_combinesLexicalAndNote() {
        ParsedSearchQuery parsed = SearchQueryParser.parse("obsidienne note:assiette");
        assertTrue(parsed.hasOperators());
        assertTrue(parsed.isCombined());
        assertEquals(ConceptSearchMode.FULL_TEXT, parsed.clauses().get(0).mode());
        assertEquals("obsidienne", parsed.clauses().get(0).value());
        assertEquals(ConceptSearchMode.NOTE, parsed.clauses().get(1).mode());
        assertEquals("assiette", parsed.clauses().get(1).value());
        assertEquals("note", parsed.clauses().get(1).key());
    }

    @Test
    void parse_quotedOperatorValue() {
        ParsedSearchQuery parsed = SearchQueryParser.parse("note:\"assiette plate\"");
        assertEquals(1, parsed.clauses().size());
        assertEquals(ConceptSearchMode.NOTE, parsed.clauses().get(0).mode());
        assertEquals("assiette plate", parsed.clauses().get(0).value());
    }

    @Test
    void parse_quotedRemainder_isExact() {
        ParsedSearchQuery parsed = SearchQueryParser.parse("«pierre noire»");
        assertEquals(ConceptSearchMode.EXACT, parsed.clauses().get(0).mode());
        assertEquals("pierre noire", parsed.clauses().get(0).value());
    }

    @Test
    void parse_prefAndSynAliases() {
        ParsedSearchQuery parsed = SearchQueryParser.parse("pref:obsidienne syn:verre");
        assertEquals(ConceptSearchMode.PREFERRED, parsed.clauses().get(0).mode());
        assertEquals("obsidienne", parsed.clauses().get(0).value());
        assertEquals(ConceptSearchMode.SYNONYM, parsed.clauses().get(1).mode());
        assertEquals("verre", parsed.clauses().get(1).value());
    }

    @Test
    void parse_idAndNotation() {
        ParsedSearchQuery parsed = SearchQueryParser.parse("id:ARK1 notation:12.3");
        assertEquals(ConceptSearchMode.IDENTIFIER, parsed.clauses().get(0).mode());
        assertEquals("ARK1", parsed.clauses().get(0).value());
        assertEquals(ConceptSearchMode.NOTATION, parsed.clauses().get(1).mode());
        assertEquals("12.3", parsed.clauses().get(1).value());
    }

    @Test
    void parse_incompleteOperator_isIgnored() {
        ParsedSearchQuery parsed = SearchQueryParser.parse("note:");
        assertTrue(parsed.hasOperators());
        assertTrue(parsed.isBlank());
    }
}
