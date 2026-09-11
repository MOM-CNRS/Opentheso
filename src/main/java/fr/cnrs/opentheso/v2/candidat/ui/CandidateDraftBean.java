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
    private String scopeNote = "";
    private String alternatives = "";
    private String hiddenForms = "";
    private String collectionIds = "";
    private String broaderTerm = "";
    private String narrowerTerms = "";
    private String relatedTerms = "";
    private String translationEn = "";
    private String translationDe = "";
    private String translationEs = "";
    private String translationIt = "";
    private String createdConceptId;
    private boolean created;

    public boolean isCanCreate() {
        return candidatAccessPolicy.canCreate(userSession, thesaurusViewBean.getId());
    }

    public void reset() {
        title = "";
        definition = "";
        scopeNote = "";
        alternatives = "";
        hiddenForms = "";
        collectionIds = "";
        broaderTerm = "";
        narrowerTerms = "";
        relatedTerms = "";
        translationEn = "";
        translationDe = "";
        translationEs = "";
        translationIt = "";
        createdConceptId = null;
        created = false;
    }

    public void cancel() {
        reset();
    }

    /**
     * Persiste le candidat draft. Appelé depuis le formulaire JSF.
     */
    public void create() {
        created = false;
        createdConceptId = null;
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

        if (StringUtils.isNotBlank(scopeNote)) {
            candidatMutationService.addOrUpdateCandidateNote(
                    candidat.getIdConcepte(),
                    lang,
                    thesaurusId,
                    scopeNote.trim(),
                    "scopeNote",
                    "",
                    userId);
        }
        for (TraductionDto traduction : buildTranslations()) {
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

        // Redirect vers la fiche candidat créée
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

    private List<TraductionDto> buildTranslations() {
        List<TraductionDto> traductions = new ArrayList<>();
        addTranslation(traductions, "en", translationEn);
        addTranslation(traductions, "de", translationDe);
        addTranslation(traductions, "es", translationEs);
        addTranslation(traductions, "it", translationIt);
        return traductions;
    }

    private void addTranslation(List<TraductionDto> list, String lang, String value) {
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
