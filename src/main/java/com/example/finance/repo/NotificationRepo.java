package com.example.finance.repo;

import com.example.finance.entity.Notification;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {
   Optional<Notification> findByUser(User user);
}
