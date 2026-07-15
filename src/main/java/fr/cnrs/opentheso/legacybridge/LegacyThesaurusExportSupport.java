package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.services.exports.ExportService;
import fr.cnrs.opentheso.v2.toolbox.export.session.ThesaurusExportLegacySupport;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class LegacyThesaurusExportSupport implements ThesaurusExportLegacySupport {

    private final ExportService exportService;

    @Override
    public StreamedContent exportSkos(String thesaurusId, String thesaurusTitle, String format) throws Exception {
        var node = NodeIdValue.builder()
                .id(thesaurusId)
                .value(StringUtils.defaultIfBlank(thesaurusTitle, thesaurusId))
                .build();
        return exportService.exportSkosFormat(
                mapFormat(format),
                node,
                false,
                false,
                Collections.emptyList()
        );
    }

    private String mapFormat(String format) {
        return switch (StringUtils.defaultString(format).toLowerCase()) {
            case "jsonld", "json-ld" -> "jsonld";
            case "turtle", "ttl" -> "turtle";
            case "json", "rdfjson" -> "json";
            default -> "rdf";
        };
    }
}
