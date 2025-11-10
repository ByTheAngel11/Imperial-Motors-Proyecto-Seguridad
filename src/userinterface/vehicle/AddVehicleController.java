package userinterface.vehicle;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import logic.DAO.VehicleDAO;
import logic.DTO.VehicleDTO;
import logic.DTO.VehicleStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AddVehicleController {

    private static final String ERROR_TITLE = "Error al guardar vehículo";
    private static final String SUCCESS_TITLE = "Vehículo creado";
    private static final String SUCCESS_MESSAGE = "El vehículo se guardó correctamente.";
    private static final long NOT_FOUND_VEHICLE_ID = -1L;
    private static final String INVENTORY_FXML_PATH = "/userinterface/inventory/inventory.fxml";

    // Regex
    private static final String INVALID_CHAR_PATTERN = ".*[=()#&/!\"'$%{}\\[\\]<>;,:*+?^\\\\|].*";
    private static final String ONLY_LETTERS_PATTERN = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$";
    private static final String ONLY_DIGITS_PATTERN = "\\d+";
    private static final String YEAR_PATTERN = "\\d{4}";

    @FXML private TextField txtMake;
    @FXML private TextField txtModel;
    @FXML private TextField txtYear;
    @FXML private TextField txtColor;
    @FXML private TextField txtVin;
    @FXML private TextField txtMileage;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<VehicleStatus> cmbStatus;
    @FXML private Button btnSave;

    private final VehicleDAO vehicleDao = new VehicleDAO();

    @FXML
    public void initialize() {
        cmbStatus.setItems(FXCollections.observableArrayList(VehicleStatus.values()));
        if (!cmbStatus.getItems().isEmpty()) {
            cmbStatus.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onSaveClicked(ActionEvent event) {
        try {
            if (!validateForm()) return;

            String make = txtMake.getText().trim();
            String model = txtModel.getText().trim();
            String vin = txtVin.getText().trim();
            String color = txtColor.getText().trim();
            short modelYear = Short.parseShort(txtYear.getText().trim());
            Integer mileageKm = parseNullableInt(txtMileage.getText().trim());
            BigDecimal price = new BigDecimal(txtPrice.getText().trim());
            VehicleStatus status = cmbStatus.getValue();

            VehicleDTO vehicle = new VehicleDTO();
            vehicle.setVin(vin);
            vehicle.setMake(make);
            vehicle.setModel(model);
            vehicle.setModelYear(modelYear);
            vehicle.setColor(color);
            vehicle.setMileageKm(mileageKm);
            vehicle.setPrice(price);
            vehicle.setStatus(status);
            vehicle.setSupplierId(null);
            vehicle.setAcquisitionDate(LocalDate.now());
            vehicle.setCreatedAt(LocalDateTime.now());
            vehicle.setUpdatedAt(LocalDateTime.now());
            vehicle.setDeletedAt(null);

            boolean inserted = vehicleDao.insertVehicle(vehicle);

            if (!inserted || vehicle.getVehicleId() == null || vehicle.getVehicleId() <= 0) {
                showError("No se pudo guardar el vehículo. Intenta de nuevo.");
                return;
            }

            new Alert(Alert.AlertType.INFORMATION, SUCCESS_MESSAGE, ButtonType.OK)
                    .showAndWait();

            navigateToInventory();

        } catch (NumberFormatException ex) {
            showError("Revisa los campos numéricos (Año, Kilometraje, Precio).");
        } catch (SQLException | IOException ex) {
            showError("Ocurrió un error al guardar el vehículo en la base de datos.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void onCancelClicked(ActionEvent event) {
        navigateToInventory();
    }

    private boolean validateForm() {
        String make = safeText(txtMake);
        String model = safeText(txtModel);
        String vin = safeText(txtVin);
        String color = safeText(txtColor);
        String yearText = safeText(txtYear);
        String mileageText = safeText(txtMileage);
        String priceText = safeText(txtPrice);
        VehicleStatus status = cmbStatus.getValue();

        // Campos obligatorios
        if (make.isEmpty() || model.isEmpty() || vin.isEmpty()
                || yearText.isEmpty() || color.isEmpty()
                || priceText.isEmpty() || status == null) {
            showError("Completa todos los campos obligatorios marcados con *.");
            return false;
        }

        // Caracteres prohibidos
        if (make.matches(INVALID_CHAR_PATTERN) || model.matches(INVALID_CHAR_PATTERN) ||
                vin.matches(INVALID_CHAR_PATTERN) || color.matches(INVALID_CHAR_PATTERN)) {
            showError("No se permiten caracteres especiales en los campos de texto.");
            return false;
        }

        // Solo letras en color
        if (!color.matches(ONLY_LETTERS_PATTERN)) {
            showError("El campo Color solo puede contener letras.");
            return false;
        }

        // Año válido
        if (!yearText.matches(YEAR_PATTERN)) {
            showError("El campo Año debe tener 4 dígitos numéricos (ej. 2025).");
            return false;
        }

        int year = Integer.parseInt(yearText);
        int currentYear = LocalDate.now().getYear();

        if (year < 1900 || year > currentYear + 1) {
            showError("El año ingresado no es válido. Debe estar entre 1900 y " + (currentYear + 1) + ".");
            return false;
        }

        // Kilometraje numérico
        if (!mileageText.isEmpty() && !mileageText.matches(ONLY_DIGITS_PATTERN)) {
            showError("El campo Kilometraje solo puede contener números.");
            return false;
        }

        // Precio numérico válido
        if (!priceText.matches("\\d+(\\.\\d{1,2})?")) {
            showError("El campo Precio debe contener solo números (ej. 25000 o 25000.50).");
            return false;
        }

        // VIN único
        try {
            VehicleDTO existingVehicle = vehicleDao.findVehicleByVin(vin);
            if (existingVehicle != null
                    && existingVehicle.getVehicleId() != null
                    && existingVehicle.getVehicleId() != NOT_FOUND_VEHICLE_ID) {
                showError("Ya existe un vehículo registrado con ese VIN.");
                return false;
            }
        } catch (SQLException | IOException ex) {
            showError("Error al validar el VIN en la base de datos.");
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    private String safeText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private Integer parseNullableInt(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        return Integer.parseInt(text.trim());
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    private void navigateToInventory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(INVENTORY_FXML_PATH));
            Parent root = loader.load();
            Stage stage = (Stage) btnSave.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene == null) scene = new Scene(root);
            else scene.setRoot(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            showError("No se pudo regresar a la ventana de inventario.");
            ex.printStackTrace();
        }
    }
}
