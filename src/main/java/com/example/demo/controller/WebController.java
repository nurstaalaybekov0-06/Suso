package com.example.demo.controller;

import com.example.demo.model.RequestStatus;
import com.example.demo.model.RequestType;
import com.example.demo.model.Role;
import com.example.demo.model.UserAccount;
import com.example.demo.service.AuthService;
import com.example.demo.service.PortalService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Year;

@Controller
public class WebController {

    private static final String SESSION_CARD_MASK = "studentCardMask";
    private static final String SESSION_CARD_EXPIRY = "studentCardExpiry";

    private final AuthService authService;
    private final PortalService portalService;

    public WebController(AuthService authService, PortalService portalService) {
        this.authService = authService;
        this.portalService = portalService;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model,
                       @RequestParam(name = "error", required = false) String error,
                       @RequestParam(name = "message", required = false) String message) {
        Long userId = (Long) session.getAttribute(AuthService.SESSION_USER_ID);
        if (userId != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        return "home";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model,
                            @RequestParam(name = "error", required = false) String error,
                            @RequestParam(name = "message", required = false) String message) {
        Long userId = (Long) session.getAttribute(AuthService.SESSION_USER_ID);
        if (userId != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model,
                               @RequestParam(name = "error", required = false) String error,
                               @RequestParam(name = "message", required = false) String message) {
        Long userId = (Long) session.getAttribute(AuthService.SESSION_USER_ID);
        if (userId != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        return "register";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            authService.login(email, password, session);
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String groupName,
                           @RequestParam String phone,
                           RedirectAttributes redirectAttributes) {
        try {
            authService.registerStudent(fullName, email, password, groupName, phone);
            redirectAttributes.addAttribute("message", "Регистрация завершена. Теперь можно войти в систему.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/register";
        }
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model,
                            @RequestParam(name = "section", required = false) String section,
                            @RequestParam(name = "message", required = false) String message,
                            @RequestParam(name = "error", required = false) String error) {
        try {
            UserAccount user = authService.requireUser(session);
            String currentSection = resolveSection(user.getRole(), section);
            if ("notifications".equals(currentSection)) {
                portalService.markNotificationsAsRead(user);
            }
            model.addAttribute("currentUser", user);
            model.addAttribute("message", message);
            model.addAttribute("error", error);
            model.addAttribute("summary", portalService.buildSummary());
            model.addAttribute("notifications", portalService.getNotifications(user));
            model.addAttribute("currentSection", currentSection);
            model.addAttribute("unreadNotificationCount", portalService.getUnreadNotificationCount(user));

            if (user.getRole() == Role.STUDENT) {
                model.addAttribute("payments", portalService.getStudentPayments(user));
                model.addAttribute("requests", portalService.getStudentRequests(user));
                model.addAttribute("rooms", portalService.getAllRooms());
                model.addAttribute("occupancyByRoomId", portalService.getRoomOccupancy());
                model.addAttribute("boundCardMask", session.getAttribute(SESSION_CARD_MASK));
                model.addAttribute("boundCardExpiry", session.getAttribute(SESSION_CARD_EXPIRY));
            } else if (user.getRole() == Role.TECH_STAFF) {
                model.addAttribute("requests", portalService.getRequestsForStaff(user));
            } else if (user.getRole() == Role.COMMANDANT) {
                model.addAttribute("users", portalService.getAllUsers());
                model.addAttribute("students", portalService.getStudents());
                model.addAttribute("staff", portalService.getStaff());
                model.addAttribute("rooms", portalService.getAllRooms());
                model.addAttribute("payments", portalService.getAllPayments());
                model.addAttribute("requests", portalService.getAllRequests());
                model.addAttribute("occupancyByRoomId", portalService.getRoomOccupancy());
            }
            return "dashboard";
        } catch (IllegalStateException ex) {
            return "redirect:/login?error=" + ex.getMessage();
        }
    }

    @PostMapping("/student/requests")
    public String createRequest(@RequestParam String title,
                                @RequestParam String description,
                                @RequestParam RequestType type,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            UserAccount student = authService.requireRole(session, Role.STUDENT);
            portalService.createStudentRequest(student, title, description, type);
            redirectAttributes.addAttribute("message", "Заявка отправлена.");
            redirectAttributes.addAttribute("section", "requests");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "requests");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/student/card")
    public String bindCard(@RequestParam String cardNumber,
                           @RequestParam Integer expiryMonth,
                           @RequestParam Integer expiryYear,
                           @RequestParam String cvc,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            authService.requireRole(session, Role.STUDENT);
            String normalizedCardNumber = normalizeCardNumber(cardNumber);
            validateCard(normalizedCardNumber, expiryMonth, expiryYear, cvc);
            session.setAttribute(SESSION_CARD_MASK, maskCard(normalizedCardNumber));
            session.setAttribute(SESSION_CARD_EXPIRY, String.format("%02d/%d", expiryMonth, expiryYear));
            redirectAttributes.addAttribute("message", "Карта привязана. Теперь можно оплатить проживание.");
            redirectAttributes.addAttribute("section", "payments");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "payments");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/student/card/delete")
    public String deleteCard(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            authService.requireRole(session, Role.STUDENT);
            session.removeAttribute(SESSION_CARD_MASK);
            session.removeAttribute(SESSION_CARD_EXPIRY);
            redirectAttributes.addAttribute("message", "Карта удалена. Можно привязать новую карту.");
            redirectAttributes.addAttribute("section", "payments");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "payments");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/student/payments")
    public String createPayment(@RequestParam BigDecimal amount,
                                @RequestParam String description,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            UserAccount student = authService.requireRole(session, Role.STUDENT);
            if (session.getAttribute(SESSION_CARD_MASK) == null) {
                throw new IllegalArgumentException("Сначала привяжите банковскую карту.");
            }
            portalService.registerPayment(student, amount, description);
            redirectAttributes.addAttribute("message", "Оплата успешно зафиксирована.");
            redirectAttributes.addAttribute("section", "payments");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "payments");
        }
        return "redirect:/dashboard";
    }

    private String normalizeCardNumber(String cardNumber) {
        return cardNumber == null ? "" : cardNumber.replaceAll("[\\s-]", "");
    }

    private void validateCard(String cardNumber, Integer expiryMonth, Integer expiryYear, String cvc) {
        if (!cardNumber.matches("\\d{16}")) {
            throw new IllegalArgumentException("Введите номер карты из 16 цифр.");
        }
        if (!isValidLuhn(cardNumber)) {
            throw new IllegalArgumentException("Номер карты введен неверно.");
        }
        if (expiryMonth == null || expiryMonth < 1 || expiryMonth > 12) {
            throw new IllegalArgumentException("Введите корректный месяц действия карты.");
        }
        int currentYear = Year.now().getValue();
        if (expiryYear == null || expiryYear < currentYear || expiryYear > currentYear + 20) {
            throw new IllegalArgumentException("Введите корректный год действия карты.");
        }
        if (cvc == null || !cvc.matches("\\d{3,4}")) {
            throw new IllegalArgumentException("Введите CVC из 3 или 4 цифр.");
        }
    }

    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = cardNumber.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private String maskCard(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    @PostMapping("/staff/requests/{requestId}/status")
    public String updateStaffRequestStatus(@PathVariable Long requestId,
                                           @RequestParam RequestStatus status,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        try {
            UserAccount staff = authService.requireRole(session, Role.TECH_STAFF);
            portalService.updateRequestStatus(requestId, status, staff);
            redirectAttributes.addAttribute("message", "Статус заявки обновлен.");
            redirectAttributes.addAttribute("section", "requests");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "requests");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/commandant/rooms")
    public String createRoom(@RequestParam String roomNumber,
                             @RequestParam String building,
                             @RequestParam Integer floorNumber,
                             @RequestParam Integer capacity,
                             @RequestParam BigDecimal monthlyFee,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            authService.requireRole(session, Role.COMMANDANT);
            portalService.createRoom(roomNumber, building, floorNumber, capacity, monthlyFee);
            redirectAttributes.addAttribute("message", "Комната добавлена.");
            redirectAttributes.addAttribute("section", "rooms");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "rooms");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/commandant/students/{studentId}/assign-room")
    public String assignRoom(@PathVariable Long studentId,
                             @RequestParam Long roomId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            authService.requireRole(session, Role.COMMANDANT);
            portalService.assignRoomToStudent(studentId, roomId);
            redirectAttributes.addAttribute("message", "Студент заселен в комнату.");
            redirectAttributes.addAttribute("section", "students");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "students");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/commandant/requests/{requestId}/assign")
    public String assignRequest(@PathVariable Long requestId,
                                @RequestParam Long staffId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            authService.requireRole(session, Role.COMMANDANT);
            portalService.assignRequestToStaff(requestId, staffId);
            redirectAttributes.addAttribute("message", "Исполнитель назначен.");
            redirectAttributes.addAttribute("section", "requests");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "requests");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/commandant/requests/{requestId}/status")
    public String updateCommandantRequestStatus(@PathVariable Long requestId,
                                                @RequestParam RequestStatus status,
                                                HttpSession session,
                                                RedirectAttributes redirectAttributes) {
        try {
            UserAccount commandant = authService.requireRole(session, Role.COMMANDANT);
            portalService.updateRequestStatus(requestId, status, commandant);
            redirectAttributes.addAttribute("message", "Статус заявки обновлен.");
            redirectAttributes.addAttribute("section", "requests");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "requests");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/commandant/notifications")
    public String sendNotification(@RequestParam Long recipientId,
                                   @RequestParam String title,
                                   @RequestParam String message,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            authService.requireRole(session, Role.COMMANDANT);
            portalService.sendNotification(recipientId, title, message);
            redirectAttributes.addAttribute("message", "Уведомление отправлено.");
            redirectAttributes.addAttribute("section", "notifications");
        } catch (RuntimeException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("section", "notifications");
        }
        return "redirect:/dashboard";
    }

    private String resolveSection(Role role, String section) {
        String normalized = section == null ? "" : section.trim().toLowerCase();
        return switch (role) {
            case STUDENT -> switch (normalized) {
                case "rooms", "payments", "requests", "notifications" -> normalized;
                default -> "overview";
            };
            case TECH_STAFF -> switch (normalized) {
                case "requests", "notifications" -> normalized;
                default -> "overview";
            };
            case COMMANDANT -> switch (normalized) {
                case "students", "rooms", "requests", "payments", "notifications" -> normalized;
                default -> "overview";
            };
        };
    }
}
