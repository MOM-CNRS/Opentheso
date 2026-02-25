package fr.cnrs.opentheso.bean.importexport.newcsvimport;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class SkosConceptImageDto {
    private String uri; // rdf:about
    private Map<String, String> metadata = new HashMap<>(); // ex: dcterms:title, dcterms:rights

    public void addMeta(String key, String value) {
        if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }
}
