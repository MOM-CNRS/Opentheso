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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void loadAllLanguages_mapsRows() {
        when(editionQueryRepository.findAllLanguages())
                .thenReturn(List.of(new fr.cnrs.opentheso.v2.shared.repository.projection.LanguageOptionRow(
                        "fr", "fr", "Français", "French")));

        var options = service.loadAllLanguages();

        assertEquals(1, options.size());
        assertEquals("fr", options.get(0).code());
    }

    @Test
    void loadMetadata_dedupesCreatedModifiedKeepingNewest() {
        when(thesaurusDcTermRepository.findAllByIdThesaurus("TH1")).thenReturn(List.of(
                ThesaurusDcTerm.builder().id(1L).name("created").value("2020-01-01").build(),
                ThesaurusDcTerm.builder().id(2L).name("created").value("2024-01-01").build(),
                ThesaurusDcTerm.builder().id(3L).name("title").value("Pays").build()
        ));

        var metadata = service.loadMetadata("TH1");

        assertEquals(2, metadata.size());
        assertTrue(metadata.stream().anyMatch(item -> "title".equals(item.getName())));
        assertEquals("2024-01-01", metadata.stream()
                .filter(item -> "created".equals(item.getName()))
                .findFirst().orElseThrow().getValue());
    }

    @Test
    void loadCollectionTree_usesWorkLanguageFallback() {
        when(toolboxPreferencePersistence.getWorkLanguage("TH1")).thenReturn(" ");
        when(editionQueryRepository.findCollections("TH1", "fr")).thenReturn(List.of(
                new EditionCollectionRow("root", "Racine", false, null)
        ));

        var tree = service.loadCollectionTree("TH1");

        assertEquals(1, tree.getChildren().size());
    }

    @Test
    void changeVisibilityAndMasterRole_delegate() {
        when(toolboxPreferencePersistence.isMaster("TH1")).thenReturn(true);
        service.changeVisibility("TH1", true);
        service.updateMasterRole("TH1", false);
        service.updateMasterLink("TH1", "http://master", "THX", "key");
        service.deleteMetadata("TH1", 4);

        verify(toolboxThesaurusPersistence).setVisibility("TH1", true);
        verify(toolboxPreferencePersistence).updateMasterRole("TH1", false);
        verify(toolboxPreferencePersistence).updateMasterLink("TH1", "http://master", "THX", "key");
        verify(thesaurusDcTermRepository).deleteDcElementThesaurus(4, "TH1");
        assertTrue(service.isMasterThesaurus("TH1"));
    }

    @Test
    void generateArkId_delegates() {
        when(toolboxThesaurusArkPersistence.generateArkIdForThesaurus("TH1")).thenReturn("ark:/1");
        assertEquals("ark:/1", service.generateArkId("TH1"));
    }

    @Test
    void updateLanguage_throwsWhenPersistenceFails() {
        when(toolboxThesaurusPersistence.updateTranslation(any())).thenReturn(false);
        assertThrows(InvalidToolboxDataException.class,
                () -> service.updateLanguage("TH1", "fr", "Titre", "admin"));
    }

    @Test
    void changeSourceLanguage_throwsWhenPersistenceFails() {
        when(toolboxPreferencePersistence.setWorkLanguage("en", "TH1")).thenReturn(false);
        assertThrows(InvalidToolboxDataException.class, () -> service.changeSourceLanguage("TH1", "en"));
    }

    @Test
    void loadDcmiCatalog_isNotEmpty() {
        assertFalse(service.loadDcmiResources().isEmpty());
        assertFalse(service.loadDcmiTypes().isEmpty());
    }

    @Test
    void saveMetadata_updatesExistingRow() {
        EditionMetadata metadata = new EditionMetadata();
        metadata.setId(5);
        metadata.setName("title");
        metadata.setValue("Pays");
        ThesaurusDcTerm existing = ThesaurusDcTerm.builder().id(5L).name("old").value("x").build();
        when(thesaurusDcTermRepository.findById(5)).thenReturn(Optional.of(existing));

        service.saveMetadata("TH1", metadata);

        verify(thesaurusDcTermRepository).save(existing);
        assertEquals("Pays", existing.getValue());
    }

    @Test
    void deleteLanguage_validatesBlankCode() {
        assertThrows(InvalidToolboxDataException.class, () -> service.deleteLanguage("TH1", ""));
        verify(toolboxThesaurusPersistence, never()).deleteTranslation(any(), any());
    }
}
