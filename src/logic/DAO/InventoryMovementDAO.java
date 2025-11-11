package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.InventoryMovementDTO;
import logic.DTO.InventoryMovementType;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventoryMovementDAO {

    private static final String SQL_INSERT =
            "INSERT INTO inventory_movement (" +
                    "vehicle_id, type, ref_table, ref_id, note, account_id, created_at" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
            "SELECT * FROM inventory_movement WHERE movement_id = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT * FROM inventory_movement";

    private static final String SQL_SELECT_BY_VEHICLE_ID =
            "SELECT * FROM inventory_movement WHERE vehicle_id = ? ORDER BY created_at";

    private static final String SQL_SELECT_BY_VEHICLE_AND_TYPE =
            "SELECT * FROM inventory_movement WHERE vehicle_id = ? AND type = ? ORDER BY created_at";

    private static final String SQL_SELECT_LAST_BY_VEHICLE_ID =
            "SELECT * FROM inventory_movement " +
                    "WHERE vehicle_id = ? " +
                    "ORDER BY created_at DESC " +
                    "LIMIT 1";
    /**
     * Inserta un movimiento de inventario.
     * Asigna el movement_id generado en el DTO si la inserción es exitosa.
     */
    public boolean insertInventoryMovement(InventoryMovementDTO movement) throws SQLException, IOException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, movement.getVehicleId());
            statement.setString(2, movement.getType().name());

            if (movement.getRefTable() != null) {
                statement.setString(3, movement.getRefTable());
            } else {
                statement.setNull(3, Types.VARCHAR);
            }

            if (movement.getRefId() != null) {
                statement.setLong(4, movement.getRefId());
            } else {
                statement.setNull(4, Types.BIGINT);
            }

            if (movement.getNote() != null) {
                statement.setString(5, movement.getNote());
            } else {
                statement.setNull(5, Types.VARCHAR);
            }

            statement.setLong(6, movement.getAccountId());

            if (movement.getCreatedAt() != null) {
                statement.setObject(7, movement.getCreatedAt());
            } else {
                statement.setNull(7, Types.TIMESTAMP);
            }

            boolean result = statement.executeUpdate() > 0;

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    movement.setMovementId(keys.getLong(1));
                }
            }

            return result;
        }
    }

    /**
     * Busca un movimiento por su ID.
     * Devuelve null si no existe.
     */
    public InventoryMovementDTO findInventoryMovementById(Long movementId) throws SQLException, IOException {
        InventoryMovementDTO movement = null;

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_ID)) {

            statement.setLong(1, movementId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    movement = mapResultSetToInventoryMovementDTO(rs);
                }
            }
        }

        return movement;
    }

    /**
     * Devuelve todos los movimientos de inventario.
     */
    public List<InventoryMovementDTO> getAllInventoryMovements() throws SQLException, IOException {
        List<InventoryMovementDTO> movements = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                movements.add(mapResultSetToInventoryMovementDTO(rs));
            }
        }

        return movements;
    }

    /**
     * Devuelve todos los movimientos para un vehículo específico.
     */
    public List<InventoryMovementDTO> getInventoryMovementsByVehicleId(Long vehicleId) throws SQLException, IOException {
        List<InventoryMovementDTO> movements = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_VEHICLE_ID)) {

            statement.setLong(1, vehicleId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    movements.add(mapResultSetToInventoryMovementDTO(rs));
                }
            }
        }

        return movements;
    }

    /**
     * Devuelve los movimientos de un vehículo filtrados por tipo (ALTA, BAJA, VENTA, etc.).
     */
    public List<InventoryMovementDTO> getInventoryMovementsByVehicleAndType(Long vehicleId, InventoryMovementType type)
            throws SQLException, IOException {

        List<InventoryMovementDTO> movements = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_VEHICLE_AND_TYPE)) {

            statement.setLong(1, vehicleId);
            statement.setString(2, type.name());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    movements.add(mapResultSetToInventoryMovementDTO(rs));
                }
            }
        }

        return movements;
    }

    public InventoryMovementDTO getLastMovementByVehicleId(Long vehicleId) throws SQLException, IOException {

        InventoryMovementDTO movement = null;

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_LAST_BY_VEHICLE_ID)) {

            statement.setLong(1, vehicleId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    movement = mapResultSetToInventoryMovementDTO(rs);
                }
            }
        }

        return movement;
    }

    // ---------- Mapeo ResultSet -> DTO ----------

    private InventoryMovementDTO mapResultSetToInventoryMovementDTO(ResultSet rs) throws SQLException {
        InventoryMovementDTO movement = new InventoryMovementDTO();

        movement.setMovementId(rs.getLong("movement_id"));
        movement.setVehicleId(rs.getLong("vehicle_id"));
        movement.setType(InventoryMovementType.valueOf(rs.getString("type")));

        String refTable = rs.getString("ref_table");
        if (refTable != null) {
            movement.setRefTable(refTable);
        }

        long refId = rs.getLong("ref_id");
        if (!rs.wasNull()) {
            movement.setRefId(refId);
        }

        String note = rs.getString("note");
        if (note != null) {
            movement.setNote(note);
        }

        movement.setAccountId(rs.getLong("account_id"));

        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        movement.setCreatedAt(createdAt);

        return movement;
    }
}
