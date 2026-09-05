package fr.cnrs.opentheso.v2.concept.export.service;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportJob;
import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportRequest;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfExportType;
import fr.cnrs.opentheso.v2.toolbox.edition.io.pdf.ThesaurusPdfWriter;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionExportOptions;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusEditionCsvStructuredExportPersistence;
import fr.cnrs.opentheso.v2.toolbox.edition.persistence.ThesaurusSkosDocumentBuilder;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionZipExportService;
import fr.cnrs.opentheso.v2.toolbox.edition.support.CsvDelimiterSupport;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.StreamedContent;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SelectionExportService {

    private static final String FORMAT_CSV_ID = "csv-id";
    private static final String FORMAT_CSV_DEPRECATED = "csv-deprecated";
    private static final String FORMAT_CSV_STRUCTURED = "csv-structured";
    private static final String FORMAT_JSONLD = "jsonld";
    private static final String FORMAT_TURTLE = "turtle";
    private static final String PHASE_LOAD_PREFIX = "Chargement de ";
    private static final String PHASE_LOAD_SUFFIX = " en base…";
    private static final String PHASE_BUILD = "build";
    private static final String PHASE_BUILD_LABEL = "Construire";
    private static final String PHASE_WRITE = "write";
    private static final String PHASE_FILE = "Fichier";


    private final ConceptSkosExportService conceptSkosExportService;
    private final ConceptQueryRepository conceptQueryRepository;
    private final ThesaurusSkosDocumentBuilder thesaurusSkosDocumentBuilder;
    private final ThesaurusCsvWriter thesaurusCsvWriter;
    private final ThesaurusPdfWriter thesaurusPdfWriter;
    private final ThesaurusEditionCsvStructuredExportPersistence csvStructuredPersistence;
    private final ThesaurusEditionZipExportService zipExportService;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

    public void export(SelectionExportRequest request, SelectionExportJob job) {
        try {
            throwIfCancelled(job);
            String thesaurusId = request == null ? null : request.thesaurusId();
            if (StringUtils.isBlank(thesaurusId)) {
                throw new IllegalStateException("Thésaurus manquant");
            }
            String format = normalizeFormat(request.format());
            job.start(1, "Préparation de l'export…");
            throwIfCancelled(job);
            validateOptions(request);

            if (request.exportByGroup() && isSkosOrCsvFull(format)) {
                if (!request.wholeThesaurus()) {
                    throw new IllegalStateException("L’archive par collection s’applique au thésaurus entier");
                }
                exportZipByCollection(request, format, job);
                return;
            }
            if (request.wholeThesaurus() && !isSelectionDocumentFormat(format)) {
                exportWholeSpecial(request, thesaurusId, format, job);
                return;
            }
            if (request.wholeThesaurus()) {
                exportWholeThesaurus(request, thesaurusId, format, job);
                return;
            }
            exportSelection(request, thesaurusId, format, job);
        } catch (ExportCancelledException ex) {
            job.cancel();
        } catch (Exception ex) {
            if (job.isCancelRequested() || Thread.currentThread().isInterrupted()) {
                job.cancel();
                return;
            }
            job.fail(publicError(ex));
        }
    }

    private void exportSelection(SelectionExportRequest request, String thesaurusId, String format, SelectionExportJob job)
            throws Exception {
        int selectedCount = countIds(request.conceptIds());
        job.enterPhase(0, "resolve", "Préparer", request.includeDescendants()
                ? "Recherche des termes spécifiques de " + conceptsLabel(selectedCount) + "…"
                : "Préparation de " + conceptsLabel(selectedCount) + "…");
        List<String> ids = resolveConceptIds(thesaurusId, request.conceptIds(), request.includeDescendants());
        ids = applyGroupFilter(thesaurusId, ids, request);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Aucun concept à exporter");
        }
        int extra = Math.max(0, ids.size() - selectedCount);
        job.progress(1, 1, extra > 0
                ? selectedCount + " sélectionné" + (selectedCount > 1 ? "s" : "")
                + " + " + extra + " descendant" + (extra > 1 ? "s" : "")
                + " → " + conceptsLabel(ids.size())
                : conceptsLabel(ids.size()) + " à exporter");
        throwIfCancelled(job);

        if (FORMAT_CSV_ID.equals(format) || FORMAT_CSV_DEPRECATED.equals(format) || FORMAT_CSV_STRUCTURED.equals(format)) {
            exportSpecialCsv(request, thesaurusId, format, ids, job);
            return;
        }

        job.enterPhase(1, "read", "Lire", PHASE_LOAD_PREFIX + conceptsLabel(ids.size()) + PHASE_LOAD_SUFFIX);
        job.progress(0, ids.size(), PHASE_LOAD_PREFIX + conceptsLabel(ids.size()) + PHASE_LOAD_SUFFIX);
        var document = buildSelectionDocument(thesaurusId, ids, request.clearHtml(), job);
        throwIfCancelled(job);
        writeDocument(request, thesaurusId, format, document, ids.size(), false, job);
    }

    private void exportWholeThesaurus(SelectionExportRequest request, String thesaurusId, String format, SelectionExportJob job)
            throws Exception {
        job.enterPhase(0, "resolve", "Préparer", "Préparation de l'export du thésaurus…");
        job.progress(1, 1, "Thésaurus entier · " + formatLabel(format));
        job.enterPhase(1, "read", "Lire", "Lecture du thésaurus entier en base…");
        throwIfCancelled(job);
        var options = new ThesaurusEditionExportOptions(
                request.filterByGroup(),
                request.groupIds(),
                request.clearHtml()
        );
        var document = thesaurusSkosDocumentBuilder.buildDocument(thesaurusId, options);
        throwIfCancelled(job);
        int conceptCount = document.getConceptList() == null ? 0 : document.getConceptList().size();
        job.enterPhase(2, PHASE_BUILD, PHASE_BUILD_LABEL, "Assemblage SKOS de " + conceptsLabel(conceptCount) + "…");
        job.progress(conceptCount, Math.max(1, conceptCount), "Assemblage SKOS terminé · " + conceptsLabel(conceptCount));
        throwIfCancelled(job);
        writeDocument(request, thesaurusId, format, document, conceptCount, true, job);
    }

    private void exportWholeSpecial(SelectionExportRequest request, String thesaurusId, String format, SelectionExportJob job)
            throws Exception {
        exportSpecialCsv(request, thesaurusId, format, List.of(), job);
    }

    private void exportSpecialCsv(
            SelectionExportRequest request,
            String thesaurusId,
            String format,
            List<String> selectionIds,
            SelectionExportJob job
    ) throws Exception {
        String lang = firstLanguage(request, thesaurusId);
        char delimiter = CsvDelimiterSupport.resolveDelimiter(request.csvDelimiter());
        boolean whole = request.wholeThesaurus() || selectionIds.isEmpty();
        job.enterPhase(1, "read", "Lire", "Lecture des données CSV…");
        job.progress(0, 1, "Lecture des données CSV…");
        throwIfCancelled(job);
        job.enterPhase(2, PHASE_BUILD, PHASE_BUILD_LABEL, "Construction du tableau…");
        byte[] csv;
        String ext = ".csv";
        if (FORMAT_CSV_ID.equals(format)) {
            List<String> groups = request.filterByGroup() ? request.groupIds() : null;
            csv = thesaurusCsvWriter.writeCsvById(thesaurusId, lang, groups, delimiter, whole ? null : selectionIds);
        } else if (FORMAT_CSV_DEPRECATED.equals(format)) {
            csv = thesaurusCsvWriter.writeCsvByDeprecated(thesaurusId, lang, delimiter, whole ? null : selectionIds);
        } else {
            String[][] matrix = csvStructuredPersistence.buildStructuredMatrix(thesaurusId, lang);
            csv = thesaurusCsvWriter.importTreeCsv(matrix, delimiter);
        }
        if (csv == null || csv.length == 0) {
            throw new IllegalStateException("Export CSV vide");
        }
        job.enterPhase(3, PHASE_WRITE, PHASE_FILE, "Écriture CSV…");
        job.complete(
                csv,
                filename(request, thesaurusId, whole, ext),
                "text/csv",
                "Fichier CSV prêt"
        );
    }

    private void exportZipByCollection(SelectionExportRequest request, String format, SelectionExportJob job)
            throws Exception {
        String thesaurusId = request.thesaurusId();
        job.enterPhase(1, "read", "Lire", "Export d'une archive par collection…");
        job.progress(0, 1, "Lecture des collections…");
        StreamedContent streamed;
        List<String> groupIds = request.filterByGroup() ? request.groupIds() : List.of();
        if ("csv".equals(format)) {
            streamed = zipExportService.exportEachGroupAsCsvZip(
                    thesaurusId,
                    request.thesaurusTitle(),
                    CsvDelimiterSupport.resolveDelimiter(request.csvDelimiter()),
                    request.languageCodes(),
                    request.clearHtml(),
                    groupIds
            );
        } else {
            streamed = zipExportService.exportEachGroupAsSkosZip(
                    thesaurusId,
                    request.thesaurusTitle(),
                    format,
                    request.clearHtml(),
                    groupIds
            );
        }
        throwIfCancelled(job);
        job.enterPhase(3, PHASE_WRITE, PHASE_FILE, "Écriture de l'archive ZIP…");
        byte[] zip = readStreamed(streamed);
        job.complete(
                zip,
                filename(request, thesaurusId, true, ".zip"),
                "application/zip",
                "Archive ZIP prête"
        );
    }

    private void writeDocument(
            SelectionExportRequest request,
            String thesaurusId,
            String format,
            SKOSXmlDocument document,
            int conceptCount,
            boolean whole,
            SelectionExportJob job
    ) throws Exception {
        job.enterPhase(3, PHASE_WRITE, PHASE_FILE, "Écriture " + formatLabel(format) + " · " + conceptsLabel(conceptCount) + "…");
        throwIfCancelled(job);
        job.progress(0, 1, "Écriture " + formatLabel(format) + "…");
        if ("csv".equals(format)) {
            completeCsv(request, thesaurusId, document, job, conceptCount, whole);
            return;
        }
        if ("pdf".equals(format)) {
            completePdf(request, thesaurusId, document, job, conceptCount, whole);
            return;
        }
        var resolved = SkosRdfFormatSupport.resolveExportFormat(format);
        var result = conceptSkosExportService.serialize(document, thesaurusId, List.of(), format);
        job.complete(
                result.content(),
                filename(request, thesaurusId, whole, resolved.extension()),
                result.contentType(),
                "Fichier " + formatLabel(format) + " prêt · " + conceptsLabel(conceptCount)
        );
    }

    private SKOSXmlDocument buildSelectionDocument(
            String thesaurusId,
            List<String> ids,
            boolean clearHtml,
            SelectionExportJob job
    ) {
        return conceptSkosExportService.buildDocument(thesaurusId, ids, (done, total) -> {
            throwIfCancelled(job);
            int safeTotal = Math.max(1, total);
            if (done <= 0) {
                job.progress(0, safeTotal, PHASE_LOAD_PREFIX + conceptsLabel(safeTotal) + PHASE_LOAD_SUFFIX);
                return;
            }
            if (job.getPhaseIndex() < 2) {
                job.enterPhase(2, PHASE_BUILD, PHASE_BUILD_LABEL, "Construction SKOS de " + conceptsLabel(safeTotal) + "…");
            }
            String currentId = done <= ids.size() ? ids.get(done - 1) : "";
            job.progress(done, safeTotal, "Concept " + done + " / " + safeTotal
                    + (StringUtils.isBlank(currentId) ? "" : " · " + currentId));
        }, clearHtml);
    }

    private void completeCsv(
            SelectionExportRequest request,
            String thesaurusId,
            SKOSXmlDocument document,
            SelectionExportJob job,
            int conceptCount,
            boolean whole
    ) {
        List<NodeLangTheso> languages = resolveCsvLanguages(thesaurusId, request.languageCodes());
        if (languages.isEmpty()) {
            throw new IllegalStateException("Aucune langue disponible pour l'export CSV");
        }
        char delimiter = CsvDelimiterSupport.resolveDelimiter(request.csvDelimiter());
        byte[] csv = thesaurusCsvWriter.writeCsv(document, languages, delimiter);
        if (csv == null || csv.length == 0) {
            throw new IllegalStateException("Export CSV vide");
        }
        job.complete(
                csv,
                filename(request, thesaurusId, whole, ".csv"),
                "text/csv",
                "Fichier CSV prêt · " + conceptsLabel(conceptCount)
        );
    }

    private void completePdf(
            SelectionExportRequest request,
            String thesaurusId,
            SKOSXmlDocument document,
            SelectionExportJob job,
            int conceptCount,
            boolean whole
    ) throws Exception {
        String lang1 = StringUtils.defaultIfBlank(request.language1(), firstLanguage(request, thesaurusId));
        if (StringUtils.isBlank(lang1)) {
            throw new IllegalStateException("Langue principale manquante");
        }
        boolean hierarchical = !"alphabetical".equalsIgnoreCase(request.pdfType())
                && !"alphabétique".equalsIgnoreCase(request.pdfType());
        ThesaurusPdfExportType type = hierarchical
                ? ThesaurusPdfExportType.HIERARCHIQUE
                : ThesaurusPdfExportType.ALPHABETIQUE;
        byte[] pdf = thesaurusPdfWriter.createPdfFile(
                document,
                lang1,
                StringUtils.defaultString(request.language2()),
                type,
                request.includeImages()
        );
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Export PDF vide");
        }
        job.complete(
                pdf,
                filename(request, thesaurusId, whole, ".pdf"),
                "application/pdf",
                "Fichier PDF prêt · " + conceptsLabel(conceptCount)
        );
    }

    List<String> resolveConceptIds(String thesaurusId, List<String> selectedIds, boolean includeDescendants) {
        Set<String> ids = new LinkedHashSet<>();
        if (selectedIds != null) {
            selectedIds.stream().filter(StringUtils::isNotBlank).forEach(ids::add);
        }
        if (includeDescendants && !ids.isEmpty()) {
            ids.addAll(conceptQueryRepository.findDescendantConceptIds(thesaurusId, ids));
        }
        return new ArrayList<>(ids);
    }

    private static void validateOptions(SelectionExportRequest request) {
        if (request.filterByGroup() && request.groupIds().isEmpty()) {
            throw new IllegalStateException("Choisissez au moins une collection");
        }
    }

    private List<String> applyGroupFilter(String thesaurusId, List<String> ids, SelectionExportRequest request) {
        if (!request.filterByGroup()) {
            return ids;
        }
        List<String> inGroups = conceptQueryRepository.findConceptIdsInGroups(thesaurusId, request.groupIds());
        if (inGroups.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = new LinkedHashSet<>(inGroups);
        return ids.stream().filter(allowed::contains).toList();
    }

    private List<NodeLangTheso> resolveCsvLanguages(String thesaurusId, List<String> selectedCodes) {
        String workLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        List<NodeLangTheso> used = toolboxThesaurusPersistence.loadUsedLanguages(thesaurusId, workLang);
        if (used == null || used.isEmpty()) {
            return List.of();
        }
        if (selectedCodes == null || selectedCodes.isEmpty()) {
            return used;
        }
        Set<String> wanted = new LinkedHashSet<>();
        selectedCodes.stream().filter(StringUtils::isNotBlank).map(code -> code.toLowerCase(Locale.ROOT)).forEach(wanted::add);
        List<NodeLangTheso> filtered = used.stream()
                .filter(lang -> lang.getCode() != null && wanted.contains(lang.getCode().toLowerCase(Locale.ROOT)))
                .toList();
        return filtered.isEmpty() ? used : filtered;
    }

    private String firstLanguage(SelectionExportRequest request, String thesaurusId) {
        if (StringUtils.isNotBlank(request.language1())) {
            return request.language1();
        }
        if (!request.languageCodes().isEmpty()) {
            return request.languageCodes().get(0);
        }
        String workLang = toolboxPreferencePersistence.getWorkLanguage(thesaurusId);
        var langs = toolboxThesaurusPersistence.loadUsedLanguages(thesaurusId, workLang);
        if (langs != null && !langs.isEmpty() && langs.get(0).getCode() != null) {
            return langs.get(0).getCode();
        }
        return workLang;
    }

    private static String filename(SelectionExportRequest request, String thesaurusId, boolean whole, String extension) {
        return SelectionExportFileNames.build(request.thesaurusTitle(), thesaurusId, whole, extension);
    }

    private static byte[] readStreamed(StreamedContent streamed) throws Exception {
        if (streamed == null || streamed.getStream() == null) {
            throw new IllegalStateException("Export ZIP vide");
        }
        try (InputStream input = streamed.getStream().get();
             var output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static void throwIfCancelled(SelectionExportJob job) {
        if (job.isCancelRequested() || Thread.currentThread().isInterrupted()) {
            throw new ExportCancelledException();
        }
    }

    static int countIds(List<String> ids) {
        if (ids == null) {
            return 0;
        }
        return (int) ids.stream().filter(StringUtils::isNotBlank).distinct().count();
    }

    static String conceptsLabel(int count) {
        return count + " concept" + (count > 1 ? "s" : "");
    }

    static String normalizeFormat(String formatCode) {
        String code = formatCode == null ? "" : formatCode.trim().toLowerCase(Locale.ROOT);
        return switch (code) {
            case FORMAT_JSONLD, "json-ld" -> FORMAT_JSONLD;
            case "json" -> "json";
            case FORMAT_TURTLE, "ttl" -> FORMAT_TURTLE;
            case "csv", "csv-full" -> "csv";
            case FORMAT_CSV_ID, "csv_id", "csvid" -> FORMAT_CSV_ID;
            case FORMAT_CSV_STRUCTURED, "csv_struc", "csv-struc" -> FORMAT_CSV_STRUCTURED;
            case FORMAT_CSV_DEPRECATED, "deprecated" -> FORMAT_CSV_DEPRECATED;
            case "pdf" -> "pdf";
            default -> "rdf";
        };
    }

    static String formatLabel(String format) {
        return switch (format == null ? "" : format) {
            case FORMAT_JSONLD -> "JSON-LD";
            case "json" -> "JSON";
            case FORMAT_TURTLE -> "Turtle";
            case "csv" -> "CSV";
            case FORMAT_CSV_ID -> "CSV réduit";
            case FORMAT_CSV_STRUCTURED -> "CSV structuré";
            case FORMAT_CSV_DEPRECATED -> "CSV dépréciés";
            case "pdf" -> "PDF";
            default -> "RDF/XML";
        };
    }

    static String publicError(Throwable ex) {
        String message = ex == null ? null : ex.getMessage();
        if (StringUtils.isBlank(message)) {
            return "Export impossible";
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("thésaurus")
                || lower.contains("concept")
                || lower.contains("langue")
                || lower.contains("collection")
                || lower.contains("préférence")
                || lower.contains("uri")
                || lower.contains("csv")
                || lower.contains("pdf")
                || lower.contains("vide")
                || lower.contains("zip")) {
            return message;
        }
        return "Export impossible";
    }

    private static boolean isSkosOrCsvFull(String format) {
        return "csv".equals(format) || "rdf".equals(format) || FORMAT_JSONLD.equals(format)
                || "json".equals(format) || FORMAT_TURTLE.equals(format);
    }

    private static boolean isSelectionDocumentFormat(String format) {
        return isSkosOrCsvFull(format) || "pdf".equals(format);
    }

    static final class ExportCancelledException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
