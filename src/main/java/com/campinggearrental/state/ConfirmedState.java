package com.campinggearrental.state;
import com.campinggearrental.model.RentalOrder;

public class ConfirmedState implements RentalState {
    @Override
    public void confirm(RentalOrder order) {
        throw new IllegalStateException("Đơn này đã được xác nhận rồi.");
    }

    @Override
    public void rent(RentalOrder order) {
        order.setCurrentState(new RentedState());
    }

    @Override
    public void returnEquipment(RentalOrder order) {
        throw new IllegalStateException("Chưa giao thiết bị, không thể trả.");
    }

    @Override
    public void cancel(RentalOrder order) {
        order.setCurrentState(new CancelledState());
    }
}
