package com.example.demo.repository;

import com.example.demo.model.Notification;
import com.example.demo.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(UserAccount recipient);

    long countByRecipientAndReadFalse(UserAccount recipient);
}
