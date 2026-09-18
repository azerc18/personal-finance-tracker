package com.example.finance.repo;

import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Long> {
    List<Category> findByType(CategoryName type);
    List<Category> findByUser(User user);
    boolean existsByNameAndUser(String name, User user);
    Optional<Category> findByIdAndUser(Long id, User user);}

