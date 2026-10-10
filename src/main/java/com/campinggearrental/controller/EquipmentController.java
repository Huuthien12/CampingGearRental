package com.campinggearrental.controller;

import com.campinggearrental.model.Equipment;
import com.campinggearrental.model.EquipmentStatus;
import com.campinggearrental.service.EquipmentService;
import java.math.BigDecimal;
import java.sql.SQLException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Standalone B-module view; application navigation is wired during final integration. */
public class EquipmentController {
    private final EquipmentService service;
    private final Runnable onBack;
    private final TableView<Equipment> table = new TableView<>();
    private final TextField search = new TextField();
    private final TextField id = new TextField();
    private final TextField name = new TextField();
    private final TextField category = new TextField();
    private final TextField price = new TextField();
    private final TextField total = new TextField();
    private Equipment selected;

    public EquipmentController(EquipmentService service) { this(service, () -> { }); }
    public EquipmentController(EquipmentService service, Runnable onBack) { this.service = service; this.onBack = onBack; }

    public Parent createView() {
        addColumn("ID", Equipment::getEquipmentId); addColumn("Name", Equipment::getName); addColumn("Category", Equipment::getCategoryId);
        addColumn("Price/day", e -> e.getPricePerDay().toPlainString()); addColumn("Available", e -> e.getAvailableQuantity() + "/" + e.getTotalQuantity());
        table.getSelectionModel().selectedItemProperty().addListener((o, old, current) -> populate(current));
        search.setPromptText("Search name or category"); search.textProperty().addListener((o, old, value) -> refresh());
        id.setPromptText("ID"); name.setPromptText("Name"); category.setPromptText("Category ID"); price.setPromptText("Price/day"); total.setPromptText("Total quantity");
        Button add = new Button("Add"); add.setOnAction(e -> save(false));
        Button update = new Button("Update"); update.setOnAction(e -> save(true));
        Button toggle = new Button("Activate/Deactivate"); toggle.setOnAction(e -> toggleStatus());
        VBox form = new VBox(8, new HBox(8, id, name, category, price, total), new HBox(8, add, update, toggle)); form.setPadding(new Insets(12));
        Button back = new Button("Back"); back.setOnAction(e -> onBack.run()); VBox header = new VBox(8, search, back); BorderPane page = new BorderPane(table); page.setTop(header); page.setBottom(form); BorderPane.setMargin(header, new Insets(12)); refresh(); return page;
    }

    private void addColumn(String title, Text value) { TableColumn<Equipment, String> column = new TableColumn<>(title); column.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.value(c.getValue()))); table.getColumns().add(column); }
    private void populate(Equipment equipment) { selected = equipment; if (equipment != null) { id.setText(equipment.getEquipmentId()); name.setText(equipment.getName()); category.setText(equipment.getCategoryId()); price.setText(equipment.getPricePerDay().toPlainString()); total.setText(String.valueOf(equipment.getTotalQuantity())); } }
    private void save(boolean update) { try { if (update) { if (selected == null) throw new IllegalArgumentException("Select equipment first"); service.updateCatalog(selected.getEquipmentId(), catalogUpdate(selected.getStatus())); } else { service.create(new Equipment(id.getText(), name.getText(), category.getText(), new BigDecimal(price.getText().trim()), Integer.parseInt(total.getText().trim()))); } refresh(); } catch (IllegalArgumentException | SQLException exception) { error(exception.getMessage()); } }
    private void toggleStatus() { if (selected == null) { error("Select equipment first"); return; } try { service.updateCatalog(selected.getEquipmentId(), new EquipmentService.CatalogUpdate(selected.getName(), selected.getCategoryId(), selected.getPricePerDay(), selected.getTotalQuantity(), selected.getStatus() == EquipmentStatus.AVAILABLE ? EquipmentStatus.INACTIVE : EquipmentStatus.AVAILABLE)); refresh(); } catch (IllegalArgumentException | SQLException exception) { error(exception.getMessage()); } }
    private EquipmentService.CatalogUpdate catalogUpdate(EquipmentStatus status) { return new EquipmentService.CatalogUpdate(name.getText(), category.getText(), new BigDecimal(price.getText().trim()), Integer.parseInt(total.getText().trim()), status); }
    private void refresh() { try { table.setItems(FXCollections.observableArrayList(service.search(search.getText()))); } catch (SQLException exception) { error("Unable to load equipment"); } }
    private void error(String message) { new Alert(Alert.AlertType.WARNING, message).showAndWait(); }
    @FunctionalInterface private interface Text { String value(Equipment equipment); }
}
