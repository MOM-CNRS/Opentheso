package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;
import fr.cnrs.opentheso.v2.concept.alignment.service.ConceptAlignmentAdminService;
import fr.cnrs.opentheso.v2.setting.service.AlignmentPersistDraft;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Named("v2ThesaurusAlignmentBean")
@ViewScoped
@RequiredArgsConstructor
public class ThesaurusAlignmentBean implements Serializable {

    private static final String MODE_CREATE = "create";

    private final transient SettingsAccess settingsAccess;
    private final transient ConceptAlignmentAdminService conceptAlignmentAdminService;

    private List<AlignmentSourceItem> alignmentSources = new ArrayList<>();
    private List<AlignmentSourceItem> alignmentBaseline = new ArrayList<>();
    private final Set<Integer> alignmentIdsToDelete = new HashSet<>();
    private final Map<Integer, String> draftAlignmentRemoteThesaurusIds = new HashMap<>();
    private int nextDraftAlignmentId = -1;
    private boolean alignmentSourcesLoaded;
    private String alignmentSourcesLoadedForThesaurus;
    private final SettingsPager alignmentPager = new SettingsPager();
    private final List<AlignmentSourceItem> pagedAlignmentSources = new ArrayList<>();
    private final List<Integer> alignmentPages = new ArrayList<>();

    @Getter
    @Setter
    private String alignmentActionTarget;
    @Getter
    @Setter
    private String alignmentPageTarget;
    @Getter
    @Setter
    private String newAlignmentSourceName = "";
    @Getter
    @Setter
    private String newAlignmentSourceType = "";
    @Getter
    @Setter
    private String newAlignmentSourceUri = "";
    @Getter
    @Setter
    private boolean newAlignmentSourceSelected = true;
    @Getter
    @Setter
    private String newAlignmentSourceThesaurusId = "";
    @Getter
    @Setter
    private String newAlignmentSourceDescription = "";
    @Getter
    private boolean alignmentDialogOpen;
    @Getter
    private String alignmentMessage;
    @Getter
    private boolean alignmentError;
    @Getter
    private int editingAlignmentSourceId;
    @Getter
    private String alignmentDialogMode;

    public void load() {
        alignmentSourcesLoaded = false;
        refreshAlignmentSources();
    }

    public AlignmentPersistDraft toPersistDraft() {
        ensureAlignmentSourcesLoaded();
        return new AlignmentPersistDraft(
                copySources(alignmentSources),
                copySources(alignmentBaseline),
                new HashSet<>(alignmentIdsToDelete)
        );
    }

    public List<AlignmentSourceItem> getAlignmentSources() {
        ensureAlignmentSourcesLoaded();
        return alignmentSources != null ? alignmentSources : Collections.emptyList();
    }

    public List<AlignmentSourceItem> getPagedAlignmentSources() {
        ensureAlignmentSourcesLoaded();
        return pagedAlignmentSources;
    }

    public boolean isAlignmentPagerNeeded() {
        return alignmentPager.needed(getAlignmentSources().size());
    }

    public int getAlignmentPage() {
        return alignmentPager.getPage();
    }

    public int getAlignmentPageFrom() {
        return alignmentPager.from(getAlignmentSources().size());
    }

    public int getAlignmentPageTo() {
        return alignmentPager.to(getAlignmentSources().size());
    }

    public List<Integer> getAlignmentPages() {
        ensureAlignmentSourcesLoaded();
        return alignmentPages;
    }

    public void prevAlignmentPage() {
        alignmentPager.prev();
        rebuildAlignmentPage();
    }

    public void nextAlignmentPage() {
        alignmentPager.next(getAlignmentSources().size());
        rebuildAlignmentPage();
    }

    public void goAlignmentPage() {
        int page = RequestParams.parsePositiveInt(RequestParams.param("previewAlignPage"));
        if (page <= 0) {
            page = RequestParams.parsePositiveInt(alignmentPageTarget);
        }
        goAlignmentPage(page);
    }

