package userinterface.users;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import logic.DAO.UserDAO;
import logic.DAO.UserManagementDAO;
import logic.DTO.UserAccountDTO;
import utilities.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class UsersController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String RESOURCE_USER_FORM = "/userinterface/users/UserFormView.fxml";

    @FXML private AnchorPane contentArea;

    @FXML private TableView<UserTableRow> tblUsuarios;

    @FXML private TableColumn<UserTableRow, String> colAccountId;
    @FXML private TableColumn<UserTableRow, String> colPersonnelNumber;

    @FXML private TableColumn<UserTableRow, String> colUsername;
    @FXML private TableColumn<UserTableRow, String> colFullName;
    @FXML private TableColumn<UserTableRow, String> colEmail;
    @FXML private TableColumn<UserTableRow, String> colRole;
    @FXML private TableColumn<UserTableRow, String> colActivo;
    @FXML private TableColumn<UserTableRow, String> colCreated;

    @FXML private TextField txtBuscar;

    @FXML private Button btnNuevo;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    private final ObservableList<UserTableRow> usuarios = FXCollections.observableArrayList();
    private FilteredList<UserTableRow> filtered;

    private List<UserAccountDTO> cacheJoin = List.of();

    private final UserDAO userDAO = new UserDAO();
    private final UserManagementDAO userManagementDAO = new UserManagementDAO();

    private Node usersViewSnapshot;

    @FXML
    public void initialize() {
        usersViewSnapshot = snapshotCurrentContent();

        configurarTabla();
        configurarPermisos();
        configurarBusqueda();
        cargarUsuarios();

        if (btnNuevo != null) btnNuevo.setOnAction(e -> onNuevo());
        if (btnEditar != null) btnEditar.setOnAction(e -> onEditar());
        if (btnEliminar != null) btnEliminar.setOnAction(e -> onEliminar());
    }

    private Node snapshotCurrentContent() {
        if (contentArea == null || contentArea.getChildren().isEmpty()) {
            return null;
        }
        return contentArea.getChildren().get(0);
    }

    private void restoreUsersViewSnapshot() {
        if (contentArea == null) {
            return;
        }
        if (usersViewSnapshot != null) {
            contentArea.getChildren().setAll(usersViewSnapshot);
            AnchorPane.setTopAnchor(usersViewSnapshot, 0.0);
            AnchorPane.setRightAnchor(usersViewSnapshot, 0.0);
            AnchorPane.setBottomAnchor(usersViewSnapshot, 0.0);
            AnchorPane.setLeftAnchor(usersViewSnapshot, 0.0);
        }
    }

    private void configurarPermisos() {
        boolean isAdmin = SessionManager.isAdmin();
        if (btnNuevo != null) btnNuevo.setDisable(!isAdmin);
        if (btnEditar != null) btnEditar.setDisable(!isAdmin);
        if (btnEliminar != null) btnEliminar.setDisable(!isAdmin);
    }

    private void configurarTabla() {
        if (colAccountId != null) {
            colAccountId.setCellValueFactory(d -> d.getValue().accountIdProperty());
        }
        if (colPersonnelNumber != null) {
            colPersonnelNumber.setCellValueFactory(d -> d.getValue().personnelNumberProperty());
        }

        colUsername.setCellValueFactory(d -> d.getValue().usernameProperty());
        colFullName.setCellValueFactory(d -> d.getValue().fullNameProperty());
        colEmail.setCellValueFactory(d -> d.getValue().emailProperty());
        colRole.setCellValueFactory(d -> d.getValue().roleProperty());
        colActivo.setCellValueFactory(d -> d.getValue().activoProperty());
        colCreated.setCellValueFactory(d -> d.getValue().createdAtProperty());

        filtered = new FilteredList<>(usuarios, x -> true);
        tblUsuarios.setItems(filtered);
    }

    private void configurarBusqueda() {
        if (txtBuscar != null) {
            txtBuscar.textProperty().addListener((obs, o, n) -> aplicarBusqueda());
        }
    }

    private void aplicarBusqueda() {
        String search = txtBuscar == null || txtBuscar.getText() == null
                ? ""
                : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);

        filtered.setPredicate(row -> {
            if (search.isEmpty()) {
                return true;
            }

            return safe(row.getAccountId()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(row.getPersonnelNumber()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(row.getUsername()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(row.getFullName()).toLowerCase(Locale.ROOT).contains(search)
                    || safe(row.getEmail()).toLowerCase(Locale.ROOT).contains(search);
        });
    }

    private void cargarUsuarios() {
        usuarios.clear();
        try {
            cacheJoin = userDAO.getAllUsersWithAccount();

            // Solo ACTivos
            for (UserAccountDTO u : cacheJoin) {
                if (u == null || !u.getIsActive()) {
                    continue;
                }

                usuarios.add(new UserTableRow(
                        u.getAccountId() == null ? "" : String.valueOf(u.getAccountId()),
                        safe(u.getPersonnelNumber()),
                        safe(u.getUsername()),
                        safe(u.getFullName()),
                        safe(u.getEmail()),
                        u.getRole() != null ? u.getRole().name() : "-",
                        "Sí",
                        u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : "-"
                ));
            }

            aplicarBusqueda();

        } catch (Exception ex) {
            mostrarError("Error al cargar usuarios", ex.getMessage());
        }
    }

    @FXML
    private void onNuevo() {
        if (!SessionManager.isAdmin()) {
            mostrarInfo("Permiso denegado", "Solo el administrador puede crear usuarios.");
            return;
        }
        openUserFormCreate();
    }

    @FXML
    private void onEditar() {
        if (!SessionManager.isAdmin()) {
            mostrarInfo("Permiso denegado", "Solo el administrador puede editar usuarios.");
            return;
        }

        UserTableRow seleccionado = tblUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarInfo("Selecciona un usuario", "Debes elegir un usuario antes de editar.");
            return;
        }

        UserAccountDTO dto = cacheJoin.stream()
                .filter(x -> x != null && x.getIsActive())
                .filter(x -> safe(x.getPersonnelNumber()).equals(safe(seleccionado.getPersonnelNumber())))
                .findFirst()
                .orElse(null);

        if (dto == null) {
            mostrarError("Error", "No se encontró el usuario seleccionado.");
            return;
        }

        openUserFormEdit(dto);
    }

    @FXML
    private void onEliminar() {
        if (!SessionManager.isAdmin()) {
            mostrarInfo("Permiso denegado", "Solo el administrador puede eliminar usuarios.");
            return;
        }

        UserTableRow seleccionado = tblUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarInfo("Selecciona un usuario", "Debes elegir un usuario antes de eliminarlo.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas eliminar al usuario " + safe(seleccionado.getUsername()) + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmar eliminación");
        confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        confirm.showAndWait();
        if (confirm.getResult() != ButtonType.YES) {
            return;
        }

        try {
            String personnelNumber = seleccionado.getPersonnelNumber();
            userManagementDAO.logicalDeleteUserByPersonnelNumber(
                    SessionManager.getCurrentAccountId(),
                    personnelNumber
            );

            mostrarInfo("Usuario eliminado", "El usuario ha sido eliminado correctamente.");
            cargarUsuarios();

        } catch (Exception ex) {
            mostrarError("Error al eliminar", ex.getMessage());
        }
    }

    private void openUserFormCreate() {
        try {
            usersViewSnapshot = snapshotCurrentContent();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(RESOURCE_USER_FORM));
            Node node = loader.load();

            UserFormController controller = loader.getController();
            controller.setCreateMode();
            controller.setOnSaveCallback(this::cargarUsuarios);
            controller.setOnCloseCallback(() -> {
                restoreUsersViewSnapshot();
                cargarUsuarios();
            });

            contentArea.getChildren().setAll(node);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);

        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir el formulario: " + ex.getMessage());
        }
    }

    private void openUserFormEdit(UserAccountDTO dto) {
        try {
            usersViewSnapshot = snapshotCurrentContent();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(RESOURCE_USER_FORM));
            Node node = loader.load();

            UserFormController controller = loader.getController();
            controller.setEditMode(dto);
            controller.setOnSaveCallback(this::cargarUsuarios);
            controller.setOnCloseCallback(() -> {
                restoreUsersViewSnapshot();
                cargarUsuarios();
            });

            contentArea.getChildren().setAll(node);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);

        } catch (Exception ex) {
            mostrarError("Error", "No se pudo abrir el formulario: " + ex.getMessage());
        }
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
        a.setContentText(msg);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }
}
