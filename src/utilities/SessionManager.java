package utilities;

import logic.DTO.AccountRole;

public final class SessionManager {


    private static Long currentAccountId;
    private static AccountRole currentRole;
    private static Boolean currentIsActive;

    private SessionManager() {
    }

    public static Long getCurrentAccountId() {
        return currentAccountId;
    }

    public static void setCurrentAccountId(Long accountId) {
        currentAccountId = accountId;
    }

    public static AccountRole getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(AccountRole role) {
        currentRole = role;
    }

    public static Boolean getCurrentIsActive() {
        return currentIsActive;
    }

    public static void setCurrentIsActive(Boolean isActive) {
        currentIsActive = isActive;
    }

    public static boolean isLoggedIn() {
        return currentAccountId != null && currentAccountId > 0;
    }

    public static boolean isAdmin() {
        return currentRole != null && currentRole == AccountRole.ADMINISTRATOR;
    }
}
