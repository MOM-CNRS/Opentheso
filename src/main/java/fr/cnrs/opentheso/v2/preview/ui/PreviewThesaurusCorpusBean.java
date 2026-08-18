package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.preview.service.CorpusPersistDraft;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.setting.ui.CorpusEditor;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named("v2PreviewCorpusBean")
@ViewScoped
@RequiredArgsConstructor
public class PreviewThesaurusCorpusBean implements Serializable {

    private final PreviewSettingsAccess settingsAccess;
    private final ThesaurusCorpusService thesaurusCorpusService;

    private List<ThesaurusCorpus> corpusList = new ArrayList<>();
    private List<ThesaurusCorpus> corpusBaseline = new ArrayList<>();
    private final Map<String, String> corpusOriginalNameByCurrent = new LinkedHashMap<>();
    private boolean corpusLoaded;
    private String corpusLoadedForThesaurus;
    private final PreviewTablePager corpusPager = new PreviewTablePager();
    private final List<ThesaurusCorpus> pagedCorpusList = new ArrayList<>();
    private final List<Integer> corpusPages = new ArrayList<>();

    @Getter
    @Setter
    private String corpusActionTarget;
    @Getter
    @Setter
    private String corpusPageTarget;
    @Getter
    @Setter
    private CorpusEditor corpusEditor = CorpusEditor.empty();
    @Getter
    private String editingCorpusName;
    @Getter
    private boolean corpusDialogOpen;
    @Getter
    private String corpusDialogMode;
    @Getter
    private String corpusMessage;
    @Getter
    private boolean corpusError;

    public void load() {
        corpusLoaded = false;
        refreshCorpusList();
    }

    public CorpusPersistDraft toPersistDraft() {
        ensureCorpusListLoaded();
        return new CorpusPersistDraft(
                new ArrayList<>(corpusList != null ? corpusList : List.of()),
                new ArrayList<>(corpusBaseline),
                new LinkedHashMap<>(corpusOriginalNameByCurrent)
        );
    }

    public List<ThesaurusCorpus> getCorpusList() {
        ensureCorpusListLoaded();
        return corpusList != null ? corpusList : Collections.emptyList();
    }

    public List<ThesaurusCorpus> getPagedCorpusList() {
        ensureCorpusListLoaded();
        return pagedCorpusList;
    }

    public boolean isCorpusPagerNeeded() {
        return corpusPager.needed(getCorpusList().size());
    }

    public int getCorpusPage() {
        return corpusPager.getPage();
    }

    public int getCorpusPageFrom() {
        return corpusPager.from(getCorpusList().size());
    }

    public int getCorpusPageTo() {
        return corpusPager.to(getCorpusList().size());
    }

    public List<Integer> getCorpusPages() {
        ensureCorpusListLoaded();
        return corpusPages;
    }

    public void prevCorpusPage() {
        corpusPager.prev();
        rebuildCorpusPage();
    }

    public void nextCorpusPage() {
        corpusPager.next(getCorpusList().size());
        rebuildCorpusPage();
    }

    public void goCorpusPage() {
        int page = PreviewRequestParams.parsePositiveInt(PreviewRequestParams.param("previewCorpusPage"));
        if (page <= 0) {
            page = PreviewRequestParams.parsePositiveInt(corpusPageTarget);
        }
        goCorpusPage(page);
    }

    public void goCorpusPage(int page) {
        corpusPager.go(page, getCorpusList().size());
        rebuildCorpusPage();
    }

    public void toggleCorpusActive() {
        toggleCorpusActive(requestCorpusTarget());
    }

    public void toggleCorpusActive(String corpusName) {
        if (!settingsAccess.isCanEdit()) {
            return;
        }
        ThesaurusCorpus corpus = findCorpus(corpusName);
        if (corpus == null) {
            return;
        }
        replaceCorpus(corpus.corpusName(), new ThesaurusCorpus(
                corpus.corpusName(),
                corpus.uriLink(),
                corpus.uriCount(),
                !corpus.active(),
                corpus.onlyUriLink(),
                corpus.omekaS(),
                corpus.sort()
        ), corpusOriginalNameByCurrent.get(corpus.corpusName()));
        rebuildCorpusPage();
    }

