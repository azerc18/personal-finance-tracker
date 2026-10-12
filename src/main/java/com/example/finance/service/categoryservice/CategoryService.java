package com.example.finance.service.categoryservice;

import com.example.finance.dto.categorydto.CategoryRequest;
import com.example.finance.dto.categorydto.CategoryResponse;

public interface CategoryService {
    CategoryResponse getAll(String email);
    CategoryResponse findByType(String type, String email);
    CategoryResponse.CategoryData create(CategoryRequest request, String userEmail);

}
