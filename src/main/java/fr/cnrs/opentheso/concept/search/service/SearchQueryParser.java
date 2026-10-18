package fr.cnrs.opentheso.concept.search.service;

import fr.cnrs.opentheso.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.concept.search.model.ParsedSearchQuery;
import fr.cnrs.opentheso.concept.search.model.SearchClause;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Codes de recherche combinables ({@code obsidienne note:assiette}), style Gmail.
 */
public final class SearchQueryParser {

    private static final Map<String, ConceptSearchMode> KEYS = new LinkedHashMap<>();
    private static final Pattern OPERATOR = Pattern.compile(
            "(?i)(?:^|\\s)(note|notes|id|ark|identifiant|notation|nota|pref|label|libelle|syn|alt|synonyme)"
                    + "\\s*:\\s*(?:\"([^\"]*)\"|«([^»]*)»|(\\S+))?"
    );

    static {
        KEYS.put("note", ConceptSearchMode.NOTE);
        KEYS.put("notes", ConceptSearchMode.NOTE);
        KEYS.put("id", ConceptSearchMode.IDENTIFIER);
        KEYS.put("ark", ConceptSearchMode.IDENTIFIER);
        KEYS.put("identifiant", ConceptSearchMode.IDENTIFIER);
        KEYS.put("notation", ConceptSearchMode.NOTATION);
        KEYS.put("nota", ConceptSearchMode.NOTATION);
        KEYS.put("pref", ConceptSearchMode.PREFERRED);
        KEYS.put("label", ConceptSearchMode.PREFERRED);
        KEYS.put("libelle", ConceptSearchMode.PREFERRED);
        KEYS.put("syn", ConceptSearchMode.SYNONYM);
        KEYS.put("alt", ConceptSearchMode.SYNONYM);
        KEYS.put("synonyme", ConceptSearchMode.SYNONYM);
    }

    private SearchQueryParser() {
    }

    public static ParsedSearchQuery parse(String raw) {
        String source = StringUtils.defaultString(raw);
        if (source.isBlank()) {
            return new ParsedSearchQuery("", List.of(), false);
        }
        Matcher matcher = OPERATOR.matcher(source);
        List<SearchClause> operators = new ArrayList<>();
        StringBuilder rest = new StringBuilder();
        int cursor = 0;
        boolean found = false;
        while (matcher.find()) {
            found = true;
            rest.append(source, cursor, matcher.start());
            cursor = matcher.end();
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            ConceptSearchMode mode = KEYS.get(key);
            if (mode == null) {
                rest.append(matcher.group());
                continue;
            }
            String value = firstNonBlank(matcher.group(2), matcher.group(3), matcher.group(4));
            if (StringUtils.isNotBlank(value)) {
                operators.add(new SearchClause(mode, value.trim(), canonicalKey(mode)));
            }
        }
        rest.append(source.substring(cursor));
        List<SearchClause> clauses = new ArrayList<>();
        String remainder = rest.toString().replaceAll("\\s+", " ").trim();
        if (StringUtils.isNotBlank(remainder)) {
            String quoted = unwrapQuotes(remainder);
            if (quoted != null) {
                clauses.add(new SearchClause(ConceptSearchMode.EXACT, quoted, ""));
            } else {
                clauses.add(new SearchClause(ConceptSearchMode.FULL_TEXT, remainder, ""));
            }
        }
        clauses.addAll(operators);
        return new ParsedSearchQuery(source.trim(), List.copyOf(clauses), found);
    }

    public static String canonicalKey(ConceptSearchMode mode) {
        return switch (mode) {
            case NOTE -> "note";
            case IDENTIFIER -> "id";
            case NOTATION -> "notation";
            case PREFERRED -> "pref";
            case SYNONYM -> "syn";
            default -> "";
        };
    }

    private static String unwrapQuotes(String text) {
        if (text == null || text.length() < 2) {
            return null;
        }
        char start = text.charAt(0);
        char end = text.charAt(text.length() - 1);
        boolean quoted = (start == '"' && end == '"')
                || (start == '«' && end == '»')
                || (start == '“' && end == '”');
        if (!quoted) {
            return null;
        }
        String inner = text.substring(1, text.length() - 1).trim();
        return inner.isEmpty() ? null : inner;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }
}
