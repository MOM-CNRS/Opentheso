package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;

public record CorpusSearchContext(
        String conceptId,
        String arkId,
        String preferredLabel
) implements Serializable {
}
