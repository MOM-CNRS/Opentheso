package fr.cnrs.opentheso.concept.search.model;

import java.util.List;

public record ParsedSearchQuery(String raw, List<SearchClause> clauses, boolean hasOperators) {

    public boolean isCombined() {
        return clauses != null && clauses.size() > 1;
    }

    public boolean isBlank() {
        return clauses == null || clauses.isEmpty();
    }
}
