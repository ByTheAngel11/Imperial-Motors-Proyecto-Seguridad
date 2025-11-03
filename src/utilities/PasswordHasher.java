package utilities;

import org.mindrot.jbcrypt.BCrypt;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class PasswordHasher {
    private static final int COST = 12;
    private static final int MAX_LEN = 72;
    private static final int MIN_LEN = 8;

    private PasswordHasher() { }

    public static String hashPassword(String plainTextPassword) {
        String pwd = sanitize(plainTextPassword);
        validateLength(pwd);
        return BCrypt.hashpw(pwd, BCrypt.gensalt(COST));
    }

    public static boolean verifyPassword(String plainTextPassword, String passwordHash) {
        if (isBlank(passwordHash)) return false;
        String pwd = sanitize(plainTextPassword);
        try {
            return BCrypt.checkpw(pwd, passwordHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean needsRehash(String passwordHash) {
        if (isBlank(passwordHash)) return true;
        String[] parts = passwordHash.split("\\$");
        if (parts.length < 3) return true;
        try {
            int currentCost = Integer.parseInt(parts[2]);
            return currentCost < COST;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private static String sanitize(String s) {
        Objects.requireNonNull(s, "La contraseña no puede ser null");
        String trimmed = s;
        if (trimmed.getBytes(StandardCharsets.UTF_8).length <= MAX_LEN) {
            return trimmed;
        }
        int end = trimmed.length();
        while (end > 0) {
            String sub = trimmed.substring(0, end);
            if (sub.getBytes(StandardCharsets.UTF_8).length <= MAX_LEN) {
                return sub;
            }
            end--;
        }
        throw new IllegalArgumentException("La contraseña excede el máximo de " + MAX_LEN + " bytes en UTF-8.");
    }

    private static void validateLength(String s) {
        if (s.codePointCount(0, s.length()) < MIN_LEN) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_LEN + " caracteres.");
        }
        if (s.getBytes(StandardCharsets.UTF_8).length > MAX_LEN) {
            throw new IllegalArgumentException("La contraseña no debe exceder " + MAX_LEN + " bytes (BCrypt).");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}