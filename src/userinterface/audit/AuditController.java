package userinterface.audit;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import logic.DAO.AuditQueryDAO;
import logic.DTO.AuditLogEntryDTO;
import logic.DTO.InventoryMovementDTO;
import utilities.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AuditController {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private enum ViewMode {
        AUDITORIA,
        INVENTARIO
    }

    @FXML private Label lblTitle;
    @FXML private Label lblModeHint;

    @FXML private TextField txtBuscar;

    @FXML private Button btnVerAuditoria;
    @FXML private Button btnVerInventario;

    @FXML private TableView<AuditTableRow> tblData;

    @FXML private TableColumn<AuditTableRow, String> colId;
    @FXML private TableColumn<AuditTableRow, String> colActor;
    @FXML private TableColumn<AuditTableRow, String> colAction;
    @FXML private TableColumn<AuditTableRow, String> colEntity;
    @FXML private TableColumn<AuditTableRow, String> colCreatedAt;
    @FXML private TableColumn<AuditTableRow, String> colIp;
    @FXML private TableColumn<AuditTableRow, String> colSummary;

    private final AuditQueryDAO auditQueryDAO = new AuditQueryDAO();

    private final ObservableList<AuditTableRow> rows = FXCollections.observableArrayList();
    private FilteredList<AuditTableRow> filtered;

    private ViewMode mode = ViewMode.AUDITORIA;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn() || !SessionManager.isAdmin()) {
            mostrarInfo("Permiso denegado", "Solo el administrador puede ver auditoría.");
            disableAll();
            return;
        }

        configurarTabla();
        configurarEventos();
        setMode(ViewMode.AUDITORIA);
    }

    private void disableAll() {
        if (txtBuscar != null) txtBuscar.setDisable(true);
        if (btnVerAuditoria != null) btnVerAuditoria.setDisable(true);
        if (btnVerInventario != null) btnVerInventario.setDisable(true);
        if (tblData != null) tblData.setDisable(true);
    }

    private void configurarTabla() {
        colId.setCellValueFactory(d -> d.getValue().idProperty());
        colActor.setCellValueFactory(d -> d.getValue().actorPersonnelNumberProperty());
        colAction.setCellValueFactory(d -> d.getValue().actionProperty());
        colEntity.setCellValueFactory(d -> d.getValue().entityProperty());
        colCreatedAt.setCellValueFactory(d -> d.getValue().createdAtProperty());
        colIp.setCellValueFactory(d -> d.getValue().ipProperty());
        colSummary.setCellValueFactory(d -> d.getValue().summaryProperty());

        filtered = new FilteredList<>(rows, x -> true);
        tblData.setItems(filtered);
    }

    private void configurarEventos() {
        if (btnVerAuditoria != null) {
            btnVerAuditoria.setOnAction(e -> setMode(ViewMode.AUDITORIA));
        }
        if (btnVerInventario != null) {
            btnVerInventario.setOnAction(e -> setMode(ViewMode.INVENTARIO));
        }
        if (txtBuscar != null) {
            txtBuscar.textProperty().addListener((obs, o, n) -> aplicarFiltroTexto());
        }
    }

    private void setMode(ViewMode newMode) {
        mode = newMode;

        if (lblTitle != null) {
            lblTitle.setText(mode == ViewMode.AUDITORIA ? "Auditoría" : "Inventario");
        }
        if (lblModeHint != null) {
            lblModeHint.setText(mode == ViewMode.AUDITORIA
                    ? "Mostrando: auditoría"
                    : "Mostrando: inventario");
        }

        cargarDatos();
    }

    private void cargarDatos() {
        rows.clear();

        try {
            if (mode == ViewMode.AUDITORIA) {
                List<AuditLogEntryDTO> data = auditQueryDAO.getAllAuditLogs();
                for (AuditLogEntryDTO a : data) {
                    rows.add(AuditTableRow.fromAudit(a, DATE_TIME_FMT));
                }
            } else {
                List<InventoryMovementDTO> data = auditQueryDAO.getAllInventoryMovements();
                for (InventoryMovementDTO m : data) {
                    rows.add(AuditTableRow.fromMovement(m, DATE_TIME_FMT));
                }
            }

            aplicarFiltroTexto();

        } catch (Exception ex) {
            mostrarError("Error al cargar datos", ex.getMessage());
        }
    }

    private void aplicarFiltroTexto() {
        String search = txtBuscar == null || txtBuscar.getText() == null
                ? ""
                : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);

        filtered.setPredicate(r -> {
            if (search.isEmpty()) {
                return true;
            }

            return safe(r.getId()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(r.getActorPersonnelNumber()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(r.getAction()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(r.getEntity()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(r.getCreatedAt()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(r.getIp()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(r.getSummary()).toLowerCase(Locale.ROOT).contains(search);
        });
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private void mostrarInfo(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    private void mostrarError(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg == null ? "Ocurrió un error." : msg);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }
}
