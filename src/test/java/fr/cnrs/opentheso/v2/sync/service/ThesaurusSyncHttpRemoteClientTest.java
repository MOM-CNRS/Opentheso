package fr.cnrs.opentheso.v2.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptResult;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusSyncHttpRemoteClientTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;

    @Test
    void postBatch_wrapsTransportErrors() {
        ThesaurusSyncHttpRemoteClient client = new ThesaurusSyncHttpRemoteClient(new ObjectMapper());

        SyncBatchRequest request = new SyncBatchRequest(
                "TH1",
                null,
                "a",
                "a@b.fr",
                "c", true,
                List.of(SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "Chat").build())
        );

        assertThrows(InvalidToolboxDataException.class, () ->
                client.postBatch("http://127.0.0.1:1/api/v2/thesaurus/TH/sync/concepts", "key", request));
    }

    @Test
    void objectMapper_roundTripsBatchResponse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SyncBatchResponse original = SyncBatchResponse.from(List.of(
                SyncConceptResult.proposition("C1", "C1", 9),
                SyncConceptResult.candidate("C2", "CA2")
        ));

        String json = mapper.writeValueAsString(original);
        SyncBatchResponse restored = mapper.readValue(json, SyncBatchResponse.class);

        assertEquals(original.total(), restored.total());
        assertEquals(original.propositionsCreated(), restored.propositionsCreated());
        assertEquals(original.candidatesCreated(), restored.candidatesCreated());
        assertEquals(original.results().get(0).propositionId(), restored.results().get(0).propositionId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void postBatch_returnsDeserializedResponseOnHttp200() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ThesaurusSyncHttpRemoteClient client = new ThesaurusSyncHttpRemoteClient(mapper, httpClient);
        SyncBatchResponse expected = SyncBatchResponse.from(List.of(
                SyncConceptResult.proposition("C1", "C1", 3)));
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mapper.writeValueAsString(expected));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        SyncBatchResponse response = client.postBatch(
                "http://localhost/api/v2/thesaurus/TH/sync/concepts",
                "secret",
                new SyncBatchRequest("TH1", null, "a", "a@b.fr", "c", true, List.of(
                        SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "Chat").build()))
        );

        assertEquals(1, response.propositionsCreated());
        assertEquals(3, response.results().get(0).propositionId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void postBatch_throwsWithStatusAndBodyOnHttpError() throws Exception {
        ThesaurusSyncHttpRemoteClient client = new ThesaurusSyncHttpRemoteClient(new ObjectMapper(), httpClient);
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("not master");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        InvalidToolboxDataException ex = assertThrows(InvalidToolboxDataException.class, () ->
                client.postBatch(
                        "http://localhost/api/v2/thesaurus/TH/sync/concepts",
                        "secret",
                        new SyncBatchRequest("TH1", null, "a", "a@b.fr", "c", true, List.of())));

        assertTrue(ex.getMessage().contains("400"));
        assertTrue(ex.getMessage().contains("not master"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void postBatch_sendsApiKeyHeaderAndJsonBody() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ThesaurusSyncHttpRemoteClient client = new ThesaurusSyncHttpRemoteClient(mapper, httpClient);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mapper.writeValueAsString(SyncBatchResponse.from(List.of())));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        SyncBatchRequest request = new SyncBatchRequest(
                "TH1", null, "a", "a@b.fr", "c", true,
                List.of(SyncConceptPayload.builder().identifier("C1").prefLabel("fr", "Chat").build()));
        client.postBatch("http://localhost/api/v2/thesaurus/TH/sync/concepts", "my-key", request);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest sent = captor.getValue();
        assertEquals(List.of("my-key"), sent.headers().allValues("X-API-KEY"));
        assertEquals(List.of("application/json"), sent.headers().allValues("Content-Type"));
        assertEquals("POST", sent.method());
    }
}
