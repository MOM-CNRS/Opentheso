package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.EditionMetadata;
import fr.cnrs.opentheso.v2.toolbox.service.ModifyThesaurusService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@ViewScoped
@Named("v2ThesaurusMetadataBean")
@RequiredArgsConstructor
public class ThesaurusHomeMetadataBean implements Serializable {

    static final int MASTER_ROW_ID = -2;

    private final transient UserSession userSession;
    private final transient RightsService rightsService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ModifyThesaurusService modifyThesaurusService;
    private final transient ObjectProvider<ThesaurusViewBean> thesaurusViewBean;

    @Getter
    private boolean editing;
    private boolean masterThesaurus;
    @Getter
    private List<EditionMetadata> rows = new ArrayList<>();
    @Getter
    private List<String> dcmiResources = Collections.emptyList();
    @Getter
    private List<String> dcmiTypes = Collections.emptyList();

    public boolean isEditable() {
        Integer userId = userSession.getCurrentUserId();
        String thesaurusId = currentThesaurusId();
        if (userId == null || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId);
    }

    public boolean isSectionVisible() {
        return editing || StringUtils.isNotBlank(currentThesaurusId());
    }

    public boolean isMasterThesaurus() {
        if (editing) {
            return masterThesaurus;
        }
        String thesaurusId = currentThesaurusId();
        return StringUtils.isNotBlank(thesaurusId) && modifyThesaurusService.isMasterThesaurus(thesaurusId);
    }

    public boolean isMasterRow(EditionMetadata row) {
        return row != null && DCMIResource.MAITRE.equalsIgnoreCase(row.getName());
    }

    /** Alias EL : #{bean.masterRow(row)} ne résout pas {@link #isMasterRow}. */
    public boolean masterRow(EditionMetadata row) {
        return isMasterRow(row);
    }

    public boolean isBooleanRow(EditionMetadata row) {
        return row != null && DCMIResource.TYPE_BOOLEAN.equalsIgnoreCase(row.getType());
    }

    /** Alias EL : #{bean.booleanRow(row)} ne résout pas {@link #isBooleanRow}. */
    public boolean booleanRow(EditionMetadata row) {
        return isBooleanRow(row);
    }

    public void startEditing() {
        if (!isEditable()) {
            return;
        }
        String thesaurusId = currentThesaurusId();
        masterThesaurus = modifyThesaurusService.isMasterThesaurus(thesaurusId);
        dcmiResources = withMasterProperty(modifyThesaurusService.loadDcmiResources());
        dcmiTypes = withBooleanType(modifyThesaurusService.loadDcmiTypes());
        reloadRows(thesaurusId);
        editing = true;
    }

    public void cancelEditing() {
        editing = false;
        rows = new ArrayList<>();
    }

    public void addRow() {
        if (!editing || !isEditable()) {
            return;
        }
        EditionMetadata row = EditionMetadata.emptyRow();
        row.setName(firstRegularProperty());
        rows.add(row);
    }

    public void onPropertyChanged(EditionMetadata row) {
        if (row == null) {
            return;
        }
        if (isMasterRow(row)) {
            applyMasterDefaults(row);
        }
    }

    public void clearLanguageIfTyped(EditionMetadata row) {
        if (row != null && StringUtils.isNotBlank(row.getType())) {
            row.setLanguage("");
        }
        if (isMasterRow(row)) {
            applyMasterDefaults(row);
        }
    }

    public void clearTypeIfLanguaged(EditionMetadata row) {
        if (isMasterRow(row) || isBooleanRow(row)) {
            row.setLanguage("");
            return;
        }
        if (row != null && StringUtils.isNotBlank(row.getLanguage())) {
            row.setType("");
        }
    }

