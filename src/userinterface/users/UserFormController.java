package userinterface.users;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Region;
import logic.DAO.UserManagementDAO;
import logic.DTO.*;
import utilities.SessionManager;

import java.util.regex.Pattern;

public class UserFormController {

    private static final int USERNAME_MAX_LENGTH = 10;
    private static final int FULL_NAME_MAX_LENGTH = 225;
    private static final int EMAIL_MAX_LENGTH = 120;

    private static final int PHONE_LENGTH = 10;
    private static final String PHONE_FIXED_PREFIX = "228";
    private static final int PHONE_PREFIX_LENGTH = 3;
    private static final int PHONE_REQUIRED_SUFFIX = 7;

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 30;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @FXML private Label LblTitle;

    @FXML private TextField TxtPersonnelNumber;
    @FXML private TextField TxtUsername;
    @FXML private TextField TxtFullName;
    @FXML private TextField TxtPhone;

    @FXML private TextField TxtEmail;
    @FXML private ComboBox<AccountRole> CmbRole;

    @FXML private PasswordField TxtPassword;

    @FXML private CheckBox ChkActive;

    @FXML private Button BtnSave;
    @FXML private Button BtnCancel;

    private final UserManagementDAO userManagementDao = new UserManagementDAO();

    private Runnable onSaveCallback;
    private Runnable onCloseCallback;

    private boolean isEditMode;
    private Long editingAccountId;
    private String editingPersonnelNumber;

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void initialize() {
        if (CmbRole != null) {
            CmbRole.getItems().setAll(AccountRole.values());
        }

        setupTextFormatters();
        setupPhonePrefixBehavior();

        if (TxtPersonnelNumber != null) {
            TxtPersonnelNumber.setEditable(false);
            TxtPersonnelNumber.setFocusTraversable(false);
            TxtPersonnelNumber.setPromptText("Se generará automáticamente");
            TxtPersonnelNumber.setText("");
        }

        if (ChkActive != null) {
            ChkActive.setSelected(true);
            ChkActive.setDisable(true);
        }

        if (TxtPassword != null) {
            TxtPassword.setPromptText("Obligatoria");
        }

        if (!SessionManager.isAdmin()) {
            disableAll();
        }
    }

    public void setCreateMode() {
        isEditMode = false;
        editingAccountId = null;
        editingPersonnelNumber = null;

        if (LblTitle != null) {
            LblTitle.setText("Crear usuario");
        }

        if (TxtPersonnelNumber != null) {
            TxtPersonnelNumber.setText("");
        }

        if (ChkActive != null) {
            ChkActive.setSelected(true);
            ChkActive.setDisable(true);
        }

        if (TxtPassword != null) {
            TxtPassword.setPromptText("Obligatoria");
            TxtPassword.clear();
        }
    }

    public void setEditMode(UserAccountDTO dto) {
        isEditMode = true;
        editingAccountId = dto.getAccountId();
        editingPersonnelNumber = dto.getPersonnelNumber();

        if (LblTitle != null) {
            LblTitle.setText("Editar usuario");
        }

        if (TxtPersonnelNumber != null) {
            TxtPersonnelNumber.setText(safe(dto.getPersonnelNumber()));
        }

        TxtUsername.setText(safe(dto.getUsername()));
        TxtFullName.setText(safe(dto.getFullName()));
        TxtPhone.setText(safePhone(dto.getPhone()));

        TxtEmail.setText(safe(dto.getEmail()));

        if (CmbRole != null) {
            CmbRole.getSelectionModel().select(dto.getRole());
        }

        if (ChkActive != null) {
            ChkActive.setSelected(dto.getIsActive());
            ChkActive.setDisable(false);
        }

        if (TxtPassword != null) {
            TxtPassword.setPromptText("Opcional (solo si deseas cambiarla)"); // ✅
            TxtPassword.clear();
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión.");
                return;
            }
            if (!SessionManager.isAdmin()) {
                showError("Solo el administrador puede guardar usuarios.");
                return;
            }

            if (!validateForm()) {
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar cambios");
            confirm.setHeaderText("¿Deseas guardar los cambios?");
            confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            UserDTO user = new UserDTO();
            user.setPersonnelNumber(isEditMode ? editingPersonnelNumber : null);
            user.setUsername(getTrim(TxtUsername));
            user.setFullName(getTrim(TxtFullName));
            user.setPhone(getTrim(TxtPhone));

            AccountDTO account = new AccountDTO();
            account.setEmail(getTrim(TxtEmail));
            account.setRole(CmbRole.getValue());
            account.setIsActive(ChkActive != null ? ChkActive.isSelected() : true);

            String password = TxtPassword != null ? TxtPassword.getText() : "";

            if (!isEditMode) {
                userManagementDao.createUserWithAccount(
                        SessionManager.getCurrentAccountId(),
                        user,
                        account,
                        password
                );
                showInfo("Usuario creado correctamente.");
            } else {
                account.setAccountId(editingAccountId);
                user.setAccountId(editingAccountId);

                // ✅ password puede ir vacío; el DAO ya no obliga ni actualiza si viene vacío
                userManagementDao.updateUserWithAccount(
                        SessionManager.getCurrentAccountId(),
                        user,
                        account,
                        password
                );
                showInfo("Usuario actualizado correctamente.");
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }

        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Error al guardar." : ex.getMessage());
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private void setupTextFormatters() {
        TxtUsername.setTextFormatter(new TextFormatter<>(change -> {
            String t = change.getControlNewText();
            return t.length() > USERNAME_MAX_LENGTH ? null : change;
        }));

        TxtFullName.setTextFormatter(new TextFormatter<>(change -> {
            String t = change.getControlNewText();
            return t.length() > FULL_NAME_MAX_LENGTH ? null : change;
        }));

        TxtEmail.setTextFormatter(new TextFormatter<>(change -> {
            String t = change.getControlNewText();
            return t.length() > EMAIL_MAX_LENGTH ? null : change;
        }));

        TxtPhone.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (!newText.matches("\\d*")) return null;
            if (newText.length() > PHONE_LENGTH) return null;
            return change;
        }));

