package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.bean.alignment.AlignmentBean;
import fr.cnrs.opentheso.bean.alignment.AlignmentManualBean;
import fr.cnrs.opentheso.v2.candidat.session.CandidatAlignmentSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class LegacyCandidatAlignmentSupport implements CandidatAlignmentSupport {

    private final ObjectProvider<AlignmentBean> alignmentBeanProvider;
    private final ObjectProvider<AlignmentManualBean> alignmentManualBeanProvider;

    public LegacyCandidatAlignmentSupport(
            ObjectProvider<AlignmentBean> alignmentBeanProvider,
            ObjectProvider<AlignmentManualBean> alignmentManualBeanProvider) {
        this.alignmentBeanProvider = alignmentBeanProvider;
        this.alignmentManualBeanProvider = alignmentManualBeanProvider;
    }

    @Override
    public void resetManualAlignment() {
        alignmentManualBeanProvider.getObject().reset();
    }

    @Override
    public void prepareAutoAlignment(String conceptLabel, String conceptId, String thesaurusId, String lang) {
        var alignmentBean = alignmentBeanProvider.getObject();
        alignmentBean.setConceptValueForAlignment(conceptLabel);
        alignmentBean.setExistingAlignment(conceptId, thesaurusId);
        alignmentBean.prepareValuesForIdRef();
        alignmentBean.setListAlignValues(null);
        alignmentBean.initAlignmentSources(thesaurusId, lang);
        alignmentBean.setIdConceptSelectedForAlignment(conceptId);
    }

    @Override
    public boolean hasAlignmentSources() {
        return !CollectionUtils.isEmpty(alignmentBeanProvider.getObject().getAlignementSources());
    }

    @Override
    public void addAlignment(String thesaurusId, String conceptId, int userId) {
        var alignmentBean = alignmentBeanProvider.getObject();
        alignmentBean.addAlignment(thesaurusId, alignmentBean.getIdConceptSelectedForAlignment(), userId, false);
    }
}
