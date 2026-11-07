package ds;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** GET kérés egy REST API-ra, a JSON választ Java objektummá alakítja. */
public class JsonHttp {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static <T> T get(String url, TypeReference<T> type) throws Exception {
        return get(url, null, type);
    }

    /** GET kérés tokennel, a bejelentkezést igénylő végpontokhoz. */
    public static <T> T get(String url, String token, TypeReference<T> type) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return MAPPER.readValue(response.body(), type);
    }

    public static <T> T post(String url, Object body, String token, TypeReference<T> type) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return MAPPER.readValue(response.body(), type);
    }
}
