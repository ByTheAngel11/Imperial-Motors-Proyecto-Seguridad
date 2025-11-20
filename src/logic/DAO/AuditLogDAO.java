package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.AuditAction;
import logic.DTO.CustomerDTO;

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

    private static final String DEFAULT_IP = "LOCALHOST";
    private static final long ENTITY_ID_NONE = 0L;

    // ================== LOGIN / LOGOUT ==================

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
        // Si quisieras before_data real habría que leer el registro antes de actualizar.
        // Por ahora registramos solo el estado final.
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

    private String buildCustomerJson(CustomerDTO c) {
        String number = c.getCostumerNumber() != null ? c.getCostumerNumber() : "";
        String fullName = c.getFullName() != null ? c.getFullName() : "";
        String email = c.getEmail() != null ? c.getEmail() : "";
        String phone = c.getPhone() != null ? c.getPhone() : "";
        boolean active = c.getIsActive();   // <-- sin comparar con null

        return "{"
                + "\"customerNumber\":\"" + escapeJson(number) + "\","
                + "\"fullName\":\"" + escapeJson(fullName) + "\","
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"phone\":\"" + escapeJson(phone) + "\","
                + "\"isActive\":" + active
                + "}";
    }

    // ================== GENÉRICO ==================

    public void insertAudit(
            Long accountId,
            AuditAction action,
            String entity,
            long entityId,
            String beforeDataJson,
            String afterDataJson) throws SQLException, IOException {

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {

            if (accountId != null) {
                statement.setLong(1, accountId);
            } else {
                statement.setNull(1, Types.BIGINT);
            }

            // El valor DEBE existir en el ENUM de la columna action
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

    // ================== HELPERS ==================

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
