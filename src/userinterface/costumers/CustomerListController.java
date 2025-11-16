package userinterface.costumers;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import logic.DAO.CustomerDAO;
import logic.DTO.CustomerDTO;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CustomerListController {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // FXML

    @FXML
    private TextField TxtSearchEmail;

    @FXML
    private TableView<CustomerDTO> TblCustomers;


    @FXML private AnchorPane contentArea;

    @FXML
    private TableColumn<CustomerDTO, String> ColNumber;

    @FXML
    private TableColumn<CustomerDTO, String> ColName;

    @FXML
    private TableColumn<CustomerDTO, String> ColEmail;

    @FXML
    private TableColumn<CustomerDTO, String> ColPhone;

    @FXML
    private Label LblNumber;

    @FXML
    private Label LblName;

    @FXML
    private Label LblEmail;

    @FXML
    private Label LblPhone;

    @FXML
    private Label LblStatus;

    @FXML
    private Label LblCreatedAt;

    @FXML
    private Label LblUpdatedAt;

    @FXML
    private Button BtnEditCustomer;

    @FXML
    private Button BtnDeleteCustomer;

    @FXML
    private Button BtnRegisterCustomer;



    private final CustomerDAO customerDao = new CustomerDAO();
    private final ObservableList<CustomerDTO> backingList = FXCollections.observableArrayList();
    private FilteredList<CustomerDTO> filteredList;

    @FXML
    private void initialize() {
        configureTable();
        loadActiveCustomers();
        configureSearch();
        configureSelectionBinding();
    }

    private void configureTable() {
        ColNumber.setCellValueFactory(c ->
                Bindings.createStringBinding(() -> c.getValue().getCostumerNumber()));
        ColName.setCellValueFactory(c ->
                Bindings.createStringBinding(() -> c.getValue().getFullName()));
        ColEmail.setCellValueFactory(c ->
                Bindings.createStringBinding(() -> c.getValue().getEmail()));
        ColPhone.setCellValueFactory(c ->
                Bindings.createStringBinding(() -> c.getValue().getPhone()));

        filteredList = new FilteredList<>(backingList, c -> true);
        TblCustomers.setItems(filteredList);

        TblCustomers.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> showCustomerDetails(newSel));
    }

    private void loadActiveCustomers() {
        backingList.clear();
        try {
            backingList.addAll(customerDao.getActiveCustomers());
        } catch (SQLException | IOException ex) {
            showError("Error al cargar clientes", ex.getMessage());
        }
    }

    private void configureSearch() {
        TxtSearchEmail.textProperty().addListener((obs, oldText, newText) -> {
            String filter = newText == null ? "" : newText.trim().toLowerCase();
            filteredList.setPredicate(c -> {
                if (filter.isEmpty()) {
                    return true;
                }
                String email = c.getEmail() == null ? "" : c.getEmail().toLowerCase();
                return email.contains(filter);
            });
        });
    }

    private void configureSelectionBinding() {
        var selectionModel = TblCustomers.getSelectionModel();
        BtnEditCustomer.disableProperty().bind(selectionModel.selectedItemProperty().isNull());
        BtnDeleteCustomer.disableProperty().bind(selectionModel.selectedItemProperty().isNull());
    }

    private void showCustomerDetails(CustomerDTO customer) {
        if (customer == null) {
            LblNumber.setText("");
            LblName.setText("");
            LblEmail.setText("");
            LblPhone.setText("");
            LblStatus.setText("");
            LblStatus.getStyleClass().removeAll("status-disponible", "status-baja");
            LblCreatedAt.setText("");
            LblUpdatedAt.setText("");
            return;
        }

        LblNumber.setText(customer.getCostumerNumber());
        LblName.setText(customer.getFullName());
        LblEmail.setText(customer.getEmail());
        LblPhone.setText(customer.getPhone());
        LblStatus.setText(customer.getIsActive() ? "Activo" : "Inactivo");

        LblStatus.getStyleClass().removeAll("status-disponible", "status-baja");
        if (customer.getIsActive()) {
            if (!LblStatus.getStyleClass().contains("status-disponible")) {
                LblStatus.getStyleClass().add("status-disponible");
            }
        } else {
            if (!LblStatus.getStyleClass().contains("status-baja")) {
                LblStatus.getStyleClass().add("status-baja");
            }
        }

        if (customer.getCreatedAt() != null) {
            LblCreatedAt.setText(customer.getCreatedAt().format(DATE_FORMATTER));
        } else {
            LblCreatedAt.setText("-");
        }

        if (customer.getUpdatedAt() != null) {
            LblUpdatedAt.setText(customer.getUpdatedAt().format(DATE_FORMATTER));
        } else {
            LblUpdatedAt.setText("-");
        }
    }



    @FXML
    private void onRegisterCustomer(ActionEvent event) {
        openCustomerForm(null);
    }

    @FXML
    private void onEditCustomer(ActionEvent event) {
        CustomerDTO selected = TblCustomers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        openCustomerForm(selected);
    }

    @FXML
    private void onDeleteCustomer(ActionEvent event) {
        CustomerDTO selected = TblCustomers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar cliente");
        confirm.setHeaderText("¿Deseas eliminar este cliente?");
        confirm.setContentText("Cliente: " + selected.getFullName() +
                "\nCorreo: " + selected.getEmail());
        confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success =
                        customerDao.logicalDeleteCustomer(selected.getCostumerNumber());
                if (success) {
                    loadActiveCustomers();
                    showCustomerDetails(null);
                } else {
                    showError("No se pudo eliminar", "Intenta nuevamente.");
                }
            } catch (SQLException | IOException ex) {
                showError("Error al eliminar cliente", ex.getMessage());
            }
        }
    }

    private void openCustomerForm(CustomerDTO customer) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userinterface/costumers/CustomerForm.fxml"));

            Node node = loader.load();
            CustomerFormController controller = loader.getController();

            controller.setCustomer(customer);

            controller.setOnSaveCallback(() -> {
                loadActiveCustomers();
                showCustomerDetails(null);
            });

            controller.setOnCloseCallback(() -> {
                loadPage("/userinterface/costumers/CustomersView.fxml");
            });

            contentArea.getChildren().setAll(node);
            AnchorPane.setTopAnchor(node, 0.0);
            AnchorPane.setRightAnchor(node, 0.0);
            AnchorPane.setBottomAnchor(node, 0.0);
            AnchorPane.setLeftAnchor(node, 0.0);

        } catch (IOException e) {
            showError("Error al abrir formulario", e.getMessage());
        }
    }



    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

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
}
