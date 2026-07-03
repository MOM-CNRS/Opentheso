package fr.cnrs.opentheso.v2.shared.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    }
}
