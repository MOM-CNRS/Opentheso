package fr.cnrs.opentheso.v2.toolbox.persistence;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupLabel;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptStatusRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolboxStatisticsPersistenceTest {

    @Mock private ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    @Mock private ToolboxStatisticsQueryRepository toolboxStatisticsQueryRepository;
    @Mock private ConceptGroupRepository conceptGroupRepository;
    @Mock private ConceptGroupLabelRepository conceptGroupLabelRepository;
    @Mock private ConceptStatusRepository conceptStatusRepository;
    @Mock private NoteRepository noteRepository;
    @Mock private ConceptRepository conceptRepository;

    private ToolboxStatisticsPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new ToolboxStatisticsPersistence(
                toolboxThesaurusPersistence,
                toolboxStatisticsQueryRepository,
                conceptGroupRepository,
                conceptGroupLabelRepository,
                conceptStatusRepository,
                noteRepository,
                conceptRepository
        );
    }

    @Test
    void loadCollectionStatistics_aggregatesSetBasedCounts() {
        ConceptGroup group = ConceptGroup.builder().idGroup("G1").idThesaurus("TH1").build();
        when(conceptGroupRepository.findAllByIdThesaurus("TH1")).thenReturn(List.of(group));
        when(conceptGroupLabelRepository.findAllByIdThesaurusAndLang("TH1", "fr"))
                .thenReturn(List.of(ConceptGroupLabel.builder()
                        .idGroup("G1")
                        .idThesaurus("TH1")
                        .lang("fr")
                        .lexicalValue("Histoire")
                        .build()));
        when(toolboxStatisticsQueryRepository.countConceptsByGroup("TH1")).thenReturn(Map.of("g1", 10));
        when(toolboxStatisticsQueryRepository.countNotesByGroup("TH1", "fr")).thenReturn(Map.of("g1", 3));
        when(toolboxStatisticsQueryRepository.countSynonymsByGroup("TH1", "fr")).thenReturn(Map.of("g1", 2));
        when(toolboxStatisticsQueryRepository.countAlignmentsByGroup("TH1")).thenReturn(Map.of("g1", new int[]{5, 1}));
        when(conceptStatusRepository.countConceptsWithoutGroup("TH1")).thenReturn(4);
        when(conceptStatusRepository.countConceptsWithoutGroupByLangAndThesaurus("TH1", "fr")).thenReturn(3);
        when(noteRepository.countNotesWithoutGroupByLangAndThesaurus("TH1", "fr")).thenReturn(1);
        when(noteRepository.countNotesOfTermsWithoutGroup("TH1", "fr")).thenReturn(0);
        when(conceptStatusRepository.countNonPreferredTermsNotInGroup("fr", "TH1")).thenReturn(1);
        when(toolboxStatisticsQueryRepository.countAlignmentsWithoutGroup("TH1")).thenReturn(new int[]{2, 0});

        List<GenericStatistiqueData> rows = persistence.loadCollectionStatistics("TH1", "fr");

        assertEquals(2, rows.size());
        GenericStatistiqueData groupRow = rows.get(0);
        assertEquals("G1", groupRow.getIdCollection());
        assertEquals("Histoire", groupRow.getCollection());
        assertEquals(10, groupRow.getConceptsNbr());
        assertEquals(2, groupRow.getSynonymesNbr());
        assertEquals(8, groupRow.getTermesNonTraduitsNbr());
        assertEquals(3, groupRow.getNotesNbr());
        assertEquals(5, groupRow.getTotalAlignment());
        assertEquals(1, groupRow.getWikidataAlignNbr());

        GenericStatistiqueData withoutGroup = rows.get(1);
        assertEquals("Sans collection", withoutGroup.getCollection());
        assertEquals(4, withoutGroup.getConceptsNbr());
        assertEquals(1, withoutGroup.getTermesNonTraduitsNbr());
        assertEquals(2, withoutGroup.getTotalAlignment());
    }
}
