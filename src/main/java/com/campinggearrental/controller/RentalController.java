package com.campinggearrental.controller;

import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.repository.RentalOrderRepository;
import com.campinggearrental.service.RentalService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class RentalController {
    private final RentalService rentalService; private final RentalOrderRepository repository; private final Runnable onBack;
    private final TableView<RentalOrder> table = new TableView<>(); private final TextField customer = new TextField(), items = new TextField();
    private final DatePicker rentalDate = new DatePicker(), expectedDate = new DatePicker(), actualDate = new DatePicker(); private RentalOrder selected;
    public RentalController(RentalService rentalService, RentalOrderRepository repository, Runnable onBack) { this.rentalService=rentalService; this.repository=repository; this.onBack=onBack; }
    public Parent createView() {
        add("ID", RentalOrder::getId); add("Customer", RentalOrder::getCustomerId); add("Rental", o -> String.valueOf(o.getRentalDate())); add("Expected", o -> String.valueOf(o.getExpectedReturnDate()));
        add("Actual", o -> String.valueOf(o.getActualReturnDate())); add("Status", o -> o.getCurrentState().getClass().getSimpleName().replace("State", "").toUpperCase()); add("Payment", o -> String.valueOf(o.getPaymentStatus())); add("Total", o -> String.valueOf(o.getTotal()));
        table.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> selected=value);
        customer.setPromptText("Customer ID"); items.setPromptText("Equipment:quantity, e.g. EQ001:2,EQ002:1"); rentalDate.setPromptText("Rental date"); expectedDate.setPromptText("Expected return"); actualDate.setPromptText("Actual return");
        Button create = new Button("Create draft"); create.setOnAction(e -> create()); Button confirm = action("Confirm", () -> rentalService.confirmRental(id())); Button rent = action("Hand over", () -> rentalService.rentRental(id())); Button cancel = action("Cancel", () -> rentalService.cancelRental(id())); Button returned = action("Return", () -> rentalService.returnRental(id(), actualDate.getValue())); Button back = new Button("Back"); back.setOnAction(e -> onBack.run());
        VBox form = new VBox(8, new HBox(8, customer, rentalDate, expectedDate), items, new HBox(8, create, confirm, rent, cancel, actualDate, returned, back)); form.setPadding(new Insets(12));
        BorderPane page = new BorderPane(table); page.setTop(new Label("Rental Management")); page.setBottom(form); BorderPane.setMargin(page.getTop(), new Insets(12)); refresh(); return page;
    }
    private void add(String title, Text value) { TableColumn<RentalOrder,String> c=new TableColumn<>(title); c.setCellValueFactory(x -> new ReadOnlyStringWrapper(value.text(x.getValue()))); table.getColumns().add(c); }
    private void create() { try { List<RentalService.RentalRequestItem> lines=Arrays.stream(items.getText().split(",")).map(String::trim).map(s -> s.split(":" )).map(p -> new RentalService.RentalRequestItem(p[0].trim(), Integer.parseInt(p[1].trim()))).toList(); rentalService.createDraft(new RentalService.RentalRequest(customer.getText(), rentalDate.getValue(), expectedDate.getValue(), lines)); refresh(); } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException | SQLException e) { error(e.getMessage()); } }
    private Button action(String text, Operation op) { Button b=new Button(text); b.setOnAction(e -> { try { op.run(); refresh(); } catch (IllegalArgumentException | IllegalStateException | SQLException x) { error(x.getMessage()); } }); return b; }
    private String id() { if(selected==null) throw new IllegalArgumentException("Select a rental first"); return selected.getId(); }
    private void refresh() { try { table.setItems(FXCollections.observableArrayList(repository.findAll())); } catch (SQLException e) { error("Unable to load rentals"); } }
    private void error(String message) { new Alert(Alert.AlertType.WARNING, message == null ? "Operation failed" : message).showAndWait(); }
    @FunctionalInterface private interface Text { String text(RentalOrder order); } @FunctionalInterface private interface Operation { void run() throws SQLException; }
}
