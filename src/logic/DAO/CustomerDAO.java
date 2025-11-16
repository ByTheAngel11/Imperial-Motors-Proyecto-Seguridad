package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.CustomerDTO;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private static final String SQL_INSERT = "INSERT INTO customer " +
            "(customer_number, full_name, email, phone, is_active, created_at, updated_at, deleted_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE = "UPDATE customer SET " +
            "full_name = ?, email = ?, phone = ?, is_active = ?, " +
            "created_at = ?, updated_at = ?, deleted_at = ? " +
            "WHERE customer_number = ?";

    private static final String SQL_LOGICAL_DELETE = "UPDATE customer SET " +
            "is_active = 0, updated_at = ?, deleted_at = ? " +
            "WHERE customer_number = ?";

    private static final String SQL_SELECT_BY_ID =
            "SELECT * FROM customer WHERE customer_number = ?";

    private static final String SQL_SELECT_ALL_ACTIVE =
            "SELECT * FROM customer WHERE is_active = 1 AND deleted_at IS NULL";

    private static final String SQL_SELECT_ALL_INACTIVE =
            "SELECT * FROM customer WHERE is_active = 0 OR deleted_at IS NOT NULL";

    private static final String SQL_SELECT_BY_EMAIL =
            "SELECT * FROM customer WHERE email = ? AND deleted_at IS NULL";

    private static final String SQL_SELECT_LAST_NUMBER =
            "SELECT customer_number FROM customer ORDER BY customer_number DESC LIMIT 1";

    public boolean registerCustomer(CustomerDTO customer) throws SQLException, IOException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {

            // Generar ID automáticamente tipo C0001, C0002, ...
            String nextNumber = getNextCustomerNumber(connection);
            customer.setCostumerNumber(nextNumber);

            statement.setString(1, customer.getCostumerNumber());
            statement.setString(2, customer.getFullName());
            statement.setString(3, customer.getEmail());
            statement.setString(4, customer.getPhone());
            statement.setBoolean(5, customer.getIsActive());
            statement.setObject(6, customer.getCreatedAt());
            statement.setObject(7, customer.getUpdatedAt());
            statement.setObject(8, customer.getDeletedAt());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateCustomer(CustomerDTO customer) throws SQLException, IOException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_UPDATE)) {

            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getEmail());
            statement.setString(3, customer.getPhone());
            statement.setBoolean(4, customer.getIsActive());
            statement.setObject(5, customer.getCreatedAt());
            statement.setObject(6, customer.getUpdatedAt());
            statement.setObject(7, customer.getDeletedAt());
            statement.setString(8, customer.getCostumerNumber());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean logicalDeleteCustomer(String costumerNumber) throws SQLException, IOException {
        LocalDateTime now = LocalDateTime.now();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_LOGICAL_DELETE)) {

            statement.setObject(1, now);
            statement.setObject(2, now);
            statement.setString(3, costumerNumber);

            return statement.executeUpdate() > 0;
        }
    }

    public CustomerDTO getCustomerDetails(String costumerNumber) throws SQLException, IOException {
        CustomerDTO customer = null;

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_ID)) {

            statement.setString(1, costumerNumber);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    customer = mapResultSetToCustomerDTO(rs);
                }
            }
        }

        return customer;
    }

    public List<CustomerDTO> getActiveCustomers() throws SQLException, IOException {
        List<CustomerDTO> customers = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL_ACTIVE);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                customers.add(mapResultSetToCustomerDTO(rs));
            }
        }

        return customers;
    }

    public List<CustomerDTO> getInactiveCustomers() throws SQLException, IOException {
        List<CustomerDTO> customers = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL_INACTIVE);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                customers.add(mapResultSetToCustomerDTO(rs));
            }
        }

        return customers;
    }

    public CustomerDTO findCustomerByEmail(String email) throws SQLException, IOException {
        CustomerDTO customer = null;

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_EMAIL)) {

            statement.setString(1, email);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    customer = mapResultSetToCustomerDTO(rs);
                }
            }
        }

        return customer;
    }

    private CustomerDTO mapResultSetToCustomerDTO(ResultSet rs) throws SQLException {
        CustomerDTO customer = new CustomerDTO();

        customer.setCostumerNumber(rs.getString("customer_number"));
        customer.setFullName(rs.getString("full_name"));
        customer.setEmail(rs.getString("email"));
        customer.setPhone(rs.getString("phone"));
        customer.setIsActive(rs.getBoolean("is_active"));
        customer.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        customer.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        customer.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));

        return customer;
    }

    private String getNextCustomerNumber(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQL_SELECT_LAST_NUMBER);
             ResultSet rs = statement.executeQuery()) {

            if (!rs.next()) {
                return "C0001";
            }

            String last = rs.getString(1);
            String numeric = last.replaceAll("\\D+", "");

            int number;
            if (numeric.isEmpty()) {
                number = 1;
            } else {
                number = Integer.parseInt(numeric) + 1;
            }

            return String.format("C%04d", number);
        }
    }
}
