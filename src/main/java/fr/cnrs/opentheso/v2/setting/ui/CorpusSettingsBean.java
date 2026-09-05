package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2CorpusSettingsBean")
public class CorpusSettingsBean implements Serializable {

    private final transient UserSession userSession;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ThesaurusAccessService thesaurusAccessService;
    private final transient ThesaurusCorpusService thesaurusCorpusService;

    private List<ThesaurusCorpus> corpusList = Collections.emptyList();
    private CorpusEditor corpusEditor = CorpusEditor.empty();
    private String editingCorpusName;

    public CorpusSettingsBean(
            UserSession userSession,
            ThesaurusContext thesaurusContext,
            ThesaurusAccessService thesaurusAccessService,
            ThesaurusCorpusService thesaurusCorpusService
    ) {
        this.userSession = userSession;
        this.thesaurusContext = thesaurusContext;
        this.thesaurusAccessService = thesaurusAccessService;
        this.thesaurusCorpusService = thesaurusCorpusService;
    }

    public void load() {
        thesaurusContext.syncFromViewParams();
        if (!canManage()) {
            corpusList = Collections.emptyList();
            corpusEditor = CorpusEditor.empty();
            editingCorpusName = null;
            return;
        }
        refreshList();
    }

    public void prepareCreate() {
        corpusEditor = CorpusEditor.empty();
        editingCorpusName = null;
    }

    public void prepareEdit(ThesaurusCorpus corpus) {
        corpusEditor = CorpusEditor.from(corpus);
        editingCorpusName = corpus.corpusName();
    }

    public void create() {
        if (!canManage()) {
            return;
        }
        try {
            thesaurusCorpusService.createCorpus(thesaurusContext.getCurrentThesaurusId(), corpusEditor.toModel());
            PrimeFaces.current().executeScript("PF('v2NewCorpus').hide();");
            refreshList();
            MessageUtils.showInformationMessage("Corpus créé avec succès");
        } catch (InvalidSettingDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void update() {
        if (!canManage() || editingCorpusName == null) {
            return;
        }
        try {
            thesaurusCorpusService.updateCorpus(
                    thesaurusContext.getCurrentThesaurusId(),
                    editingCorpusName,
                    corpusEditor.toModel()
            );
            PrimeFaces.current().executeScript("PF('v2EditCorpus').hide();");
            refreshList();
            MessageUtils.showInformationMessage("Corpus modifié avec succès");
        } catch (InvalidSettingDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void delete() {
        if (!canManage() || editingCorpusName == null) {
            return;
        }
        try {
            thesaurusCorpusService.deleteCorpus(thesaurusContext.getCurrentThesaurusId(), editingCorpusName);
            PrimeFaces.current().executeScript("PF('v2ConfirmDeleteCorpus').hide();");
            refreshList();
            MessageUtils.showInformationMessage("Corpus supprimé avec succès");
        } catch (InvalidSettingDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public boolean isScreenAvailable() {
        return canManage();
    }

    private void refreshList() {
        corpusList = thesaurusCorpusService.listCorpus(thesaurusContext.getCurrentThesaurusId());
        PrimeFaces.current().ajax().update("containerIndex", "menuBar");
    }

    private boolean canManage() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || thesaurusContext.getCurrentThesaurusId() == null) {
            return false;
        }
        return thesaurusAccessService.canManageThesaurus(
                userId,
                userSession.isSuperAdmin(),
                thesaurusContext.getCurrentThesaurusId()
        );
    }
}
