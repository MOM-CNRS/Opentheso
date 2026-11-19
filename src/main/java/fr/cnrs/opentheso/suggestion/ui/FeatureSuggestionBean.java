package fr.cnrs.opentheso.suggestion.ui;

import fr.cnrs.opentheso.shared.ui.LocaleBean;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.suggestion.service.FeatureSuggestionMailer;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.regex.Pattern;

@Getter
@Setter
@ViewScoped
@Named("featureSuggestionBean")
@RequiredArgsConstructor
public class FeatureSuggestionBean implements Serializable {

    static final String KIND_FEATURE = "feature";
    static final String KIND_IMPROVEMENT = "improvement";
    private static final String SESSION_SENT_AT = "featureSuggestAt";
    private static final long MIN_INTERVAL_MS = 20_000L;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final transient UserSession userSession;
    private final transient LocaleBean localeBean;
    private final transient InstanceFeatureBean instanceFeatureBean;
    private final transient FeatureSuggestionMailer featureSuggestionMailer;

    private String kind = KIND_FEATURE;
    private String subject = "";
    private String message = "";
    private String name = "";
    private String email = "";
    private String website = "";
    private String error;
    private boolean sent;

    public void prepare() {
        kind = KIND_FEATURE;
        subject = "";
        message = "";
        website = "";
        error = null;
        sent = false;
        if (userSession != null && userSession.isLoggedIn()) {
            name = StringUtils.defaultString(userSession.getCurrentUsername());
            email = StringUtils.defaultString(userSession.getCurrentUserEmail());
        } else {
            name = "";
            email = "";
        }
    }

    public void submit() {
        error = null;
        if (StringUtils.isNotBlank(website)) {
            sent = true;
            return;
        }
        if (instanceFeatureBean == null || !instanceFeatureBean.isSuggestionEnabled()) {
            error = text("suggest.error.disabled");
            return;
        }
        String kindCode = normalizedKind();
        String cleanSubject = oneLine(subject);
        String cleanMessage = StringUtils.trimToEmpty(message);
        String cleanName = oneLine(name);
        String cleanEmail = oneLine(email).toLowerCase();
        String problem = validate(kindCode, cleanSubject, cleanMessage, cleanName, cleanEmail);
        if (problem != null) {
            error = problem;
            return;
        }
        if (!featureSuggestionMailer.isConfigured()) {
            error = text("suggest.error.unconfigured");
            return;
        }
        if (tooSoon()) {
            error = text("suggest.error.wait");
            return;
        }
        String kindLabel = text(KIND_FEATURE.equals(kindCode)
                ? "suggest.kind.feature"
                : "suggest.kind.improvement");
        String mailSubject = "[OpenTheso] " + kindLabel + " — " + cleanSubject;
        String html = renderHtml(kindLabel, cleanSubject, cleanMessage, cleanName, cleanEmail);
        if (!featureSuggestionMailer.dispatch(mailSubject, html, cleanEmail)) {
            error = text("suggest.error.unconfigured");
            return;
        }
        rememberSubmit();
        sent = true;
    }

    private String validate(String kindCode, String cleanSubject, String cleanMessage, String cleanName, String cleanEmail) {
        if (kindCode == null) {
            return text("suggest.error.kind");
        }
        if (cleanSubject.length() < 3 || cleanSubject.length() > 160) {
            return text("suggest.error.subject");
        }
        if (cleanMessage.length() < 20 || cleanMessage.length() > 4000) {
            return text("suggest.error.message");
        }
        if (cleanName.length() < 2 || cleanName.length() > 120) {
            return text("suggest.error.name");
        }
        if (cleanEmail.length() > 180 || !EMAIL.matcher(cleanEmail).matches()) {
            return text("suggest.error.email");
        }
        return null;
    }

    private String renderHtml(String kindLabel, String cleanSubject, String cleanMessage, String cleanName, String cleanEmail) {
        String account = userSession != null && userSession.isLoggedIn()
                && StringUtils.isNotBlank(userSession.getCurrentUsername())
                ? userSession.getCurrentUsername()
                : text("suggest.mail.guest");
        return """
                <div style="font-family:sans-serif;font-size:14px;line-height:1.5;color:#1c1917">
                <p><b>%s</b><br>%s</p>
                <p><b>%s</b><br>%s</p>
                <p><b>%s</b><br>%s</p>
                <p><b>%s</b><br>%s &lt;%s&gt;</p>
                <p><b>%s</b><br>%s</p>
                <p><b>%s</b><br>%s</p>
                </div>
                """.formatted(
                escape(text("suggest.mail.kind")),
                escape(kindLabel),
                escape(text("suggest.mail.subject")),
                escape(cleanSubject),
                escape(text("suggest.mail.message")),
                escape(cleanMessage).replace("\n", "<br>"),
                escape(text("suggest.mail.from")),
                escape(cleanName),
                escape(cleanEmail),
                escape(text("suggest.mail.account")),
                escape(account),
                escape(text("suggest.mail.page")),
                escape(currentPage()));
    }

    private String normalizedKind() {
        if (KIND_FEATURE.equals(kind)) {
            return KIND_FEATURE;
        }
        if (KIND_IMPROVEMENT.equals(kind)) {
            return KIND_IMPROVEMENT;
        }
        return null;
    }

    private boolean tooSoon() {
        Long at = sessionStamp();
        return at != null && System.currentTimeMillis() - at < MIN_INTERVAL_MS;
    }

    private void rememberSubmit() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        context.getExternalContext().getSessionMap().put(SESSION_SENT_AT, System.currentTimeMillis());
    }

    private Long sessionStamp() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return null;
        }
        Object raw = context.getExternalContext().getSessionMap().get(SESSION_SENT_AT);
        return raw instanceof Long at ? at : null;
    }

    private String currentPage() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null || !(context.getExternalContext().getRequest() instanceof HttpServletRequest request)) {
            return "";
        }
        String url = request.getRequestURL().toString();
        String query = request.getQueryString();
        return StringUtils.isBlank(query) ? url : url + "?" + query;
    }

    private String text(String key) {
        return localeBean == null ? key : localeBean.getMsg(key);
    }

    private static String oneLine(String value) {
        return StringUtils.trimToEmpty(value).replaceAll("[\\r\\n]+", " ");
    }

    static String escape(String value) {
        return StringUtils.defaultString(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
