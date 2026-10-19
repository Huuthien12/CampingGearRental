package com.campinggearrental.service;

import com.campinggearrental.repository.UserRepository;
import com.campinggearrental.model.Customer;
import com.campinggearrental.model.User;
import com.campinggearrental.model.UserRole;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticate(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username.trim())
                .filter(user -> passwordEncoder.matches(password, user.password()));
    }

    public boolean login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) return false;
        return userRepository.authenticate(username.trim(), password);
    }

    public User registerCustomer(String username, String password, String fullName, String phone,
                                 String email, String address) throws SQLException {
        String normalizedUsername = normalize(username);
        String normalizedName = normalize(fullName);
        String normalizedPhone = normalize(phone);
        if (normalizedUsername.isBlank() || normalizedName.isBlank() || normalizedPhone.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập, họ tên và số điện thoại là bắt buộc.");
        }
        if (normalizedUsername.length() > 50 || normalizedName.length() > 100 || normalizedPhone.length() > 20
                || normalize(email).length() > 255 || normalize(address).length() > 255) {
            throw new IllegalArgumentException("Thông tin đăng ký vượt quá độ dài cho phép.");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");
        }
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng.");
        }
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        Customer customer = new Customer("CUS" + suffix, normalizedName, normalizedPhone,
                normalize(email), normalize(address));
        User user = new User("USR" + suffix, normalizedUsername, passwordEncoder.encode(password),
                UserRole.CUSTOMER, customer.id());
        userRepository.registerCustomer(customer, user);
        return user;
    }

    private String normalize(String value) { return value == null ? "" : value.trim(); }
}
