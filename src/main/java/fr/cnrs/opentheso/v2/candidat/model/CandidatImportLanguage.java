package fr.cnrs.opentheso.v2.candidat.model;

import java.io.Serializable;

public record CandidatImportLanguage(
        String iso6391,
        String frenchName,
        String englishName
) implements Serializable {
}
