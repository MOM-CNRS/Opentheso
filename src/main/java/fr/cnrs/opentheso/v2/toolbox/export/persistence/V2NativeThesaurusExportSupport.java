package fr.cnrs.opentheso.v2.toolbox.export.persistence;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.skos.exports.ThesaurusSkosExportOperations;
import fr.cnrs.opentheso.v2.toolbox.export.session.ThesaurusExportLegacySupport;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.StreamedContent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class V2NativeThesaurusExportSupport implements ThesaurusExportLegacySupport {

    private final ThesaurusSkosExportOperations thesaurusSkosExportOperations;

    @Override
    public StreamedContent exportSkos(String thesaurusId, String thesaurusTitle, String format) throws Exception {
        var node = NodeIdValue.builder()
                .id(thesaurusId)
                .value(StringUtils.defaultIfBlank(thesaurusTitle, thesaurusId))
                .build();
        return thesaurusSkosExportOperations.exportSkosFormat(mapFormat(format), node);
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
