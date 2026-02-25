package fr.cnrs.opentheso.services.imports.rdf4j;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.InputStream;


@Slf4j
public class ReadRDF4JNewGen {

    public SKOSXmlDocument readRdfFlux(InputStream inputStream, RDFFormat rdfFormat, String defaultLang, StringBuffer error) throws IOException {

        SKOSXmlDocument skosXmlDocument = new SKOSXmlDocument();
        RDFParser parser = Rio.createParser(rdfFormat);

        if (RDFFormat.RDFJSON.equals(rdfFormat)) {
            parser.setRDFHandler(new ReadJsonFlux(skosXmlDocument, defaultLang));
        } else {
            parser.setRDFHandler(new ReadCommunFlux(skosXmlDocument, defaultLang));
        }

        try {
            parser.parse(inputStream, "");
        } catch(Exception ex) {
            error.append("Erreur dans le contenu du fichier source");
            log.error(ex.getMessage());
            throw new RuntimeException(ex);
        }

        return skosXmlDocument;
    }
}
