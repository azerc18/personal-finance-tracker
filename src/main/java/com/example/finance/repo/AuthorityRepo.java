package com.example.finance.repo;

import com.example.finance.entity.Authority;
import com.example.finance.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityRepo extends JpaRepository<Authority, Long> {
    Optional<Authority> findByName(RoleName name);
}
