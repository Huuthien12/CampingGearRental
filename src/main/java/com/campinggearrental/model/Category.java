package com.campinggearrental.model;

public class Category {
    private String categoryId;
    private String name;

    public Category(String categoryId, String name) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("Mã danh mục không được rỗng");
        }
        this.categoryId = categoryId.trim();
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
    }

    @Override
    public String toString() {
        return name;
    }
}
