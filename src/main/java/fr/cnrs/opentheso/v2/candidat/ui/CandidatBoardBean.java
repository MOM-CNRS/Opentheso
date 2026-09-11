package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.model.CandidatBoardItem;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.policy.CandidatAccessPolicy;
import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatSkosImportService;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tableau de bord V2 : propositions en cours, acceptées et rejetées.
 */
@Named("v2CandidatBoardBean")
@ViewScoped
@RequiredArgsConstructor
public class CandidatBoardBean implements Serializable {

    private static final String TAB_PENDING = "attente";
    private static final String TAB_ACCEPTED = "insere";
    private static final String TAB_REJECTED = "rejete";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient CandidatReadService candidatReadService;
    private final transient CandidatProcessService candidatProcessService;
    private final transient CandidatExportService candidatExportService;
    private final transient CandidatSkosImportService candidatSkosImportService;
    private final transient CandidatAccessPolicy candidatAccessPolicy;
    private final transient ThesaurusPreferencesProvider thesaurusPreferencesProvider;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient V2LocaleBean localeBean;
    private boolean loaded;

    @Getter
    @Setter
    private String activeTab = TAB_PENDING;
    @Getter
    @Setter
    private String searchTerm = "";
    @Getter
    @Setter
    private boolean mineOnly;
    @Getter
    private final Set<String> selectedIds = new LinkedHashSet<>();
    @Getter
    @Setter
    private String batchMessage = "";
    @Getter
    @Setter
    private String exportFormat = "rdf";
    @Getter
    @Setter
    private transient Part importUpload;
    @Getter
    @Setter
    private String adminBatchMessage = "";
    private List<CandidatBoardItem> pending = Collections.emptyList();
    private List<CandidatBoardItem> accepted = Collections.emptyList();
    private List<CandidatBoardItem> rejected = Collections.emptyList();
    private int pendingCount;
    private int acceptedCount;
    private int rejectedCount;

    public void load() {
        loaded = false;
        ensureLoaded();
    }

    public void load(String thesaurusId) {
        loaded = false;
        reload(thesaurusId, thesaurusViewBean.getSelectedLang());
        loaded = true;
    }

    public void search() {
        load();
    }

    public void toggleMine() {
        load();
    }

    public boolean isCanProcess() {
        return candidatAccessPolicy.canProcess(userSession, thesaurusViewBean.getId(), null);
    }

    public boolean isCanImport() {
        return candidatAccessPolicy.canImport(userSession, thesaurusViewBean.getId());
    }

    public boolean isCanExport() {
        return candidatAccessPolicy.canExport(userSession, thesaurusViewBean.getId());
    }

    public void toggleSelect(String conceptId) {
        if (StringUtils.isBlank(conceptId)) {
            return;
        }
        if (!selectedIds.add(conceptId)) {
            selectedIds.remove(conceptId);
        }
    }

    public boolean isSelected(String conceptId) {
        return StringUtils.isNotBlank(conceptId) && selectedIds.contains(conceptId);
    }

