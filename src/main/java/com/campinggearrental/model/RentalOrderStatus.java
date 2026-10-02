package com.campinggearrental.model;

import com.campinggearrental.state.CancelledState;
import com.campinggearrental.state.ConfirmedState;
import com.campinggearrental.state.PendingState;
import com.campinggearrental.state.RentalState;
import com.campinggearrental.state.RentedState;
import com.campinggearrental.state.ReturnedState;
import java.util.Objects;

/** Durable representation of the State owned by a rental order. */
public enum RentalOrderStatus {
    PENDING, CONFIRMED, RENTED, RETURNED, CANCELLED;

    public static RentalOrderStatus fromState(RentalState state) {
        Objects.requireNonNull(state, "rental state must not be null");
        if (state instanceof PendingState) return PENDING;
        if (state instanceof ConfirmedState) return CONFIRMED;
        if (state instanceof RentedState) return RENTED;
        if (state instanceof ReturnedState) return RETURNED;
        if (state instanceof CancelledState) return CANCELLED;
        throw new IllegalArgumentException("unsupported rental state: " + state.getClass().getName());
    }

    public static RentalOrderStatus fromPersisted(String value) {
        if (value == null) throw new IllegalArgumentException("rental status must not be null");
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown rental status: " + value, exception);
        }
    }

    public RentalState toState() {
        return switch (this) {
            case PENDING -> new PendingState();
            case CONFIRMED -> new ConfirmedState();
            case RENTED -> new RentedState();
            case RETURNED -> new ReturnedState();
            case CANCELLED -> new CancelledState();
        };
    }
}
