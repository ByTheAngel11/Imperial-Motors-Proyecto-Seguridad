package userinterface.suppliers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import logic.DAO.SupplierDAO;
import logic.DTO.SupplierDTO;
import utilities.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SupplierController {

    @FXML
    private AnchorPane contentArea;

    @FXML private TextField TxtSupplierId;
    @FXML private TextField TxtLegalName;
    @FXML private TextField TxtRfc;
    @FXML private TextField TxtContactName;
    @FXML private TextField TxtPhone;
    @FXML private TextField TxtEmail;
    @FXML private TextField TxtStatus;

    @FXML private TextField TxtSearchSupplier;

    @FXML private TableView<SupplierDTO> TblSuppliers;
    @FXML private TableColumn<SupplierDTO, String> ColLegalName;
    @FXML private TableColumn<SupplierDTO, String> ColRfc;
    @FXML private TableColumn<SupplierDTO, String> ColContact;
    @FXML private TableColumn<SupplierDTO, String> ColPhone;
    @FXML private TableColumn<SupplierDTO, String> ColEmail;
    @FXML private TableColumn<SupplierDTO, String> ColActive;

    @FXML private Button BtnNewSupplier;
    @FXML private Button BtnSaveSupplier;
    @FXML private Button BtnDeleteSupplier;

    @FXML private ComboBox<String> CmbStatusFilter;

    private final SupplierDAO supplierDao = new SupplierDAO();
    private final ObservableList<SupplierDTO> allSuppliers = FXCollections.observableArrayList();

    private boolean isAdmin;

    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{1,10}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern RFC_PATTERN =
            Pattern.compile("^([A-ZÑ&]{3,4})\\d{6}[A-Z0-9]{3}$");

    @FXML
    private void initialize() {
        isAdmin = SessionManager.isAdmin();

        configureTableColumns();
        configureSelectionListener();
        configureSearchFilter();
        configureStatusFilter();

        clearForm();
        setFormDisabled(true);
        reloadSuppliersTable();
    }

    private void configureSearchFilter() {
        TxtSearchSupplier.textProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void configureTableColumns() {
        ColLegalName.setCellValueFactory(
                cell -> new SimpleStringProperty(safe(cell.getValue().getLegalName())));

        ColRfc.setCellValueFactory(
                cell -> new SimpleStringProperty(safe(cell.getValue().getRfc())));

        ColContact.setCellValueFactory(
                cell -> new SimpleStringProperty(safe(cell.getValue().getContactName())));

        ColPhone.setCellValueFactory(
                cell -> new SimpleStringProperty(safe(cell.getValue().getPhone())));

        ColEmail.setCellValueFactory(
                cell -> new SimpleStringProperty(safe(cell.getValue().getEmail())));

        ColActive.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        Boolean.TRUE.equals(cell.getValue().getActive()) ? "Activo" : "Inactivo"));
    }

    private void configureSelectionListener() {
        TblSuppliers.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> onSupplierSelected(newSel));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void onSupplierSelected(SupplierDTO supplier) {
        if (supplier == null) {
            clearForm();
            setFormDisabled(true);
            return;
        }

        TxtSupplierId.setText(
                supplier.getSupplierId() == null ? "" : supplier.getSupplierId().toString());
        TxtLegalName.setText(safe(supplier.getLegalName()));
        TxtRfc.setText(safe(supplier.getRfc()));
        TxtContactName.setText(safe(supplier.getContactName()));
        TxtPhone.setText(safe(supplier.getPhone()));
        TxtEmail.setText(safe(supplier.getEmail()));
        TxtStatus.setText(Boolean.TRUE.equals(supplier.getActive()) ? "Activo" : "Inactivo");

        setFormDisabled(!isAdmin);
    }
    private void configureStatusFilter() {
        if (CmbStatusFilter == null) {
            return;
        }

        CmbStatusFilter.getItems().setAll("Activos", "Inactivos", "Todos");
        CmbStatusFilter.getSelectionModel().select("Activos"); // comportamiento actual por defecto

        CmbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void setFormDisabled(boolean disabled) {
        TxtSupplierId.setDisable(true);
        TxtStatus.setDisable(true);

        TxtLegalName.setDisable(disabled);
        TxtRfc.setDisable(disabled);
        TxtContactName.setDisable(disabled);
        TxtPhone.setDisable(disabled);
        TxtEmail.setDisable(disabled);

        BtnSaveSupplier.setDisable(disabled);
        BtnDeleteSupplier.setDisable(disabled);
    }

    @FXML
    private void onNewSupplier() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/suppliers/SupplierFormView.fxml"));

            AnchorPane form = loader.load();
            SupplierFormController controller = loader.getController();

            controller.setOnSaveCallback(this::reloadSuppliersTable);
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
                    getClass().getResource("/userinterface/suppliers/SupplierView.fxml"));

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
    private void onSaveSupplier() {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión.");
                return;
            }

            if (!SessionManager.isAdmin()) {
                showError("Solo el administrador puede modificar proveedores.");
                return;
            }

            SupplierDTO selected = TblSuppliers.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Selecciona un proveedor.");
                return;
            }

            Long supplierId = selected.getSupplierId();

            String legalName = requireNotBlank(TxtLegalName.getText(), "Razón social");
            String contactName = requireNotBlank(TxtContactName.getText(), "Nombre contacto");
            String phone = requireNotBlank(TxtPhone.getText(), "Teléfono");
            validatePhone(phone);

            String rfc = normalizeOptionalRfc(TxtRfc.getText());
            String email = emptyToNull(TxtEmail.getText());
            if (email != null) {
                validateEmail(email);
            }

            // Unicidad
            if (supplierDao.existsPhone(phone, supplierId)) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese teléfono.");
            }
            if (email != null && supplierDao.existsEmail(email, supplierId)) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese correo.");
            }
            if (rfc != null && supplierDao.existsRfc(rfc, supplierId)) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese RFC.");
            }

            SupplierDTO supplier = new SupplierDTO();
            supplier.setSupplierId(supplierId);
            supplier.setLegalName(legalName);
            supplier.setRfc(rfc);
            supplier.setContactName(contactName);
            supplier.setPhone(phone);
            supplier.setEmail(email);
            supplier.setActive(selected.getActive());

            supplierDao.updateSupplier(supplier);

            showInfo("Proveedor actualizado correctamente.");
            reloadSuppliersTable();

        } catch (Exception ex) {
            showError("Error al guardar el proveedor: " + ex.getMessage());
        }
    }

    @FXML
    private void onDeleteSupplier() {
        try {
            if (!SessionManager.isLoggedIn()) {
                showError("Debes iniciar sesión.");
                return;
            }

            if (!SessionManager.isAdmin()) {
                showError("Solo el administrador puede eliminar proveedores.");
                return;
            }

            SupplierDTO selected = TblSuppliers.getSelectionModel().getSelectedItem();
            if (selected == null || selected.getSupplierId() == null) {
                showError("Selecciona un proveedor.");
                return;
            }

            if (Boolean.FALSE.equals(selected.getActive())) {
                showInfo("El proveedor ya está inactivo.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Eliminar proveedor");
            confirm.setHeaderText(null);
            confirm.setContentText(
                    "¿Deseas eliminar al proveedor \"" + safe(selected.getLegalName()) + "\"?");

            var result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            supplierDao.logicalDeleteSupplier(selected.getSupplierId());

            showInfo("Proveedor eliminado correctamente.");
            reloadSuppliersTable();

        } catch (Exception ex) {
            showError("Error al eliminar el proveedor: " + ex.getMessage());
        }
    }

    private void reloadSuppliersTable() {
        try {
            List<SupplierDTO> suppliers = supplierDao.getAllSuppliers(); // antes: getAllActiveSuppliers()
            allSuppliers.setAll(suppliers);
            applyFilter();
        } catch (SQLException ex) {
            showError("No se pudieron cargar los proveedores:\n" + ex.getMessage());
        }
    }

    private void applyFilter() {
        String sText = TxtSearchSupplier.getText();
        final String search = (sText == null) ? "" : sText.trim().toLowerCase(Locale.ROOT);

        String statusFilter = (CmbStatusFilter == null) ? "Activos" : CmbStatusFilter.getValue();
        if (statusFilter == null || statusFilter.isBlank()) {
            statusFilter = "Activos";
        }

        final String finalStatusFilter = statusFilter;

        List<SupplierDTO> filtered = allSuppliers.stream()
                // filtro por estado
                .filter(s -> {
                    boolean isActive = Boolean.TRUE.equals(s.getActive());

                    switch (finalStatusFilter) {
                        case "Activos":
                            return isActive;
                        case "Inactivos":
                            return !isActive;
                        case "Todos":
                        default:
                            return true;
                    }
                })
                .filter(s -> {
                    if (search.isEmpty()) {
                        return true;
                    }

                    String legal = safe(s.getLegalName()).toLowerCase(Locale.ROOT);
                    String rfc = safe(s.getRfc()).toLowerCase(Locale.ROOT);
                    String contact = safe(s.getContactName()).toLowerCase(Locale.ROOT);
                    String phone = safe(s.getPhone()).toLowerCase(Locale.ROOT);
                    String email = safe(s.getEmail()).toLowerCase(Locale.ROOT);

                    return legal.contains(search)
                            || rfc.contains(search)
                            || contact.contains(search)
                            || phone.contains(search)
                            || email.contains(search);
                })
                .collect(Collectors.toList());

        TblSuppliers.setItems(FXCollections.observableArrayList(filtered));
    }

    private void clearForm() {
        TxtSupplierId.clear();
        TxtLegalName.clear();
        TxtRfc.clear();
        TxtContactName.clear();
        TxtPhone.clear();
        TxtEmail.clear();
        TxtStatus.clear();
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireNotBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo " + field + " es obligatorio.");
        }
        return value.trim();
    }

    private void validatePhone(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException(
                    "El teléfono debe contener solo dígitos y máximo 10 números.");
        }
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("El correo electrónico no tiene un formato válido.");
        }
    }

    private String normalizeOptionalRfc(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!RFC_PATTERN.matcher(upper).matches()) {
            throw new IllegalArgumentException("El RFC no tiene un formato válido.");
        }
        return upper;
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
