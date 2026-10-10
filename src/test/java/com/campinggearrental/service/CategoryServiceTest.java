package com.campinggearrental.service;

import static org.junit.jupiter.api.Assertions.*;

import com.campinggearrental.model.Category;
import com.campinggearrental.repository.CategoryRepository;
import com.campinggearrental.repository.JdbcCategoryRepository;
import com.campinggearrental.web.WebConfiguration;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class CategoryServiceTest {
    @Test void listsAndFindsCategoriesWithoutInventingMissingData() throws Exception {
        Category category = new Category("CAT001", "Tent");
        CategoryService service = new CategoryService(new CategoryRepository() {
            public List<Category> findAll() { return List.of(category); }
            public Optional<Category> findById(String id) { return "CAT001".equals(id) ? Optional.of(category) : Optional.empty(); }
        });
        assertSame(category, service.list().getFirst());
        assertSame(category, service.findById(" CAT001 ").orElseThrow());
        assertTrue(service.findById("unknown").isEmpty());
        assertTrue(service.findById(null).isEmpty());
        assertTrue(service.findById(" ").isEmpty());
    }

    @Test void propagatesPersistenceFailureToWebFeedbackLayer() {
        CategoryService service = new CategoryService(new CategoryRepository() {
            public List<Category> findAll() throws SQLException { throw new SQLException("read failed"); }
            public Optional<Category> findById(String id) throws SQLException { throw new SQLException("read failed"); }
        });
        assertThrows(SQLException.class, service::list);
        assertThrows(SQLException.class, () -> service.findById("CAT001"));
    }

    @Test void foundationWiresCategoryBeansAlongsideExistingServices() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WebConfiguration.class)) {
            assertInstanceOf(JdbcCategoryRepository.class, context.getBean(CategoryRepository.class));
            assertNotNull(context.getBean(CategoryService.class));
            assertNotNull(context.getBean(EquipmentService.class));
            assertNotNull(context.getBean(RentalService.class));
            assertNotNull(context.getBean(PersistedCheckoutService.class));
        }
    }
}