    public void prepareCreateCorpus() {
        if (!settingsAccess.isCanEdit()) {
            corpusError = true;
            corpusMessage = "Action non autorisée";
            return;
        }
        corpusEditor = CorpusEditor.empty();
        editingCorpusName = null;
        corpusDialogMode = "create";
        corpusDialogOpen = true;
        corpusMessage = null;
        corpusError = false;
    }

    public void prepareEditCorpus() {
        prepareEditCorpus(findCorpus(requestCorpusTarget()));
    }

    public void prepareEditCorpus(ThesaurusCorpus corpus) {
        if (!settingsAccess.isCanEdit() || corpus == null) {
            return;
        }
        corpusEditor = CorpusEditor.from(corpus);
        editingCorpusName = corpus.corpusName();
        corpusDialogMode = "edit";
        corpusDialogOpen = true;
        corpusMessage = null;
        corpusError = false;
    }

    public void prepareDeleteCorpus() {
        prepareDeleteCorpus(findCorpus(requestCorpusTarget()));
    }

    public void prepareDeleteCorpus(ThesaurusCorpus corpus) {
        if (!settingsAccess.isCanEdit() || corpus == null) {
            return;
        }
        corpusEditor = CorpusEditor.from(corpus);
        editingCorpusName = corpus.corpusName();
        corpusDialogMode = "delete";
        corpusDialogOpen = true;
        corpusMessage = null;
        corpusError = false;
    }

    public void cancelCorpusDialog() {
        closeCorpusDialog();
        if (corpusError) {
            corpusMessage = null;
            corpusError = false;
        }
    }

    public void createCorpus() {
        if (!settingsAccess.isCanEdit()) {
            denyCorpusAction();
            return;
        }
        try {
            ThesaurusCorpus created = corpusEditor.toModel();
            validateDraftCorpus(created);
            ensureCorpusListLoaded();
            ensureUniqueCorpusName(created.corpusName(), null);
            ensureCorpusListMutable();
            corpusList.add(created);
            closeCorpusDialog();
            corpusPager.go(corpusPager.pageCount(corpusList.size()), corpusList.size());
            rebuildCorpusPage();
            corpusError = false;
            corpusMessage = "Corpus ajouté — cliquez sur Enregistrer pour conserver";
        } catch (InvalidSettingDataException e) {
            corpusError = true;
            corpusMessage = e.getMessage();
        }
    }

    public void updateCorpus() {
        if (!settingsAccess.isCanEdit()) {
            denyCorpusAction();
            return;
        }
        if (editingCorpusName == null) {
            return;
        }
        try {
            ThesaurusCorpus updated = corpusEditor.toModel();
            validateDraftCorpus(updated);
            ensureCorpusListLoaded();
            ensureUniqueCorpusName(updated.corpusName(), editingCorpusName);
            String originalName = corpusOriginalNameByCurrent.getOrDefault(editingCorpusName, null);
            replaceCorpus(editingCorpusName, updated, originalName);
            closeCorpusDialog();
            rebuildCorpusPage();
            corpusError = false;
            corpusMessage = "Corpus modifié — cliquez sur Enregistrer pour conserver";
        } catch (InvalidSettingDataException e) {
            corpusError = true;
            corpusMessage = e.getMessage();
        }
    }

    public void deleteCorpus() {
        if (!settingsAccess.isCanEdit()) {
            denyCorpusAction();
            return;
        }
        if (editingCorpusName == null) {
            return;
        }
        ensureCorpusListLoaded();
        ensureCorpusListMutable();
        corpusList.removeIf(item -> editingCorpusName.equals(item.corpusName()));
        corpusOriginalNameByCurrent.remove(editingCorpusName);
        closeCorpusDialog();
        rebuildCorpusPage();
        corpusError = false;
        corpusMessage = "Corpus supprimé — cliquez sur Enregistrer pour conserver";
    }

    public boolean isCorpusFormDialog() {
        return corpusDialogOpen && ("create".equals(corpusDialogMode) || "edit".equals(corpusDialogMode));
    }

    public boolean isCorpusCreateDialog() {
        return corpusDialogOpen && "create".equals(corpusDialogMode);
    }

    public boolean isCorpusDeleteDialog() {
        return corpusDialogOpen && "delete".equals(corpusDialogMode);
    }

