package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusSummary;
import fr.cnrs.opentheso.v2.toolbox.export.ui.ThesaurusExportBean;
import fr.cnrs.opentheso.v2.toolbox.model.EditionView;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
@ViewScoped
@Named("v2EditionBean")
public class EditionBean implements Serializable {

    private final UserSession userSession;
    private final ThesaurusContext thesaurusContext;
    private final V2LocaleBean localeBean;
    private final EditionThesaurusService editionThesaurusService;

    private EditionView currentView = EditionView.LIST;
    private List<EditionThesaurusSummary> thesaurusList = Collections.emptyList();
    private List<EditionThesaurusSummary> filteredThesaurusList;

    private String thesaurusIdToDelete;
    private String thesaurusTitleToDelete;
    private boolean deletePerennialIdentifiers;

    private final NewThesaurusBean newThesaurusBean;
    private final ModifyThesaurusBean modifyThesaurusBean;
    private final ThesaurusExportBean thesaurusExportBean;

    private EditionThesaurusSummary selectedThesaurusForAction;

    public EditionBean(
            UserSession userSession,
            ThesaurusContext thesaurusContext,
            V2LocaleBean localeBean,
            EditionThesaurusService editionThesaurusService,
            NewThesaurusBean newThesaurusBean,
            ModifyThesaurusBean modifyThesaurusBean,
            ThesaurusExportBean thesaurusExportBean
    ) {
        this.userSession = userSession;
        this.thesaurusContext = thesaurusContext;
        this.localeBean = localeBean;
        this.editionThesaurusService = editionThesaurusService;
        this.newThesaurusBean = newThesaurusBean;
        this.modifyThesaurusBean = modifyThesaurusBean;
        this.thesaurusExportBean = thesaurusExportBean;
    }

    public void load() {
        thesaurusContext.syncFromViewParams();
        resetToListView();
        if (!canAccessScreen()) {
            thesaurusList = Collections.emptyList();
            filteredThesaurusList = null;
            return;
        }
        refreshThesaurusList();
    }

    public boolean isScreenAvailable() {
        return canAccessScreen();
    }

