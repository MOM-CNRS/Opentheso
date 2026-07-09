package fr.cnrs.opentheso.v2.toolbox.edition.support;

public final class CsvDelimiterSupport {

    private CsvDelimiterSupport() {
    }

    public static char resolveDelimiter(int choice) {
        return switch (choice) {
            case 1 -> ';';
            case 2 -> '\t';
            default -> ',';
        };
    }

    public static char resolveDelimiter(String value) {
        if (value == null || value.isEmpty()) {
            return ',';
        }
        return switch (value) {
            case ";" -> ';';
            case "\\t", "\t" -> '\t';
            default -> ',';
        };
    }
}
