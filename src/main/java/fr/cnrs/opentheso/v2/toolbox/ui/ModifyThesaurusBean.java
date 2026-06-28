package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusAccessService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.EditionCollectionNode;
import fr.cnrs.opentheso.v2.toolbox.model.EditionMetadata;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusDetails;
import fr.cnrs.opentheso.v2.toolbox.model.EditionThesaurusLanguage;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.service.ModifyThesaurusService;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ModifyThesaurusBean")
public class ModifyThesaurusBean implements Serializable {

    private final UserSession userSession;
    private final CurrentUser currentUser;
    private final ThesaurusAccessService thesaurusAccessService;
    private final ModifyThesaurusService modifyThesaurusService;

    private String thesaurusId;
    private EditionThesaurusDetails details;
    private String sourceLang;
    private boolean privateThesaurus;
    private String arkId;
    private int activeTabIndex;

    private List<EditionThesaurusLanguage> languages = Collections.emptyList();
    private List<LanguageOption> allLanguages = Collections.emptyList();
    private List<EditionMetadata> metadata = Collections.emptyList();
    private List<String> dcmiResources = Collections.emptyList();
    private List<String> dcmiTypes = Collections.emptyList();
    private TreeNode<EditionCollectionNode> collectionRoot = new DefaultTreeNode<>(null, null);

    private AddLanguageEditor addLanguageEditor = AddLanguageEditor.empty();
    private EditLanguageEditor editLanguageEditor = EditLanguageEditor.empty();
    private ChangeThesaurusIdEditor changeIdEditor = ChangeThesaurusIdEditor.empty();

    public ModifyThesaurusBean(
            UserSession userSession,
            CurrentUser currentUser,
            ThesaurusAccessService thesaurusAccessService,
            ModifyThesaurusService modifyThesaurusService
    ) {
        this.userSession = userSession;
        this.currentUser = currentUser;
        this.thesaurusAccessService = thesaurusAccessService;
        this.modifyThesaurusService = modifyThesaurusService;
    }

    public boolean isFormAvailable() {
        return StringUtils.isNotBlank(thesaurusId) && canManage();
    }

    public void load(String thesaurusId) {
        this.thesaurusId = thesaurusId;
        activeTabIndex = 0;
        if (!canManage()) {
            clearState();
            return;
        }
        refreshAll();
    }

    public void prepareAddLanguage() {
        addLanguageEditor = AddLanguageEditor.empty();
    }

    public void prepareEditLanguage(EditionThesaurusLanguage language) {
        editLanguageEditor = EditLanguageEditor.from(
                language.code(),
                language.countryCode(),
                language.displayLabel(),
                language.label()
        );
    }

    public void prepareChangeId() {
        changeIdEditor = ChangeThesaurusIdEditor.forThesaurus(thesaurusId);
    }

