package example.infrai;

public record ShipmentEvent(String shipmentId, String occurredAt, String status, String location, String exception) {}
