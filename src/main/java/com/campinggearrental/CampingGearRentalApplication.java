package com.campinggearrental;

import javafx.application.Application;
import com.campinggearrental.controller.CheckoutController;
import com.campinggearrental.service.CheckoutService;
import com.campinggearrental.service.PricingService;
import com.campinggearrental.service.RentalPricingAdapter;
import com.campinggearrental.util.CheckoutDemoData;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CampingGearRentalApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/checkout.fxml"));
        Parent root = loader.load();
        CheckoutController controller = loader.getController();
        controller.load(
                CheckoutDemoData.createOrder(),
                new CheckoutService(new RentalPricingAdapter(new PricingService())));

        stage.setTitle("Camping Gear Rental Checkout");
        stage.setScene(new Scene(root, 760, 560));
        stage.show();
    }
}
