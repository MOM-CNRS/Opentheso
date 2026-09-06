package fr.cnrs.opentheso.v2.concept.alignment.ui;

import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentProposition;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;
import fr.cnrs.opentheso.v2.concept.alignment.service.ConceptAlignmentAdminService;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignmentGroup;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteAlignmentType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Recherche automatique d'alignements sur la fiche concept (équivalent V1
 * {@code SetAlignmentSourceBean#initAlignementAutomatique} pour le concept courant).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptAlignmentAutoSearchBean")
@RequiredArgsConstructor
public class ConceptAlignmentAutoSearchBean implements Serializable {

    static final String FICHE_CARD = "alignement";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptAlignmentAdminService conceptAlignmentAdminService;
    private final transient ConceptAlignmentMutationService conceptAlignmentMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;

    @Getter(AccessLevel.NONE)
    private boolean open;
    private String editingConceptId;
    private List<AlignmentSourceItem> sources = new ArrayList<>();
    private int selectedSourceId;
    private String selectedSourceName;
    private List<AlignmentProposition> propositions = new ArrayList<>();
    private List<ConceptWriteAlignmentType> alignmentTypes = new ArrayList<>();
    private String errorMessage;
    private String statusMessage;
    private String flashMessage;
    private String flashToken;
    private String alignmentToDeleteId;
    private String mode = MODE_SEARCH;
    private String propositionToReplaceIndex;

    static final String MODE_SEARCH = "search";
    static final String MODE_COMPARE = "compare";

    public boolean isAvailable() {
        return thesaurusViewBean.getSelectedConcept() != null
                && conceptWritePolicy.canMutateAlignments(
                        userSession, thesaurusViewBean.isSelectedConceptDeprecated());
    }

    public boolean isOpen() {
        if (open && !matchesCurrentConcept()) {
            reset(false);
        }
        return open && FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard());
    }

    public boolean isSourceSelected() {
        return selectedSourceId > 0;
    }

    public boolean isComparing() {
        return MODE_COMPARE.equals(mode);
    }

    public void startSearching() {
        startFlow(MODE_SEARCH);
    }

    public void startComparing() {
        startFlow(MODE_COMPARE);
    }

    private void startFlow(String nextMode) {
        if (!isAvailable()) {
            return;
        }
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return;
        }
        editingConceptId = detail.getSummary().getConceptId();
        List<ConceptWriteAlignmentType> types = conceptAlignmentMutationService.listAlignmentTypes();
        alignmentTypes = types == null
                ? List.of()
                : types.stream()
                        .sorted(java.util.Comparator.comparingInt(ConceptWriteAlignmentType::getId))
                        .toList();
        sources = toItems(conceptAlignmentAdminService.listActiveSources(thesaurusViewBean.getId()));
        selectedSourceId = 0;
        selectedSourceName = "";
        propositions = new ArrayList<>();
        errorMessage = "";
        statusMessage = "";
        flashMessage = "";
        flashToken = "";
        propositionToReplaceIndex = "";
        mode = nextMode;
        open = true;
        thesaurusViewBean.setFicheEditCard(FICHE_CARD);
        conceptSelectionContext.update(thesaurusViewBean.getId(), detail);
        if (sources.isEmpty()) {
            errorMessage = "Aucune source d'alignement n'est activée pour ce thésaurus.";
            return;
        }
        if (sources.size() == 1) {
            selectSource(sources.get(0).getSourceId());
        }
    }

    public void cancel() {
        reset(false);
    }

    public void selectSource(int sourceId) {
        selectSource(sourceId, true);
    }

    private void selectSource(int sourceId, boolean announce) {
        if (!isOpen()) {
            return;
        }
        AlignementSource source = conceptAlignmentAdminService.findActiveSource(
                thesaurusViewBean.getId(), sourceId);
        if (source == null) {
            errorMessage = "Veuillez sélectionner une source !";
            return;
        }
        selectedSourceId = source.getId();
        selectedSourceName = StringUtils.defaultString(source.getSource());
        errorMessage = "";
        statusMessage = "";
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            errorMessage = "Le concept n'a pas de libellé à interroger.";
            propositions = new ArrayList<>();
            return;
        }
        String label = StringUtils.defaultString(detail.getSummary().getPreferredLabel());
        if (StringUtils.isBlank(label)) {
            errorMessage = "Le concept n'a pas de libellé à interroger.";
            propositions = new ArrayList<>();
            return;
        }
        if (isComparing()) {
            List<ConceptAlignment> existing = conceptAlignmentAdminService.alignmentsTowardSource(
                    currentAlignments(), source);
            if (existing.isEmpty()) {
                errorMessage = "Aucun alignement existant vers cette source.";
                propositions = new ArrayList<>();
                return;
            }
            propositions = new ArrayList<>(conceptAlignmentAdminService.searchComparisonsForConcept(
                    thesaurusViewBean.getId(),
                    detail.getSummary().getLang(),
                    editingConceptId,
                    label,
                    localDefinition(detail),
                    existing,
                    source
            ));
        } else {
            propositions = new ArrayList<>(conceptAlignmentAdminService.searchPropositionsForConcept(
                    thesaurusViewBean.getId(),
                    detail.getSummary().getLang(),
                    editingConceptId,
                    label,
                    source
            ));
            markExisting();
        }
        markSearchDone(announce);
    }

    private void markSearchDone(boolean announce) {
        int count = propositions.size();
        String source = StringUtils.defaultString(selectedSourceName);
        String head;
        if (isComparing()) {
            head = count == 0
                    ? "Comparaison terminée · aucun résultat"
                    : "Comparaison terminée · " + count + " résultat(s)";
        } else {
            head = count == 0
                    ? "Recherche terminée · aucun résultat"
                    : "Recherche terminée · " + count + " résultat(s)";
        }
        statusMessage = StringUtils.isBlank(source) ? head : head + " · " + source;
        if (announce) {
            flashMessage = statusMessage;
            flashToken = String.valueOf(System.currentTimeMillis());
        }
        if (count == 0 && StringUtils.isBlank(errorMessage)) {
            errorMessage = "Aucun alignement trouvé !";
        }
    }

    public void addProposition(int index) {
        if (!isAvailable() || !isOpen() || index < 0 || index >= propositions.size()) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        AlignmentProposition proposition = propositions.get(index);
        if (proposition.isAlreadyAligned()) {
            return;
        }
        boolean ok = conceptAlignmentAdminService.acceptProposition(
                thesaurusViewBean.getId(),
                proposition,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        if (!ok) {
            errorMessage = "L'ajout de l'alignement a échoué !";
            return;
        }
        errorMessage = "";
        flashMessage = "Alignement ajouté";
        flashToken = String.valueOf(System.currentTimeMillis());
        thesaurusViewBean.reloadSelectedConcept();
        conceptSelectionContext.update(thesaurusViewBean.getId(), thesaurusViewBean.getSelectedConcept());
        markExisting();
    }

    public void replaceProposition() {
        if (!isAvailable() || !isOpen() || !isComparing()) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        int index = parseAlignmentId(propositionToReplaceIndex);
        propositionToReplaceIndex = "";
        if (index < 0 || index >= propositions.size()) {
            return;
        }
        AlignmentProposition proposition = propositions.get(index);
        if (proposition.isAlreadyAligned()) {
            return;
        }
        boolean ok = conceptAlignmentAdminService.replaceAlignmentFromProposition(
                thesaurusViewBean.getId(),
                proposition,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        if (!ok) {
            errorMessage = "Le remplacement de l'alignement a échoué !";
            return;
        }
        errorMessage = "";
        flashMessage = "Alignement remplacé";
        flashToken = String.valueOf(System.currentTimeMillis());
        thesaurusViewBean.reloadSelectedConcept();
        conceptSelectionContext.update(thesaurusViewBean.getId(), thesaurusViewBean.getSelectedConcept());
        selectSource(selectedSourceId, false);
        flashMessage = "Alignement remplacé";
        flashToken = String.valueOf(System.currentTimeMillis());
    }

    public void deleteAlignment() {
        if (!isAvailable()) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            return;
        }
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return;
        }
        int alignmentId = parseAlignmentId(alignmentToDeleteId);
        alignmentToDeleteId = "";
        if (alignmentId <= 0) {
            return;
        }
        MutationResult result = conceptAlignmentMutationService.deleteAlignment(
                new DeleteAlignmentCommand(
                        thesaurusViewBean.getId(),
                        detail.getSummary().getConceptId(),
                        alignmentId,
                        userId,
                        StringUtils.defaultString(userSession.getCurrentUsername())
                )
        );
        if (result == null || !result.success()) {
            flashMessage = result != null ? result.message() : "La suppression de l'alignement a échoué !";
            flashToken = String.valueOf(System.currentTimeMillis());
            return;
        }
        flashMessage = "Alignement supprimé";
        flashToken = String.valueOf(System.currentTimeMillis());
        thesaurusViewBean.reloadSelectedConcept();
        conceptSelectionContext.update(thesaurusViewBean.getId(), thesaurusViewBean.getSelectedConcept());
        markExisting();
    }

    public void selectPropositionType(int index, int typeId) {
        if (!isOpen() || index < 0 || index >= propositions.size() || typeId <= 0) {
            return;
        }
        propositions.get(index).setAlignmentTypeId(typeId);
    }

    public boolean isSelectedSource(int sourceId) {
        return selectedSourceId == sourceId;
    }

    public String sourceKind(AlignmentSourceItem item) {
        if (item == null) {
            return "";
        }
        if (StringUtils.isNotBlank(item.getDescription())) {
            return item.getDescription();
        }
        return StringUtils.defaultString(item.getSourceType()).replace('_', ' ');
    }

    public String getSearchLabel() {
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return "";
        }
        return StringUtils.defaultString(detail.getSummary().getPreferredLabel());
    }

    public String propositionTypeKey(AlignmentProposition proposition) {
        int typeId = proposition == null ? 1 : proposition.getAlignmentTypeId();
        return typeKey(typeId);
    }

    public String typeKey(int typeId) {
        return switch (typeId) {
            case 2 -> "closeMatch";
            case 3 -> "broadMatch";
            case 4 -> "relatedMatch";
            case 5 -> "narrowMatch";
            default -> "exactMatch";
        };
    }

    public String typeMessageKey(String typeKey) {
        String key = StringUtils.defaultString(typeKey).trim();
        if (key.startsWith("skos:")) {
            key = key.substring(5);
        }
        return switch (key.toLowerCase()) {
            case "closematch", "close" -> "v2.concept.alignment.type.close";
            case "broadmatch", "broad" -> "v2.concept.alignment.type.broad";
            case "relatedmatch", "related" -> "v2.concept.alignment.type.related";
            case "narrowmatch", "narrow" -> "v2.concept.alignment.type.narrow";
            default -> "v2.concept.alignment.type.exact";
        };
    }

    public String typeMessageKey(int typeId) {
        return typeMessageKey(typeKey(typeId));
    }

    private void markExisting() {
        Set<String> uris = existingUris();
        for (AlignmentProposition proposition : propositions) {
            String uri = StringUtils.trimToEmpty(proposition.getTargetUri());
            proposition.setAlreadyAligned(!uri.isEmpty() && uris.contains(uri));
        }
    }

    private Set<String> existingUris() {
        Set<String> uris = new LinkedHashSet<>();
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getAlignmentGroups() == null) {
            return uris;
        }
        for (ConceptAlignmentGroup group : detail.getAlignmentGroups()) {
            if (group == null || group.items() == null) {
                continue;
            }
            for (ConceptAlignment alignment : group.items()) {
                String uri = StringUtils.trimToEmpty(alignment.uri());
                if (!uri.isEmpty()) {
                    uris.add(uri);
                }
            }
        }
        return uris;
    }

    private void reset(boolean keepFlash) {
        open = false;
        if (FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        editingConceptId = null;
        sources = new ArrayList<>();
        selectedSourceId = 0;
        selectedSourceName = "";
        propositions = new ArrayList<>();
        alignmentTypes = new ArrayList<>();
        errorMessage = "";
        statusMessage = "";
        mode = MODE_SEARCH;
        propositionToReplaceIndex = "";
        if (!keepFlash) {
            flashMessage = "";
            flashToken = "";
        }
    }

    private boolean matchesCurrentConcept() {
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return false;
        }
        return StringUtils.equals(editingConceptId, detail.getSummary().getConceptId());
    }

    private List<ConceptAlignment> currentAlignments() {
        List<ConceptAlignment> alignments = new ArrayList<>();
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getAlignmentGroups() == null) {
            return alignments;
        }
        for (ConceptAlignmentGroup group : detail.getAlignmentGroups()) {
            if (group == null || group.items() == null) {
                continue;
            }
            alignments.addAll(group.items());
        }
        return alignments;
    }

    private static String localDefinition(ConceptDetail detail) {
        if (detail == null || detail.getNotes() == null || detail.getSummary() == null) {
            return "";
        }
        String lang = StringUtils.defaultString(detail.getSummary().getLang());
        String fallback = "";
        for (var note : detail.getNotes()) {
            if (note == null || !"definition".equalsIgnoreCase(note.typeCode())) {
                continue;
            }
            if (StringUtils.isBlank(note.value())) {
                continue;
            }
            if (StringUtils.equalsIgnoreCase(lang, note.lang())) {
                return note.value();
            }
            if (fallback.isEmpty()) {
                fallback = note.value();
            }
        }
        return fallback;
    }

    private static int parseAlignmentId(String rawId) {
        if (StringUtils.isBlank(rawId) || !StringUtils.isNumeric(rawId.trim())) {
            return -1;
        }
        return Integer.parseInt(rawId.trim());
    }

    private static List<AlignmentSourceItem> toItems(List<AlignementSource> active) {
        List<AlignmentSourceItem> items = new ArrayList<>();
        if (active == null) {
            return items;
        }
        for (AlignementSource source : active) {
            items.add(new AlignmentSourceItem(
                    source.getId(),
                    source.getSource(),
                    StringUtils.defaultString(source.getDescription()),
                    false,
                    false,
                    StringUtils.defaultString(source.getSource_filter()),
                    StringUtils.defaultString(source.getRequete())
            ));
        }
        return items;
    }
}
