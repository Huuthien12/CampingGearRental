package com.campinggearrental;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CampingGearRentalApplication extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Camping Gear Rental Management");
        stage.setScene(new Scene(new StackPane(new Label("Camping Gear Rental Management")), 480, 240));
        stage.show();
    }
}
