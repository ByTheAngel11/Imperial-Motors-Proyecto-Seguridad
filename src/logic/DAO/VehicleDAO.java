package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.VehicleDTO;
import logic.DTO.VehicleStatus;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    private static final String SQL_INSERT =
            "INSERT INTO vehicle (" +
                    "vin, make, model, model_year, color, mileage_km, price, status, " +
                    "supplier_id, acquisition_date, created_at, updated_at, deleted_at" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE vehicle SET " +
                    "vin = ?, make = ?, model = ?, model_year = ?, color = ?, " +
                    "mileage_km = ?, price = ?, status = ?, supplier_id = ?, " +
                    "acquisition_date = ?, created_at = ?, updated_at = ?, deleted_at = ? " +
                    "WHERE vehicle_id = ?";

    private static final String SQL_SOFT_DELETE =
            "UPDATE vehicle SET deleted_at = ? " +
                    "WHERE vehicle_id = ? AND deleted_at IS NULL";

    private static final String SQL_SELECT_BY_ID =
            "SELECT * FROM vehicle WHERE vehicle_id = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT * FROM vehicle WHERE deleted_at IS NULL";

    private static final String SQL_SELECT_BY_VIN =
            "SELECT * FROM vehicle WHERE vin = ? AND deleted_at IS NULL";

    private static final String SQL_SELECT_BY_STATUS =
            "SELECT * FROM vehicle WHERE status = ? AND deleted_at IS NULL";


    public boolean insertVehicle(VehicleDTO vehicle) throws SQLException, IOException {

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, vehicle.getVin());
            statement.setString(2, vehicle.getMake());
            statement.setString(3, vehicle.getModel());
            statement.setShort(4, vehicle.getModelYear());
            statement.setString(5, vehicle.getColor());

            if (vehicle.getMileageKm() != null) {
                statement.setInt(6, vehicle.getMileageKm());
            } else {
                statement.setNull(6, Types.INTEGER);
            }

            statement.setBigDecimal(7, vehicle.getPrice());
            statement.setString(8, vehicle.getStatus().name());

            if (vehicle.getSupplierId() != null) {
                statement.setLong(9, vehicle.getSupplierId());
            } else {
                statement.setNull(9, Types.BIGINT);
            }

            if (vehicle.getAcquisitionDate() != null) {
                statement.setObject(10, vehicle.getAcquisitionDate());
            } else {
                statement.setNull(10, Types.DATE);
            }

            statement.setObject(11, vehicle.getCreatedAt());
            statement.setObject(12, vehicle.getUpdatedAt());
            statement.setObject(13, vehicle.getDeletedAt());

            boolean result = statement.executeUpdate() > 0;

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    vehicle.setVehicleId(keys.getLong(1));
                }
            }

            return result;
        }
    }

    public boolean updateVehicle(VehicleDTO vehicle) throws SQLException, IOException {

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_UPDATE)) {

            statement.setString(1, vehicle.getVin());
            statement.setString(2, vehicle.getMake());
            statement.setString(3, vehicle.getModel());
            statement.setShort(4, vehicle.getModelYear());
            statement.setString(5, vehicle.getColor());

            if (vehicle.getMileageKm() != null) {
                statement.setInt(6, vehicle.getMileageKm());
            } else {
                statement.setNull(6, Types.INTEGER);
            }

            statement.setBigDecimal(7, vehicle.getPrice());
            statement.setString(8, vehicle.getStatus().name());

            if (vehicle.getSupplierId() != null) {
                statement.setLong(9, vehicle.getSupplierId());
            } else {
                statement.setNull(9, Types.BIGINT);
            }

            if (vehicle.getAcquisitionDate() != null) {
                statement.setObject(10, vehicle.getAcquisitionDate());
            } else {
                statement.setNull(10, Types.DATE);
            }

            statement.setObject(11, vehicle.getCreatedAt());
            statement.setObject(12, vehicle.getUpdatedAt());
            statement.setObject(13, vehicle.getDeletedAt());
            statement.setLong(14, vehicle.getVehicleId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteVehicle(Long vehicleId) throws SQLException, IOException {

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SOFT_DELETE)) {

            statement.setObject(1, LocalDateTime.now());
            statement.setLong(2, vehicleId);
            return statement.executeUpdate() > 0;
        }
    }

    public VehicleDTO findVehicleById(Long vehicleId) throws SQLException, IOException {

        VehicleDTO vehicle = VehicleDTO.createNotFoundVehicle();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_ID)) {

            statement.setLong(1, vehicleId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    vehicle = mapResultSetToVehicleDTO(rs);
                }
            }
        }

        return vehicle;
    }

    public List<VehicleDTO> getAllVehicles() throws SQLException, IOException {

        List<VehicleDTO> vehicles = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                vehicles.add(mapResultSetToVehicleDTO(rs));
            }
        }
        return vehicles;
    }

    public List<VehicleDTO> getVehiclesByStatus(VehicleStatus status) throws SQLException, IOException {
        List<VehicleDTO> vehicles = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_STATUS)) {

            statement.setString(1, status.name());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(mapResultSetToVehicleDTO(rs));
                }
            }
        }

        return vehicles;
    }

    public VehicleDTO findVehicleByVin(String vin) throws SQLException, IOException {

        VehicleDTO vehicle = VehicleDTO.createNotFoundVehicle();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_VIN)) {

            statement.setString(1, vin);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    vehicle = mapResultSetToVehicleDTO(rs);
                }
            }
        }
        return vehicle;
    }

    private VehicleDTO mapResultSetToVehicleDTO(ResultSet rs) throws SQLException {

        VehicleDTO vehicle = new VehicleDTO();

        vehicle.setVehicleId(rs.getLong("vehicle_id"));
        vehicle.setVin(rs.getString("vin"));
        vehicle.setMake(rs.getString("make"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setModelYear(rs.getShort("model_year"));
        vehicle.setColor(rs.getString("color"));

        int mileage = rs.getInt("mileage_km");
        if (!rs.wasNull()) {
            vehicle.setMileageKm(mileage);
        }

        vehicle.setPrice(rs.getBigDecimal("price"));
        vehicle.setStatus(VehicleStatus.valueOf(rs.getString("status")));

        long supplierId = rs.getLong("supplier_id");
        if (!rs.wasNull()) {
            vehicle.setSupplierId(supplierId);
        }

        LocalDate acquisitionDate = rs.getObject("acquisition_date", LocalDate.class);
        vehicle.setAcquisitionDate(acquisitionDate);

        vehicle.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        vehicle.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        vehicle.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));

        return vehicle;
    }
}
