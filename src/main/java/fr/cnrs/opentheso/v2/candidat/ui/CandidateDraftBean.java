package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.TraductionDto;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.policy.CandidatAccessPolicy;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusLanguage;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Création d'un candidat depuis le formulaire draft V2 (board candidats).
 */
@Named("v2CandidateDraftBean")
@ViewScoped
@RequiredArgsConstructor
@Getter
@Setter
public class CandidateDraftBean implements Serializable {

    private final transient CandidatMutationService candidatMutationService;
    private final transient CandidatAccessPolicy candidatAccessPolicy;
    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ThesaurusPreferencesProvider thesaurusPreferencesProvider;
    private final transient UserSession userSession;
    private final transient V2LocaleBean localeBean;
    private final transient CandidatBoardBean candidatBoardBean;

    private String title = "";
    private String definition = "";
    private String notesPayload = "";
    private String alternatives = "";
    private String hiddenForms = "";
    private String collectionIds = "";
    private String broaderTerm = "";
    private String narrowerTerms = "";
    private String relatedTerms = "";
    private String translationsPayload = "";
    private String createdConceptId;
    private boolean created;
    private boolean chainNext;
    /** {@code create} ou {@code chain} — posé par le formulaire avant soumission. */
    private String createMode = "create";

    public boolean isCanCreate() {
        return candidatAccessPolicy.canCreate(userSession, thesaurusViewBean.getId());
    }

    public List<ThesaurusLanguage> getTranslationLanguages() {
        return thesaurusViewBean.getLanguages().stream()
                .filter(lang -> !thesaurusViewBean.currentWorkLangIs(lang.code()))
                .toList();
    }

    public List<String> getDraftNoteTypes() {
        return List.of("scopeNote", "example", "historyNote", "editorialNote", "changeNote");
    }

    public void reset() {
        resetFields();
        broaderTerm = "";
        createdConceptId = null;
        created = false;
        chainNext = false;
        createMode = "create";
    }

    private void resetFields() {
        title = "";
        definition = "";
        notesPayload = "";
        alternatives = "";
        hiddenForms = "";
        collectionIds = "";
        narrowerTerms = "";
        relatedTerms = "";
        translationsPayload = "";
    }

    public void cancel() {
        reset();
    }

    public void create() {
        persist("chain".equalsIgnoreCase(StringUtils.trimToEmpty(createMode)));
    }

    public void createAndContinue() {
        createMode = "chain";
        persist(true);
    }

    /**
     * Persiste le candidat draft. Appelé depuis le formulaire JSF.
     */
    private void persist(boolean chain) {
        created = false;
        createdConceptId = null;
        chainNext = chain;
        String thesaurusId = thesaurusViewBean.getId();
        if (!candidatAccessPolicy.canCreate(userSession, thesaurusId)) {
            MessageUtils.showErrorMessage(localeBean.getMsg("v2.candidat.draft.unauthorized"));
            return;
        }
        if (StringUtils.isBlank(title)) {
            MessageUtils.showWarnMessage(localeBean.getMsg("v2.candidat.draft.titleRequired"));
            return;
        }
        if (thesaurusPreferencesProvider.findPreferences(thesaurusId).isEmpty()) {
            MessageUtils.showWarnMessage(localeBean.getMsg("candidat.save.msg2"));
            return;
        }

        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage(localeBean.getMsg("v2.candidat.draft.unauthorized"));
            return;
        }

        String lang = StringUtils.defaultIfBlank(
                thesaurusViewBean.getSelectedLang(),
                thesaurusContext.resolveWorkLanguage());

        CandidatDto candidat = new CandidatDto();
        candidat.setNomPref(title.trim());
        candidat.setIdThesaurus(thesaurusId);
        candidat.setLang(lang);
        candidat.setCollections(parseCollectionIds(collectionIds));
        candidat.setEmployePourList(splitCsv(alternatives));
        candidat.setTermesGenerique(resolveRelationIds(broaderTerm, lang, thesaurusId));
        candidat.setTermesAssocies(resolveRelationIds(relatedTerms, lang, thesaurusId));

        String def = StringUtils.defaultIfBlank(definition, "").trim();
        if (!candidatMutationService.saveNewCandidat(
                candidat,
                thesaurusId,
                lang,
                userId,
                userSession.getCurrentUsername(),
                thesaurusContext.resolveWorkLanguage(),
                def)) {
            return;
        }

