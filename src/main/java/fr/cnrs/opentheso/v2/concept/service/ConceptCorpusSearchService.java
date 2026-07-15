package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.CorpusSearchContext;
import fr.cnrs.opentheso.v2.concept.model.ConceptCorpusLinkItem;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusCorpusService;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptCorpusSearchService {

    private final ThesaurusCorpusService thesaurusCorpusService;

    @Transactional(readOnly = true)
    public boolean hasActiveCorpus(String thesaurusId) {
        return thesaurusCorpusService.hasActiveCorpus(thesaurusId);
    }

    @Transactional(readOnly = true)
    public List<ConceptCorpusLinkItem> loadCorpusLinks(String thesaurusId, CorpusSearchContext context) {
        if (context == null || StringUtils.isBlank(thesaurusId)) {
            return Collections.emptyList();
        }
        List<ThesaurusCorpus> corpusList = thesaurusCorpusService.listCorpus(thesaurusId);
        if (CollectionUtils.isEmpty(corpusList)) {
            return Collections.emptyList();
        }
        List<ResolvedCorpusLink> resolvedLinks = corpusList.stream()
                .map(ResolvedCorpusLink::from)
                .toList();
        if (!searchCorpus(resolvedLinks, context)) {
            return Collections.emptyList();
        }
        return resolvedLinks.stream()
                .filter(ResolvedCorpusLink::isActive)
                .filter(corpus -> corpus.isOnlyUriLink() || corpus.getCount() > 0)
                .map(corpus -> new ConceptCorpusLinkItem(
                        corpus.getCorpusName(),
                        corpus.getUriLink(),
                        corpus.getCount(),
                        corpus.isOnlyUriLink(),
                        corpus.isActive()
                ))
                .toList();
    }

    private boolean searchCorpus(List<ResolvedCorpusLink> resolvedLinks, CorpusSearchContext context) {
        boolean haveCorpus = false;
        for (ResolvedCorpusLink corpus : resolvedLinks) {
            if (corpus.isOnlyUriLink()) {
                applyOnlyUriLinkPlaceholders(corpus, context);
                haveCorpus = true;
                continue;
            }
            applyCountPlaceholders(corpus, context);
            applyLinkPlaceholders(corpus, context);
            setCorpusCount(corpus);
            if (corpus.getCount() > 0) {
                haveCorpus = true;
            }
        }
        return haveCorpus;
    }

    private void applyOnlyUriLinkPlaceholders(ResolvedCorpusLink corpus, CorpusSearchContext context) {
        if (corpus.getUriLink().contains("##id##")) {
            corpus.setUriLink(corpus.getUriLink().replace("##id##", context.conceptId()));
        }
        if (corpus.getUriLink().contains("##value##")
                && StringUtils.isNotBlank(context.preferredLabel())) {
            corpus.setUriLink(corpus.getUriLink().replace("##value##", context.preferredLabel()));
        }
    }

    private void applyCountPlaceholders(ResolvedCorpusLink corpus, CorpusSearchContext context) {
        if (StringUtils.isNotBlank(corpus.getUriCount()) && corpus.getUriCount().contains("##id##")) {
            corpus.setUriCount(corpus.getUriCount().replace("##id##", context.conceptId()));
        }
        if (StringUtils.isNotBlank(corpus.getUriCount())
                && corpus.getUriCount().contains("##arkid##")
                && StringUtils.isNotBlank(context.arkId())) {
            corpus.setUriCount(corpus.getUriCount().replace("##arkid##", context.arkId()));
        }
        if (StringUtils.isNotBlank(corpus.getUriCount())
                && corpus.getUriCount().contains("##value##")
                && StringUtils.isNotBlank(context.preferredLabel())) {
            corpus.setUriCount(replaceEncodedValue(corpus.getUriCount(), context.preferredLabel()));
        }
    }

    private void applyLinkPlaceholders(ResolvedCorpusLink corpus, CorpusSearchContext context) {
        if (corpus.getUriLink().contains("##id##")) {
            corpus.setUriLink(corpus.getUriLink().replace("##id##", context.conceptId()));
        }
        if (corpus.getUriLink().contains("##arkid##") && StringUtils.isNotBlank(context.arkId())) {
            corpus.setUriLink(corpus.getUriLink().replace("##arkid##", context.arkId()));
        }
        if (corpus.getUriLink().contains("##value##")
                && StringUtils.isNotBlank(context.preferredLabel())) {
            corpus.setUriLink(replaceEncodedValue(corpus.getUriLink(), context.preferredLabel()));
        }
    }

    private String replaceEncodedValue(String source, String value) {
        try {
            return source.replace("##value##", URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            return source;
        }
    }

    private void setCorpusCount(ResolvedCorpusLink corpus) {
        if (corpus.isOmekaS()) {
            corpus.setCount(getCountOfResourcesFromOmekaS(corpus.getUriCount()));
        } else {
            corpus.setCount(getCountOfResourcesFromHttp(corpus.getUriCount()));
        }
    }

    private int getCountOfResourcesFromHttp(String uri) {
        if (StringUtils.isBlank(uri)) {
            return -1;
        }
        StringBuilder json = new StringBuilder();
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HttpURLConnection conn = (HttpURLConnection) new URL(uri).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "JavaHttpClient");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return -1;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(status >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
                String output;
                while ((output = br.readLine()) != null) {
                    json.append(output);
                }
            }
            return getCountFromJson(json.toString());
        } catch (Exception ex) {
            log.warn("Unable to fetch corpus count from {}", uri, ex);
        }
        return -1;
    }

    private int getCountFromJson(String jsonText) {
        if (jsonText == null) {
            return -1;
        }
        try (JsonReader reader = Json.createReader(new StringReader(jsonText))) {
            JsonObject jsonObject = reader.readObject();
            int count = -1;
            try {
                count = jsonObject.getInt("count");
            } catch (Exception ignored) {
            }
            if (count == -1) {
                try {
                    count = jsonObject.getJsonObject("response").getInt("numFound");
                } catch (Exception ignored) {
                }
            }
            return count;
        } catch (Exception e) {
            log.warn("Unable to parse corpus count json", e);
            return -1;
        }
    }

    private int getCountOfResourcesFromOmekaS(String uri) {
        if (StringUtils.isBlank(uri)) {
            return -1;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
            connection.setRequestMethod("GET");
            Map<String, List<String>> headers = connection.getHeaderFields();
            Map<String, List<String>> lowerCaseHeaders = headers.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey() != null ? entry.getKey().toLowerCase() : "",
                            Map.Entry::getValue
                    ));
            List<String> values = lowerCaseHeaders.get("omeka-s-total-results");
            connection.disconnect();
            if (values != null && !values.isEmpty()) {
                return Integer.parseInt(values.get(0));
            }
        } catch (Exception ex) {
            log.warn("Unable to fetch Omeka-S corpus count from {}", uri, ex);
        }
        return -1;
    }

    @Getter
    @Setter
    private static final class ResolvedCorpusLink {
        private String corpusName;
        private boolean active;
        private boolean onlyUriLink;
        private boolean omekaS;
        private String uriLink;
        private String uriCount;
        private int count;

        static ResolvedCorpusLink from(ThesaurusCorpus corpus) {
            ResolvedCorpusLink resolved = new ResolvedCorpusLink();
            resolved.corpusName = corpus.corpusName();
            resolved.active = corpus.active();
            resolved.onlyUriLink = corpus.onlyUriLink();
            resolved.omekaS = corpus.omekaS();
            resolved.uriLink = corpus.uriLink();
            resolved.uriCount = corpus.uriCount();
            return resolved;
        }
    }
}
