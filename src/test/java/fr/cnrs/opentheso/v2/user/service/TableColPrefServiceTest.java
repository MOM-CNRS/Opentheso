package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.persistence.UserTableColPrefEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserTableColPrefRepository;
import fr.cnrs.opentheso.v2.user.model.TableColIds;
import fr.cnrs.opentheso.v2.user.model.TableColPref;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableColPrefServiceTest {

    @Mock
    private UserTableColPrefRepository repository;

    private TableColPrefService service;

    @BeforeEach
    void setUp() {
        service = new TableColPrefService(repository);
    }

    @Test
    void getPref_returnsDefaultsWhenNoRows() {
        when(repository.findByUserId(7)).thenReturn(List.of());

        TableColPref pref = service.getPref(7);

        assertEquals(TableColIds.DEFAULT_SELECTED, pref.selected());
    }

    @Test
    void getPref_readsStoredSelectionAndIgnoresUnknown() {
        when(repository.findByUserId(7)).thenReturn(List.of(
                row("status", true),
                row("ghost", true),
                row("path", true),
                row("type", false)
        ));

        TableColPref pref = service.getPref(7);

        assertTrue(pref.contains("status"));
        assertTrue(pref.contains("path"));
        assertFalse(pref.contains("type"));
        assertFalse(pref.contains("ghost"));
        assertEquals(2, pref.selected().size());
    }

    @Test
    void savePref_writesAllKnownColumns() {
        service.savePref(3, List.of("path", "unknown", "status"));

        verify(repository).deleteByUserId(3);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserTableColPrefEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<UserTableColPrefEntity> saved = captor.getValue();
        assertEquals(TableColIds.ALL.size(), saved.size());
        assertTrue(saved.stream().anyMatch(row -> "status".equals(row.getColId()) && row.isSelected()));
        assertTrue(saved.stream().anyMatch(row -> "path".equals(row.getColId()) && row.isSelected()));
        assertTrue(saved.stream().anyMatch(row -> "type".equals(row.getColId()) && !row.isSelected()));
    }

    @Test
    void resetPref_deletesRowsAndReturnsDefaults() {
        TableColPref pref = service.resetPref(4);

        verify(repository).deleteByUserId(4);
        assertEquals(TableColIds.DEFAULT_SELECTED, pref.selected());
    }

    private static UserTableColPrefEntity row(String colId, boolean selected) {
        return UserTableColPrefEntity.builder()
                .userId(7)
                .colId(colId)
                .selected(selected)
                .build();
    }
}
