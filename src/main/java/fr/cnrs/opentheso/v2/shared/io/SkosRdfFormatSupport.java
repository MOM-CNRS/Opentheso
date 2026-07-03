package fr.cnrs.opentheso.v2.shared.io;

import org.eclipse.rdf4j.rio.RDFFormat;

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
    }
}
