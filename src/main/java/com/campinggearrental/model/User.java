package com.campinggearrental.model;

public record User(String id, String username, String password, UserRole role, String customerId) {
}
