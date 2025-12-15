package logic.DTO;

import java.time.LocalDateTime;

public class UserAccountDTO {
    private String personnelNumber;
    private Long accountId;

    private String username;
    private String fullName;
    private String phone;

    private String email;
    private AccountRole role;
    private boolean isActive;

    private LocalDateTime createdAt;

    public String getPersonnelNumber() { return personnelNumber; }
    public void setPersonnelNumber(String personnelNumber) { this.personnelNumber = personnelNumber; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public AccountRole getRole() { return role; }
    public void setRole(AccountRole role) { this.role = role; }

    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
