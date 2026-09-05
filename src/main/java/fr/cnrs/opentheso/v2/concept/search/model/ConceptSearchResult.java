package fr.cnrs.opentheso.v2.concept.search.model;

import java.text.Normalizer;
import java.io.Serializable;
import java.util.List;

public record ConceptSearchResult(
        String thesaurusId,
        String conceptId,
        String preferredLabel,
        String language,
        boolean deprecated,
        List<String> synonyms,
        List<String> broaderTerms,
        List<String> relatedTerms
) implements Comparable<ConceptSearchResult>, Serializable {

    public String getThesaurusId() {
        return thesaurusId;
    }

    public String getConceptId() {
        return conceptId;
    }

    public String getPreferredLabel() {
        return preferredLabel;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public List<String> getBroaderTerms() {
        return broaderTerms;
    }

    public List<String> getRelatedTerms() {
        return relatedTerms;
    }

    @Override
    public int compareTo(ConceptSearchResult other) {
        return naturalCompare(normalize(prefLabel()), normalize(other.prefLabel()));
    }

    private String prefLabel() {
        return preferredLabel == null ? "" : preferredLabel;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    private static int naturalCompare(String left, String right) {
        int min = Math.min(left.length(), right.length());
        for (int i = 0; i < min; i++) {
            char leftChar = left.charAt(i);
            char rightChar = right.charAt(i);
            if (leftChar != rightChar) {
                if (Character.isDigit(leftChar) && Character.isDigit(rightChar)) {
                    return compareNumericSuffix(left, right, i);
                }
                return Character.compare(leftChar, rightChar);
            }
        }
        return Integer.compare(left.length(), right.length());
    }

    private static int compareNumericSuffix(String left, String right, int index) {
        int leftEnd = index;
        while (leftEnd < left.length() && Character.isDigit(left.charAt(leftEnd))) {
            leftEnd++;
        }
        int rightEnd = index;
        while (rightEnd < right.length() && Character.isDigit(right.charAt(rightEnd))) {
            rightEnd++;
        }
        int leftNumber = Integer.parseInt(left.substring(index, leftEnd));
        int rightNumber = Integer.parseInt(right.substring(index, rightEnd));
        int numberCompare = Integer.compare(leftNumber, rightNumber);
        if (numberCompare != 0) {
            return numberCompare;
        }
        return naturalCompare(left.substring(leftEnd), right.substring(rightEnd));
    }
}
