package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.toolbox.model.LocalArkSettings;
import fr.cnrs.opentheso.v2.toolbox.persistence.ThesaurusMaintenancePersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThesaurusMaintenanceService {

    private final ThesaurusMaintenancePersistence thesaurusMaintenancePersistence;
    private final ThesaurusPreferenceService thesaurusPreferenceService;

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    @Transactional(readOnly = true)
    public LocalArkSettings loadLocalArkSettings(String thesaurusId) {
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferences(thesaurusId, workLanguage);
        return new LocalArkSettings(
                preferences.naanArkLocal(),
                preferences.prefixArkLocal(),
                preferences.sizeIdArkLocal() != null ? preferences.sizeIdArkLocal() : 0
        );
    }

    @Transactional
    public int correctDisplayTopTerm(String thesaurusId) {
        return thesaurusMaintenancePersistence.correctDisplayTopTerm(thesaurusId);
    }

    @Transactional
    public void reorganizeHierarchy(String thesaurusId) {
        thesaurusMaintenancePersistence.reorganizeHierarchy(thesaurusId);
    }

    @Transactional
    public void reorganizeConceptsAndCollections(String thesaurusId) {
        thesaurusMaintenancePersistence.reorganizeConceptsAndCollections(thesaurusId);
    }

    @Transactional
    public void switchRolesFromTermToConcept(String thesaurusId) {
        thesaurusMaintenancePersistence.switchRolesFromTermToConcept(thesaurusId, workLanguage);
    }

    @Transactional
    public int generateArkFromConceptId(String thesaurusId, String prefix, String naan, boolean overwrite) {
        return thesaurusMaintenancePersistence.generateArkFromConceptId(
                thesaurusId,
                StringUtils.trimToEmpty(prefix),
                naan,
                overwrite
        );
    }

    @Transactional
    public int generateLocalArk(String thesaurusId, boolean overwrite) {
        return thesaurusMaintenancePersistence.generateLocalArk(thesaurusId, overwrite);
    }

    @Transactional
    public void generateSitemap(String thesaurusId) {
        thesaurusMaintenancePersistence.generateSitemap(thesaurusId);
    }
}
