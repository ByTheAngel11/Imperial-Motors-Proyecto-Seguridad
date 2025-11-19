package userinterface.sales;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import logic.DAO.SaleDAO;
import logic.DAO.VehicleDAO;
import logic.DTO.SaleDTO;
import logic.DTO.SaleStatus;
import logic.DTO.VehicleDTO;
import utilities.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SalesController {

    private static final String CUSTOMER_NUMBER_PATTERN = "C\\d{4}";

    @FXML
    private AnchorPane contentArea;

    @FXML private TextField TxtFolio;
    @FXML private TextField TxtVehicleId;
    @FXML private TextField TxtCostumerNumber;
    @FXML private TextField TxtSubtotal;
    @FXML private TextField TxtDiscount;
    @FXML private TextField TxtTaxes;
    @FXML private TextField TxtTotal;

    @FXML private TextField TxtSearchSale;
    @FXML private ComboBox<SaleStatus> CmbStatusFilter;

    @FXML private TableView<SaleDTO> TblSales;
    @FXML private TableColumn<SaleDTO, String> ColFolio;
    @FXML private TableColumn<SaleDTO, String> ColCustomer;
    @FXML private TableColumn<SaleDTO, String> ColVehicle;
    @FXML private TableColumn<SaleDTO, String> ColTotal;
    @FXML private TableColumn<SaleDTO, SaleStatus> ColStatus;

    @FXML private Button BtnNewSale;
    @FXML private Button BtnSaveSale;
    @FXML private Button BtnClearForm;

    @FXML private TextField TxtStatus;
    @FXML private Button BtnAnnulSale;

    private final SaleDAO saleDao = new SaleDAO();
    private final VehicleDAO vehicleDao = new VehicleDAO();

    private boolean isAdmin;
    private final ObservableList<SaleDTO> allSales = FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        isAdmin = SessionManager.isAdmin();

        configureStatusFilter();
        configureTableColumns();
        configureSelectionListener();
        configureSearchFilter();
        configureDetailRecalc();

        clearForm();
        setFormDisabled(true);

        reloadSalesTable();
    }

    private void configureStatusFilter() {
        CmbStatusFilter.getItems().setAll(SaleStatus.values());
        CmbStatusFilter.setPromptText("Estado de las ventas");
        CmbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void configureSearchFilter() {
        TxtSearchSale.textProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void configureTableColumns() {
        ColFolio.setCellValueFactory(
                cell -> new SimpleStringProperty(safe(cell.getValue().getFolio())));

        ColCustomer.setCellValueFactory(
                cell -> new SimpleStringProperty(safe(cell.getValue().getCostumerNumber())));

        ColVehicle.setCellValueFactory(
                cell -> {
                    Long id = cell.getValue().getVehicleId();
                    return new SimpleStringProperty(id == null ? "" : id.toString());
                });

        ColTotal.setCellValueFactory(
                cell -> {
                    BigDecimal t = cell.getValue().getTotal();
                    return new SimpleStringProperty(t == null ? "" : t.toPlainString());
                });

        ColStatus.setCellValueFactory(
                cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));
    }

    private void configureSelectionListener() {
        TblSales.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> onSaleSelected(newSel));
    }

    private void configureDetailRecalc() {
        TxtDiscount.textProperty().addListener((obs, o, n) -> recalcDetailTotal());
        TxtTaxes.textProperty().addListener((obs, o, n) -> recalcDetailTotal());
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String safeDec(BigDecimal v) {
        return v == null ? "" : v.toPlainString();
    }

    private void onSaleSelected(SaleDTO sale) {
        if (sale == null) {
            clearForm();
            setFormDisabled(true);
            return;
        }

        TxtFolio.setText(safe(sale.getFolio()));
        TxtVehicleId.setText(String.valueOf(sale.getVehicleId()));
        TxtCostumerNumber.setText(safe(sale.getCostumerNumber()));
        TxtSubtotal.setText(safeDec(sale.getSubtotal()));

        BigDecimal discount = sale.getDiscount();
        BigDecimal subtotal = sale.getSubtotal();

        if (discount != null && subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal perc = discount
                    .divide(subtotal, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            TxtDiscount.setText(perc.toPlainString());
        } else {
            TxtDiscount.setText("0");
        }

        TxtTaxes.setText(safeDec(sale.getTaxes()));
        TxtTotal.setText(safeDec(sale.getTotal()));

        TxtStatus.setText(
                sale.getStatus() == null ? "" : sale.getStatus().name()
        );

        setFormDisabled(!isAdmin);
    }

    private void recalcDetailTotal() {
        try {
            String subtotalText = TxtSubtotal.getText();
            if (subtotalText == null || subtotalText.trim().isEmpty()) {
                TxtTotal.clear();
                return;
            }

            BigDecimal subtotal = parseMoney(subtotalText, "Subtotal");
            BigDecimal discountPercentage = parsePercentage(TxtDiscount.getText());
            BigDecimal taxes = parseOptionalMoney(TxtTaxes.getText());

            BigDecimal discount = subtotal
                    .multiply(discountPercentage
                            .divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            BigDecimal total = subtotal.subtract(discount).add(taxes);

            TxtTotal.setText(total.toPlainString());
        } catch (Exception ex) {
            // Ignoramos mientras el usuario escribe (para no spamear alertas)
        }
    }

    private void setFormDisabled(boolean disabled) {

        TxtSubtotal.setDisable(true);
        TxtTotal.setDisable(true);
        TxtStatus.setDisable(true);
        TxtFolio.setDisable(true);

        if (disabled) {
            TxtVehicleId.setDisable(true);
            TxtCostumerNumber.setDisable(true);
            TxtDiscount.setDisable(true);
            TxtTaxes.setDisable(true);

            BtnSaveSale.setDisable(true);
            BtnClearForm.setDisable(true);
            BtnAnnulSale.setDisable(true);
        } else {
            TxtVehicleId.setDisable(false);
            TxtCostumerNumber.setDisable(false);
            TxtDiscount.setDisable(false);
            TxtTaxes.setDisable(false);

            BtnSaveSale.setDisable(false);
            BtnClearForm.setDisable(false);
            BtnAnnulSale.setDisable(false);
        }
    }

    // =========================
    //  Abrir formulario embebido
    // =========================
    @FXML
    private void onNewSale() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/sales/SaleFormView.fxml"));

            AnchorPane form = loader.load();
            SaleFormController controller = loader.getController();

            controller.setOnSaveCallback(this::reloadSalesTable);
            controller.setOnCloseCallback(this::showMainView);

            contentArea.getChildren().setAll(form);
            AnchorPane.setTopAnchor(form, 0.0);
            AnchorPane.setBottomAnchor(form, 0.0);
            AnchorPane.setLeftAnchor(form, 0.0);
            AnchorPane.setRightAnchor(form, 0.0);

        } catch (IOException ex) {
            showError("Error al cargar formulario: " + ex.getMessage());
        }
    }

    // =======================
    //  Regresar a la vista lista
    // =======================
    private void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/sales/SalesView.fxml"));

            AnchorPane main = loader.load();
            contentArea.getChildren().setAll(main);

            AnchorPane.setTopAnchor(main, 0.0);
            AnchorPane.setBottomAnchor(main, 0.0);
            AnchorPane.setLeftAnchor(main, 0.0);
            AnchorPane.setRightAnchor(main, 0.0);

        } catch (IOException ex) {
            showError("Error al regresar: " + ex.getMessage());
        }
    }

    @FXML
    private void onSaveSale() {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión.");
                return;
            }

            if (!SessionManager.isAdmin()) {
                showError("Solo el administrador puede modificar ventas.");
                return;
            }

            SaleDTO selected = TblSales.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Selecciona una venta.");
                return;
            }

            if (selected.getStatus() == SaleStatus.ANULADA) {
                showError("No se puede modificar una venta anulada.");
                return;
            }

            validateRequired(TxtVehicleId.getText(), "ID vehículo");
            validateRequired(TxtCostumerNumber.getText(), "No. cliente");
            validateRequired(TxtSubtotal.getText(), "Subtotal");

            String customerNumber = TxtCostumerNumber.getText().trim();
            if (!customerNumber.matches(CUSTOMER_NUMBER_PATTERN)) {
                showError("Formato del cliente inválido. Ejemplo: C0001.");
                return;
            }

            long vehicleId = parseLong(TxtVehicleId.getText(), "ID vehículo");

            VehicleDTO vehicle = vehicleDao.findVehicleById(vehicleId);
            if (vehicle == null || vehicle.getVehicleId() == null) {
                showError("El vehículo no existe.");
                return;
            }

            BigDecimal subtotal = parseMoney(TxtSubtotal.getText(), "Subtotal");
            BigDecimal discountPercentage = parsePercentage(TxtDiscount.getText());
            BigDecimal taxes = parseOptionalMoney(TxtTaxes.getText());

            BigDecimal discount = subtotal
                    .multiply(discountPercentage
                            .divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            BigDecimal total = subtotal.subtract(discount).add(taxes);

            SaleStatus status = selected.getStatus();

            SaleDTO sale = new SaleDTO();
            sale.setSaleId(selected.getSaleId());
            sale.setFolio(selected.getFolio());
            sale.setVehicleId(vehicleId);
            sale.setCostumerNumber(customerNumber);
            sale.setSellerAccountId(selected.getSellerAccountId());
            sale.setStatus(status);
            sale.setSubtotal(subtotal);
            sale.setDiscount(discount);
            sale.setTaxes(taxes);
            sale.setTotal(total);

            sale.setCreatedAt(selected.getCreatedAt());
            sale.setClosedAt(selected.getClosedAt());
            sale.setAnnulledAt(selected.getAnnulledAt());
            sale.setAnnulReason(selected.getAnnulReason());

            saleDao.updateSaleWithAudit(sale);

            showInfo("Venta actualizada correctamente.");
            reloadSalesTable();

        } catch (Exception ex) {
            showError("Error al guardar la venta: " + ex.getMessage());
        }
    }

    @FXML
    private void onAnnulSale() {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión.");
                return;
            }

            if (!SessionManager.isAdmin()) {
                showError("Solo el administrador puede anular ventas.");
                return;
            }

            SaleDTO selected = TblSales.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Selecciona una venta.");
                return;
            }

            if (selected.getStatus() == SaleStatus.ANULADA) {
                showInfo("La venta ya está anulada.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Anular venta");
            confirm.setHeaderText(null);
            confirm.setContentText(
                    "¿Deseas anular la venta con folio " + safe(selected.getFolio()) + "?"
            );

            var result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            SaleDTO sale = new SaleDTO();
            sale.setSaleId(selected.getSaleId());
            sale.setFolio(selected.getFolio());
            sale.setVehicleId(selected.getVehicleId());
            sale.setCostumerNumber(selected.getCostumerNumber());
            sale.setSellerAccountId(selected.getSellerAccountId());
            sale.setStatus(SaleStatus.ANULADA);
            sale.setSubtotal(selected.getSubtotal());
            sale.setDiscount(selected.getDiscount());
            sale.setTaxes(selected.getTaxes());
            sale.setTotal(selected.getTotal());
            sale.setCreatedAt(selected.getCreatedAt());
            sale.setClosedAt(selected.getClosedAt());

            saleDao.updateSaleWithAudit(sale);

            showInfo("Venta anulada correctamente.");
            reloadSalesTable();

        } catch (Exception ex) {
            showError("Error al anular la venta: " + ex.getMessage());
        }
    }

    @FXML
    private void onClearForm() {
        TblSales.getSelectionModel().clearSelection();
        clearForm();
        setFormDisabled(true);
    }

    // ======================
    //  Recarga + filtros
    // ======================
    private void reloadSalesTable() {
        try {
            List<SaleDTO> sales = saleDao.getAllSales();
            allSales.setAll(sales);
            applyFilter();
        } catch (SQLException ex) {
            showError("No se pudieron cargar las ventas:\n" + ex.getMessage());
        }
    }

    private void applyFilter() {
        SaleStatus filterStatus = CmbStatusFilter.getValue();
        String sText = TxtSearchSale.getText();
        final String search = (sText == null) ? "" : sText.trim().toLowerCase(Locale.ROOT);

        List<SaleDTO> filtered = allSales.stream()
                .filter(s -> {
                    if (filterStatus == null) {
                        return s.getStatus() != SaleStatus.ANULADA;
                    }
                    return s.getStatus() == filterStatus;
                })
                .filter(s -> {
                    if (search.isEmpty()) return true;

                    String folio = safe(s.getFolio()).toLowerCase(Locale.ROOT);
                    String customer = safe(s.getCostumerNumber()).toLowerCase(Locale.ROOT);
                    String vehicleId = s.getVehicleId() == null ? "" : s.getVehicleId().toString();

                    return folio.contains(search)
                            || customer.contains(search)
                            || vehicleId.contains(search);
                })
                .collect(Collectors.toList());

        TblSales.setItems(FXCollections.observableArrayList(filtered));
    }

    private void clearForm() {
        TxtFolio.clear();
        TxtVehicleId.clear();
        TxtCostumerNumber.clear();
        TxtSubtotal.clear();
        TxtDiscount.clear();
        TxtTaxes.clear();
        TxtTotal.clear();
        TxtStatus.clear();
    }

    // ======================
    //  Helpers parse / validación
    // ======================
    private BigDecimal parseMoney(String text, String field) {
        try {
            return new BigDecimal(text.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(field + " debe ser numérico.");
        }
    }

    private BigDecimal parseOptionalMoney(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        return parseMoney(text, "Valor");
    }

    private long parseLong(String text, String field) {
        try {
            return Long.parseLong(text.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(field + " debe ser entero.");
        }
    }

    private BigDecimal parsePercentage(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;

        try {
            BigDecimal p = new BigDecimal(text.trim());
            if (p.compareTo(BigDecimal.ZERO) < 0 || p.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Descuento debe estar entre 0 y 100.");
            }
            return p;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Descuento inválido.");
        }
    }

    private void validateRequired(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo " + name + " es obligatorio.");
        }
    }

    private void showError(String text) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(text);
        a.showAndWait();
    }

    private void showInfo(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(null);
        a.setContentText(text);
        a.showAndWait();
    }
}
