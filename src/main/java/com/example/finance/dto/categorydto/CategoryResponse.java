package com.example.finance.dto.categorydto;

import com.example.finance.enums.CategoryName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private List<CategoryData> income;
    private List<CategoryData> expense;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryData {
        private Long id;
        private String name;
        private CategoryName type;
        private String icon;
        private String iconUrl;
    }

}
