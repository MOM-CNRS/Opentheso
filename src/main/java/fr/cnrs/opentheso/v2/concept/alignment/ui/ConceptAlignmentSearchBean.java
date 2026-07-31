package fr.cnrs.opentheso.v2.concept.alignment.ui;

import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.alignment.CandidatAutoAlignmentEngine;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Recherche d'alignement externe (par concept) pour l'atelier V2 —
 * même moteur que les candidats.
 */
@ViewScoped
@Named("v2ConceptAlignmentSearchBean")
@RequiredArgsConstructor
public class ConceptAlignmentSearchBean implements Serializable {

    private final CandidatAutoAlignmentEngine engine;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptNavigationSupport conceptNavigationSupport;

    public void prepare(String conceptId, String conceptLabel) {
        engine.prepare(
                conceptLabel,
                conceptId,
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    public void searchAlignments() {
        engine.searchAlignments();
    }

    public void getUriAndOptions(NodeAlignment nodeAlignment) throws IOException, InterruptedException {
        engine.getUriAndOptions(nodeAlignment, thesaurusContext.resolveThesaurusId());
    }

    public void addAlignment() {
        Integer userId = userSession.getCurrentUserId();
        String conceptId = engine.getIdConceptSelectedForAlignment();
        if (userId == null || StringUtils.isBlank(conceptId)) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        engine.addAlignment(thesaurusContext.resolveThesaurusId(), conceptId, userId);
        MessageUtils.showInformationMessage("Alignement ajouté avec succès");
        conceptNavigationSupport.openConcept(conceptId);
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :messageIndex");
        PrimeFaces.current().executeScript("PF('v2ConceptSearchAlignement').hide();");
    }

    public void addManualAlignment() {
        if (StringUtils.isBlank(engine.getManualAlignmentUri())) {
            MessageUtils.showWarnMessage("L'URI cible est obligatoire !");
            return;
        }
        var fake = new NodeAlignment();
        fake.setUri_target(engine.getManualAlignmentUri());
        fake.setConcept_target("");
        fake.setThesaurus_target(StringUtils.defaultString(engine.getSelectedAlignement()));
        fake.setAlignement_id_type(engine.getSelectedAlignementType());
        engine.setSelectedNodeAlignment(fake);
        engine.getAlignementSources().stream()
                .filter(s -> s.getSource().equalsIgnoreCase(engine.getSelectedAlignement()))
                .findFirst()
                .ifPresentOrElse(
                        engine::setSelectedAlignementSource,
                        () -> engine.setSelectedAlignementSource(AlignementSource.builder()
                                .id(0)
                                .source(StringUtils.defaultString(engine.getSelectedAlignement()))
                                .build())
                );
        engine.setTraductionsOfAlignment(List.of());
        engine.setDescriptionsOfAlignment(List.of());
        engine.setImagesOfAlignment(List.of());
        addAlignment();
    }

    public void cancelManualAlignment() {
        engine.cancelManualAlignment();
    }

    public void actionChoix() {
        engine.actionChoix();
    }

    public boolean hasAlignmentSources() {
        return engine.hasAlignmentSources();
    }

    public boolean isNameAlignment() {
        return engine.isNameAlignment();
    }

    public String getConceptValueForAlignment() {
        return engine.getConceptValueForAlignment();
    }

    public void setConceptValueForAlignment(String value) {
        engine.setConceptValueForAlignment(value);
    }

    public String getNom() {
        return engine.getNom();
    }

    public void setNom(String value) {
        engine.setNom(value);
    }

    public String getPrenom() {
        return engine.getPrenom();
    }

    public void setPrenom(String value) {
        engine.setPrenom(value);
    }

    public String getSelectedAlignement() {
        return engine.getSelectedAlignement();
    }

    public void setSelectedAlignement(String value) {
        engine.setSelectedAlignement(value);
    }

    public String getAlertWikidata() {
        return engine.getAlertWikidata();
    }

    public List<AlignementSource> getAlignementSources() {
        return engine.getAlignementSources();
    }

    public boolean isViewResult() {
        return engine.isViewResult();
    }

    public List<NodeAlignment> getListAlignValues() {
        return engine.getListAlignValues();
    }

    public List<Map.Entry<String, String>> getAlignmentTypes() {
        return engine.getAlignmentTypes();
    }

    public int getSelectedAlignementType() {
        return engine.getSelectedAlignementType();
    }

    public void setSelectedAlignementType(int value) {
        engine.setSelectedAlignementType(value);
    }

    public String getManualAlignmentUri() {
        return engine.getManualAlignmentUri();
    }

    public void setManualAlignmentUri(String value) {
        engine.setManualAlignmentUri(value);
    }

    public boolean isViewSelection() {
        return engine.isViewSelection();
    }

    public NodeAlignment getSelectedNodeAlignment() {
        return engine.getSelectedNodeAlignment();
    }

    public List<SelectedResource> getTraductionsOfAlignment() {
        return engine.getTraductionsOfAlignment();
    }

    public List<SelectedResource> getDescriptionsOfAlignment() {
        return engine.getDescriptionsOfAlignment();
    }

    public List<SelectedResource> getImagesOfAlignment() {
        return engine.getImagesOfAlignment();
    }

    public boolean isWithLang() {
        return engine.isWithLang();
    }

    public void setWithLang(boolean value) {
        engine.setWithLang(value);
    }

    public boolean isWithNote() {
        return engine.isWithNote();
    }

    public void setWithNote(boolean value) {
        engine.setWithNote(value);
    }

    public boolean isWithImage() {
        return engine.isWithImage();
    }

    public void setWithImage(boolean value) {
        engine.setWithImage(value);
    }
}
