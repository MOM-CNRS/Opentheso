package fr.cnrs.opentheso.collection.model;

import java.io.Serializable;

public record CollectionNoteItem(
        String typeCode,
        String typeLabel,
        String lang,
        String value
) implements Serializable {
}
