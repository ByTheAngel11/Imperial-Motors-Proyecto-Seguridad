package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.AccountDTO;
import logic.DTO.AccountRole;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {

    private static final String SQL_INSERT =
            "INSERT INTO account (email, password_hash, role, is_active) VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
            "SELECT account_id, email, password_hash, role, is_active, created_at, updated_at, deleted_at " +
                    "FROM account WHERE account_id = ?";

    private static final String SQL_SELECT_BY_EMAIL =
            "SELECT account_id, email, password_hash, role, is_active, created_at, updated_at, deleted_at " +
                    "FROM account WHERE email = ?";

    private static final String SQL_SELECT_ALL =
            "SELECT account_id, email, password_hash, role, is_active, created_at, updated_at, deleted_at " +
                    "FROM account";

    private static final String SQL_UPDATE_CORE =
            "UPDATE account SET email = ?, role = ?, is_active = ?, updated_at = ? WHERE account_id = ?";

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE account SET password_hash = ?, updated_at = ? WHERE account_id = ?";

    private static final String SQL_LOGICAL_DELETE =
            "UPDATE account SET is_active = 0, updated_at = ?, deleted_at = ? WHERE account_id = ?";

    public long insertAccount(Connection connection, AccountDTO account) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, account.getEmail());
            statement.setString(2, account.getPasswordHash());
            statement.setString(3, normalizeRoleForDb(account.getRole()));
            statement.setBoolean(4, account.getIsActive());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating account failed, no rows affected.");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating account failed, no ID obtained.");
                }
                long id = keys.getLong(1);
                account.setAccountId(id);
                return id;
            }
        }
    }

    public AccountDTO findAccountById(long accountId) throws SQLException, IOException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_ID)) {

            statement.setLong(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public AccountDTO findAccountByEmail(String email) throws SQLException, IOException {
        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_EMAIL)) {

            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public List<AccountDTO> getAllAccounts() throws SQLException, IOException {
        List<AccountDTO> accounts = new ArrayList<>();

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                accounts.add(map(rs));
            }
        }
        return accounts;
    }

    public void updateAccountCore(Connection connection, AccountDTO account) throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_CORE)) {
            statement.setString(1, account.getEmail());
            statement.setString(2, normalizeRoleForDb(account.getRole()));
            statement.setBoolean(3, account.getIsActive());
            statement.setObject(4, now);
            statement.setLong(5, account.getAccountId());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating account failed, no rows affected.");
            }
        }
    }

    public void updateAccountPassword(Connection connection, long accountId, String newHash) throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_PASSWORD)) {
            statement.setString(1, newHash);
            statement.setObject(2, now);
            statement.setLong(3, accountId);
            statement.executeUpdate();
        }
    }

    public void logicalDeleteAccount(Connection connection, long accountId) throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        try (PreparedStatement statement = connection.prepareStatement(SQL_LOGICAL_DELETE)) {
            statement.setObject(1, now);
            statement.setObject(2, now);
            statement.setLong(3, accountId);

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Logical delete account failed, no rows affected.");
            }
        }
    }

    private String normalizeRoleForDb(AccountRole role) {
        if (role == null) {
            return "employee";
        }
        String n = role.name();
        if ("ADMIN".equals(n) || "ADMINISTRATOR".equals(n) || "ADMINISTRADOR".equals(n)) {
            return "administrator";
        }
        return "employee";
    }

    private AccountDTO map(ResultSet rs) throws SQLException {
        AccountDTO a = new AccountDTO();
        a.setAccountId(rs.getLong("account_id"));
        a.setEmail(rs.getString("email"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setRole(AccountRole.valueOf(rs.getString("role").toUpperCase()));
        a.setIsActive(rs.getBoolean("is_active"));
        a.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        a.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        a.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));
        return a;
    }
}
