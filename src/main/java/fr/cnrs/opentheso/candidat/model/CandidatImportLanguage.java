package fr.cnrs.opentheso.candidat.model;

import java.io.Serializable;

public record CandidatImportLanguage(
        String iso6391,
        String frenchName,
        String englishName
) implements Serializable {
}
