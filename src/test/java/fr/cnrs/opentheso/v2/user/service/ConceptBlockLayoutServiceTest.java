package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.persistence.UserConceptBlockPrefEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserConceptBlockPrefRepository;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockIds;
import fr.cnrs.opentheso.v2.user.model.ConceptBlockLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConceptBlockLayoutServiceTest {

    @Mock
    private UserConceptBlockPrefRepository repository;

    private ConceptBlockLayoutService service;

    @BeforeEach
    void setUp() {
        service = new ConceptBlockLayoutService(repository);
    }

    @Test
    void getLayout_returnsDefaultsWhenNoRows() {
        when(repository.findByUserIdOrderByPositionAsc(7)).thenReturn(List.of());

        ConceptBlockLayout layout = service.getLayout(7);

        assertEquals(ConceptBlockIds.DEFAULT_ORDER, layout.order());
        assertTrue(layout.collapsed().isEmpty());
    }

    @Test
    void getLayout_normalizesStoredRowsAndFillsMissingBlocks() {
        when(repository.findByUserIdOrderByPositionAsc(7)).thenReturn(List.of(
                row("notes", 0, true),
                row("unknown", 1, false),
                row("contexte", 2, false)
        ));

        ConceptBlockLayout layout = service.getLayout(7);

        assertEquals("notes", layout.order().get(0));
        assertEquals("contexte", layout.order().get(1));
        assertEquals(ConceptBlockIds.DEFAULT_ORDER.size(), layout.order().size());
        assertTrue(layout.collapsed().contains("notes"));
        assertFalse(layout.collapsed().contains("unknown"));
    }

    @Test
    void saveLayout_replacesAllRows() {
        service.saveLayout(3, List.of("temporel", "notes"), List.of("notes", "ghost"));

        verify(repository).deleteByUserId(3);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserConceptBlockPrefEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<UserConceptBlockPrefEntity> saved = captor.getValue();
        assertEquals(ConceptBlockIds.DEFAULT_ORDER.size(), saved.size());
        assertEquals("temporel", saved.get(0).getBlockId());
        assertEquals("notes", saved.get(1).getBlockId());
        assertEquals(0, saved.get(0).getPosition());
        assertTrue(saved.stream().filter(row -> "notes".equals(row.getBlockId())).findFirst().orElseThrow().isCollapsed());
        assertFalse(saved.get(0).isCollapsed());
    }

    @Test
    void resetLayout_deletesRows() {
        ConceptBlockLayout layout = service.resetLayout(4);

        verify(repository).deleteByUserId(4);
        assertEquals(ConceptBlockLayout.defaults(), layout);
    }

    @Test
    void normalize_ignoresUnknownAndDeduplicates() {
        ConceptBlockLayout layout = service.normalize(
                List.of("notes", "notes", "nope", "contexte"),
                List.of("notes", "nope")
        );

        assertEquals("notes", layout.order().get(0));
        assertEquals("contexte", layout.order().get(1));
        assertEquals(Set.of("notes"), layout.collapsed());
    }

    private static UserConceptBlockPrefEntity row(String id, int position, boolean collapsed) {
        return UserConceptBlockPrefEntity.builder()
                .userId(7)
                .blockId(id)
                .position(position)
                .collapsed(collapsed)
                .build();
    }
}
