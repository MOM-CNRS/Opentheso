package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.LanguageOptionRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatLanguageServiceTest {

    @Mock
    private EditionQueryRepository editionQueryRepository;

    private CandidatLanguageService service;

    @BeforeEach
    void setUp() {
        service = new CandidatLanguageService(editionQueryRepository);
    }

    @Test
    void listAllLanguages_mapsRepositoryRows() {
        when(editionQueryRepository.findAllLanguages()).thenReturn(List.of(
                new LanguageOptionRow("fr", "fr", "Français", "French"),
                new LanguageOptionRow("en", "en", "Anglais", "English")
        ));

        var languages = service.listAllLanguages();

        assertEquals(2, languages.size());
        assertEquals("fr", languages.get(0).iso6391());
        assertEquals("Français", languages.get(0).frenchName());
        assertEquals("French", languages.get(0).englishName());
        assertEquals("en", languages.get(1).iso6391());
        assertEquals("English", languages.get(1).englishName());
    }
}
