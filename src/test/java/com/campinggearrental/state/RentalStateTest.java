package com.campinggearrental.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campinggearrental.model.PaymentStatus;
import com.campinggearrental.model.RentalDetail;
import com.campinggearrental.model.RentalOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RentalStateTest {
    @Test
    void newOrderStartsPending() {
        assertInstanceOf(PendingState.class, order().getCurrentState());
    }

    @Test
    void pendingCanConfirm() {
        RentalOrder order = order();
        order.confirmOrder();
        assertInstanceOf(ConfirmedState.class, order.getCurrentState());
    }

    @Test
    void pendingCanCancel() {
        RentalOrder order = order();
        order.cancelOrder();
        assertInstanceOf(CancelledState.class, order.getCurrentState());
    }

    @Test
    void confirmedCanRent() {
        RentalOrder order = confirmedOrder();
        order.rentEquipment();
        assertInstanceOf(RentedState.class, order.getCurrentState());
    }

    @Test
    void confirmedCanCancel() {
        RentalOrder order = confirmedOrder();
        order.cancelOrder();
        assertInstanceOf(CancelledState.class, order.getCurrentState());
    }

    @Test
    void rentedCanReturn() {
        RentalOrder order = rentedOrder();
        order.returnEquipment();
        assertInstanceOf(ReturnedState.class, order.getCurrentState());
    }

    @Test
    void pendingCannotRent() {
        assertThrows(IllegalStateException.class, () -> order().rentEquipment());
    }

    @Test
    void pendingCannotReturn() {
        assertThrows(IllegalStateException.class, () -> order().returnEquipment());
    }

    @Test
    void confirmedCannotConfirmAgain() {
        assertThrows(IllegalStateException.class, () -> confirmedOrder().confirmOrder());
    }

    @Test
    void confirmedCannotReturn() {
        assertThrows(IllegalStateException.class, () -> confirmedOrder().returnEquipment());
    }

    @Test
    void rentedCannotConfirmAgain() {
        assertThrows(IllegalStateException.class, () -> rentedOrder().confirmOrder());
    }

    @Test
    void rentedCannotRentAgain() {
        assertThrows(IllegalStateException.class, () -> rentedOrder().rentEquipment());
    }

    @Test
    void rentedCannotCancel() {
        assertThrows(IllegalStateException.class, () -> rentedOrder().cancelOrder());
    }

    @Test
    void returnedStateIsTerminal() {
        RentalOrder order = rentedOrder();
        order.returnEquipment();
        assertAllInvalidTransitions(order);
    }

    @Test
    void cancelledStateIsTerminal() {
        RentalOrder order = order();
        order.cancelOrder();
        assertAllInvalidTransitions(order);
    }

    @Test
    void stateTransitionsPreservePricingPaymentAndDetailSnapshot() {
        RentalOrder order = order();
        RentalDetail detail = order.getDetails().getFirst();
        order.confirmOrder();
        order.rentEquipment();
        order.returnEquipment();

        assertEquals(new BigDecimal("100.00"), order.getSubtotal());
        assertEquals(new BigDecimal("10.00"), order.getDiscount());
        assertEquals(new BigDecimal("90.00"), order.getTotal());
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
        assertEquals(2, detail.getQuantity());
        assertEquals(new BigDecimal("50.00"), detail.getUnitPrice());
    }

    private static void assertAllInvalidTransitions(RentalOrder order) {
        assertThrows(IllegalStateException.class, order::confirmOrder);
        assertThrows(IllegalStateException.class, order::rentEquipment);
        assertThrows(IllegalStateException.class, order::returnEquipment);
        assertThrows(IllegalStateException.class, order::cancelOrder);
    }

    private static RentalOrder confirmedOrder() {
        RentalOrder order = order();
        order.confirmOrder();
        return order;
    }

    private static RentalOrder rentedOrder() {
        RentalOrder order = confirmedOrder();
        order.rentEquipment();
        return order;
    }

    private static RentalOrder order() {
        RentalDetail detail = new RentalDetail();
        detail.setQuantity(2);
        detail.setUnitPrice(new BigDecimal("50.00"));

        RentalOrder order = new RentalOrder();
        order.setRentalDate(LocalDate.of(2026, 10, 1));
        order.setExpectedReturnDate(LocalDate.of(2026, 10, 4));
        order.setDetails(List.of(detail));
        order.setSubtotal(new BigDecimal("100.00"));
        order.setDiscount(new BigDecimal("10.00"));
        order.setTotal(new BigDecimal("90.00"));
        order.setPaymentStatus(PaymentStatus.UNPAID);
        return order;
    }
}
