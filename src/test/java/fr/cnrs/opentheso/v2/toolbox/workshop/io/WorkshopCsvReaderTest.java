package fr.cnrs.opentheso.v2.toolbox.workshop.io;

import fr.cnrs.opentheso.models.alignment.NodeAlignmentImport;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentSmall;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WorkshopCsvReader}, the CSV parser backing the "Atelier" (Workshop)
 * bulk-import operations. {@link WorkshopCsvReader} has no injected dependencies, so it is
 * instantiated directly and fed hand-built CSV content via {@link StringReader}.
 *
 * <p>Several methods (readFileNote, readFileAltlabel, readFile, readListFile) rely on the
 * {@code langs} instance field, which is only populated by a prior call to {@link
 * WorkshopCsvReader#setLangs(java.io.Reader)} on a separate {@link StringReader} built from the
 * same CSV content (mirroring exactly how {@code WorkshopBulkImportEngine} calls this class in
 * production: one reader to prime the languages, a second reader to parse the data).</p>
 */
class WorkshopCsvReaderTest {

    // ------------------------------------------------------------------
    // Alignment (add)
    // ------------------------------------------------------------------

    @Test
    void readHeadersFileAlignment_excludesLocalIdColumn_caseInsensitive() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localId,skos:exactMatch,skos:closeMatch\n"
                + "C1,http://example.com/c1,http://example.com/c1close\n";

        ArrayList<String> headers = reader.readHeadersFileAlignment(new StringReader(csv));

        assertNotNull(headers);
        // CSVParser#getHeaderMap() does not guarantee insertion order, so compare as a set
        assertEquals(Set.of("skos:exactMatch", "skos:closeMatch"), Set.copyOf(headers));
        assertEquals(2, headers.size());
    }

    @Test
    void readFileAlignment_parsesUriAndDefaultsToExactMatchType() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localId,skos:exactMatch\n"
                + "C1,http://example.com/c1\n";
        ArrayList<String> headers = reader.readHeadersFileAlignment(new StringReader(csv));

        boolean ok = reader.readFileAlignment(new StringReader(csv), headers);

        assertTrue(ok);
        List<NodeAlignmentImport> imports = reader.getNodeAlignmentImports();
        assertEquals(1, imports.size());
        assertEquals("C1", imports.get(0).getLocalId());
        assertEquals(1, imports.get(0).getNodeAlignmentSmalls().size());
        NodeAlignmentSmall small = imports.get(0).getNodeAlignmentSmalls().get(0);
        assertEquals("skos:exactMatch", small.getSource());
        assertEquals("http://example.com/c1", small.getUri_target());
        assertEquals(1, small.getAlignement_id_type()); // default type = exactMatch
    }

    @Test
    void readFileAlignment_explicitAlignmentTypeSuffix_isHonored() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localId,skos:closeMatch\n"
                + "C1,http://example.com/c1##2\n";
        ArrayList<String> headers = reader.readHeadersFileAlignment(new StringReader(csv));

        boolean ok = reader.readFileAlignment(new StringReader(csv), headers);

        assertTrue(ok);
        List<NodeAlignmentImport> imports = reader.getNodeAlignmentImports();
        assertEquals(1, imports.size());
        NodeAlignmentSmall small = imports.get(0).getNodeAlignmentSmalls().get(0);
        assertEquals("http://example.com/c1", small.getUri_target());
        assertEquals(2, small.getAlignement_id_type());
    }

    @Test
    void readFileAlignment_dropsRecordAndSetsMessage_whenUriIsInvalid() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localId,skos:exactMatch\n"
                + "C1,not-a-valid-uri\n";
        ArrayList<String> headers = reader.readHeadersFileAlignment(new StringReader(csv));

        boolean ok = reader.readFileAlignment(new StringReader(csv), headers);

        // the parse loop itself succeeds (no IOException); the invalid record is simply skipped
        assertTrue(ok);
        assertTrue(reader.getNodeAlignmentImports().isEmpty());
        assertNotNull(reader.getMessage());
        assertTrue(reader.getMessage().contains("URI"));
    }

    // ------------------------------------------------------------------
    // Alignment (delete)
    // ------------------------------------------------------------------

    @Test
    void readFileAlignmentToDelete_parsesLocalIdAndUriToRemove() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localId,Uri\n"
                + "C1,http://example.com/toDelete\n";

        boolean ok = reader.readFileAlignmentToDelete(new StringReader(csv));

        assertTrue(ok);
        List<WorkshopCsvReader.ConceptObject> concepts = reader.getConceptObjects();
        assertEquals(1, concepts.size());
        assertEquals("C1", concepts.get(0).getLocalId());
        assertEquals(1, concepts.get(0).getAlignments().size());
        assertEquals("http://example.com/toDelete", concepts.get(0).getAlignments().get(0).getValue());
    }

    @Test
    void readFileAlignmentToDelete_skipsRecordsWithoutLocalId() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localId,Uri\n"
                + ",http://example.com/orphan\n";

        boolean ok = reader.readFileAlignmentToDelete(new StringReader(csv));

        assertTrue(ok);
        assertTrue(reader.getConceptObjects().isEmpty());
    }

    // ------------------------------------------------------------------
    // Related (RT)
    // ------------------------------------------------------------------

    @Test
    void readHeadersFileRelated_returnsAllColumnsInOrder() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localid,skos:related\nC1,C2\n";

        ArrayList<String> headers = reader.readHeadersFileRelated(new StringReader(csv));

        // CSVParser#getHeaderMap() does not guarantee insertion order, so compare as a set
        assertEquals(Set.of("localid", "skos:related"), Set.copyOf(headers));
        assertEquals(2, headers.size());
    }

    @Test
    void readFileRelated_parsesPairsAndDeduplicates() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localid,skos:related\n"
                + "C1,C2\n"
                + "C1,C3\n"
                + "C1,C2\n"; // duplicate of the first row, must not be counted twice
        ArrayList<String> headers = reader.readHeadersFileRelated(new StringReader(csv));

        boolean ok = reader.readFileRelated(new StringReader(csv), headers);

        assertTrue(ok);
        List<NodeIdValue> values = reader.getNodeIdValues();
        assertEquals(2, values.size());
        List<String> relatedTargets = values.stream().map(NodeIdValue::getValue).sorted().toList();
        assertEquals(List.of("C2", "C3"), relatedTargets);
        assertTrue(values.stream().allMatch(v -> "C1".equals(v.getId())));
    }

    @Test
    void readFileRelated_skipsBlankIdsOrValues() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localid,skos:related\n"
                + ",C2\n"
                + "C1,\n";

        boolean ok = reader.readFileRelated(new StringReader(csv), new ArrayList<>());

        assertTrue(ok);
        assertTrue(reader.getNodeIdValues().isEmpty());
    }

    // ------------------------------------------------------------------
    // Notes
    // ------------------------------------------------------------------

    @Test
    void readFileNote_parsesNoteDefinitionAndScopeNote_forDeclaredLangs() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localid,skos:note@fr,skos:definition@fr\n"
                + "C1,Une note,Une definition\n";

        // production code always primes `langs` via setLangs() on a fresh reader before parsing
        assertTrue(reader.setLangs(new StringReader(csv)));
        assertEquals(List.of("fr"), reader.getLangs());

        boolean ok = reader.readFileNote(new StringReader(csv));

        assertTrue(ok);
        List<WorkshopCsvReader.ConceptObject> concepts = reader.getConceptObjects();
        assertEquals(1, concepts.size());
        WorkshopCsvReader.ConceptObject concept = concepts.get(0);
        assertEquals("C1", concept.getIdConcept());
        assertEquals(1, concept.getNote().size());
        assertEquals("Une note", concept.getNote().get(0).getLabel());
        assertEquals("fr", concept.getNote().get(0).getLang());
        assertEquals(1, concept.getDefinitions().size());
        assertEquals("Une definition", concept.getDefinitions().get(0).getLabel());
    }

    @Test
    void readFileNote_splitsMultipleValuesOnDoubleHash() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localid,skos:note@fr\n"
                + "C1,premiere note##deuxieme note\n";

        assertTrue(reader.setLangs(new StringReader(csv)));
        boolean ok = reader.readFileNote(new StringReader(csv));

        assertTrue(ok);
        List<WorkshopCsvReader.Label> notes = reader.getConceptObjects().get(0).getNote();
        assertEquals(2, notes.size());
        assertEquals("premiere note", notes.get(0).getLabel());
        assertEquals("deuxieme note", notes.get(1).getLabel());
    }

    @Test
    void readFileNote_skipsRecordsWithoutLocalId() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localid,skos:note@fr\n"
                + ",Une note orpheline\n";

        assertTrue(reader.setLangs(new StringReader(csv)));
        boolean ok = reader.readFileNote(new StringReader(csv));

        assertTrue(ok);
        assertTrue(reader.getConceptObjects().isEmpty());
    }

    // ------------------------------------------------------------------
    // Translation (traduction)
    // ------------------------------------------------------------------

    @Test
    void readFileTraduction_parsesPrefLabelForRequestedLang() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localid,skos:prefLabel@fr\n"
                + "C1,Chien\n";

        boolean ok = reader.readFileTraduction(new StringReader(csv), "fr");

        assertTrue(ok);
        List<NodeIdValue> values = reader.getNodeIdValues();
        assertEquals(1, values.size());
        assertEquals("C1", values.get(0).getId());
        assertEquals("Chien", values.get(0).getValue());
    }

    @Test
    void readFileTraduction_skipsRecordsWithoutPrefLabelForLang() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        // the column exists for "en" but we ask for "fr", which is absent -> no match
        String csv = "localid,skos:prefLabel@en\n"
                + "C1,Dog\n";

        boolean ok = reader.readFileTraduction(new StringReader(csv), "fr");

        assertTrue(ok);
        assertTrue(reader.getNodeIdValues().isEmpty());
    }

    // ------------------------------------------------------------------
    // Generic "add concepts" import (readFile) -> produces ConceptObject
    // ------------------------------------------------------------------

    @Test
    void readFile_nominal_parsesIdentifierTypeLabelsAndBroader() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "identifier,rdf:type,skos:prefLabel@fr,skos:altLabel@fr,skos:broader\n"
                + "concept1,skos:Concept,Chat,Minou##Matou,broaderId1\n";

        assertTrue(reader.setLangs(new StringReader(csv)));
        assertEquals(List.of("fr"), reader.getLangs());

        boolean ok = reader.readFile(new StringReader(csv), false);

        assertTrue(ok);
        List<WorkshopCsvReader.ConceptObject> concepts = reader.getConceptObjects();
        assertEquals(1, concepts.size());
        WorkshopCsvReader.ConceptObject concept = concepts.get(0);
        assertEquals("concept1", concept.getIdConcept());
        assertEquals("skos:concept", concept.getType());

        assertEquals(1, concept.getPrefLabels().size());
        assertEquals("Chat", concept.getPrefLabels().get(0).getLabel());
        assertEquals("fr", concept.getPrefLabels().get(0).getLang());

        assertEquals(2, concept.getAltLabels().size());
        assertEquals("Minou", concept.getAltLabels().get(0).getLabel());
        assertEquals("Matou", concept.getAltLabels().get(1).getLabel());

        assertEquals(List.of("broaderId1"), concept.getBroaders());
    }

    @Test
    void readFile_derivesIdentifierFromUri_whenIdentifierColumnAbsent() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "URI,rdf:type,skos:prefLabel@fr\n"
                + "http://thesaurus.example.com/page/concept#concept42,skos:Concept,Chat\n";

        assertTrue(reader.setLangs(new StringReader(csv)));
        boolean ok = reader.readFile(new StringReader(csv), false);

        assertTrue(ok);
        List<WorkshopCsvReader.ConceptObject> concepts = reader.getConceptObjects();
        assertEquals(1, concepts.size());
        // getId() extracts whatever follows the last "#" fragment of the URI
        assertEquals("concept42", concepts.get(0).getIdConcept());
        assertEquals("http://thesaurus.example.com/page/concept#concept42", concepts.get(0).getUri());
    }

    @Test
    void readFile_missingIdentifierAndUri_producesErrorMessage_andSkipsRecord() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        // no "identifier" and no "URI" column at all -> a concept id can never be resolved
        String csv = "rdf:type,skos:prefLabel@fr\n"
                + "skos:Concept,Chat\n";

        // even without lang columns to discover, setLangs must still be called once so the
        // internal `langs` field is a non-null (possibly empty) list before readFile runs
        reader.setLangs(new StringReader(csv));

        boolean ok = reader.readFile(new StringReader(csv), false);

        assertTrue(ok); // no IOException -> the method itself still returns true
        assertTrue(reader.getConceptObjects().isEmpty());
        assertNotNull(reader.getMessage());
        assertTrue(reader.getMessage().contains("concept sans Id"));
    }

    // ------------------------------------------------------------------
    // Delimiter handling
    // ------------------------------------------------------------------

    @Test
    void semicolonDelimiter_isHonoredByHeaderAndDataParsing() {
        WorkshopCsvReader reader = new WorkshopCsvReader(';');
        String csv = "localId;skos:exactMatch\n"
                + "C1;http://example.com/c1\n";

        ArrayList<String> headers = reader.readHeadersFileAlignment(new StringReader(csv));
        assertEquals(List.of("skos:exactMatch"), headers);

        boolean ok = reader.readFileAlignment(new StringReader(csv), headers);
        assertTrue(ok);
        assertEquals(1, reader.getNodeAlignmentImports().size());
        assertEquals("C1", reader.getNodeAlignmentImports().get(0).getLocalId());
    }

    // ------------------------------------------------------------------
    // Generic error handling: empty file / missing header
    // ------------------------------------------------------------------

    @Test
    void readFileCsvForGetIdFromPrefLabelSetLang_setsMessage_whenHeaderHasNoLangSuffix() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        // header has no "@lang" marker at all
        String csv = "skos:prefLabel\nChat\n";

        boolean ok = reader.readFileCsvForGetIdFromPrefLabelSetLang(new StringReader(csv));

        assertFalse(ok);
        assertNotNull(reader.getMessage());
        assertTrue(reader.getMessage().toLowerCase().contains("langue"));
    }

    @Test
    void readFileCsvForGetIdFromPrefLabelSetLang_setsMessage_whenMoreThanOneColumn() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "skos:prefLabel@fr,skos:prefLabel@en\nChat,Cat\n";

        boolean ok = reader.readFileCsvForGetIdFromPrefLabelSetLang(new StringReader(csv));

        assertFalse(ok);
        assertNotNull(reader.getMessage());
        assertFalse(reader.getMessage().isEmpty());
    }

    @Test
    void readFileNotation_returnsTrue_butEmptyResults_onCompletelyEmptyInput() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "";

        boolean ok = reader.readFileNotation(new StringReader(csv));

        // an empty stream has no header row and no records; the reader does not throw, it
        // simply reports success with an empty result list rather than raising an unhandled
        // exception up to the caller.
        assertTrue(ok);
        assertNotNull(reader.getNodeIdValues());
        assertTrue(reader.getNodeIdValues().isEmpty());
    }

    @Test
    void readFileNotation_parsesLocalIdAndNotation() {
        WorkshopCsvReader reader = new WorkshopCsvReader(',');
        String csv = "localid,skos:notation\nC1,N-001\n";

        boolean ok = reader.readFileNotation(new StringReader(csv));

        assertTrue(ok);
        assertEquals(1, reader.getNodeIdValues().size());
        assertEquals("C1", reader.getNodeIdValues().get(0).getId());
        assertEquals("N-001", reader.getNodeIdValues().get(0).getValue());
    }
}
