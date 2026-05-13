    package com.example.finance.service;

    import com.example.finance.dto.reportdto.ReportCategoryResponse;
    import com.example.finance.dto.reportdto.ReportMonthlyResponse;
    import com.example.finance.entity.Category;
    import com.example.finance.entity.CategoryIcon;
    import com.example.finance.entity.Transaction;
    import com.example.finance.entity.User;
    import com.example.finance.enums.CategoryName;
    import com.example.finance.repo.TransactionRepo;
    import com.example.finance.repo.UserRepo;
    import com.example.finance.service.reportservice.ReportServiceImpl;
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
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;
    import java.util.Optional;

    import static org.assertj.core.api.Assertions.assertThat;
    import static org.assertj.core.api.Assertions.within;
    import static org.mockito.ArgumentMatchers.*;
    import static org.mockito.Mockito.when;

    @Nested
    @DisplayName("ReportService – Business Logic")
    @ExtendWith(MockitoExtension.class)
    class ReportServiceTest {

        @Mock
        TransactionRepo transactionRepository;
        @Mock
        UserRepo userRepo;

        @InjectMocks
        ReportServiceImpl reportService;

        private User user;

        @BeforeEach
        void setUp() {
            user = new User();
            user.setId(1L);
        }

        /**
         * TC-RPT-SUM-07 – Không có giao dịch → tất cả 0, topExpenses rỗng
         */
        @Test
        @DisplayName("TC-RPT-SUM-07: Không có giao dịch trả về các giá trị bằng 0")
        void getMonthlyReport_noTransactions_returnsZero() {
            // 1. Mock user
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            // 2. Mock Repository trả về danh sách trống cho Summary và Chart
            when(transactionRepository.getSummary(anyLong(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.getMonthlyChartData(anyLong(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // 3. Thực thi
            ReportMonthlyResponse res = reportService.getMonthlyReport(4, 2024, user.getEmail());

            // 4. Assert (Sử dụng BigDecimal)
            assertThat(res.getSummary().getIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(res.getSummary().getExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * TC-RPT-CAT-02 – Tổng percentage ≈ 100%
         */
        @Test
        @DisplayName("TC-RPT-CAT-02: Tổng percentage của tất cả categories ≈ 100%")
        void getCategoryReport_totalPercentageApprox100() {
            // 1. Mock user và trả về Optional
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            // 2. Mock Repository khớp với code thực tế
            // Đảm bảo buildExpenseTransactions() trả về List<Transaction> có dữ liệu
            when(transactionRepository.findByUserIdAndTypeAndMonthYear(
                    eq(user.getId()),
                    eq(CategoryName.EXPENSE),
                    anyInt(),
                    anyInt()))
                    .thenReturn(buildExpenseTransactions());

            // 3. Gọi Service với đúng kiểu dữ liệu (Enum và Email)
            ReportCategoryResponse res = reportService.getCategoryReport(CategoryName.EXPENSE, 4, 2024, user.getEmail());

            // 4. Tính tổng percentage (Chuyển từ BigDecimal sang Double để dùng isCloseTo)
            double totalPercentage = res.getData().stream()
                    .map(data -> data.getPercentage())
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            // 5. Assert (Cho phép sai số nhỏ do làm tròn HALF_UP)
            assertThat(totalPercentage).isCloseTo(100.0, within(0.1));
        }

        /**
         * TC-RPT-CAT-03 – percentage tính đúng
         * Housing: 700 / 1400 * 100 = 50.0%
         */
        @Test
        @DisplayName("TC-RPT-CAT-03: percentage = (amount / total) * 100 tính đúng")
        void getCategoryReport_percentageCalculatedCorrectly() {
            // 1. Mock tìm kiếm User (Bắt buộc)
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            // 2. Mock Repository (Phải khớp chính xác tên hàm và kiểu tham số)
            // Code gọi: transactionRepo.findByUserIdAndTypeAndMonthYear(user.getId(), type, month, year)
            when(transactionRepository.findByUserIdAndTypeAndMonthYear(
                    eq(user.getId()),
                    eq(CategoryName.EXPENSE),
                    eq(4),
                    eq(2024)))
                    .thenReturn(buildExpenseTransactions()); // Housing:700, Food:420, Shopping:280 → total:1400

            // 3. Gọi Service đúng thứ tự tham số: (type, month, year, email)
            ReportCategoryResponse res = reportService.getCategoryReport(CategoryName.EXPENSE, 4, 2024, user.getEmail());

            // 4. Assert dữ liệu (Sử dụng BigDecimal)
            var housing = res.getData().stream() // Lưu ý: Trong DTO của bạn là getCategoryData()
                    .filter(c -> c.getCategory().equals("Housing"))
                    .findFirst()
                    .orElseThrow();

            // So sánh giá trị BigDecimal 50.0
            assertThat(housing.getPercentage()).isEqualByComparingTo(new BigDecimal("0.5"));
        }

        /**
         * TC-RPT-MONTHLY-02 – summary khớp tháng được chọn
         */
        @Test
        @DisplayName("TC-RPT-MONTHLY-02: summary trả về income/expense đúng của tháng được chọn")
        void getMonthlyReport_summaryMatchesSelectedMonth() {
            // 1. Mock tìm kiếm User (Bắt buộc vì code gọi đầu tiên)
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            // 2. Mock dữ liệu Summary từ Repository (Object[])
            // Cấu trúc mảng Object dựa trên code của bạn: [0]=monthName, [1]=income, [2]=expense
            Object[] mockSummaryRow = new Object[]{"APRIL 2024", new BigDecimal("3000.0"), new BigDecimal("1500.0")};

            List<Object[]> mockResultList = new ArrayList<>();
            mockResultList.add(mockSummaryRow);

            when(transactionRepository.getSummary(eq(user.getId()), eq(2024), eq(4)))
                    .thenReturn(mockResultList);

            // 3. Mock Chart Data (trả về list rỗng để tránh lỗi logic fillingMissingData)
            when(transactionRepository.getMonthlyChartData(eq(user.getId()), eq(2024)))
                    .thenReturn(new ArrayList<>());

            // 4. Gọi Service với đúng thứ tự tham số: (month, year, email)
            ReportMonthlyResponse res = reportService.getMonthlyReport(4, 2024, user.getEmail());

            // 5. Kiểm tra kết quả
            assertThat(res.getSummary().getMonth()).contains("APRIL");
            // So sánh BigDecimal với 0
            assertThat(res.getSummary().getIncome()).isEqualByComparingTo(new BigDecimal("3000.0"));
            assertThat(res.getSummary().getExpense()).isEqualByComparingTo(new BigDecimal("1500.0"));
        }

        // --- Helpers ---
        private List<Transaction> buildMixedTransactions() {
            Category salary = buildCategory("Salary", "INCOME");
            Category housing = buildCategory("Housing", "EXPENSE");
            return List.of(
                    buildTx(BigDecimal.valueOf(3250.0), salary,  LocalDate.of(2024, 4, 1)),
                    buildTx(BigDecimal.valueOf(2150.0), housing, LocalDate.of(2024, 4, 15))
            );
        }

        private List<Transaction> buildExpenseTransactions() {
            Category housing  = buildCategory("Housing",  "EXPENSE");
            Category food     = buildCategory("Food",     "EXPENSE");
            Category shopping = buildCategory("Shopping", "EXPENSE");
            return List.of(
                    buildTx(BigDecimal.valueOf(700.0),  housing,  LocalDate.of(2024, 4, 5)),
                    buildTx(BigDecimal.valueOf(420.0),  food,     LocalDate.of(2024, 4, 10)),
                    buildTx(BigDecimal.valueOf(150280.0),  shopping, LocalDate.of(2024, 4, 15))
            );
        }

        private List<Transaction> buildTransactionsWithFourCategories() {
            Category c1 = buildCategory("Housing",  "EXPENSE");
            Category c2 = buildCategory("Food",     "EXPENSE");
            Category c3 = buildCategory("Shopping", "EXPENSE");
            Category c4 = buildCategory("Transport","EXPENSE");
            return List.of(
                    buildTx(BigDecimal.valueOf(800.0), c1, LocalDate.of(2024, 4, 1)),
                    buildTx(BigDecimal.valueOf(450.0), c2, LocalDate.of(2024, 4, 5)),
                    buildTx(BigDecimal.valueOf(300.0), c3, LocalDate.of(2024, 4, 10)),
                    buildTx(BigDecimal.valueOf(150.0), c4, LocalDate.of(2024, 4, 15))
            );
        }

        private Transaction buildTx(BigDecimal amount, Category category, LocalDate date) {
            Transaction t = new Transaction();
            t.setId((long)(Math.random() * 10000));
            t.setAmount(amount);
            t.setCategory(category);
            t.setDate(date);
            t.setUser(user);
            return t;
        }

        private Category buildCategory(String name, String type) {
            CategoryIcon icon = new CategoryIcon();
            icon.setEmoji("🔖");
            icon.setIconUrl("https://cdn.example.com/" + name.toLowerCase() + ".png");
            Category c = new Category();
            c.setId((long)(Math.random() * 100));
            c.setName(name);
            c.setType(CategoryName.valueOf(type));
            c.setCategoryIcon(icon);
            return c;
        }
    }