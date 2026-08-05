package fr.cnrs.opentheso.v2.concept.alignment.ui;

import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentAdminRow;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentProposition;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentWorkbenchMode;
import fr.cnrs.opentheso.v2.concept.alignment.service.ConceptAlignmentAdminService;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.ui.ConceptAlignmentEditorBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Atelier d'alignement du panneau droit V2 (équivalent legacy AlignmentBean / SetAlignmentSourceBean).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptAlignmentAdminBean")
@RequiredArgsConstructor
public class ConceptAlignmentAdminBean implements Serializable {

    private final ConceptAlignmentAdminService conceptAlignmentAdminService;
    private final ConceptAlignmentEditorBean conceptAlignmentEditorBean;
    private final ObjectProvider<ConceptAlignmentSearchBean> conceptAlignmentSearchBean;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;

    private AlignmentWorkbenchMode mode = AlignmentWorkbenchMode.SUMMARY;
    private String rootConceptId;
    private List<AlignmentAdminRow> summaryRows = Collections.emptyList();
    private List<AlignmentAdminRow> filteredRows;
    private List<AlignmentSourceItem> sourceItems = Collections.emptyList();
    private List<AlignmentProposition> propositions = new ArrayList<>();
    /** Sélection unique (radio), comme le legacy. */
    private AlignmentProposition selectedProposition;
    /** Proposition en cours de validation (dialog enrichissement). */
    private AlignmentProposition propositionToValidate;
    private int activeSearchSourceId;
    private List<AlignmentSourceItem> selectableSources = Collections.emptyList();
    private Integer selectedSourceIdForSearch;
    private String pendingSearchMode;
    private String selectedSourceName;

    private String newSourceName;
    private String newSourceUri;
    private String newSourceThesaurusId;
    private String newSourceDescription;

    public void openForCurrentConcept() {
        if (!conceptSelectionContext.hasSelection() || conceptSelectionContext.getSummary() == null) {
            clear();
            return;
        }
        rootConceptId = conceptSelectionContext.getSummary().conceptId();
        reloadSummary();
        mode = AlignmentWorkbenchMode.SUMMARY;
        resetPropositionsState();
    }

    /**
     * Recharge le résumé en conservant la racine de branche courante
     * (ex. après ajout d'alignement depuis l'atelier).
     */
    public void reloadCurrentBranchSummary() {
        if (StringUtils.isBlank(rootConceptId)) {
            openForCurrentConcept();
            return;
        }
        reloadSummary();
        if (mode != AlignmentWorkbenchMode.PROPOSITIONS && mode != AlignmentWorkbenchMode.COMPARISON) {
            mode = AlignmentWorkbenchMode.SUMMARY;
        }
    }

    public void reloadBranchSummary(String branchRootConceptId) {
        if (StringUtils.isBlank(branchRootConceptId)) {
            reloadCurrentBranchSummary();
            return;
        }
        rootConceptId = branchRootConceptId;
        reloadCurrentBranchSummary();
    }

    public void clear() {
        rootConceptId = null;
        summaryRows = Collections.emptyList();
        filteredRows = null;
        sourceItems = Collections.emptyList();
        resetPropositionsState();
        mode = AlignmentWorkbenchMode.SUMMARY;
    }

    public boolean isSummaryMode() {
        return mode == AlignmentWorkbenchMode.SUMMARY;
    }

    public boolean isPropositionsMode() {
        return mode == AlignmentWorkbenchMode.PROPOSITIONS;
    }

    public boolean isComparisonMode() {
        return mode == AlignmentWorkbenchMode.COMPARISON;
    }

    public boolean isManageSourcesMode() {
        return mode == AlignmentWorkbenchMode.MANAGE_SOURCES;
    }

    public boolean isReady() {
        return StringUtils.isNotBlank(rootConceptId);
    }

