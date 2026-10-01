package com.campinggearrental.state;
import com.campinggearrental.model.RentalOrder;

public class PendingState implements RentalState {
    @Override
    public void confirm(RentalOrder order) {
        order.setCurrentState(new ConfirmedState());
    }

    @Override
    public void rent(RentalOrder order) {
        throw new IllegalStateException("Đơn mới tạo, chưa xác nhận nên không thể giao thiết bị.");
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
