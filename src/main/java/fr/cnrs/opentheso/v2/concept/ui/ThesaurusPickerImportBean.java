package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusPickerImportProgressContext;
import fr.cnrs.opentheso.v2.concept.service.ThesaurusPickerImportProgressTracker;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvImportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvStructuredImportService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionSkosImportService;
import fr.cnrs.opentheso.v2.toolbox.edition.support.CsvDelimiterSupport;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Flux d'import de thésaurus depuis le picker V2 (SKOS / CSV / CSV structuré).
 * Réutilise les services toolbox edition existants, sans PrimeFaces.
 * Le fichier uploadé est stocké sur disque (temp) pour éviter de retenir un gros {@code byte[]} en ViewScoped.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusPickerImportBean")
@RequiredArgsConstructor
public class ThesaurusPickerImportBean implements Serializable {

    public static final String FORMAT_SKOS = "skos";
    public static final String FORMAT_CSV = "csv";
    public static final String FORMAT_CSV_STRUCTURED = "csvStructured";

    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;
    private static final String UPLOAD_TEMP_DIR = "opentheso-import";
    private static final String UPLOAD_TEMP_PREFIX = "picker-";

    private final transient UserSession userSession;
    private final transient V2LocaleBean v2LocaleBean;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;
    private final transient NewThesaurusService newThesaurusService;
    private final transient ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final transient ThesaurusEditionSkosImportService skosImportService;
    private final transient ThesaurusEditionCsvImportService csvImportService;
    private final transient ThesaurusEditionCsvStructuredImportService csvStructuredImportService;
    private final transient ThesaurusPickerImportProgressTracker importProgressTracker;

    private boolean importMode;
    private String format = FORMAT_SKOS;
    private String error;
    private String info;

    private List<LanguageOption> languages = Collections.emptyList();
    private List<ProjectOption> projects = Collections.emptyList();
    private boolean superAdmin;

    private String selectedLang;
    private String selectedProjectId;
    private String thesaurusName;
    private String persistentName;
    private String formatDate = "yyyy-MM-dd";
    private int skosTypeImport;
    private int csvDelimiter;
    private String selectedIdentifier = "sans";
    private String prefixHandle = "";
    private String prefixDoi = "";
    private boolean existingThesaurusDetected;
    private String existingThesaurusId;
    private boolean importAsMaster;
    /** {@code true} = privé, {@code false} = public (défaut). */
    private boolean privateThesaurus;

    private transient Part upload;
    /** Chemin absolu du fichier temporaire d'upload (sous {@code java.io.tmpdir/opentheso-import/}). */
    private String uploadTempPath;
    private String uploadedFileName;
    private long uploadedFileSize;
    private boolean loadDone;
    private int totalConcepts;
    private String skosUri;
    private String csvWarning;

    private transient SKOSXmlDocument skosDocument;
    private List<ThesaurusCsvConceptObject> csvConcepts = Collections.emptyList();
    private List<String> csvDetectedLangs = Collections.emptyList();
    private transient NodeTree structuredRoot;

    public boolean isCanImportThesaurus() {
        return toolboxAccessPolicy.canCreateOrImportThesaurus(userSession);
    }

    public void open() {
        if (!isCanImportThesaurus()) {
            importMode = false;
            return;
        }
        resetState();
        importMode = true;
        var options = newThesaurusService.loadFormOptions(
                userSession.getCurrentUserId() != null ? userSession.getCurrentUserId() : -1,
                userSession.isSuperAdmin()
        );
        languages = options.languages() != null ? options.languages() : Collections.emptyList();
        projects = options.projects() != null ? options.projects() : Collections.emptyList();
        superAdmin = options.superAdmin();
        selectedLang = resolveDefaultLang();
        if (!superAdmin && projects.size() == 1) {
            selectedProjectId = String.valueOf(projects.get(0).id());
        }
    }

    public void cancel() {
        resetState();
        importMode = false;
    }

    public void setFormat(String next) {
        if (!FORMAT_SKOS.equals(next) && !FORMAT_CSV.equals(next) && !FORMAT_CSV_STRUCTURED.equals(next)) {
            return;
        }
        if (Strings.CS.equals(format, next)) {
            return;
        }
        format = next;
        // Conserver un Part déjà lié dans le même cycle JSF (ordre des setters).
        Part inFlight = upload;
        clearLoadedFile();
        upload = inFlight;
        error = null;
        info = null;
    }

    public void selectFormat(String next) {
        setFormat(next);
    }

