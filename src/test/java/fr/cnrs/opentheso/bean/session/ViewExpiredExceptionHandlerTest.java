package fr.cnrs.opentheso.bean.session;

import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewExpiredExceptionHandlerTest {

    @Mock
    private ExceptionHandler wrapped;
    @Mock
    private FacesContext facesContext;
    @Mock
    private ExternalContext externalContext;
    @Mock
    private HttpSession httpSession;

    @Test
    void handle_redirectsWithSessionExpiredWhenSessionMissing() throws Exception {
        ExceptionQueuedEvent event = viewExpiredEvent();
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(new ArrayList<>(List.of(event)));
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getSession(false)).thenReturn(null);
        when(externalContext.getRequestContextPath()).thenReturn("/opentheso");

        ViewExpiredExceptionHandler handler = new ViewExpiredExceptionHandler(wrapped);

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            handler.handle();
        }

        verify(externalContext).invalidateSession();
        verify(externalContext).redirect("/opentheso/index.xhtml?sessionExpired=1");
        verify(facesContext).responseComplete();
        verify(wrapped).handle();
    }

    @Test
    void handle_redirectsHomeWithoutInvalidateWhenSessionStillAlive() throws Exception {
        ExceptionQueuedEvent event = viewExpiredEvent();
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(new ArrayList<>(List.of(event)));
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getSession(false)).thenReturn(httpSession);
        when(httpSession.isNew()).thenReturn(false);
        when(externalContext.getRequestContextPath()).thenReturn("/");

        ViewExpiredExceptionHandler handler = new ViewExpiredExceptionHandler(wrapped);

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            handler.handle();
        }

        verify(externalContext, never()).invalidateSession();
        verify(externalContext).redirect("/index.xhtml");
        verify(facesContext).responseComplete();
        verify(wrapped).handle();
    }

    @Test
    void handle_treatsNewSessionAsExpired() throws Exception {
        ExceptionQueuedEvent event = viewExpiredEvent();
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(new ArrayList<>(List.of(event)));
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        when(externalContext.getSession(false)).thenReturn(httpSession);
        when(httpSession.isNew()).thenReturn(true);
        when(externalContext.getRequestContextPath()).thenReturn("");

        ViewExpiredExceptionHandler handler = new ViewExpiredExceptionHandler(wrapped);

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            handler.handle();
        }

        verify(externalContext).invalidateSession();
        verify(externalContext).redirect("/index.xhtml?sessionExpired=1");
    }

    @Test
    void handle_ignoresNonViewExpiredExceptions() throws Exception {
        ExceptionQueuedEventContext context = mock(ExceptionQueuedEventContext.class);
        when(context.getException()).thenReturn(new IllegalStateException("other"));
        ExceptionQueuedEvent event = mock(ExceptionQueuedEvent.class);
        when(event.getSource()).thenReturn(context);
        when(wrapped.getUnhandledExceptionQueuedEvents()).thenReturn(new ArrayList<>(List.of(event)));

        ViewExpiredExceptionHandler handler = new ViewExpiredExceptionHandler(wrapped);

        try (MockedStatic<FacesContext> faces = mockStatic(FacesContext.class)) {
            faces.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            handler.handle();
        }

        verify(externalContext, never()).redirect(org.mockito.ArgumentMatchers.anyString());
        verify(wrapped).handle();
    }

    private static ExceptionQueuedEvent viewExpiredEvent() {
        ExceptionQueuedEventContext context = mock(ExceptionQueuedEventContext.class);
        when(context.getException()).thenReturn(new ViewExpiredException("expired", "/v2/graph"));
        ExceptionQueuedEvent event = mock(ExceptionQueuedEvent.class);
        when(event.getSource()).thenReturn(context);
        return event;
    }
}
