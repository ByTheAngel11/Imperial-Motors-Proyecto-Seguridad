package userinterface.dashboard;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class DashboardController {

    @FXML
    private AnchorPane contentArea;

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
    private void onInventory() { loadPage("/userinterface/inventory/Inventory.fxml"); }

    @FXML
    private void onSales() { loadPage("/userinterface/sales/SalesView.fxml");}
    @FXML
    private void onPurchases() {
        loadPage("/userinterface/purchase/PurchaseView.fxml");}
    @FXML
    private void onProviders() {
        mostrarPendiente("Proveedores");
    }

    @FXML
    private void onReports() {
        mostrarPendiente("Reportes");
    }

    @FXML
    private void onConfiguration() {
        mostrarPendiente("Configuración");
    }

    @FXML
    private void onAbout() {
        mostrarPendiente("Acerca de");
    }

    @FXML
    private void onLogout() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Cerrar sesión");
        a.setHeaderText(null);
        a.setContentText("Sesión finalizada.");
        a.showAndWait();
        System.exit(0);
    }

    private void loadPage(String resourcePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Node node = loader.load();

            contentArea.getChildren().setAll(node);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al cargar módulo");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo cargar: " + resourcePath + "\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    private void mostrarPendiente(String modulo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Módulo en implementación");
        alert.setHeaderText(null);
        alert.setContentText("El módulo de " + modulo + " aún no está implementado.");
        alert.showAndWait();
    }
}
