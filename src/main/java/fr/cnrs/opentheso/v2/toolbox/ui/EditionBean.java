package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusSummary;
import fr.cnrs.opentheso.v2.sync.ui.ThesaurusSyncBean;
import fr.cnrs.opentheso.v2.toolbox.edition.ui.ThesaurusEditionCsvImportBean;
import fr.cnrs.opentheso.v2.toolbox.edition.ui.ThesaurusEditionCsvStructuredImportBean;
import fr.cnrs.opentheso.v2.toolbox.edition.ui.ThesaurusEditionSkosImportBean;
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

    private final transient EditionSessionCollaborators session;
    private final transient EditionChildBeans children;

    private EditionView currentView = EditionView.LIST;
    private List<EditionThesaurusSummary> thesaurusList = Collections.emptyList();
    private List<EditionThesaurusSummary> filteredThesaurusList;

    private String thesaurusIdToDelete;
    private String thesaurusTitleToDelete;
    private boolean deletePerennialIdentifiers;

    private EditionThesaurusSummary selectedThesaurusForAction;

    public EditionBean(EditionSessionCollaborators session, EditionChildBeans children) {
        this.session = session;
        this.children = children;
    }

    public UserSession getUserSession() {
        return session.userSession();
    }

    public ToolboxAccessPolicy getToolboxAccessPolicy() {
        return session.toolboxAccessPolicy();
    }

    public ThesaurusContext getThesaurusContext() {
        return session.thesaurusContext();
    }

    public V2LocaleBean getLocaleBean() {
        return session.localeBean();
    }

    public EditionThesaurusService getEditionThesaurusService() {
        return session.editionThesaurusService();
    }

    public ConsultationShellBean getConsultationShellBean() {
        return session.consultationShellBean();
    }

    public NewThesaurusBean getNewThesaurusBean() {
        return children.newThesaurusBean();
    }

    public ModifyThesaurusBean getModifyThesaurusBean() {
        return children.modifyThesaurusBean();
    }

    public ThesaurusExportBean getThesaurusExportBean() {
        return children.thesaurusExportBean();
    }

    public ThesaurusEditionSkosImportBean getSkosImportBean() {
        return children.skosImportBean();
    }

    public ThesaurusEditionCsvImportBean getCsvImportBean() {
        return children.csvImportBean();
    }

    public ThesaurusEditionCsvStructuredImportBean getCsvStructuredImportBean() {
        return children.csvStructuredImportBean();
    }

    public ThesaurusSyncBean getThesaurusSyncBean() {
        return children.thesaurusSyncBean();
    }

    public void load() {
        session.thesaurusContext().syncFromViewParams();
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
        return session.toolboxAccessPolicy().canCreateOrImportThesaurus(session.userSession());
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

    public boolean isSyncView() {
        return currentView == EditionView.SYNC;
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
        children.newThesaurusBean().prepareForm();
        currentView = EditionView.NEW;
    }

    public void showImportSkos() {
        if (!isCanCreateOrImport()) {
            return;
        }
        children.skosImportBean().init();
        currentView = EditionView.IMPORT_SKOS;
    }

    public void showImportCsv() {
        if (!isCanCreateOrImport()) {
            return;
        }
        children.csvImportBean().init();
        currentView = EditionView.IMPORT_CSV;
    }

    public void showImportCsvStructure() {
        if (!isCanCreateOrImport()) {
            return;
        }
        children.csvStructuredImportBean().init();
        currentView = EditionView.IMPORT_CSV_STRUCTURE;
    }

    public void showModifyThesaurus(EditionThesaurusSummary thesaurus) {
        if (!canAccessScreen()) {
            return;
        }
        selectedThesaurusForAction = thesaurus;
        children.modifyThesaurusBean().load(thesaurus.id());
        currentView = EditionView.MODIFY;
    }

    public void showModifyThesaurusById(String thesaurusId) {
        if (!canAccessScreen() || StringUtils.isBlank(thesaurusId)) {
            return;
        }
        children.modifyThesaurusBean().load(thesaurusId);
        currentView = EditionView.MODIFY;
    }

    public void showSyncThesaurus(String thesaurusId) {
        if (!canAccessScreen() || StringUtils.isBlank(thesaurusId)) {
            return;
        }
        children.thesaurusSyncBean().init(thesaurusId);
        currentView = EditionView.SYNC;
    }

    public void showExport(EditionThesaurusSummary thesaurus, EditionView exportView) {
        selectedThesaurusForAction = thesaurus;
        currentView = exportView;
        if (thesaurus == null) {
            return;
        }
        if (exportView == EditionView.EXPORT_SKOS) {
            children.thesaurusExportBean().init(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_CSV) {
            children.thesaurusExportBean().initCsv(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_CSV_ID) {
            children.thesaurusExportBean().initCsvById(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_CSV_STRUCTURE) {
            children.thesaurusExportBean().initCsvStructured(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_PDF) {
            children.thesaurusExportBean().initPdf(thesaurus.id(), thesaurus.title());
        } else if (exportView == EditionView.EXPORT_DEPRECATED) {
            children.thesaurusExportBean().initDeprecated(thesaurus.id(), thesaurus.title());
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
            session.editionThesaurusService().deleteThesaurus(thesaurusIdToDelete, deletePerennialIdentifiers);
            if (session.thesaurusContext().matchesCurrentThesaurus(thesaurusIdToDelete)) {
                session.thesaurusContext().clearSelection();
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
        var stats = session.editionThesaurusService().loadStatistics(thesaurus.id());
        var message = new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                session.localeBean().getMsg("info"),
                session.localeBean().getMsg("candidat.total_concepts") + " = " + stats.conceptCount() + "\n"
                        + session.localeBean().getMsg("candidat.titre") + " = " + stats.candidateCount() + "\n"
                        + session.localeBean().getMsg("search.deprecated") + " = " + stats.deprecatedCount()
        );
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    private void resetToListView() {
        currentView = EditionView.LIST;
        selectedThesaurusForAction = null;
    }

    private void refreshThesaurusList() {
        thesaurusList = session.editionThesaurusService().listAdminThesauri(
                session.userSession().getCurrentUserId(),
                session.userSession().isSuperAdmin()
        );
        filteredThesaurusList = null;
        session.consultationShellBean().refreshHeaderCatalog();
    }

    private boolean canAccessScreen() {
        return session.toolboxAccessPolicy().canAccessEditionScreen(session.userSession());
    }
}

@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
record EditionSessionCollaborators(
        UserSession userSession,
        ToolboxAccessPolicy toolboxAccessPolicy,
        ThesaurusContext thesaurusContext,
        V2LocaleBean localeBean,
        EditionThesaurusService editionThesaurusService,
        ConsultationShellBean consultationShellBean
) {
}

@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
record EditionChildBeans(
        NewThesaurusBean newThesaurusBean,
        ModifyThesaurusBean modifyThesaurusBean,
        ThesaurusExportBean thesaurusExportBean,
        ThesaurusEditionSkosImportBean skosImportBean,
        ThesaurusEditionCsvImportBean csvImportBean,
        ThesaurusEditionCsvStructuredImportBean csvStructuredImportBean,
        ThesaurusSyncBean thesaurusSyncBean
) {
}
