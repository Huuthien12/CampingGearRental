package com.campinggearrental.controller;

import com.campinggearrental.model.Customer;
import com.campinggearrental.service.CustomerService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

public class CustomerController {
    private final CustomerService customerService;
    private final Runnable onLogout;
    private final TableView<Customer> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField addressField = new TextField();
    private Customer selectedCustomer;

    public CustomerController(CustomerService customerService, Runnable onLogout) {
        this.customerService = customerService;
        this.onLogout = onLogout;
    }

    public Parent createView() {
        configureTable();
        Label title = new Label("Quản lý khách hàng");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Button logoutButton = new Button("Đăng xuất");
        logoutButton.setOnAction(event -> onLogout.run());
        HBox header = new HBox(12, title, logoutButton);
        HBox.setHgrow(title, Priority.ALWAYS);
        header.setPadding(new Insets(16));

        searchField.setPromptText("Tìm theo tên hoặc số điện thoại");
        searchField.textProperty().addListener((observable, previous, current) -> refreshCustomers());
        VBox listArea = new VBox(10, searchField, table);
        listArea.setPadding(new Insets(0, 16, 16, 16));
        VBox.setVgrow(table, Priority.ALWAYS);

        Parent form = createForm();
        BorderPane page = new BorderPane();
        page.setTop(header);
        page.setCenter(listArea);
        page.setBottom(form);
        refreshCustomers();
        return page;
    }

    private void configureTable() {
        addColumn("Mã", Customer::id, 130);
        addColumn("Họ tên", Customer::fullName, 220);
        addColumn("Số điện thoại", Customer::phone, 150);
        addColumn("Email", Customer::email, 220);
        addColumn("Địa chỉ", Customer::address, 240);
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> {
            selectedCustomer = current;
            if (current != null) {
                nameField.setText(current.fullName());
                phoneField.setText(current.phone());
                emailField.setText(current.email());
                addressField.setText(current.address());
            }
        });
    }

    private void addColumn(String title, CustomerText value, double width) {
        TableColumn<Customer, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.get(cell.getValue())));
        column.setPrefWidth(width);
        table.getColumns().add(column);
    }

    private Parent createForm() {
        nameField.setPromptText("Họ tên *");
        phoneField.setPromptText("Số điện thoại *");
        emailField.setPromptText("Email");
        addressField.setPromptText("Địa chỉ");
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(8);
        fields.addRow(0, new Label("Họ tên"), nameField, new Label("Điện thoại"), phoneField);
        fields.addRow(1, new Label("Email"), emailField, new Label("Địa chỉ"), addressField);
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(phoneField, Priority.ALWAYS);
        GridPane.setHgrow(emailField, Priority.ALWAYS);
        GridPane.setHgrow(addressField, Priority.ALWAYS);

        Button addButton = new Button("Thêm");
        addButton.setOnAction(event -> addCustomer());
        Button updateButton = new Button("Cập nhật");
        updateButton.setOnAction(event -> updateCustomer());
        Button deleteButton = new Button("Xóa");
        deleteButton.setOnAction(event -> deleteCustomer());
        Button clearButton = new Button("Làm mới");
        clearButton.setOnAction(event -> clearForm());

        HBox actions = new HBox(8, addButton, updateButton, deleteButton, clearButton);
        VBox form = new VBox(10, fields, actions);
        form.setPadding(new Insets(14, 16, 16, 16));
        form.setStyle("-fx-border-color: #d8dedb; -fx-border-width: 1 0 0 0;");
        return form;
    }

    private void refreshCustomers() {
        try {
            table.setItems(FXCollections.observableArrayList(customerService.search(searchField.getText())));
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Không thể tải danh sách khách hàng. Kiểm tra kết nối MySQL.");
        }
    }

    private void addCustomer() {
        try {
            customerService.add(nameField.getText(), phoneField.getText(), emailField.getText(), addressField.getText());
            clearForm();
            refreshCustomers();
        } catch (IllegalArgumentException exception) {
            showAlert(Alert.AlertType.WARNING, "Thông tin chưa hợp lệ", exception.getMessage());
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Không thể thêm khách hàng", databaseMessage(exception));
        }
    }

    private void updateCustomer() {
        if (selectedCustomer == null) {
            showAlert(Alert.AlertType.INFORMATION, "Chưa chọn khách hàng", "Chọn khách hàng trong danh sách để cập nhật.");
            return;
        }
        try {
            customerService.update(new Customer(selectedCustomer.id(), nameField.getText(), phoneField.getText(),
                    emailField.getText(), addressField.getText()));
            clearForm();
            refreshCustomers();
        } catch (IllegalArgumentException exception) {
            showAlert(Alert.AlertType.WARNING, "Thông tin chưa hợp lệ", exception.getMessage());
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Không thể cập nhật khách hàng", databaseMessage(exception));
        }
    }

    private void deleteCustomer() {
        if (selectedCustomer == null) {
            showAlert(Alert.AlertType.INFORMATION, "Chưa chọn khách hàng", "Chọn khách hàng trong danh sách để xóa.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa khách hàng " + selectedCustomer.fullName() + "?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText(null);
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            customerService.delete(selectedCustomer.id());
            clearForm();
            refreshCustomers();
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Không thể xóa khách hàng",
                    "Khách hàng có thể đang được tham chiếu bởi đơn thuê.");
        }
    }

    private void clearForm() {
        selectedCustomer = null;
        table.getSelectionModel().clearSelection();
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        addressField.clear();
    }

    private String databaseMessage(SQLException exception) {
        if (exception.getErrorCode() == 1062) {
            return "Số điện thoại đã được sử dụng.";
        }
        return "Kiểm tra kết nối cơ sở dữ liệu và dữ liệu nhập.";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FunctionalInterface
    private interface CustomerText {
        String get(Customer customer);
    }
}