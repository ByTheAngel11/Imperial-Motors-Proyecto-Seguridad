package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.AuditAction;
import logic.DTO.InventoryMovementType;
import logic.DTO.SaleDTO;
import logic.DTO.SaleStatus;
import logic.DTO.VehicleStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    private static final String INSERT_SALE_SQL =
            "INSERT INTO sale " +
                    "(folio, vehicle_id, costumer_number, seller_account_id, status, " +
                    "subtotal, discount, taxes, total) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_VEHICLE_STATUS_SQL =
            "UPDATE vehicle SET status = ? WHERE vehicle_id = ?";

    private static final String INSERT_INVENTORY_MOVEMENT_SQL =
            "INSERT INTO inventory_movement " +
                    "(vehicle_id, type, ref_table, ref_id, note, account_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_AUDIT_LOG_SQL =
            "INSERT INTO audit_log " +
                    "(account_id, action, entity, entity_id, before_data, after_data, ip_address) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String ENTITY_SALE = "sale";
    private static final String REF_TABLE_SALE = "sale";
    private static final String DEFAULT_IP_PLACEHOLDER = "127.0.0.1";

    private static final String SELECT_ALL_SALES_SQL =
            "SELECT sale_id, folio, vehicle_id, costumer_number, seller_account_id, status, " +
                    "subtotal, discount, taxes, total, created_at, closed_at, annulled_at, annul_reason " +
                    "FROM sale";

    private static final String SELECT_SALE_BY_ID_SQL =
            SELECT_ALL_SALES_SQL + " WHERE sale_id = ?";

    private static final String UPDATE_SALE_SQL =
            "UPDATE sale SET " +
                    "vehicle_id = ?, " +
                    "costumer_number = ?, " +
                    "status = ?, " +
                    "subtotal = ?, " +
                    "discount = ?, " +
                    "taxes = ?, " +
                    "total = ?, " +
                    "closed_at = ?, " +
                    "annulled_at = ?, " +
                    "annul_reason = ? " +
                    "WHERE sale_id = ?";

    private static final String DEFAULT_ANNUL_REASON =
            "Venta anulada desde módulo de ventas.";

    public long createSaleWithLogAndInventory(SaleDTO sale) throws SQLException {
        validateSaleAmounts(sale);

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            try {
                long saleId = insertSale(connection, sale);
                updateVehicleAsSold(connection, sale.getVehicleId());
                insertInventoryMovement(connection, sale, saleId);
                insertAuditLog(connection, sale, saleId);
                connection.commit();
                return saleId;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void updateSaleWithAudit(SaleDTO sale) throws SQLException {
        validateSaleAmounts(sale);

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            try {
                SaleDTO current = getSaleById(connection, sale.getSaleId());
                if (current == null) {
                    throw new SQLException("Sale with ID " + sale.getSaleId() + " not found.");
                }

                SaleStatus newStatus = sale.getStatus();
                LocalDateTime now = LocalDateTime.now();

                sale.setCreatedAt(current.getCreatedAt());

                if (newStatus == SaleStatus.ANULADA) {
                    sale.setAnnulledAt(now);
                    if (sale.getAnnulReason() == null || sale.getAnnulReason().trim().isEmpty()) {
                        sale.setAnnulReason(DEFAULT_ANNUL_REASON);
                    }
                    sale.setClosedAt(current.getClosedAt());
                } else if (newStatus == SaleStatus.COMPLETADA) {
                    sale.setClosedAt(now);
                    sale.setAnnulledAt(current.getAnnulledAt());
                    sale.setAnnulReason(current.getAnnulReason());
                } else {
                    sale.setClosedAt(null);
                    sale.setAnnulledAt(null);
                    sale.setAnnulReason(null);
                }

                String beforeJson = buildAfterDataJson(current);
                String afterJson = buildAfterDataJson(sale);

                updateSaleRow(connection, sale);

                insertAuditLog(
                        connection,
                        sale.getSellerAccountId(),
                        AuditAction.UPDATE,
                        sale.getSaleId(),
                        beforeJson,
                        afterJson);

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<SaleDTO> getAllSales() throws SQLException {
        List<SaleDTO> sales = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SALES_SQL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                sales.add(mapResultSetToSaleDTO(rs));
            }
        }

        return sales;
    }

    private void validateSaleAmounts(SaleDTO sale) {
        BigDecimal subtotal = nullToZero(sale.getSubtotal());
        BigDecimal discount = nullToZero(sale.getDiscount());
        BigDecimal taxes = nullToZero(sale.getTaxes());

        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtotal cannot be negative.");
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount cannot be negative.");
        }
        if (taxes.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Taxes cannot be negative.");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount cannot be greater than subtotal.");
        }

        BigDecimal expectedTotal = subtotal.subtract(discount).add(taxes);
        if (sale.getTotal() == null) {
            sale.setTotal(expectedTotal);
        } else if (sale.getTotal().compareTo(expectedTotal) != 0) {
            throw new IllegalArgumentException("Total does not match subtotal - discount + taxes.");
        }

        if (sale.getStatus() == null) {
            sale.setStatus(SaleStatus.COMPLETADA);
        }
    }

    private long insertSale(Connection connection, SaleDTO sale) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_SALE_SQL,
                Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, sale.getFolio());
            statement.setLong(2, sale.getVehicleId());
            statement.setString(3, sale.getCostumerNumber());
            statement.setLong(4, sale.getSellerAccountId());
            statement.setString(5, sale.getStatus().name());
            statement.setBigDecimal(6, sale.getSubtotal());
            statement.setBigDecimal(7, nullToZero(sale.getDiscount()));
            statement.setBigDecimal(8, nullToZero(sale.getTaxes()));
            statement.setBigDecimal(9, sale.getTotal());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating sale failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long saleId = generatedKeys.getLong(1);
                    sale.setSaleId(saleId);
                    return saleId;
                } else {
                    throw new SQLException("Creating sale failed, no ID obtained.");
                }
            }
        }
    }

    private void updateVehicleAsSold(Connection connection, Long vehicleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_VEHICLE_STATUS_SQL)) {
            statement.setString(1, VehicleStatus.VENDIDO.name());
            statement.setLong(2, vehicleId);

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating vehicle status failed, no rows affected.");
            }
        }
    }

    private void insertInventoryMovement(Connection connection, SaleDTO sale, long saleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_INVENTORY_MOVEMENT_SQL)) {
            statement.setLong(1, sale.getVehicleId());
            statement.setString(2, InventoryMovementType.VENTA.name());
            statement.setString(3, REF_TABLE_SALE);
            statement.setLong(4, saleId);
            statement.setString(5, "Venta folio " + sale.getFolio());
            statement.setLong(6, sale.getSellerAccountId());
            statement.executeUpdate();
        }
    }

    private void insertAuditLog(Connection connection, SaleDTO sale, long saleId) throws SQLException {
        String afterDataJson = buildAfterDataJson(sale);

        insertAuditLog(
                connection,
                sale.getSellerAccountId(),
                AuditAction.CREATE,
                saleId,
                null,
                afterDataJson);
    }

    private void insertAuditLog(
            Connection connection,
            long accountId,
            AuditAction action,
            long entityId,
            String beforeDataJson,
            String afterDataJson) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(INSERT_AUDIT_LOG_SQL)) {

            if (accountId > 0) {
                statement.setLong(1, accountId);
            } else {
                statement.setNull(1, Types.BIGINT);
            }

            statement.setString(2, action.name());
            statement.setString(3, ENTITY_SALE);
            statement.setLong(4, entityId);

            if (beforeDataJson != null) {
                statement.setString(5, beforeDataJson);
            } else {
                statement.setNull(5, Types.VARCHAR);
            }

            if (afterDataJson != null) {
                statement.setString(6, afterDataJson);
            } else {
                statement.setNull(6, Types.VARCHAR);
            }

            statement.setString(7, DEFAULT_IP_PLACEHOLDER);
            statement.executeUpdate();
        }
    }

    private SaleDTO getSaleById(Connection connection, long saleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_SALE_BY_ID_SQL)) {
            statement.setLong(1, saleId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSaleDTO(rs);
                }
            }
        }
        return null;
    }

    private void updateSaleRow(Connection connection, SaleDTO sale) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SALE_SQL)) {

            statement.setLong(1, sale.getVehicleId());
            statement.setString(2, sale.getCostumerNumber());
            statement.setString(3, sale.getStatus().name());
            statement.setBigDecimal(4, sale.getSubtotal());
            statement.setBigDecimal(5, nullToZero(sale.getDiscount()));
            statement.setBigDecimal(6, nullToZero(sale.getTaxes()));
            statement.setBigDecimal(7, sale.getTotal());

            if (sale.getClosedAt() != null) {
                statement.setObject(8, sale.getClosedAt());
            } else {
                statement.setNull(8, Types.TIMESTAMP);
            }

            if (sale.getAnnulledAt() != null) {
                statement.setObject(9, sale.getAnnulledAt());
            } else {
                statement.setNull(9, Types.TIMESTAMP);
            }

            if (sale.getAnnulReason() != null) {
                statement.setString(10, sale.getAnnulReason());
            } else {
                statement.setNull(10, Types.VARCHAR);
            }

            statement.setLong(11, sale.getSaleId());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating sale failed, no rows affected.");
            }
        }
    }

    private SaleDTO mapResultSetToSaleDTO(ResultSet rs) throws SQLException {
        SaleDTO sale = new SaleDTO();

        sale.setSaleId(rs.getLong("sale_id"));
        sale.setFolio(rs.getString("folio"));
        sale.setVehicleId(rs.getLong("vehicle_id"));
        sale.setCostumerNumber(rs.getString("costumer_number"));
        sale.setSellerAccountId(rs.getLong("seller_account_id"));
        sale.setStatus(SaleStatus.valueOf(rs.getString("status")));
        sale.setSubtotal(rs.getBigDecimal("subtotal"));
        sale.setDiscount(rs.getBigDecimal("discount"));
        sale.setTaxes(rs.getBigDecimal("taxes"));
        sale.setTotal(rs.getBigDecimal("total"));

        sale.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        sale.setClosedAt(rs.getObject("closed_at", LocalDateTime.class));
        sale.setAnnulledAt(rs.getObject("annulled_at", LocalDateTime.class));
        sale.setAnnulReason(rs.getString("annul_reason"));

        return sale;
    }

    private String buildAfterDataJson(SaleDTO sale) {
        return "{"
                + "\"folio\":\"" + escapeJson(sale.getFolio()) + "\","
                + "\"status\":\"" + sale.getStatus().name() + "\","
                + "\"subtotal\":" + sale.getSubtotal() + ","
                + "\"discount\":" + nullToZero(sale.getDiscount()) + ","
                + "\"taxes\":" + nullToZero(sale.getTaxes()) + ","
                + "\"total\":" + sale.getTotal()
                + "}";
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
