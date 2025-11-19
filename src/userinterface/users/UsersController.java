package userinterface.users;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import logic.DAO.AccountDAO;
import logic.DAO.UserDAO;
import logic.DTO.AccountDTO;
import logic.DTO.UserDTO;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class UsersController {

    @FXML private TableView<UserTableRow> tblUsuarios;
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

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarUsuarios();
        txtBuscar.textProperty().addListener((obs, old, val) -> filtrar(val));
    }

    private void configurarTabla() {
        colUsername.setCellValueFactory(d -> d.getValue().usernameProperty());
        colFullName.setCellValueFactory(d -> d.getValue().fullNameProperty());
        colEmail.setCellValueFactory(d -> d.getValue().emailProperty());
        colRole.setCellValueFactory(d -> d.getValue().roleProperty());
        colActivo.setCellValueFactory(d -> d.getValue().activoProperty());
        colCreated.setCellValueFactory(d -> d.getValue().createdAtProperty());
        tblUsuarios.setItems(usuarios);
    }

    private void cargarUsuarios() {
        usuarios.clear();
        try {
            List<UserDTO> allUsers = userDAO.getAllUsers();
            List<AccountDTO> allAccounts = accountDAO.getAllAccounts();

            List<UserTableRow> rows = allUsers.stream()
                    .map(u -> {
                        AccountDTO acc = allAccounts.stream()
                                .filter(a -> a.getAccountId() == u.getAccountId())
                                .findFirst()
                                .orElse(null);

                        String email = acc != null ? acc.getEmail() : "(sin cuenta)";
                        String role = (acc != null && acc.getRole() != null)
                                ? acc.getRole().name()
                                : "-";
                        String activo = acc != null && acc.getIsActive() ? "Sí" : "No";
                        String created = acc != null && acc.getCreatedAt() != null
                                ? acc.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                : "-";

                        return new UserTableRow(
                                u.getUsername(),
                                u.getFullName(),
                                email,
                                role,
                                activo,
                                created
                        );
                    })
                    .collect(Collectors.toList());

            usuarios.setAll(rows);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al cargar usuarios", e.toString());
        }
    }

    private void filtrar(String texto) {
        if (texto == null || texto.isBlank()) {
            cargarUsuarios();
            return;
        }
        String filtro = texto.toLowerCase();
        usuarios.setAll(usuarios.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(filtro)
                        || u.getFullName().toLowerCase().contains(filtro)
                        || u.getEmail().toLowerCase().contains(filtro))
                .collect(Collectors.toList()));
    }

    @FXML
    private void onNuevo() {
        mostrarInfo("Módulo en implementación", "La creación de usuarios se implementará más adelante.");
    }

    @FXML
    private void onEditar() {
        mostrarInfo("Módulo en implementación", "La edición de usuarios se implementará más adelante.");
    }

    @FXML
    private void onEliminar() {
        UserTableRow seleccionado = tblUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarInfo("Selecciona un usuario", "Debes elegir un usuario antes de eliminarlo.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas eliminar al usuario " + seleccionado.getUsername() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmar eliminación");
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            try {
                UserDTO user = userDAO.findUserByPersonnelNumber(seleccionado.getUsername());
                if (user != null) {
                    userDAO.deleteUser(user.getPersonnelNumber());
                    mostrarInfo("Usuario eliminado", "El usuario ha sido eliminado correctamente.");
                    cargarUsuarios();
                }
            } catch (Exception e) {
                mostrarError("Error al eliminar", e.getMessage());
            }
        }
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
