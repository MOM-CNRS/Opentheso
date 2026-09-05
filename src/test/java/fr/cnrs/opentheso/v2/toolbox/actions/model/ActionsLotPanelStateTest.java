package fr.cnrs.opentheso.v2.toolbox.actions.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionsLotPanelStateTest {

    @Test
    void acceptFile_validation_applyResult_andReset() {
        ActionsLotPanelState state = new ActionsLotPanelState();
        state.acceptFile("align.csv", new byte[2048]);
        assertTrue(state.isHasFile());
        assertEquals("align.csv", state.getFileName());
        assertTrue(state.getFileMeta().contains("Ko"));
        assertTrue(state.getCssClasses().contains("has-file"));

        state.applyValidation(ActionsLotValidationResult.failure("boom"));
        assertTrue(state.isChecked());
        assertEquals("boom", state.getGlobalError());
        assertTrue(state.getCssClasses().contains("has-errors"));

        state.applyValidation(new ActionsLotValidationResult(
                true, null, 10, 8, 1, 1,
                List.of(new ActionsLotLineError(2, "x", "col", "bad")),
                List.of()
        ));
        assertEquals(10, state.getLinesRead());
        assertEquals(8, state.getValidCount());
        assertEquals(1, state.getErrorCount());
        assertFalse(state.getErrors().isEmpty());

        state.applyResult(new ActionsLotApplyResult(true, "ok", 10, 7, 1));
        assertTrue(state.isDone());
        assertEquals(7, state.getAppliedCount());
        assertTrue(state.getCssClasses().contains("is-done"));

        state.setBusy(true);
        assertTrue(state.getCssClasses().contains("is-busy"));

        state.resetFile();
        assertFalse(state.isHasFile());
        assertEquals(0, state.getLinesRead());
    }

    @Test
    void noteAltLabelAndImportPanels_acceptValidationAndReset() {
        var note = new ActionsLotNotePanelState();
        note.acceptFile("notes.csv", "note".getBytes());
        assertTrue(note.isHasFile());
        note.applyValidation(ActionsLotNoteValidationResult.failure("err"));
        note.applyValidation(new ActionsLotNoteValidationResult(true, null, 2, 2, 0, 0, List.of(), List.of()));
        note.applyResult(ActionsLotApplyResult.failure("nope"));
        note.resetFile();
        assertFalse(note.isHasFile());

        var alt = new ActionsLotAltLabelPanelState();
        alt.acceptFile("alt.csv", new byte[100]);
        assertTrue(alt.isHasFile());
        alt.applyValidation(ActionsLotAltLabelValidationResult.failure("bad"));
        alt.applyValidation(new ActionsLotAltLabelValidationResult(true, null, 3, 3, 0, 0, List.of(), List.of()));
        alt.applyResult(new ActionsLotApplyResult(true, "ok", 3, 3, 0));
        assertTrue(alt.isDone());
        alt.resetFile();
        assertFalse(alt.isDone());

        var imp = new ActionsLotImportPanelState<String>();
        imp.acceptFile("import.csv", new byte[10]);
        assertTrue(imp.isHasFile());
        imp.applyValidation(ActionsLotImportValidationResult.failure("x"));
        imp.applyValidation(new ActionsLotImportValidationResult<>(true, null, 1, 1, 0, 0, List.of(), List.of("c")));
        imp.applyResult(new ActionsLotApplyResult(true, "ok", 1, 1, 0));
        imp.resetFile();
        assertFalse(imp.isHasFile());
    }
}
