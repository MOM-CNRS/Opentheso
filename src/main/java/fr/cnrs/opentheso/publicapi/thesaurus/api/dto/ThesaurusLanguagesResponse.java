package fr.cnrs.opentheso.publicapi.thesaurus.api.dto;

import java.util.List;

public record ThesaurusLanguagesResponse(
        List<String> languages
) {
}
