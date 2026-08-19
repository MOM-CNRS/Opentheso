package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;
import fr.cnrs.opentheso.v2.concept.alignment.service.ConceptAlignmentAdminService;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusSearchLanguageSync;
import fr.cnrs.opentheso.v2.setting.ui.PreferenceEditor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSettingsPersistServiceTest {

    @Mock
    private ThesaurusPreferenceService thesaurusPreferenceService;
    @Mock
    private ThesaurusCorpusService thesaurusCorpusService;
    @Mock
    private ConceptAlignmentAdminService conceptAlignmentAdminService;
    @Mock
    private ThesaurusSearchLanguageSync thesaurusSearchLanguageSync;

    private ThesaurusSettingsPersistService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusSettingsPersistService(
                thesaurusPreferenceService,
                thesaurusCorpusService,
                conceptAlignmentAdminService,
                thesaurusSearchLanguageSync
        );
    }

    @Test
    void saveAll_persistsPreferencesCorpusAndAlignmentInOneCall() {
        PreferenceEditor editor = PreferenceEditor.from(SettingTestFixtures.samplePreferences());
        ThesaurusPreferences saved = SettingTestFixtures.samplePreferences();
        when(thesaurusPreferenceService.savePreferences(
                eq("th17"), any(), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq("fr")
        )).thenReturn(saved);
        ThesaurusCorpus current = new ThesaurusCorpus(
                "Corpus A", "http://link", null, false, true, false, null);
        CorpusPersistDraft corpus = new CorpusPersistDraft(
                List.of(current),
                List.of(SettingTestFixtures.sampleCorpus()),
                Map.of("Corpus A", "Corpus A")
        );
        AlignmentSourceItem source = new AlignmentSourceItem(
                1, "Wikidata", "Wiki", true, true, "wikidata", "https://www.wikidata.org/");
        AlignmentPersistDraft alignment = new AlignmentPersistDraft(
                List.of(source),
                List.of(new AlignmentSourceItem(
                        1, "Wikidata", "Wiki", false, true, "wikidata", "https://www.wikidata.org/")),
                Set.of()
        );

        ThesaurusPreferences result = service.saveAll("th17", 2, editor, "fr", corpus, alignment);

        assertEquals(saved, result);
        verify(thesaurusCorpusService).updateCorpus(eq("th17"), eq("Corpus A"), any(ThesaurusCorpus.class));
        verify(conceptAlignmentAdminService).setSourceSelected("th17", 1, true);
        verify(thesaurusSearchLanguageSync).applyAfterSourceLanguageChange("th17", saved.sourceLang());
    }

    @Test
    void saveAll_stopsAndPropagatesWhenAlignmentCreateFails() {
        PreferenceEditor editor = PreferenceEditor.from(SettingTestFixtures.samplePreferences());
        when(thesaurusPreferenceService.savePreferences(
                eq("th17"), any(), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), eq("fr")
        )).thenReturn(SettingTestFixtures.samplePreferences());
        ThesaurusCorpus created = new ThesaurusCorpus("New", "http://link", null, true, true, false, null);
        CorpusPersistDraft corpus = new CorpusPersistDraft(List.of(created), List.of(), new LinkedHashMap<>());
        AlignmentSourceItem draftSource = new AlignmentSourceItem(
                -1, "Pactols", "", true, false, "Opentheso", "https://ex.org/api/search");
        AlignmentPersistDraft alignment = new AlignmentPersistDraft(List.of(draftSource), List.of(), Set.of());
        when(conceptAlignmentAdminService.addLocalSource(
                eq("th17"), eq(2), eq("Pactols"), any(), any(), any(), anyBoolean()
        )).thenReturn("Le nom de la source existe déjà");

        InvalidSettingDataException ex = assertThrows(
                InvalidSettingDataException.class,
                () -> service.saveAll("th17", 2, editor, "fr", corpus, alignment)
        );

        assertEquals("Le nom de la source existe déjà", ex.getMessage());
        verify(thesaurusCorpusService).createCorpus(eq("th17"), any(ThesaurusCorpus.class));
        verify(thesaurusSearchLanguageSync, never()).applyAfterSourceLanguageChange(any(), any());
        verify(conceptAlignmentAdminService, never()).setSourceSelected(any(), anyInt(), anyBoolean());
    }
}
