package userinterface.purchase;

import dataaccess.ConnectionDataBase;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import logic.DAO.PurchaseOrderDAO;
import logic.DAO.SupplierDAO;
import logic.DTO.PurchaseOrderDTO;
import logic.DTO.PurchaseOrderItemDTO;
import logic.DTO.PurchaseStatus;
import logic.DTO.SupplierDTO;
import utilities.SessionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Locale;
import javafx.scene.control.DateCell;

public class PurchaseFormController {

    // Título
    @FXML private Label LblTitle;

    // Datos vehículo
    @FXML private TextField TxtMake;
    @FXML private TextField TxtModel;
    @FXML private TextField TxtYear;
    @FXML private TextField TxtVin;
    @FXML private TextField TxtColor;

    // Datos compra
    @FXML private TextField TxtSupplierId;
    @FXML private TextField TxtAgreedPrice;
    @FXML private DatePicker DpExpectedDate;

    @FXML private Button BtnSave;
    @FXML private Button BtnCancel;

    private final PurchaseOrderDAO purchaseOrderDao = new PurchaseOrderDAO();
    private final SupplierDAO supplierDao = new SupplierDAO();

    private Runnable onSaveCallback;
    private Runnable onCloseCallback;

    // SQL local para insertar vehículo SIN tocar el módulo de inventario
    private static final String SQL_INSERT_VEHICLE =
            "INSERT INTO vehicle (make, model, model_year, color, vin, price, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'RESERVADO')";

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void initialize() {
        LblTitle.setText("Registrar compra");

        DpExpectedDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setDisable(false);
                } else {
                    setDisable(date.isBefore(LocalDate.now()));
                }
            }
        });
    }

    @FXML
    private void onSave() {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión para registrar compras.");
                return;
            }

            // -------- Validaciones vehículo --------
            validateRequired(TxtMake.getText(), "Marca");
            validateRequired(TxtModel.getText(), "Modelo");
            validateRequired(TxtYear.getText(), "Año");
            validateRequired(TxtVin.getText(), "VIN");
            validateRequired(TxtColor.getText(), "Color");

            int modelYear = parseInt(TxtYear.getText(), "Año");
            String color = TxtColor.getText().trim();

            // -------- Validaciones compra --------
            validateRequired(TxtSupplierId.getText(), "ID proveedor");
            validateRequired(TxtAgreedPrice.getText(), "Precio acordado");

            long supplierId = parseLong(TxtSupplierId.getText(), "ID proveedor");
            BigDecimal agreedPrice = parseMoney(TxtAgreedPrice.getText(), "Precio acordado");

            LocalDate expected = DpExpectedDate.getValue();
            if (expected == null) {
                showError("La fecha esperada es obligatoria.");
                return;
            }
            if (expected.isBefore(LocalDate.now())) {
                showError("La fecha esperada no puede ser anterior a hoy.");
                return;
            }

            // Validar proveedor existente y activo
            SupplierDTO supplier;
            try {
                supplier = supplierDao.findSupplierById(supplierId);
            } catch (SQLException ex) {
                showError("No se pudo consultar el proveedor: " + ex.getMessage());
                return;
            }

            if (supplier == null || supplier.getSupplierId() == null) {
                showError("El proveedor indicado no existe.");
                return;
            }

            if (Boolean.FALSE.equals(supplier.getActive())) {
                showError("No se pueden registrar compras con un proveedor inactivo.");
                return;
            }

            // 1) Crear vehículo y obtener su ID
            long vehicleId = createVehicleFromForm(
                    TxtMake.getText().trim(),
                    TxtModel.getText().trim(),
                    modelYear,
                    color,
                    TxtVin.getText().trim(),
                    agreedPrice
            );

            // 2) Crear compra ligando ese vehículo
            PurchaseOrderItemDTO item = new PurchaseOrderItemDTO();
            item.setVehicleId(vehicleId);
            item.setAgreedPrice(agreedPrice);

            PurchaseOrderDTO order = new PurchaseOrderDTO();
            order.setSupplierId(supplierId);
            order.setAccountId(SessionManager.getCurrentAccountId());
            order.setStatus(PurchaseStatus.CREADA);
            order.setSubtotal(agreedPrice);
            order.setDiscount(BigDecimal.ZERO);
            order.setTaxes(BigDecimal.ZERO);
            order.setTotal(agreedPrice);
            order.setExpectedDate(expected);
            order.setItems(Collections.singletonList(item));

            purchaseOrderDao.createPurchaseOrder(order);

            showInfo("Compra registrada correctamente en estado CREADA.\n" +
                    "Vehículo creado con ID " + vehicleId + ".");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            if (onCloseCallback != null) {
                onCloseCallback.run();
            }

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError(mapDatabaseError(ex));
        } catch (Exception ex) {
            showError("Error al registrar la compra. Verifica los datos e inténtalo de nuevo.");
        }
    }

    @FXML
    private void onCancel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    // ======================
    //  Helpers de negocio
    // ======================

    /**
     * Inserta un vehículo en la tabla vehicle y devuelve su ID generado.
     * No toca el módulo de inventario.
     */
    private long createVehicleFromForm(
            String make,
            String model,
            int modelYear,
            String color,
            String vin,
            BigDecimal price) throws SQLException {

        try (Connection connection = ConnectionDataBase.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     SQL_INSERT_VEHICLE, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, make);
            stmt.setString(2, model);
            stmt.setInt(3, modelYear);
            stmt.setString(4, color);
            stmt.setString(5, vin);
            stmt.setBigDecimal(6, price);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No se pudo insertar el vehículo.");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("No se pudo obtener el ID del vehículo.");
                }
                return rs.getLong(1);
            }
        }
    }

    private void validateRequired(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo \"" + name + "\" es obligatorio.");
        }
    }

    private long parseLong(String text, String field) {
        try {
            return Long.parseLong(text.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(field + " debe ser entero.");
        }
    }

    private int parseInt(String text, String field) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(field + " debe ser entero.");
        }
    }

    private BigDecimal parseMoney(String text, String field) {
        try {
            return new BigDecimal(text.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(field + " debe ser numérico.");
        }
    }

    private String mapDatabaseError(SQLException ex) {
        String msg = ex.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase(Locale.ROOT);

            if (lower.contains("fk_purchase_supplier")
                    || (lower.contains("foreign key") && lower.contains("supplier_id"))) {
                return "El proveedor especificado no existe. Verifica el ID de proveedor.";
            }

            if (lower.contains("vehicle")
                    && lower.contains("foreign key")
                    && lower.contains("vehicle_id")) {
                return "El vehículo especificado no existe o no es válido.";
            }

            if (lower.contains("vin") && lower.contains("duplicate")) {
                return "Ya existe un vehículo con ese VIN.";
            }

            if (lower.contains("color") && lower.contains("null")) {
                return "El color del vehículo es obligatorio.";
            }
        }

        return "Error al guardar en la base de datos. Verifica los datos e inténtalo de nuevo.";
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
