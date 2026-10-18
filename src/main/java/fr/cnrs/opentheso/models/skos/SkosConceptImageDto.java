package fr.cnrs.opentheso.models.skos;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SkosConceptImageDto {
    private String uri;
    private Map<String, String> metadata = new HashMap<>();

    public void addMeta(String key, String value) {
        if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }
}
