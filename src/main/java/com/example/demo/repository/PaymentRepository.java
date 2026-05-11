package com.example.demo.repository;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStudentOrderByCreatedAtDesc(UserAccount student);

    List<Payment> findAllByOrderByCreatedAtDesc();

    long countByStatus(PaymentStatus status);
}
