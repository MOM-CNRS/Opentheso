package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
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
    private final ApplicationUriService applicationUriService;

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    @Transactional(readOnly = true)
    public LocalArkSettings loadLocalArkSettings(String thesaurusId) {
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, workLanguage);
        if (preferences == null) {
            return new LocalArkSettings("", "", 0);
        }
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
    public int reorganizeConceptsAndCollections(String thesaurusId) {
        return thesaurusMaintenancePersistence.reorganizeConceptsAndCollections(thesaurusId);
    }

    @Transactional
    public void switchRolesFromTermToConcept(String thesaurusId) {
        String lang = resolveWorkLanguage(thesaurusId);
        thesaurusMaintenancePersistence.switchRolesFromTermToConcept(thesaurusId, lang);
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

    @Transactional(readOnly = true)
    public String buildSitemapXml(String thesaurusId) {
        return thesaurusMaintenancePersistence.buildSitemapXml(
                thesaurusId,
                applicationUriService.resolveApplicationBaseUrl()
        );
    }

    @Transactional
    public void generateSitemap(String thesaurusId) {
        thesaurusMaintenancePersistence.generateSitemap(thesaurusId);
    }

    private String resolveWorkLanguage(String thesaurusId) {
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, workLanguage);
        if (preferences != null && StringUtils.isNotBlank(preferences.sourceLang())) {
            return preferences.sourceLang();
        }
        return workLanguage;
    }
}