    public void saveRow(EditionMetadata row) {
        if (!editing || !isEditable() || row == null) {
            return;
        }
        try {
            String thesaurusId = currentThesaurusId();
            if (isMasterRow(row)) {
                applyMasterDefaults(row);
                masterThesaurus = parseBooleanValue(row.getValue());
                modifyThesaurusService.updateMasterRole(thesaurusId, masterThesaurus);
                if (row.getId() > 0) {
                    modifyThesaurusService.deleteMetadata(thesaurusId, row.getId());
                }
                reloadRows(thesaurusId);
                refreshHome();
                MessageUtils.showInformationMessage("Rôle du thésaurus enregistré");
                return;
            }
            if (isBooleanRow(row)) {
                row.setLanguage("");
            }
            modifyThesaurusService.saveMetadata(thesaurusId, row);
            reloadRows(thesaurusId);
            refreshHome();
            MessageUtils.showInformationMessage("Métadonnée enregistrée");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void deleteRow(EditionMetadata row) {
        if (!editing || !isEditable() || row == null) {
            return;
        }
        if (isMasterRow(row)) {
            MessageUtils.showErrorMessage("La propriété maitre ne peut pas être supprimée");
            return;
        }
        if (row.getId() == -1) {
            rows.remove(row);
            return;
        }
        try {
            String thesaurusId = currentThesaurusId();
            modifyThesaurusService.deleteMetadata(thesaurusId, row.getId());
            reloadRows(thesaurusId);
            refreshHome();
            MessageUtils.showInformationMessage("Métadonnée supprimée");
        } catch (RuntimeException e) {
            MessageUtils.showErrorMessage("Erreur lors de la suppression de la métadonnée");
        }
    }

    public List<ThesaurusLanguage> getLanguages() {
        ThesaurusViewBean view = thesaurusViewBean.getIfAvailable();
        return view == null ? List.of() : view.getLanguages();
    }

    private void reloadRows(String thesaurusId) {
        rows = new ArrayList<>();
        rows.add(masterRow());
        for (EditionMetadata row : modifyThesaurusService.loadMetadata(thesaurusId)) {
            if (!isMasterRow(row)) {
                rows.add(row);
            }
        }
    }

    private EditionMetadata masterRow() {
        EditionMetadata row = EditionMetadata.emptyRow();
        row.setId(MASTER_ROW_ID);
        applyMasterDefaults(row);
        return row;
    }

    private void applyMasterDefaults(EditionMetadata row) {
        row.setName(DCMIResource.MAITRE);
        row.setType(DCMIResource.TYPE_BOOLEAN);
        row.setLanguage("");
        if (!isTrueOrFalse(row.getValue())) {
            row.setValue(masterThesaurus ? "true" : "false");
        }
    }

    private String firstRegularProperty() {
        for (String resource : dcmiResources) {
            if (!DCMIResource.MAITRE.equalsIgnoreCase(resource)) {
                return resource;
            }
        }
        return DCMIResource.TITLE;
    }

    private static List<String> withMasterProperty(List<String> resources) {
        List<String> list = new ArrayList<>(resources == null ? List.of() : resources);
        boolean present = list.stream().anyMatch(DCMIResource.MAITRE::equalsIgnoreCase);
        if (!present) {
            list.add(0, DCMIResource.MAITRE);
        }
        return list;
    }

    private static List<String> withBooleanType(List<String> types) {
        List<String> list = new ArrayList<>(types == null ? List.of() : types);
        boolean present = list.stream().anyMatch(DCMIResource.TYPE_BOOLEAN::equalsIgnoreCase);
        if (!present) {
            list.add(DCMIResource.TYPE_BOOLEAN);
        }
        return list;
    }

    private static boolean parseBooleanValue(String value) {
        String normalized = StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT);
        return normalized.equals("true")
                || normalized.equals("1")
                || normalized.equals("oui")
                || normalized.equals("yes")
                || normalized.equals("maitre")
                || normalized.equals("master");
    }

    private static boolean isTrueOrFalse(String value) {
        String normalized = StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("false");
    }

    private void refreshHome() {
        ThesaurusViewBean view = thesaurusViewBean.getIfAvailable();
        if (view != null) {
            view.refreshHomeOverview();
        }
    }

    private String currentThesaurusId() {
        return StringUtils.trimToNull(thesaurusContext.resolveThesaurusId());
    }
}
