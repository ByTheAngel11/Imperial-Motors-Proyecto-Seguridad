package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.SupplierDTO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    private static final int ACTIVE_FLAG = 1;
    private static final int INACTIVE_FLAG = 0;

    private static final String BASE_COLUMNS =
            "supplier_id, legal_name, rfc, contact_name, phone, email, " +
                    "is_active, created_at, updated_at, deleted_at";

    private static final String INSERT_SUPPLIER_SQL =
            "INSERT INTO supplier " +
                    "(legal_name, rfc, contact_name, phone, email, is_active, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

    private static final String UPDATE_SUPPLIER_SQL =
            "UPDATE supplier SET " +
                    "legal_name = ?, rfc = ?, contact_name = ?, phone = ?, email = ?, " +
                    "is_active = ?, updated_at = NOW() " +
                    "WHERE supplier_id = ? AND deleted_at IS NULL";

    private static final String SELECT_SUPPLIER_BY_ID_SQL =
            "SELECT " + BASE_COLUMNS + " FROM supplier WHERE supplier_id = ?";

    private static final String SELECT_SUPPLIER_BY_RFC_SQL =
            "SELECT " + BASE_COLUMNS + " FROM supplier " +
                    "WHERE rfc = ? AND deleted_at IS NULL";

    // Solo activos para la tabla
    private static final String SELECT_ALL_ACTIVE_SUPPLIERS_SQL =
            "SELECT " + BASE_COLUMNS + " FROM supplier " +
                    "WHERE deleted_at IS NULL AND is_active = 1 " +
                    "ORDER BY legal_name";

    // Búsqueda de activos (si algún día la quieres en DB)
    private static final String SEARCH_ACTIVE_SUPPLIERS_SQL =
            "SELECT " + BASE_COLUMNS + " FROM supplier " +
                    "WHERE deleted_at IS NULL AND is_active = 1 AND (" +
                    "LOWER(legal_name) LIKE ? OR " +
                    "LOWER(rfc) LIKE ? OR " +
                    "LOWER(contact_name) LIKE ? OR " +
                    "LOWER(phone) LIKE ? OR " +
                    "LOWER(email) LIKE ?" +
                    ") ORDER BY legal_name";

    private static final String EXISTS_RFC_SQL_BASE =
            "SELECT 1 FROM supplier WHERE rfc = ? AND deleted_at IS NULL";
    private static final String EXISTS_PHONE_SQL_BASE =
            "SELECT 1 FROM supplier WHERE phone = ? AND deleted_at IS NULL";
    private static final String EXISTS_EMAIL_SQL_BASE =
            "SELECT 1 FROM supplier WHERE email = ? AND deleted_at IS NULL";
    private static final String LOGICAL_DELETE_SUPPLIER_SQL =
            "UPDATE supplier SET is_active = ?, updated_at = NOW(), deleted_at = NOW() " +
                    "WHERE supplier_id = ? AND deleted_at IS NULL";
    // SupplierDAO

    private static final String SELECT_ALL_SUPPLIERS_SQL =
            "SELECT " + BASE_COLUMNS + " FROM supplier " +
                    "ORDER BY legal_name";

    public void createSupplier(SupplierDTO supplier) throws SQLException {
        if (supplier == null) {
            throw new IllegalArgumentException("supplier is required");
        }

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SUPPLIER_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, supplier.getLegalName());
            statement.setString(2, supplier.getRfc());
            statement.setString(3, supplier.getContactName());
            statement.setString(4, supplier.getPhone());
            statement.setString(5, supplier.getEmail());
            statement.setInt(6, Boolean.TRUE.equals(supplier.getActive()) ? ACTIVE_FLAG : INACTIVE_FLAG);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    supplier.setSupplierId(keys.getLong(1));
                }
            }
        }
    }
    public List<SupplierDTO> getAllSuppliers() throws SQLException {
        List<SupplierDTO> suppliers = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SUPPLIERS_SQL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                suppliers.add(mapRowToSupplier(rs));
            }
        }

        return suppliers;
    }

    public void updateSupplier(SupplierDTO supplier) throws SQLException {
        if (supplier == null || supplier.getSupplierId() == null) {
            throw new IllegalArgumentException("supplier id is required");
        }

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SUPPLIER_SQL)) {

            statement.setString(1, supplier.getLegalName());
            statement.setString(2, supplier.getRfc());
            statement.setString(3, supplier.getContactName());
            statement.setString(4, supplier.getPhone());
            statement.setString(5, supplier.getEmail());
            statement.setInt(6, Boolean.TRUE.equals(supplier.getActive()) ? ACTIVE_FLAG : INACTIVE_FLAG);
            statement.setLong(7, supplier.getSupplierId());

            statement.executeUpdate();
        }
    }

    public void logicalDeleteSupplier(long supplierId) throws SQLException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(LOGICAL_DELETE_SUPPLIER_SQL)) {

            statement.setInt(1, INACTIVE_FLAG);
            statement.setLong(2, supplierId);

            statement.executeUpdate();
        }
    }

    public SupplierDTO findSupplierById(long supplierId) throws SQLException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SUPPLIER_BY_ID_SQL)) {

            statement.setLong(1, supplierId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSupplier(rs);
                }
                return null;
            }
        }
    }

    public SupplierDTO findSupplierByRfc(String rfc) throws SQLException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_SUPPLIER_BY_RFC_SQL)) {

            statement.setString(1, rfc);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSupplier(rs);
                }
                return null;
            }
        }
    }

    public List<SupplierDTO> getAllActiveSuppliers() throws SQLException {
        List<SupplierDTO> suppliers = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_ACTIVE_SUPPLIERS_SQL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                suppliers.add(mapRowToSupplier(rs));
            }
        }

        return suppliers;
    }

    public List<SupplierDTO> searchActiveSuppliers(String filter) throws SQLException {
        String value = filter == null ? "" : filter.trim().toLowerCase();
        String like = "%" + value + "%";

        List<SupplierDTO> suppliers = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH_ACTIVE_SUPPLIERS_SQL)) {

            for (int i = 1; i <= 5; i++) {
                statement.setString(i, like);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    suppliers.add(mapRowToSupplier(rs));
                }
            }
        }

        return suppliers;
    }

    public boolean existsRfc(String rfc, Long excludeId) throws SQLException {
        if (rfc == null) {
            return false;
        }

        String sql = EXISTS_RFC_SQL_BASE + (excludeId != null ? " AND supplier_id <> ?" : "");
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, rfc);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsPhone(String phone, Long excludeId) throws SQLException {
        if (phone == null) {
            return false;
        }

        String sql = EXISTS_PHONE_SQL_BASE + (excludeId != null ? " AND supplier_id <> ?" : "");
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, phone);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsEmail(String email, Long excludeId) throws SQLException {
        if (email == null) {
            return false;
        }

        String sql = EXISTS_EMAIL_SQL_BASE + (excludeId != null ? " AND supplier_id <> ?" : "");
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            if (excludeId != null) {
                statement.setLong(2, excludeId);
            }

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private SupplierDTO mapRowToSupplier(ResultSet rs) throws SQLException {
        SupplierDTO supplier = new SupplierDTO();

        supplier.setSupplierId(rs.getLong("supplier_id"));
        supplier.setLegalName(rs.getString("legal_name"));
        supplier.setRfc(rs.getString("rfc"));
        supplier.setContactName(rs.getString("contact_name"));
        supplier.setPhone(rs.getString("phone"));
        supplier.setEmail(rs.getString("email"));

        boolean isActive = rs.getInt("is_active") == ACTIVE_FLAG;
        supplier.setActive(isActive);

        supplier.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        supplier.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        supplier.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));

        return supplier;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }
}
