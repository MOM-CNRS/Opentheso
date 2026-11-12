package fr.cnrs.opentheso.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.candidat.model.CandidatBoardItem;
import fr.cnrs.opentheso.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.candidat.policy.CandidatAccessPolicy;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.candidat.service.CandidatSkosImportService;
import fr.cnrs.opentheso.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.shared.time.Dates;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.shared.ui.LocaleBean;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
@Named("candidatBoardBean")
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
    private final transient RightsService rightsService;
    private final transient LocaleBean localeBean;
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
    private String selectId;
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
    @Getter
    private int badgeCount;
    @Getter
    private String notice = "";
    @Getter
    private boolean noticeError;
    @Getter
    private String noticeToken = "";

    public void load() {
        loaded = false;
        ensureLoaded();
        badgeCount = pendingCount;
    }

    /**
     * Bouton du rail : thésaurus ouvert, et super-administrateur ou administrateur
     * du projet auquel ce thésaurus est rattaché.
     */
    public boolean isBoardLinkVisible() {
        String thesaurusId = currentThesaurusId();
        return StringUtils.isNotBlank(thesaurusId)
                && rightsService.isAdminOfThesaurusProject(userSession, thesaurusId);
    }

    /**
     * Compteur du bouton, sans charger les trois listes.
     */
    public void refreshBadgeCount() {
        if (loaded) {
            badgeCount = pendingCount;
            return;
        }
        String thesaurusId = currentThesaurusId();
        if (!isBoardLinkVisible()) {
            badgeCount = 0;
            return;
        }
        try {
            badgeCount = candidatReadService.countByStatus(thesaurusId, CandidatStatusCode.PENDING);
        } catch (RuntimeException ex) {
            badgeCount = 0;
        }
    }

    public void openBoard() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || facesContext.getResponseComplete()) {
            return;
        }
        ExternalContext context = facesContext.getExternalContext();
        String id = currentThesaurusId();
        StringBuilder url = new StringBuilder(context.getRequestContextPath())
                .append("/candidat/candidats.xhtml");
        if (StringUtils.isNotBlank(id)) {
            url.append("?idt=").append(URLEncoder.encode(id, StandardCharsets.UTF_8));
        }
        try {
            context.redirect(url.toString());
        } catch (IOException ignored) {
            // La page courante reste affichée.
        }
    }

    private void redirectWithNotice(String message, boolean error) {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null || faces.getResponseComplete()) {
            return;
        }
        ExternalContext context = faces.getExternalContext();
        context.getSessionMap().put("candBoardNotice", StringUtils.defaultString(message));
        context.getSessionMap().put("candBoardNoticeError", error);
        String id = currentThesaurusId();
        StringBuilder url = new StringBuilder(context.getRequestContextPath())
                .append("/candidat/candidats.xhtml");
        if (StringUtils.isNotBlank(id)) {
            url.append("?idt=").append(URLEncoder.encode(id, StandardCharsets.UTF_8));
        }
        try {
            context.redirect(url.toString());
        } catch (IOException ignored) {
            notice = StringUtils.defaultString(message);
            noticeError = error;
            noticeToken = String.valueOf(System.currentTimeMillis());
        }
    }

    private void consumeBoardNotice() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            return;
        }
        var session = faces.getExternalContext().getSessionMap();
        Object message = session.remove("candBoardNotice");
        Object error = session.remove("candBoardNoticeError");
        if (message == null || StringUtils.isBlank(message.toString())) {
            return;
        }
        notice = message.toString().trim();
        noticeError = Boolean.TRUE.equals(error);
        noticeToken = String.valueOf(System.currentTimeMillis());
    }

    private String currentThesaurusId() {
        String fromView = thesaurusViewBean == null ? null : StringUtils.trimToNull(thesaurusViewBean.getId());
        if (fromView != null) {
            return fromView;
        }
        return StringUtils.trimToNull(thesaurusContext.resolveThesaurusId());
    }

    public void load(String thesaurusId) {
        loaded = false;
        reload(thesaurusId, thesaurusViewBean.getSelectedLang());
        loaded = true;
        badgeCount = pendingCount;
        consumeBoardNotice();
    }

    public void search() {
        load();
    }

    public void toggleMine() {
        load();
    }

    public boolean isCanProcess() {
        return candidatAccessPolicy.canProcess(userSession, thesaurusViewBean.getId());
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

    public void toggleSelected() {
        toggleSelect(selectId);
        selectId = null;
    }

    public boolean isSelected(String conceptId) {
        return StringUtils.isNotBlank(conceptId) && selectedIds.contains(conceptId);
    }

    public boolean isHasSelection() {
        return !selectedIds.isEmpty();
    }

    public boolean isHasPendingSelection() {
        return selectedIn(pending);
    }

    public boolean isHasRejectedSelection() {
        return selectedIn(rejected);
    }

    public void acceptSelected() {
        if (!isCanProcess()) {
            return;
        }
        List<CandidatDto> candidates = loadSelectedPending();
        if (candidates.isEmpty()) {
            redirectWithNotice(localeBean.getMsg("candidat.board.acceptSelected"), true);
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
            String detail = StringUtils.defaultIfBlank(failed.getNomPref(), failed.getIdConcepte());
            if (!candidatProcessService.hasDefinition(failed.getIdThesaurus(), failed.getIdConcepte())) {
                redirectWithNotice(localeBean.getMsg("candidat.validate.needDefinition")
                        + (StringUtils.isBlank(detail) ? "" : " : " + detail), true);
            } else {
                redirectWithNotice(localeBean.getMsg("candidat.insertError")
                        + " : " + detail, true);
            }
            return;
        }
        notifyCreatorsAccepted(candidates);
        selectedIds.clear();
        batchMessage = "";
        adminBatchMessage = "";
        redirectWithNotice(localeBean.getMsg("candidat.board.acceptOk"), false);
    }

    public void rejectSelected() {
        if (!isCanProcess()) {
            return;
        }
        List<CandidatDto> candidates = loadSelectedPending();
        if (candidates.isEmpty()) {
            redirectWithNotice(localeBean.getMsg("candidat.board.rejectSelected"), true);
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
            redirectWithNotice(localeBean.getMsg("candidat.rejectError")
                    + " : " + failed.getNomPref(), true);
            return;
        }
        notifyCreatorsRejected(candidates);
        selectedIds.clear();
        batchMessage = "";
        adminBatchMessage = "";
        redirectWithNotice(localeBean.getMsg("candidat.board.rejectOk"), false);
    }

    public void reopenSelected() {
        if (!isCanProcess()) {
            return;
        }
        List<CandidatDto> candidates = loadSelectedRejected();
        if (candidates.isEmpty()) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.board.reopenSelected"));
            return;
        }
        CandidatDto failed = candidatProcessService.reactivateCandidatesBatch(candidates);
        selectedIds.clear();
        if (failed == null) {
            activeTab = TAB_PENDING;
        }
        load(thesaurusViewBean.getId());
        if (failed != null) {
            String detail = StringUtils.defaultIfBlank(failed.getNomPref(), failed.getIdConcepte());
            MessageUtils.showErrorMessage(localeBean.getMsg("candidat.reactivateError")
                    + (StringUtils.isBlank(detail) ? "" : " : " + detail));
            return;
        }
        MessageUtils.showInformationMessage(localeBean.getMsg("candidat.reactivateOk"));
    }

    public void exportSelectedSkos() {
        exportCandidates();
    }

    public void exportPendingSkos() {
        exportCandidates();
    }

    public void exportCandidates() {
        if (!isCanExport()) {
            return;
        }
        List<CandidatDto> toExport = loadVisibleCandidates();
        if (toExport.isEmpty()) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.exportError"));
            return;
        }
        byte[] csv = candidatProcessService.exportProcessedCandidatesCsv(toExport);
        if (csv == null || csv.length == 0) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.exportError"));
            return;
        }
        try {
            FacesContext faces = FacesContext.getCurrentInstance();
            if (faces == null) {
                return;
            }
            HttpServletResponse response = (HttpServletResponse) faces.getExternalContext().getResponse();
            response.reset();
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"candidats.csv\"");
            try (OutputStream out = response.getOutputStream()) {
                out.write(csv);
                out.flush();
            }
            faces.responseComplete();
        } catch (Exception ex) {
            MessageUtils.showErrorMessage(
                    ex.getMessage() != null ? ex.getMessage() : localeBean.getMsg("candidat.exportError"));
        }
    }

    public void importSkos() {
        if (!isCanImport()) {
            return;
        }
        if (importUpload == null) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.board.import"));
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
                        !error.isEmpty() ? error.toString() : localeBean.getMsg("candidat.board.import"));
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
            MessageUtils.showInformationMessage(localeBean.getMsg("candidat.board.import"));
        } catch (Exception ex) {
            MessageUtils.showErrorMessage(
                    ex.getMessage() != null ? ex.getMessage() : localeBean.getMsg("candidat.board.import"));
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

    public boolean isCanCreate() {
        return candidatAccessPolicy.canCreate(userSession, thesaurusViewBean.getId());
    }

    public boolean isExportEnabled() {
        ensureLoaded();
        return switch (StringUtils.defaultString(activeTab)) {
            case TAB_ACCEPTED -> accepted != null && !accepted.isEmpty();
            case TAB_REJECTED -> rejected != null && !rejected.isEmpty();
            default -> pending != null && !pending.isEmpty();
        };
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
        if (StringUtils.isBlank(thesaurusId)
                || !rightsService.isAdminOfThesaurusProject(userSession, thesaurusId)) {
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
        String date = formatDate(Dates.toInstant(dto.getCreationDate()));
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

    private List<CandidatDto> loadVisibleCandidates() {
        String thesaurusId = thesaurusViewBean.getId();
        String lang = StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
        boolean pendingTab = !TAB_ACCEPTED.equals(activeTab) && !TAB_REJECTED.equals(activeTab);
        int status = switch (StringUtils.defaultString(activeTab)) {
            case TAB_ACCEPTED -> CandidatStatusCode.ACCEPTED;
            case TAB_REJECTED -> CandidatStatusCode.REJECTED;
            default -> CandidatStatusCode.PENDING;
        };
        List<CandidatDto> source = candidatReadService.searchByStatus(
                thesaurusId, lang, status, StringUtils.trimToNull(searchTerm));
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Integer userId = mineOnly && isLoggedIn() ? userSession.getCurrentUserId() : null;
        return source.stream()
                .filter(dto -> userId == null || dto.getCreatedById() == userId)
                .filter(dto -> !pendingTab || selectedIds.isEmpty() || selectedIds.contains(dto.getIdConcepte()))
                .toList();
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

    private List<CandidatDto> loadSelectedRejected() {
        if (selectedIds.isEmpty()) {
            return List.of();
        }
        String thesaurusId = thesaurusViewBean.getId();
        String lang = StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
        List<CandidatDto> rejectedCandidates = candidatReadService.loadByStatus(
                thesaurusId, lang, CandidatStatusCode.REJECTED);
        if (rejectedCandidates == null || rejectedCandidates.isEmpty()) {
            return List.of();
        }
        List<CandidatDto> selected = new ArrayList<>();
        for (CandidatDto dto : rejectedCandidates) {
            if (selectedIds.contains(dto.getIdConcepte())) {
                selected.add(dto);
            }
        }
        return selected;
    }

    private boolean selectedIn(List<CandidatBoardItem> items) {
        if (selectedIds.isEmpty() || items == null || items.isEmpty()) {
            return false;
        }
        for (CandidatBoardItem item : items) {
            if (item != null && selectedIds.contains(item.conceptId())) {
                return true;
            }
        }
        return false;
    }

    private void notifyCreatorsAccepted(List<CandidatDto> candidates) {
        var alertMails = candidatProcessService.resolveAlertMails(candidates);
        String message = resolveBatchMessage();
        String title = resolveThesaurusTitle();
        String baseUrl = resolveAppBaseUrl();
        for (CandidatDto candidate : candidates) {
            String mail = alertMails.get(candidate.getCreatedById());
            if (mail != null) {
                candidatProcessService.sendAcceptedMailLater(mail, candidate, message, title, baseUrl);
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
            if (mail != null) {
                candidatProcessService.sendRejectedMailLater(mail, candidate, message, title, baseUrl);
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
