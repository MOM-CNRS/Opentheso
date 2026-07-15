package fr.cnrs.opentheso.v2.toolbox.service;

import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.v2.toolbox.persistence.ThesaurusLifecyclePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusArkPersistence;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import fr.cnrs.opentheso.v2.shared.repository.EditionQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.EditionCollectionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.EditionThesaurusDetailsRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ThesaurusLanguageRow;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.EditionCollectionNode;
import fr.cnrs.opentheso.v2.toolbox.model.EditionMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModifyThesaurusServiceTest {

    @Mock
    private EditionQueryRepository editionQueryRepository;
    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock
    private ToolboxThesaurusArkPersistence toolboxThesaurusArkPersistence;
    @Mock
    private ThesaurusLifecyclePersistence thesaurusLifecyclePersistence;
    @Mock
    private ThesaurusDcTermRepository thesaurusDcTermRepository;

    private ModifyThesaurusService service;

    @BeforeEach
    void setUp() {
        service = new ModifyThesaurusService(
                editionQueryRepository,
                thesaurusSettingsQueryRepository,
                toolboxPreferencePersistence,
                toolboxThesaurusPersistence,
                toolboxThesaurusArkPersistence,
                thesaurusLifecyclePersistence,
                thesaurusDcTermRepository
        );
        ReflectionTestUtils.setField(service, "workLanguage", "fr");
    }

    @Test
    void loadDetails_returnsMappedDetails() {
        when(editionQueryRepository.findThesaurusDetails("TH1", "fr"))
                .thenReturn(Optional.of(new EditionThesaurusDetailsRow("TH1", "Titre", "ark/1", false, "fr")));

        var details = service.loadDetails("TH1");

        assertEquals("TH1", details.id());
        assertEquals("Titre", details.title());
        assertEquals("ark/1", details.arkId());
        assertFalse(details.privateThesaurus());
    }

    @Test
    void loadDetails_throwsWhenMissing() {
        when(editionQueryRepository.findThesaurusDetails("TH1", "fr")).thenReturn(Optional.empty());

        assertThrows(InvalidToolboxDataException.class, () -> service.loadDetails("TH1"));
    }

    @Test
    void changeSourceLanguage_validatesBlankLanguage() {
        assertThrows(InvalidToolboxDataException.class, () -> service.changeSourceLanguage("TH1", " "));
    }

    @Test
    void changeThesaurusId_returnsNewIdOnSuccess() {
        when(thesaurusLifecyclePersistence.changeThesaurusId("TH1", "TH2")).thenReturn(true);

        assertEquals("TH2", service.changeThesaurusId("TH1", "TH2"));
    }

    @Test
    void addLanguage_delegatesToThesaurusService() {
        service.addLanguage("TH1", "Label FR", "fr", "admin");

        ArgumentCaptor<Thesaurus> captor = ArgumentCaptor.forClass(Thesaurus.class);
        verify(toolboxThesaurusPersistence).addTranslation(captor.capture());
        assertEquals("TH1", captor.getValue().getId_thesaurus());
        assertEquals("fr", captor.getValue().getLanguage());
        assertEquals("Label FR", captor.getValue().getTitle());
    }

    @Test
    void updateCollectionsVisibility_delegatesBulkUpdate() {
        service.updateCollectionsVisibility("TH1", List.of("G1", "G2"), true);

        verify(editionQueryRepository).updateCollectionsVisibility("TH1", List.of("G1", "G2"), true);
    }

    @Test
    void buildCollectionTree_buildsHierarchyInMemory() {
        var rows = List.of(
                new EditionCollectionRow("root", "Racine", false, null),
                new EditionCollectionRow("child", "Enfant", true, "root")
        );

        var tree = service.buildCollectionTree(rows);

        assertEquals(1, tree.getChildren().size());
        EditionCollectionNode root = tree.getChildren().get(0).getData();
        assertEquals("root", root.getId());
        assertEquals(1, tree.getChildren().get(0).getChildren().size());
        assertEquals("child", tree.getChildren().get(0).getChildren().get(0).getData().getId());
    }

    @Test
    void saveMetadata_createsNewRowWhenIdIsMinusOne() {
        EditionMetadata metadata = EditionMetadata.emptyRow();
        metadata.setName("title");
        metadata.setValue("valeur");
        when(thesaurusDcTermRepository.save(any(ThesaurusDcTerm.class)))
                .thenAnswer(invocation -> {
                    ThesaurusDcTerm term = invocation.getArgument(0);
                    term.setId(12L);
                    return term;
                });

        service.saveMetadata("TH1", metadata);

        verify(thesaurusDcTermRepository).save(any(ThesaurusDcTerm.class));
        assertEquals(12, metadata.getId());
    }

    @Test
    void loadLanguages_usesSourceLanguageFromPreferences() {
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn("en");
        when(thesaurusSettingsQueryRepository.findUsedLanguages("TH1", "en"))
                .thenReturn(List.of(new ThesaurusLanguageRow(1L, "en", "gb", "Thesaurus EN", "English")));

        var languages = service.loadLanguages("TH1");

        assertEquals(1, languages.size());
        assertEquals("en", languages.get(0).code());
        verify(thesaurusSettingsQueryRepository).findUsedLanguages("TH1", "en");
    }

    @Test
    void deleteLanguage_validatesBlankCode() {
        assertThrows(InvalidToolboxDataException.class, () -> service.deleteLanguage("TH1", ""));
        verify(toolboxThesaurusPersistence, never()).deleteTranslation(any(), any());
    }
}