    public void reloadSummary() {
        if (StringUtils.isBlank(rootConceptId)) {
            summaryRows = Collections.emptyList();
            return;
        }
        summaryRows = conceptAlignmentAdminService.loadBranchSummary(
                thesaurusContext.resolveThesaurusId(),
                rootConceptId,
                thesaurusContext.resolveWorkLanguage()
        );
        filteredRows = null;
    }

    public int getTotalAlignments() {
        return conceptAlignmentAdminService.countAlignments(summaryRows);
    }

    public int getTotalAlignmentsForConcept(String conceptId) {
        return conceptAlignmentAdminService.countAlignmentsForConcept(summaryRows, conceptId);
    }

    public int getPropositionCountForConcept(String conceptId) {
        if (StringUtils.isBlank(conceptId) || propositions == null) {
            return 0;
        }
        return (int) propositions.stream().filter(item -> conceptId.equals(item.getConceptId())).count();
    }

    public void checkUrls(String conceptId) {
        int invalid = conceptAlignmentAdminService.checkUrlsForConcept(
                thesaurusContext.resolveThesaurusId(), conceptId, summaryRows);
        reloadSummary();
        if (invalid > 0) {
            MessageUtils.showWarnMessage("Il existe au moins un alignement qui n'est plus disponible !");
        } else {
            MessageUtils.showInformationMessage("Tous les alignements sont opérationnels !");
        }
    }

    public void prepareAddForConcept(AlignmentAdminRow row) {
        if (row == null || StringUtils.isBlank(row.conceptId())) {
            return;
        }
        conceptAlignmentSearchBean.getObject().prepare(row.conceptId(), row.conceptLabel());
    }

    public void prepareEdit(AlignmentAdminRow row) {
        if (row == null || row.isPlaceholder()) {
            return;
        }
        conceptNavigationSupport.openConcept(row.conceptId());
        conceptAlignmentEditorBean.prepareEdit(toConceptAlignment(row));
    }

    public void prepareDelete(AlignmentAdminRow row) {
        if (row == null || row.isPlaceholder()) {
            return;
        }
        conceptNavigationSupport.openConcept(row.conceptId());
        conceptAlignmentEditorBean.prepareDelete(toConceptAlignment(row));
    }

    public void openManageSources() {
        sourceItems = conceptAlignmentAdminService.listSourcesForManagement(thesaurusContext.resolveThesaurusId());
        mode = AlignmentWorkbenchMode.MANAGE_SOURCES;
    }

    public void backToSummary() {
        mode = AlignmentWorkbenchMode.SUMMARY;
        resetPropositionsState();
        reloadSummary();
    }

    public void onSourceToggle(AlignmentSourceItem item) {
        if (item == null) {
            return;
        }
        conceptAlignmentAdminService.setSourceSelected(
                thesaurusContext.resolveThesaurusId(),
                item.getSourceId(),
                item.isSelected()
        );
        MessageUtils.showInformationMessage("Source mise à jour !");
    }

    public void deleteSource(AlignmentSourceItem item) {
        if (item == null || item.isGlobal()) {
            MessageUtils.showErrorMessage("Cette source ne peut pas être supprimée.");
            return;
        }
        if (!conceptAlignmentAdminService.deleteLocalSource(item.getSourceId())) {
            MessageUtils.showErrorMessage("Erreur pendant la suppression de la source !");
            return;
        }
        MessageUtils.showWarnMessage("Suppression réussie");
        openManageSources();
    }

    public void initAutomaticSearch() {
        startSearchFlow("alignement-auto");
    }

    public void initComparisonSearch() {
        startSearchFlow("alignement-comparaison");
    }

    public void confirmSelectedSourceAndSearch() {
        if (selectedSourceIdForSearch == null || selectedSourceIdForSearch <= 0) {
            MessageUtils.showWarnMessage("Veuillez sélectionner une source !");
            return;
        }
        runSearch(selectedSourceIdForSearch, pendingSearchMode);
        PrimeFaces.current().executeScript("PF('v2SelectAlignmentSource').hide();");
    }

