package fr.cnrs.opentheso.bean.rightbody.viewconcept;

import fr.cnrs.opentheso.models.concept.NodeFullConcept;
import fr.cnrs.opentheso.models.nodes.NodeCorpus;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sparql.function.library.leviathan.log;

import javax.net.ssl.*;

@Log4j2
@Getter
@Setter
public class SearchCorpus2 {
    private boolean haveCorpus;

    public List<NodeCorpus> SearchCorpus(List<NodeCorpus> nodeCorpuses, NodeFullConcept nodeFullConcept) {
        haveCorpus = false;
        if (nodeFullConcept != null) {
            for (NodeCorpus nodeCorpus : nodeCorpuses) {

                // cas où on compose uniquement une URL de lien vers les notices
                if (nodeCorpus.isOnlyUriLink()) {
                    if (nodeCorpus.getUriLink().contains("##id##")) {
                        nodeCorpus.setUriLink(nodeCorpus.getUriLink().replace("##id##", nodeFullConcept.getIdentifier()));
                    }
                    if (nodeCorpus.getUriLink().contains("##value##")) {
                        nodeCorpus.setUriLink(nodeCorpus.getUriLink().replace("##value##", nodeFullConcept.getPrefLabel().getLabel()));
                    }
                    haveCorpus = true;
                } else {
                    // recherche par Id
                    /// pour le count par Id interne
                    if (nodeCorpus.getUriCount().contains("##id##")) {
                        if (nodeCorpus.getUriCount() != null && !nodeCorpus.getUriCount().isEmpty()) {
                            nodeCorpus.setUriCount(nodeCorpus.getUriCount().replace("##id##", nodeFullConcept.getIdentifier()));
                        }
                    }
                    /// pour le count par Id ark
                    if (nodeCorpus.getUriCount().contains("##arkid##")) {
                        if (nodeCorpus.getUriCount() != null && !nodeCorpus.getUriCount().isEmpty()) {
                            nodeCorpus.setUriCount(nodeCorpus.getUriCount().replace("##arkid##", nodeFullConcept.getPermanentId()));
                        }
                    }

                    /// pour la construction de l'URL avec Id interne
                    if (nodeCorpus.getUriLink().contains("##id##")) {
                        nodeCorpus.setUriLink(nodeCorpus.getUriLink().replace("##id##", nodeFullConcept.getIdentifier()));
                    }
                    /// pour la construction de l'URL avec Id Ark
                    if (nodeCorpus.getUriLink().contains("##arkid##")) {
                        nodeCorpus.setUriLink(nodeCorpus.getUriLink().replace("##arkid##", nodeFullConcept.getPermanentId()));
                    }

                    // recherche par value
                    if (nodeCorpus.getUriCount().contains("##value##")) {
                        if (nodeCorpus.getUriCount() != null && !nodeCorpus.getUriCount().isEmpty()) {
                            try {
                                nodeCorpus.setUriCount(nodeCorpus.getUriCount().replace("##value##",
                                        URLEncoder.encode(nodeFullConcept.getPrefLabel().getLabel(), StandardCharsets.UTF_8.toString())));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (nodeCorpus.getUriLink().contains("##value##")) {
                        try {
                            nodeCorpus.setUriLink(nodeCorpus.getUriLink().replace("##value##",
                                    URLEncoder.encode(nodeFullConcept.getPrefLabel().getLabel(), StandardCharsets.UTF_8.toString())));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    setCorpusCount(nodeCorpus);
                }
            }
        }
        return nodeCorpuses;
    }

    private void setCorpusCount(NodeCorpus nodeCorpus) {
        if (nodeCorpus == null) {
            return;
        }
        if(nodeCorpus.isOmekaS()){
            nodeCorpus.setCount(getCountOfResourcesFromOmekaS(nodeCorpus.getUriCount()));
        } else {
            nodeCorpus.setCount(getCountOfResourcesFromHttp(nodeCorpus.getUriCount()));
        }
    }

    private int getCountOfResourcesFromHttp(String uri) {
        String output;
        StringBuilder json = new StringBuilder();
        try {
            // ------------------- TRUST MANAGER PERMISSIF (TEST) -------------------
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            // -----------------------------------------------------------------------

            URL url = new URL(uri);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/137.0 Safari/537.36");
            conn.setRequestProperty("User-Agent", "JavaHttpClient"); // optionnel mais recommandé
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
                while ((output = br.readLine()) != null) {
                    json.append(output);
                }
            }

            return getCountFromJson(json.toString());

        } catch (Exception ex) {
            Logger.getLogger(ConceptView.class.getName()).log(Level.SEVERE, null, ex + " " + uri);
        }
        return -1;
    }

    private int getCountFromJson(String jsonText) {
        if (jsonText == null) {
            return -1;
        }
        JsonObject jsonObject;
        try {
            JsonReader reader = Json.createReader(new StringReader(jsonText));
            jsonObject = reader.readObject();
            int count = -1;
            try {
                count = jsonObject.getInt("count");
            } catch (Exception e) {
            }
            ///  récupération du total de HAL SHS
            if(count == -1) {
                try {
                    count = jsonObject.getJsonObject("response").getInt("numFound");
                } catch (Exception e) {
                }
            }
            if (count > 0) {
                haveCorpus = true;
            }
            return count;
        } catch (Exception e) {
            System.err.println(e + " " + jsonText );
            return -1;
        }
    }    

    //// code pour OmekaS
    private int getCountOfResourcesFromOmekaS(String uri) {
        try {
            URL url = new URL(uri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Lire les headers de la réponse
            // Convertir les headers en minuscule
            Map<String, List<String>> headers = connection.getHeaderFields();
            Map<String, List<String>> lowerCaseHeaders = headers.entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey() != null ? entry.getKey().toLowerCase() : "", Map.Entry::getValue));
            List<String> values = lowerCaseHeaders.get("omeka-s-total-results");

            connection.disconnect();

            if (values != null && !values.isEmpty()) {
                int val = Integer.parseInt(values.get(0));
                if(val > 0) haveCorpus = true;
                return val; // Convertir la valeur en entier
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ConceptView.class.getName()).log(Level.SEVERE, null, ex + " " + uri);
        } catch (MalformedURLException ex) {
            Logger.getLogger(ConceptView.class.getName()).log(Level.SEVERE, null, ex + " " + uri);
        } catch (IOException ex) {
            Logger.getLogger(ConceptView.class.getName()).log(Level.SEVERE, null, ex + " " + uri);
        } catch (Exception ex) {
            Logger.getLogger(ConceptView.class.getName()).log(Level.SEVERE, null, ex + " " + uri);
        }
        return -1;
    }

}
