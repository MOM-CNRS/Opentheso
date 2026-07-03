package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ThesaurusHomeOverview;
import fr.cnrs.opentheso.v2.setting.model.ExportUriType;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ThesaurusHomeReadService {

    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ApplicationUriService applicationUriService;

    @Transactional(readOnly = true)
    public ThesaurusHomeOverview loadOverview(String thesaurusId, String lang, String thesaurusTitle) {
        if (StringUtils.isBlank(thesaurusId)) {
            return emptyOverview(thesaurusTitle);
        }
        String workLang = StringUtils.isBlank(lang) ? "fr" : lang;
        String title = StringUtils.defaultIfBlank(thesaurusTitle, thesaurusId);
        String arkId = thesaurusHomeQueryRepository.findArkId(thesaurusId).orElse("");
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, workLang);
        String baseUrl = applicationUriService.resolveApplicationBaseUrl();
        String permalinkUrl = buildPermalinkUrl(thesaurusId, preferences, arkId, baseUrl);
        String permalinkLabel = buildPermalinkLabel(thesaurusId, preferences, arkId, baseUrl);
        Date lastModified = thesaurusHomeQueryRepository.findLastModificationDate(thesaurusId).orElse(null);

        return new ThesaurusHomeOverview(
                title,
                thesaurusHomeQueryRepository.countValidConcepts(thesaurusId),
                thesaurusHomeQueryRepository.findProjectName(thesaurusId).orElse(""),
                lastModified != null ? lastModified.toString() : "",
                permalinkUrl,
                permalinkLabel,
                thesaurusHomeQueryRepository.findLastModifiedConcepts(thesaurusId, workLang),
                thesaurusHomeQueryRepository.findMetadata(thesaurusId),
                thesaurusHomeQueryRepository.findHomePageHtml(thesaurusId, workLang)
        );
    }

    private String buildPermalinkUrl(String thesaurusId, ThesaurusPreferences preferences, String arkId, String baseUrl) {
        if (preferences != null
                && preferences.exportUriType() == ExportUriType.ARK
                && StringUtils.isNotBlank(preferences.originalUri())
                && StringUtils.isNotBlank(arkId)) {
            return preferences.originalUri().replaceAll("/$", "") + "/" + arkId.replaceAll("^/", "");
        }
        if (StringUtils.isBlank(arkId)) {
            return baseUrl + "/v2/thesaurus?idt=" + thesaurusId;
        }
        return baseUrl + "/api/ark:/" + arkId.replaceAll("^/", "");
    }

    private String buildPermalinkLabel(String thesaurusId, ThesaurusPreferences preferences, String arkId, String baseUrl) {
        String url = buildPermalinkUrl(thesaurusId, preferences, arkId, baseUrl);
        if (preferences != null
                && preferences.exportUriType() == ExportUriType.ARK
                && StringUtils.isNotBlank(preferences.originalUri())
                && StringUtils.isNotBlank(arkId)) {
            return url;
        }
        String host = baseUrl.replaceFirst("^https?://", "");
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host + (url.startsWith(baseUrl) ? url.substring(baseUrl.length()) : url);
    }

    private ThesaurusHomeOverview emptyOverview(String thesaurusTitle) {
        return new ThesaurusHomeOverview(
                Objects.toString(thesaurusTitle, ""),
                0,
                "",
                "",
                "",
                "",
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                ""
        );
    }
}
