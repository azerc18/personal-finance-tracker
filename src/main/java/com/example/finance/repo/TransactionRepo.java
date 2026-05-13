package com.example.finance.repo;

import com.example.finance.dto.dashboarddto.DashboardResponse;
import com.example.finance.dto.reportdto.CategorySumProjection;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.category.type = 'INCOME'")
    BigDecimal getTotalIncome(User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
            "FROM Transaction t " +
            "WHERE t.user = :user " +
            "AND t.category.type = 'INCOME'" +
            "AND month(t.date) = :month " +
            "and year(t.date) = :year")
    BigDecimal getTotalIncomeByMonthAndYear(@Param("user") User user,
                                            @Param("month") int month,
                                            @Param("year") int year);

    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
            "FROM Transaction t " +
            "WHERE t.user = :user " +
            "AND t.category.type = 'EXPENSE'" +
            "AND month(t.date) = :month " +
            "and year(t.date) = :year")
    BigDecimal getTotalExpenseByMonthAndYear(@Param("user") User user,
                                             @Param("month") int month,
                                             @Param("year") int year);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.category.type = 'EXPENSE'")
    BigDecimal getTotalExpense(User user);

    @Query("""
    SELECT t.category.name, SUM(t.amount), t.category.type
    FROM Transaction t
    WHERE t.user = :user
    GROUP BY t.category.name, t.category.type
""")
    List<DashboardResponse.pieChartData> getPieChart(User user);

        @Query("SELECT t FROM Transaction t " +
                "WHERE t.user.id = :userId " +
                "AND (:type IS NULL OR t.category.type = :type) " +
                "AND FUNCTION('MONTH', t.date) = :month " +
                "AND FUNCTION('YEAR', t.date) = :year")
        List<Transaction> findByUserIdAndTypeAndMonthYear(
                @Param("userId") Long userId,
                @Param("type") CategoryName type,
                @Param("month") int month,
                @Param("year") int year
        );

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.user = :user " +
            "AND month(t.date) = :month " +
            "AND year(t.date) = :year")
    List<Transaction> findByUserAndMonthAndYear(
            @Param("user") User user,
            @Param("month") int month,
            @Param("year") int year
    );

    List<Transaction> findTop3ByUserOrderByDateDesc(User user);

    @Query("""
    SELECT t FROM Transaction t
    WHERE t.user = :user
    AND t.date BETWEEN :startDate AND :endDate
    AND (:type IS NULL OR t.category.type = :type)
    ORDER BY t.date DESC
    """)
    List<Transaction> findTransactionsHistory(
            User user,
            CategoryName type,
            LocalDate startDate,
            LocalDate endDate
    );
    @Query(value = "select month(t.date) as month, " +
            "coalesce(sum(case when c.type = 'INCOME' then t.amount else 0 end), 0 ) as income, " +
            "coalesce(sum(case when c.type = 'EXPENSE' then t.amount else 0 end), 0 ) as expense " +
            "from transactions t " +
            "join categories c on t.category_id = c.id " +
            "where t.user_id = :userId " +
            "and year(t.date) = :year " +
            "group by month(t.date) " +
            "order by month(t.date)",
            nativeQuery = true)
    List<Object[]> getMonthlyChartData(
            @Param("userId") Long userId,
            @Param("year") int year
    );

    @Query(value = "select concat(monthname(t.date), ' ', year(t.date)) as month, " +
            "coalesce(sum(case when c.type = 'INCOME' then t.amount else 0 end), 0) as income, " +
            "coalesce(sum(case when c.type = 'EXPENSE' then t.amount else 0 end), 0) as expense " +
            "from transactions t " +
            "join categories c on t.category_id = c.id " +
            "where t.user_id = :userId " +
            "and year(t.date) = :year " +
            "and month(t.date) = :month " +
            "group by month",
            nativeQuery = true)
    List<Object[]> getSummary(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") Integer month
    );

    @Query("""
    SELECT t.category.name as categoryName, SUM(t.amount) as totalAmount
    FROM Transaction t
    WHERE t.user = :user
    AND month(t.date) = :month
    AND year(t.date) = :year
    AND t.category.type = 'EXPENSE'
    GROUP BY t.category.name
    ORDER BY SUM(t.amount) DESC
""")
    List<CategorySumProjection> findTopCategoriesByAmount(@Param("user") User user,
                                                          @Param("month") int month,
                                                          @Param("year") int year,
                                                          Pageable pageable);



}
