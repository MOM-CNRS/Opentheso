package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.services.RestoreThesaurusService;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusMaintenanceServiceTest {

    @Mock
    private RestoreThesaurusService restoreThesaurusService;
    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;

    private ThesaurusMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusMaintenanceService(restoreThesaurusService, thesaurusPreferenceService);
        ReflectionTestUtils.setField(service, "workLanguage", "fr");
    }

    @Test
    void loadLocalArkSettings_mapsPreferences() {
        when(thesaurusPreferenceService.loadPreferences("TH1", "fr"))
                .thenReturn(SettingTestFixtures.samplePreferences());

        var settings = service.loadLocalArkSettings("TH1");

        assertEquals(0, settings.getSize());
    }

    @Test
    void correctDisplayTopTerm_delegatesToRestoreService() {
        when(restoreThesaurusService.correctDisplayTopTerm("TH1")).thenReturn(3);

        assertEquals(3, service.correctDisplayTopTerm("TH1"));
    }

    @Test
    void generateArkFromConceptId_trimsPrefix() {
        when(restoreThesaurusService.generateArkFromConceptId("TH1", "ark", "12345", true)).thenReturn(5);

        assertEquals(5, service.generateArkFromConceptId("TH1", " ark ", "12345", true));
        verify(restoreThesaurusService).generateArkFromConceptId("TH1", "ark", "12345", true);
    }
}
