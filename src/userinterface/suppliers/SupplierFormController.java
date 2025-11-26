package userinterface.suppliers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import logic.DAO.SupplierDAO;
import logic.DTO.SupplierDTO;

import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

public class SupplierFormController {

    @FXML private Label LblTitle;
    @FXML private TextField TxtSupplierId;
    @FXML private TextField TxtLegalName;
    @FXML private TextField TxtRfc;
    @FXML private TextField TxtContactName;
    @FXML private TextField TxtPhone;
    @FXML private TextField TxtEmail;
    @FXML private TextField TxtStatus;

    @FXML private Button BtnSave;
    @FXML private Button BtnCancel;

    private final SupplierDAO supplierDao = new SupplierDAO();

    private Runnable onSaveCallback;
    private Runnable onCloseCallback;

    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{1,10}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern RFC_PATTERN =
            Pattern.compile("^([A-ZÑ&]{3,4})\\d{6}[A-Z0-9]{3}$");

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void initialize() {
        LblTitle.setText("Registrar proveedor");
        TxtSupplierId.setEditable(false);
        TxtSupplierId.setPromptText("Autogenerado");
        TxtStatus.setEditable(false);
        TxtStatus.setText("Activo");
    }

    @FXML
    private void onSave() {
        try {
            String legalName = requireNotBlank(TxtLegalName.getText(), "Razón social");
            String contactName = requireNotBlank(TxtContactName.getText(), "Nombre contacto");
            String phone = requireNotBlank(TxtPhone.getText(), "Teléfono");
            validatePhone(phone);

            String rfc = normalizeOptionalRfc(TxtRfc.getText());
            String email = emptyToNull(TxtEmail.getText());
            if (email != null) {
                validateEmail(email);
            }

            // Unicidad contra todos
            if (supplierDao.existsPhone(phone, null)) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese teléfono.");
            }
            if (email != null && supplierDao.existsEmail(email, null)) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese correo.");
            }
            if (rfc != null && supplierDao.existsRfc(rfc, null)) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese RFC.");
            }

            SupplierDTO supplier = new SupplierDTO();
            supplier.setLegalName(legalName);
            supplier.setRfc(rfc);
            supplier.setContactName(contactName);
            supplier.setPhone(phone);
            supplier.setEmail(email);
            supplier.setActive(true);

            supplierDao.createSupplier(supplier);

            showInfo("Proveedor registrado correctamente.");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }

        } catch (SQLException ex) {
            showError("Error al guardar en la base de datos: " + ex.getMessage());
        } catch (Exception ex) {
            showError("Error al guardar: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireNotBlank(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("El campo \"" + field + "\" es obligatorio.");
        }
        return v.trim();
    }

    private void validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException(
                    "El teléfono debe contener solo dígitos y máximo 10 números.");
        }
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("El correo electrónico no tiene un formato válido.");
        }
    }

    private String normalizeOptionalRfc(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!RFC_PATTERN.matcher(upper).matches()) {
            throw new IllegalArgumentException("El RFC no tiene un formato válido.");
        }
        return upper;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(msg);
        a.show();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg);
        a.show();
    }
}
