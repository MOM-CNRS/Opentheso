package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSelectionServiceTest {

    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    @InjectMocks
    private ThesaurusSelectionService thesaurusSelectionService;

    @Test
    void resolve_returnsNullForBlankId() {
        assertNull(thesaurusSelectionService.resolve(" "));
    }

    @Test
    void resolve_loadsTitleFromRepository() {
        ReflectionTestUtils.setField(thesaurusSelectionService, "workLanguage", "fr");
        when(thesaurusSettingsQueryRepository.findThesaurusTitle("TH1", "fr")).thenReturn(Optional.of("Thésaurus test"));

        ThesaurusSelection selection = thesaurusSelectionService.resolve("TH1");

        assertEquals("TH1", selection.thesaurusId());
        assertEquals("Thésaurus test", selection.title());
    }
}
