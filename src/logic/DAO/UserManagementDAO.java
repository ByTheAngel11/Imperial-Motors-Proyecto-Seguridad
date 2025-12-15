package logic.DAO;

import dataaccess.ConnectionDataBase;
import logic.DTO.AccountDTO;
import logic.DTO.AccountRole;
import logic.DTO.UserDTO;
import utilities.PasswordUtiities;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserManagementDAO {

    private static final boolean DEFAULT_IS_ACTIVE = true;

    private static final int PERSONNEL_NUMBER_LENGTH = 10;
    private static final int PERSONNEL_NUMBER_DIGITS = 9;

    private static final int USERNAME_MAX_LENGTH = 10;
    private static final int FULL_NAME_MAX_LENGTH = 225;
    private static final int EMAIL_MAX_LENGTH = 120;

    private static final int PHONE_LENGTH = 10;
    private static final int PHONE_PREFIX_LENGTH = 3;
    private static final int PHONE_REQUIRED_SUFFIX = 7;
    private static final String PHONE_PREFIX_FIXED = "228";

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 30;

    private static final String SQL_EXISTS_EMAIL =
            "SELECT COUNT(1) FROM account WHERE email = ? AND (? IS NULL OR account_id <> ?)";

    private static final String SQL_EXISTS_USERNAME =
            "SELECT COUNT(1) FROM `user` WHERE username = ? AND (? IS NULL OR account_id <> ?)";

    private static final String SQL_MAX_PERSONNEL_FOR_PREFIX =
            "SELECT MAX(personnel_number) FROM `user` WHERE personnel_number LIKE ?";

    private final AccountDAO accountDao = new AccountDAO();
    private final UserDAO userDao = new UserDAO();
    private final AuditLogDAO auditDao = new AuditLogDAO();

    public void createUserWithAccount(long actorAccountId, UserDTO user, AccountDTO account, String plainPassword)
            throws SQLException, IOException {

        validateAdminActor(actorAccountId);
        validateCreate(user, account, plainPassword);

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (existsEmail(connection, account.getEmail(), null)) {
                    throw new IllegalArgumentException("El correo ya está registrado.");
                }
                if (existsUsername(connection, user.getUsername(), null)) {
                    throw new IllegalArgumentException("El usuario ya está registrado.");
                }

                String personnelNumber = generateNextPersonnelNumber(connection, account.getRole());
                user.setPersonnelNumber(personnelNumber);

                user.setPhone(normalizePhoneWithFixedPrefix(user.getPhone()));

                String hash = PasswordUtiities.hashPassword(plainPassword);
                account.setPasswordHash(hash);
                account.setIsActive(DEFAULT_IS_ACTIVE);

                long accountId = accountDao.insertAccount(connection, account);

                user.setAccountId(accountId);
                userDao.insertUser(connection, user);

                auditDao.logAccountCreate(connection, actorAccountId, account);
                auditDao.logUserCreate(connection, actorAccountId, user);

                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                if (ex instanceof SQLException) throw (SQLException) ex;
                if (ex instanceof IOException) throw (IOException) ex;
                if (ex instanceof IllegalArgumentException) throw (IllegalArgumentException) ex;
                throw new SQLException("Error inesperado al crear usuario.", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void updateUserWithAccount(long actorAccountId, UserDTO user, AccountDTO account, String newPlainPassword)
            throws SQLException, IOException {

        validateAdminActor(actorAccountId);
        validateUpdate(user, account);

        // ✅ password opcional en editar
        boolean shouldUpdatePassword = newPlainPassword != null && !newPlainPassword.trim().isEmpty();
        if (shouldUpdatePassword) {
            validatePasswordStrong(newPlainPassword);
        }

        if (account.getAccountId() == null || account.getAccountId() <= 0) {
            throw new IllegalArgumentException("accountId inválido.");
        }

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (existsEmail(connection, account.getEmail(), account.getAccountId())) {
                    throw new IllegalArgumentException("El correo ya está registrado.");
                }
                if (existsUsername(connection, user.getUsername(), account.getAccountId())) {
                    throw new IllegalArgumentException("El usuario ya está registrado.");
                }

                AccountDTO currentAccount = accountDao.findAccountById(account.getAccountId());
                UserDTO currentUser = userDao.findUserByPersonnelNumber(user.getPersonnelNumber());

                if (currentAccount == null || currentUser == null) {
                    throw new SQLException("User/Account not found.");
                }

                String beforeAccountJson = buildAccountJson(currentAccount);
                String beforeUserJson = buildUserJson(currentUser);

                user.setPersonnelNumber(currentUser.getPersonnelNumber());
                user.setPhone(normalizePhoneWithFixedPrefix(user.getPhone()));

                accountDao.updateAccountCore(connection, account);
                userDao.updateUser(connection, user);

                // ✅ Solo si escribió contraseña nueva
                if (shouldUpdatePassword) {
                    String newHash = PasswordUtiities.hashPassword(newPlainPassword);
                    accountDao.updateAccountPassword(connection, account.getAccountId(), newHash);
                }

                AccountDTO afterAccount = accountDao.findAccountById(account.getAccountId());
                UserDTO afterUser = userDao.findUserByPersonnelNumber(user.getPersonnelNumber());

                auditDao.logAccountUpdate(connection, actorAccountId, account.getAccountId(),
                        beforeAccountJson, buildAccountJson(afterAccount));

                auditDao.logUserUpdate(connection, actorAccountId, account.getAccountId(),
                        beforeUserJson, buildUserJson(afterUser));

                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                if (ex instanceof SQLException) throw (SQLException) ex;
                if (ex instanceof IOException) throw (IOException) ex;
                if (ex instanceof IllegalArgumentException) throw (IllegalArgumentException) ex;
                throw new SQLException("Error inesperado al actualizar usuario.", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void logicalDeleteUserByPersonnelNumber(long actorAccountId, String personnelNumber)
            throws SQLException, IOException {

        validateAdminActor(actorAccountId);

        if (personnelNumber == null || personnelNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("No. de personal requerido.");
        }

        try (Connection connection = ConnectionDataBase.getConnection()) {
            connection.setAutoCommit(false);

            try {
                UserDTO currentUser = userDao.findUserByPersonnelNumber(personnelNumber);
                if (currentUser == null || currentUser.getAccountId() == null) {
                    throw new SQLException("User not found.");
                }

                AccountDTO currentAccount = accountDao.findAccountById(currentUser.getAccountId());
                if (currentAccount == null) {
                    throw new SQLException("Account not found.");
                }

                String beforeAccountJson = buildAccountJson(currentAccount);
                String beforeUserJson = buildUserJson(currentUser);

                accountDao.logicalDeleteAccount(connection, currentAccount.getAccountId());
                userDao.logicalDeleteUser(connection, personnelNumber);

                AccountDTO afterAccount = accountDao.findAccountById(currentAccount.getAccountId());
                UserDTO afterUser = userDao.findUserByPersonnelNumber(personnelNumber);

                auditDao.logAccountDelete(connection, actorAccountId, currentAccount.getAccountId(),
                        beforeAccountJson, buildAccountJson(afterAccount));

                auditDao.logUserDelete(connection, actorAccountId, currentAccount.getAccountId(),
                        beforeUserJson, buildUserJson(afterUser));

                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                if (ex instanceof SQLException) throw (SQLException) ex;
                if (ex instanceof IOException) throw (IOException) ex;
                if (ex instanceof IllegalArgumentException) throw (IllegalArgumentException) ex;
                throw new SQLException("Error inesperado al eliminar usuario.", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void validateAdminActor(long actorAccountId) {
        if (!utilities.SessionManager.isLoggedIn()) {
            throw new IllegalStateException("Debes iniciar sesión.");
        }
        if (!utilities.SessionManager.isAdmin()) {
            throw new IllegalStateException("Solo el administrador puede administrar usuarios.");
        }
        if (actorAccountId <= 0) {
            throw new IllegalArgumentException("actorAccountId inválido.");
        }
    }

    private void validateCreate(UserDTO user, AccountDTO account, String plainPassword) {
        validateUpdate(user, account);
        validatePasswordStrong(plainPassword); // ✅ obligatoria solo al crear
    }

    private void validateUpdate(UserDTO user, AccountDTO account) {
        if (user == null) throw new IllegalArgumentException("User requerido.");
        if (account == null) throw new IllegalArgumentException("Account requerido.");

        requireNotBlank(user.getUsername(), "Usuario");
        requireNotBlank(user.getFullName(), "Nombre completo");
        requireNotBlank(account.getEmail(), "Correo");

        if (user.getUsername().trim().length() > USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("El usuario no puede exceder " + USERNAME_MAX_LENGTH + " caracteres.");
        }
        if (user.getFullName().trim().length() > FULL_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("El nombre completo no puede exceder " + FULL_NAME_MAX_LENGTH + " caracteres.");
        }
        if (account.getEmail().trim().length() > EMAIL_MAX_LENGTH) {
            throw new IllegalArgumentException("El correo no puede exceder " + EMAIL_MAX_LENGTH + " caracteres.");
        }

        if (!isValidEmail(account.getEmail().trim())) {
            throw new IllegalArgumentException("Formato de correo inválido.");
        }

        if (account.getRole() == null) {
            throw new IllegalArgumentException("Rol requerido.");
        }

        String normalized = normalizePhoneWithFixedPrefix(user.getPhone());

        // ✅ exacto 10 (228 + 7)
        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException("El teléfono solo puede contener números.");
        }
        if (!normalized.startsWith(PHONE_PREFIX_FIXED)) {
            throw new IllegalArgumentException("El teléfono debe iniciar con " + PHONE_PREFIX_FIXED + ".");
        }
        if (normalized.length() != PHONE_LENGTH) {
            throw new IllegalArgumentException("El teléfono debe tener " + PHONE_LENGTH + " dígitos (228 + " + PHONE_REQUIRED_SUFFIX + ").");
        }
        if (normalized.equals(PHONE_PREFIX_FIXED)) {
            throw new IllegalArgumentException("Debes completar los " + PHONE_REQUIRED_SUFFIX + " dígitos después de 228.");
        }
    }

    private void validatePasswordStrong(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        if (plainPassword.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres.");
        }
        if (plainPassword.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("La contraseña no puede exceder " + PASSWORD_MAX_LENGTH + " caracteres.");
        }

        boolean hasLower = plainPassword.matches(".*[a-z].*");
        boolean hasUpper = plainPassword.matches(".*[A-Z].*");
        boolean hasDigit = plainPassword.matches(".*\\d.*");
        boolean hasSpecial = plainPassword.matches(".*[^A-Za-z0-9].*");

        if (!hasLower || !hasUpper || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException("La contraseña debe incluir 1 minúscula, 1 mayúscula, 1 número y 1 caracter especial.");
        }
    }

    private void requireNotBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo \"" + field + "\" es obligatorio.");
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean existsEmail(Connection connection, String email, Long excludeAccountId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SQL_EXISTS_EMAIL)) {
            ps.setString(1, email.trim());

            if (excludeAccountId == null) {
                ps.setObject(2, null);
                ps.setObject(3, null);
            } else {
                ps.setLong(2, excludeAccountId);
                ps.setLong(3, excludeAccountId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean existsUsername(Connection connection, String username, Long excludeAccountId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SQL_EXISTS_USERNAME)) {
            ps.setString(1, username.trim());

            if (excludeAccountId == null) {
                ps.setObject(2, null);
                ps.setObject(3, null);
            } else {
                ps.setLong(2, excludeAccountId);
                ps.setLong(3, excludeAccountId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private String generateNextPersonnelNumber(Connection connection, AccountRole role) throws SQLException {
        String prefix = resolvePrefix(role);
        String like = prefix + "%";

        String max = null;
        try (PreparedStatement ps = connection.prepareStatement(SQL_MAX_PERSONNEL_FOR_PREFIX)) {
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    max = rs.getString(1);
                }
            }
        }

        int nextNumber = 0;
        if (max != null && max.length() == PERSONNEL_NUMBER_LENGTH && max.startsWith(prefix)) {
            String digits = max.substring(1);
            if (digits.matches("\\d{" + PERSONNEL_NUMBER_DIGITS + "}")) {
                nextNumber = Integer.parseInt(digits) + 1;
            }
        }

        String padded = String.format("%0" + PERSONNEL_NUMBER_DIGITS + "d", nextNumber);
        return prefix + padded;
    }

    private String resolvePrefix(AccountRole role) {
        return isAdminRole(role) ? "A" : "V";
    }

    private boolean isAdminRole(AccountRole role) {
        if (role == null) return false;
        String name = role.name();
        return "ADMIN".equalsIgnoreCase(name)
                || "ADMINISTRATOR".equalsIgnoreCase(name)
                || "ADMINISTRADOR".equalsIgnoreCase(name);
    }

    private String normalizePhoneWithFixedPrefix(String phoneRaw) {
        String raw = phoneRaw == null ? "" : phoneRaw.trim();
        raw = raw.replaceAll("\\D", "");

        if (raw.isEmpty()) {
            return PHONE_PREFIX_FIXED;
        }
        if (raw.startsWith(PHONE_PREFIX_FIXED)) {
            return raw;
        }
        return PHONE_PREFIX_FIXED + raw;
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
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
