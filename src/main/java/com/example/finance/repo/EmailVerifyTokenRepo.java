package com.example.finance.repo;

import com.example.finance.entity.User;
import com.example.finance.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface EmailVerifyTokenRepo extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(User user);

}