    public boolean isCanCreateOrImport() {
        return ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession);
    }

    public boolean isListView() {
        return currentView == EditionView.LIST;
    }

    public boolean isNewView() {
        return currentView == EditionView.NEW;
    }

    public boolean isModifyView() {
        return currentView == EditionView.MODIFY;
    }

    public boolean isImportSkosView() {
        return currentView == EditionView.IMPORT_SKOS;
    }

    public boolean isImportCsvView() {
        return currentView == EditionView.IMPORT_CSV;
    }

    public boolean isImportCsvStructureView() {
        return currentView == EditionView.IMPORT_CSV_STRUCTURE;
    }

    public boolean isExportView() {
        return currentView == EditionView.EXPORT_SKOS
                || currentView == EditionView.EXPORT_PDF
                || currentView == EditionView.EXPORT_CSV
                || currentView == EditionView.EXPORT_CSV_ID
                || currentView == EditionView.EXPORT_CSV_STRUCTURE
                || currentView == EditionView.EXPORT_DEPRECATED;
    }

    public boolean isExportSkosView() {
        return currentView == EditionView.EXPORT_SKOS;
    }

    public boolean isExportCsvView() {
        return currentView == EditionView.EXPORT_CSV;
    }

    public boolean isExportCsvIdView() {
        return currentView == EditionView.EXPORT_CSV_ID;
    }

    public boolean isExportCsvStructureView() {
        return currentView == EditionView.EXPORT_CSV_STRUCTURE;
    }

    public boolean isExportPdfView() {
        return currentView == EditionView.EXPORT_PDF;
    }

    public boolean isExportDeprecatedView() {
        return currentView == EditionView.EXPORT_DEPRECATED;
    }

    public void showList() {
        resetToListView();
        if (canAccessScreen()) {
            refreshThesaurusList();
        }
    }

    public void showNewThesaurus() {
        if (!isCanCreateOrImport()) {
            return;
        }
        newThesaurusBean.prepareForm();
        currentView = EditionView.NEW;
    }

    public void showImportSkos() {
        navigateIfCanCreate(EditionView.IMPORT_SKOS);
    }

    public void showImportCsv() {
        navigateIfCanCreate(EditionView.IMPORT_CSV);
    }

    public void showImportCsvStructure() {
        navigateIfCanCreate(EditionView.IMPORT_CSV_STRUCTURE);
    }

    public void showModifyThesaurus(EditionThesaurusSummary thesaurus) {
        if (!canAccessScreen()) {
            return;
        }
        selectedThesaurusForAction = thesaurus;
        modifyThesaurusBean.load(thesaurus.id());
        currentView = EditionView.MODIFY;
    }

    public void showExport(EditionThesaurusSummary thesaurus, EditionView exportView) {
        selectedThesaurusForAction = thesaurus;
        currentView = exportView;
        if (thesaurus == null) {
            return;
        }
        if (exportView == EditionView.EXPORT_SKOS) {
            thesaurusExportBean.init(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_CSV) {
            thesaurusExportBean.initCsv(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_CSV_ID) {
            thesaurusExportBean.initCsvById(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_CSV_STRUCTURE) {
            thesaurusExportBean.initCsvStructured(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_PDF) {
            thesaurusExportBean.initPdf(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_DEPRECATED) {
            thesaurusExportBean.initDeprecated(thesaurus.id(), thesaurus.title());
        }
    }

    public void showExport(EditionThesaurusSummary thesaurus, String exportViewName) {
        showExport(thesaurus, EditionView.valueOf(exportViewName));
    }

    public void prepareDelete(EditionThesaurusSummary thesaurus) {
        thesaurusIdToDelete = thesaurus.id();
        thesaurusTitleToDelete = thesaurus.title();
        deletePerennialIdentifiers = false;
    }

    public void deleteThesaurus() {
        if (!canAccessScreen() || StringUtils.isBlank(thesaurusIdToDelete)) {
            return;
        }
        try {
            editionThesaurusService.deleteThesaurus(thesaurusIdToDelete, deletePerennialIdentifiers);
            if (thesaurusContext.matchesCurrentThesaurus(thesaurusIdToDelete)) {
                thesaurusContext.clearSelection();
            }
            thesaurusIdToDelete = null;
            thesaurusTitleToDelete = null;
            deletePerennialIdentifiers = false;
            PrimeFaces.current().executeScript("PF('v2ConfirmDeleteThesaurus').hide();");
            refreshThesaurusList();
            MessageUtils.showInformationMessage("Thesaurus supprimé avec succès");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void showThesaurusStatistics(EditionThesaurusSummary thesaurus) {
        var stats = editionThesaurusService.loadStatistics(thesaurus.id());
        var message = new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                localeBean.getMsg("info"),
                localeBean.getMsg("candidat.total_concepts") + " = " + stats.conceptCount() + "\n"
                        + localeBean.getMsg("candidat.titre") + " = " + stats.candidateCount() + "\n"
                        + localeBean.getMsg("search.deprecated") + " = " + stats.deprecatedCount()
        );
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    private void navigateIfCanCreate(EditionView view) {
        if (!isCanCreateOrImport()) {
            return;
        }
        currentView = view;
    }

    private void resetToListView() {
        currentView = EditionView.LIST;
        selectedThesaurusForAction = null;
    }

    private void refreshThesaurusList() {
        thesaurusList = editionThesaurusService.listAdminThesauri(
                userSession.getCurrentUserId(),
                userSession.isSuperAdmin()
        );
        filteredThesaurusList = null;
    }

    private boolean canAccessScreen() {
        return ToolboxAccessPolicy.canAccessEditionScreen(userSession);
    }
}