    /**
     * Fermer : retire toutes les propositions du concept (sans écriture en base).
     */
    public void closePropositionsForConcept(String conceptId) {
        if (StringUtils.isBlank(conceptId) || propositions == null) {
            return;
        }
        propositions = new ArrayList<>(propositions.stream()
                .filter(item -> !conceptId.equals(item.getConceptId()))
                .toList());
        if (selectedProposition != null && conceptId.equals(selectedProposition.getConceptId())) {
            selectedProposition = null;
        }
        if (propositions.isEmpty()) {
            backToSummary();
        }
    }

    /**
     * Valider : ouvre le dialog d'enrichissement pour la proposition sélectionnée du concept.
     */
    public void prepareValidateForConcept(String conceptId) {
        if (StringUtils.isBlank(conceptId)) {
            return;
        }
        if (selectedProposition == null
                || !conceptId.equals(selectedProposition.getConceptId())) {
            MessageUtils.showWarnMessage("Veuillez sélectionner un alignement pour ce concept.");
            return;
        }
        AlignementSource source = resolveActiveSearchSource();
        if (source == null) {
            MessageUtils.showErrorMessage("Source d'alignement introuvable.");
            return;
        }
        propositionToValidate = selectedProposition;
        conceptAlignmentAdminService.enrichProposition(
                propositionToValidate,
                source,
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
        PrimeFaces.current().executeScript("PF('v2ValidateAlignmentProposition').show();");
        PrimeFaces.current().ajax().update(":containerIndex:v2ValidateAlignmentPropositionDlg");
    }

    /**
     * Confirmation du dialog : persiste alignement + enrichissements cochés.
     */
    public void confirmValidateProposition() {
        if (propositionToValidate == null) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        boolean ok = conceptAlignmentAdminService.acceptProposition(
                thesaurusContext.resolveThesaurusId(),
                propositionToValidate,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        if (!ok) {
            MessageUtils.showErrorMessage("L'ajout de l'alignement a échoué !");
            return;
        }
        MessageUtils.showInformationMessage("Alignement ajouté avec succès");
        String conceptId = propositionToValidate.getConceptId();
        propositionToValidate = null;
        selectedProposition = null;
        closePropositionsForConcept(conceptId);
        conceptNavigationSupport.openConcept(conceptId);
        PrimeFaces.current().executeScript("PF('v2ValidateAlignmentProposition').hide();");
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :messageIndex");
    }

    public void cancelValidateProposition() {
        propositionToValidate = null;
        PrimeFaces.current().executeScript("PF('v2ValidateAlignmentProposition').hide();");
    }

    public void discardProposition(AlignmentProposition proposition) {
        if (proposition == null || propositions == null) {
            return;
        }
        propositions = new ArrayList<>(propositions.stream()
                .filter(item -> !(StringUtils.equals(item.getConceptId(), proposition.getConceptId())
                        && StringUtils.equals(item.getTargetUri(), proposition.getTargetUri())))
                .toList());
        if (selectedProposition != null
                && StringUtils.equals(selectedProposition.getConceptId(), proposition.getConceptId())
                && StringUtils.equals(selectedProposition.getTargetUri(), proposition.getTargetUri())) {
            selectedProposition = null;
        }
    }

    public void replaceWithProposition(AlignmentProposition proposition) {
        if (proposition == null) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        boolean replaced = conceptAlignmentAdminService.replaceAlignmentFromProposition(
                thesaurusContext.resolveThesaurusId(),
                proposition,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        if (replaced) {
            MessageUtils.showInformationMessage("Alignement remplacé avec succès");
            discardProposition(proposition);
            conceptNavigationSupport.openConcept(proposition.getConceptId());
            if (propositions.isEmpty()) {
                backToSummary();
            }
        } else {
            MessageUtils.showErrorMessage("Le remplacement de l'alignement a échoué");
        }
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :messageIndex");
    }

    public void prepareAddSource() {
        newSourceName = "";
        newSourceUri = "";
        newSourceThesaurusId = "";
        newSourceDescription = "";
    }

    public void addOpenthesoSource() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        String error = conceptAlignmentAdminService.addOpenthesoSource(
                thesaurusContext.resolveThesaurusId(),
                userId,
                newSourceName,
                newSourceUri,
                newSourceThesaurusId,
                newSourceDescription
        );
        if (error != null) {
            MessageUtils.showWarnMessage(error);
            return;
        }
        MessageUtils.showInformationMessage("Source ajoutée avec succès !");
        openManageSources();
        PrimeFaces.current().executeScript("PF('v2AddAlignmentSource').hide();");
    }

    private void startSearchFlow(String searchMode) {
        if (!isReady()) {
            MessageUtils.showWarnMessage("Sélectionnez d'abord un concept dans l'arbre.");
            return;
        }
        List<AlignementSource> active = conceptAlignmentAdminService.listActiveSources(
                thesaurusContext.resolveThesaurusId());
        if (active.isEmpty()) {
            MessageUtils.showWarnMessage("Veuillez sélectionner une source !");
            return;
        }
        selectableSources = active.stream()
                .map(source -> new AlignmentSourceItem(
                        source.getId(),
                        source.getSource(),
                        StringUtils.defaultString(source.getDescription()),
                        true,
                        false
                ))
                .toList();
        pendingSearchMode = searchMode;
        if (selectableSources.size() == 1) {
            runSearch(selectableSources.get(0).getSourceId(), searchMode);
            return;
        }
        selectedSourceIdForSearch = null;
        PrimeFaces.current().executeScript("PF('v2SelectAlignmentSource').show();");
    }

    private void runSearch(int sourceId, String searchMode) {
        AlignementSource source = conceptAlignmentAdminService.findActiveSource(
                thesaurusContext.resolveThesaurusId(), sourceId);
        if (source == null) {
            MessageUtils.showWarnMessage("Veuillez sélectionner une source !");
            return;
        }
        if (summaryRows == null || summaryRows.isEmpty()) {
            reloadSummary();
        }
        conceptAlignmentEditorBean.prepareManualAlignment();
        String lang = thesaurusContext.resolveWorkLanguage();
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        activeSearchSourceId = source.getId();
        selectedSourceName = source.getSource();
        selectedProposition = null;
        propositionToValidate = null;

        if ("alignement-comparaison".equalsIgnoreCase(searchMode)) {
            propositions = new ArrayList<>(conceptAlignmentAdminService.searchComparisons(
                    thesaurusId, lang, summaryRows, source));
            mode = AlignmentWorkbenchMode.COMPARISON;
        } else {
            propositions = new ArrayList<>(conceptAlignmentAdminService.searchPropositions(
                    thesaurusId, lang, summaryRows, source));
            mode = AlignmentWorkbenchMode.PROPOSITIONS;
        }
        if (propositions.isEmpty()) {
            MessageUtils.showErrorMessage("Aucun alignement trouvé !");
            mode = AlignmentWorkbenchMode.SUMMARY;
        }
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :messageIndex");
    }

    private void resetPropositionsState() {
        propositions = new ArrayList<>();
        selectedProposition = null;
        propositionToValidate = null;
        activeSearchSourceId = 0;
        selectedSourceName = null;
    }

    private AlignementSource resolveActiveSearchSource() {
        if (activeSearchSourceId <= 0) {
            return null;
        }
        return conceptAlignmentAdminService.findActiveSource(
                thesaurusContext.resolveThesaurusId(), activeSearchSourceId);
    }

    private ConceptAlignment toConceptAlignment(AlignmentAdminRow row) {
        return new ConceptAlignment(
                String.valueOf(row.alignmentId()),
                row.targetUri(),
                row.typeLabel(),
                row.sourceName(),
                row.urlAvailable(),
                row.typeId()
        );
    }
}
