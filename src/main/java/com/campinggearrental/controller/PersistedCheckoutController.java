package com.campinggearrental.controller;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.service.PersistedCheckoutService;
import java.sql.SQLException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
public class PersistedCheckoutController {
    private final PersistedCheckoutService service; private final Runnable onBack; private RentalOrder order;
    private final TextField id=new TextField(); private final Label info=new Label(); private final TableView<RentalDetail> details=new TableView<>();
    public PersistedCheckoutController(PersistedCheckoutService service,Runnable onBack){this.service=service;this.onBack=onBack;}
    public Parent createView(){ id.setPromptText("Rental ID"); Button load=b("Load",this::load),refresh=b("Refresh price",()->run(() -> order=service.refreshPricing(requireId()))),pay=b("Confirm payment",()->run(() -> order=service.confirmPayment(requireId()))),back=b("Back",onBack); TableColumn<RentalDetail,Integer> q=new TableColumn<>("Quantity");q.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue().getQuantity()));TableColumn<RentalDetail,java.math.BigDecimal> p=new TableColumn<>("Unit price");p.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue().getUnitPrice()));details.getColumns().add(q);details.getColumns().add(p); VBox page=new VBox(10,new Label("Checkout"),new HBox(8,id,load,refresh,pay,back),details,info);page.setPadding(new Insets(16));return page; }
    private Button b(String text,Runnable r){Button b=new Button(text);b.setOnAction(e->r.run());return b;} private void load(){run(()->order=service.load(id.getText()));} private String requireId(){if(order==null)throw new IllegalArgumentException("Load a rental first");return order.getId();} private void run(Operation o){try{o.run();details.setItems(FXCollections.observableArrayList(order.getDetails()));info.setText("Rental: "+order.getRentalDate()+" | Expected: "+order.getExpectedReturnDate()+" | Subtotal: "+order.getSubtotal()+" | Discount: "+order.getDiscount()+" | Total: "+order.getTotal()+" | Payment: "+order.getPaymentStatus());}catch(IllegalArgumentException|IllegalStateException|SQLException e){new Alert(Alert.AlertType.WARNING,e.getMessage()).showAndWait();}} @FunctionalInterface private interface Operation{void run()throws SQLException;}
}
