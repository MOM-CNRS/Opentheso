package fr.cnrs.opentheso.v2.preview.ui;

import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusSelectionService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreviewSettingsAccessTest {

    @Mock
    private ThesaurusSelectionService thesaurusSelectionService;
    @Mock
    private ThesaurusWorkLanguageService thesaurusWorkLanguageService;
    @Mock
    private UserSession userSession;
    @Mock
    private RightsService rightsService;

    private PreviewSettingsAccess access;

    @BeforeEach
    void setUp() {
        ThesaurusContext thesaurusContext = new ThesaurusContext(
                thesaurusSelectionService, thesaurusWorkLanguageService);
        ReflectionTestUtils.setField(thesaurusContext, "defaultWorkLanguage", "fr");
        thesaurusContext.selectThesaurus("th17", "Pactols_Lieux", "fr");
        access = new PreviewSettingsAccess(thesaurusContext, userSession, rightsService);
    }

    @Test
    void canEdit_isCachedForTheView() {
        when(userSession.getCurrentUserId()).thenReturn(2);
        when(rightsService.canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17")).thenReturn(true);

        assertTrue(access.isCanEdit());
        assertTrue(access.isCanEdit());

        verify(rightsService, times(1)).canOnThesaurus(2, Permission.MANAGE_THESAURUS, "th17");
    }

    @Test
    void canEdit_deniesAnonymous() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        assertFalse(access.isCanEdit());
    }
}
