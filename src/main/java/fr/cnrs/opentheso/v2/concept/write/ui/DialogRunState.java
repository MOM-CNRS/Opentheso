package fr.cnrs.opentheso.v2.concept.write.ui;

import java.io.Serializable;

/**
 * Cycle d'une action guidée V2 : idle → error | done, plus toast flash.
 */
public class DialogRunState implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String IDLE = "";
    public static final String DONE = "done";
    public static final String ERROR = "error";

    private String state = IDLE;
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isDone() {
        return DONE.equals(state);
    }

    public void reset() {
        state = IDLE;
        errorMessage = null;
        flashMessage = null;
        flashToken = null;
    }

    public void fail(String message) {
        state = ERROR;
        errorMessage = message;
    }

    public void succeed(String message) {
        state = DONE;
        errorMessage = null;
        flash(message);
    }

    /** Termine en succès sans effacer un avertissement déjà posé. */
    public void complete(String message) {
        state = DONE;
        flash(message);
    }

    public void flash(String message) {
        flashMessage = message;
        flashToken = String.valueOf(System.currentTimeMillis());
    }

    public void clearFlash() {
        flashMessage = null;
        flashToken = null;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state == null ? IDLE : state;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFlashMessage() {
        return flashMessage;
    }

    public void setFlashMessage(String flashMessage) {
        this.flashMessage = flashMessage;
    }

    public String getFlashToken() {
        return flashToken;
    }

    public void setFlashToken(String flashToken) {
        this.flashToken = flashToken;
    }
}
