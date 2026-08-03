package fr.cnrs.opentheso.v2.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ThesaurusSyncHttpRemoteClient implements ThesaurusSyncRemoteClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ThesaurusSyncHttpRemoteClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    /** Constructeur de test (HttpClient mockable). */
    ThesaurusSyncHttpRemoteClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public SyncBatchResponse postBatch(String endpoint, String apiKey, SyncBatchRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), SyncBatchResponse.class);
            }
            throw new InvalidToolboxDataException(
                    "Erreur HTTP " + response.statusCode() + " lors de l'appel au maître: "
                            + StringUtils.abbreviate(response.body(), 300));
        } catch (InvalidToolboxDataException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidToolboxDataException(
                    "Impossible de contacter le serveur maître: " + ex.getMessage());
        }
    }
}
