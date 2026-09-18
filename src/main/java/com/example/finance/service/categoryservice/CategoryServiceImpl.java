package com.example.finance.service.categoryservice;

import com.example.finance.dto.categorydto.CategoryRequest;
import com.example.finance.dto.categorydto.CategoryResponse;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategoryIcon;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import com.example.finance.exception.BusinessConflictException;
import com.example.finance.repo.CategoryIconRepo;
import com.example.finance.repo.CategoryRepo;
import com.example.finance.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService{
    private final CategoryRepo categoryRepo;
    private final CategoryIconRepo categoryIconRepo;
    private final UserRepo userRepo;



    @Override
    public CategoryResponse getAll() {

        List<Category> allCategories = categoryRepo.findAll();

        List<CategoryResponse.CategoryData> incomeList = allCategories.stream()
                .filter(cat -> cat.getType() == CategoryName.INCOME)
                .map(this::toCategoryData)
                .toList();

        List<CategoryResponse.CategoryData> expenseList = allCategories.stream()
                .filter(cat -> cat.getType() == CategoryName.EXPENSE)
                .map(this::toCategoryData)
                .toList();

        return new CategoryResponse(incomeList, expenseList);
    }

    @Override
    public CategoryResponse findByType(String type) {
        CategoryName typeEnum = CategoryName.valueOf(type.toUpperCase());
        List<Category> categories = categoryRepo.findByType(typeEnum);

        List<CategoryResponse.CategoryData> dataList = categories.stream()
                .map(this::toCategoryData)
                .toList();

        if (typeEnum == CategoryName.INCOME) {
            return new CategoryResponse(dataList, List.of());
        } else {
            return new CategoryResponse(List.of(), dataList);
        }
    }

    @Override
    public CategoryResponse.CategoryData create(CategoryRequest request, String currentUser) {
        User user = userRepo.findByEmail(currentUser).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        //Kiểm tra name đã tồn tại
        if(categoryRepo.existsByNameAndUser(request.getName(), user)){
            throw new BusinessConflictException("Category name already exist");
        }

        String emoji = request.getEmoji();
        String iconUrl;
        CategoryIcon categoryIcon;

        //Case 1: có emoji -> map qua url
        if(emoji != null){
            emoji = emoji.trim();
            categoryIcon = categoryIconRepo.findByEmoji(emoji).orElse(null);
            if(categoryIcon!= null){
                iconUrl = categoryIcon.getIconUrl();
            }
        }

        //Case 2: Không có emoji, tìm bằng tên -> map qua url và emoji
        else {
            categoryIcon = categoryIconRepo.findByCategoryNameIgnoreCase(request.getName().trim()).orElse(null);
            if (categoryIcon != null) {
                emoji = categoryIcon.getEmoji();
                iconUrl = categoryIcon.getIconUrl();
            }
        }

        //Case 3: Không tìm thấy tên -> Gán cho other
        if (categoryIcon == null) {
            categoryIcon = categoryIconRepo.findByCategoryNameIgnoreCase("Other").orElse(null);
            if (categoryIcon != null) {
                emoji = categoryIcon.getEmoji();
                iconUrl = categoryIcon.getIconUrl();
            }
        }

        //Case 4: DB chưa có other, tạo mới và gán
        if (categoryIcon == null) {
            iconUrl = "https://cdn.example.com/icons/other.png";
            emoji = "👤";

            categoryIcon = new CategoryIcon();
            categoryIcon.setCategoryName(request.getName());
            categoryIcon.setEmoji(emoji);
            categoryIcon.setIconUrl(iconUrl);
            categoryIcon = categoryIconRepo.save(categoryIcon);
        }

        //Tạo Category và lưu
        Category category = new Category();
        category.setName(request.getName());

        CategoryName typeEnum = CategoryName.valueOf(request.getType().toUpperCase());
        category.setType(typeEnum);
        category.setCategoryIcon(categoryIcon);

        category.setUser(user);

        Category saved = categoryRepo.save(category);
        CategoryResponse.CategoryData data = toCategoryData(saved);

        return data;
    }


    private CategoryResponse.CategoryData toCategoryData(Category category){
        String emoji = (category.getCategoryIcon() != null) ? category.getCategoryIcon().getEmoji() : "👤"; // Icon mặc định
        String iconUrl = (category.getCategoryIcon() != null) ? category.getCategoryIcon().getIconUrl() : "https://cdn.example.com/icons/other.png";

        return new CategoryResponse.CategoryData(
                category.getId(),
                category.getName(),
                category.getType(),
                emoji,
                iconUrl
        );
    };

}
