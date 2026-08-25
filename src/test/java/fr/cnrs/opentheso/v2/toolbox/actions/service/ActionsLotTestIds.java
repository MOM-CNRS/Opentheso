package fr.cnrs.opentheso.v2.toolbox.actions.service;

import fr.cnrs.opentheso.v2.toolbox.workshop.persistence.WorkshopBulkImportPersistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

final class ActionsLotTestIds {

    private ActionsLotTestIds() {
    }

    static void existing(WorkshopBulkImportPersistence persistence, String... ids) {
        Set<String> existing = Set.of(ids);
        org.mockito.Mockito.lenient().when(persistence.resolveConceptIds(any(), anyString(), anyString())).thenAnswer(invocation -> {
            Collection<?> input = invocation.getArgument(0);
            Map<String, String> map = new HashMap<>();
            if (input == null) {
                return map;
            }
            for (Object value : input) {
                if (value == null) {
                    continue;
                }
                String id = value.toString();
                if (existing.contains(id)) {
                    map.put(id, id);
                }
            }
            return map;
        });
        org.mockito.Mockito.lenient().when(persistence.findExistingIdSet(any(), anyString())).thenAnswer(invocation -> {
            Collection<?> input = invocation.getArgument(0);
            java.util.HashSet<String> found = new java.util.HashSet<>();
            if (input == null) {
                return found;
            }
            for (Object value : input) {
                if (value != null && existing.contains(value.toString())) {
                    found.add(value.toString());
                }
            }
            return found;
        });
    }

    static void existingGroups(WorkshopBulkImportPersistence persistence, String... ids) {
        Set<String> existing = Set.of(ids);
        when(persistence.findExistingGroupIdSet(any(), anyString())).thenAnswer(invocation -> {
            Collection<?> input = invocation.getArgument(0);
            java.util.HashSet<String> found = new java.util.HashSet<>();
            if (input == null) {
                return found;
            }
            for (Object value : input) {
                if (value != null && existing.contains(value.toString())) {
                    found.add(value.toString());
                }
            }
            return found;
        });
    }
}
