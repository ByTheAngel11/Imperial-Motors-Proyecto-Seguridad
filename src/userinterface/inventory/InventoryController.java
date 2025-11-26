package userinterface.inventory;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import logic.DAO.InventoryMovementDAO;
import logic.DAO.VehicleDAO;
import logic.DTO.InventoryMovementDTO;
import logic.DTO.InventoryMovementType;
import logic.DTO.VehicleDTO;
import logic.DTO.VehicleStatus;
import logic.DTO.AccountRole;
import utilities.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class InventoryController {

    @FXML private TableView<VehicleTableRow> tblVehicle;
    @FXML private TableColumn<VehicleTableRow, String> colId;
    @FXML private TableColumn<VehicleTableRow, String> colMake;
    @FXML private TableColumn<VehicleTableRow, String> colModel;
    @FXML private TableColumn<VehicleTableRow, String> colYear;
    @FXML private TableColumn<VehicleTableRow, String> colVin;
    @FXML private TableColumn<VehicleTableRow, String> colPrice;
    @FXML private TableColumn<VehicleTableRow, String> colStatus;
    @FXML private TableColumn<VehicleTableRow, VehicleTableRow> colActions;

    @FXML private AnchorPane contentArea;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private ComboBox<String> cmbBrandFilter;
    @FXML private Button btnRefresh;

    private final ObservableList<VehicleTableRow> masterVehicles = FXCollections.observableArrayList();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final InventoryMovementDAO inventoryMovementDAO = new InventoryMovementDAO();

    @FXML
    public void initialize() {
        configurarTabla();

        if (cmbStatusFilter.getValue() == null) {
            cmbStatusFilter.setValue("Todos los estados");
        }
        if (cmbBrandFilter.getValue() == null) {
            cmbBrandFilter.setValue("Todas las marcas");
        }

        cargarVehiculos();
        aplicarFiltros();

        txtSearch.textProperty().addListener((obs, ov, nv) -> aplicarFiltros());
        cmbBrandFilter.getSelectionModel().selectedItemProperty()
                .addListener((obs, ov, nv) -> aplicarFiltros());
        cmbStatusFilter.getSelectionModel().selectedItemProperty()
                .addListener((obs, ov, nv) -> {
                    cargarVehiculos();
                    aplicarFiltros();
                });

        configurarAccesoPorRol();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(d -> d.getValue().idProperty());
        colMake.setCellValueFactory(d -> d.getValue().makeProperty());
        colModel.setCellValueFactory(d -> d.getValue().modelProperty());
        colYear.setCellValueFactory(d -> d.getValue().yearProperty());
        colVin.setCellValueFactory(d -> d.getValue().vinProperty());
        colPrice.setCellValueFactory(d -> d.getValue().priceProperty());
        colStatus.setCellValueFactory(d -> d.getValue().statusProperty());

        // ---------- Píldora de estado ----------
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();

            {
                pill.getStyleClass().add("inv-status-pill");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                pill.setText(formatStatus(item));
                pill.getStyleClass().removeAll(
                        "status-disponible",
                        "status-reservado",
                        "status-vendido",
                        "status-baja"
                );

                switch (item.toUpperCase()) {
                    case "DISPONIBLE" -> pill.getStyleClass().add("status-disponible");
                    case "RESERVADO"  -> pill.getStyleClass().add("status-reservado");
                    case "VENDIDO"    -> pill.getStyleClass().add("status-vendido");
                    case "BAJA"       -> pill.getStyleClass().add("status-baja");
                    default -> { }
                }

                setGraphic(pill);
            }
        });

        // ---------- Columna ACCIONES (botones) ----------
        colActions.setCellValueFactory(param ->
                new ReadOnlyObjectWrapper<>(param.getValue()));

        colActions.setCellFactory(col -> new TableCell<>() {

            private final Button btnView      = new Button("👁");
            private final Button btnEditRow   = new Button("✎");
            private final Button btnDeleteRow = new Button("🗑");
            private final HBox box = new HBox(8, btnView, btnEditRow, btnDeleteRow);

            {
                String baseStyle =
                        "-fx-background-color: transparent;" +
                                "-fx-border-color: transparent;" +
                                "-fx-padding: 4 6;" +
                                "-fx-min-width: 26;" +
                                "-fx-min-height: 26;" +
                                "-fx-font-size: 14;" +
                                "-fx-cursor: hand;";

                btnView.setStyle(baseStyle);
                btnEditRow.setStyle(baseStyle);
                btnDeleteRow.setStyle(baseStyle + "-fx-text-fill: #DC2626;");

                boolean isAdmin = isCurrentUserAdmin();
                if (!isAdmin) {
                    // Vendedor: solo puede ver, no editar ni dar de baja
                    btnEditRow.setVisible(false);
                    btnEditRow.setManaged(false);
                    btnDeleteRow.setVisible(false);
                    btnDeleteRow.setManaged(false);
                }

                btnView.setOnAction(e -> {
                    VehicleTableRow row = getItem();
                    if (row != null) {
                        onViewVehicle(row.getVehicle());
                    }
                });

                btnEditRow.setOnAction(e -> {
                    VehicleTableRow row = getItem();
                    if (row != null) {
                        onEditVehicle(row.getVehicle());
                    }
                });

                btnDeleteRow.setOnAction(e -> {
                    VehicleTableRow row = getItem();
                    if (row != null) {
                        onDeleteVehicle(row.getVehicle());
                    }
                });
            }

            @Override
            protected void updateItem(VehicleTableRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });

        tblVehicle.setItems(FXCollections.observableArrayList());
    }

    private void cargarVehiculos() {
        masterVehicles.clear();

        try {
            String statusStr = cmbStatusFilter.getValue();
            List<VehicleDTO> dtoList;

            if (statusStr == null || statusStr.equals("Todos los estados")) {
                dtoList = vehicleDAO.getAllVehicles();
            } else {
                VehicleStatus status = VehicleStatus.valueOf(statusStr.toUpperCase());
                dtoList = vehicleDAO.getVehiclesByStatus(status);
            }

            List<VehicleTableRow> rows = dtoList.stream()
                    .map(VehicleTableRow::new)
                    .collect(Collectors.toList());

            masterVehicles.setAll(rows);
            actualizarOpcionesMarca();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            mostrarError("Error al cargar vehículos", e.toString());
        } catch (IllegalArgumentException e) {
            mostrarError("Estado inválido", "El estado seleccionado no es válido.");
        }
    }

    private void actualizarOpcionesMarca() {
        TreeSet<String> marcas = new TreeSet<>();
        for (VehicleTableRow row : masterVehicles) {
            String m = row.getMake();
            if (m != null && !m.isBlank() && !m.equals("-")) {
                marcas.add(m);
            }
        }

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Todas las marcas");
        items.addAll(marcas);

        cmbBrandFilter.setItems(items);

        if (!items.contains(cmbBrandFilter.getValue())) {
            cmbBrandFilter.setValue("Todas las marcas");
        }
    }

    private void aplicarFiltros() {
        String texto = txtSearch.getText();
        String marca = cmbBrandFilter.getValue();

        List<VehicleTableRow> filtrados = masterVehicles.stream()
                .filter(row -> {
                    boolean okSearch = true;
                    if (texto != null && !texto.isBlank()) {
                        String f = texto.toLowerCase();
                        okSearch =
                                contains(row.getMake(), f) ||
                                        contains(row.getModel(), f) ||
                                        contains(row.getVin(), f);
                    }

                    boolean okBrand = true;
                    if (marca != null && !marca.equals("Todas las marcas")) {
                        okBrand = marca.equalsIgnoreCase(row.getMake());
                    }

                    return okSearch && okBrand;
                })
                .collect(Collectors.toList());

        tblVehicle.setItems(FXCollections.observableArrayList(filtrados));
    }


    private void onViewVehicle(VehicleDTO v) {
        String id        = v.getVehicleId()  != null ? v.getVehicleId().toString() : "-";
        String make      = safe(v.getMake());
        String model     = safe(v.getModel());
        String year      = v.getModelYear()  != null ? v.getModelYear().toString() : "-";
        String color     = safe(v.getColor());
        String vin       = safe(v.getVin());
        String mileage   = v.getMileageKm()  != null ? v.getMileageKm() + " km" : "-";
        String price     = (v.getPrice()     != null ? "$" + v.getPrice().toPlainString() : "-");
        String status    = v.getStatus()     != null ? v.getStatus().name() : "-";

        String mensaje =
                "ID: " + id + "\n" +
                        "Marca: " + make + "\n" +
                        "Modelo: " + model + "\n" +
                        "Año: " + year + "\n" +
                        "Color: " + color + "\n" +
                        "VIN: " + vin + "\n" +
                        "Kilometraje: " + mileage + "\n" +
                        "Precio: " + price + "\n" +
                        "Estado: " + status;

        mostrarInfo("Ver vehículo", mensaje);
    }

    private void onEditVehicle(VehicleDTO v) {
        if (!isCurrentUserAdmin()) {
            mostrarError("Acceso denegado",
                    "Solo el administrador puede editar datos de inventario.");
            return;
        }

        if (v.getStatus() == VehicleStatus.VENDIDO || v.getStatus() == VehicleStatus.BAJA) {
            mostrarError("Operación no permitida",
                    "No puedes editar vehículos vendidos o dados de baja.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/inventory/VehicleEditFormView.fxml"));

            AnchorPane form = loader.load();
            VehicleEditFormController controller = loader.getController();

            controller.setVehicleToEdit(v);
            controller.setOnSaveCallback(() -> {
                cargarVehiculos();
                aplicarFiltros();
                showMainView();
            });
            controller.setOnCloseCallback(this::showMainView);

            contentArea.getChildren().setAll(form);
            AnchorPane.setTopAnchor(form, 0.0);
            AnchorPane.setBottomAnchor(form, 0.0);
            AnchorPane.setLeftAnchor(form, 0.0);
            AnchorPane.setRightAnchor(form, 0.0);

        } catch (IOException ex) {
            mostrarError("Error al cargar formulario de edición", ex.getMessage());
        }
    }

    private void onDeleteVehicle(VehicleDTO v) {
        if (!isCurrentUserAdmin()) {
            mostrarError("Acceso denegado",
                    "Solo el administrador puede dar de baja vehículos del inventario.");
            return;
        }

        if (v.getVehicleId() == null) {
            mostrarError("Vehículo inválido", "No se puede dar de baja un vehículo sin ID.");
            return;
        }
        if (v.getStatus() == VehicleStatus.VENDIDO) {
            mostrarError("Operación no permitida",
                    "No puedes dar de baja un vehículo que ya fue vendido.");
            return;
        }
        if (v.getStatus() == VehicleStatus.BAJA) {
            mostrarError("Operación no permitida",
                    "El vehículo ya está dado de baja.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Dar de baja vehículo");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Seguro que deseas dar de baja el vehículo:\n" +
                safe(v.getMake()) + " " + safe(v.getModel()) + "\nVIN: " + safe(v.getVin()) + "?");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    marcarVehiculoComoBaja(v);
                    mostrarInfo("Vehículo dado de baja",
                            "El vehículo se marcó como BAJA y se registró el movimiento de inventario.");
                    cargarVehiculos();
                    aplicarFiltros();
                } catch (SQLException | IOException ex) {
                    mostrarError("Error al dar de baja",
                            "Ocurrió un error al dar de baja el vehículo:\n" + ex.getMessage());
                }
            }
        });
    }

    private void marcarVehiculoComoBaja(VehicleDTO v) throws SQLException, IOException {
        Long vehicleId = v.getVehicleId();
        if (vehicleId == null) {
            throw new IllegalArgumentException("vehicleId requerido para marcar BAJA.");
        }

        vehicleDAO.updateVehicleStatus(vehicleId, VehicleStatus.BAJA);

        InventoryMovementDTO movement = new InventoryMovementDTO();
        movement.setVehicleId(vehicleId);
        movement.setType(InventoryMovementType.BAJA);
        movement.setRefTable("vehicle");
        movement.setRefId(vehicleId);
        movement.setNote("Baja desde módulo de inventario.");
        movement.setAccountId(SessionManager.getCurrentAccountId());
        movement.setCreatedAt(LocalDateTime.now());

        inventoryMovementDAO.insertInventoryMovement(movement);
    }

    // ---------- Botón Refrescar ----------

    @FXML
    private void onRefresh() {
        cargarVehiculos();
        aplicarFiltros();
    }

    // ---------- Helpers ----------

    private void configurarAccesoPorRol() {
        AccountRole role = SessionManager.getCurrentRole();
        if (role == null) {
            return;
        }

        if (!isCurrentUserAdmin()) {
            colActions.setText("Detalles");
        }
    }

    private boolean isCurrentUserAdmin() {
        AccountRole role = SessionManager.getCurrentRole();
        if (role == null) {
            return false;
        }

        return role == AccountRole.ADMINISTRATOR;
    }

    private void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/inventory/Inventory.fxml"));

            AnchorPane main = loader.load();
            contentArea.getChildren().setAll(main);

            AnchorPane.setTopAnchor(main, 0.0);
            AnchorPane.setBottomAnchor(main, 0.0);
            AnchorPane.setLeftAnchor(main, 0.0);
            AnchorPane.setRightAnchor(main, 0.0);

        } catch (IOException ex) {
            mostrarError("Error al regresar a la vista de inventario", ex.getMessage());
        }
    }

    private String formatStatus(String raw) {
        if (raw == null) return "-";

        return switch (raw.toUpperCase()) {
            case "DISPONIBLE" -> "Disponible";
            case "RESERVADO"  -> "Reservado";
            case "VENDIDO"    -> "Vendido";
            case "BAJA"       -> "Baja";
            default           -> raw;
        };
    }

    private boolean contains(String value, String filtro) {
        if (value == null) return false;
        return value.toLowerCase().contains(filtro);
    }

    private String safe(String s) {
        return s != null ? s : "-";
    }

    private void mostrarInfo(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void mostrarError(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
