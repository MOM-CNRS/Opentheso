package fr.cnrs.opentheso.v2.portal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OntoPortalClientTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;

    @Test
    @SuppressWarnings("unchecked")
    void ontologyExists_returnsFalseOn404() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        assertFalse(client.ontologyExists("https://hsportal.espadon.net", "key", "TH1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void ontologyExists_returnsTrueOn200() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        assertTrue(client.ontologyExists("https://hsportal.espadon.net/", "key", "TH1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void probeOntology_usesListWhenShowReturns500() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        HttpResponse<String> show = org.mockito.Mockito.mock(HttpResponse.class);
        HttpResponse<String> list = org.mockito.Mockito.mock(HttpResponse.class);
        when(show.statusCode()).thenReturn(500);
        when(list.statusCode()).thenReturn(200);
        when(list.body()).thenReturn("[{\"acronym\":\"TH1\"}]");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(show, list);

        assertEquals(
                OntoPortalClient.OntologyProbe.PRESENT,
                client.probeOntology("https://hsportal.espadon.net", "key", "TH1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void probeOntology_isUnreadableWhenShowAndListReturn500() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        assertEquals(
                OntoPortalClient.OntologyProbe.UNREADABLE,
                client.probeOntology("https://hsportal.espadon.net", "key", "TH1"));
        assertFalse(client.ontologyExists("https://hsportal.espadon.net", "key", "TH1"));
    }

    @Test
    void readableBody_replacesPassengerHtml() {
        String message = OntoPortalClient.readableBody(
                "<html>If you are the administrator of this website, then please read this web application's log file</html>",
                500);
        assertTrue(message.contains("échoué"));
        assertFalse(message.contains("administrator"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void ontologyExists_throwsOnUnexpectedClientStatus() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("bad acronym");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        InvalidToolboxDataException ex = assertThrows(InvalidToolboxDataException.class, () ->
                client.ontologyExists("https://hsportal.espadon.net", "key", "TH1"));
        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void probeOntology_fallsBackToGetWhenHeadIsNotAllowed() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        HttpResponse<String> head = org.mockito.Mockito.mock(HttpResponse.class);
        HttpResponse<String> get = org.mockito.Mockito.mock(HttpResponse.class);
        when(head.statusCode()).thenReturn(405);
        when(get.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(head, get);

        assertTrue(client.ontologyExists("https://hsportal.espadon.net", "key", "TH1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createOntology_putsJsonWithUsername() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        client.createOntology("https://hsportal.espadon.net", "secret", "TH1", "Animaux", "alice");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = captor.getValue();
        assertEquals("PUT", sent.method());
        assertEquals(List.of("apikey token=secret"), sent.headers().allValues("Authorization"));
        assertEquals(List.of("application/json"), sent.headers().allValues("Content-Type"));
        assertTrue(sent.uri().toString().startsWith("https://data.hsportal.espadon.net/ontologies/TH1"));
        assertTrue(sent.uri().toString().contains("apikey=secret"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteOntology_sendsDelete() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(204);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        assertTrue(client.deleteOntology("https://hsportal.espadon.net", "secret", "TH1"));

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = captor.getValue();
        assertEquals("DELETE", sent.method());
        assertTrue(sent.uri().toString().startsWith("https://data.hsportal.espadon.net/ontologies/TH1"));
        assertTrue(sent.uri().toString().contains("apikey=secret"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteOntology_treats404AsAlreadyGone() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        assertFalse(client.deleteOntology("https://hsportal.espadon.net", "secret", "TH1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitSkos_postsJsonPullLocation() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        client.submitSkos(
                "https://hsportal.espadon.net",
                "secret",
                "TH1",
                "Alice",
                "a@b.fr",
                "2026-09-25",
                "https://opentheso.fr/openapi/v1/thesaurus/TH1",
                "https://opentheso.fr/?idt=TH1",
                "Animaux"
        );

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = captor.getValue();
        assertEquals("POST", sent.method());
        assertTrue(sent.uri().toString().startsWith("https://data.hsportal.espadon.net/ontologies/TH1/submissions"));
        assertTrue(sent.uri().toString().contains("apikey=secret"));
        assertEquals(List.of("application/json"), sent.headers().allValues("Content-Type"));
    }

    @Test
    void submitSkos_requiresPublicPullLocation() {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        InvalidToolboxDataException ex = assertThrows(InvalidToolboxDataException.class, () ->
                client.submitSkos(
                        "https://hsportal.espadon.net",
                        "secret",
                        "TH1",
                        "Alice",
                        "a@b.fr",
                        "2026-09-25",
                        "localhost/openapi/v1/thesaurus/TH1",
                        "https://opentheso.fr/?idt=TH1",
                        "Animaux"
                ));
        assertTrue(ex.getMessage().contains("pullLocation"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitSkos_replaysPostAfterSamePathRedirect() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        HttpResponse<String> redirect = org.mockito.Mockito.mock(HttpResponse.class);
        HttpResponse<String> created = org.mockito.Mockito.mock(HttpResponse.class);
        when(redirect.statusCode()).thenReturn(302);
        when(redirect.headers()).thenReturn(HttpHeaders.of(
                Map.of("Location", List.of("https://data.hsportal.espadon.net/ontologies/TH1/submissions/")),
                (name, value) -> true));
        when(created.statusCode()).thenReturn(201);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redirect, created);

        client.submitSkos(
                "https://hsportal.espadon.net",
                "secret",
                "TH1",
                "Alice",
                "a@b.fr",
                "2026-09-25",
                "https://opentheso.fr/openapi/v1/thesaurus/TH1",
                "https://opentheso.fr/?idt=TH1",
                "Animaux"
        );

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, org.mockito.Mockito.times(2)).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("POST", captor.getAllValues().get(1).method());
        assertTrue(captor.getAllValues().get(1).uri().toString()
                .startsWith("https://data.hsportal.espadon.net/ontologies/TH1/submissions"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitSkos_explainsLoginRedirect() throws Exception {
        OntoPortalClient client = new OntoPortalClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(302);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Location", List.of("https://hsportal.espadon.net/login")),
                (name, value) -> true));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        InvalidToolboxDataException ex = assertThrows(InvalidToolboxDataException.class, () ->
                client.submitSkos(
                        "https://hsportal.espadon.net",
                        "secret",
                        "TH1",
                        "Alice",
                        "a@b.fr",
                        "2026-09-25",
                        "https://opentheso.fr/openapi/v1/thesaurus/TH1",
                        "https://opentheso.fr/?idt=TH1",
                        "Animaux"
                ));
        assertTrue(ex.getMessage().contains("connexion"));
    }

    @Test
    void readableBody_explainsRedirect() {
        assertTrue(OntoPortalClient.readableBody("<html>moved</html>", 302).contains("redirection"));
    }

    @Test
    void publicOntologyUrl_usesUiHost() {
        assertEquals(
                "https://hsportal.espadon.net/ontologies/TH1",
                OntoPortalClient.publicOntologyUrl("https://hsportal.espadon.net/", "TH1"));
        assertEquals(
                "https://hsportal.espadon.net/ontologies/TH1",
                OntoPortalClient.publicOntologyUrl("https://data.hsportal.espadon.net", "TH1"));
    }

    @Test
    void dateTime_appendsUtcForIsoDate() {
        assertEquals("2026-09-25T00:00:00Z", OntoPortalClient.dateTime("2026-09-25"));
    }

    @Test
    void userIri_usesApiHost() {
        assertEquals(
                "https://data.hsportal.espadon.net/users/alice",
                OntoPortalClient.userIri("https://hsportal.espadon.net", "alice"));
    }

    @Test
    void maskApiKey_hidesQueryToken() {
        assertEquals(
                "https://data.hsportal.espadon.net/ontologies/TH1/submissions?apikey=***",
                OntoPortalClient.maskApiKey(
                        "https://data.hsportal.espadon.net/ontologies/TH1/submissions?apikey=secret"));
    }

    @Test
    void resolveApiBase_prefixesDataHost() {
        assertEquals(
                "https://data.hsportal.espadon.net",
                OntoPortalClient.resolveApiBase("https://hsportal.espadon.net/"));
        assertEquals(
                "https://data.hsportal.espadon.net",
                OntoPortalClient.resolveApiBase("https://data.hsportal.espadon.net"));
    }
}
