package fr.cnrs.opentheso.v2.collection.model;

import java.io.Serializable;

public record CollectionTranslationItem(
        String lang,
        String value
) implements Serializable {
}
