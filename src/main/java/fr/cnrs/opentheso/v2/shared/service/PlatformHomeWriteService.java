package fr.cnrs.opentheso.v2.shared.service;

import fr.cnrs.opentheso.services.HomePageService;
import fr.cnrs.opentheso.utils.ToolsHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Écriture page d'accueil plateforme + Google Analytics.
 * Délègue à {@link HomePageService} (même tables que le legacy).
 */
@Service
@RequiredArgsConstructor
public class PlatformHomeWriteService {

    private final HomePageService homePageService;
    private final PlatformHomeReadService platformHomeReadService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @Transactional(readOnly = true)
    public String loadHomePageHtml(String lang) {
        return platformHomeReadService.loadHomePageHtml(lang);
    }

    @Transactional
    public boolean saveHomePageHtml(String lang, String html) {
        String resolvedLang = StringUtils.isBlank(lang) ? defaultWorkLanguage : lang.toLowerCase();
        String normalized = new ToolsHelper().normalizeHtml(html);
        return homePageService.setHomePage(normalized, resolvedLang);
    }

    @Transactional(readOnly = true)
    public String loadGoogleAnalyticsCode() {
        return StringUtils.defaultString(homePageService.getCodeGoogleAnalytics());
    }

    @Transactional
    public void saveGoogleAnalyticsCode(String code) {
        homePageService.setCodeGoogleAnalytics(StringUtils.defaultString(code));
    }
}
