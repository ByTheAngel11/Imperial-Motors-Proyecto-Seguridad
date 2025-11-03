package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.UserDTO;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final String SQL_INSERT = "INSERT INTO user (personnel_number, account_id, username, full_name, phone, created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE user SET account_id = ?, username = ?, full_name = ?, phone = ?, created_at = ?, updated_at = ?, deleted_at = ? WHERE personnel_number = ?";
    private static final String SQL_DELETE = "DELETE FROM user WHERE personnel_number = ?";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM user WHERE personnel_number = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM user";

    public boolean insertUser(UserDTO user) throws SQLException, IOException {
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_INSERT);
            statement.setString(1, user.getPersonnelNumber());
            statement.setLong(2, user.getAccountId());
            statement.setString(3, user.getUsername());
            statement.setString(4, user.getFullName());
            statement.setString(5, user.getPhone());
            statement.setObject(6, user.getCreatedAt());
            statement.setObject(7, user.getUpdatedAt());
            statement.setObject(8, user.getDeletedAt());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateUser(UserDTO user) throws SQLException, IOException {
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_UPDATE);
            statement.setLong(1, user.getAccountId());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getFullName());
            statement.setString(4, user.getPhone());
            statement.setObject(5, user.getCreatedAt());
            statement.setObject(6, user.getUpdatedAt());
            statement.setObject(7, user.getDeletedAt());
            statement.setString(8, user.getPersonnelNumber());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(String personnelNumber) throws SQLException, IOException {
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_DELETE);
            statement.setString(1, personnelNumber);
            return statement.executeUpdate() > 0;
        }
    }

    public UserDTO findUserByPersonnelNumber(String personnelNumber) throws SQLException, IOException {
        UserDTO user = null;
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_ID);
            statement.setString(1, personnelNumber);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    user = mapResultSetToUserDTO(rs);
                }
            }
        }
        return user;
    }

    public List<UserDTO> getAllUsers() throws SQLException, IOException {
        List<UserDTO> users = new ArrayList<>();
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUserDTO(rs));
            }
        }
        return users;
    }

    private UserDTO mapResultSetToUserDTO(ResultSet rs) throws SQLException {
        UserDTO user = new UserDTO();
        user.setPersonnelNumber(rs.getString("personnel_number"));
        user.setAccountId(rs.getLong("account_id"));
        user.setUsername(rs.getString("username"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        user.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        user.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));
        return user;
    }
}