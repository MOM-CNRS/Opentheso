package fr.cnrs.opentheso.v2.portal.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OntoPortalMultipartTest {

    @Test
    void body_includesFieldsAndOntologyFile() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("hasOntologyLanguage", "SKOS");
        fields.put("URI", "http://mondomaine.fr/?idt=TH1");
        fields.put("contact", "[{\"name\":\"Alice\"}]");

        String body = new String(OntoPortalMultipart.body(
                "bound",
                fields,
                "TH1.rdf",
                "<rdf/>".getBytes(StandardCharsets.UTF_8)
        ), StandardCharsets.UTF_8);

        assertTrue(body.contains("name=\"hasOntologyLanguage\""));
        assertTrue(body.contains("name=\"URI\""));
        assertTrue(body.contains("SKOS"));
        assertTrue(body.contains("name=\"ontology\"; filename=\"TH1.rdf\""));
        assertTrue(body.contains("<rdf/>"));
        assertTrue(body.contains("--bound--"));
    }
}
