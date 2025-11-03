package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.AccountDTO;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {

    private static final String SQL_INSERT = "INSERT INTO account (email, password_hash, role, is_active, created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE account SET email = ?, password_hash = ?, role = ?, is_active = ?, created_at = ?, updated_at = ?, deleted_at = ? WHERE account_id = ?";
    private static final String SQL_DELETE = "DELETE FROM account WHERE account_id = ?";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM account WHERE account_id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM account";

    public boolean insertAccount(AccountDTO account) throws SQLException, IOException {
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, account.getEmail());
            statement.setString(2, account.getPasswordHash());
            statement.setString(3, account.getRole().name().toLowerCase());
            statement.setBoolean(4, account.getIsActive());
            statement.setObject(5, account.getCreatedAt());
            statement.setObject(6, account.getUpdatedAt());
            statement.setObject(7, account.getDeletedAt());
            boolean result = statement.executeUpdate() > 0;
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                account.setAccountId(keys.getLong(1));
            }
            return result;
        }
    }

    public boolean updateAccount(AccountDTO account) throws SQLException, IOException {
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_UPDATE);
            statement.setString(1, account.getEmail());
            statement.setString(2, account.getPasswordHash());
            statement.setString(3, account.getRole().name().toLowerCase());
            statement.setBoolean(4, account.getIsActive());
            statement.setObject(5, account.getCreatedAt());
            statement.setObject(6, account.getUpdatedAt());
            statement.setObject(7, account.getDeletedAt());
            statement.setLong(8, account.getAccountId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteAccount(Long accountId) throws SQLException, IOException {
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_DELETE);
            statement.setLong(1, accountId);
            return statement.executeUpdate() > 0;
        }
    }

    public AccountDTO findAccountById(Long accountId) throws SQLException, IOException {
        AccountDTO account = null;
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_ID);
            statement.setLong(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    account = mapResultSetToAccountDTO(rs);
                }
            }
        }
        return account;
    }

    public List<AccountDTO> getAllAccounts() throws SQLException, IOException {
        List<AccountDTO> accounts = new ArrayList<>();
        try (ConnectionDataBase connectionDataBase = new ConnectionDataBase()) {
            Connection connection = connectionDataBase.connectDB();
            PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                accounts.add(mapResultSetToAccountDTO(rs));
            }
        }
        return accounts;
    }

    private AccountDTO mapResultSetToAccountDTO(ResultSet rs) throws SQLException {
        AccountDTO account = new AccountDTO();
        account.setAccountId(rs.getLong("account_id"));
        account.setEmail(rs.getString("email"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setRole(AccountDTO.Role.valueOf(rs.getString("role").toUpperCase()));
        account.setIsActive(rs.getBoolean("is_active"));
        account.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        account.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        account.setDeletedAt(rs.getObject("deleted_at", LocalDateTime.class));
        return account;
    }
}