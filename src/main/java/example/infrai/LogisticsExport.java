package example.infrai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public final class LogisticsExport {
    private static final String BUCKET = "logistics-export-example";
    private final InfraiClient infrai;

    public LogisticsExport(InfraiClient infrai) { this.infrai = infrai; }

    public String run(List<ShipmentEvent> events) throws IOException, InterruptedException {
        infrai.call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + BUCKET + "\"}");
        String objectKey = "exports/" + Instant.now().toEpochMilli() + ".csv";
        String csv = ExportDecision.csv(events);
        String upload = infrai.call("POST", "/v1/storage/object/presign/" + BUCKET + "/" + objectKey,
                "{\"op\":\"put\",\"expires_seconds\":600,\"content_type\":\"text/csv\"}");
        put(infrai.extract(upload, "url"), csv);

        String head = infrai.call("GET", "/v1/storage/object/head/" + BUCKET + "/" + objectKey, null);
        if (!head.contains("\"found\":true")) throw new IOException("export object was not found after upload");
        infrai.call("GET", "/v1/storage/object/list/" + BUCKET, null); // response rows are under `items`
        String download = infrai.call("POST", "/v1/storage/object/presign/" + BUCKET + "/" + objectKey,
                "{\"op\":\"get\",\"expires_seconds\":600,\"response_disposition\":\"attachment\"}");
        infrai.call("POST", "/v1/pdf/generate", "{\"markdown\":" + quote(ExportDecision.markdown(events))
                + ",\"page_size\":\"A4\",\"orientation\":\"portrait\",\"store\":true}");
        return infrai.extract(download, "url");
    }

    private static void put(String url, String csv) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "text/csv")
                .method("PUT", HttpRequest.BodyPublishers.ofString(csv, StandardCharsets.UTF_8)).build();
        HttpResponse<Void> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() >= 400) throw new IOException("upload response: " + response.statusCode());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
