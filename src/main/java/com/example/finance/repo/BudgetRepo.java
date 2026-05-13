package com.example.finance.repo;

import com.example.finance.entity.Budget;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BudgetRepo extends JpaRepository<Budget, Long> {
    Optional<Budget> findByCategoryIdAndMonthAndYearAndUser(Long categoryId, Integer month, Integer year, User user);
    List<Budget> findByMonthAndYearAndUser(Integer month, Integer year, User user);
    Optional<Budget> findByIdAndUser(Long id, User user);
}