        TxtPassword.setTextFormatter(new TextFormatter<>(change -> {
            String t = change.getControlNewText();
            return t.length() > PASSWORD_MAX_LENGTH ? null : change;
        }));
    }

    private void setupPhonePrefixBehavior() {
        if (TxtPhone == null) return;

        if (TxtPhone.getText() == null || TxtPhone.getText().trim().isEmpty()) {
            TxtPhone.setText(PHONE_FIXED_PREFIX);
            TxtPhone.positionCaret(PHONE_FIXED_PREFIX.length());
        }

        TxtPhone.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                TxtPhone.setText(PHONE_FIXED_PREFIX);
                TxtPhone.positionCaret(PHONE_FIXED_PREFIX.length());
                return;
            }

            if (!newV.matches("\\d*")) {
                TxtPhone.setText(oldV == null ? PHONE_FIXED_PREFIX : oldV);
                return;
            }

            if (!newV.startsWith(PHONE_FIXED_PREFIX)) {
                TxtPhone.setText(PHONE_FIXED_PREFIX);
                TxtPhone.positionCaret(PHONE_FIXED_PREFIX.length());
                return;
            }

            if (newV.length() < PHONE_FIXED_PREFIX.length()) {
                TxtPhone.setText(PHONE_FIXED_PREFIX);
                TxtPhone.positionCaret(PHONE_FIXED_PREFIX.length());
            }
        });
    }

    private boolean validateForm() {
        String username = getTrim(TxtUsername);
        String fullName = getTrim(TxtFullName);
        String email = getTrim(TxtEmail);
        String phone = getTrim(TxtPhone);
        String password = TxtPassword != null ? TxtPassword.getText() : "";

        if (username.isEmpty()) return error("El usuario es obligatorio.");
        if (fullName.isEmpty()) return error("El nombre completo es obligatorio.");
        if (email.isEmpty()) return error("El correo es obligatorio.");
        if (CmbRole == null || CmbRole.getValue() == null) return error("El rol es obligatorio.");

        if (username.length() > USERNAME_MAX_LENGTH) return error("El usuario no puede exceder " + USERNAME_MAX_LENGTH + " caracteres.");
        if (fullName.length() > FULL_NAME_MAX_LENGTH) return error("El nombre completo no puede exceder " + FULL_NAME_MAX_LENGTH + " caracteres.");
        if (email.length() > EMAIL_MAX_LENGTH) return error("El correo no puede exceder " + EMAIL_MAX_LENGTH + " caracteres.");

        if (!EMAIL_PATTERN.matcher(email).matches()) return error("Formato de correo inválido.");

        // ✅ Teléfono exacto 10 (228 + 7)
        if (phone.isEmpty()) return error("El teléfono es obligatorio.");
        if (!phone.matches("\\d+")) return error("El teléfono solo puede contener números.");
        if (!phone.startsWith(PHONE_FIXED_PREFIX)) return error("El teléfono debe iniciar con 228.");
        if (phone.length() != PHONE_LENGTH) return error("El teléfono debe tener 10 dígitos (228 + 7).");
        if (phone.equals(PHONE_FIXED_PREFIX)) return error("Debes completar los 7 dígitos después de 228.");

        // ✅ Password: obligatoria SOLO al crear
        boolean hasPasswordInput = password != null && !password.trim().isEmpty();
        if (!isEditMode) {
            if (!hasPasswordInput) return error("La contraseña es obligatoria.");
        } else {
            // edit: si está vacía, no validar
            if (!hasPasswordInput) {
                return true;
            }
        }

        if (password.length() < PASSWORD_MIN_LENGTH) return error("La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres.");
        if (password.length() > PASSWORD_MAX_LENGTH) return error("La contraseña no puede exceder " + PASSWORD_MAX_LENGTH + " caracteres.");

        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");

        if (!hasLower || !hasUpper || !hasDigit || !hasSpecial) {
            return error("La contraseña debe incluir 1 minúscula, 1 mayúscula, 1 número y 1 caracter especial.");
        }

        return true;
    }

    private boolean error(String msg) {
        showError(msg);
        return false;
    }

    private void disableAll() {
        if (TxtPersonnelNumber != null) TxtPersonnelNumber.setDisable(true);
        if (TxtUsername != null) TxtUsername.setDisable(true);
        if (TxtFullName != null) TxtFullName.setDisable(true);
        if (TxtPhone != null) TxtPhone.setDisable(true);
        if (TxtEmail != null) TxtEmail.setDisable(true);
        if (CmbRole != null) CmbRole.setDisable(true);
        if (TxtPassword != null) TxtPassword.setDisable(true);
        if (ChkActive != null) ChkActive.setDisable(true);
        if (BtnSave != null) BtnSave.setDisable(true);
    }

    private String getTrim(TextField tf) {
        if (tf == null || tf.getText() == null) return "";
        return tf.getText().trim();
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String safePhone(String v) {
        String raw = v == null ? "" : v.replaceAll("\\D", "");
        if (raw.isEmpty()) return PHONE_FIXED_PREFIX;
        if (raw.startsWith(PHONE_FIXED_PREFIX)) return raw;
        return PHONE_FIXED_PREFIX + raw;
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
