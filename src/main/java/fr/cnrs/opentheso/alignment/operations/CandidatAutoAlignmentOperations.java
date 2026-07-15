package fr.cnrs.opentheso.alignment.operations;

import fr.cnrs.opentheso.bean.alignment.AlignmentBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.models.alignment.AlignementSource;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.alignment.SelectedResource;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CandidatAutoAlignmentOperations {

    private final ObjectProvider<AlignmentBean> alignmentBeanProvider;
    private final ObjectProvider<SelectedTheso> selectedThesoProvider;

    public void prepare(String conceptLabel, String conceptId, String thesaurusId, String lang) {
        syncThesaurus(thesaurusId, lang);
        AlignmentBean bean = alignmentBeanProvider.getObject();
        bean.setConceptValueForAlignment(conceptLabel);
        bean.setExistingAlignment(conceptId, thesaurusId);
        bean.prepareValuesForIdRef();
        bean.setListAlignValues(null);
        bean.initAlignmentSources(thesaurusId, lang);
        bean.setIdConceptSelectedForAlignment(conceptId);
    }

    public boolean hasAlignmentSources() {
        return CollectionUtils.isNotEmpty(alignmentBeanProvider.getObject().getAlignementSources());
    }

    public void searchAlignments() {
        alignmentBeanProvider.getObject().searchAlignments(null);
    }

    public void getUriAndOptions(NodeAlignment selectedNodeAlignment, String thesaurusId)
            throws IOException, InterruptedException {
        syncThesaurus(thesaurusId, null);
        alignmentBeanProvider.getObject().getUriAndOptions(selectedNodeAlignment, thesaurusId);
    }

    public void addAlignment(String thesaurusId, String conceptId, int userId) {
        syncThesaurus(thesaurusId, null);
        alignmentBeanProvider.getObject().addAlignment(thesaurusId, conceptId, userId, false);
    }

    public void cancelManualAlignment() {
        alignmentBeanProvider.getObject().cancelManualAlignment();
    }

    public void actionChoix() {
        alignmentBeanProvider.getObject().actionChoix();
    }

    public boolean isNameAlignment() {
        return alignmentBeanProvider.getObject().isNameAlignment();
    }

    public String getConceptValueForAlignment() {
        return alignmentBeanProvider.getObject().getConceptValueForAlignment();
    }

    public void setConceptValueForAlignment(String value) {
        alignmentBeanProvider.getObject().setConceptValueForAlignment(value);
    }

    public String getNom() {
        return alignmentBeanProvider.getObject().getNom();
    }

    public void setNom(String value) {
        alignmentBeanProvider.getObject().setNom(value);
    }

    public String getPrenom() {
        return alignmentBeanProvider.getObject().getPrenom();
    }

    public void setPrenom(String value) {
        alignmentBeanProvider.getObject().setPrenom(value);
    }

    public String getSelectedAlignement() {
        return alignmentBeanProvider.getObject().getSelectedAlignement();
    }

    public void setSelectedAlignement(String value) {
        alignmentBeanProvider.getObject().setSelectedAlignement(value);
    }

    public String getAlertWikidata() {
        return alignmentBeanProvider.getObject().getAlertWikidata();
    }

    public List<AlignementSource> getAlignementSources() {
        return alignmentBeanProvider.getObject().getAlignementSources();
    }

    public boolean isViewResult() {
        return alignmentBeanProvider.getObject().isViewResult();
    }

    public List<NodeAlignment> getListAlignValues() {
        return alignmentBeanProvider.getObject().getListAlignValues();
    }

    public List<Map.Entry<String, String>> getAlignmentTypes() {
        return alignmentBeanProvider.getObject().getAlignmentTypes();
    }

    public int getSelectedAlignementType() {
        return alignmentBeanProvider.getObject().getSelectedAlignementType();
    }

    public void setSelectedAlignementType(int value) {
        alignmentBeanProvider.getObject().setSelectedAlignementType(value);
    }

    public String getManualAlignmentUri() {
        return alignmentBeanProvider.getObject().getManualAlignmentUri();
    }

    public void setManualAlignmentUri(String value) {
        alignmentBeanProvider.getObject().setManualAlignmentUri(value);
    }

    public boolean isViewSelection() {
        return alignmentBeanProvider.getObject().isViewSelection();
    }

    public NodeAlignment getSelectedNodeAlignment() {
        return alignmentBeanProvider.getObject().getSelectedNodeAlignment();
    }

    public List<SelectedResource> getTraductionsOfAlignment() {
        return alignmentBeanProvider.getObject().getTraductionsOfAlignment();
    }

    public List<SelectedResource> getDescriptionsOfAlignment() {
        return alignmentBeanProvider.getObject().getDescriptionsOfAlignment();
    }

    public List<SelectedResource> getImagesOfAlignment() {
        return alignmentBeanProvider.getObject().getImagesOfAlignment();
    }

    private void syncThesaurus(String thesaurusId, String lang) {
        SelectedTheso selectedTheso = selectedThesoProvider.getObject();
        selectedTheso.setSelectedIdTheso(thesaurusId);
        selectedTheso.setCurrentIdTheso(thesaurusId);
        if (lang != null) {
            selectedTheso.setSelectedLang(lang);
            selectedTheso.setCurrentLang(lang);
        }
    }
}
