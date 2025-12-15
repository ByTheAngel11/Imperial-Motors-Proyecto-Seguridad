package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.AuditLogEntryDTO;
import logic.DTO.InventoryMovementDTO;
import logic.DTO.InventoryMovementType;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditQueryDAO {

    private static final String ENTITY_CUSTOMER = "customer";
    private static final String ENTITY_ACCOUNT = "account";
    private static final String ENTITY_USER = "user";
    private static final String ENTITY_SALE = "sale";
    private static final String ENTITY_PURCHASE_ORDER = "purchase_order";
    private static final String ENTITY_INVENTORY_MOVEMENT = "inventory_movement";

    private static final String SQL_SELECT_AUDIT_ALL =
            "SELECT " +
                    "  al.audit_id, " +
                    "  ua.personnel_number AS actor_personnel_number, " +
                    "  al.action, " +
                    "  al.entity, " +
                    "  CASE al.entity " +
                    "    WHEN '" + ENTITY_ACCOUNT + "' THEN COALESCE(ue.personnel_number, '') " +
                    "    WHEN '" + ENTITY_USER + "' THEN COALESCE(ue.personnel_number, '') " +
                    "    WHEN '" + ENTITY_SALE + "' THEN COALESCE(s.folio, '') " +
                    "    WHEN '" + ENTITY_PURCHASE_ORDER + "' THEN CONCAT('OC-', COALESCE(po.purchase_id, 0)) " +
                    "    WHEN '" + ENTITY_INVENTORY_MOVEMENT + "' THEN COALESCE(CONCAT(v.vin, ' - ', v.make, ' ', v.model, ' ', v.model_year), '') " +
                    "    WHEN '" + ENTITY_CUSTOMER + "' THEN '' " + // no hay JOIN confiable por entity_id=0 en tus logs de customer
                    "    ELSE '' " +
                    "  END AS entity_name, " +
                    "  al.before_data, " +
                    "  al.after_data, " +
                    "  al.ip_address, " +
                    "  al.created_at " +
                    "FROM audit_log al " +
                    "LEFT JOIN `user` ua ON ua.account_id = al.account_id " +
                    "LEFT JOIN `user` ue ON ((al.entity = '" + ENTITY_ACCOUNT + "' OR al.entity = '" + ENTITY_USER + "') AND ue.account_id = al.entity_id) " +
                    "LEFT JOIN sale s ON (al.entity = '" + ENTITY_SALE + "' AND s.sale_id = al.entity_id) " +
                    "LEFT JOIN purchase_order po ON (al.entity = '" + ENTITY_PURCHASE_ORDER + "' AND po.purchase_id = al.entity_id) " +
                    "LEFT JOIN inventory_movement im ON (al.entity = '" + ENTITY_INVENTORY_MOVEMENT + "' AND im.movement_id = al.entity_id) " +
                    "LEFT JOIN vehicle v ON (im.vehicle_id = v.vehicle_id) " +
                    "ORDER BY al.created_at DESC";

    private static final String SQL_SELECT_INVENTORY_ALL =
            "SELECT " +
                    "  im.movement_id, im.vehicle_id, im.type, im.ref_table, im.ref_id, im.note, im.created_at, " +
                    "  ua.personnel_number AS actor_personnel_number, " +
                    "  CONCAT(v.vin, ' - ', v.make, ' ', v.model, ' ', v.model_year) AS vehicle_name " +
                    "FROM inventory_movement im " +
                    "LEFT JOIN `user` ua ON ua.account_id = im.account_id " +
                    "LEFT JOIN vehicle v ON v.vehicle_id = im.vehicle_id " +
                    "ORDER BY im.created_at DESC";

    public List<AuditLogEntryDTO> getAllAuditLogs() throws SQLException, IOException {
        List<AuditLogEntryDTO> list = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_AUDIT_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                list.add(mapAudit(rs));
            }
        }

        return list;
    }

    public List<InventoryMovementDTO> getAllInventoryMovements() throws SQLException, IOException {
        List<InventoryMovementDTO> list = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_INVENTORY_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                list.add(mapInventory(rs));
            }
        }

        return list;
    }

    private AuditLogEntryDTO mapAudit(ResultSet rs) throws SQLException {
        AuditLogEntryDTO a = new AuditLogEntryDTO();

        long auditId = rs.getLong("audit_id");
        if (!rs.wasNull()) {
            a.setAuditId(auditId);
        }

        a.setActorPersonnelNumber(rs.getString("actor_personnel_number"));
        a.setAction(rs.getString("action"));
        a.setEntity(rs.getString("entity"));
        a.setEntityName(rs.getString("entity_name"));

        a.setBeforeData(rs.getString("before_data"));
        a.setAfterData(rs.getString("after_data"));
        a.setIpAddress(rs.getString("ip_address"));

        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        a.setCreatedAt(createdAt);

        return a;
    }

    private InventoryMovementDTO mapInventory(ResultSet rs) throws SQLException {
        InventoryMovementDTO m = new InventoryMovementDTO();

        long movementId = rs.getLong("movement_id");
        if (!rs.wasNull()) {
            m.setMovementId(movementId);
        }

        long vehicleId = rs.getLong("vehicle_id");
        if (!rs.wasNull()) {
            m.setVehicleId(vehicleId);
        }

        String type = rs.getString("type");
        if (type != null && !type.trim().isEmpty()) {
            m.setType(InventoryMovementType.valueOf(type));
        }

        m.setRefTable(rs.getString("ref_table"));

        long refId = rs.getLong("ref_id");
        if (!rs.wasNull()) {
            m.setRefId(refId);
        }

        m.setNote(rs.getString("note"));

        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        m.setCreatedAt(createdAt);

        m.setActorPersonnelNumber(rs.getString("actor_personnel_number"));
        m.setVehicleName(rs.getString("vehicle_name"));

        return m;
    }
}