    private void ensureCorpusListLoaded() {
        String thesaurusId = settingsAccess.getThesaurusId();
        if (corpusLoaded && StringUtils.equals(corpusLoadedForThesaurus, thesaurusId) && corpusList != null) {
            return;
        }
        refreshCorpusList();
    }

    private void refreshCorpusList() {
        String thesaurusId = settingsAccess.getThesaurusId();
        corpusLoaded = true;
        corpusLoadedForThesaurus = thesaurusId;
        if (StringUtils.isBlank(thesaurusId)) {
            corpusList = new ArrayList<>();
            snapshotCorpusBaseline();
            rebuildCorpusPage();
            return;
        }
        List<ThesaurusCorpus> loaded = thesaurusCorpusService.listCorpus(thesaurusId);
        corpusList = loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        snapshotCorpusBaseline();
        rebuildCorpusPage();
    }

    private void rebuildCorpusPage() {
        List<ThesaurusCorpus> all = corpusList != null ? corpusList : List.of();
        pagedCorpusList.clear();
        pagedCorpusList.addAll(corpusPager.slice(all));
        corpusPages.clear();
        corpusPages.addAll(corpusPager.pages(all.size()));
    }

    private String requestCorpusTarget() {
        String raw = PreviewRequestParams.param("previewCorpusTarget");
        if (StringUtils.isNotBlank(raw)) {
            return raw;
        }
        return corpusActionTarget;
    }

    private ThesaurusCorpus findCorpus(String corpusName) {
        if (StringUtils.isBlank(corpusName)) {
            return null;
        }
        ensureCorpusListLoaded();
        if (corpusList == null) {
            return null;
        }
        return corpusList.stream()
                .filter(item -> corpusName.equals(item.corpusName()))
                .findFirst()
                .orElse(null);
    }

    private void closeCorpusDialog() {
        corpusDialogOpen = false;
        corpusDialogMode = null;
        corpusEditor = CorpusEditor.empty();
        editingCorpusName = null;
    }

    private void denyCorpusAction() {
        corpusError = true;
        corpusMessage = "Action non autorisée";
    }

    private void snapshotCorpusBaseline() {
        corpusBaseline = new ArrayList<>(corpusList != null ? corpusList : List.of());
        corpusOriginalNameByCurrent.clear();
        for (ThesaurusCorpus corpus : corpusBaseline) {
            corpusOriginalNameByCurrent.put(corpus.corpusName(), corpus.corpusName());
        }
    }

    private void ensureCorpusListMutable() {
        if (corpusList == null || !(corpusList instanceof ArrayList)) {
            corpusList = new ArrayList<>(corpusList != null ? corpusList : List.of());
        }
    }

    private void validateDraftCorpus(ThesaurusCorpus corpus) {
        if (corpus == null || StringUtils.isBlank(corpus.corpusName())) {
            throw new InvalidSettingDataException("Le nom du corpus est obligatoire.");
        }
        if (StringUtils.isBlank(corpus.uriLink())) {
            throw new InvalidSettingDataException("L'URI du lien est obligatoire.");
        }
        if (!corpus.onlyUriLink() && StringUtils.isBlank(corpus.uriCount())) {
            throw new InvalidSettingDataException("L'URI pour le comptage est obligatoire.");
        }
    }

    private void ensureUniqueCorpusName(String name, String currentName) {
        boolean duplicate = corpusList != null && corpusList.stream()
                .anyMatch(item -> name.equalsIgnoreCase(item.corpusName())
                        && (currentName == null || !currentName.equals(item.corpusName())));
        if (duplicate) {
            throw new InvalidSettingDataException("Ce corpus existe déjà.");
        }
    }

    private void replaceCorpus(String currentName, ThesaurusCorpus updated, String originalName) {
        ensureCorpusListMutable();
        for (int i = 0; i < corpusList.size(); i++) {
            if (currentName.equals(corpusList.get(i).corpusName())) {
                corpusList.set(i, updated);
                break;
            }
        }
        corpusOriginalNameByCurrent.remove(currentName);
        if (StringUtils.isNotBlank(originalName)) {
            corpusOriginalNameByCurrent.put(updated.corpusName(), originalName);
        }
    }
}
