package fr.cnrs.opentheso.collection.model;

import java.io.Serializable;

public record CollectionTranslationItem(
        String lang,
        String value
) implements Serializable {
}
