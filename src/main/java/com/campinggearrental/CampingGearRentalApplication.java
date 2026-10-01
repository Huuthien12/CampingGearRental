package com.campinggearrental;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.campinggearrental.controller.CustomerController;
import com.campinggearrental.controller.LoginController;
import com.campinggearrental.repository.JdbcCustomerRepository;
import com.campinggearrental.repository.JdbcUserRepository;
import com.campinggearrental.service.AuthService;
import com.campinggearrental.service.CustomerService;

public class CampingGearRentalApplication extends Application {
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Camping Gear Rental Management");
        showLogin();
        stage.show();
    }

    private void showLogin() {
        LoginController controller = new LoginController(
                new AuthService(new JdbcUserRepository()), this::showCustomers);
        stage.setScene(new Scene(controller.createView(), 460, 320));
        stage.centerOnScreen();
    }

    private void showCustomers() {
        CustomerController controller = new CustomerController(
                new CustomerService(new JdbcCustomerRepository()), this::showLogin);
        stage.setScene(new Scene(controller.createView(), 1050, 680));
        stage.centerOnScreen();
    }
}
