package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.setting.fixtures.SettingTestFixtures;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.shared.persistence.CorpusLinkEntity;
import fr.cnrs.opentheso.v2.shared.repository.CorpusLinkJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusCorpusServiceTest {

    @Mock
    private CorpusLinkJpaRepository corpusLinkJpaRepository;

    private ThesaurusCorpusService thesaurusCorpusService;

    @BeforeEach
    void setUp() {
        thesaurusCorpusService = new ThesaurusCorpusService(corpusLinkJpaRepository);
    }

    @Test
    void listCorpus_returnsMappedCorpus() {
        when(corpusLinkJpaRepository.findAllByIdThesaurusOrderBySortAsc("TH1")).thenReturn(
                List.of(SettingTestFixtures.sampleCorpusEntity())
        );

        List<ThesaurusCorpus> corpus = thesaurusCorpusService.listCorpus("TH1");

        assertEquals(1, corpus.size());
        assertEquals("Corpus A", corpus.get(0).corpusName());
    }

    @Test
    void createCorpus_savesEntity() {
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus A"))
                .thenReturn(Optional.empty());
        when(corpusLinkJpaRepository.save(any(CorpusLinkEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ThesaurusCorpus created = thesaurusCorpusService.createCorpus("TH1", SettingTestFixtures.sampleCorpus());

        assertEquals("Corpus A", created.corpusName());
        verify(corpusLinkJpaRepository).save(any(CorpusLinkEntity.class));
    }

    @Test
    void createCorpus_rejectsDuplicate() {
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus A"))
                .thenReturn(Optional.of(new CorpusLinkEntity()));

        assertThrows(InvalidSettingDataException.class,
                () -> thesaurusCorpusService.createCorpus("TH1", SettingTestFixtures.sampleCorpus()));
    }

    @Test
    void createCorpus_rejectsInvalidData() {
        assertThrows(InvalidSettingDataException.class,
                () -> thesaurusCorpusService.createCorpus("TH1", null));
        assertThrows(InvalidSettingDataException.class,
                () -> thesaurusCorpusService.createCorpus("TH1",
                        new ThesaurusCorpus("", "http://link", "http://count", true, false, false, null)));
        assertThrows(InvalidSettingDataException.class,
                () -> thesaurusCorpusService.createCorpus("TH1",
                        new ThesaurusCorpus("Corpus A", "", "http://count", true, false, false, null)));
        assertThrows(InvalidSettingDataException.class,
                () -> thesaurusCorpusService.createCorpus("TH1",
                        new ThesaurusCorpus("Corpus A", "http://link", null, true, false, false, null)));
    }

    @Test
    void updateCorpus_updatesExistingCorpus() {
        CorpusLinkEntity existing = SettingTestFixtures.sampleCorpusEntity();
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus A"))
                .thenReturn(Optional.of(existing));
        when(corpusLinkJpaRepository.save(any(CorpusLinkEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ThesaurusCorpus updated = thesaurusCorpusService.updateCorpus(
                "TH1",
                "Corpus A",
                new ThesaurusCorpus("Corpus A", "http://new-link", "http://count", false, true, true, 2)
        );

        assertEquals("http://new-link", updated.uriLink());
        assertEquals(false, updated.active());
    }

    @Test
    void updateCorpus_renamesCorpus() {
        CorpusLinkEntity existing = SettingTestFixtures.sampleCorpusEntity();
        CorpusLinkEntity renamed = SettingTestFixtures.sampleCorpusEntity();
        renamed.setCorpusName("Corpus B");
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus A"))
                .thenReturn(Optional.of(existing));
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus B"))
                .thenReturn(Optional.empty(), Optional.of(renamed));
        when(corpusLinkJpaRepository.save(any(CorpusLinkEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ThesaurusCorpus updated = thesaurusCorpusService.updateCorpus(
                "TH1",
                "Corpus A",
                new ThesaurusCorpus("Corpus B", "http://link", "http://count", true, false, false, 1)
        );

        assertEquals("Corpus B", updated.corpusName());
        verify(corpusLinkJpaRepository).updateCorpusName("Corpus B", "Corpus A", "TH1");
    }

    @Test
    void updateCorpus_throwsWhenMissing() {
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Missing"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidSettingDataException.class,
                () -> thesaurusCorpusService.updateCorpus("TH1", "Missing", SettingTestFixtures.sampleCorpus()));
    }

    @Test
    void updateCorpus_rejectsDuplicateNameOnRename() {
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus A"))
                .thenReturn(Optional.of(SettingTestFixtures.sampleCorpusEntity()));
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus B"))
                .thenReturn(Optional.of(new CorpusLinkEntity()));

        assertThrows(InvalidSettingDataException.class, () -> thesaurusCorpusService.updateCorpus(
                "TH1",
                "Corpus A",
                new ThesaurusCorpus("Corpus B", "http://link", "http://count", true, false, false, null)
        ));
    }

    @Test
    void deleteCorpus_removesEntity() {
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus A"))
                .thenReturn(Optional.of(new CorpusLinkEntity()));

        thesaurusCorpusService.deleteCorpus("TH1", "Corpus A");

        verify(corpusLinkJpaRepository).deleteByIdThesaurusAndCorpusName("TH1", "Corpus A");
    }

    @Test
    void deleteCorpus_throwsWhenMissing() {
        when(corpusLinkJpaRepository.findByIdThesaurusAndCorpusName("TH1", "Corpus A"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidSettingDataException.class,
                () -> thesaurusCorpusService.deleteCorpus("TH1", "Corpus A"));
    }
}
