package fr.cnrs.opentheso.bean.session;

import java.io.IOException;
import java.util.Iterator;

import jakarta.faces.FacesException;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import jakarta.servlet.http.HttpSession;

/**
 * Redirige proprement (y compris en AJAX PrimeFaces) lorsque la vue JSF a expiré.
 * Si la session HTTP est morte, on invalide et on marque {@code sessionExpired=1}.
 */
public class ViewExpiredExceptionHandler extends ExceptionHandlerWrapper {

    private static final String HOME_PATH = "/v2/thesaurus";

    private final ExceptionHandler handler;

    public ViewExpiredExceptionHandler(ExceptionHandler handler) {
        this.handler = handler;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return handler;
    }

    @Override
    public void handle() throws FacesException {
        for (Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator(); i.hasNext();) {
            ExceptionQueuedEvent queuedEvent = i.next();
            ExceptionQueuedEventContext queuedEventContext = (ExceptionQueuedEventContext) queuedEvent.getSource();
            Throwable throwable = queuedEventContext.getException();
            if (!(throwable instanceof ViewExpiredException)) {
                continue;
            }
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                i.remove();
                continue;
            }
            ExternalContext externalContext = facesContext.getExternalContext();
            try {
                boolean sessionDead = isSessionDead(externalContext);
                if (sessionDead) {
                    invalidateQuietly(externalContext);
                }
                String redirectUrl = buildRedirectUrl(externalContext, sessionDead);
                externalContext.redirect(redirectUrl);
                facesContext.responseComplete();
            } catch (IOException ex) {
                throw new FacesException(ex);
            } finally {
                i.remove();
            }
        }
        getWrapped().handle();
    }

    private static boolean isSessionDead(ExternalContext externalContext) {
        Object session = externalContext.getSession(false);
        if (session == null) {
            return true;
        }
        if (session instanceof HttpSession httpSession) {
            try {
                return httpSession.isNew();
            } catch (IllegalStateException ex) {
                return true;
            }
        }
        return false;
    }

    private static void invalidateQuietly(ExternalContext externalContext) {
        try {
            externalContext.invalidateSession();
        } catch (IllegalStateException ignored) {
            // déjà invalidée
        }
    }

    private static String buildRedirectUrl(ExternalContext externalContext, boolean sessionExpired) {
        String contextPath = externalContext.getRequestContextPath();
        if (contextPath == null || "/".equals(contextPath)) {
            contextPath = "";
        }
        String base = contextPath + HOME_PATH;
        return sessionExpired ? base + "?sessionExpired=1" : base;
    }
}
