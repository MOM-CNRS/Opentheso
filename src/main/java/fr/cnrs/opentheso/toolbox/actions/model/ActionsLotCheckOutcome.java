package fr.cnrs.opentheso.toolbox.actions.model;

public interface ActionsLotCheckOutcome {

    boolean success();

    String errorMessage();

    boolean hasErrors();

    int errorCount();

    int validCount();

    int ignoredCount();
}
