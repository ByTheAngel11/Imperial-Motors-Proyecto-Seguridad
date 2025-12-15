package userinterface.users;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UserTableRow {

    private final StringProperty accountId = new SimpleStringProperty();
    private final StringProperty personnelNumber = new SimpleStringProperty();

    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final StringProperty activo = new SimpleStringProperty();
    private final StringProperty createdAt = new SimpleStringProperty();

    public UserTableRow(
            String accountId,
            String personnelNumber,
            String username,
            String fullName,
            String email,
            String role,
            String activo,
            String createdAt) {

        this.accountId.set(safe(accountId));
        this.personnelNumber.set(safe(personnelNumber));
        this.username.set(safe(username));
        this.fullName.set(safe(fullName));
        this.email.set(safe(email));
        this.role.set(safe(role));
        this.activo.set(safe(activo));
        this.createdAt.set(safe(createdAt));
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    public String getAccountId() { return accountId.get(); }
    public String getPersonnelNumber() { return personnelNumber.get(); }
    public String getUsername() { return username.get(); }
    public String getFullName() { return fullName.get(); }
    public String getEmail() { return email.get(); }
    public String getRole() { return role.get(); }
    public String getActivo() { return activo.get(); }
    public String getCreatedAt() { return createdAt.get(); }

    public StringProperty accountIdProperty() { return accountId; }
    public StringProperty personnelNumberProperty() { return personnelNumber; }
    public StringProperty usernameProperty() { return username; }
    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty emailProperty() { return email; }
    public StringProperty roleProperty() { return role; }
    public StringProperty activoProperty() { return activo; }
    public StringProperty createdAtProperty() { return createdAt; }
}
