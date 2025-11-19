package userinterface.costumers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Region;
import logic.DAO.AuditLogDAO;
import logic.DAO.CustomerDAO;
import logic.DTO.CustomerDTO;
import utilities.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class CustomerFormController {

    private static final int CUSTOMER_NUMBER_MAX_LENGTH = 10;
    private static final int FULL_NAME_MAX_LENGTH = 225;
    private static final int EMAIL_MAX_LENGTH = 120;
    private static final int PHONE_MIN_LENGTH = 7;
    private static final int PHONE_MAX_LENGTH = 20;

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @FXML private Label LblTitle;
    @FXML private TextField TxtCustomerNumber;
    @FXML private TextField TxtFullName;
    @FXML private TextField TxtEmail;
    @FXML private TextField TxtPhone;
    @FXML private CheckBox ChkActive;

    private final CustomerDAO customerDao = new CustomerDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private CustomerDTO editingCustomer;

    private Runnable onSaveCallback;
    private Runnable onCloseCallback;

    @FXML
    private void initialize() {
        TxtCustomerNumber.setEditable(false);
        ChkActive.setSelected(true);
        setupTextFormatters();
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    private void setupTextFormatters() {

        TxtCustomerNumber.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() > CUSTOMER_NUMBER_MAX_LENGTH) return null;
            if (!newText.matches("\\d*")) return null;
            return change;
        }));

        TxtFullName.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() > FULL_NAME_MAX_LENGTH) return null;
            if (!newText.matches("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]*")) return null;
            return change;
        }));

        TxtEmail.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().length() > EMAIL_MAX_LENGTH) return null;
            return change;
        }));

        TxtPhone.setTextFormatter(new TextFormatter<>(change -> {
            String t = change.getControlNewText();
            if (t.length() > PHONE_MAX_LENGTH) return null;
            if (!t.matches("\\d*")) return null;
            return change;
        }));
    }

    public void setCustomer(CustomerDTO customer) {
        this.editingCustomer = customer;

        if (customer == null) {
            LblTitle.setText("Registrar cliente");
            TxtCustomerNumber.clear();
            TxtCustomerNumber.setPromptText("Se generará automáticamente");
            ChkActive.setSelected(true);
            ChkActive.setDisable(true);
        } else {
            LblTitle.setText("Editar cliente");
            TxtCustomerNumber.setText(customer.getCostumerNumber());
            TxtFullName.setText(customer.getFullName());
            TxtEmail.setText(customer.getEmail());
            TxtPhone.setText(customer.getPhone());
            ChkActive.setSelected(customer.getIsActive());
            ChkActive.setDisable(false);
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        if (!validate()) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar cambios");
        confirm.setHeaderText("¿Deseas guardar los cambios?");
        confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            boolean success = editingCustomer == null
                    ? insertCustomer()
                    : updateCustomer();

            if (success) {
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
                closeWindow();
            } else {
                showError("No se pudieron guardar los cambios.");
            }

        } catch (SQLException | IOException ex) {
            showError("Error al guardar: " + ex.getMessage());
        }
    }

    private boolean insertCustomer() throws SQLException, IOException {
        CustomerDTO dto = new CustomerDTO();
        LocalDateTime now = LocalDateTime.now();

        dto.setFullName(TxtFullName.getText().trim());
        dto.setEmail(TxtEmail.getText().trim());
        dto.setPhone(TxtPhone.getText().trim());
        dto.setIsActive(true);
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);

        boolean inserted = customerDao.registerCustomer(dto);

        if (inserted) {
            Long actorId = SessionManager.getCurrentAccountId();
            auditLogDAO.logCustomerCreate(actorId, dto);
        }

        return inserted;
    }

    private boolean updateCustomer() throws SQLException, IOException {
        editingCustomer.setFullName(TxtFullName.getText().trim());
        editingCustomer.setEmail(TxtEmail.getText().trim());
        editingCustomer.setPhone(TxtPhone.getText().trim());
        editingCustomer.setIsActive(ChkActive.isSelected());
        editingCustomer.setUpdatedAt(LocalDateTime.now());

        boolean updated = customerDao.updateCustomer(editingCustomer);

        if (updated) {
            Long actorId = SessionManager.getCurrentAccountId();
            auditLogDAO.logCustomerUpdate(actorId, editingCustomer);
        }

        return updated;
    }

    @FXML
    private void onCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private boolean validate() {
        String fullName = TxtFullName.getText().trim();
        String email = TxtEmail.getText().trim();
        String phone = TxtPhone.getText().trim();

        if (fullName.isEmpty()) return error("El nombre completo es obligatorio.");
        if (!NAME_PATTERN.matcher(fullName).matches()) return error("El nombre solo puede contener letras y espacios.");
        if (email.isEmpty()) return error("El correo electrónico es obligatorio.");
        if (!EMAIL_PATTERN.matcher(email).matches()) return error("Formato de email inválido.");
        if (phone.isEmpty()) return error("El teléfono es obligatorio.");
        if (phone.length() < PHONE_MIN_LENGTH) return error("El teléfono debe tener al menos " + PHONE_MIN_LENGTH + " dígitos.");
        if (!phone.matches("\\d+")) return error("El teléfono solo puede contener números.");

        return true;
    }

    private boolean error(String msg) {
        showError(msg);
        return false;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
