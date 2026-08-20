package fr.cnrs.opentheso.v2.concept.export.service;

import fr.cnrs.opentheso.v2.concept.export.model.SelectionExportJob;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SelectionExportJobStore {

    private static final String SESSION_JOB_KEY = "v2.selectionExport.job";
    private static final ConcurrentHashMap<String, SelectionExportJob> JOBS = new ConcurrentHashMap<>();

    public SelectionExportJob current() {
        HttpSession session = currentSession();
        if (session == null) {
            return new SelectionExportJob();
        }
        String sessionId = session.getId();
        if (session.getAttribute(SESSION_JOB_KEY) == null) {
            session.setAttribute(SESSION_JOB_KEY, new SessionBinding(sessionId));
        }
        return JOBS.computeIfAbsent(sessionId, id -> new SelectionExportJob());
    }

    public void clear() {
        HttpSession session = currentSession();
        if (session == null) {
            return;
        }
        remove(session.getId());
        session.removeAttribute(SESSION_JOB_KEY);
    }

    static void remove(String sessionId) {
        if (sessionId == null) {
            return;
        }
        SelectionExportJob job = JOBS.remove(sessionId);
        if (job != null) {
            job.requestCancel();
            job.reset();
        }
    }

    private static HttpSession currentSession() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        return servletAttributes.getRequest().getSession(true);
    }

    static final class SessionBinding implements HttpSessionBindingListener, Serializable {
        private static final long serialVersionUID = 1L;
        private final String sessionId;

        SessionBinding(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            remove(sessionId);
        }
    }
}
