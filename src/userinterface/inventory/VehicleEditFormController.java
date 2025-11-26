package userinterface.inventory;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import logic.DAO.VehicleDAO;
import logic.DTO.VehicleDTO;
import utilities.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

public class VehicleEditFormController {

    @FXML private Label LblTitle;

    @FXML private TextField TxtVehicleId;
    @FXML private TextField TxtVin;
    @FXML private TextField TxtMake;
    @FXML private TextField TxtModel;
    @FXML private TextField TxtYear;

    @FXML private TextField TxtColor;
    @FXML private TextField TxtMileageKm;
    @FXML private TextField TxtPrice;

    @FXML private Button BtnSave;
    @FXML private Button BtnCancel;

    private final VehicleDAO vehicleDAO = new VehicleDAO();

    private VehicleDTO vehicle;
    private Runnable onSaveCallback;
    private Runnable onCloseCallback;

    public void setVehicleToEdit(VehicleDTO vehicle) {
        this.vehicle = vehicle;
        if (vehicle == null) {
            return;
        }

        TxtVehicleId.setText(
                vehicle.getVehicleId() == null ? "" : vehicle.getVehicleId().toString());
        TxtVin.setText(vehicle.getVin());
        TxtMake.setText(vehicle.getMake());
        TxtModel.setText(vehicle.getModel());
        TxtYear.setText(
                vehicle.getModelYear() == null ? "" : vehicle.getModelYear().toString());

        TxtColor.setText(vehicle.getColor() == null ? "" : vehicle.getColor());

        if (vehicle.getMileageKm() != null) {
            TxtMileageKm.setText(vehicle.getMileageKm().toString());
        } else {
            TxtMileageKm.clear();
        }

        if (vehicle.getPrice() != null) {
            TxtPrice.setText(vehicle.getPrice().toPlainString());
        } else {
            TxtPrice.clear();
        }
    }

    public void setOnSaveCallback(Runnable onSaveCallback) {
        this.onSaveCallback = onSaveCallback;
    }

    public void setOnCloseCallback(Runnable onCloseCallback) {
        this.onCloseCallback = onCloseCallback;
    }

    @FXML
    private void onSave() {
        try {
            if (!SessionManager.isLoggedIn() || !SessionManager.isAdmin()) {
                showError("Solo el administrador autenticado puede editar vehículos.");
                return;
            }

            if (vehicle == null || vehicle.getVehicleId() == null) {
                showError("Vehículo no válido para edición.");
                return;
            }

            String color = safeTrim(TxtColor.getText());
            if (color.isEmpty()) {
                showError("El color es obligatorio.");
                return;
            }

            Integer mileageKm = null;
            String mileageText = safeTrim(TxtMileageKm.getText());
            if (!mileageText.isEmpty()) {
                try {
                    mileageKm = Integer.parseInt(mileageText);
                    if (mileageKm < 0) {
                        showError("El kilometraje no puede ser negativo.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showError("El kilometraje debe ser un número entero.");
                    return;
                }
            }

            String priceText = safeTrim(TxtPrice.getText());
            if (priceText.isEmpty()) {
                showError("El precio es obligatorio.");
                return;
            }

            BigDecimal price;
            try {
                price = new BigDecimal(priceText);
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    showError("El precio no puede ser negativo.");
                    return;
                }
            } catch (NumberFormatException ex) {
                showError("El precio debe ser un número válido.");
                return;
            }

            vehicleDAO.updateVehicleDetails(
                    vehicle.getVehicleId(),
                    color,
                    mileageKm,
                    price);

            showInfo("Vehículo actualizado correctamente.");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

        } catch (SQLException | IOException ex) {
            showError("Error al actualizar el vehículo: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private String safeTrim(String text) {
        return text == null ? "" : text.trim();
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
