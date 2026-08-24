package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.candidat.model.CandidatBoardItem;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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

    private final ThesaurusViewBean thesaurusViewBean;
    private final CandidatReadService candidatReadService;
    private final UserSession userSession;
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
        loaded = false;
        ensureLoaded();
    }

    public void toggleMine() {
        loaded = false;
        ensureLoaded();
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
        String date = formatDate(dto.getCreationDate());
        String meta = StringUtils.isBlank(date) ? author : author + " · " + date;
        int up = Math.max(0, dto.getNbrVote());
        int down = Math.max(0, dto.getNbrNoteVote());
        return new CandidatBoardItem(
                dto.getIdConcepte(),
                title,
                meta,
                "+" + up + " −" + down,
                openType
        );
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
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
