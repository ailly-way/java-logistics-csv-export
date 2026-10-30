package example.infrai;

import java.util.List;

public final class ExportExample {
    public static void main(String[] args) throws Exception {
        InfraiClient client = new InfraiClient(System.getenv("INFRAI_API_KEY"));
        List<ShipmentEvent> events = List.of(
                new ShipmentEvent("SHP-1042", "2026-10-23T09:15:00Z", "DELIVERED", "Shanghai", ""),
                new ShipmentEvent("SHP-1043", "2026-10-23T10:20:00Z", "EXCEPTION", "Suzhou", "address-check"));
        String downloadUrl = new LogisticsExport(client).run(events);
        System.out.println("Download URL: " + downloadUrl);
    }
}
