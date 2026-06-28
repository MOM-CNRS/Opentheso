package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.services.RestoreThesaurusService;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.toolbox.model.LocalArkSettings;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThesaurusMaintenanceService {

    private final RestoreThesaurusService restoreThesaurusService;
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
        return restoreThesaurusService.correctDisplayTopTerm(thesaurusId);
    }

    @Transactional
    public void reorganizeHierarchy(String thesaurusId) {
        restoreThesaurusService.reorganizing(thesaurusId);
    }

    @Transactional
    public void reorganizeConceptsAndCollections(String thesaurusId) {
        restoreThesaurusService.reorganizeConceptsAndCollections(thesaurusId);
    }

    @Transactional
    public void switchRolesFromTermToConcept(String thesaurusId) {
        restoreThesaurusService.switchRolesFromTermToConcept(thesaurusId, workLanguage);
    }

    @Transactional
    public int generateArkFromConceptId(String thesaurusId, String prefix, String naan, boolean overwrite) {
        return restoreThesaurusService.generateArkFromConceptId(
                thesaurusId,
                StringUtils.trimToEmpty(prefix),
                naan,
                overwrite
        );
    }

    @Transactional
    public int generateLocalArk(String thesaurusId, boolean overwrite) {
        return restoreThesaurusService.generateArkLacal(thesaurusId, overwrite);
    }

    @Transactional
    public void generateSitemap(String thesaurusId) {
        restoreThesaurusService.generateSitemap(thesaurusId);
    }
}