    public boolean isFormat(String value) {
        return Strings.CS.equals(format, value);
    }

    /** Déclenché à la sélection du fichier (ajax valueChange), comme les imports toolbox. */
    public void onFileSelected() {
        error = null;
        info = null;
        if (!isCanImportThesaurus()) {
            error = v2LocaleBean.getMsg("v2.picker.import.denied");
            return;
        }
        if (upload == null || upload.getSize() <= 0) {
            error = v2LocaleBean.getMsg("v2.picker.import.fileRequired");
            return;
        }
        try {
            uploadedFileName = StringUtils.defaultIfBlank(fileNameOf(upload), "fichier");
            if (!persistUploadToTemp(upload)) {
                return;
            }
            parseUploadedFile();
        } catch (Exception ex) {
            clearLoadedFile();
            error = StringUtils.defaultIfBlank(ex.getMessage(), v2LocaleBean.getMsg("v2.picker.import.loadError"));
        } finally {
            upload = null;
        }
    }

    /**
     * Re-analyse le fichier déjà chargé avec les options courantes
     * (type RDF, séparateur, langue…), ou lit un nouveau Part si présent.
     */
    public void loadFile() {
        error = null;
        info = null;
        if (!isCanImportThesaurus()) {
            error = v2LocaleBean.getMsg("v2.picker.import.denied");
            return;
        }
        try {
            if (upload != null && upload.getSize() > 0) {
                uploadedFileName = StringUtils.defaultIfBlank(fileNameOf(upload), "fichier");
                if (!persistUploadToTemp(upload)) {
                    return;
                }
            }
            if (!hasUploadedFile()) {
                error = v2LocaleBean.getMsg("v2.picker.import.fileRequired");
                return;
            }
            parseUploadedFile();
        } catch (Exception ex) {
            clearParsedResult();
            error = StringUtils.defaultIfBlank(ex.getMessage(), v2LocaleBean.getMsg("v2.picker.import.loadError"));
        } finally {
            upload = null;
        }
    }

