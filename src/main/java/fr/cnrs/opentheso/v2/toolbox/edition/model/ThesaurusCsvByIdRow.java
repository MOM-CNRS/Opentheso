package fr.cnrs.opentheso.v2.toolbox.edition.model;

import java.util.List;
import java.io.Serializable;

public record ThesaurusCsvByIdRow(
        String conceptId,
        String arkId,
        String handleId,
        String prefLabel,
        List<String> altLabels,
        List<String> definitions,
        List<ThesaurusCsvAlignmentRow> alignments) implements Serializable {
}
