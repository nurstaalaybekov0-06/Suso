package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.model.UserAccount;
import com.example.demo.repository.UserAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuthService {

    public static final String SESSION_USER_ID = "currentUserId";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount registerStudent(String fullName, String email, String password, String groupName, String phone) {
        userAccountRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new IllegalArgumentException("Пользователь с таким email уже существует.");
        });

        UserAccount account = new UserAccount(
                fullName,
                email.trim().toLowerCase(),
                passwordEncoder.encode(password),
                Role.STUDENT,
                groupName,
                phone,
                BigDecimal.ZERO
        );
        return userAccountRepository.save(account);
    }

    public UserAccount login(String email, String password, HttpSession session) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль."));
        session.setAttribute(SESSION_USER_ID, user.getId());
        return user;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public UserAccount requireUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long userId)) {
            throw new IllegalStateException("Требуется авторизация.");
        }
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден."));
    }

    public UserAccount requireRole(HttpSession session, Role role) {
        UserAccount user = requireUser(session);
        if (user.getRole() != role) {
            throw new IllegalStateException("Недостаточно прав для доступа к разделу.");
        }
        return user;
    }
}
