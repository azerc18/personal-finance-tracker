package com.example.finance.service;

import com.example.finance.dto.budgetdto.BudgetRequest;
import com.example.finance.dto.budgetdto.BudgetResponse;
import com.example.finance.dto.budgetdto.UpdateAmountRequest;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategoryIcon;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import com.example.finance.exception.BusinessConflictException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repo.BudgetRepo;
import com.example.finance.repo.CategoryRepo;
import com.example.finance.repo.UserRepo;
import com.example.finance.service.budgetservice.BudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgeServiceTest {
    @Nested
    @DisplayName("BudgetService – Business Logic")
    class BudgetServiceTest {

        @Mock
        BudgetRepo budgetRepository;
        @Mock
        CategoryRepo categoryRepository;
        @Mock
        UserRepo userRepo;

        @InjectMocks
        BudgetServiceImpl budgetService;

        private User user;
        private Category expenseCategory;
        private Category incomeCategory;

        @BeforeEach
        void setUp() {
            user = new User();
            user.setId(1L);

            CategoryIcon icon = new CategoryIcon();
            icon.setEmoji("🏠");
            icon.setIconUrl("https://cdn.example.com/housing.png");

            expenseCategory = new Category();
            expenseCategory.setId(3L);
            expenseCategory.setName("Housing");
            expenseCategory.setType(CategoryName.EXPENSE);
            expenseCategory.setCategoryIcon(icon);

            incomeCategory = new Category();
            incomeCategory.setId(4L);
            incomeCategory.setName("Salary");
            incomeCategory.setType(CategoryName.INCOME);
            incomeCategory.setCategoryIcon(icon);
        }

        /**
         * TC-BUDGET-CREATE-01 – Tạo budget thành công
         */
        @Test
        @DisplayName("TC-BUDGET-CREATE-01: Tạo budget thành công trả về đủ fields")
        void createBudget_valid_returnsFullResponse() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(categoryRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(expenseCategory));

            when(budgetRepository.findByCategoryIdAndMonthAndYearAndUser(eq(3L), eq(4), eq(2024), eq(user)))
                    .thenReturn(Optional.empty());

            when(budgetRepository.save(any())).thenAnswer(inv -> {
                Budget b = inv.getArgument(0);
                b.setId(15L);
                return b;
            });

            BudgetRequest req = new BudgetRequest(3L, BigDecimal.valueOf(2500.0), 4, 2024);
            BudgetResponse res = budgetService.create(req, user.getEmail());

            assertThat(res.getId()).isEqualTo(15L);
            assertThat(res.getAmount().equals(BigDecimal.valueOf(2500.0)));
            assertThat(res.getMonth()).isEqualTo(4);
            assertThat(res.getYear()).isEqualTo(2024);
            assertThat(res.getCategory().getName()).isEqualTo("Housing");
        }

        /**
         * TC-BUDGET-CREATE-02 – Budget đã tồn tại → cập nhật thay vì tạo mới
         */
        @Test
        @DisplayName("TC-BUDGET-CREATE-02: Budget đã tồn tại thì update, không tạo bản ghi mới")
        void createBudget_alreadyExists_updatesInsteadOfCreating() {
            Budget existing = new Budget();
            existing.setId(15L);
            existing.setAmount(BigDecimal.valueOf(2500.0));
            existing.setCategory(expenseCategory);
            existing.setMonth(4);
            existing.setYear(2024);
            existing.setUser(user);
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(categoryRepository.findByIdAndUser(3L, user)).thenReturn(Optional.of(expenseCategory));
            when(budgetRepository.findByCategoryIdAndMonthAndYearAndUser(eq(3L), eq(4), eq(2024), eq(user)))
                    .thenReturn(Optional.of(existing)); // Đã tồn tại
            when(budgetRepository.save(any())).thenReturn(existing);

            BudgetRequest req = new BudgetRequest(3L, BigDecimal.valueOf(3000.0), 4, 2024);
            BudgetResponse res = budgetService.create(req, user.getEmail());

            // Phải update amount, không tạo bản ghi mới
            verify(budgetRepository, times(1)).save(argThat(b -> Objects.equals(b.getAmount(), BigDecimal.valueOf(3000.0))));
            assertThat(res.getAmount()).isEqualTo(BigDecimal.valueOf(3000.0));
        }

        /**
         * TC-BUDGET-CREATE-07 – categoryId là INCOME → BadRequestException
         */
        @Test
        @DisplayName("TC-BUDGET-CREATE-07: categoryId là INCOME ném BadRequestException")
        void createBudget_incomeCategoryId_throwsBadRequest() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(categoryRepository.findByIdAndUser(4L,user)).thenReturn(Optional.of(incomeCategory));

            BudgetRequest req = new BudgetRequest(4L, BigDecimal.valueOf(2500.0), 4, 2024);

            assertThatThrownBy(() -> budgetService.create(req, user.getEmail()))
                    .isInstanceOf(BusinessConflictException.class)
                    .hasMessageContaining("EXPENSE");

            verify(budgetRepository, never()).save(any());
        }

        /**
         * TC-BUDGET-LIST-01 – Lấy danh sách budget theo tháng/năm
         */
        @Test
        @DisplayName("TC-BUDGET-LIST-01: Lấy budget tháng 4/2024 trả về list đúng")
        void getBudgets_validMonthYear_returnsList() {
            Budget b = new Budget();
            b.setId(15L);
            b.setAmount(BigDecimal.valueOf(2500.0));
            b.setCategory(expenseCategory);
            b.setMonth(4);
            b.setYear(2024);
            b.setUser(user);
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(budgetRepository.findByMonthAndYearAndUser(eq(4), eq(2024), eq(user))).thenReturn(List.of(b));

            List<BudgetResponse> result = budgetService.getAll(4, 2024, user.getEmail());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(15L);
            assertThat(result.get(0).getCategory().getName()).isEqualTo("Housing");
            assertThat(result.get(0).getCategory().getIconUrl()).isNotBlank();
        }

        /**
         * TC-BUDGET-LIST-02 – Không có budget trong tháng → list rỗng
         */
        @Test
        @DisplayName("TC-BUDGET-LIST-02: Không có budget trong tháng trả về list rỗng")
        void getBudgets_noData_returnsEmptyList() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(budgetRepository.findByMonthAndYearAndUser(eq(5), eq(2024), eq(user))).thenReturn(List.of());

            List<BudgetResponse> result = budgetService.getAll(5, 2024, user.getEmail());

            assertThat(result).isEmpty();
        }

        /**
         * TC-BUDGET-LIST-05 – Chỉ trả về budget của user đang đăng nhập
         */
        @Test
        @DisplayName("TC-BUDGET-LIST-05: Query budget theo đúng user_id")
        void getBudgets_queriesCorrectUserId() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(budgetRepository.findByMonthAndYearAndUser(eq(4), eq(2024), eq(user))).thenReturn(List.of());

            budgetService.getAll(4, 2024, user.getEmail());

            verify(budgetRepository).findByMonthAndYearAndUser(eq(4), eq(2024), eq(user));
        }

        /**
         * TC-BUDGET-DELETE-01 – Xoá budget thành công
         */
        @Test
        @DisplayName("TC-BUDGET-DELETE-01: Xoá budget thành công không ném exception")
        void deleteBudget_success_doesNotThrow() {
            Budget b = new Budget();
            b.setId(15L);
            b.setUser(user);
            when(budgetRepository.findByIdAndUser(15L, user)).thenReturn(Optional.of(b));
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatCode(() -> budgetService.delete(15L, user.getEmail())).doesNotThrowAnyException();
            verify(budgetRepository).delete(b);
        }

        /**
         * TC-BUDGET-DELETE-02 – ID không tồn tại → 404
         */
        @Test
        @DisplayName("TC-BUDGET-DELETE-02: ID không tồn tại ném ResourceNotFoundException")
        void deleteBudget_notFound_throwsNotFoundException() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(budgetRepository.findByIdAndUser(9999L,user)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.delete(9999L, user.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Budget not found");
        }

        /**
         * TC-BUDGET-DELETE-03 – Budget thuộc user khác → 404 (không lộ thông tin)
         */
        @Test
        @DisplayName("TC-BUDGET-DELETE-03: Budget thuộc user khác cũng ném 404 (không lộ thông tin)")
        void deleteBudget_belongsToOtherUser_throwsNotFound() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(budgetRepository.findByIdAndUser(eq(15L), eq(user)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.delete(15L, user.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Budget not found"); // Cùng message, không lộ lý do thật
        }

        /**
         * TC-BUDGET-UPDATE-01 – Cập nhật budget thành công
         */
        @Test
        @DisplayName("TC-BUDGET-UPDATE-01: Cập nhật amount thành công")
        void updateBudget_success_returnsUpdatedAmount() {
            Budget existing = new Budget();
            existing.setId(15L);
            existing.setAmount(BigDecimal.valueOf(2500.0));
            existing.setCategory(expenseCategory);
            existing.setMonth(4);
            existing.setYear(2024);
            existing.setUser(user);
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(budgetRepository.findByIdAndUser(15L, user)).thenReturn(Optional.of(existing));
            when(budgetRepository.save(any())).thenReturn(existing);

            UpdateAmountRequest req = new UpdateAmountRequest(BigDecimal.valueOf(3000.0));
            BudgetResponse res = budgetService.update(15L, req, user.getEmail());

            assertThat(res.getAmount()).isEqualTo(BigDecimal.valueOf(3000.0));
            verify(budgetRepository).save(argThat(b -> b.getAmount().equals(BigDecimal.valueOf(3000.0))));
        }

        /**
         * TC-BUDGET-UPDATE-04 – Budget thuộc user khác → 404
         */
        @Test
        @DisplayName("TC-BUDGET-UPDATE-04: Budget thuộc user khác ném ResourceNotFoundException")
        void updateBudget_belongsToOtherUser_throwsNotFound() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(budgetRepository.findByIdAndUser(eq(15L), eq(user)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.update(15L, new UpdateAmountRequest(BigDecimal.valueOf(3000.0)), user.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Budget not found");
        }
    }
}