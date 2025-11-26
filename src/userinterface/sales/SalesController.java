package userinterface.sales;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.ListCell;

import logic.DAO.SaleDAO;
import logic.DAO.VehicleDAO;
import logic.DTO.SaleDTO;
import logic.DTO.SaleStatus;
import logic.DTO.SalesReportRange;
import logic.DTO.VehicleDTO;
import utilities.SessionManager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

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

    @FXML private TextField TxtVehicleMake;
    @FXML private TextField TxtVehicleModel;
    @FXML private TextField TxtVehicleColor;
    @FXML private TextField TxtVehicleYear;

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
    @FXML private ComboBox<SalesReportRange> CmbReportRange;
    @FXML private Button BtnExportReport;

    private final SaleDAO saleDao = new SaleDAO();
    private final VehicleDAO vehicleDao = new VehicleDAO();

    private static final String SALES_REPORT_DIR_NAME = "ImperialReports";
    private static final String SALES_REPORT_FILE_PREFIX = "reporte_ventas_";
    private static final int LAST_DAYS_COUNT = 7;

    private static final float REPORT_MARGIN_LEFT = 50f;
    private static final float REPORT_MARGIN_TOP = 750f;
    private static final float REPORT_LINE_HEIGHT = 16f;
    private static final float REPORT_MIN_Y = 80f;

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
        configureReportRange();

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
        TxtSubtotal.textProperty().addListener((obs, o, n) -> recalcDetailTotal());
        TxtDiscount.textProperty().addListener((obs, o, n) -> recalcDetailTotal());
        TxtTaxes.textProperty().addListener((obs, o, n) -> recalcDetailTotal());

        TxtVehicleId.focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (!newFocus) {
                onVehicleIdChanged();
            }
        });
    }

    private void configureReportRange() {
        if (CmbReportRange == null) {
            return;
        }

        // Items
        CmbReportRange.getItems().setAll(SalesReportRange.values());

        // Cómo se ven en el dropdown
        CmbReportRange.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(SalesReportRange item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getRangeLabel(item));
                }
            }
        });

        // Cómo se ve el seleccionado
        CmbReportRange.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SalesReportRange item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Periodo");
                } else {
                    setText(getRangeLabel(item));
                }
            }
        });

        CmbReportRange.getSelectionModel().select(SalesReportRange.ALL);
        CmbReportRange.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private String getRangeLabel(SalesReportRange range) {
        if (range == null) {
            return "";
        }
        switch (range) {
            case ALL:
                return "Todas las ventas";
            case TODAY:
                return "Hoy";
            case LAST_7_DAYS:
                return "Últimos 7 días";
            case THIS_MONTH:
                return "Este mes";
            case THIS_YEAR:
                return "Año actual";
            default:
                return "";
        }
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String safeDec(BigDecimal v) {
        return v == null ? "" : v.toPlainString();
    }

    // ==== helpers para detalle del vehículo ====
    private void clearVehicleFields() {
        if (TxtVehicleMake != null) TxtVehicleMake.clear();
        if (TxtVehicleModel != null) TxtVehicleModel.clear();
        if (TxtVehicleColor != null) TxtVehicleColor.clear();
        if (TxtVehicleYear != null) TxtVehicleYear.clear();
    }

    private void fillVehicleFields(VehicleDTO vehicle) {
        if (vehicle == null) {
            clearVehicleFields();
            return;
        }

        if (TxtVehicleMake != null) {
            TxtVehicleMake.setText(safe(vehicle.getMake()));
        }
        if (TxtVehicleModel != null) {
            TxtVehicleModel.setText(safe(vehicle.getModel()));
        }
        if (TxtVehicleColor != null) {
            TxtVehicleColor.setText(safe(vehicle.getColor()));
        }
        if (TxtVehicleYear != null) {
            TxtVehicleYear.setText(
                    vehicle.getModelYear() == null ? "" : vehicle.getModelYear().toString()
            );
        }
    }
    // ===========================================

    private void onSaleSelected(SaleDTO sale) {
        if (sale == null) {
            clearForm();
            setFormDisabled(true);
            return;
        }

        TxtFolio.setText(safe(sale.getFolio()));
        TxtVehicleId.setText(
                sale.getVehicleId() == null ? "" : String.valueOf(sale.getVehicleId()));
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

        try {
            if (sale.getVehicleId() != null) {
                VehicleDTO vehicle = vehicleDao.findVehicleById(sale.getVehicleId());
                fillVehicleFields(vehicle);
            } else {
                clearVehicleFields();
            }
        } catch (SQLException | IOException ex) {
            clearVehicleFields();
        }

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
            // Ignorado mientras escribe
        }
    }

    private void onVehicleIdChanged() {
        String text = TxtVehicleId.getText();
        if (text == null || text.trim().isEmpty()) {
            TxtSubtotal.clear();
            TxtTotal.clear();
            clearVehicleFields();
            return;
        }

        try {
            long vehicleId = parseLong(text, "ID vehículo");

            VehicleDTO vehicle;
            try {
                vehicle = vehicleDao.findVehicleById(vehicleId);
            } catch (SQLException | IOException ex) {
                showError("No se pudo consultar el vehículo: " + ex.getMessage());
                TxtSubtotal.clear();
                TxtTotal.clear();
                clearVehicleFields();
                return;
            }

            if (vehicle == null || vehicle.getVehicleId() == null) {
                showError("El vehículo con ID " + vehicleId + " no existe.");
                TxtSubtotal.clear();
                TxtTotal.clear();
                clearVehicleFields();
                return;
            }

            if (vehicle.getPrice() == null) {
                showError("El vehículo con ID " + vehicleId + " no tiene precio configurado.");
                TxtSubtotal.clear();
                TxtTotal.clear();
                clearVehicleFields();
                return;
            }

            fillVehicleFields(vehicle);
            TxtSubtotal.setText(vehicle.getPrice().toPlainString());
            recalcDetailTotal();

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            TxtSubtotal.clear();
            TxtTotal.clear();
            clearVehicleFields();
        }
    }

    private void setFormDisabled(boolean disabled) {

        TxtSubtotal.setDisable(true);
        TxtTotal.setDisable(true);
        TxtStatus.setDisable(true);
        TxtFolio.setDisable(true);

        if (TxtVehicleMake != null) TxtVehicleMake.setDisable(true);
        if (TxtVehicleModel != null) TxtVehicleModel.setDisable(true);
        if (TxtVehicleColor != null) TxtVehicleColor.setDisable(true);
        if (TxtVehicleYear != null) TxtVehicleYear.setDisable(true);

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
    private void onExportReport() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/sales/SalesReportView.fxml"));

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Impresión de reportes");
            stage.initModality(Modality.WINDOW_MODAL);

            if (contentArea != null && contentArea.getScene() != null) {
                stage.initOwner(contentArea.getScene().getWindow());
            }

            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException ex) {
            showError("Error al abrir la ventana de reportes: " + ex.getMessage());
        }
    }

    @FXML
    private void onClearForm() {
        TblSales.getSelectionModel().clearSelection();
        clearForm();
        setFormDisabled(true);
    }

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

        SalesReportRange selectedRange =
                (CmbReportRange == null || CmbReportRange.getValue() == null)
                        ? SalesReportRange.ALL
                        : CmbReportRange.getValue();

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
                .filter(s -> isSaleInSelectedRange(s, selectedRange))
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
        clearVehicleFields();
    }

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

    private boolean isSaleInSelectedRange(SaleDTO sale, SalesReportRange selectedRange) {
        if (selectedRange == null) {
            return true;
        }

        LocalDate today = LocalDate.now();
        LocalDate saleDate = sale.getCreatedAt() == null
                ? null
                : sale.getCreatedAt().toLocalDate();

        if (saleDate == null) {
            return selectedRange == SalesReportRange.ALL;
        }

        switch (selectedRange) {
            case ALL:
                return true;
            case TODAY:
                return saleDate.isEqual(today);
            case LAST_7_DAYS:
                LocalDate start = today.minusDays(LAST_DAYS_COUNT - 1L);
                return !saleDate.isBefore(start) && !saleDate.isAfter(today);
            case THIS_MONTH:
                return saleDate.getYear() == today.getYear()
                        && saleDate.getMonth() == today.getMonth();
            case THIS_YEAR:
                return saleDate.getYear() == today.getYear();
            default:
                return true;
        }
    }

    private String truncate(String value, int maxLength) {
        String safeValue = safe(value);
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        return safeValue.substring(0, maxLength - 3) + "...";
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
