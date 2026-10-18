package fr.cnrs.opentheso.concept.model;

import java.io.Serializable;

public record CorpusSearchContext(
        String conceptId,
        String arkId,
        String preferredLabel
) implements Serializable {
}
