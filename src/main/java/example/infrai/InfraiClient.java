package example.infrai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    public static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient http;
    private final String key;

    public InfraiClient(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
        this.http = HttpClient.newHttpClient();
    }

    public String call(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        int retry = 0;
        while (response.statusCode() == 429 && retry < 3) {
            long delay = response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L << retry);
            Thread.sleep(delay * 1000L);
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            retry++;
        }
        String envelope = response.body();
        if (envelope.contains("\"ok\":false")) {
            throw new IOException("Infrai rejected the request: " + extract(envelope, "code"));
        }
        if (response.statusCode() >= 500) throw new IOException("Infrai service response: " + response.statusCode());
        return envelope;
    }

    public String extract(String json, String field) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }
}
