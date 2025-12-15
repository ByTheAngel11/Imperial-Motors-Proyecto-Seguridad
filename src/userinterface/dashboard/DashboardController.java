package userinterface.dashboard;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import logic.DAO.AuditLogDAO;
import utilities.SessionManager;

import java.io.IOException;
import java.util.Optional;

public class DashboardController {

    private static final String RESOURCE_LOGIN = "/userinterface/login/LoginView.fxml";

    @FXML
    private AnchorPane contentArea;

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @FXML
    public void initialize() {
    }

    @FXML
    private void onUsers() {
        loadPage("/userinterface/users/users.fxml");
    }

    @FXML
    private void onClients() {
        loadPage("/userinterface/costumers/CustomersView.fxml");
    }

    @FXML
    private void onInventory() {
        loadPage("/userinterface/inventory/Inventory.fxml");
    }

    @FXML
    private void onSales() {
        loadPage("/userinterface/sales/SalesView.fxml");
    }

    @FXML
    private void onPurchases() {
        loadPage("/userinterface/purchase/PurchaseView.fxml");
    }

    @FXML
    private void onProviders() {
        loadPage("/userinterface/suppliers/SupplierView.fxml");
    }

    @FXML
    private void onAudit() {
        if (!SessionManager.isLoggedIn()) {
            mostrarInfo("Permiso denegado", "Debes iniciar sesión.");
            return;
        }
        if (!SessionManager.isAdmin()) {
            mostrarInfo("Permiso denegado", "Solo el administrador puede ver auditoría.");
            return;
        }
        loadPage("/userinterface/audit/AuditView.fxml");
    }

    @FXML
    private void onLogout() {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Estás seguro que quieres cerrar sesión?",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        confirm.setTitle("Cerrar sesión");
        confirm.setHeaderText(null);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            if (SessionManager.isLoggedIn()) {
                Long accountId = SessionManager.getCurrentAccountId();
                auditLogDAO.logLogout(accountId, "");
            }
        } catch (Exception ignored) {
        } finally {
            SessionManager.logout();
            loadLoginView();
        }
    }

    private void loadLoginView() {
        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(RESOURCE_LOGIN));
            Parent loginRoot = loader.load();
            stage.getScene().setRoot(loginRoot);

        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo regresar al login.\n" + ex.getMessage());
            alert.showAndWait();
        }
    }

    private void loadPage(String resourcePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent node = loader.load();

            contentArea.getChildren().setAll(node);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);

        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al cargar módulo");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo cargar: " + resourcePath + "\n" + ex.getMessage());
            alert.showAndWait();
        }
    }

    private void mostrarInfo(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
