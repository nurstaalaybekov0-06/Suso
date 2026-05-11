package com.example.demo.repository;

import com.example.demo.model.RequestStatus;
import com.example.demo.model.ServiceRequest;
import com.example.demo.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByStudentOrderByCreatedAtDesc(UserAccount student);

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    List<ServiceRequest> findByAssignedStaffOrderByCreatedAtDesc(UserAccount assignedStaff);

    long countByStatus(RequestStatus status);
}
