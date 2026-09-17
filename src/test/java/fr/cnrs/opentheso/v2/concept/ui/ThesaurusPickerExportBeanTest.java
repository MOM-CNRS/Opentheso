package fr.cnrs.opentheso.v2.concept.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusPickerExportBeanTest {

    @Test
    void open_setsExportModeAndMetadata() {
        ThesaurusPickerExportBean bean = new ThesaurusPickerExportBean();
        bean.open("th-1", "Alpha", 1200);

        assertTrue(bean.isExportMode());
        assertEquals("th-1", bean.getThesaurusId());
        assertEquals("Alpha", bean.getThesaurusTitle());
        assertEquals(1200L, bean.getConceptCount());
        assertTrue(bean.getConceptCountLabel().contains("1"));
    }

    @Test
    void open_blankId_ignored() {
        ThesaurusPickerExportBean bean = new ThesaurusPickerExportBean();
        bean.open("  ", "X", 10);
        assertFalse(bean.isExportMode());
    }

    @Test
    void cancel_resetsState() {
        ThesaurusPickerExportBean bean = new ThesaurusPickerExportBean();
        bean.open("th-1", "Alpha", 3);
        bean.cancel();

        assertFalse(bean.isExportMode());
        assertEquals(null, bean.getThesaurusId());
        assertEquals(0L, bean.getConceptCount());
    }
}
