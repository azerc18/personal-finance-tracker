package com.example.finance.repo;

import com.example.finance.entity.Role;
import com.example.finance.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepo extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
