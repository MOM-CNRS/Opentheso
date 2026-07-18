package fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto;

import java.util.List;

public record ThesaurusLanguagesResponse(
        List<String> languages
) {
}
