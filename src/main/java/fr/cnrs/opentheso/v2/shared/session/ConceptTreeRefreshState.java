package fr.cnrs.opentheso.v2.shared.session;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.io.Serializable;

/**
 * Marqueur session indiquant que l'arbre concept V2 doit être reconstruit
 * (ex. après acceptation d'un candidat).
 */
@SessionScoped
@Named("v2ConceptTreeRefreshState")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ConceptTreeRefreshState implements Serializable {

    private boolean refreshPending;

    public void requestRefresh() {
        refreshPending = true;
    }

    public boolean consumeRefresh() {
        if (!refreshPending) {
            return false;
        }
        refreshPending = false;
        return true;
    }
}
