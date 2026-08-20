package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.SkosConceptProjection;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ExportRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import fr.cnrs.opentheso.repositories.ThesaurusDcTermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSkosDocumentBuilderConceptExportTest {

    @Mock
    private ExportRepository exportRepository;
    @Mock
    private ConceptFacetRepository conceptFacetRepository;
    @Mock
    private ConceptRepository conceptRepository;
    @Mock
    private ConceptGroupRepository conceptGroupRepository;
    @Mock
    private ConceptGroupLabelRepository conceptGroupLabelRepository;
    @Mock
    private RelationGroupRepository relationGroupRepository;
    @Mock
    private ThesaurusDcTermRepository thesaurusDcTermRepository;
    @Mock
    private ThesaurusLabelRepository thesaurusLabelRepository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private ThesaurusArrayRepository thesaurusArrayRepository;
    @Mock
    private ToolboxPreferencePersistence toolboxPreferencePersistence;
    @Mock
    private SkosConceptProjection projectionC1;
    @Mock
    private SkosConceptProjection projectionC2;

    private ThesaurusSkosDocumentBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ThesaurusSkosDocumentBuilder(
                exportRepository,
                conceptFacetRepository,
                conceptRepository,
                conceptGroupRepository,
                conceptGroupLabelRepository,
                relationGroupRepository,
                thesaurusDcTermRepository,
                thesaurusLabelRepository,
                noteRepository,
                thesaurusArrayRepository,
                toolboxPreferencePersistence
        );
    }

    @Test
    void exportConcepts_queriesProjectionsOnceForTheWholeSelection() throws Exception {
        var preferences = Preferences.builder()
                .cheminSite("https://example.com/")
                .originalUri("https://example.com/theso/")
                .build();
        stubProjection(projectionC1, "C1");
        stubProjection(projectionC2, "C2");
        when(exportRepository.getConceptsByIds(eq("TH1"), any(), any()))
                .thenReturn(List.of(projectionC1, projectionC2));

        var resources = builder.exportConcepts(
                "TH1", List.of("C1", "C2"), preferences, false, null
        );

        assertEquals(2, resources.size());
        verify(exportRepository, times(1)).getConceptsByIds(eq("TH1"), any(), any());
        verify(exportRepository, never()).getAllConcepts(any(), any());
    }

    private void stubProjection(SkosConceptProjection projection, String id) {
        when(projection.getIdentifier()).thenReturn(id);
        when(projection.getUri()).thenReturn("https://example.com/" + id);
        when(projection.getLocal_uri()).thenReturn("https://example.com/local/" + id);
        when(projection.getType()).thenReturn("concept");
        when(projection.getCreated()).thenReturn(new Date(0));
        when(projection.getModified()).thenReturn(new Date(0));
    }
}
