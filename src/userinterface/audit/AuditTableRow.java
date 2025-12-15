package userinterface.audit;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import logic.DTO.AuditLogEntryDTO;
import logic.DTO.InventoryMovementDTO;

import java.time.format.DateTimeFormatter;

public class AuditTableRow {

    private static final int SUMMARY_MAX = 140;

    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty actorPersonnelNumber = new SimpleStringProperty();
    private final StringProperty action = new SimpleStringProperty();
    private final StringProperty entity = new SimpleStringProperty();
    private final StringProperty createdAt = new SimpleStringProperty();
    private final StringProperty ip = new SimpleStringProperty();
    private final StringProperty summary = new SimpleStringProperty();

    public static AuditTableRow fromAudit(AuditLogEntryDTO a, DateTimeFormatter fmt) {
        AuditTableRow r = new AuditTableRow();

        r.id.set(a.getAuditId() == null ? "" : a.getAuditId().toString());
        r.actorPersonnelNumber.set(safe(a.getActorPersonnelNumber()));
        r.action.set(safe(a.getAction()));
        r.entity.set(safe(a.getEntity()));
        r.createdAt.set(a.getCreatedAt() == null ? "" : a.getCreatedAt().format(fmt));
        r.ip.set(safe(a.getIpAddress()));

        String before = a.getBeforeData() == null ? "" : a.getBeforeData();
        String after = a.getAfterData() == null ? "" : a.getAfterData();
        r.summary.set(buildSummary(before, after));

        return r;
    }

    public static AuditTableRow fromMovement(InventoryMovementDTO m, DateTimeFormatter fmt) {
        AuditTableRow r = new AuditTableRow();

        r.id.set(m.getMovementId() == null ? "" : m.getMovementId().toString());
        r.actorPersonnelNumber.set(safe(m.getActorPersonnelNumber()));
        r.action.set(m.getType() == null ? "" : m.getType().name());
        r.entity.set("inventario");
        r.createdAt.set(m.getCreatedAt() == null ? "" : m.getCreatedAt().format(fmt));
        r.ip.set("LOCALHOST");

        String s = "vehiculo=" + safe(m.getVehicleName())
                + " nota=" + safe(m.getNote());
        r.summary.set(compact(s));

        return r;
    }

    private static String buildSummary(String before, String after) {
        String b = compact(before);
        String a = compact(after);

        if (!b.isEmpty() && !a.isEmpty()) return "before=" + b + " | after=" + a;
        if (!a.isEmpty()) return "after=" + a;
        if (!b.isEmpty()) return "before=" + b;
        return "";
    }

    private static String compact(String s) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        if (t.length() <= SUMMARY_MAX) return t;
        return t.substring(0, SUMMARY_MAX) + "...";
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    public String getId() { return id.get(); }
    public String getActorPersonnelNumber() { return actorPersonnelNumber.get(); }
    public String getAction() { return action.get(); }
    public String getEntity() { return entity.get(); }
    public String getCreatedAt() { return createdAt.get(); }
    public String getIp() { return ip.get(); }
    public String getSummary() { return summary.get(); }

    public StringProperty idProperty() { return id; }
    public StringProperty actorPersonnelNumberProperty() { return actorPersonnelNumber; }
    public StringProperty actionProperty() { return action; }
    public StringProperty entityProperty() { return entity; }
    public StringProperty createdAtProperty() { return createdAt; }
    public StringProperty ipProperty() { return ip; }
    public StringProperty summaryProperty() { return summary; }
}
