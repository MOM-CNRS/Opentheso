package fr.cnrs.opentheso.bean.session;

import java.util.Iterator;
import java.util.Map;

import jakarta.faces.FacesException;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;

public class ViewExpiredExceptionHandler extends ExceptionHandlerWrapper {

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
        //iterate over unhandler exceptions using the iterator returned from getUnhandledExceptionQueuedEvents().iterator()
        for (Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents()
                .iterator(); i.hasNext();) {
            ExceptionQueuedEvent queuedEvent = i.next();
            ExceptionQueuedEventContext queuedEventContext = (ExceptionQueuedEventContext) queuedEvent
                    .getSource();
            Throwable throwable = queuedEventContext.getException();
            if (throwable instanceof ViewExpiredException) {
                ViewExpiredException viewExpiredException = (ViewExpiredException) throwable;
                FacesContext facesContext = FacesContext.getCurrentInstance();
                Map<String, Object> map = facesContext.getExternalContext()
                        .getRequestMap();
                NavigationHandler navigationHandler = facesContext
                        .getApplication().getNavigationHandler();
                try {
                    map.put("currentViewId", viewExpiredException.getViewId());
                    navigationHandler.handleNavigation(facesContext, null, "/v2/thesaurus?faces-redirect=true");
                    facesContext.renderResponse();
                } finally {
                    i.remove();
                }
            }
        }
        getWrapped().handle();
    }

}