    /**
     * Écrit le {@link Part} vers un fichier temporaire et met à jour la métadonnée de taille.
     * @return {@code false} si le fichier est vide ou trop volumineux (erreur déjà renseignée)
     */
    private boolean persistUploadToTemp(Part part) throws IOException {
        if (part.getSize() > MAX_UPLOAD_BYTES) {
            rejectUpload(v2LocaleBean.getMsg("v2.picker.import.fileTooLarge"));
            return false;
        }

        Path dir = Path.of(System.getProperty("java.io.tmpdir"), UPLOAD_TEMP_DIR);
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, UPLOAD_TEMP_PREFIX, null);
        boolean keep = false;
        try {
            long written = 0L;
            try (InputStream in = part.getInputStream();
                 OutputStream out = Files.newOutputStream(temp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    written += n;
                    if (written > MAX_UPLOAD_BYTES) {
                        rejectUpload(v2LocaleBean.getMsg("v2.picker.import.fileTooLarge"));
                        return false;
                    }
                    out.write(buf, 0, n);
                }
            }

            if (written == 0) {
                rejectUpload(v2LocaleBean.getMsg("v2.picker.import.fileRequired"));
                return false;
            }

            deleteUploadTempFile();
            uploadTempPath = temp.toAbsolutePath().toString();
            uploadedFileSize = written;
            keep = true;
            return true;
        } finally {
            if (!keep) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // best-effort
                }
            }
        }
    }

    private void rejectUpload(String message) {
        deleteUploadTempFile();
        clearParsedResult();
        uploadedFileName = null;
        error = message;
    }

    private void parseUploadedFile() throws IOException {
        clearParsedResult();
        if (!hasUploadedFile()) {
            throw new IllegalStateException(v2LocaleBean.getMsg("v2.picker.import.fileRequired"));
        }
        Path path = Path.of(uploadTempPath);
        if (FORMAT_SKOS.equals(format)) {
            try (InputStream in = Files.newInputStream(path)) {
                loadSkos(in);
            }
        } else if (FORMAT_CSV.equals(format)) {
            byte[] content = Files.readAllBytes(path);
            loadCsv(content);
        } else {
            byte[] content = Files.readAllBytes(path);
            loadCsvStructured(content);
        }
    }

    private static String fileNameOf(Part part) {
        if (part == null) {
            return null;
        }
        String submitted = part.getSubmittedFileName();
        if (StringUtils.isNotBlank(submitted)) {
            return Paths.get(submitted).getFileName().toString();
        }
        return null;
    }

    public void submitImport() {
        error = null;
        info = null;
        if (!isCanImportThesaurus()) {
            error = v2LocaleBean.getMsg("v2.picker.import.denied");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            error = v2LocaleBean.getMsg("v2.picker.import.denied");
            return;
        }

        String progressKey = resolveProgressKey();
        importProgressTracker.start(progressKey);
        applyProgress(progressKey, 2, ThesaurusPickerImportProgressTracker.STEP_PREPARE, "prepare",
                v2LocaleBean.getMsg("v2.picker.import.progress.prepare"), 0, Math.max(totalConcepts, 0));

        String thesaurusId;
        try {
            thesaurusId = ThesaurusPickerImportProgressContext.callWith(
                    (done, total, phase) -> onImportProgress(progressKey, done, total, phase),
                    () -> {
                        if (!ensureParsedForImport()) {
                            throw new IllegalStateException(StringUtils.defaultIfBlank(
                                    error, v2LocaleBean.getMsg("v2.picker.import.fileRequired")));
                        }
                        applyProgress(progressKey, 6, ThesaurusPickerImportProgressTracker.STEP_CREATE, "create",
                                v2LocaleBean.getMsg("v2.picker.import.progress.create"), -1, Math.max(totalConcepts, 0));
                        String id;
                        if (FORMAT_SKOS.equals(format)) {
                            id = submitSkos(userId);
                        } else if (FORMAT_CSV.equals(format)) {
                            id = submitCsv(userId);
                        } else {
                            id = submitCsvStructured(userId);
                        }
                        if (StringUtils.isBlank(id)) {
                            throw new IllegalStateException(v2LocaleBean.getMsg("v2.picker.import.failed"));
                        }
                        return id;
                    }
            );
        } catch (NumberFormatException ex) {
            error = v2LocaleBean.getMsg("v2.picker.import.invalidProject");
            importProgressTracker.fail(progressKey, error);
            return;
        } catch (Exception ex) {
            error = StringUtils.defaultIfBlank(ex.getMessage(), v2LocaleBean.getMsg("v2.picker.import.failed"));
            importProgressTracker.fail(progressKey, error);
            return;
        }

        applyProgress(progressKey, 100, ThesaurusPickerImportProgressTracker.STEP_FINALIZE, "done",
                v2LocaleBean.getMsg("v2.picker.import.progress.done"), -1, -1);
        importProgressTracker.succeed(progressKey);

        MessageUtils.showInformationMessage(
                v2LocaleBean.getMsg("v2.picker.import.success") + " (" + thesaurusId + ")");
        importMode = false;
        resetState();
        String successFlash = v2LocaleBean.getMsg("v2.picker.import.success")
                + " (" + thesaurusId + ")";
        try {
            FacesLookup.refreshPickerList(successFlash);
        } catch (Exception ex) {
            info = successFlash;
        } finally {
            importProgressTracker.clear(progressKey);
        }
    }

    private void onImportProgress(String key, int done, int total, String phase) {
        if (phase == null) {
            return;
        }
        if ("create".equals(phase)) {
            applyProgress(key, 8, ThesaurusPickerImportProgressTracker.STEP_CREATE, phase,
                    v2LocaleBean.getMsg("v2.picker.import.progress.create"), done, total);
            return;
        }
        if (phase.startsWith("finalize")) {
            String message = switch (phase) {
                case "finalize.facets" -> v2LocaleBean.getMsg("v2.picker.import.progress.finalize.facets");
                case "finalize.groups" -> v2LocaleBean.getMsg("v2.picker.import.progress.finalize.groups");
                case "finalize.langs" -> v2LocaleBean.getMsg("v2.picker.import.progress.finalize.langs");
                case "finalize.images" -> v2LocaleBean.getMsg("v2.picker.import.progress.finalize.images");
                default -> v2LocaleBean.getMsg("v2.picker.import.progress.finalize");
            };
            int percent = switch (phase) {
                case "finalize.facets" -> 93;
                case "finalize.groups" -> 95;
                case "finalize.langs" -> 97;
                case "finalize.images" -> 98;
                default -> 92;
            };
            applyProgress(key, percent, ThesaurusPickerImportProgressTracker.STEP_FINALIZE, phase,
                    message, done, total);
            return;
        }
        if ("concepts".equals(phase)) {
            int percent = 12;
            if (total > 0 && done >= 0) {
                percent = 12 + (int) Math.round(80.0 * Math.min(done, total) / (double) total);
            }
            String message = total > 0 && done >= 0
                    ? v2LocaleBean.getMsg("v2.picker.import.progress.conceptsCount", Math.max(done, 0), total)
                    : v2LocaleBean.getMsg("v2.picker.import.progress.concepts");
            applyProgress(key, Math.min(92, percent), ThesaurusPickerImportProgressTracker.STEP_CONCEPTS, phase,
                    message, done, total);
            return;
        }
        applyProgress(key, -1, ThesaurusPickerImportProgressTracker.STEP_PREPARE, phase,
                v2LocaleBean.getMsg("v2.picker.import.progress.prepare"), done, total);
    }

    private void applyProgress(String key, int percent, int step, String phase, String message, int done, int total) {
        var current = importProgressTracker.snapshot(key);
        int resolvedPercent = percent >= 0 ? percent : current.percent();
        importProgressTracker.update(key, resolvedPercent, step, phase, message, done, total);
    }

    private String resolveProgressKey() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces != null) {
            Object session = faces.getExternalContext().getSession(false);
            if (session instanceof jakarta.servlet.http.HttpSession httpSession) {
                return httpSession.getId();
            }
        }
        Integer userId = userSession.getCurrentUserId();
        return "user-" + (userId != null ? userId : "anon");
    }

    /**
     * Les résultats d'analyse (SKOS / arbre CSV) sont {@code transient} : après un postback
     * ils peuvent être nuls alors que le fichier temp et {@link #loadDone} restent.
     * On re-parse avant l'import si besoin.
     */
    private boolean ensureParsedForImport() {
        if (!hasUploadedFile()) {
            return false;
        }
        boolean needsParse = (FORMAT_SKOS.equals(format) && skosDocument == null)
                || (FORMAT_CSV.equals(format) && (csvConcepts == null || csvConcepts.isEmpty()))
                || (FORMAT_CSV_STRUCTURED.equals(format) && structuredRoot == null);
        if (needsParse) {
            try {
                parseUploadedFile();
            } catch (Exception ex) {
                error = StringUtils.defaultIfBlank(ex.getMessage(), v2LocaleBean.getMsg("v2.picker.import.loadError"));
                return false;
            }
        }
        if (FORMAT_SKOS.equals(format)) {
            return skosDocument != null;
        }
        if (FORMAT_CSV.equals(format)) {
            return csvConcepts != null && !csvConcepts.isEmpty() && StringUtils.isNotBlank(thesaurusName);
        }
        return structuredRoot != null && StringUtils.isNotBlank(thesaurusName);
    }

    public void selectLanguage() {
        FacesContext faces = FacesContext.getCurrentInstance();
        String code = faces != null
                ? faces.getExternalContext().getRequestParameterMap().get("importLangCode")
                : null;
        selectedLang = StringUtils.defaultString(code);
        if (FORMAT_SKOS.equals(format) && loadDone && hasUploadedFile()) {
            try {
                parseUploadedFile();
            } catch (Exception ignored) {
                // aperçu déjà affiché ; l'utilisateur pourra relancer l'analyse
            }
        }
    }

    public boolean isLanguageSelected(String code) {
        return Strings.CI.equals(selectedLang, code);
    }

    public void onProjectChanged() {
        if (FORMAT_SKOS.equals(format) && loadDone) {
            if (skosDocument == null && hasUploadedFile()) {
                try {
                    parseUploadedFile();
                } catch (Exception ignored) {
                    return;
                }
            }
            if (skosDocument != null) {
                detectExistingSkosThesaurus();
            }
        }
    }

    public boolean isSkosReady() {
        return FORMAT_SKOS.equals(format) && loadDone && hasUploadedFile();
    }

    public boolean isCsvReady() {
        return FORMAT_CSV.equals(format) && loadDone && hasUploadedFile();
    }

    public boolean isCsvStructuredReady() {
        return FORMAT_CSV_STRUCTURED.equals(format) && loadDone && hasUploadedFile();
    }

    public boolean isImportReady() {
        if (!loadDone || !hasUploadedFile()) {
            return false;
        }
        if (FORMAT_CSV.equals(format) || FORMAT_CSV_STRUCTURED.equals(format)) {
            return StringUtils.isNotBlank(thesaurusName);
        }
        return true;
    }

    /** Indique qu'un fichier uploadé est disponible (chemin temp + taille). */
    public boolean isHasUpload() {
        return hasUploadedFile();
    }

    private boolean hasUploadedFile() {
        return StringUtils.isNotBlank(uploadTempPath) && uploadedFileSize > 0;
    }

    private void loadSkos(InputStream in) throws IOException {
        var errorBuffer = new StringBuilder();
        var result = skosImportService.loadSkosFile(in, skosTypeImport, selectedLang, errorBuffer);
        if (!errorBuffer.isEmpty()) {
            throw new IllegalStateException(errorBuffer.toString());
        }
        skosDocument = result.document();
        totalConcepts = result.totalConcepts();
        skosUri = result.uri();
        loadDone = true;
        detectExistingSkosThesaurus();
        info = v2LocaleBean.getMsg("v2.picker.import.loaded");
    }

    private void loadCsv(byte[] content) {
        var result = csvImportService.loadCsvFile(content, CsvDelimiterSupport.resolveDelimiter(csvDelimiter));
        if (!result.success()) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(result.error(), "CSV invalide"));
        }
        csvConcepts = new ArrayList<>(result.conceptObjects());
        csvDetectedLangs = result.languages();
        totalConcepts = result.totalConcepts();
        csvWarning = result.warning();
        loadDone = true;
        suggestThesaurusNameFromFile();
        info = v2LocaleBean.getMsg("v2.picker.import.loaded");
    }

    private void loadCsvStructured(byte[] content) {
        var result = csvStructuredImportService.loadCsvFile(content, CsvDelimiterSupport.resolveDelimiter(csvDelimiter));
        if (!result.success()) {
            throw new IllegalStateException(StringUtils.defaultIfBlank(result.error(), "CSV structuré invalide"));
        }
        structuredRoot = result.root();
        totalConcepts = result.totalConcepts();
        loadDone = true;
        suggestThesaurusNameFromFile();
        info = v2LocaleBean.getMsg("v2.picker.import.loaded");
    }

    /** Préremplit le nom CSV depuis le fichier si l'utilisateur ne l'a pas encore saisi. */
    private void suggestThesaurusNameFromFile() {
        if (StringUtils.isNotBlank(thesaurusName) || StringUtils.isBlank(uploadedFileName)) {
            return;
        }
        String base = uploadedFileName.trim();
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0 && slash < base.length() - 1) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        if (StringUtils.isNotBlank(base)) {
            thesaurusName = base.trim();
        }
    }

    private String submitSkos(int userId) {
        if (skosDocument == null) {
            throw new IllegalStateException(v2LocaleBean.getMsg("v2.picker.import.fileRequired"));
        }
        Integer projectId = parseProjectId();
        boolean asMaster = existingThesaurusDetected && importAsMaster;
        String thesaurusId = skosImportService.importNewThesaurus(
                skosDocument,
                formatDate,
                userId,
                superAdmin,
                projectId,
                selectedLang,
                new ThesaurusEditionSkosImportService.SkosImportOptions(
                        selectedIdentifier,
                        prefixHandle,
                        prefixDoi,
                        persistentName,
                        asMaster
                )
        );
        applyVisibility(thesaurusId);
        return thesaurusId;
    }

    private String submitCsv(int userId) {
        if (StringUtils.isBlank(thesaurusName)) {
            throw new IllegalStateException(v2LocaleBean.getMsg("v2.picker.import.nameRequired"));
        }
        var outcome = csvImportService.importNewThesaurus(
                thesaurusName.trim(),
                selectedLang,
                formatDate,
                userId,
                StringUtils.defaultIfBlank(userSession.getCurrentUsername(), "user"),
                superAdmin,
                new ThesaurusEditionCsvImportService.CsvImportExtras(
                        parseProjectId(),
                        csvConcepts,
                        csvDetectedLangs,
                        persistentName
                )
        );
        applyVisibility(outcome.thesaurusId());
        return outcome.thesaurusId();
    }

    private String submitCsvStructured(int userId) {
        if (StringUtils.isBlank(thesaurusName)) {
            throw new IllegalStateException(v2LocaleBean.getMsg("v2.picker.import.nameRequired"));
        }
        var outcome = csvStructuredImportService.importNewThesaurus(
                thesaurusName.trim(),
                selectedLang,
                userId,
                StringUtils.defaultIfBlank(userSession.getCurrentUsername(), "user"),
                superAdmin,
                parseProjectId(),
                structuredRoot
        );
        applyVisibility(outcome.thesaurusId());
        return outcome.thesaurusId();
    }

    private void applyVisibility(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        toolboxThesaurusPersistence.setVisibility(thesaurusId, privateThesaurus);
    }

    private void detectExistingSkosThesaurus() {
        existingThesaurusDetected = false;
        existingThesaurusId = null;
        importAsMaster = false;
        if (skosDocument == null) {
            return;
        }
        var existing = skosImportService.findExistingThesaurusId(skosDocument, parseProjectId(), selectedLang);
        if (existing.isPresent()) {
            existingThesaurusDetected = true;
            existingThesaurusId = existing.get();
        }
    }

    private Integer parseProjectId() {
        if (StringUtils.isBlank(selectedProjectId)) {
            return null;
        }
        return Integer.parseInt(selectedProjectId.trim());
    }

    private String resolveDefaultLang() {
        if (languages == null || languages.isEmpty()) {
            return StringUtils.defaultIfBlank(v2LocaleBean.getIdLangue(), "fr");
        }
        String ui = v2LocaleBean.getIdLangue();
        return languages.stream()
                .map(LanguageOption::code)
                .filter(code -> Strings.CI.equals(code, ui))
                .findFirst()
                .or(() -> languages.stream().map(LanguageOption::code).filter("fr"::equalsIgnoreCase).findFirst())
                .orElse(languages.get(0).code());
    }

    private void clearParsedResult() {
        loadDone = false;
        totalConcepts = 0;
        skosUri = null;
        csvWarning = null;
        skosDocument = null;
        csvConcepts = Collections.emptyList();
        csvDetectedLangs = Collections.emptyList();
        structuredRoot = null;
        existingThesaurusDetected = false;
        existingThesaurusId = null;
        importAsMaster = false;
    }

    public String getFileAcceptHint() {
        if (FORMAT_SKOS.equals(format)) {
            return v2LocaleBean.getMsg("v2.picker.import.acceptSkos");
        }
        return v2LocaleBean.getMsg("v2.picker.import.acceptCsv");
    }

    /** Valeur HTML accept= pour le sélecteur de fichier (selon le format). */
    public String getFileAccept() {
        if (FORMAT_SKOS.equals(format)) {
            return ".rdf,.xml,.ttl,.json,.jsonld,.owl,application/rdf+xml,text/turtle,application/ld+json";
        }
        return ".csv,.tsv,.txt,text/csv,text/tab-separated-values,text/plain";
    }

    public String getUploadedFileMeta() {
        if (!hasUploadedFile()) {
            return "";
        }
        long bytes = uploadedFileSize;
        if (bytes < 1024) {
            return bytes + " o";
        }
        if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.FRENCH, "%.1f Ko", bytes / 1024.0);
        }
        return String.format(java.util.Locale.FRENCH, "%.1f Mo", bytes / (1024.0 * 1024.0));
    }

    public void clearUploadedFile() {
        error = null;
        info = null;
        clearLoadedFile();
    }

    private void clearLoadedFile() {
        upload = null;
        deleteUploadTempFile();
        uploadedFileName = null;
        clearParsedResult();
    }

    private void deleteUploadTempFile() {
        if (StringUtils.isNotBlank(uploadTempPath)) {
            try {
                Files.deleteIfExists(Path.of(uploadTempPath));
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }
        uploadTempPath = null;
        uploadedFileSize = 0L;
    }

    private void resetState() {
        format = FORMAT_SKOS;
        error = null;
        info = null;
        thesaurusName = "";
        persistentName = "";
        formatDate = "yyyy-MM-dd";
        skosTypeImport = 0;
        csvDelimiter = 0;
        selectedIdentifier = "sans";
        prefixHandle = "";
        prefixDoi = "";
        selectedProjectId = null;
        privateThesaurus = false;
        importAsMaster = false;
        clearLoadedFile();
    }

    public void setUpload(Part upload) {
        // Ignore les parts vides du postback d'import (sinon le cycle JSF peut échouer).
        if (upload != null && upload.getSize() > 0) {
            this.upload = upload;
        }
    }

    /**
     * Évite une dépendance circulaire ViewScoped : retour à la liste du picker après import.
     */
    static final class FacesLookup {
        private FacesLookup() {
        }

        static void refreshPickerList(String flashMessage) {
            var faces = jakarta.faces.context.FacesContext.getCurrentInstance();
            if (faces == null || faces.getResponseComplete()) {
                return;
            }
            ThesaurusPickerBean picker = faces.getApplication().evaluateExpressionGet(
                    faces, "#{v2ThesaurusPickerBean}", ThesaurusPickerBean.class);
            if (picker != null) {
                picker.cancelImportThesaurus();
                picker.showListFlash(flashMessage);
                picker.load();
            }
        }
    }
}
