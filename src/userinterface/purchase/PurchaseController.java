package userinterface.purchase;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import logic.DAO.PurchaseOrderDAO;
import logic.DTO.PurchaseOrderDTO;
import logic.DTO.PurchaseStatus;
import utilities.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;

public class PurchaseController {

    @FXML private AnchorPane contentArea;

    @FXML private TextField TxtSearchPurchase;
    @FXML private ComboBox<PurchaseStatus> CmbStatusFilter;

    @FXML private TableView<PurchaseOrderDTO> TblPurchases;
    @FXML private TableColumn<PurchaseOrderDTO, String> ColId;
    @FXML private TableColumn<PurchaseOrderDTO, String> ColSupplier;
    @FXML private TableColumn<PurchaseOrderDTO, String> ColTotal;
    @FXML private TableColumn<PurchaseOrderDTO, PurchaseStatus> ColStatus;
    @FXML private TableColumn<PurchaseOrderDTO, String> ColCreatedAt;

    @FXML private TextField TxtPurchaseId;
    @FXML private TextField TxtSupplierId;
    @FXML private TextField TxtAccountId;
    @FXML private DatePicker DpExpectedDate;
    @FXML private TextField TxtSubtotal;
    @FXML private TextField TxtTotal;
    @FXML private TextField TxtStatus;

    @FXML private Button BtnNewPurchase;
    @FXML private Button BtnMarkReceived;
    @FXML private Button BtnCancelPurchase;
    @FXML private Button BtnSaveChanges;

    private final PurchaseOrderDAO purchaseOrderDao = new PurchaseOrderDAO();
    private final ObservableList<PurchaseOrderDTO> allPurchases = FXCollections.observableArrayList();

    private boolean isAdmin;
    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {

        isAdmin = SessionManager.isAdmin();

        configureStatusFilter();
        configureTableColumns();
        configureSelectionListener();
        configureSearchFilter();

        reloadPurchasesTable();
        clearForm();
        setDetailDisabled(true);
    }