    public void acceptSelected() {
        if (!isCanProcess()) {
            return;
        }
        List<CandidatDto> candidates = loadSelectedPending();
        if (candidates.isEmpty()) {
            MessageUtils.showWarnMessage(localeBean.getMsg("v2.candidat.board.acceptSelected"));
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        String thesaurusId = thesaurusViewBean.getId();
        String lang = StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
        candidatProcessService.prepareCandidatesForAccept(candidates, thesaurusId, lang);
        var preferences = thesaurusPreferencesProvider.findPreferences(thesaurusId).orElse(null);
        CandidatDto failed = candidatProcessService.acceptCandidatesBatch(
                candidates,
                resolveBatchMessage(),
                userId,
                userSession.getCurrentUsername(),
                preferences
        );
        if (failed != null) {
            MessageUtils.showErrorMessage(localeBean.getMsg("v2.candidat.insertError")
                    + " : " + failed.getNomPref());
            return;
        }
        notifyCreatorsAccepted(candidates);
        selectedIds.clear();
        batchMessage = "";
        adminBatchMessage = "";
        load(thesaurusId);
        MessageUtils.showInformationMessage(localeBean.getMsg("v2.candidat.insertOk"));
    }

    public void rejectSelected() {
        if (!isCanProcess()) {
            return;
        }
        List<CandidatDto> candidates = loadSelectedPending();
        if (candidates.isEmpty()) {
            MessageUtils.showWarnMessage(localeBean.getMsg("v2.candidat.board.rejectSelected"));
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        CandidatDto failed = candidatProcessService.rejectCandidatesBatch(
                candidates,
                resolveBatchMessage(),
                userId,
                userSession.getCurrentUsername()
        );
        if (failed != null) {
            MessageUtils.showErrorMessage(localeBean.getMsg("v2.candidat.rejectError")
                    + " : " + failed.getNomPref());
            return;
        }
        notifyCreatorsRejected(candidates);
        selectedIds.clear();
        batchMessage = "";
        adminBatchMessage = "";
        load(thesaurusViewBean.getId());
        MessageUtils.showInformationMessage(localeBean.getMsg("v2.candidat.rejectOk"));
    }

    public void exportSelectedSkos() {
        exportPendingSkos();
    }

    public void exportPendingSkos() {
        if (!isCanExport()) {
            return;
        }
        String thesaurusId = thesaurusViewBean.getId();
        String lang = StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
        List<CandidatDto> pendingCandidates = candidatReadService.loadByStatus(
                thesaurusId, lang, CandidatStatusCode.PENDING);
        List<CandidatDto> toExport;
        if (!selectedIds.isEmpty()) {
            toExport = pendingCandidates.stream()
                    .filter(dto -> selectedIds.contains(dto.getIdConcepte()))
                    .toList();
        } else {
            toExport = pendingCandidates;
        }
        if (toExport.isEmpty()) {
            MessageUtils.showWarnMessage(localeBean.getMsg("v2.candidat.exportError"));
            return;
        }
        try {
            var result = candidatExportService.exportPendingCandidates(
                    thesaurusId,
                    toExport,
                    StringUtils.defaultIfBlank(exportFormat, "rdf"),
                    ignored -> { });
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces == null) {
                return;
            }
            HttpServletResponse response = (HttpServletResponse) faces.getExternalContext().getResponse();
            response.reset();
            response.setContentType(result.contentType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + result.filename() + "\"");
            try (OutputStream out = response.getOutputStream()) {
                out.write(result.content());
                out.flush();
            }
            faces.responseComplete();
        } catch (Exception ex) {
            MessageUtils.showErrorMessage(
                    ex.getMessage() != null ? ex.getMessage() : localeBean.getMsg("v2.candidat.exportError"));
        }
    }

    public void importSkos() {
        if (!isCanImport()) {
            return;
        }
        if (importUpload == null) {
            MessageUtils.showWarnMessage(localeBean.getMsg("v2.candidat.board.import"));
            return;
        }
        String thesaurusId = thesaurusViewBean.getId();
        Integer userId = userSession.getCurrentUserId();
        if (StringUtils.isBlank(thesaurusId) || userId == null) {
            return;
        }
        String lang = StringUtils.defaultIfBlank(
                thesaurusViewBean.getSelectedLang(),
                thesaurusContext.resolveWorkLanguage());
        var error = new StringBuilder();
        try (InputStream inputStream = importUpload.getInputStream()) {
            var loadedFile = candidatSkosImportService.loadSkosFile(inputStream, 0, lang, error);
            if (!error.isEmpty() || loadedFile.document() == null
                    || loadedFile.document().getConceptList() == null
                    || loadedFile.document().getConceptList().isEmpty()) {
                MessageUtils.showErrorMessage(
                        !error.isEmpty() ? error.toString() : localeBean.getMsg("v2.candidat.board.import"));
                return;
            }
            candidatSkosImportService.importCandidatesForThesaurus(
                    loadedFile.document(),
                    thesaurusId,
                    userId,
                    lang,
                    null
            );
            importUpload = null;
            load(thesaurusId);
            MessageUtils.showInformationMessage(localeBean.getMsg("v2.candidat.board.import"));
        } catch (Exception ex) {
            MessageUtils.showErrorMessage(
                    ex.getMessage() != null ? ex.getMessage() : localeBean.getMsg("v2.candidat.board.import"));
        }
    }

    public List<CandidatBoardItem> getPending() {
        ensureLoaded();
        return pending;
    }

    public List<CandidatBoardItem> getAccepted() {
        ensureLoaded();
        return accepted;
    }

    public List<CandidatBoardItem> getRejected() {
        ensureLoaded();
        return rejected;
    }

    public int getPendingCount() {
        ensureLoaded();
        return pendingCount;
    }

    public int getAcceptedCount() {
        ensureLoaded();
        return acceptedCount;
    }

    public int getRejectedCount() {
        ensureLoaded();
        return rejectedCount;
    }

    public boolean isLoggedIn() {
        return userSession != null && userSession.isLoggedIn();
    }

    public boolean isCapped(String tab) {
        ensureLoaded();
        int shown = itemsFor(tab).size();
        int total = countFor(tab);
        return total > shown;
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        reload(thesaurusViewBean.getId(), thesaurusViewBean.getSelectedLang());
        loaded = true;
    }

    private void reload(String thesaurusId, String lang) {
        if (StringUtils.isBlank(thesaurusId)) {
            pending = Collections.emptyList();
            accepted = Collections.emptyList();
            rejected = Collections.emptyList();
            pendingCount = 0;
            acceptedCount = 0;
            rejectedCount = 0;
            return;
        }
        lang = StringUtils.defaultIfBlank(lang, "fr");
        String query = StringUtils.trimToNull(searchTerm);

        Integer userId = mineOnly && isLoggedIn() ? userSession.getCurrentUserId() : null;
        pending = mapList(
                candidatReadService.searchByStatus(thesaurusId, lang, CandidatStatusCode.PENDING, query),
                "candidat",
                userId
        );
        accepted = mapList(
                candidatReadService.searchByStatus(thesaurusId, lang, CandidatStatusCode.ACCEPTED, query),
                "concept",
                userId
        );
        rejected = mapList(
                candidatReadService.searchByStatus(thesaurusId, lang, CandidatStatusCode.REJECTED, query),
                "candidat",
                userId
        );

        if (StringUtils.isNotBlank(query) || mineOnly) {
            pendingCount = pending.size();
            acceptedCount = accepted.size();
            rejectedCount = rejected.size();
            return;
        }
        pendingCount = candidatReadService.countByStatus(thesaurusId, CandidatStatusCode.PENDING);
        acceptedCount = candidatReadService.countByStatus(thesaurusId, CandidatStatusCode.ACCEPTED);
        rejectedCount = candidatReadService.countByStatus(thesaurusId, CandidatStatusCode.REJECTED);
    }

    private List<CandidatBoardItem> mapList(List<CandidatDto> source, String openType, Integer userId) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream()
                .filter(dto -> userId == null || dto.getCreatedById() == userId)
                .map(dto -> toItem(dto, openType))
                .toList();
    }

