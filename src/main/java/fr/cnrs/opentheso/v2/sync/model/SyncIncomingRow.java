package fr.cnrs.opentheso.v2.sync.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ligne sélectionnable pour le pull maître → copie.
 */
@Getter
@Setter
public class SyncIncomingRow implements Serializable {

    private String id;
    private String label;
    private List<String> changedFields = new ArrayList<>();
    private boolean selected = true;

    public static SyncIncomingRow from(SyncPendingConcept concept) {
        SyncIncomingRow row = new SyncIncomingRow();
        if (concept == null) {
            return row;
        }
        row.id = concept.id();
        row.label = concept.label();
        row.changedFields = concept.changedFields() == null
                ? new ArrayList<>()
                : new ArrayList<>(concept.changedFields());
        row.selected = true;
        return row;
    }
}
