package fr.cnrs.opentheso.v2.setting.exception;

public class SettingAccessDeniedException extends RuntimeException {

    public SettingAccessDeniedException() {
        super("Accès refusé aux paramètres du thésaurus.");
    }
}
