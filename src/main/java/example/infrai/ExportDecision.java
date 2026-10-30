package example.infrai;

import java.util.List;

public final class ExportDecision {
    private ExportDecision() {}

    public static String csv(List<ShipmentEvent> events) {
        StringBuilder out = new StringBuilder("shipment_id,occurred_at,status,location,exception\n");
        for (ShipmentEvent event : events) {
            out.append(row(event.shipmentId())).append(',')
                    .append(row(event.occurredAt())).append(',')
                    .append(row(event.status())).append(',')
                    .append(row(event.location())).append(',')
                    .append(row(event.exception())).append('\n');
        }
        return out.toString();
    }

    public static String markdown(List<ShipmentEvent> events) {
        return "# Shipment export\n\nRows: " + events.size() + "\n\n" + csv(events);
    }

    private static String row(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
