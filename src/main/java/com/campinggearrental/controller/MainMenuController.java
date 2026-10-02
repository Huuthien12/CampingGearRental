package com.campinggearrental.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MainMenuController {
    private final Runnable customers, equipment, rentals, checkout, logout;
    public MainMenuController(Runnable customers, Runnable equipment, Runnable rentals, Runnable checkout, Runnable logout) {
        this.customers = customers; this.equipment = equipment; this.rentals = rentals; this.checkout = checkout; this.logout = logout;
    }
    public Parent createView() {
        Label title = new Label("Camping Gear Rental"); title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Button customer = button("Customer Management", customers), gear = button("Equipment Management", equipment);
        Button rental = button("Rental Management", rentals), pay = button("Checkout", checkout), out = button("Logout", logout);
        VBox page = new VBox(12, title, customer, gear, rental, pay, out); page.setAlignment(Pos.CENTER); page.setPadding(new Insets(36));
        return page;
    }
    private static Button button(String text, Runnable action) { Button button = new Button(text); button.setMaxWidth(Double.MAX_VALUE); button.setOnAction(e -> action.run()); return button; }
}
