package com.campinggearrental.state;

import com.campinggearrental.model.RentalOrder;

public interface RentalState {
    void confirm(RentalOrder order);
    void rent(RentalOrder order);
    void returnEquipment(RentalOrder order);
    void cancel(RentalOrder order);
}