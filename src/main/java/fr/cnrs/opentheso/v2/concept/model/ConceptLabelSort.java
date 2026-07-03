package fr.cnrs.opentheso.v2.concept.model;

import java.text.Normalizer;

final class ConceptLabelSort {

    private ConceptLabelSort() {
    }

    static int compareLabels(String left, String right) {
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
        int minSize = Math.min(a.length(), b.length());
        boolean asNumeric = false;
        int lastNumericCompare = 0;
        for (int i = 0; i < minSize; i++) {
            char aChar = a.charAt(i);
            char bChar = b.charAt(i);
            boolean aNumber = aChar >= '0' && aChar <= '9';
            boolean bNumber = bChar >= '0' && bChar <= '9';
            if (asNumeric) {
                if (aNumber && bNumber) {
                    if (lastNumericCompare == 0) {
                        lastNumericCompare = aChar - bChar;
                    }
                } else if (aNumber) {
                    return 1;
                } else if (bNumber) {
                    return -1;
                } else if (lastNumericCompare == 0) {
                    if (aChar != bChar) {
                        return aChar - bChar;
                    }
                    asNumeric = false;
                } else {
                    return lastNumericCompare;
                }
            } else if (aNumber && bNumber) {
                asNumeric = true;
                if (lastNumericCompare == 0) {
                    lastNumericCompare = aChar - bChar;
                }
            } else if (aChar != bChar) {
                return aChar - bChar;
            }
        }
        if (asNumeric) {
            if (a.length() > b.length()
                    && a.charAt(b.length()) >= '0'
                    && a.charAt(b.length()) <= '9') {
                return 1;
            }
            if (b.length() > a.length()
                    && b.charAt(a.length()) >= '0'
                    && b.charAt(a.length()) <= '9') {
                return -1;
            }
            if (lastNumericCompare == 0) {
                return a.length() - b.length();
            }
            return lastNumericCompare;
        }
        return a.length() - b.length();
    }
}
