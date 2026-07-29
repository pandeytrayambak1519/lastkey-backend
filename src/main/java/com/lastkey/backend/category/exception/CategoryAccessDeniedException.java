package com.lastkey.backend.category.exception;

public class CategoryAccessDeniedException extends RuntimeException {

    public CategoryAccessDeniedException(String message) {
        super(message);
    }
}