package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.alignment.operations.CandidatAutoAlignmentOperations;
import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
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

    private final CandidatAutoAlignmentOperations candidatAutoAlignmentOperations;
    private final ThesaurusContext thesaurusContext;

    public void prepareForCandidate(String conceptLabel, String conceptId) {
        candidatAutoAlignmentOperations.prepare(
                conceptLabel,
                conceptId,
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    public boolean hasAlignmentSources() {
        return candidatAutoAlignmentOperations.hasAlignmentSources();
    }

    public void searchAlignments() {
        candidatAutoAlignmentOperations.searchAlignments();
    }

    public void getUriAndOptions(NodeAlignment nodeAlignment) throws IOException, InterruptedException {
        candidatAutoAlignmentOperations.getUriAndOptions(nodeAlignment, thesaurusContext.resolveThesaurusId());
    }

    public void addAlignment(String conceptId, int userId) {
        candidatAutoAlignmentOperations.addAlignment(thesaurusContext.resolveThesaurusId(), conceptId, userId);
    }

    public void cancelManualAlignment() {
        candidatAutoAlignmentOperations.cancelManualAlignment();
    }

    public void actionChoix() {
        candidatAutoAlignmentOperations.actionChoix();
    }

    public boolean isNameAlignment() {
        return candidatAutoAlignmentOperations.isNameAlignment();
    }

    public String getConceptValueForAlignment() {
        return candidatAutoAlignmentOperations.getConceptValueForAlignment();
    }

    public void setConceptValueForAlignment(String value) {
        candidatAutoAlignmentOperations.setConceptValueForAlignment(value);
    }

    public String getNom() {
        return candidatAutoAlignmentOperations.getNom();
    }

    public void setNom(String value) {
        candidatAutoAlignmentOperations.setNom(value);
    }

    public String getPrenom() {
        return candidatAutoAlignmentOperations.getPrenom();
    }

    public void setPrenom(String value) {
        candidatAutoAlignmentOperations.setPrenom(value);
    }

    public String getSelectedAlignement() {
        return candidatAutoAlignmentOperations.getSelectedAlignement();
    }

    public void setSelectedAlignement(String value) {
        candidatAutoAlignmentOperations.setSelectedAlignement(value);
    }

    public String getAlertWikidata() {
        return candidatAutoAlignmentOperations.getAlertWikidata();
    }

    public List<AlignementSource> getAlignementSources() {
        return candidatAutoAlignmentOperations.getAlignementSources();
    }

    public boolean isViewResult() {
        return candidatAutoAlignmentOperations.isViewResult();
    }

    public List<NodeAlignment> getListAlignValues() {
        return candidatAutoAlignmentOperations.getListAlignValues();
    }

    public List<Map.Entry<String, String>> getAlignmentTypes() {
        return candidatAutoAlignmentOperations.getAlignmentTypes();
    }

    public int getSelectedAlignementType() {
        return candidatAutoAlignmentOperations.getSelectedAlignementType();
    }

    public void setSelectedAlignementType(int value) {
        candidatAutoAlignmentOperations.setSelectedAlignementType(value);
    }

    public String getManualAlignmentUri() {
        return candidatAutoAlignmentOperations.getManualAlignmentUri();
    }

    public void setManualAlignmentUri(String value) {
        candidatAutoAlignmentOperations.setManualAlignmentUri(value);
    }

    public boolean isViewSelection() {
        return candidatAutoAlignmentOperations.isViewSelection();
    }

    public NodeAlignment getSelectedNodeAlignment() {
        return candidatAutoAlignmentOperations.getSelectedNodeAlignment();
    }

    public List<SelectedResource> getTraductionsOfAlignment() {
        return candidatAutoAlignmentOperations.getTraductionsOfAlignment();
    }

    public List<SelectedResource> getDescriptionsOfAlignment() {
        return candidatAutoAlignmentOperations.getDescriptionsOfAlignment();
    }

    public List<SelectedResource> getImagesOfAlignment() {
        return candidatAutoAlignmentOperations.getImagesOfAlignment();
    }
}
