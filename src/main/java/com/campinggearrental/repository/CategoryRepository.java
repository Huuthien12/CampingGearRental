package com.campinggearrental.repository;

import com.campinggearrental.model.Category;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    List<Category> findAll() throws SQLException;
    Optional<Category> findById(String id) throws SQLException;
}
