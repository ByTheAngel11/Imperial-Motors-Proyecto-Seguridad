package userinterface.sales;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import logic.DAO.SaleDAO;
import logic.DAO.VehicleDAO;
import logic.DTO.SaleDTO;
import logic.DTO.SaleStatus;
import logic.DTO.VehicleDTO;
import utilities.SessionManager;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class SaleFormController {

    private static final String CUSTOMER_PATTERN = "C\\d{4}";

    @FXML private Label LblTitle;
    @FXML private TextField TxtFolio;
    @FXML private TextField TxtVehicleId;
    @FXML private TextField TxtCostumerNumber;
    @FXML private TextField TxtSubtotal;
    @FXML private TextField TxtDiscount;
    @FXML private TextField TxtTaxes;
    @FXML private TextField TxtTotal;

    @FXML private Button BtnSave;
    @FXML private Button BtnCancel;

    private final VehicleDAO vehicleDao = new VehicleDAO();
    private final SaleDAO saleDao = new SaleDAO();

    private Runnable onSaveCallback;
    private Runnable onCloseCallback;

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void initialize() {
        LblTitle.setText("Registrar venta");
        TxtFolio.setText(generateFolio());

        TxtSubtotal.setEditable(false);
        TxtTotal.setEditable(false);

        TxtVehicleId.textProperty().addListener((obs, oldVal, newVal) -> loadVehiclePrice());
        TxtDiscount.textProperty().addListener((obs, o, n) -> recalcTotal());
        TxtTaxes.textProperty().addListener((obs, o, n) -> recalcTotal());
    }

    private void loadVehiclePrice() {
        try {
            if (TxtVehicleId.getText().trim().isEmpty()) {
                return;
            }

            long vehicleId = Long.parseLong(TxtVehicleId.getText().trim());
            VehicleDTO vehicle = vehicleDao.findVehicleById(vehicleId);

            if (vehicle == null || vehicle.getVehicleId() == null) {
                TxtSubtotal.setText("");
                return;
            }

            TxtSubtotal.setText(vehicle.getPrice().toPlainString());
            recalcTotal();

        } catch (Exception ex) {
            TxtSubtotal.setText("");
        }
    }

    private void recalcTotal() {
        try {
            BigDecimal subtotal = parseMoney(TxtSubtotal.getText());
            BigDecimal discountPercent = parsePercentage(TxtDiscount.getText());
            BigDecimal taxes = parseOptional(TxtTaxes.getText());

            BigDecimal discountAmt = subtotal
                    .multiply(discountPercent.divide(new BigDecimal(100)))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            BigDecimal total = subtotal.subtract(discountAmt).add(taxes);

            TxtTotal.setText(total.toPlainString());

        } catch (Exception ignored) {
        }
    }

    private String generateFolio() {
        return "V-" + LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    @FXML
    private void onSave() {
        try {
            validateRequired(TxtVehicleId.getText(), "ID vehículo");
            validateRequired(TxtCostumerNumber.getText(), "Número de cliente");

            if (!TxtCostumerNumber.getText().matches(CUSTOMER_PATTERN)) {
                showError("El número de cliente debe ser como: C0001");
                return;
            }

            long vehicleId = Long.parseLong(TxtVehicleId.getText());
            VehicleDTO vehicle = vehicleDao.findVehicleById(vehicleId);

            if (vehicle == null || vehicle.getVehicleId() == null) {
                showError("El vehículo no existe.");
                return;
            }

            BigDecimal subtotal = parseMoney(TxtSubtotal.getText());
            BigDecimal discountPercent = parsePercentage(TxtDiscount.getText());
            BigDecimal taxes = parseOptional(TxtTaxes.getText());

            BigDecimal discountAmt = subtotal
                    .multiply(discountPercent.divide(new BigDecimal(100)))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            BigDecimal total = subtotal.subtract(discountAmt).add(taxes);

            SaleDTO sale = new SaleDTO();
            sale.setFolio(TxtFolio.getText());
            sale.setVehicleId(vehicleId);
            sale.setCostumerNumber(TxtCostumerNumber.getText().trim());
            sale.setSubtotal(subtotal);
            sale.setDiscount(discountAmt);
            sale.setTaxes(taxes);
            sale.setTotal(total);
            sale.setStatus(SaleStatus.COMPLETADA);
            sale.setCreatedAt(LocalDateTime.now());
            sale.setSellerAccountId(SessionManager.getCurrentAccountId());

            saleDao.createSaleWithLogAndInventory(sale);

            showInfo("Venta registrada correctamente.");

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

    private BigDecimal parseMoney(String text) {
        return new BigDecimal(text.trim());
    }

    private BigDecimal parseOptional(String t) {
        if (t == null || t.isBlank()) {
            return BigDecimal.ZERO;
        }
        return parseMoney(t);
    }

    private BigDecimal parsePercentage(String t) {
        if (t == null || t.isBlank()) {
            return BigDecimal.ZERO;
        }

        BigDecimal p = new BigDecimal(t.trim());
        if (p.compareTo(BigDecimal.ZERO) < 0 || p.compareTo(new BigDecimal(100)) > 0) {
            throw new IllegalArgumentException("El descuento debe estar entre 0% y 100%");
        }
        return p;
    }

    private void validateRequired(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("El campo \"" + field + "\" es obligatorio.");
        }
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
