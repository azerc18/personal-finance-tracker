package com.example.finance.service;

import com.example.finance.dto.dashboarddto.DashboardResponse;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategoryIcon;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import com.example.finance.repo.TransactionRepo;
import com.example.finance.repo.UserRepo;
import com.example.finance.service.dashboardservice.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service Test – TransactionService & DashboardService
 * Phạm vi: suy ra type từ category, gắn user_id, tính balance, giới hạn recent 3.
 */
@ExtendWith(MockitoExtension.class)
    @Nested
    @DisplayName("DashboardService – Business Logic")
    class DashboardServiceTest {

        @Mock TransactionRepo transactionRepository;
        @Mock
        UserRepo userRepo;
        @InjectMocks
        DashboardServiceImpl dashboardService;

        private User user;

        @BeforeEach
        void setUp() {
            user = new User();
            user.setId(1L);
            user.setEmail("user@gmail.com");
        }

        /**
         * TC-DASH-02 – balance = income - expenses
         */
        @Test
        @DisplayName("TC-DASH-02: balance = income - expenses tính chính xác")
        void getDashboard_balanceEqualsIncomeMinusExpenses() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(transactionRepository.getTotalExpense(user)).thenReturn(BigDecimal.valueOf(800.0));
            when(transactionRepository.getTotalIncome(user)).thenReturn(BigDecimal.valueOf(2000.0));

            when(transactionRepository.getPieChart(user)).thenReturn(List.of());
            when(transactionRepository.findTop3ByUserOrderByDateDesc(user)).thenReturn(List.of());

            DashboardResponse res = dashboardService.getDashboard(user.getEmail());

            assertThat(res.getIncome()).isEqualTo(BigDecimal.valueOf(2000.0));
            assertThat(res.getExpenses()).isEqualTo(BigDecimal.valueOf(800.0));
            assertThat(res.getBalance()).isEqualTo(BigDecimal.valueOf(1200.0)); // 2000 - 800
        }

        /**
         * TC-DASH-04 – recentTransactions tối đa 3 phần tử
         */
        @Test
        @DisplayName("TC-DASH-04: recentTransactions tối đa 3 phần tử dù có nhiều hơn")
        void getDashboard_recentTransactionsMaxThree() {
            List<Transaction> fiveTransactions = List.of(
                    buildTransaction(BigDecimal.valueOf(100.0), "EXPENSE", LocalDate.of(2024, 4, 28)),
                    buildTransaction(BigDecimal.valueOf(200.0), "EXPENSE", LocalDate.of(2024, 4, 27)),
                    buildTransaction(BigDecimal.valueOf(300.0), "INCOME",  LocalDate.of(2024, 4, 26))
            );

            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(transactionRepository.findTop3ByUserOrderByDateDesc(eq(user)))
                    .thenReturn(fiveTransactions);

            DashboardResponse res = dashboardService.getDashboard(user.getEmail());

            assertThat(res.getRecentTransactions()).hasSize(3);
        }

        /**
         * TC-DASH-05 – recentTransactions sắp xếp theo date DESC
         */
        @Test
        @DisplayName("TC-DASH-05: recentTransactions sắp xếp date DESC (mới nhất đứng đầu)")
        void getDashboard_recentTransactionsSortedByDateDesc() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(transactionRepository.getTotalIncome(eq(user)))
                    .thenReturn(BigDecimal.valueOf(2000.0));
            when(transactionRepository.getTotalExpense(eq(user)))
                    .thenReturn(BigDecimal.valueOf(800.0));


            Transaction t1 = buildTransaction(BigDecimal.valueOf(100.0), "EXPENSE", LocalDate.of(2024, 4, 28)); // mới nhất
            Transaction t2 = buildTransaction(BigDecimal.valueOf(200.0), "INCOME",  LocalDate.of(2024, 4, 26));
            Transaction t3 = buildTransaction(BigDecimal.valueOf(300.0), "EXPENSE", LocalDate.of(2024, 4, 22)); // cũ nhất


            when(transactionRepository.findTop3ByUserOrderByDateDesc(user))
                    .thenReturn(List.of(t1, t2, t3));

            DashboardResponse res = dashboardService.getDashboard(user.getEmail());

            List<LocalDate> actualDates = res.getRecentTransactions().stream()
                    .map(DashboardResponse.recentTransactionsData::getDate)
                    .toList();

            assertThat(actualDates).containsExactly(
                    LocalDate.of(2024, 4, 28),
                    LocalDate.of(2024, 4, 26),
                    LocalDate.of(2024, 4, 22)
            );

            assertThat(actualDates).isSortedAccordingTo(Comparator.reverseOrder());
        }

        /**
         * TC-DASH-06 – pieChart gộp đúng giao dịch cùng category
         */
        @Test
        @DisplayName("TC-DASH-06: pieChart gộp 2 giao dịch cùng category Housing thành 1 entry")
        void getDashboard_pieChartGroupsByCategory() {
            Category housing = buildCategory("Housing", "EXPENSE");
            Transaction t1 = buildTransactionWithCategory(BigDecimal.valueOf(500.0), housing, LocalDate.of(2024, 4, 28));
            Transaction t2 = buildTransactionWithCategory(BigDecimal.valueOf(300.0), housing, LocalDate.of(2024, 4, 27));
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            DashboardResponse.pieChartData mockPieData = new DashboardResponse.pieChartData("Housing", BigDecimal.valueOf(800.0), CategoryName.EXPENSE);

            when(transactionRepository.findTop3ByUserOrderByDateDesc(eq(user)))
                    .thenReturn(List.of(t1, t2));
            when(transactionRepository.getPieChart(user))
                    .thenReturn(List.of(mockPieData));

            DashboardResponse res = dashboardService.getDashboard(user.getEmail());

            assertThat(res.getPieChart()).hasSize(1);
            assertThat(res.getPieChart().get(0).getCategory()).isEqualTo("Housing");
            assertThat(res.getPieChart().get(0).getAmount()).isEqualTo(BigDecimal.valueOf(800.0)); // 500 + 300
        }

        /**
         * TC-DASH-10 – User không có giao dịch → income=0, expenses=0, balance=0
         */
        @Test
        @DisplayName("TC-DASH-10: Không có giao dịch trong tháng trả về tất cả 0")
        void getDashboard_noTransactions_returnsAllZero() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(transactionRepository.getTotalIncome(eq(user)))
                    .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.getTotalExpense(eq(user)))
                    .thenReturn(BigDecimal.ZERO);

            when(transactionRepository.getPieChart(user)).thenReturn(List.of());

            when(transactionRepository.findTop3ByUserOrderByDateDesc(user)).thenReturn(List.of());

            DashboardResponse res = dashboardService.getDashboard(user.getEmail());

            assertThat(res.getIncome()).isEqualTo(BigDecimal.ZERO);
            assertThat(res.getExpenses()).isEqualTo(BigDecimal.ZERO);
            assertThat(res.getBalance()).isEqualTo(BigDecimal.ZERO);
            assertThat(res.getPieChart()).isEmpty();
            assertThat(res.getRecentTransactions()).isEmpty();
        }

        /**
         * TC-DASH-08 – Dữ liệu chỉ của user đang đăng nhập (query đúng user_id)
         */
        @Test
        @DisplayName("TC-DASH-08: Query transaction đúng theo user_id đang đăng nhập")
        void getDashboard_queriesOnlyCurrentUserId() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            when(transactionRepository.findTop3ByUserOrderByDateDesc(eq(user)))
                    .thenReturn(List.of());

            dashboardService.getDashboard(user.getEmail());

            verify(transactionRepository).findTop3ByUserOrderByDateDesc(eq(user));
            // Không bao giờ query user_id khác
        }

        // --- Helpers ---
        private Transaction buildTransaction(BigDecimal amount, String type, LocalDate date) {
            Category cat = buildCategory("Housing", type);
            return buildTransactionWithCategory(amount, cat, date);
        }

        private Transaction buildTransactionWithCategory(BigDecimal amount, Category cat, LocalDate date) {
            Transaction t = new Transaction();
            t.setId((long)(Math.random() * 1000));
            t.setAmount(amount);
            t.setCategory(cat);
            t.setDate(date);
            t.setUser(user);
            return t;
        }

        private Category buildCategory(String name, String type) {
            CategoryIcon icon = new CategoryIcon();
            icon.setEmoji("🏠");
            icon.setIconUrl("https://cdn.example.com/housing.png");
            Category c = new Category();
            c.setId(3L);
            c.setName(name);
            c.setType(CategoryName.valueOf(type));
            c.setCategoryIcon(icon);
            return c;
        }
    }