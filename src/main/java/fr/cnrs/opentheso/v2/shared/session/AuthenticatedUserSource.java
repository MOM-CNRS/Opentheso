package fr.cnrs.opentheso.v2.shared.session;

import java.util.Optional;

/**
 * Lecture / mise à jour minimale de l'utilisateur connecté, sans dépendance directe au legacy.
 */
public interface AuthenticatedUserSource {

    boolean isLoggedIn();

    Optional<Integer> getUserId();

    void refreshDisplayName(String name);

    void refreshEmail(String email);

    void refreshAlertMail(boolean alertMail);
}
