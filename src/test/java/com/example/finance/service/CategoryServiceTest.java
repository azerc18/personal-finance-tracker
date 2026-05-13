package com.example.finance.service;

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
import com.example.finance.service.categoryservice.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service Test – CategoryService
 * Phạm vi: logic map emoji/icon, kiểm tra trùng tên, category gắn đúng user.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService – Business Logic")
class CategoryServiceTest {

    @Mock
    CategoryRepo categoryRepository;
    @Mock
    CategoryIconRepo categoryIconRepository;
    @Mock
    UserRepo userRepo;

    @InjectMocks
    CategoryServiceImpl categoryService;

    private User currentUser;
    private CategoryIcon transportIcon;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("user@example.com");

        transportIcon = new CategoryIcon();
        transportIcon.setId(1L);
        transportIcon.setCategoryName("Transportation");
        transportIcon.setEmoji("🚗");
        transportIcon.setIconUrl("https://cdn.example.com/icons/transport.png");
    }

    // =========================================================
    // CREATE CATEGORY
    // =========================================================
    @Nested
    @DisplayName("createCategory() – Tạo danh mục mới")
    class CreateCategory {

        /**
         * TC-CAT-CREATE-01 – Không có emoji → backend tự tìm icon theo name
         */
        @Test
        @DisplayName("TC-CAT-CREATE-01: Không có emoji thì backend tự map icon từ name")
        void createCategory_noEmoji_backendMapsIconFromName() {
            when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(currentUser));
            when(categoryRepository.existsByNameAndUser("Transportation", currentUser)).thenReturn(false);
            when(categoryIconRepository.findByCategoryNameIgnoreCase("Transportation"))
                    .thenReturn(Optional.of(transportIcon));
            when(categoryRepository.save(any())).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(8L);
                return c;
            });

            CategoryRequest req = new CategoryRequest("Transportation", "EXPENSE", null);
            CategoryResponse.CategoryData res = categoryService.create(req, currentUser.getEmail());

            assertThat(res.getId()).isGreaterThan(0);
            assertThat(res.getName()).isEqualTo("Transportation");
            assertThat(res.getType().name()).isEqualTo("EXPENSE");
            assertThat(res.getIcon()).isEqualTo("🚗");
            assertThat(res.getIconUrl()).isEqualTo("https://cdn.example.com/icons/transport.png");
        }

        /**
         * TC-CAT-CREATE-02 – Có emoji → backend lấy iconUrl từ bảng category_icons theo emoji
         */
        @Test
        @DisplayName("TC-CAT-CREATE-02: Có emoji thì backend map iconUrl từ bảng category_icons")
        void createCategory_withEmoji_backendMapsIconUrlFromEmoji() {
            when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(currentUser));
            when(categoryRepository.existsByNameAndUser("Transportation", currentUser)).thenReturn(false);
            when(categoryIconRepository.findByEmoji("🚗")).thenReturn(Optional.of(transportIcon));
            when(categoryRepository.save(any())).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(8L);
                return c;
            });

            CategoryRequest req = new CategoryRequest("Transportation", "EXPENSE", "🚗");
            CategoryResponse.CategoryData res = categoryService.create(req, currentUser.getEmail());

            assertThat(res.getIcon()).isEqualTo("🚗");
            assertThat(res.getIconUrl()).isEqualTo("https://cdn.example.com/icons/transport.png");
            // Verify dùng emoji lookup, không phải name lookup
            verify(categoryIconRepository).findByEmoji("🚗");
            verify(categoryIconRepository, never()).findByCategoryNameIgnoreCase(any());
        }

        /**
         * TC-CAT-CREATE-03 – Tên đã tồn tại → ConflictException
         */
        @Test
        @DisplayName("TC-CAT-CREATE-03: Tên category đã tồn tại ném ConflictException")
        void createCategory_duplicateName_throwsConflict() {
            when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(currentUser));

            when(categoryRepository.existsByNameAndUser("Housing", currentUser)).thenReturn(true);

            CategoryRequest req = new CategoryRequest("Housing", "EXPENSE", null);

            assertThatThrownBy(() -> categoryService.create(req, currentUser.getEmail()))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessage("Category name already exist");

            verify(categoryRepository, never()).save(any());
        }

        /**
         * TC-CAT-CREATE-09 – Category được gắn với user đang đăng nhập
         */
        @Test
        @DisplayName("TC-CAT-CREATE-09: Category được gắn với user_id của người tạo")
        void createCategory_savedWithCorrectUserId() {
            when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(currentUser));
            when(categoryRepository.existsByNameAndUser(any(), eq(currentUser))).thenReturn(false);
            when(categoryIconRepository.findByCategoryNameIgnoreCase(any()))
                    .thenReturn(Optional.of(transportIcon));
            when(categoryRepository.save(any())).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });

            categoryService.create(new CategoryRequest("Food", "EXPENSE", null), currentUser.getEmail());

            verify(categoryRepository).save(argThat(c ->
                    c.getUser().getId().equals(1L) // Gắn đúng user
            ));
        }

        @Test
        @DisplayName("Không tìm thấy icon phù hợp → dùng icon mặc định")
        void createCategory_noMatchingIcon_usesDefaultIcon() {
            when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(currentUser));
            when(categoryRepository.existsByNameAndUser(any(), any())).thenReturn(false);
            when(categoryIconRepository.findByCategoryNameIgnoreCase("UnknownCat"))
                    .thenReturn(Optional.empty());
            when(categoryRepository.save(any())).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });

            CategoryRequest req = new CategoryRequest("UnknownCat", "EXPENSE", null);
            CategoryResponse.CategoryData res = categoryService.create(req, currentUser.getEmail());

            // Icon mặc định khi không map được
            assertThat(res.getIcon()).isNotNull();
        }
    }

    // =========================================================
    // GET ALL CATEGORIES
    // =========================================================
    @Nested
    @DisplayName("getAllCategories() – Lấy danh sách danh mục")
    class GetAllCategories {

        /**
         * TC-CAT-LIST-01 – Không filter → trả về Map với 2 key EXPENSE và INCOME
         */
        @Test
        @DisplayName("TC-CAT-LIST-01: Không filter trả về Map có cả EXPENSE và INCOME")
        void getAllCategories_noFilter_returnsBothGroups() {
            Category expense = buildCategory(1L, "Food", "EXPENSE");
            Category income  = buildCategory(4L, "Salary", "INCOME");
            when(categoryRepository.findAll()).thenReturn(List.of(expense, income));

            CategoryResponse result = categoryService.getAll();

            assertThat(result).isInstanceOf(CategoryResponse.class);

            assertThat(result.getExpense()).hasSize(1);
            assertThat(result.getExpense().get(0).getName()).isEqualTo("Food");

            assertThat(result.getIncome()).hasSize(1);
            assertThat(result.getIncome().get(0).getName()).isEqualTo("Salary");
        }

        /**
         * TC-CAT-LIST-02 – Filter EXPENSE → trả về List chỉ EXPENSE
         */
        @Test
        @DisplayName("TC-CAT-LIST-02: Filter EXPENSE trả về List chỉ chứa EXPENSE")
        void getAllCategories_filterExpense_returnsOnlyExpense() {
            Category food     = buildCategory(1L, "Food",     "EXPENSE");
            Category shopping = buildCategory(2L, "Shopping", "EXPENSE");
            when(categoryRepository.findByType(CategoryName.valueOf("EXPENSE"))).thenReturn(List.of(food, shopping));

            CategoryResponse result = categoryService.findByType("EXPENSE");

            assertThat(result).isInstanceOf(CategoryResponse.class);
            assertThat(result.getExpense()).hasSize(2);
            assertThat(result.getExpense()).allMatch(c -> c.getType() == CategoryName.EXPENSE);
            assertThat(result.getIncome()).isEmpty();
        }

        /**
         * TC-CAT-LIST-04 – Không có category khớp type → trả về List rỗng
         */
        @Test
        @DisplayName("TC-CAT-LIST-04: Không có category khớp type trả về list rỗng")
        void getAllCategories_noMatch_returnsEmptyList() {
            when(categoryRepository.findByType(CategoryName.INCOME)).thenReturn(List.of());

            CategoryResponse result = categoryService.findByType("INCOME");

            assertThat(result).isExactlyInstanceOf(CategoryResponse.class);

            assertThat(result.getIncome()).isEmpty();
            assertThat(result.getExpense()).isEmpty();
        }

        /**
         * TC-CAT-LIST-06 – Mỗi item phải có iconUrl
         */
        @Test
        @DisplayName("TC-CAT-LIST-06: Mỗi category trả về phải có iconUrl không rỗng")
        void getAllCategories_eachItemHasIconUrl() {
            Category food = buildCategory(1L, "Food", "EXPENSE");
            food.getCategoryIcon().setIconUrl("https://cdn.example.com/food.png");

            when(categoryRepository.findByType(CategoryName.EXPENSE)).thenReturn(List.of(food));

            CategoryResponse result = categoryService.findByType("EXPENSE");

            assertThat(result.getExpense()).allMatch(c -> c.getIconUrl() != null && !c.getIconUrl().isBlank());
        }

        private Category buildCategory(Long id, String name, String type) {
            CategoryIcon icon = new CategoryIcon();
            icon.setEmoji("🔖");
            icon.setIconUrl("https://cdn.example.com/" + name.toLowerCase() + ".png");

            Category c = new Category();
            c.setId(id);
            c.setName(name);
            c.setType(CategoryName.valueOf(type));
            c.setCategoryIcon(icon);
            return c;
        }
    }
}