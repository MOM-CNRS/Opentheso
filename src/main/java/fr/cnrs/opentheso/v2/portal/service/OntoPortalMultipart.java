package fr.cnrs.opentheso.v2.portal.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

final class OntoPortalMultipart {

    private OntoPortalMultipart() {
    }

    static byte[] body(String boundary, Map<String, String> fields, String filename, byte[] fileBytes) {
        var out = new java.io.ByteArrayOutputStream();
        try {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                write(out, "--" + boundary + "\r\n");
                write(out, "Content-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n");
                write(out, field.getValue() == null ? "" : field.getValue());
                write(out, "\r\n");
            }
            write(out, "--" + boundary + "\r\n");
            write(out, "Content-Disposition: form-data; name=\"ontology\"; filename=\""
                    + filename + "\"\r\n");
            write(out, "Content-Type: application/rdf+xml\r\n\r\n");
            out.write(fileBytes == null ? new byte[0] : fileBytes);
            write(out, "\r\n--" + boundary + "--\r\n");
            return out.toByteArray();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Construction multipart impossible", ex);
        }
    }

    private static void write(java.io.ByteArrayOutputStream out, String text) throws java.io.IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }
}
