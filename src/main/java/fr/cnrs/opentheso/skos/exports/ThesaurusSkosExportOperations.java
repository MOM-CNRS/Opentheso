package fr.cnrs.opentheso.skos.exports;

import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.services.exports.ExportService;
import lombok.RequiredArgsConstructor;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ThesaurusSkosExportOperations {

    private final ExportService exportService;

    public StreamedContent exportSkosFormat(String format, NodeIdValue thesaurusNode) throws Exception {
        return exportService.exportSkosFormat(format, thesaurusNode, false, false, Collections.emptyList());
    }
}
