package com.campinggearrental.state;
import com.campinggearrental.model.RentalOrder;

public class ReturnedState implements RentalState {
    @Override
    public void confirm(RentalOrder order) { throw new IllegalStateException("Đơn đã hoàn tất."); }
    @Override
    public void rent(RentalOrder order) { throw new IllegalStateException("Đơn đã hoàn tất."); }
    @Override
    public void returnEquipment(RentalOrder order) { throw new IllegalStateException("Đơn đã hoàn tất."); }
    @Override
    public void cancel(RentalOrder order) { throw new IllegalStateException("Đơn đã hoàn tất."); }
}