package com.campinggearrental.service;

import com.campinggearrental.model.Customer;
import com.campinggearrental.repository.CustomerRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> findAll() throws SQLException {
        return customerRepository.findAll();
    }

    public List<Customer> search(String keyword) throws SQLException {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return normalizedKeyword.isEmpty() ? findAll() : customerRepository.search(normalizedKeyword);
    }

    public Customer add(String fullName, String phone, String email, String address) throws SQLException {
        Customer customer = new Customer("CUS" + UUID.randomUUID().toString().replace("-", "").substring(0, 17),
                normalize(fullName), normalize(phone), normalize(email), normalize(address));
        validate(customer);
        ensurePhoneAvailable(customer);
        customerRepository.insert(customer);
        return customer;
    }

    public void update(Customer customer) throws SQLException {
        if (customer == null || customer.id() == null || customer.id().isBlank()) {
            throw new IllegalArgumentException("Khách hàng cần có mã hợp lệ.");
        }
        Customer normalized = new Customer(customer.id().trim(), normalize(customer.fullName()),
                normalize(customer.phone()), normalize(customer.email()), normalize(customer.address()));
        validate(normalized);
        ensurePhoneAvailable(normalized);
        customerRepository.update(normalized);
    }

    public void delete(String id) throws SQLException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn khách hàng cần xóa.");
        }
        customerRepository.deleteById(id.trim());
    }

    private void ensurePhoneAvailable(Customer customer) throws SQLException {
        customerRepository.findByPhone(customer.phone()).ifPresent(existing -> {
            if (!existing.id().equals(customer.id())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
            }
        });
    }

    private void validate(Customer customer) {
        if (customer.fullName().isBlank()) {
            throw new IllegalArgumentException("Tên khách hàng không được để trống.");
        }
        if (customer.phone().isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (customer.id().length() > 20) {
            throw new IllegalArgumentException("Mã khách hàng vượt quá 20 ký tự.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}