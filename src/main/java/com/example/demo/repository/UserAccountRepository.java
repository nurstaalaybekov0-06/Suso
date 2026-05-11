package com.example.demo.repository;

import com.example.demo.model.Role;
import com.example.demo.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    List<UserAccount> findByRoleOrderByFullNameAsc(Role role);

    long countByRole(Role role);

    long countByCurrentRoomId(Long roomId);
}
