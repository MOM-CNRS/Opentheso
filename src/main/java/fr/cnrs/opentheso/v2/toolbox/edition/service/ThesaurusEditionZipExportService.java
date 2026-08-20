package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.io.rdf.ThesaurusSkosSerializer;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxExportPersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.rio.Rio;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionZipExportService {

    private final ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    private final ThesaurusCsvWriter thesaurusCsvWriter;
    private final ToolboxExportPersistence toolboxExportPersistence;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    public StreamedContent exportEachGroupAsCsvZip(
            String thesaurusId,
            String thesaurusTitle,
            char delimiter,
            List<String> selectedLanguageCodes,
            boolean clearHtml
    ) throws Exception {
        return exportEachGroupAsCsvZip(
                thesaurusId, thesaurusTitle, delimiter, selectedLanguageCodes, clearHtml, List.of()
        );
    }

    public StreamedContent exportEachGroupAsCsvZip(
            String thesaurusId,
            String thesaurusTitle,
            char delimiter,
            List<String> selectedLanguageCodes,
            boolean clearHtml,
            List<String> restrictGroupIds
    ) throws Exception {
        return exportZip(
                thesaurusId,
                thesaurusTitle,
                ".csv",
                restrictGroupIds,
                (groupId, groupLabel) -> {
                    var document = thesaurusSkosDocumentBuilder.buildDocumentByGroup(thesaurusId, groupId, clearHtml);
                    var languages = resolveLanguages(thesaurusId, selectedLanguageCodes);
                    return thesaurusCsvWriter.writeCsv(document, languages, delimiter);
                }
        );
    }

    public StreamedContent exportEachGroupAsSkosZip(
            String thesaurusId,
            String thesaurusTitle,
            String formatCode,
            boolean clearHtml
    ) throws Exception {
        return exportEachGroupAsSkosZip(thesaurusId, thesaurusTitle, formatCode, clearHtml, List.of());
    }

    public StreamedContent exportEachGroupAsSkosZip(
            String thesaurusId,
            String thesaurusTitle,
            String formatCode,
            boolean clearHtml,
            List<String> restrictGroupIds
    ) throws Exception {
        var resolved = SkosRdfFormatSupport.resolveExportFormat(formatCode);
        return exportZip(
                thesaurusId,
                thesaurusTitle,
                resolved.extension(),
                restrictGroupIds,
                (groupId, groupLabel) -> {
                    var document = thesaurusSkosDocumentBuilder.buildDocumentByGroup(thesaurusId, groupId, clearHtml);
                    try (var output = new ByteArrayOutputStream()) {
                        var serializer = new ThesaurusSkosSerializer(document);
                        Rio.write(serializer.getModel(), output, resolved.rdfFormat());
                        serializer.closeCache();
                        return output.toByteArray();
                    }
                }
        );
    }

    private StreamedContent exportZip(
            String thesaurusId,
            String thesaurusTitle,
            String extension,
            List<String> restrictGroupIds,
            GroupExporter exporter
    ) throws Exception {
        List<NodeGroup> groups = toolboxExportPersistence.loadConceptGroups(thesaurusId);
        if (CollectionUtils.isNotEmpty(restrictGroupIds)) {
            var wanted = restrictGroupIds.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(id -> id.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            groups = groups.stream()
                    .filter(group -> group.getConceptGroup() != null
                            && wanted.contains(StringUtils.defaultString(group.getConceptGroup().getIdGroup())
                            .toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (CollectionUtils.isEmpty(groups)) {
            throw new IllegalStateException("Aucune collection disponible");
        }

        var fileNames = new ArrayList<String>();
        var entries = new ArrayList<byte[]>();
        for (NodeGroup group : groups) {
            String groupId = group.getConceptGroup().getIdGroup();
            byte[] content = exporter.export(groupId, group.getLexicalValue());
            if (content == null || content.length == 0) {
                continue;
            }
            String fileName = buildUniqueFileName(
                    thesaurusTitle + "_" + group.getLexicalValue(),
                    extension,
                    fileNames
            );
            fileNames.add(fileName);
            entries.add(content);
        }

        if (entries.isEmpty()) {
            throw new IllegalStateException("Export ZIP vide");
        }

        byte[] zipBytes = buildZip(fileNames, entries);
        var node = NodeIdValue.builder()
                .id(thesaurusId)
                .value(StringUtils.defaultIfBlank(thesaurusTitle, thesaurusId))
                .build();

        byte[] content = zipBytes;
        return DefaultStreamedContent.builder()
                .contentType("application/zip")
                .name(node.getValue() + "_" + node.getId() + ".zip")
                .stream(() -> new ByteArrayInputStream(content))
                .build();
    }

    private List<NodeLangTheso> resolveLanguages(String thesaurusId, List<String> selectedLanguageCodes) {
        String workLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        List<NodeLangTheso> usedLanguages = toolboxThesaurusPersistence.loadUsedLanguages(thesaurusId, workLang);
        if (CollectionUtils.isEmpty(selectedLanguageCodes)) {
            return usedLanguages;
        }
        return usedLanguages.stream()
                .filter(lang -> selectedLanguageCodes.contains(lang.getCode()))
                .toList();
    }

    private String buildUniqueFileName(String baseName, String extension, List<String> existingNames) {
        String sanitized = StringUtils.defaultIfBlank(baseName, "export").replaceAll("[^a-zA-Z0-9._-]", "_");
        String candidate = sanitized + extension;
        int index = 1;
        while (existingNames.contains(candidate)) {
            candidate = sanitized + "_" + index + extension;
            index++;
        }
        return candidate;
    }

    private byte[] buildZip(List<String> fileNames, List<byte[]> entries) throws IOException {
        try (var zipOut = new ByteArrayOutputStream();
             var zipStream = new ZipOutputStream(zipOut)) {
            for (int i = 0; i < entries.size(); i++) {
                zipStream.putNextEntry(new ZipEntry(fileNames.get(i)));
                zipStream.write(entries.get(i));
                zipStream.closeEntry();
            }
            zipStream.finish();
            return zipOut.toByteArray();
        }
    }

    @FunctionalInterface
    private interface GroupExporter {
        byte[] export(String groupId, String groupLabel) throws Exception;
    }
}