    public void goAlignmentPage(int page) {
        alignmentPager.go(page, getAlignmentSources().size());
        rebuildAlignmentPage();
    }

    public void toggleAlignmentSource() {
        toggleAlignmentSource(requestAlignmentSourceId());
    }

    public void toggleAlignmentSource(int sourceId) {
        if (!settingsAccess.isCanEdit() || sourceId == 0) {
            return;
        }
        AlignmentSourceItem item = findAlignmentSource(sourceId);
        if (item == null) {
            return;
        }
        item.setSelected(!item.isSelected());
        rebuildAlignmentPage();
    }

    public boolean isAlignmentFormDialog() {
        return alignmentDialogOpen && (MODE_CREATE.equals(alignmentDialogMode) || "edit".equals(alignmentDialogMode));
    }

    public boolean isAlignmentCreateDialog() {
        return alignmentDialogOpen && MODE_CREATE.equals(alignmentDialogMode);
    }

    public boolean isAlignmentEditDialog() {
        return alignmentDialogOpen && "edit".equals(alignmentDialogMode);
    }

    public boolean isAlignmentDeleteDialog() {
        return alignmentDialogOpen && "delete".equals(alignmentDialogMode);
    }

    public boolean isAlignmentOpenthesoType() {
        return StringUtils.isBlank(newAlignmentSourceType)
                || "opentheso".equalsIgnoreCase(newAlignmentSourceType.trim());
    }

    public boolean isAlignmentRemoteThesaurusVisible() {
        return isAlignmentCreateDialog() && isAlignmentOpenthesoType();
    }

    public boolean canManageAlignmentSource(AlignmentSourceItem item) {
        if (!settingsAccess.isCanEdit() || item == null) {
            return false;
        }
        if (settingsAccess.isSuperAdmin() || !item.isGlobal()) {
            return true;
        }
        return StringUtils.equals(item.getThesaurusOwner(), settingsAccess.getThesaurusId());
    }

