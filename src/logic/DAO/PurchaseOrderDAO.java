package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.PurchaseOrderDTO;
import logic.DTO.PurchaseOrderItemDTO;
import logic.DTO.PurchaseStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrderDAO {

    private static final String ENTITY_PURCHASE_ORDER = "purchase_order";
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String INVENTORY_TYPE_ALTA = "ALTA";
    private static final String REF_TABLE_PURCHASE = "purchase_order";
    private static final String VEHICLE_STATUS_AVAILABLE = "DISPONIBLE";

    public Long createPurchaseOrder(PurchaseOrderDTO order) throws SQLException {
        String sqlInsertOrder =
                "INSERT INTO purchase_order " +
                        "(supplier_id, account_id, status, subtotal, discount, taxes, total, expected_date) " +
                        "VALUES (?, ?, 'CREADA', ?, ?, ?, ?, ?)";

        String sqlInsertItem =
                "INSERT INTO purchase_order_item " +
                        "(purchase_id, vehicle_id, agreed_price) " +
                        "VALUES (?, ?, ?)";

        String sqlAudit =
                "INSERT INTO audit_log (account_id, action, entity, entity_id, before_data, after_data, ip_address) " +
                        "VALUES (?, ?, ?, ?, NULL, ?, 'LOCALHOST')";

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            Long purchaseId;

            try (PreparedStatement stmtOrder = connection.prepareStatement(
                    sqlInsertOrder, Statement.RETURN_GENERATED_KEYS)) {

                stmtOrder.setLong(1, order.getSupplierId());
                stmtOrder.setLong(2, order.getAccountId());
                stmtOrder.setBigDecimal(3, defaultAmount(order.getSubtotal()));
                stmtOrder.setBigDecimal(4, defaultAmount(order.getDiscount()));
                stmtOrder.setBigDecimal(5, defaultAmount(order.getTaxes()));
                stmtOrder.setBigDecimal(6, defaultAmount(order.getTotal()));

                if (order.getExpectedDate() != null) {
                    stmtOrder.setDate(7, Date.valueOf(order.getExpectedDate()));
                } else {
                    stmtOrder.setNull(7, Types.DATE);
                }

                stmtOrder.executeUpdate();

                try (ResultSet rs = stmtOrder.getGeneratedKeys()) {
                    if (!rs.next()) {
                        connection.rollback();
                        throw new SQLException("No se pudo obtener el ID de la orden de compra.");
                    }
                    purchaseId = rs.getLong(1);
                }
            }

            // Detalle
            try (PreparedStatement stmtItem = connection.prepareStatement(sqlInsertItem)) {
                for (PurchaseOrderItemDTO item : order.getItems()) {
                    stmtItem.setLong(1, purchaseId);
                    stmtItem.setLong(2, item.getVehicleId());
                    stmtItem.setBigDecimal(3, defaultAmount(item.getAgreedPrice()));
                    stmtItem.addBatch();
                }
                stmtItem.executeBatch();
            }

            // Audit log CREATE
            String afterJson = "{\"status\":\"CREADA\",\"supplierId\":" + order.getSupplierId() + "}";
            try (PreparedStatement stmtAudit = connection.prepareStatement(sqlAudit)) {
                stmtAudit.setLong(1, order.getAccountId());
                stmtAudit.setString(2, ACTION_CREATE);
                stmtAudit.setString(3, ENTITY_PURCHASE_ORDER);
                stmtAudit.setLong(4, purchaseId);
                stmtAudit.setString(5, afterJson);
                stmtAudit.executeUpdate();
            }

            connection.commit();
            return purchaseId;
        }
    }

    public void markAsReceived(long purchaseId, long adminAccountId) throws SQLException {
        String sqlSelectStatus =
                "SELECT status FROM purchase_order WHERE purchase_id = ?";

        String sqlUpdateStatus =
                "UPDATE purchase_order " +
                        "SET status = 'RECIBIDA', received_at = NOW() " +
                        "WHERE purchase_id = ?";

        String sqlInsertInventory =
                "INSERT INTO inventory_movement " +
                        "(vehicle_id, `type`, ref_table, ref_id, note, account_id) " +
                        "SELECT poi.vehicle_id, ?, ?, po.purchase_id, " +
                        "       CONCAT('Compra proveedor ', po.supplier_id), ? " +
                        "FROM purchase_order po " +
                        "JOIN purchase_order_item poi ON poi.purchase_id = po.purchase_id " +
                        "WHERE po.purchase_id = ?";

        String sqlUpdateVehicleStatus =
                "UPDATE vehicle v " +
                        "JOIN purchase_order_item poi ON poi.vehicle_id = v.vehicle_id " +
                        "SET v.status = ? " +
                        "WHERE poi.purchase_id = ?";

        // precio = agreed_price * 1.06
        String sqlUpdateVehiclePrice =
                "UPDATE vehicle v " +
                        "JOIN purchase_order_item poi ON poi.vehicle_id = v.vehicle_id " +
                        "SET v.price = ROUND(poi.agreed_price * 1.06, 2) " +
                        "WHERE poi.purchase_id = ?";

        String sqlAudit =
                "INSERT INTO audit_log (account_id, action, entity, entity_id, before_data, after_data, ip_address) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'LOCALHOST')";

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            // Validar estado actual
            PurchaseStatus currentStatus;

            try (PreparedStatement stmt = connection.prepareStatement(sqlSelectStatus)) {
                stmt.setLong(1, purchaseId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        throw new SQLException("La orden de compra no existe.");
                    }
                    currentStatus = PurchaseStatus.valueOf(rs.getString("status"));
                }
            }

            if (currentStatus != PurchaseStatus.CREADA) {
                connection.rollback();
                throw new SQLException("Solo se pueden recibir órdenes en estado CREADA.");
            }

            // 1) Actualizar orden a RECIBIDA
            try (PreparedStatement stmtUpdate = connection.prepareStatement(sqlUpdateStatus)) {
                stmtUpdate.setLong(1, purchaseId);
                stmtUpdate.executeUpdate();
            }

            // 2) Movimientos de inventario (ALTA)
            try (PreparedStatement stmtInv = connection.prepareStatement(sqlInsertInventory)) {
                stmtInv.setString(1, INVENTORY_TYPE_ALTA);
                stmtInv.setString(2, REF_TABLE_PURCHASE);
                stmtInv.setLong(3, adminAccountId);
                stmtInv.setLong(4, purchaseId);
                stmtInv.executeUpdate();
            }

            // 3) Actualizar estado de los vehículos a DISPONIBLE
            try (PreparedStatement stmtVeh = connection.prepareStatement(sqlUpdateVehicleStatus)) {
                stmtVeh.setString(1, VEHICLE_STATUS_AVAILABLE);
                stmtVeh.setLong(2, purchaseId);
                stmtVeh.executeUpdate();
            }

            // 4) Actualizar precio (6 % más caro que el acordado)
            try (PreparedStatement stmtPrice = connection.prepareStatement(sqlUpdateVehiclePrice)) {
                stmtPrice.setLong(1, purchaseId);
                stmtPrice.executeUpdate();
            }

            // 5) Audit log UPDATE
            String beforeJson = "{\"status\":\"CREADA\"}";
            String afterJson = "{\"status\":\"RECIBIDA\"}";

            try (PreparedStatement stmtAudit = connection.prepareStatement(sqlAudit)) {
                stmtAudit.setLong(1, adminAccountId);
                stmtAudit.setString(2, ACTION_UPDATE);
                stmtAudit.setString(3, ENTITY_PURCHASE_ORDER);
                stmtAudit.setLong(4, purchaseId);
                stmtAudit.setString(5, beforeJson);
                stmtAudit.setString(6, afterJson);
                stmtAudit.executeUpdate();
            }

            connection.commit();
        }
    }

    public void cancelOrder(long purchaseId, long adminAccountId, String reason) throws SQLException {
        String sqlUpdateStatus =
                "UPDATE purchase_order " +
                        "SET status = 'CANCELADA', cancelled_at = NOW(), cancel_reason = ? " +
                        "WHERE purchase_id = ? AND status = 'CREADA'";

        String sqlAudit =
                "INSERT INTO audit_log (account_id, action, entity, entity_id, before_data, after_data, ip_address) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'LOCALHOST')";

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            int updated;
            try (PreparedStatement stmtUpdate = connection.prepareStatement(sqlUpdateStatus)) {
                stmtUpdate.setString(1, reason);
                stmtUpdate.setLong(2, purchaseId);
                updated = stmtUpdate.executeUpdate();
            }

            if (updated == 0) {
                connection.rollback();
                throw new SQLException("Solo se pueden cancelar órdenes en estado CREADA.");
            }

            // Audit log
            String beforeJson = "{\"status\":\"CREADA\"}";
            String afterJson = "{\"status\":\"CANCELADA\"}";

            try (PreparedStatement stmtAudit = connection.prepareStatement(sqlAudit)) {
                stmtAudit.setLong(1, adminAccountId);
                stmtAudit.setString(2, ACTION_UPDATE);
                stmtAudit.setString(3, ENTITY_PURCHASE_ORDER);
                stmtAudit.setLong(4, purchaseId);
                stmtAudit.setString(5, beforeJson);
                stmtAudit.setString(6, afterJson);
                stmtAudit.executeUpdate();
            }

            connection.commit();
        }
    }

    /**
     * Actualiza los datos principales de la orden (proveedor, fecha esperada, subtotal).
     * Solo permite modificar órdenes en estado CREADA.
     */
    public void updatePurchase(PurchaseOrderDTO order, long actorAccountId) throws SQLException {
        if (order == null || order.getPurchaseId() == null) {
            throw new SQLException("La orden de compra a actualizar no es válida.");
        }

        String sqlSelect =
                "SELECT supplier_id, expected_date, subtotal, status " +
                        "FROM purchase_order WHERE purchase_id = ?";

        String sqlUpdate =
                "UPDATE purchase_order " +
                        "SET supplier_id = ?, expected_date = ?, subtotal = ?, updated_at = NOW() " +
                        "WHERE purchase_id = ? AND status = 'CREADA'";

        String sqlAudit =
                "INSERT INTO audit_log (account_id, action, entity, entity_id, before_data, after_data, ip_address) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'LOCALHOST')";

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            Long purchaseId = order.getPurchaseId();
            Long beforeSupplierId = null;
            String beforeExpectedDate = null;
            BigDecimal beforeSubtotal = null;
            String statusStr;

            // Leer valores actuales
            try (PreparedStatement stmtSelect = connection.prepareStatement(sqlSelect)) {
                stmtSelect.setLong(1, purchaseId);
                try (ResultSet rs = stmtSelect.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        throw new SQLException("La orden de compra no existe.");
                    }

                    beforeSupplierId = rs.getLong("supplier_id");
                    Date expectedDateSql = rs.getDate("expected_date");
                    if (expectedDateSql != null) {
                        beforeExpectedDate = expectedDateSql.toString();
                    }
                    beforeSubtotal = rs.getBigDecimal("subtotal");
                    statusStr = rs.getString("status");
                }
            }

            PurchaseStatus currentStatus = PurchaseStatus.valueOf(statusStr);
            if (currentStatus != PurchaseStatus.CREADA) {
                connection.rollback();
                throw new SQLException("Solo se pueden modificar órdenes en estado CREADA.");
            }

            // Ejecutar UPDATE
            try (PreparedStatement stmtUpdate = connection.prepareStatement(sqlUpdate)) {
                stmtUpdate.setLong(1, order.getSupplierId());

                if (order.getExpectedDate() != null) {
                    stmtUpdate.setDate(2, Date.valueOf(order.getExpectedDate()));
                } else {
                    stmtUpdate.setNull(2, Types.DATE);
                }

                stmtUpdate.setBigDecimal(3, defaultAmount(order.getSubtotal()));
                stmtUpdate.setLong(4, purchaseId);

                int updated = stmtUpdate.executeUpdate();
                if (updated == 0) {
                    connection.rollback();
                    throw new SQLException("No se pudo actualizar la orden, verifique el estado.");
                }
            }

            // Audit log con before/after simples
            String beforeJson = String.format(
                    "{\"supplierId\":%d,\"expectedDate\":%s,\"subtotal\":%s}",
                    beforeSupplierId,
                    beforeExpectedDate == null ? "null" : "\"" + beforeExpectedDate + "\"",
                    beforeSubtotal == null ? "null" : "\"" + beforeSubtotal.toPlainString() + "\""
            );

            String afterExpectedDate = order.getExpectedDate() != null
                    ? order.getExpectedDate().toString()
                    : null;

            String afterJson = String.format(
                    "{\"supplierId\":%d,\"expectedDate\":%s,\"subtotal\":\"%s\"}",
                    order.getSupplierId(),
                    afterExpectedDate == null ? "null" : "\"" + afterExpectedDate + "\"",
                    defaultAmount(order.getSubtotal()).toPlainString()
            );

            try (PreparedStatement stmtAudit = connection.prepareStatement(sqlAudit)) {
                stmtAudit.setLong(1, actorAccountId);
                stmtAudit.setString(2, ACTION_UPDATE);
                stmtAudit.setString(3, ENTITY_PURCHASE_ORDER);
                stmtAudit.setLong(4, purchaseId);
                stmtAudit.setString(5, beforeJson);
                stmtAudit.setString(6, afterJson);
                stmtAudit.executeUpdate();
            }

            connection.commit();
        }
    }

    public List<PurchaseOrderDTO> getAllPurchases() throws SQLException {
        String sql =
                "SELECT po.purchase_id, po.supplier_id, po.account_id, po.status, " +
                        "       po.subtotal, po.discount, po.taxes, po.total, po.expected_date, " +
                        "       po.created_at, po.updated_at, po.received_at, po.cancelled_at, po.cancel_reason, " +
                        "       s.legal_name AS supplier_name " +
                        "FROM purchase_order po " +
                        "JOIN supplier s ON s.supplier_id = po.supplier_id " +
                        "ORDER BY po.created_at DESC";

        List<PurchaseOrderDTO> purchases = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                PurchaseOrderDTO dto = new PurchaseOrderDTO();

                dto.setPurchaseId(rs.getLong("purchase_id"));
                dto.setSupplierId(rs.getLong("supplier_id"));
                dto.setAccountId(rs.getLong("account_id"));

                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    dto.setStatus(PurchaseStatus.valueOf(statusStr));
                }

                dto.setSubtotal(rs.getBigDecimal("subtotal"));
                dto.setDiscount(rs.getBigDecimal("discount"));
                dto.setTaxes(rs.getBigDecimal("taxes"));
                dto.setTotal(rs.getBigDecimal("total"));

                Date expected = rs.getDate("expected_date");
                if (expected != null) {
                    dto.setExpectedDate(expected.toLocalDate());
                }

                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    dto.setCreatedAt(created.toLocalDateTime());
                }

                Timestamp updated = rs.getTimestamp("updated_at");
                if (updated != null) {
                    dto.setUpdatedAt(updated.toLocalDateTime());
                }

                Timestamp received = rs.getTimestamp("received_at");
                if (received != null) {
                    dto.setReceivedAt(received.toLocalDateTime());
                }

                Timestamp cancelled = rs.getTimestamp("cancelled_at");
                if (cancelled != null) {
                    dto.setCancelledAt(cancelled.toLocalDateTime());
                }

                dto.setCancelReason(rs.getString("cancel_reason"));
                dto.setSupplierName(rs.getString("supplier_name"));

                purchases.add(dto);
            }
        }

        return purchases;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