    public void changeSourceLanguage() {
        if (!canManage()) {
            return;
        }
        try {
            modifyThesaurusService.changeSourceLanguage(thesaurusId, sourceLang);
            refreshAll();
            MessageUtils.showInformationMessage("Langue source modifiée avec succès");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void changeVisibility() {
        if (!canManage()) {
            return;
        }
        try {
            modifyThesaurusService.changeVisibility(thesaurusId, privateThesaurus);
            refreshDetails();
            refreshMenuThesaurusList();
            MessageUtils.showInformationMessage(privateThesaurus
                    ? "Le thésaurus est maintenant privé"
                    : "Le thésaurus est maintenant public");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void generateArkId() {
        if (!canManage()) {
            return;
        }
        String generated = modifyThesaurusService.generateArkId(thesaurusId);
        if (StringUtils.isNotBlank(generated)) {
            arkId = generated;
        }
        refreshDetails();
    }

    public void changeThesaurusId() {
        if (!canManage()) {
            return;
        }
        try {
            String newId = modifyThesaurusService.changeThesaurusId(thesaurusId, changeIdEditor.getNewId());
            thesaurusId = newId;
            changeIdEditor = ChangeThesaurusIdEditor.forThesaurus(newId);
            refreshAll();
            refreshMenuThesaurusList();
            MessageUtils.showInformationMessage("Le changement d'identifiant a réussi, veuillez recharger les thésaurus");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void addLanguage() {
        if (!canManage()) {
            return;
        }
        try {
            modifyThesaurusService.addLanguage(
                    thesaurusId,
                    addLanguageEditor.getTitle(),
                    addLanguageEditor.getSelectedLanguage(),
                    userSession.getCurrentUsername()
            );
            addLanguageEditor = AddLanguageEditor.empty();
            refreshLanguages();
            refreshMenuThesaurusList();
            MessageUtils.showInformationMessage("Langue ajoutée avec succès");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void updateLanguage() {
        if (!canManage()) {
            return;
        }
        try {
            modifyThesaurusService.updateLanguage(
                    thesaurusId,
                    editLanguageEditor.getCode(),
                    editLanguageEditor.getLabel(),
                    userSession.getCurrentUsername()
            );
            refreshLanguages();
            refreshMenuThesaurusList();
            MessageUtils.showInformationMessage("Langue modifiée avec succès");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void deleteLanguage(String languageCode) {
        if (!canManage()) {
            return;
        }
        try {
            modifyThesaurusService.deleteLanguage(thesaurusId, languageCode);
            refreshAll();
            MessageUtils.showInformationMessage("Langue supprimée avec succès");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void addMetadataRow() {
        metadata.add(EditionMetadata.emptyRow());
    }

    public void onMetadataRowEdit(RowEditEvent<EditionMetadata> event) {
        if (!canManage()) {
            return;
        }
        try {
            modifyThesaurusService.saveMetadata(thesaurusId, event.getObject());
            MessageUtils.showInformationMessage("Métadonnée mise à jour");
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void deleteMetadata(EditionMetadata metadataRow) {
        if (!canManage() || metadataRow == null) {
            return;
        }
        if (metadataRow.getId() == -1) {
            metadata.remove(metadataRow);
            return;
        }
        try {
            modifyThesaurusService.deleteMetadata(thesaurusId, metadataRow.getId());
            refreshMetadata();
            MessageUtils.showInformationMessage("Métadonnée supprimée");
        } catch (RuntimeException e) {
            MessageUtils.showErrorMessage("Erreur lors de la suppression de la métadonnée");
        }
    }

    public void clearMetadataLanguage(EditionMetadata metadataRow) {
        if (metadataRow != null && StringUtils.isNotBlank(metadataRow.getType())) {
            metadataRow.setLanguage("");
        }
    }

    public void clearMetadataType(EditionMetadata metadataRow) {
        if (metadataRow != null && StringUtils.isNotBlank(metadataRow.getLanguage())) {
            metadataRow.setType("");
        }
    }

    public void updateCollectionStatus(EditionCollectionNode collection) {
        if (!canManage() || collection == null) {
            return;
        }
        TreeNode<EditionCollectionNode> node = findCollectionNode(collectionRoot, collection.getId());
        List<String> groupIds = new ArrayList<>();
        groupIds.add(collection.getId());
        if (node != null && CollectionUtils.isNotEmpty(node.getChildren())) {
            collectDescendantIds(node, groupIds);
            applyPrivateStatusToDescendants(node, collection.isPrivateCollection());
        }
        try {
            modifyThesaurusService.updateCollectionsVisibility(thesaurusId, groupIds, collection.isPrivateCollection());
            MessageUtils.showInformationMessage(String.format(
                    "La collection %s est maintenant %s",
                    collection.getLabel(),
                    collection.isPrivateCollection() ? "privée" : "publique"
            ));
        } catch (RuntimeException e) {
            MessageUtils.showErrorMessage("Erreur lors de la mise à jour de la visibilité de la collection");
        }
    }

    public void backToList() {
        editionBean().showList();
    }

    private void refreshAll() {
        refreshDetails();
        refreshLanguages();
        allLanguages = modifyThesaurusService.loadAllLanguages();
        refreshMetadata();
        dcmiResources = modifyThesaurusService.loadDcmiResources();
        dcmiTypes = modifyThesaurusService.loadDcmiTypes();
        collectionRoot = modifyThesaurusService.loadCollectionTree(thesaurusId);
    }

    private void refreshDetails() {
        details = modifyThesaurusService.loadDetails(thesaurusId);
        sourceLang = details.sourceLang();
        privateThesaurus = details.privateThesaurus();
        arkId = details.arkId();
    }

    private void refreshLanguages() {
        languages = modifyThesaurusService.loadLanguages(thesaurusId);
    }

    private void refreshMetadata() {
        metadata = new ArrayList<>(modifyThesaurusService.loadMetadata(thesaurusId));
    }

    private void clearState() {
        thesaurusId = null;
        details = null;
        sourceLang = null;
        arkId = null;
        languages = Collections.emptyList();
        allLanguages = Collections.emptyList();
        metadata = Collections.emptyList();
        dcmiResources = Collections.emptyList();
        dcmiTypes = Collections.emptyList();
        collectionRoot = new DefaultTreeNode<>(null, null);
        addLanguageEditor = AddLanguageEditor.empty();
        editLanguageEditor = EditLanguageEditor.empty();
        changeIdEditor = ChangeThesaurusIdEditor.empty();
    }

    private boolean canManage() {
        if (!userSession.isLoggedIn() || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return thesaurusAccessService.canManageThesaurus(
                userSession.getCurrentUserId(),
                userSession.isSuperAdmin(),
                thesaurusId
        );
    }

    private void refreshMenuThesaurusList() {
        PrimeFaces.current().ajax().update("formMenu:idListTheso");
    }

    private EditionBean editionBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{v2EditionBean}", EditionBean.class);
    }

    private TreeNode<EditionCollectionNode> findCollectionNode(TreeNode<EditionCollectionNode> parent, String groupId) {
        if (parent == null || groupId == null) {
            return null;
        }
        if (parent.getData() != null && groupId.equalsIgnoreCase(parent.getData().getId())) {
            return parent;
        }
        for (TreeNode<EditionCollectionNode> child : parent.getChildren()) {
            TreeNode<EditionCollectionNode> found = findCollectionNode(child, groupId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void collectDescendantIds(TreeNode<EditionCollectionNode> node, List<String> groupIds) {
        for (TreeNode<EditionCollectionNode> child : node.getChildren()) {
            if (child.getData() != null) {
                groupIds.add(child.getData().getId());
            }
            if (CollectionUtils.isNotEmpty(child.getChildren())) {
                collectDescendantIds(child, groupIds);
            }
        }
    }

    private void applyPrivateStatusToDescendants(TreeNode<EditionCollectionNode> node, boolean isPrivate) {
        for (TreeNode<EditionCollectionNode> child : node.getChildren()) {
            if (child.getData() != null) {
                child.getData().setPrivateCollection(isPrivate);
            }
            if (CollectionUtils.isNotEmpty(child.getChildren())) {
                applyPrivateStatusToDescendants(child, isPrivate);
            }
        }
    }
}
