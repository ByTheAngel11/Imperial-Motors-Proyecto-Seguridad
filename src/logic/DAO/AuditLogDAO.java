package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.AuditAction;
import logic.DTO.AccountDTO;
import logic.DTO.CustomerDTO;
import logic.DTO.UserDTO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AuditLogDAO {

    private static final String SQL_INSERT =
            "INSERT INTO audit_log " +
                    "(account_id, action, entity, entity_id, before_data, after_data, ip_address) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String ENTITY_AUTH = "AUTH_LOGIN";
    private static final String ENTITY_CUSTOMER = "customer";
    private static final String ENTITY_ACCOUNT = "account";
    private static final String ENTITY_USER = "user";

    private static final String DEFAULT_IP = "LOCALHOST";
    private static final long ENTITY_ID_NONE = 0L;

    public void logLoginSuccess(Long accountId, String email) throws SQLException, IOException {
        String afterJson = "{\"email\":\"" + escapeJson(email) + "\",\"success\":true}";
        insertAudit(
                accountId,
                AuditAction.LOGIN,
                ENTITY_AUTH,
                accountId != null ? accountId : ENTITY_ID_NONE,
                null,
                afterJson
        );
    }

    public void logLoginFailure(String email) throws SQLException, IOException {
        String afterJson = "{\"email\":\"" + escapeJson(email) + "\",\"success\":false}";
        insertAudit(
                null,
                AuditAction.LOGIN,
                ENTITY_AUTH,
                ENTITY_ID_NONE,
                null,
                afterJson
        );
    }

    public void logLogout(Long accountId, String email) throws SQLException, IOException {
        String afterJson = "{\"email\":\"" + escapeJson(email) + "\"}";
        insertAudit(
                accountId,
                AuditAction.LOGOUT,
                ENTITY_AUTH,
                accountId != null ? accountId : ENTITY_ID_NONE,
                null,
                afterJson
        );
    }

    public void logCustomerCreate(Long actorId, CustomerDTO customer) throws SQLException, IOException {
        String afterJson = buildCustomerJson(customer);

        insertAudit(
                actorId,
                AuditAction.CREATE,
                ENTITY_CUSTOMER,
                ENTITY_ID_NONE,
                null,
                afterJson
        );
    }

    public void logCustomerUpdate(Long actorId, CustomerDTO customer) throws SQLException, IOException {
        String afterJson = buildCustomerJson(customer);

        insertAudit(
                actorId,
                AuditAction.UPDATE,
                ENTITY_CUSTOMER,
                ENTITY_ID_NONE,
                null,
                afterJson
        );
    }

    // ===== NUEVO: Auditoría para users/account =====

    public void logAccountCreate(Connection connection, Long actorId, AccountDTO account) throws SQLException {
        insertAudit(
                connection,
                actorId,
                AuditAction.CREATE,
                ENTITY_ACCOUNT,
                account.getAccountId() != null ? account.getAccountId() : ENTITY_ID_NONE,
                null,
                buildAccountJson(account)
        );
    }

    public void logAccountUpdate(Connection connection, Long actorId, long accountId, String beforeJson, String afterJson)
            throws SQLException {

        insertAudit(connection, actorId, AuditAction.UPDATE, ENTITY_ACCOUNT, accountId, beforeJson, afterJson);
    }

    public void logAccountDelete(Connection connection, Long actorId, long accountId, String beforeJson, String afterJson)
            throws SQLException {

        insertAudit(connection, actorId, AuditAction.DELETE, ENTITY_ACCOUNT, accountId, beforeJson, afterJson);
    }

    public void logUserCreate(Connection connection, Long actorId, UserDTO user) throws SQLException {
        insertAudit(
                connection,
                actorId,
                AuditAction.CREATE,
                ENTITY_USER,
                user.getAccountId() != null ? user.getAccountId() : ENTITY_ID_NONE,
                null,
                buildUserJson(user)
        );
    }

    public void logUserUpdate(Connection connection, Long actorId, long entityId, String beforeJson, String afterJson)
            throws SQLException {

        insertAudit(connection, actorId, AuditAction.UPDATE, ENTITY_USER, entityId, beforeJson, afterJson);
    }

    public void logUserDelete(Connection connection, Long actorId, long entityId, String beforeJson, String afterJson)
            throws SQLException {

        insertAudit(connection, actorId, AuditAction.DELETE, ENTITY_USER, entityId, beforeJson, afterJson);
    }

    // ===== INSERT (2 versiones): una con Connection (para transacciones) y otra standalone =====

    public void insertAudit(
            Long accountId,
            AuditAction action,
            String entity,
            long entityId,
            String beforeDataJson,
            String afterDataJson) throws SQLException, IOException {

        try (Connection connection = ConnectionDataBase.getConnection()) {
            insertAudit(connection, accountId, action, entity, entityId, beforeDataJson, afterDataJson);
        }
    }

    public void insertAudit(
            Connection connection,
            Long accountId,
            AuditAction action,
            String entity,
            long entityId,
            String beforeDataJson,
            String afterDataJson) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {

            if (accountId != null) {
                statement.setLong(1, accountId);
            } else {
                statement.setNull(1, Types.BIGINT);
            }

            statement.setString(2, action.name());
            statement.setString(3, entity);
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

            statement.setString(7, DEFAULT_IP);
            statement.executeUpdate();
        }
    }

    private String buildCustomerJson(CustomerDTO c) {
        String number = c.getCostumerNumber() != null ? c.getCostumerNumber() : "";
        String fullName = c.getFullName() != null ? c.getFullName() : "";
        String email = c.getEmail() != null ? c.getEmail() : "";
        String phone = c.getPhone() != null ? c.getPhone() : "";
        boolean active = c.getIsActive();

        return "{"
                + "\"customerNumber\":\"" + escapeJson(number) + "\","
                + "\"fullName\":\"" + escapeJson(fullName) + "\","
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"phone\":\"" + escapeJson(phone) + "\","
                + "\"isActive\":" + active
                + "}";
    }

    private String buildAccountJson(AccountDTO a) {
        String email = a.getEmail() != null ? a.getEmail() : "";
        String role = a.getRole() != null ? a.getRole().name() : "";
        boolean active = a.getIsActive();

        return "{"
                + "\"accountId\":" + (a.getAccountId() == null ? 0 : a.getAccountId()) + ","
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"role\":\"" + escapeJson(role) + "\","
                + "\"isActive\":" + active
                + "}";
    }

    private String buildUserJson(UserDTO u) {
        String pn = u.getPersonnelNumber() != null ? u.getPersonnelNumber() : "";
        String username = u.getUsername() != null ? u.getUsername() : "";
        String fullName = u.getFullName() != null ? u.getFullName() : "";
        String phone = u.getPhone() != null ? u.getPhone() : "";

        return "{"
                + "\"personnelNumber\":\"" + escapeJson(pn) + "\","
                + "\"username\":\"" + escapeJson(username) + "\","
                + "\"fullName\":\"" + escapeJson(fullName) + "\","
                + "\"phone\":\"" + escapeJson(phone) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
