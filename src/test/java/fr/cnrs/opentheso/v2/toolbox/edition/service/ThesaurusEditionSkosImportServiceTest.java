package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSProperty;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.io.skos.ThesaurusEditionSkosImportEngine;
import fr.cnrs.opentheso.v2.toolbox.edition.support.ThesaurusImportBatchSupport;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusEditionSkosImportServiceTest {

    @Mock
    private ThesaurusEditionSkosImportEngine thesaurusEditionSkosImportEngine;
    @Mock
    private NewThesaurusService newThesaurusService;

    private ThesaurusEditionSkosImportService service;

    @BeforeEach
    void setUp() {
        service = new ThesaurusEditionSkosImportService(
                thesaurusEditionSkosImportEngine,
                newThesaurusService,
                new SyncImportBatchSupport()
        );
    }

    @Test
    void importNewThesaurus_createsThesaurusViaEngine() throws Exception {
        var document = sampleDocument();
        when(thesaurusEditionSkosImportEngine.addThesaurus()).thenReturn("TH99");

        String thesaurusId = service.importNewThesaurus(
                document,
                "yyyy-MM-dd",
                7,
                true,
                12,
                "fr",
                "ark",
                "",
                "",
                ""
        );

        assertEquals("TH99", thesaurusId);
        verify(thesaurusEditionSkosImportEngine).setInfos("yyyy-MM-dd", 7, 12, "fr");
        verify(thesaurusEditionSkosImportEngine).addConceptV2(any(SKOSResource.class), eq("TH99"));
    }

    @Test
    void importNewThesaurus_usesSingleProjectForNonSuperAdmin() throws Exception {
        var document = sampleDocument();
        when(newThesaurusService.loadFormOptions(7, false))
                .thenReturn(new NewThesaurusFormOptions(List.of(), List.of(new ProjectOption(5, "Projet A")), false));
        when(thesaurusEditionSkosImportEngine.addThesaurus()).thenReturn("TH5");

        String thesaurusId = service.importNewThesaurus(
                document,
                "yyyy-MM-dd",
                7,
                false,
                null,
                "fr",
                "sans",
                "",
                "",
                ""
        );

        assertEquals("TH5", thesaurusId);
        verify(thesaurusEditionSkosImportEngine).setInfos("yyyy-MM-dd", 7, 5, "fr");
    }

    @Test
    void importNewThesaurus_surfacesEngineError() throws Exception {
        var document = sampleDocument();
        when(thesaurusEditionSkosImportEngine.addThesaurus()).thenReturn(null);
        when(thesaurusEditionSkosImportEngine.getMessage()).thenReturn(new StringBuilder("Erreur SKOS"));

        assertThrows(IllegalStateException.class, () -> service.importNewThesaurus(
                document, "yyyy-MM-dd", 7, true, null, "fr", "sans", "", "", ""
        ));
    }

    private SKOSXmlDocument sampleDocument() throws Exception {
        var document = new SKOSXmlDocument();
        document.setTitle("https://example.com/theso");
        var concept = new SKOSResource();
        concept.getLabelsList().add(new SKOSLabel("Chat", "fr", SKOSProperty.PREF_LABEL));
        document.setConceptList(new ArrayList<>(List.of(concept)));
        return document;
    }

    /**
     * Exécute les callbacks immédiatement, sans transaction Spring ni EntityManager.
     */
    private static final class SyncImportBatchSupport extends ThesaurusImportBatchSupport {

        private SyncImportBatchSupport() {
            super(null);
        }

        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }

        @Override
        public void inTransaction(Runnable work) {
            work.run();
        }

        @Override
        public <T> int forEachBatched(List<T> items, BiConsumer<List<T>, Integer> batchConsumer) {
            if (items == null || items.isEmpty()) {
                return 0;
            }
            batchConsumer.accept(items, items.size());
            return items.size();
        }

        @Override
        public void flushAndClear() {
            // no-op in unit tests
        }
    }
}
