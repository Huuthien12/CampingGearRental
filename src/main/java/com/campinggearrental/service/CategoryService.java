package com.campinggearrental.service;

import com.campinggearrental.model.Category;
import com.campinggearrental.repository.CategoryRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<Category> list() throws SQLException { return repository.findAll(); }

    public Optional<Category> findById(String id) throws SQLException {
        if (id == null || id.isBlank()) return Optional.empty();
        return repository.findById(id.trim());
    }
}
