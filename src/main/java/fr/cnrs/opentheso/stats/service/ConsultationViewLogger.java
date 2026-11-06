package fr.cnrs.opentheso.stats.service;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Journal de consultation et statistique associée.
 * L'adresse IP reste dans le journal : la table des statistiques ne la stocke pas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationViewLogger {

    private final StatEventService statEventService;

    public void logConcept(String label, String lang, String identifier,
                           String thesaurusName, String thesaurusId, String fallbackLang) {
        Viewed viewed = viewed(label, lang, identifier, fallbackLang);
        info("Concept", viewed, thesaurusName, thesaurusId);
        statEventService.logConceptView(identifier, viewed.label, viewed.lang, thesaurusId, thesaurusName);
    }

    public void logCollection(String label, String lang, String identifier,
                              String thesaurusName, String thesaurusId, String fallbackLang) {
        Viewed viewed = viewed(label, lang, identifier, fallbackLang);
        info("Collection", viewed, thesaurusName, thesaurusId);
        statEventService.logCollectionView(identifier, viewed.label, viewed.lang, thesaurusId, thesaurusName);
    }

    public void logFacet(String label, String lang, String identifier,
                         String thesaurusName, String thesaurusId, String fallbackLang) {
        Viewed viewed = viewed(label, lang, identifier, fallbackLang);
        info("Facette", viewed, thesaurusName, thesaurusId);
        statEventService.logFacetView(identifier, viewed.label, viewed.lang, thesaurusId, thesaurusName);
    }

    public void logCandidate(String label, String lang, String identifier,
                             String thesaurusName, String thesaurusId, String fallbackLang) {
        Viewed viewed = viewed(label, lang, identifier, fallbackLang);
        info("Candidat", viewed, thesaurusName, thesaurusId);
        statEventService.logCandidateView(identifier, viewed.label, viewed.lang, thesaurusId, thesaurusName);
    }

    public void logApiCall(HttpServletRequest request) {
        if (request == null) {
            return;
        }
        String url = request.getRequestURL() == null ? "" : request.getRequestURL().toString();
        String method = StringUtils.defaultString(request.getMethod());
        log.info("API: {}, Méthode: {}, IP: {}", url, method, clientIp(request));
        statEventService.logApiCall(url, method);
    }

    private void info(String kind, Viewed viewed, String thesaurusName, String thesaurusId) {
        log.info("{}: {}, Langue: {}, identifier: {}, Thesaurus: {}, Idt: {}, IP: {}",
                kind,
                viewed.label,
                viewed.lang,
                viewed.identifier,
                thesaurusName,
                thesaurusId,
                clientIpAddress());
    }

    private static Viewed viewed(String label, String lang, String identifier, String fallbackLang) {
        boolean missingLabel = label == null;
        String shownLabel = missingLabel ? "(" + StringUtils.defaultString(identifier) + ")" : label;
        String shownLang = missingLabel
                ? StringUtils.defaultString(fallbackLang)
                : StringUtils.defaultString(lang);
        return new Viewed(shownLabel, shownLang, StringUtils.defaultString(identifier));
    }

    private static String clientIpAddress() {
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            return "";
        }
        Object request = faces.getExternalContext().getRequest();
        if (!(request instanceof HttpServletRequest http)) {
            return "";
        }
        return clientIp(http);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return StringUtils.defaultString(request.getRemoteAddr());
    }

    private record Viewed(String label, String lang, String identifier) {
    }
}