    private void configureStatusFilter() {
        CmbStatusFilter.getItems().setAll(PurchaseStatus.values());
        CmbStatusFilter.setPromptText("Estado de la compra");
        CmbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void configureSearchFilter() {
        TxtSearchPurchase.textProperty().addListener((obs, o, n) -> applyFilter());

        // Bloquear fechas anteriores a hoy
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

    private void configureTableColumns() {
        ColId.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue().getPurchaseId() == null
                                ? ""
                                : cell.getValue().getPurchaseId().toString()));

        ColSupplier.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue().getSupplierName() == null
                                ? ""
                                : cell.getValue().getSupplierName()));

        ColTotal.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        toPlain(cell.getValue().getTotal())));

        ColStatus.setCellValueFactory(
                cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));

        ColCreatedAt.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        cell.getValue().getCreatedAt() == null
                                ? ""
                                : cell.getValue().getCreatedAt().format(dateTimeFormatter)));
    }

    private void configureSelectionListener() {
        TblPurchases.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> onPurchaseSelected(newSel));
    }

    private void onPurchaseSelected(PurchaseOrderDTO order) {
        if (order == null) {
            clearForm();
            setDetailDisabled(true);
            return;
        }

        TxtPurchaseId.setText(order.getPurchaseId() == null ? "" : order.getPurchaseId().toString());
        TxtSupplierId.setText(order.getSupplierId() == null ? "" : order.getSupplierId().toString());
        TxtAccountId.setText(order.getAccountId() == null ? "" : order.getAccountId().toString());

        if (order.getExpectedDate() != null) {
            DpExpectedDate.setValue(order.getExpectedDate());
        } else {
            DpExpectedDate.setValue(null);
        }

        TxtSubtotal.setText(toPlain(order.getSubtotal()));
        TxtTotal.setText(toPlain(order.getTotal()));
        TxtStatus.setText(order.getStatus() == null ? "" : order.getStatus().name());

        setDetailDisabled(false);
    }

    private void setDetailDisabled(boolean disabled) {

        TxtPurchaseId.setDisable(true);
        TxtAccountId.setDisable(true);
        TxtStatus.setDisable(true);
        TxtTotal.setDisable(true);

        boolean canEdit = !disabled && isAdmin;

        TxtSupplierId.setDisable(!canEdit);
        DpExpectedDate.setDisable(!canEdit);
        TxtSubtotal.setDisable(!canEdit);

        BtnMarkReceived.setDisable(!isAdmin || disabled);
        BtnCancelPurchase.setDisable(!isAdmin || disabled);
        BtnSaveChanges.setDisable(!canEdit);
    }

    private void reloadPurchasesTable() {
        try {
            List<PurchaseOrderDTO> purchases = purchaseOrderDao.getAllPurchases();
            allPurchases.setAll(purchases);
            applyFilter();
        } catch (SQLException ex) {
            showError("No se pudieron cargar las compras:\n" + ex.getMessage());
        }
    }

    private void applyFilter() {
        PurchaseStatus filterStatus = CmbStatusFilter.getValue();
        String sText = TxtSearchPurchase.getText();
        final String search = (sText == null) ? "" : sText.trim().toLowerCase(Locale.ROOT);

        List<PurchaseOrderDTO> filtered = allPurchases.stream()
                .filter(p -> filterStatus == null || p.getStatus() == filterStatus)
                .filter(p -> {
                    if (search.isEmpty()) {
                        return true;
                    }
                    String id = p.getPurchaseId() == null ? "" : p.getPurchaseId().toString();
                    String supplier = p.getSupplierId() == null ? "" : p.getSupplierId().toString();
                    return id.toLowerCase(Locale.ROOT).contains(search)
                            || supplier.toLowerCase(Locale.ROOT).contains(search);
                })
                .collect(Collectors.toList());

        TblPurchases.setItems(FXCollections.observableArrayList(filtered));
    }

    private void clearForm() {
        TxtPurchaseId.clear();
        TxtSupplierId.clear();
        TxtAccountId.clear();
        DpExpectedDate.setValue(null);
        TxtSubtotal.clear();
        TxtTotal.clear();
        TxtStatus.clear();
    }

    // =========================
    //  Abrir formulario embebido
    // =========================
    @FXML
    private void onNewPurchase() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/purchase/PurchaseFormView.fxml"));

            AnchorPane form = loader.load();
            PurchaseFormController controller = loader.getController();

            controller.setOnSaveCallback(this::reloadPurchasesTable);
            controller.setOnCloseCallback(this::showMainView);

            contentArea.getChildren().setAll(form);
            AnchorPane.setTopAnchor(form, 0.0);
            AnchorPane.setBottomAnchor(form, 0.0);
            AnchorPane.setLeftAnchor(form, 0.0);
            AnchorPane.setRightAnchor(form, 0.0);

        } catch (IOException ex) {
            showError("Error al cargar formulario de compras: " + ex.getMessage());
        }
    }

    // =======================
    //  Regresar a vista lista
    // =======================
    private void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/purchase/PurchaseView.fxml"));

            AnchorPane main = loader.load();
            contentArea.getChildren().setAll(main);

            AnchorPane.setTopAnchor(main, 0.0);
            AnchorPane.setBottomAnchor(main, 0.0);
            AnchorPane.setLeftAnchor(main, 0.0);
            AnchorPane.setRightAnchor(main, 0.0);

        } catch (IOException ex) {
            showError("Error al regresar a la pantalla de compras: " + ex.getMessage());
        }
    }

    // =======================
    //  Acciones admin
    // =======================
    @FXML
    private void onMarkReceived() {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión.");
                return;
            }

            if (!SessionManager.isAdmin()) {
                showError("Solo el administrador puede marcar compras como recibidas.");
                return;
            }

            PurchaseOrderDTO selected = TblPurchases.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Selecciona una compra.");
                return;
            }

            if (selected.getStatus() == PurchaseStatus.RECIBIDA) {
                showInfo("La compra ya está marcada como recibida.");
                return;
            }

            purchaseOrderDao.markAsReceived(
                    selected.getPurchaseId(),
                    SessionManager.getCurrentAccountId());

            showInfo("Compra marcada como recibida y vehículos dados de alta en inventario.");
            reloadPurchasesTable();

        } catch (Exception ex) {
            showError("No se pudo marcar como recibida: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancelPurchase() {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión.");
                return;
            }

            if (!SessionManager.isAdmin()) {
                showError("Solo el administrador puede cancelar compras.");
                return;
            }

            PurchaseOrderDTO selected = TblPurchases.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Selecciona una compra.");
                return;
            }

            if (selected.getStatus() == PurchaseStatus.CANCELADA) {
                showInfo("La compra ya está cancelada.");
                return;
            }
            if (selected.getStatus() == PurchaseStatus.RECIBIDA) {
                showError("Una compra ya recibida no se puede cancelar");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Cancelar compra");
            confirm.setHeaderText(null);
            confirm.setContentText(
                    "¿Deseas cancelar la compra con ID " + selected.getPurchaseId() + "?");

            var result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            String reason = "Cancelada desde interfaz";
            purchaseOrderDao.cancelOrder(
                    selected.getPurchaseId(),
                    SessionManager.getCurrentAccountId(),
                    reason);

            showInfo("Compra cancelada correctamente.");
            reloadPurchasesTable();

        } catch (Exception ex) {
            showError("No se pudo cancelar la compra: " + ex.getMessage());
        }
    }

    @FXML
    private void onSaveChanges() {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión.");
                return;
            }

            if (!SessionManager.isAdmin()) {
                showError("Solo el administrador puede modificar compras.");
                return;
            }

            PurchaseOrderDTO selected = TblPurchases.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Selecciona una compra.");
                return;
            }
            if (selected.getStatus() == PurchaseStatus.RECIBIDA) {
                showError("No puedes modificar una compra ya recibida");
                return;
            }
            if (selected.getStatus() == PurchaseStatus.CANCELADA) {
                showError("No puedes modificar una compra ya cancelada");
                return;
            }
            String supplierText = TxtSupplierId.getText();
            if (supplierText == null || supplierText.trim().isEmpty()) {
                showError("El ID de proveedor no puede estar vacío.");
                return;
            }
            Long supplierId;
            try {
                supplierId = Long.parseLong(supplierText.trim());
            } catch (NumberFormatException ex) {
                showError("El ID de proveedor debe ser numérico.");
                return;
            }

            // Validar fecha esperada
            LocalDate expected = DpExpectedDate.getValue();
            if (expected == null) {
                showError("La fecha esperada es obligatoria.");
                return;
            }
            if (expected.isBefore(LocalDate.now())) {
                showError("La fecha esperada no puede ser anterior a hoy.");
                return;
            }

            // Validar subtotal
            String subtotalText = TxtSubtotal.getText();
            if (subtotalText == null || subtotalText.trim().isEmpty()) {
                showError("El subtotal no puede estar vacío.");
                return;
            }
            BigDecimal subtotal;
            try {
                subtotal = new BigDecimal(subtotalText.trim());
            } catch (NumberFormatException ex) {
                showError("El subtotal debe ser un número válido.");
                return;
            }

            // Aplicar cambios al DTO seleccionado
            selected.setSupplierId(supplierId);
            selected.setExpectedDate(expected);
            selected.setSubtotal(subtotal);

            long actorAccountId = SessionManager.getCurrentAccountId();
            purchaseOrderDao.updatePurchase(selected, actorAccountId);

            showInfo("Cambios guardados correctamente.");
            reloadPurchasesTable();

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError(mapDatabaseError(ex));
        } catch (Exception ex) {
            showError("Ocurrió un error inesperado al guardar los cambios.");
        }
    }

    // ======================
    //  Helpers
    // ======================

    private String toPlain(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
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
                return "El vehículo especificado no existe. Verifica el ID de vehículo.";
            }
        }

        return "Error al guardar en la base de datos. Verifica los datos e inténtalo de nuevo.";
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
