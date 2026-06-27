package fr.cnrs.opentheso.v2.setting.ui;

import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpusEditorTest {

    @Test
    void from_mapsCorpusFields() {
        CorpusEditor editor = CorpusEditor.from(SettingTestFixtures.sampleCorpus());

        assertEquals("Corpus A", editor.getCorpusName());
        assertEquals("http://link", editor.getUriLink());
        assertTrue(editor.isActive());
    }

    @Test
    void empty_createsBlankEditor() {
        CorpusEditor editor = CorpusEditor.empty();

        assertNull(editor.getCorpusName());
        assertFalse(editor.isActive());
    }

    @Test
    void toModel_buildsCorpus() {
        CorpusEditor editor = CorpusEditor.from(SettingTestFixtures.sampleCorpus());
        editor.setUriLink("http://updated");

        var corpus = editor.toModel();

        assertEquals("Corpus A", corpus.corpusName());
        assertEquals("http://updated", corpus.uriLink());
        assertNull(corpus.sort());
    }
}
