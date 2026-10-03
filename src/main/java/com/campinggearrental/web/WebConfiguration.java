package com.campinggearrental.web;

import com.campinggearrental.repository.*;
import com.campinggearrental.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfiguration {
    @Bean CategoryRepository categoryRepository() { return new JdbcCategoryRepository(); }
    @Bean CategoryService categoryService(CategoryRepository r) { return new CategoryService(r); }
    @Bean CustomerRepository customerRepository() { return new JdbcCustomerRepository(); }
    @Bean EquipmentRepository equipmentRepository() { return new JdbcEquipmentRepository(); }
    @Bean RentalOrderRepository rentalOrderRepository() { return new JdbcRentalOrderRepository(); }
    @Bean CustomerService customerService(CustomerRepository r) { return new CustomerService(r); }
    @Bean EquipmentService equipmentService(EquipmentRepository r) { return new EquipmentService(r); }
    @Bean RentalService rentalService(CustomerRepository c, EquipmentRepository e, RentalOrderRepository r) { return new RentalService(c,e,r); }
    @Bean PricingService pricingService() { return new PricingService(); }
    @Bean CheckoutService checkoutService(PricingService p) { return new CheckoutService(new RentalPricingAdapter(p)); }
    @Bean PersistedCheckoutService persistedCheckoutService(RentalOrderRepository r, CheckoutService c) { return new PersistedCheckoutService(r,c); }
}
