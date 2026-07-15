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
        return "/v2/graph/visualize/force.xhtml";
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
        return baseUrl + "/openapi/v1/concept/" + thesaurusId + "/thesoGraph?lang=" + resolveLanguage(lang);
    }

    public String buildBranchTreeDataUrl(String baseUrl, String thesaurusId, String conceptId, String lang) {
        return baseUrl + "/openapi/v1/concept/" + thesaurusId + "/" + conceptId + "/graph/?lang=" + resolveLanguage(lang);
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
