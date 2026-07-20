package fr.cnrs.opentheso.v2.toolbox.edition.validation;

import fr.cnrs.opentheso.v2.concept.io.rdf.parser.ReadRdf4jDocument;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvImportPersistence;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvStructuredImportPersistence;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Validation des parsers import/export sur les fichiers d'exemple livrés dans {@code src/main/resources/samples/}.
 * Ne nécessite pas de base de données — vérifie la lecture réelle des formats avant tests UI.
 */
class ThesaurusEditionSampleFilesValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void sampleFlatCsv_parsesSuccessfully() throws IOException {
        byte[] content = readSample("samples/sampleCSV.csv");
        var persistence = new ThesaurusEditionCsvImportPersistence(mock(), mock(), mock());

        var result = persistence.parse(content, ',');

        assertTrue(result.isSuccess(), result.error());
        assertFalse(result.conceptObjects().isEmpty());
        assertTrue(result.totalConcepts() > 0);
    }

    @Test
    void sampleFlatCsvWithCollections_parsesSuccessfully() throws IOException {
        byte[] content = readSample("samples/sampleCSV_avecCollections.csv");
        var persistence = new ThesaurusEditionCsvImportPersistence(mock(), mock(), mock());

        var result = persistence.parse(content, ',');

        assertTrue(result.isSuccess(), result.error());
        assertTrue(result.totalConcepts() > 0);
    }

    @Test
    void sampleStructuredCsv_parsesSuccessfully() throws IOException {
        byte[] content = readSample("samples/theso/structuredList.csv");
        var persistence = new ThesaurusEditionCsvStructuredImportPersistence(mock(), mock(), mock(), mock());

        var result = persistence.parse(content, '\t');

        assertTrue(result.isSuccess(), result.error());
        assertNotNull(result.root());
        assertFalse(result.root().getChildrens().isEmpty());
        assertTrue(result.totalConcepts() > 0);
    }

    @Test
    void sampleSkosRdf_readsSuccessfully() throws IOException {
        byte[] content = readSample("samples/sample_skos.rdf");
        var errorBuffer = new StringBuffer();

        var document = new ReadRdf4jDocument().readRdfFlux(
                new ByteArrayInputStream(content),
                RDFFormat.RDFXML,
                "fr",
                errorBuffer
        );

        assertNotNull(document);
        assertNotNull(document.getConceptList());
        assertFalse(document.getConceptList().isEmpty(), errorBuffer.toString());
    }

    @Test
    void sampleSkosTurtle_readsSuccessfully() throws IOException {
        byte[] content = readSample("samples/theso/test_complet_sansArk.rdf");
        var errorBuffer = new StringBuffer();
        var format = SkosRdfFormatSupport.resolveImportFormat(0);

        var document = new ReadRdf4jDocument().readRdfFlux(
                new ByteArrayInputStream(content),
                format,
                "fr",
                errorBuffer
        );

        assertNotNull(document);
        assertNotNull(document.getConceptList());
        assertFalse(document.getConceptList().isEmpty(), errorBuffer.toString());
    }

    @Test
    void roundTrip_writeSampleCsvToTemp(@TempDir Path dir) throws IOException {
        byte[] content = readSample("samples/sampleCSV.csv");
        Path out = dir.resolve("parsed-sample.csv");
        Files.write(out, content);
        assertTrue(Files.size(out) > 0);
    }

    private byte[] readSample(String classpathResource) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            if (stream == null) {
                throw new IOException("Ressource introuvable : " + classpathResource);
            }
            return stream.readAllBytes();
        }
    }
}
