package fr.cnrs.opentheso.client;

import fr.cnrs.opentheso.ws.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import java.util.List;
@Slf4j
@Service
public class ArkApiClient {

    private final RestTemplate restTemplate;

    public ArkApiClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public boolean arkExistsById(String arkId, Integer naan, String urlServerOpenArk) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(urlServerOpenArk)
                    .path("/arks/exists-by-id")
                    .queryParam("naan", naan)
                    .queryParam("arkId", arkId)
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<ArkExistsResponse> response =
                    restTemplate.getForEntity(uri, ArkExistsResponse.class);

            ArkExistsResponse body = response.getBody();
            return body != null && body.isExists();

        } catch (Exception e) {
            log.error("Erreur lors de la vérification d'existence ARK", e);
            throw new ArkApiException("Erreur lors de la vérification d'existence ARK");
        }
    }

    // #MR Ok validé
    public boolean arkExistsByUrl(Integer naan, String urlArk, String urlServerOpenArk) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(urlServerOpenArk)
                    .path("/arks/exists-by-url")
                    .queryParam("naan", naan)
                    .queryParam("url", urlArk)
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<ArkExistsResponse> response =
                    restTemplate.getForEntity(uri, ArkExistsResponse.class);

            return response.getBody() != null && response.getBody().isExists();
        } catch (Exception e) {
            log.error("Erreur lors de la vérification d'existence ARK par URL", e);
            throw new ArkApiException("Erreur lors de la vérification d'existence ARK par URL");
        }
    }

    public ArkResponse getArkByNaanAndUrlWithApiKey(
            Integer naan,
            String urlArk,
            String urlServerOpenArk,
            String apiKey) {

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(urlServerOpenArk)
                    .path("/getArkByNaanUrl")
                    .queryParam("naan", naan)
                    .queryParam("url", urlArk)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            headers.setAccept(List.of(MediaType.ALL));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ArkResponse> response =
                    restTemplate.exchange(uri, HttpMethod.POST, entity, ArkResponse.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Erreur lors de l'appel getArkByNaanUrl avec API Key", e);
            throw new ArkApiException("Erreur lors de l'appel getArkByNaanUrl");
        }
    }

    public ArkResponse createArk(ArkRequest request, String urlServerOpenArk, String apiKey) {

        // "http://localhost:8080/api/addArk";
        String url = urlServerOpenArk + "/addArk";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", apiKey);

            HttpEntity<ArkRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ArkResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, ArkResponse.class);

            return response.getBody();

        } catch (HttpClientErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode(); // Spring 6+
            String body = e.getResponseBodyAsString();

            if (statusCode.value() == 401) {
                log.warn("Clé API invalide : {}", body);
            } else {
                log.error("Erreur HTTP OpenArk {} : {}", statusCode.value(), body);
            }

            throw new ArkApiException("Clé API invalide : " + body);
        } catch (ResourceAccessException e) {
            // Serveur inaccessible
            log.error("Serveur OpenArk inaccessible : {}", url);
            throw new ArkApiException("Serveur OpenArk indisponible");

        } catch (Exception e) {
            log.error("Erreur technique OpenArk", e);
            throw new ArkApiException("Erreur technique lors de l'appel OpenArk");
        }
    }

    public ArkResponse updateArk(
            ArkRequest request,
            String urlServerOpenArk,
            String apiKey) {

        try {
            // Construire l'URL du PUT
            String url = urlServerOpenArk + "/updateArk";

            // Préparer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-API-KEY", apiKey);

            // Créer la requête avec le corps JSON
            HttpEntity<ArkRequest> entity = new HttpEntity<>(request, headers);

            // Exécuter le PUT
            ResponseEntity<ArkResponse> response =
                    restTemplate.exchange(url, HttpMethod.PUT, entity, ArkResponse.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de l'ARK", e);
            throw new ArkApiException("Erreur lors de la mise à jour de l'ARK");
        }
    }

    public DeleteArkResponse deleteArk(
            DeleteArkRequest request,
            String urlServerOpenArk,
            String apiKey) {

        try {
            // URL du endpoint DELETE
            String url = urlServerOpenArk + "/deleteArk";

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-API-KEY", apiKey);

            // Body JSON
            HttpEntity<DeleteArkRequest> entity = new HttpEntity<>(request, headers);

            // Appel DELETE
            ResponseEntity<DeleteArkResponse> response =
                    restTemplate.exchange(url, HttpMethod.DELETE, entity, DeleteArkResponse.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'ARK", e);
            throw new ArkApiException("Erreur lors de la suppression de l'ARK");
        }
    }
}
