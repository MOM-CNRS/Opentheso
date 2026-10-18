package fr.cnrs.opentheso.concept.io.rdf.parser;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

import java.io.IOException;
import java.io.InputStream;


@Slf4j
public class ReadRdf4jDocument {

    public SKOSXmlDocument readRdfFlux(InputStream inputStream, RDFFormat rdfFormat, String defaultLang, StringBuilder error) throws IOException {

        SKOSXmlDocument skosXmlDocument = new SKOSXmlDocument();
        RDFParser parser = Rio.createParser(rdfFormat);
        // Certains exports SKOS mettent du wikicode ([[File:…]]) dans une IRI.
        // RDF4J interrompt alors tout le fichier. On conserve la valeur telle quelle.
        parser.set(BasicParserSettings.VERIFY_URI_SYNTAX, false);

        if (RDFFormat.RDFJSON.equals(rdfFormat)) {
            parser.setRDFHandler(new ReadJsonFlux(skosXmlDocument, defaultLang));
        } else {
            parser.setRDFHandler(new ReadCommunFlux(skosXmlDocument, defaultLang));
        }

        try {
            parser.parse(inputStream, "");
        } catch (RDFParseException | RDFHandlerException ex) {
            error.append("Erreur dans le contenu du fichier source");
            log.error(ex.getMessage());
            throw new IOException(ex);
        }

        return skosXmlDocument;
    }
}
