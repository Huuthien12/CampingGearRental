package com.campinggearrental.controller;

import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.service.CheckoutService;
import com.campinggearrental.service.PricingService;
import java.math.BigDecimal;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class CheckoutController {
    @FXML private Label rentalDateLabel;
    @FXML private Label expectedReturnDateLabel;
    @FXML private Label rentalDaysLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;
    @FXML private Label paymentStatusLabel;
    @FXML private TableView<RentalDetail> detailsTable;
    @FXML private TableColumn<RentalDetail, Integer> quantityColumn;
    @FXML private TableColumn<RentalDetail, BigDecimal> unitPriceColumn;

    private RentalOrder rentalOrder;
    private CheckoutService checkoutService;

    @FXML
    private void initialize() {
        quantityColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getQuantity()));
        unitPriceColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getUnitPrice()));
    }

    public void load(RentalOrder rentalOrder, CheckoutService checkoutService) {
        this.rentalOrder = rentalOrder;
        this.checkoutService = checkoutService;
        rentalDateLabel.setText(rentalOrder.getRentalDate().toString());
        expectedReturnDateLabel.setText(rentalOrder.getExpectedReturnDate().toString());
        detailsTable.setItems(FXCollections.observableArrayList(rentalOrder.getDetails()));
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    @FXML
    private void handleConfirmPayment() {
        try {
            checkoutService.confirmPayment(rentalOrder);
            updatePaymentStatus();
        } catch (IllegalStateException exception) {
            showError("Payment cannot be confirmed", "The order must be unpaid before payment can be confirmed.");
        }
    }

    private void refresh() {
        try {
            updatePricing(checkoutService.refresh(rentalOrder));
            updatePaymentStatus();
        } catch (IllegalArgumentException exception) {
            showError("Invalid rental dates", exception.getMessage());
        } catch (RuntimeException exception) {
            showError("Calculation failed", "Unable to calculate the checkout price.");
        }
    }

    private void updatePricing(PricingService.PricingResult result) {
        rentalDaysLabel.setText(Long.toString(result.rentalDays()));
        subtotalLabel.setText(format(result.subtotal()));
        discountLabel.setText(format(result.discount()));
        totalLabel.setText(format(result.total()));
    }

    private void updatePaymentStatus() {
        paymentStatusLabel.setText(String.valueOf(rentalOrder.getPaymentStatus()));
    }

    private String format(BigDecimal amount) {
        return amount.toPlainString();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
