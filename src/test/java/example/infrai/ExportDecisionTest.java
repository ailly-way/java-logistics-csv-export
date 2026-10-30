package example.infrai;

import java.util.List;

public final class ExportDecisionTest {
    public static void main(String[] args) {
        String csv = ExportDecision.csv(List.of(new ShipmentEvent("S-1", "2026-10-23", "EXCEPTION", "Ningbo", "address, check")));
        String expected = "shipment_id,occurred_at,status,location,exception\n\"S-1\",\"2026-10-23\",\"EXCEPTION\",\"Ningbo\",\"address, check\"\n";
        if (!expected.equals(csv)) throw new AssertionError("exception rows must remain one CSV record");
        System.out.println("ExportDecisionTest passed");
    }
}
