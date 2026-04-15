package com.tekup.quiz.model;

import java.util.Objects;

public class Category {
    private long id;
    private String name;

    public Category(long id, String name) {
        this.id = id;
        this.name = validateName(name);
    }

    public Category(String name) {
        this(0L, name);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = validateName(name);
    }

    private String validateName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Category other)) {
            return false;
        }
        return id == other.id && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
