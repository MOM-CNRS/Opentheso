package fr.cnrs.opentheso.v2.shared.io;

import org.eclipse.rdf4j.rio.RDFFormat;

import java.util.Arrays;
import java.util.Objects;

public final class SkosRdfFormatSupport {

    private SkosRdfFormatSupport() {
    }

    public static ResolvedFormat resolveExportFormat(String formatCode) {
        return switch (formatCode == null ? "" : formatCode.toLowerCase()) {
            case "jsonld" -> new ResolvedFormat(RDFFormat.JSONLD, ".json");
            case "turtle" -> new ResolvedFormat(RDFFormat.TURTLE, ".ttl");
            case "json" -> new ResolvedFormat(RDFFormat.RDFJSON, ".json");
            default -> new ResolvedFormat(RDFFormat.RDFXML, ".rdf");
        };
    }

    public static RDFFormat resolveImportFormat(int typeImport) {
        return switch (typeImport) {
            case 1 -> RDFFormat.JSONLD;
            case 2 -> RDFFormat.TURTLE;
            case 3 -> RDFFormat.RDFJSON;
            default -> RDFFormat.RDFXML;
        };
    }

    public record ResolvedFormat(RDFFormat rdfFormat, String extension) {
    }

    public record ExportResult(byte[] content, String filename, String contentType) {
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ExportResult that)) {
                return false;
            }
            return Arrays.equals(content, that.content)
                    && Objects.equals(filename, that.filename)
                    && Objects.equals(contentType, that.contentType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(content), filename, contentType);
        }

        @Override
        public String toString() {
            return "ExportResult[filename=" + filename
                    + ", contentType=" + contentType
                    + ", bytes=" + (content == null ? 0 : content.length) + "]";
        }
    }
}
