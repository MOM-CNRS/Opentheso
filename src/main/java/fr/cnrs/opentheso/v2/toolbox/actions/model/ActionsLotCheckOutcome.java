package fr.cnrs.opentheso.v2.toolbox.actions.model;

public interface ActionsLotCheckOutcome {

    boolean success();

    String errorMessage();

    boolean hasErrors();

    int errorCount();

    int validCount();

    int ignoredCount();
}