    private CandidatBoardItem toItem(CandidatDto dto, String openType) {
        String title = StringUtils.defaultIfBlank(dto.getNomPref(), dto.getIdConcepte());
        String author = StringUtils.defaultIfBlank(dto.getCreatedBy(), "—");
        String date = formatDate(V2Dates.toInstant(dto.getCreationDate()));
        String meta = StringUtils.isBlank(date) ? author : author + " · " + date;
        int up = Math.max(0, dto.getNbrVote());
        int down = Math.max(0, dto.getNbrDownVote());
        return new CandidatBoardItem(
                dto.getIdConcepte(),
                title,
                meta,
                "+" + up + " −" + down,
                openType
        );
    }

    private List<CandidatDto> loadSelectedPending() {
        if (selectedIds.isEmpty()) {
            return List.of();
        }
        String thesaurusId = thesaurusViewBean.getId();
        String lang = StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
        List<CandidatDto> pendingCandidates = candidatReadService.loadByStatus(
                thesaurusId, lang, CandidatStatusCode.PENDING);
        List<CandidatDto> selected = new ArrayList<>();
        for (CandidatDto dto : pendingCandidates) {
            if (selectedIds.contains(dto.getIdConcepte())) {
                selected.add(dto);
            }
        }
        return selected;
    }

    private void notifyCreatorsAccepted(List<CandidatDto> candidates) {
        var alertMails = candidatProcessService.resolveAlertMails(candidates);
        String message = resolveBatchMessage();
        String title = resolveThesaurusTitle();
        String baseUrl = resolveAppBaseUrl();
        for (CandidatDto candidate : candidates) {
            String mail = alertMails.get(candidate.getCreatedById());
            if (mail != null && !candidatProcessService.sendAcceptedMail(
                    mail, candidate, message, title, baseUrl)) {
                MessageUtils.showErrorMessage(localeBean.getMsg("v2.candidat.mailError"));
            }
        }
    }

    private void notifyCreatorsRejected(List<CandidatDto> candidates) {
        var alertMails = candidatProcessService.resolveAlertMails(candidates);
        String message = resolveBatchMessage();
        String title = resolveThesaurusTitle();
        String baseUrl = resolveAppBaseUrl();
        for (CandidatDto candidate : candidates) {
            String mail = alertMails.get(candidate.getCreatedById());
            if (mail != null && !candidatProcessService.sendRejectedMail(
                    mail, candidate, message, title, baseUrl)) {
                MessageUtils.showErrorMessage(localeBean.getMsg("v2.candidat.mailError"));
            }
        }
    }

    private String resolveBatchMessage() {
        return StringUtils.defaultIfBlank(adminBatchMessage, batchMessage);
    }

    private String resolveThesaurusTitle() {
        String title = thesaurusContext.getCurrentThesaurusTitle();
        return title != null ? title : thesaurusViewBean.getId();
    }

    private String resolveAppBaseUrl() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            return "";
        }
        var ext = faces.getExternalContext();
        String origin = ext.getRequestHeaderMap().get("origin");
        if (origin == null) {
            origin = "";
        }
        return origin + ext.getRequestContextPath();
    }

    private String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(instant);
    }

    private List<CandidatBoardItem> itemsFor(String tab) {
        return switch (StringUtils.defaultString(tab)) {
            case TAB_ACCEPTED -> accepted;
            case TAB_REJECTED -> rejected;
            default -> pending;
        };
    }

    private int countFor(String tab) {
        return switch (StringUtils.defaultString(tab)) {
            case TAB_ACCEPTED -> acceptedCount;
            case TAB_REJECTED -> rejectedCount;
            default -> pendingCount;
        };
    }
}
