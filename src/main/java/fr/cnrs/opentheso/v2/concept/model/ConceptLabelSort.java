package fr.cnrs.opentheso.v2.concept.model;

import java.text.Normalizer;

public final class ConceptLabelSort {

    private ConceptLabelSort() {
    }

    public static int compareLabels(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return naturalCompare(normalizedLeft, normalizedRight, true);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    private static int naturalCompare(String left, String right, boolean ignoreCase) {
        String a = ignoreCase ? left.toLowerCase() : left;
        String b = ignoreCase ? right.toLowerCase() : right;
        NaturalCompareState state = new NaturalCompareState();
        int minSize = Math.min(a.length(), b.length());
        for (int i = 0; i < minSize; i++) {
            Integer result = compareAt(a.charAt(i), b.charAt(i), state);
            if (result != null) {
                return result;
            }
        }
        if (state.asNumeric) {
            return finishNumericCompare(a, b, state.lastNumericCompare);
        }
        return a.length() - b.length();
    }

    private static Integer compareAt(char aChar, char bChar, NaturalCompareState state) {
        boolean aNumber = isDigit(aChar);
        boolean bNumber = isDigit(bChar);
        if (state.asNumeric) {
            return compareInNumericMode(aChar, bChar, aNumber, bNumber, state);
        }
        return compareInTextMode(aChar, bChar, aNumber, bNumber, state);
    }

    private static Integer compareInNumericMode(
            char aChar,
            char bChar,
            boolean aNumber,
            boolean bNumber,
            NaturalCompareState state
    ) {
        if (aNumber && bNumber) {
            if (state.lastNumericCompare == 0) {
                state.lastNumericCompare = aChar - bChar;
            }
            return null;
        }
        if (aNumber) {
            return 1;
        }
        if (bNumber) {
            return -1;
        }
        if (state.lastNumericCompare == 0) {
            if (aChar != bChar) {
                return aChar - bChar;
            }
            state.asNumeric = false;
            return null;
        }
        return state.lastNumericCompare;
    }

    private static Integer compareInTextMode(
            char aChar,
            char bChar,
            boolean aNumber,
            boolean bNumber,
            NaturalCompareState state
    ) {
        if (aNumber && bNumber) {
            state.asNumeric = true;
            if (state.lastNumericCompare == 0) {
                state.lastNumericCompare = aChar - bChar;
            }
            return null;
        }
        if (aChar != bChar) {
            return aChar - bChar;
        }
        return null;
    }

    private static int finishNumericCompare(String a, String b, int lastNumericCompare) {
        if (a.length() > b.length() && isDigit(a.charAt(b.length()))) {
            return 1;
        }
        if (b.length() > a.length() && isDigit(b.charAt(a.length()))) {
            return -1;
        }
        if (lastNumericCompare == 0) {
            return a.length() - b.length();
        }
        return lastNumericCompare;
    }

    private static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static final class NaturalCompareState {
        private boolean asNumeric;
        private int lastNumericCompare;
    }
}
