package com.campinggearrental.state;
import com.campinggearrental.model.RentalOrder;

public class RentedState implements RentalState {
    @Override
    public void confirm(RentalOrder order) {
        throw new IllegalStateException("Đơn đã giao, không thể xác nhận lại.");
    }

    @Override
    public void rent(RentalOrder order) {
        throw new IllegalStateException("Đơn đã giao rồi.");
    }

    @Override
    public void returnEquipment(RentalOrder order) {
        System.out.println("Khách đang trả thiết bị. Đang hoàn kho...");
        order.setCurrentState(new ReturnedState());
    }

    @Override
    public void cancel(RentalOrder order) {
        throw new IllegalStateException("Thiết bị đang được khách thuê, tuyệt đối không được hủy ngang.");
    }
}