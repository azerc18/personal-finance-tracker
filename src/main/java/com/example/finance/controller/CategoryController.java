package com.example.finance.controller;


import com.example.finance.dto.ApiResponse;
import com.example.finance.dto.categorydto.CategoryRequest;
import com.example.finance.dto.categorydto.CategoryResponse;
import com.example.finance.service.categoryservice.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PreAuthorize("hasAuthority('VIEW')")
    @GetMapping
    public ApiResponse<CategoryResponse> getAll(){
        CategoryResponse list = categoryService.getAll();

        return new ApiResponse<CategoryResponse>(true, "Category list fetched successfully", list);
    }

    @PreAuthorize("hasAuthority('VIEW')")
    @GetMapping({"/search"})
    public ApiResponse<CategoryResponse> getByType (@RequestParam String type){
        CategoryResponse list = categoryService.findByType(type.toUpperCase());

        return new ApiResponse<>(true, "Category list fetched successfully", list);
    }

    @PreAuthorize("hasAuthority('CREATE')")
    @PostMapping
    public ApiResponse<CategoryResponse.CategoryData> create(@Valid @RequestBody CategoryRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails){

        CategoryResponse.CategoryData response = categoryService.create(request, userDetails.getUsername());
        return new ApiResponse<>(true, "Category created successfully", response);
    }

}
