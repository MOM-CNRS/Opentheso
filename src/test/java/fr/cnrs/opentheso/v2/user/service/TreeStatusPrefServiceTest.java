package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.persistence.UserTreeStatusPrefEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserTreeStatusPrefRepository;
import fr.cnrs.opentheso.v2.user.model.TreeStatusIds;
import fr.cnrs.opentheso.v2.user.model.TreeStatusPref;
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
class TreeStatusPrefServiceTest {

    @Mock
    private UserTreeStatusPrefRepository repository;

    private TreeStatusPrefService service;

    @BeforeEach
    void setUp() {
        service = new TreeStatusPrefService(repository);
    }

    @Test
    void getPref_returnsDefaultsWhenNoRows() {
        when(repository.findByUserId(7)).thenReturn(List.of());

        TreeStatusPref pref = service.getPref(7);

        assertEquals(TreeStatusIds.DEFAULT_SELECTED, pref.selected());
    }

    @Test
    void getPref_readsStoredSelectionAndIgnoresUnknown() {
        when(repository.findByUserId(7)).thenReturn(List.of(
                row("valide", true),
                row("ghost", true),
                row("deprecie", true),
                row("candidat", false)
        ));

        TreeStatusPref pref = service.getPref(7);

        assertTrue(pref.contains("valide"));
        assertTrue(pref.contains("deprecie"));
        assertFalse(pref.contains("candidat"));
        assertFalse(pref.contains("ghost"));
        assertEquals(2, pref.selected().size());
    }

    @Test
    void savePref_writesAllKnownStatuses() {
        service.savePref(3, List.of("deprecie", "unknown", "valide"));

        verify(repository).deleteByUserId(3);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserTreeStatusPrefEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<UserTreeStatusPrefEntity> saved = captor.getValue();
        assertEquals(TreeStatusIds.ALL.size(), saved.size());
        assertTrue(saved.stream().anyMatch(row -> "valide".equals(row.getStatusId()) && row.isSelected()));
        assertTrue(saved.stream().anyMatch(row -> "deprecie".equals(row.getStatusId()) && row.isSelected()));
        assertTrue(saved.stream().anyMatch(row -> "candidat".equals(row.getStatusId()) && !row.isSelected()));
    }

    @Test
    void resetPref_deletesRowsAndReturnsDefaults() {
        TreeStatusPref pref = service.resetPref(4);

        verify(repository).deleteByUserId(4);
        assertEquals(TreeStatusIds.DEFAULT_SELECTED, pref.selected());
    }

    private static UserTreeStatusPrefEntity row(String statusId, boolean selected) {
        return UserTreeStatusPrefEntity.builder()
                .userId(7)
                .statusId(statusId)
                .selected(selected)
                .build();
    }
}
