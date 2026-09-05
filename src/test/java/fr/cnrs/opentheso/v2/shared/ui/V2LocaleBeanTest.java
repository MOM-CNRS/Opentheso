package fr.cnrs.opentheso.v2.shared.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V2LocaleBeanTest {

    private V2LocaleBean bean;

    @BeforeEach
    void setUp() {
        bean = new V2LocaleBean();
        ReflectionTestUtils.setField(bean, "workLanguage", "fr");
        bean.init();
    }

    @Test
    void init_setsBundleFromWorkLanguage() {
        assertEquals("langue_fr", ReflectionTestUtils.getField(bean, "currentBundle"));
        assertEquals("fr", bean.getIdLangue());
        assertTrue(bean.currentLangIs("fr"));
        assertEquals("🇫🇷", bean.getFlagEmoji());
    }

    @Test
    void changeLangue_switchesSupportedLanguage() {
        bean.changeLangue("en");

        assertEquals("en", bean.getIdLangue());
        assertEquals("langue_en", ReflectionTestUtils.getField(bean, "currentBundle"));
        assertTrue(bean.currentLangIs("EN"));
        assertEquals("🇬🇧", bean.getFlagEmoji());
    }

    @Test
    void changeLangue_ignoresUnsupportedCode() {
        bean.changeLangue("it");

        assertEquals("fr", bean.getIdLangue());
        assertEquals("langue_fr", ReflectionTestUtils.getField(bean, "currentBundle"));
    }

    @Test
    void applyPendingLang_usesHiddenFieldValue() {
        bean.setPendingLang("de");
        bean.applyPendingLang();

        assertEquals("de", bean.getIdLangue());
        assertEquals("🇩🇪", bean.getFlagEmoji());
        assertTrue(bean.currentLangIs("de"));
    }

    @Test
    void flagEmoji_mapsKnownLanguages() {
        assertEquals("🇪🇸", bean.flagEmoji("es"));
        assertEquals("🇸🇦", bean.flagEmoji("ar"));
        assertEquals("🇫🇷", bean.flagEmoji("unknown"));
    }
}
