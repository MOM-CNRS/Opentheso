package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.v2.graph.model.GraphExportEntry;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import fr.cnrs.opentheso.v2.graph.model.GraphViewSummary;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GraphVisualizationUrlService {

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    private final GraphViewReadService graphViewReadService;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    public String buildForceGraphViewerPath() {
        return "/d3js/index.xhtml";
    }

    public String buildVisualizationUrl(String viewId, String baseUrl, String lang) throws URISyntaxException {
        var view = graphViewReadService.loadView(viewId);
        if (view == null) {
            return null;
        }
        return buildVisualizationUrl(view, baseUrl, lang);
    }

    public String buildVisualizationUrl(GraphViewSummary view, String baseUrl, String lang) throws URISyntaxException {
        if (view == null) {
            return null;
        }

        var dataUrlBuilder = new URIBuilder(baseUrl + "/openapi/v1/graph/getData");
        dataUrlBuilder.addParameter("lang", resolveLanguage(lang));
        // Limite le volume renvoyé pour accélérer le premier rendu D3js.
        dataUrlBuilder.addParameter("limit", "true");

        appendExportParameters(dataUrlBuilder, view.getExports());

        var viewerBuilder = new URIBuilder(baseUrl + buildForceGraphViewerPath());
        viewerBuilder.addParameter("dataUrl", dataUrlBuilder.build().toString());
        viewerBuilder.addParameter("format", "opentheso");
        return viewerBuilder.build().toString();
    }

    public String appendTitle(String url, String title) {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(title)) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "title=" + java.net.URLEncoder.encode(title, StandardCharsets.UTF_8);
    }

    public String buildThesaurusTreeDataUrl(String baseUrl, String thesaurusId, String lang) {
        String root = normalizeBaseUrl(baseUrl);
        return root + "/openapi/v1/concept/" + encodePath(thesaurusId)
                + "/thesoGraph?lang=" + resolveLanguage(lang);
    }

    public String buildBranchTreeDataUrl(String baseUrl, String thesaurusId, String conceptId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            return null;
        }
        String root = normalizeBaseUrl(baseUrl);
        return root + "/openapi/v1/concept/" + encodePath(thesaurusId)
                + "/" + encodePath(conceptId)
                + "/graph/?lang=" + resolveLanguage(lang);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String encodePath(String value) {
        return java.net.URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private void appendExportParameters(URIBuilder dataUrlBuilder, List<GraphExportEntry> exports) {
        if (exports == null) {
            return;
        }
        for (GraphExportEntry export : exports) {
            String idThesoConcept = StringUtils.isBlank(export.conceptId())
                    ? export.thesaurusId()
                    : export.thesaurusId() + ":" + export.conceptId();
            dataUrlBuilder.addParameter("idThesoConcept", idThesoConcept);
        }
    }

    private String resolveLanguage(String lang) {
        return StringUtils.isBlank(lang) ? defaultWorkLanguage : lang;
    }

    public String resolveWorkLanguageForThesaurus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return defaultWorkLanguage;
        }
        return thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }
}
