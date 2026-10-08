package com.campinggearrental.web;
import com.campinggearrental.model.RentalOrder;
import com.campinggearrental.model.RentalOrderStatus;
import com.campinggearrental.service.PersistedCheckoutService;
import com.campinggearrental.service.PricingService;
import java.sql.SQLException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller public class CheckoutWebController {
 private final PersistedCheckoutService checkout; private final PricingService pricing;
 public CheckoutWebController(PersistedCheckoutService checkout,PricingService pricing){this.checkout=checkout;this.pricing=pricing;}
 @GetMapping("/checkout") String lookup(){return "checkout/lookup";}
 @GetMapping("/checkout/{rentalId}") String detail(@PathVariable("rentalId") String rentalId,Model model,RedirectAttributes flash){try{RentalOrder order=checkout.load(rentalId);model.addAttribute("order",order);model.addAttribute("rentalState",RentalOrderStatus.fromState(order.getCurrentState()).name());model.addAttribute("rentalDays",pricing.calculateRentalDays(order.getRentalDate(),order.getExpectedReturnDate()));return "checkout/detail";}catch(IllegalArgumentException|SQLException e){flash.addFlashAttribute("error","Rental order was not found or cannot be loaded.");return "redirect:/checkout";}}
 @PostMapping("/checkout/load") String load(@RequestParam("rentalId") String rentalId,RedirectAttributes flash){if(rentalId==null||rentalId.isBlank()){flash.addFlashAttribute("error","Rental ID is required.");return "redirect:/checkout";}flash.addFlashAttribute("success","Rental order loaded.");return "redirect:/checkout/"+rentalId.trim();}
 @PostMapping("/checkout/{rentalId}/refresh") String refresh(@PathVariable("rentalId") String rentalId,RedirectAttributes flash){try{checkout.refreshPricing(rentalId);flash.addFlashAttribute("success","Pricing refreshed.");}catch(IllegalArgumentException|SQLException e){flash.addFlashAttribute("error","Pricing could not be refreshed.");}return "redirect:/checkout/"+rentalId;}
 @PostMapping("/checkout/{rentalId}/pay") String pay(@PathVariable("rentalId") String rentalId,RedirectAttributes flash){try{checkout.confirmPayment(rentalId);flash.addFlashAttribute("success","Payment confirmed.");}catch(IllegalArgumentException|IllegalStateException|SQLException e){flash.addFlashAttribute("error","Payment could not be confirmed.");}return "redirect:/checkout/"+rentalId;}
}
