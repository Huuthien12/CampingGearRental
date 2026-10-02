package com.campinggearrental.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campinggearrental.state.CancelledState;
import com.campinggearrental.state.ConfirmedState;
import com.campinggearrental.state.PendingState;
import com.campinggearrental.state.RentedState;
import com.campinggearrental.state.ReturnedState;
import org.junit.jupiter.api.Test;

class RentalOrderStatusTest {
    @Test
    void mapsEveryPersistedValueToItsState() {
        assertInstanceOf(PendingState.class, RentalOrderStatus.PENDING.toState());
        assertInstanceOf(ConfirmedState.class, RentalOrderStatus.CONFIRMED.toState());
        assertInstanceOf(RentedState.class, RentalOrderStatus.RENTED.toState());
        assertInstanceOf(ReturnedState.class, RentalOrderStatus.RETURNED.toState());
        assertInstanceOf(CancelledState.class, RentalOrderStatus.CANCELLED.toState());
    }

    @Test
    void mapsEveryStateToItsPersistedValue() {
        assertEquals(RentalOrderStatus.PENDING, RentalOrderStatus.fromState(new PendingState()));
        assertEquals(RentalOrderStatus.CONFIRMED, RentalOrderStatus.fromState(new ConfirmedState()));
        assertEquals(RentalOrderStatus.RENTED, RentalOrderStatus.fromState(new RentedState()));
        assertEquals(RentalOrderStatus.RETURNED, RentalOrderStatus.fromState(new ReturnedState()));
        assertEquals(RentalOrderStatus.CANCELLED, RentalOrderStatus.fromState(new CancelledState()));
    }

    @Test
    void rejectsUnknownOrMissingPersistedValues() {
        assertThrows(IllegalArgumentException.class, () -> RentalOrderStatus.fromPersisted("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> RentalOrderStatus.fromPersisted(null));
    }
}
