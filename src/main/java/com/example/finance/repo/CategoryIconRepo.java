package com.example.finance.repo;

import com.example.finance.entity.CategoryIcon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryIconRepo extends JpaRepository<CategoryIcon, Long> {
    Optional<CategoryIcon> findByCategoryNameIgnoreCase(String name);
    Optional<CategoryIcon> findByEmoji(String emoji);
    boolean existsByEmoji(String emoji);
}