        candidatMutationService.saveContributorMetadata(
                candidat.getIdConcepte(), thesaurusId, userSession.getCurrentUsername());
        candidatMutationService.updateCandidateDetails(candidat);

        for (DraftNote note : parseNotesPayload(notesPayload)) {
            candidatMutationService.addOrUpdateCandidateNote(
                    candidat.getIdConcepte(),
                    note.lang(),
                    thesaurusId,
                    note.value(),
                    note.type(),
                    note.source(),
                    userId);
        }
        for (TraductionDto traduction : parseTranslationsPayload(translationsPayload)) {
            candidatMutationService.addCandidateTranslation(
                    fr.cnrs.opentheso.models.terms.Term.builder()
                            .lang(traduction.getLangue())
                            .idThesaurus(thesaurusId)
                            .contributor(userId)
                            .lexicalValue(traduction.getTraduction())
                            .source("candidat")
                            .status("D")
                            .idTerm(candidat.getIdTerm())
                            .build(),
                    userId);
        }

        createdConceptId = candidat.getIdConcepte();
        created = true;
        candidatBoardBean.load(thesaurusId);
        MessageUtils.showInformationMessage(localeBean.getMsg("v2.candidat.draft.created"));

        if (chain) {
            String keepBt = broaderTerm;
            resetFields();
            broaderTerm = keepBt;
            createMode = "create";
            return;
        }

        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces != null && StringUtils.isNotBlank(createdConceptId)) {
            try {
                String ctx = faces.getExternalContext().getRequestContextPath();
                faces.getExternalContext().redirect(
                        ctx + "/v2/thesaurus/consultation.xhtml?id=" + createdConceptId
                                + "&type=candidat&from=candidats");
            } catch (Exception ex) {
                // reste sur le board si redirect échoue
            }
        }
        reset();
    }

    private List<NodeIdValue> parseCollectionIds(String csv) {
        List<NodeIdValue> result = new ArrayList<>();
        for (String id : splitCsv(csv)) {
            NodeIdValue node = new NodeIdValue();
            node.setId(id);
            node.setValue(id);
            result.add(node);
        }
        return result;
    }

    private List<NodeIdValue> resolveRelationIds(String raw, String lang, String thesaurusId) {
        List<NodeIdValue> result = new ArrayList<>();
        for (String token : splitCsv(raw)) {
            // Si l'utilisateur a saisi un id concept connu, on l'utilise ; sinon recherche par libellé.
            List<NodeIdValue> found = candidatMutationService.searchRelationTerms(token, lang, thesaurusId);
            if (found != null && !found.isEmpty()) {
                result.add(found.get(0));
            } else {
                NodeIdValue node = new NodeIdValue();
                node.setId(token);
                node.setValue(token);
                result.add(node);
            }
        }
        return result;
    }

    static List<TraductionDto> parseTranslationsPayload(String payload) {
        List<TraductionDto> traductions = new ArrayList<>();
        if (StringUtils.isBlank(payload)) {
            return traductions;
        }
        for (String line : payload.split("\\R")) {
            int tab = line.indexOf('\t');
            if (tab <= 0) {
                continue;
            }
            addTranslation(traductions, line.substring(0, tab).trim(), line.substring(tab + 1));
        }
        return traductions;
    }

    static List<DraftNote> parseNotesPayload(String payload) {
        List<DraftNote> notes = new ArrayList<>();
        if (StringUtils.isBlank(payload)) {
            return notes;
        }
        for (String line : payload.split("\\R")) {
            String[] parts = line.split("\\t", 4);
            if (parts.length < 3) {
                continue;
            }
            String type = StringUtils.trimToEmpty(parts[0]);
            String lang = StringUtils.trimToEmpty(parts[1]);
            String value = decodeNotePart(parts[2]);
            String source = parts.length > 3 ? decodeNotePart(parts[3]) : "";
            if (StringUtils.isAnyBlank(type, lang, value)) {
                continue;
            }
            notes.add(new DraftNote(type, lang, value, source));
        }
        return notes;
    }

    private static String decodeNotePart(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException ex) {
            return raw.trim();
        }
    }

    record DraftNote(String type, String lang, String value, String source) {
    }

    private static void addTranslation(List<TraductionDto> list, String lang, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        list.add(TraductionDto.builder()
                .langue(lang)
                .traduction(value.trim())
                .build());
    }

    private static List<String> splitCsv(String raw) {
        if (StringUtils.isBlank(raw)) {
            return new ArrayList<>();
        }
        return Arrays.stream(raw.split("[,;]"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }
}
