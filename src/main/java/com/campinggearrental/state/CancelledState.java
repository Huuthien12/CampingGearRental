package com.campinggearrental.state;
import com.campinggearrental.model.RentalOrder;

public class CancelledState implements RentalState {
    @Override
    public void confirm(RentalOrder order) { throw new IllegalStateException("Đơn đã hủy."); }
    @Override
    public void rent(RentalOrder order) { throw new IllegalStateException("Đơn đã hủy."); }
    @Override
    public void returnEquipment(RentalOrder order) { throw new IllegalStateException("Đơn đã hủy."); }
    @Override
    public void cancel(RentalOrder order) { throw new IllegalStateException("Đơn đã hủy."); }
}