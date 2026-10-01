package com.campinggearrental.model;

public class Category {
    private String categoryId;
    private String name;

    public Category(String categoryId, String name) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Mã danh mục không được rỗng");
        }
        this.categoryId = categoryId.trim();
        if (this.categoryId.length() > 20) {
            throw new IllegalArgumentException("category id must not exceed 20 characters");
        }
        setName(name);
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được rỗng");
        }
        this.name = name.trim();
        if (this.name.length() > 100) {
            throw new IllegalArgumentException("category name must not exceed 100 characters");
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
