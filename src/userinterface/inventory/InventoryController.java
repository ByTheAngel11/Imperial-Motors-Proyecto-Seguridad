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
import logic.DAO.VehicleDAO;
import logic.DTO.VehicleDTO;
import logic.DTO.VehicleStatus;
import java.io.IOException;
import java.sql.SQLException;
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

    private final ObservableList<VehicleTableRow> masterVehicles = FXCollections.observableArrayList();
    private final VehicleDAO vehicleDAO = new VehicleDAO();

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
                // Estilos INLINE para evitar líos con el CSS
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

                btnView.setOnAction(e -> {
                    VehicleTableRow row = getItem();
                    if (row != null) onViewVehicle(row.getVehicle());
                });

                btnEditRow.setOnAction(e -> {
                    VehicleTableRow row = getItem();
                    if (row != null) onEditVehicle(row.getVehicle());
                });

                btnDeleteRow.setOnAction(e -> {
                    VehicleTableRow row = getItem();
                    if (row != null) onDeleteVehicle(row.getVehicle());
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

    // ---------- Acciones de los botones de la columna Acciones ----------

    private void onViewVehicle(VehicleDTO v) {
        mostrarInfo("Ver vehículo",
                "Vehículo: " + safe(v.getMake()) + " " + safe(v.getModel()) +
                        "\nVIN: " + safe(v.getVin()));
    }

    private void onEditVehicle(VehicleDTO v) {
        mostrarInfo("Editar vehículo",
                "Aquí abrirías la ventana de edición para el vehículo ID " + v.getVehicleId());
    }

    private void onDeleteVehicle(VehicleDTO v) {
        mostrarInfo("Eliminar vehículo",
                "Aquí podrías pedir confirmación para dar de baja el vehículo ID " + v.getVehicleId());
    }

    public void onAddVehicle() {
        loadPage("/userinterface/vehicle/addVehicle.fxml");
    }

    // ---------- Helpers ----------

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
