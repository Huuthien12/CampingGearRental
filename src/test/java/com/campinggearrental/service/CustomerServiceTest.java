package com.campinggearrental.service;

import com.campinggearrental.model.Customer;
import com.campinggearrental.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerServiceTest {
    @Test
    void trimsValuesAndCreatesCustomerWithSchemaCompatibleId() throws SQLException {
        InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
        CustomerService service = new CustomerService(repository);

        Customer added = service.add("  Nguyễn Văn A ", " 0900000001 ", " a@example.com ", " Đà Lạt ");

        assertEquals(20, added.id().length());
        assertEquals("Nguyễn Văn A", added.fullName());
        assertEquals("0900000001", added.phone());
        assertEquals(added, repository.customers.get(0));
    }

    @Test
    void rejectsBlankNameAndDuplicatePhone() {
        InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
        repository.customers.add(new Customer("CUS001", "Existing", "0900000001", "", ""));
        CustomerService service = new CustomerService(repository);

        assertThrows(IllegalArgumentException.class,
                () -> service.add(" ", "0900000002", "", ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.add("New", " ", "", ""));
        assertThrows(IllegalArgumentException.class,
                () -> service.add("New", "0900000001", "", ""));
    }

    @Test
    void updateAllowsTheCustomersExistingPhone() throws SQLException {
        InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
        Customer existing = new Customer("CUS001", "Existing", "0900000001", "", "");
        repository.customers.add(existing);

        new CustomerService(repository).update(new Customer("CUS001", "Updated", "0900000001", "", ""));

        assertEquals("Updated", repository.customers.getFirst().fullName());
    }

    @Test
    void searchTreatsNullAndBlankKeywordsAsFindAll() throws SQLException {
        InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
        repository.customers.add(new Customer("CUS001", "Existing", "0900000001", "", ""));
        CustomerService service = new CustomerService(repository);

        assertEquals(repository.customers, service.search(null));
        assertEquals(repository.customers, service.search("  "));
    }

    @Test
    void deleteRejectsBlankId() {
        CustomerService service = new CustomerService(new InMemoryCustomerRepository());

        assertThrows(IllegalArgumentException.class, () -> service.delete("  "));
    }

    private static class InMemoryCustomerRepository implements CustomerRepository {
        private final List<Customer> customers = new ArrayList<>();

        @Override
        public List<Customer> findAll() {
            return List.copyOf(customers);
        }

        @Override
        public List<Customer> search(String keyword) {
            return customers.stream()
                    .filter(customer -> customer.fullName().contains(keyword) || customer.phone().contains(keyword))
                    .toList();
        }

        @Override
        public Optional<Customer> findByPhone(String phone) {
            return customers.stream().filter(customer -> customer.phone().equals(phone)).findFirst();
        }

        @Override
        public void insert(Customer customer) {
            customers.add(customer);
        }

        @Override
        public void update(Customer customer) {
            customers.removeIf(existing -> existing.id().equals(customer.id()));
            customers.add(customer);
        }

        @Override
        public void deleteById(String id) {
            customers.removeIf(customer -> customer.id().equals(id));
        }
    }
}
