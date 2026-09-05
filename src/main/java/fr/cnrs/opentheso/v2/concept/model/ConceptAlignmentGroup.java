package fr.cnrs.opentheso.v2.concept.model;

import java.util.List;
import java.io.Serializable;

public record ConceptAlignmentGroup(
        String typeKey,
        String typeLabel,
        List<ConceptAlignment> items
) implements Serializable {

    public String getTypeKey() {
        return typeKey;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public List<ConceptAlignment> getItems() {
        return items;
    }

    public String messageKey() {
        if (typeKey == null) {
            return "rightbody.concept.alignment";
        }
        return switch (typeKey.toLowerCase()) {
            case "exactmatch" -> "alignment.exactMatch";
            case "closematch" -> "alignment.closeMatch";
            case "narrowmatch" -> "alignment.narrowMatch";
            case "broadmatch" -> "alignment.broadMatch";
            case "relatedmatch" -> "alignment.relatedMatch";
            default -> "rightbody.concept.alignment";
        };
    }

    public String getMessageKey() {
        return messageKey();
    }
}
