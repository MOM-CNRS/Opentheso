package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.bean.leftbody.viewgroups.TreeGroups;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.bean.rightbody.viewgroup.GroupView;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusLegacySync;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.io.Serializable;

@SessionScoped
@Named("consultationVersionSwitchSupport")
@RequiredArgsConstructor
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ConsultationVersionSwitchSupport implements Serializable {

    private final ThesaurusContext thesaurusContext;
    private final ThesaurusLegacySync legacyThesaurusSync;
    private final LegacyConceptSync legacyConceptSync;
    private final SelectedTheso selectedTheso;
    private final ConceptView conceptView;
    private final GroupView groupView;
    private final TreeGroups treeGroups;
    private final ConceptSelectionContext conceptSelectionContext;

    public void syncLegacyFromV2(ThesaurusBrowseBean browseBean) {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isBlank(thesaurusId)) {
            return;
        }
        String language = thesaurusContext.resolveWorkLanguage();
        legacyThesaurusSync.applyThesaurusId(thesaurusId, language);

        String conceptId = resolveV2ConceptId(browseBean);
        if (StringUtils.isNotBlank(conceptId)) {
            legacyConceptSync.syncConceptSelection(thesaurusId, conceptId, language);
            return;
        }

        String groupId = resolveV2GroupId(browseBean);
        if (StringUtils.isNotBlank(groupId)) {
            treeGroups.selectThisGroup(groupId.trim());
        }
    }

    public void syncV2FromLegacy() {
        String thesaurusId = StringUtils.firstNonBlank(
                selectedTheso.getCurrentIdTheso(),
                selectedTheso.getSelectedIdTheso()
        );
        if (StringUtils.isNotBlank(thesaurusId)) {
            thesaurusContext.selectThesaurus(thesaurusId.trim());
            String language = StringUtils.firstNonBlank(
                    selectedTheso.getCurrentLang(),
                    selectedTheso.getSelectedLang()
            );
            if (StringUtils.isNotBlank(language)) {
                thesaurusContext.changeWorkLanguage(language.trim());
            }
        }

        thesaurusContext.setIdConceptFromUri(null);
        thesaurusContext.setIdGroupFromUri(null);

        String conceptId = resolveLegacyConceptId();
        if (StringUtils.isNotBlank(conceptId)) {
            thesaurusContext.setIdConceptFromUri(conceptId.trim());
            return;
        }

        String groupId = resolveLegacyGroupId();
        if (StringUtils.isNotBlank(groupId)) {
            thesaurusContext.setIdGroupFromUri(groupId.trim());
        }
    }

    private String resolveV2ConceptId(ThesaurusBrowseBean browseBean) {
        if (browseBean != null
                && browseBean.isConceptPanel()
                && browseBean.getSelectedConcept() != null
                && browseBean.getSelectedConcept().summary() != null) {
            return browseBean.getSelectedConcept().summary().conceptId();
        }
        if (conceptSelectionContext.hasSelection()) {
            return conceptSelectionContext.getConceptId();
        }
        return null;
    }

    private String resolveV2GroupId(ThesaurusBrowseBean browseBean) {
        if (browseBean != null
                && browseBean.isGroupPanel()
                && browseBean.getSelectedGroup() != null) {
            return browseBean.getSelectedGroup().groupId();
        }
        return null;
    }

    private String resolveLegacyConceptId() {
        if (conceptView.getNodeConcept() == null || conceptView.getNodeConcept().getConcept() == null) {
            return null;
        }
        return StringUtils.trimToNull(conceptView.getNodeConcept().getConcept().getIdConcept());
    }

    private String resolveLegacyGroupId() {
        if (groupView.getNodeGroup() == null || groupView.getNodeGroup().getConceptGroup() == null) {
            return null;
        }
        return StringUtils.trimToNull(groupView.getNodeGroup().getConceptGroup().getIdGroup());
    }
}
