package com.campinggearrental;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.campinggearrental.controller.CustomerController;
import com.campinggearrental.controller.LoginController;
import com.campinggearrental.controller.EquipmentController;
import com.campinggearrental.controller.MainMenuController;
import com.campinggearrental.controller.RentalController;
import com.campinggearrental.controller.PersistedCheckoutController;
import com.campinggearrental.repository.JdbcCustomerRepository;
import com.campinggearrental.repository.JdbcEquipmentRepository;
import com.campinggearrental.repository.JdbcRentalOrderRepository;
import com.campinggearrental.repository.JdbcUserRepository;
import com.campinggearrental.service.AuthService;
import com.campinggearrental.service.CustomerService;
import com.campinggearrental.service.EquipmentService;
import com.campinggearrental.service.RentalService;
import com.campinggearrental.service.PersistedCheckoutService;
import com.campinggearrental.service.CheckoutService;
import com.campinggearrental.service.RentalPricingAdapter;
import com.campinggearrental.service.PricingService;

public class CampingGearRentalApplication extends Application {
    private Stage stage;
    private final JdbcCustomerRepository customers = new JdbcCustomerRepository(); private final JdbcEquipmentRepository equipment = new JdbcEquipmentRepository(); private final JdbcRentalOrderRepository rentals = new JdbcRentalOrderRepository();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Camping Gear Rental Management");
        showLogin();
        stage.show();
    }

    private void showLogin() {
        LoginController controller = new LoginController(
                new AuthService(new JdbcUserRepository()), this::showMenu);
        stage.setScene(new Scene(controller.createView(), 460, 320));
        stage.centerOnScreen();
    }

    private void showCustomers() {
        CustomerController controller = new CustomerController(
                new CustomerService(customers), this::showMenu);
        stage.setScene(new Scene(controller.createView(), 1050, 680));
        stage.centerOnScreen();
    }
    private void showMenu(){ show(new MainMenuController(this::showCustomers,this::showEquipment,this::showRentals,this::showCheckout,this::showLogin).createView(),520,440); }
    private void showEquipment(){ show(new EquipmentController(new EquipmentService(equipment),this::showMenu).createView(),1050,680); }
    private void showRentals(){ show(new RentalController(new RentalService(customers,equipment,rentals),rentals,this::showMenu).createView(),1100,680); }
    private void showCheckout(){ show(new PersistedCheckoutController(new PersistedCheckoutService(rentals,new CheckoutService(new RentalPricingAdapter(new PricingService()))),this::showMenu).createView(),850,620); }
    private void show(javafx.scene.Parent root,int width,int height){stage.setScene(new Scene(root,width,height));stage.centerOnScreen();}
}
