package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.UserDTO;
import logic.DTO.UserAccountDTO;
import logic.DTO.AccountRole;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final String SQL_INSERT =
            "INSERT INTO `user` (personnel_number, account_id, username, full_name, phone) " +
                    "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE `user` SET username = ?, full_name = ?, phone = ?, updated_at = ? " +
                    "WHERE personnel_number = ?";

    private static final String SQL_LOGICAL_DELETE =
            "UPDATE `user` SET updated_at = ?, deleted_at = ? WHERE personnel_number = ?";

    private static final String SQL_SELECT_BY_PERSONNEL =
            "SELECT personnel_number, account_id, username, full_name, phone, created_at, updated_at, deleted_at " +
                    "FROM `user` WHERE personnel_number = ?";

    // ✅ CAMBIO: se quitó "AND a.deleted_at IS NULL" para que aparezcan inactivos/eliminados de account como "No"
    private static final String SQL_SELECT_ALL_JOIN =
            "SELECT " +
                    "u.personnel_number, u.account_id, u.username, u.full_name, u.phone, " +
                    "a.email, a.role, a.is_active, " +
                    "COALESCE(u.created_at, a.created_at) AS created_at " +
                    "FROM `user` u " +
                    "JOIN account a ON a.account_id = u.account_id " +
                    "WHERE u.deleted_at IS NULL";

    public void insertUser(Connection connection, UserDTO user) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {
            statement.setString(1, user.getPersonnelNumber());
            statement.setLong(2, user.getAccountId());
            statement.setString(3, user.getUsername());
            statement.setString(4, user.getFullName());
            statement.setString(5, user.getPhone());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
        }
    }

    public void updateUser(Connection connection, UserDTO user) throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getFullName());
            statement.setString(3, user.getPhone());
            statement.setObject(4, now);
            statement.setString(5, user.getPersonnelNumber());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            }
        }
    }

    public void logicalDeleteUser(Connection connection, String personnelNumber) throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        try (PreparedStatement statement = connection.prepareStatement(SQL_LOGICAL_DELETE)) {
            statement.setObject(1, now);
            statement.setObject(2, now);
            statement.setString(3, personnelNumber);

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Logical delete user failed, no rows affected.");
            }
        }
    }

    public UserDTO findUserByPersonnelNumber(String personnelNumber) throws SQLException, IOException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_PERSONNEL)) {

            statement.setString(1, personnelNumber);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    public List<UserAccountDTO> getAllUsersWithAccount() throws SQLException, IOException {
        List<UserAccountDTO> users = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL_JOIN);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                users.add(mapJoin(rs));
            }
        }

        return users;
    }

    private UserDTO mapUser(ResultSet rs) throws SQLException {
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

    private UserAccountDTO mapJoin(ResultSet rs) throws SQLException {
        UserAccountDTO dto = new UserAccountDTO();

        dto.setPersonnelNumber(rs.getString("personnel_number"));
        dto.setAccountId(rs.getLong("account_id"));
        dto.setUsername(rs.getString("username"));
        dto.setFullName(rs.getString("full_name"));
        dto.setPhone(rs.getString("phone"));
        dto.setEmail(rs.getString("email"));

        String dbRole = rs.getString("role");
        if (dbRole != null) {
            dto.setRole(AccountRole.valueOf(dbRole.toUpperCase()));
        }

        dto.setIsActive(rs.getBoolean("is_active"));
        dto.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));

        return dto;
    }
}
