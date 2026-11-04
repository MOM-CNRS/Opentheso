package fr.cnrs.opentheso.shared.ui;

import fr.cnrs.opentheso.shared.model.LegalPageContent;
import fr.cnrs.opentheso.shared.service.LegalPageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalPageBeanTest {

    @Mock
    private LegalPageService legalPageService;
    @Mock
    private UserSession userSession;
    @Mock
    private LocaleBean localeBean;

    private LegalPageBean bean;

    @BeforeEach
    void setUp() {
        bean = new LegalPageBean(legalPageService, userSession, localeBean);
        lenient().when(localeBean.getIdLangue()).thenReturn("fr");
        lenient().when(localeBean.getMsg(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void load_usesTheStoredPageForTheSelectedLanguage() {
        when(legalPageService.isKnown("mentions")).thenReturn(true);
        when(legalPageService.find("mentions", "fr"))
                .thenReturn(Optional.of(new LegalPageContent("Notice", "<p>Bonjour</p>")));
        bean.setCode("mentions");

        bean.load();

        assertTrue(bean.isKnown());
        assertEquals("Notice", bean.getTitle());
        assertEquals("<p>Bonjour</p>", bean.getHtml());
        assertTrue(bean.isContentPresent());
    }

    @Test
    void load_fallsBackToTheDefaultTitle() {
        when(legalPageService.isKnown("donnees")).thenReturn(true);
        when(legalPageService.find("donnees", "fr")).thenReturn(Optional.empty());
        bean.setCode("donnees");

        bean.load();

        assertEquals("legal.privacy", bean.getTitle());
        assertFalse(bean.isContentPresent());
    }

    @Test
    void save_isReservedToTheSuperAdmin() {
        when(legalPageService.isKnown("mentions")).thenReturn(true);
        when(legalPageService.find("mentions", "fr")).thenReturn(Optional.empty());
        when(userSession.isSuperAdmin()).thenReturn(false);
        bean.setCode("mentions");
        bean.load();
        bean.setTitle("Notice");
        bean.setHtml("<p>x</p>");

        bean.save();

        verify(legalPageService, never()).save(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        assertTrue(bean.isSaveError());
        assertFalse(bean.isCanEdit());
    }

    @Test
    void save_storesThePageAndLeavesEditMode() {
        when(legalPageService.isKnown("mentions")).thenReturn(true);
        when(userSession.isSuperAdmin()).thenReturn(true);
        when(legalPageService.save("mentions", "fr", "Notice", "<p>x</p>")).thenReturn(true);
        when(legalPageService.find("mentions", "fr"))
                .thenReturn(Optional.of(new LegalPageContent("Ancien", "")))
                .thenReturn(Optional.of(new LegalPageContent("Notice", "<p>x</p>")));
        bean.setCode("mentions");
        bean.load();
        bean.startEditing();
        bean.setTitle("Notice");
        bean.setHtml("<p>x</p>");

        bean.save();

        assertFalse(bean.isEditing());
        assertFalse(bean.isSaveError());
        assertEquals("legal.saved", bean.getSaveMessage());
        assertEquals("Notice", bean.getTitle());
    }

    @Test
    void footerTitle_prefersTheStoredName() {
        when(legalPageService.findTitles("fr")).thenReturn(Map.of("mentions", "Notice légale"));

        assertEquals("Notice légale", bean.getMentionsTitle());
        assertEquals("legal.privacy", bean.getPrivacyTitle());
    }
}
