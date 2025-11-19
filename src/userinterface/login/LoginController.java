package userinterface.login;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import logic.DAO.AccountDAO;
import logic.DTO.AccountDTO;
import utilities.PasswordUtiities;
import utilities.SessionManager;
import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    private Hyperlink lnkForgot;

    private final AccountDAO accountDAO = new AccountDAO();

    @FXML
    public void initialize() {
        btnLogin.setDisable(true);

        txtEmail.textProperty().addListener((obs, oldV, newV) -> updateButtonState());
        txtPassword.textProperty().addListener((obs, oldV, newV) -> updateButtonState());

        lnkForgot.setOnAction(e -> {
            Alert a = new Alert(AlertType.INFORMATION);
            a.setTitle("Recuperar contraseña");
            a.setHeaderText(null);
            a.setContentText("Funcionalidad de recuperación no implementada.");
            a.showAndWait();
        });
    }

    private void updateButtonState() {
        boolean disable = txtEmail.getText() == null || txtEmail.getText().trim().isEmpty()
                || txtPassword.getText() == null || txtPassword.getText().trim().isEmpty();
        btnLogin.setDisable(disable);
    }

    @FXML
    private void onLogin() {
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText() : "";

        if (email.isEmpty() || password.isEmpty()) {
            mostrarError("Campos requeridos", "Ingresa correo y contraseña.");
            return;
        }

        try {
            AccountDTO account = accountDAO.findAccountByEmail(email.toLowerCase());

            if (account == null) {
                mostrarErrorLogin();
                return;
            }

            if (!Boolean.TRUE.equals(account.getIsActive()) || account.getDeletedAt() != null) {
                mostrarError("Cuenta inactiva",
                        "La cuenta está desactivada o ha sido eliminada. Consulta con el administrador.");
                return;
            }

            boolean ok = PasswordUtiities.verifyPassword(password, account.getPasswordHash());
            if (!ok) {
                mostrarErrorLogin();
                return;
            }
            SessionManager.setCurrentAccountId(account.getAccountId());
            SessionManager.setCurrentRole(account.getRole()); 
            SessionManager.setCurrentIsActive(account.getIsActive());
            abrirDashboard(account);

        } catch (SQLException | IOException ex) {
            mostrarError("Error de autenticación", ex.getMessage());
        }
    }

    private void abrirDashboard(AccountDTO account) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/dashboard/DashboardView.fxml")
            );
            Parent root = loader.load();

            Stage currentStage = (Stage) btnLogin.getScene().getWindow();

            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Imperial Motors - Dashboard");
            currentStage.centerOnScreen();

        } catch (IOException ex) {
            mostrarError("Error al abrir el dashboard", ex.getMessage());
        }
    }

    private void mostrarErrorLogin() {
        mostrarError("Credenciales inválidas",
                "Correo o contraseña incorrectos. Verifica tus datos e intenta de nuevo.");
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert a = new Alert(AlertType.ERROR);
        a.isResizable();
        a.setResizable(true);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}