    public void prepareCreateAlignmentSource() {
        if (!settingsAccess.isCanEdit()) {
            alignmentError = true;
            alignmentMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        resetAlignmentSourceEditor();
        newAlignmentSourceType = "Opentheso";
        newAlignmentSourceSelected = true;
        alignmentDialogMode = MODE_CREATE;
        editingAlignmentSourceId = 0;
        alignmentDialogOpen = true;
        alignmentMessage = null;
        alignmentError = false;
    }

    public void prepareEditAlignmentSource() {
        prepareEditAlignmentSource(findAlignmentSource(requestAlignmentSourceId()));
    }

    public void prepareEditAlignmentSource(AlignmentSourceItem item) {
        if (!canManageAlignmentSource(item)) {
            return;
        }
        newAlignmentSourceName = StringUtils.defaultString(item.getLabel());
        newAlignmentSourceType = StringUtils.defaultString(item.getSourceType());
        newAlignmentSourceUri = StringUtils.defaultString(item.getUrl());
        newAlignmentSourceSelected = item.isSelected();
        newAlignmentSourceDescription = StringUtils.defaultString(item.getDescription());
        newAlignmentSourceThesaurusId = StringUtils.defaultString(
                draftAlignmentRemoteThesaurusIds.get(item.getSourceId()));
        editingAlignmentSourceId = item.getSourceId();
        alignmentDialogMode = "edit";
        alignmentDialogOpen = true;
        alignmentMessage = null;
        alignmentError = false;
    }

    public void prepareDeleteAlignmentSource() {
        prepareDeleteAlignmentSource(findAlignmentSource(requestAlignmentSourceId()));
    }

    public void prepareDeleteAlignmentSource(AlignmentSourceItem item) {
        if (!canManageAlignmentSource(item)) {
            return;
        }
        newAlignmentSourceName = StringUtils.defaultString(item.getLabel());
        editingAlignmentSourceId = item.getSourceId();
        alignmentDialogMode = "delete";
        alignmentDialogOpen = true;
        alignmentMessage = null;
        alignmentError = false;
    }

    public void cancelAlignmentSourceDialog() {
        closeAlignmentSourceDialog();
        if (alignmentError) {
            alignmentMessage = null;
            alignmentError = false;
        }
    }

    public void createAlignmentSource() {
        if (!settingsAccess.isCanEdit()) {
            denyAlignmentSourceAction();
            return;
        }
        String type = StringUtils.defaultIfBlank(newAlignmentSourceType, "Opentheso").trim();
        String url;
        if (isAlignmentOpenthesoType()) {
            String error = conceptAlignmentAdminService.validateOpenthesoSource(
                    newAlignmentSourceName,
                    newAlignmentSourceUri,
                    newAlignmentSourceThesaurusId
            );
            if (error != null) {
                alignmentError = true;
                alignmentMessage = error;
                return;
            }
            url = openthesoSearchUrl(newAlignmentSourceUri, newAlignmentSourceThesaurusId);
        } else if (StringUtils.isBlank(newAlignmentSourceName) || StringUtils.isBlank(newAlignmentSourceUri)) {
            alignmentError = true;
            alignmentMessage = StringUtils.isBlank(newAlignmentSourceName)
                    ? "Le nom de la source est obligatoire !"
                    : "L'URL est obligatoire !";
            return;
        } else {
            url = newAlignmentSourceUri.trim();
        }
        ensureAlignmentSourcesLoaded();
        int draftId = nextDraftAlignmentId--;
        AlignmentSourceItem item = new AlignmentSourceItem(
                draftId,
                newAlignmentSourceName.trim(),
                StringUtils.defaultString(newAlignmentSourceDescription).trim(),
                newAlignmentSourceSelected,
                false,
                type,
                url,
                settingsAccess.getThesaurusId()
        );
        if (isAlignmentOpenthesoType()) {
            draftAlignmentRemoteThesaurusIds.put(draftId, newAlignmentSourceThesaurusId.trim());
        }
        alignmentSources.add(item);
        closeAlignmentSourceDialog();
        alignmentPager.go(alignmentPager.pageCount(alignmentSources.size()), alignmentSources.size());
        rebuildAlignmentPage();
        alignmentError = false;
        alignmentMessage = "Source ajoutée — cliquez sur Enregistrer pour conserver";
    }

    public void updateAlignmentSource() {
        if (!settingsAccess.isCanEdit() || editingAlignmentSourceId == 0) {
            return;
        }
        AlignmentSourceItem item = findAlignmentSource(editingAlignmentSourceId);
        if (item == null || !canManageAlignmentSource(item)) {
            return;
        }
        if (StringUtils.isBlank(newAlignmentSourceName) || StringUtils.isBlank(newAlignmentSourceUri)) {
            alignmentError = true;
            alignmentMessage = StringUtils.isBlank(newAlignmentSourceName)
                    ? "Le nom de la source est obligatoire !"
                    : "L'URL est obligatoire !";
            return;
        }
        item.setLabel(newAlignmentSourceName.trim());
        item.setSourceType(StringUtils.defaultIfBlank(newAlignmentSourceType, item.getSourceType()).trim());
        item.setUrl(newAlignmentSourceUri.trim());
        item.setSelected(newAlignmentSourceSelected);
        item.setDescription(StringUtils.defaultString(newAlignmentSourceDescription).trim());
        closeAlignmentSourceDialog();
        rebuildAlignmentPage();
        alignmentError = false;
        alignmentMessage = "Source modifiée — cliquez sur Enregistrer pour conserver";
    }

    public void deleteAlignmentSource() {
        if (!settingsAccess.isCanEdit() || editingAlignmentSourceId == 0) {
            return;
        }
        AlignmentSourceItem item = findAlignmentSource(editingAlignmentSourceId);
        if (item == null || !canManageAlignmentSource(item)) {
            return;
        }
        alignmentSources.remove(item);
        draftAlignmentRemoteThesaurusIds.remove(item.getSourceId());
        if (item.getSourceId() > 0) {
            alignmentIdsToDelete.add(item.getSourceId());
        }
        closeAlignmentSourceDialog();
        rebuildAlignmentPage();
        alignmentError = false;
        alignmentMessage = "Source supprimée — cliquez sur Enregistrer pour conserver";
    }

    private void ensureAlignmentSourcesLoaded() {
        String thesaurusId = settingsAccess.getThesaurusId();
        if (alignmentSourcesLoaded
                && StringUtils.equals(alignmentSourcesLoadedForThesaurus, thesaurusId)
                && alignmentSources != null) {
            return;
        }
        refreshAlignmentSources();
    }

    private void refreshAlignmentSources() {
        String thesaurusId = settingsAccess.getThesaurusId();
        alignmentSourcesLoaded = true;
        alignmentSourcesLoadedForThesaurus = thesaurusId;
        if (StringUtils.isBlank(thesaurusId)) {
            alignmentSources = new ArrayList<>();
            snapshotAlignmentBaseline();
            rebuildAlignmentPage();
            return;
        }
        List<AlignmentSourceItem> loaded = conceptAlignmentAdminService.listSourcesForManagement(thesaurusId);
        alignmentSources = loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        snapshotAlignmentBaseline();
        rebuildAlignmentPage();
    }

    private void rebuildAlignmentPage() {
        List<AlignmentSourceItem> all = alignmentSources != null ? alignmentSources : List.of();
        pagedAlignmentSources.clear();
        pagedAlignmentSources.addAll(alignmentPager.slice(all));
        alignmentPages.clear();
        alignmentPages.addAll(alignmentPager.pages(all.size()));
    }

    private int requestAlignmentSourceId() {
        int sourceId = RequestParams.intParam("previewAlignSourceId");
        return sourceId != 0 ? sourceId : RequestParams.parseInt(alignmentActionTarget);
    }

    private AlignmentSourceItem findAlignmentSource(int sourceId) {
        if (sourceId == 0) {
            return null;
        }
        ensureAlignmentSourcesLoaded();
        if (alignmentSources == null) {
            return null;
        }
        return alignmentSources.stream()
                .filter(item -> item.getSourceId() == sourceId)
                .findFirst()
                .orElse(null);
    }

    private void closeAlignmentSourceDialog() {
        alignmentDialogOpen = false;
        alignmentDialogMode = null;
        editingAlignmentSourceId = 0;
        resetAlignmentSourceEditor();
    }

    private void resetAlignmentSourceEditor() {
        newAlignmentSourceName = "";
        newAlignmentSourceType = "";
        newAlignmentSourceUri = "";
        newAlignmentSourceSelected = true;
        newAlignmentSourceThesaurusId = "";
        newAlignmentSourceDescription = "";
    }

    private String openthesoSearchUrl(String sourceUri, String sourceThesaurusId) {
        return normalizeDraftUri(sourceUri)
                + "/api/search?q=##value##&lang=##lang##&theso="
                + sourceThesaurusId.trim();
    }

    private String normalizeDraftUri(String sourceUri) {
        String uri = StringUtils.trimToEmpty(sourceUri);
        if (uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    private void snapshotAlignmentBaseline() {
        alignmentBaseline = copySources(alignmentSources);
        alignmentIdsToDelete.clear();
        draftAlignmentRemoteThesaurusIds.clear();
        nextDraftAlignmentId = -1;
    }

    private List<AlignmentSourceItem> copySources(List<AlignmentSourceItem> sources) {
        List<AlignmentSourceItem> copy = new ArrayList<>();
        if (sources == null) {
            return copy;
        }
        for (AlignmentSourceItem item : sources) {
            copy.add(new AlignmentSourceItem(
                    item.getSourceId(),
                    item.getLabel(),
                    item.getDescription(),
                    item.isSelected(),
                    item.isGlobal(),
                    item.getSourceType(),
                    item.getUrl(),
                    item.getThesaurusOwner()
            ));
        }
        return copy;
    }

    private void denyAlignmentSourceAction() {
        alignmentError = true;
        alignmentMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
    }
}
