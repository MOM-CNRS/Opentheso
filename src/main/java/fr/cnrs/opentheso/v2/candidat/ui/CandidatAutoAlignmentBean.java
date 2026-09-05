package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import fr.cnrs.opentheso.v2.candidat.alignment.CandidatAutoAlignmentEngine;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@SessionScoped
@Named("v2CandidatAutoAlignmentBean")
@RequiredArgsConstructor
public class CandidatAutoAlignmentBean implements Serializable {

    private final transient CandidatAutoAlignmentEngine candidatAutoAlignmentEngine;
    private final transient ThesaurusContext thesaurusContext;

    public void prepareForCandidate(String conceptLabel, String conceptId) {
        candidatAutoAlignmentEngine.prepare(
                conceptLabel,
                conceptId,
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    public boolean hasAlignmentSources() {
        return candidatAutoAlignmentEngine.hasAlignmentSources();
    }

    public void searchAlignments() {
        candidatAutoAlignmentEngine.searchAlignments();
    }

    public void getUriAndOptions(NodeAlignment nodeAlignment) throws IOException, InterruptedException {
        candidatAutoAlignmentEngine.getUriAndOptions(nodeAlignment, thesaurusContext.resolveThesaurusId());
    }

    public void addAlignment(String conceptId, int userId) {
        candidatAutoAlignmentEngine.addAlignment(thesaurusContext.resolveThesaurusId(), conceptId, userId);
    }

    public void cancelManualAlignment() {
        candidatAutoAlignmentEngine.cancelManualAlignment();
    }

    public void actionChoix() {
        candidatAutoAlignmentEngine.actionChoix();
    }

    public boolean isNameAlignment() {
        return candidatAutoAlignmentEngine.isNameAlignment();
    }

    public String getConceptValueForAlignment() {
        return candidatAutoAlignmentEngine.getConceptValueForAlignment();
    }

    public void setConceptValueForAlignment(String value) {
        candidatAutoAlignmentEngine.setConceptValueForAlignment(value);
    }

    public String getNom() {
        return candidatAutoAlignmentEngine.getNom();
    }

    public void setNom(String value) {
        candidatAutoAlignmentEngine.setNom(value);
    }

    public String getPrenom() {
        return candidatAutoAlignmentEngine.getPrenom();
    }

    public void setPrenom(String value) {
        candidatAutoAlignmentEngine.setPrenom(value);
    }

    public String getSelectedAlignement() {
        return candidatAutoAlignmentEngine.getSelectedAlignement();
    }

    public void setSelectedAlignement(String value) {
        candidatAutoAlignmentEngine.setSelectedAlignement(value);
    }

    public String getAlertWikidata() {
        return candidatAutoAlignmentEngine.getAlertWikidata();
    }

    public List<AlignementSource> getAlignementSources() {
        return candidatAutoAlignmentEngine.getAlignementSources();
    }

    public boolean isViewResult() {
        return candidatAutoAlignmentEngine.isViewResult();
    }

    public List<NodeAlignment> getListAlignValues() {
        return candidatAutoAlignmentEngine.getListAlignValues();
    }

    public List<Map.Entry<String, String>> getAlignmentTypes() {
        return candidatAutoAlignmentEngine.getAlignmentTypes();
    }

    public int getSelectedAlignementType() {
        return candidatAutoAlignmentEngine.getSelectedAlignementType();
    }

    public void setSelectedAlignementType(int value) {
        candidatAutoAlignmentEngine.setSelectedAlignementType(value);
    }

    public String getManualAlignmentUri() {
        return candidatAutoAlignmentEngine.getManualAlignmentUri();
    }

    public void setManualAlignmentUri(String value) {
        candidatAutoAlignmentEngine.setManualAlignmentUri(value);
    }

    public boolean isViewSelection() {
        return candidatAutoAlignmentEngine.isViewSelection();
    }

    public NodeAlignment getSelectedNodeAlignment() {
        return candidatAutoAlignmentEngine.getSelectedNodeAlignment();
    }

    public List<SelectedResource> getTraductionsOfAlignment() {
        return candidatAutoAlignmentEngine.getTraductionsOfAlignment();
    }

    public List<SelectedResource> getDescriptionsOfAlignment() {
        return candidatAutoAlignmentEngine.getDescriptionsOfAlignment();
    }

    public List<SelectedResource> getImagesOfAlignment() {
        return candidatAutoAlignmentEngine.getImagesOfAlignment();
    }
}
