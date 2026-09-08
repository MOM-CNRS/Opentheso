package fr.cnrs.opentheso.v2.concept.write.ui;

/**
 * Result of a mutation step that updates a dirty flag.
 * Never null: {@code ok=false} aborts the save chain; {@code dirty} tracks prior successful writes.
 */
final class DirtyUpdate {

    final boolean ok;
    final boolean dirty;

    private DirtyUpdate(boolean ok, boolean dirty) {
        this.ok = ok;
        this.dirty = dirty;
    }

    static DirtyUpdate fail() {
        return new DirtyUpdate(false, false);
    }

    static DirtyUpdate of(boolean dirty) {
        return new DirtyUpdate(true, dirty);
    }
}
