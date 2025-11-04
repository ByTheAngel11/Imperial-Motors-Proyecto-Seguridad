package userinterface.users;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UserTableRow {
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final StringProperty activo = new SimpleStringProperty();
    private final StringProperty createdAt = new SimpleStringProperty();

    public UserTableRow(String username, String fullName, String email,
                        String role, String activo, String createdAt) {
        this.username.set(username);
        this.fullName.set(fullName);
        this.email.set(email);
        this.role.set(role);
        this.activo.set(activo);
        this.createdAt.set(createdAt);
    }

    public String getUsername() { return username.get(); }
    public String getFullName() { return fullName.get(); }
    public String getEmail() { return email.get(); }
    public String getRole() { return role.get(); }
    public String getActivo() { return activo.get(); }
    public String getCreatedAt() { return createdAt.get(); }

    public StringProperty usernameProperty() { return username; }
    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty emailProperty() { return email; }
    public StringProperty roleProperty() { return role; }
    public StringProperty activoProperty() { return activo; }
    public StringProperty createdAtProperty() { return createdAt; }
}
