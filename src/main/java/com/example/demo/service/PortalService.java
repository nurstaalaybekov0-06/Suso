package com.example.demo.service;

import com.example.demo.model.Notification;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.model.RequestStatus;
import com.example.demo.model.RequestType;
import com.example.demo.model.Role;
import com.example.demo.model.Room;
import com.example.demo.model.RoomStatus;
import com.example.demo.model.ServiceRequest;
import com.example.demo.model.UserAccount;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ServiceRequestRepository;
import com.example.demo.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PortalService {

    private final UserAccountRepository userAccountRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final NotificationRepository notificationRepository;

    public PortalService(UserAccountRepository userAccountRepository, RoomRepository roomRepository, PaymentRepository paymentRepository,
                         ServiceRequestRepository serviceRequestRepository, NotificationRepository notificationRepository) {
        this.userAccountRepository = userAccountRepository;
        this.roomRepository = roomRepository;
        this.paymentRepository = paymentRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.notificationRepository = notificationRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<UserAccount> getAllUsers() {
        return userAccountRepository.findAll();
    }

    public List<UserAccount> getStudents() {
        return userAccountRepository.findByRoleOrderByFullNameAsc(Role.STUDENT);
    }

    public List<UserAccount> getStaff() {
        return userAccountRepository.findByRoleOrderByFullNameAsc(Role.TECH_STAFF);
    }

    public List<Payment> getStudentPayments(UserAccount student) {
        return paymentRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ServiceRequest> getStudentRequests(UserAccount student) {
        return serviceRequestRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    public List<ServiceRequest> getAllRequests() {
        return serviceRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ServiceRequest> getRequestsForStaff(UserAccount staff) {
        List<ServiceRequest> assigned = serviceRequestRepository.findByAssignedStaffOrderByCreatedAtDesc(staff);
        return assigned.isEmpty() ? serviceRequestRepository.findAllByOrderByCreatedAtDesc() : assigned;
    }

    public List<Notification> getNotifications(UserAccount user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    public long getUnreadNotificationCount(UserAccount user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    public void markNotificationsAsRead(UserAccount user) {
        for (Notification notification : notificationRepository.findByRecipientOrderByCreatedAtDesc(user)) {
            if (!notification.isRead()) {
                notification.setRead(true);
            }
        }
    }

    public Map<Long, Long> getRoomOccupancy() {
        Map<Long, Long> occupancy = new LinkedHashMap<>();
        for (Room room : roomRepository.findAll()) {
            occupancy.put(room.getId(), userAccountRepository.countByCurrentRoomId(room.getId()));
        }
        return occupancy;
    }

    public void createStudentRequest(UserAccount student, String title, String description, RequestType type) {
        ServiceRequest request = new ServiceRequest(
                student,
                title,
                description,
                type,
                RequestStatus.OPEN,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        serviceRequestRepository.save(request);
        notificationRepository.save(new Notification(
                student,
                "Заявка зарегистрирована",
                "Ваше обращение \"" + title + "\" принято в обработку.",
                LocalDateTime.now()
        ));
        notifyUsersByRole(
                Role.COMMANDANT,
                "Новая заявка от студента",
                student.getFullName() + " отправил(а) заявку \"" + title + "\"."
        );
        if (type == RequestType.MAINTENANCE) {
            notifyUsersByRole(
                    Role.TECH_STAFF,
                    "Новая техническая заявка",
                    "Поступила новая заявка \"" + title + "\" от " + student.getFullName() + "."
            );
        }
    }

    public void registerPayment(UserAccount student, BigDecimal amount, String description) {
        Payment payment = new Payment(student, amount, description, LocalDateTime.now(), PaymentStatus.PAID);
        paymentRepository.save(payment);
        student.setBalance(student.getBalance().subtract(amount));
        userAccountRepository.save(student);
        notificationRepository.save(new Notification(
                student,
                "Платеж проведен",
                "Оплата на сумму " + amount + " сом успешно зафиксирована.",
                LocalDateTime.now()
        ));
        notifyUsersByRole(
                Role.COMMANDANT,
                "Новый платеж",
                student.getFullName() + " оплатил(а) " + amount + " сом."
        );
    }

    public Room createRoom(String roomNumber, String building, Integer floorNumber, Integer capacity, BigDecimal monthlyFee) {
        Room room = new Room(roomNumber, building, floorNumber, capacity, monthlyFee, RoomStatus.AVAILABLE);
        return roomRepository.save(room);
    }

    public void assignRoomToStudent(Long studentId, Long roomId) {
        UserAccount student = userAccountRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден."));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Комната не найдена."));
        long currentOccupancy = userAccountRepository.countByCurrentRoomId(roomId);
        if (currentOccupancy >= room.getCapacity()) {
            room.setStatus(RoomStatus.FULL);
            roomRepository.save(room);
            throw new IllegalArgumentException("В комнате больше нет свободных мест.");
        }
        student.setCurrentRoom(room);
        student.setBalance(room.getMonthlyFee());
        userAccountRepository.save(student);
        if (currentOccupancy + 1 >= room.getCapacity()) {
            room.setStatus(RoomStatus.FULL);
        } else {
            room.setStatus(RoomStatus.AVAILABLE);
        }
        roomRepository.save(room);
        notificationRepository.save(new Notification(
                student,
                "Заселение подтверждено",
                "Вас заселили в комнату " + room.getRoomNumber() + ".",
                LocalDateTime.now()
        ));
        notifyUsersByRole(
                Role.COMMANDANT,
                "Заселение выполнено",
                student.getFullName() + " заселен(а) в комнату " + room.getRoomNumber() + "."
        );
    }

    public void assignRequestToStaff(Long requestId, Long staffId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена."));
        UserAccount staff = userAccountRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден."));
        request.setAssignedStaff(staff);
        request.setStatus(RequestStatus.IN_PROGRESS);
        request.setUpdatedAt(LocalDateTime.now());
        serviceRequestRepository.save(request);
        notificationRepository.save(new Notification(
                request.getStudent(),
                "Назначен исполнитель",
                "По заявке \"" + request.getTitle() + "\" назначен технический специалист.",
                LocalDateTime.now()
        ));
        notificationRepository.save(new Notification(
                staff,
                "Вам назначена заявка",
                "Вам назначена заявка \"" + request.getTitle() + "\" от " + request.getStudent().getFullName() + ".",
                LocalDateTime.now()
        ));
    }

    public void updateRequestStatus(Long requestId, RequestStatus status, UserAccount actingUser) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена."));
        request.setStatus(status);
        if (actingUser.getRole() == Role.TECH_STAFF) {
            request.setAssignedStaff(actingUser);
        }
        request.setUpdatedAt(LocalDateTime.now());
        serviceRequestRepository.save(request);
        notificationRepository.save(new Notification(
                request.getStudent(),
                "Статус заявки обновлен",
                "Заявка \"" + request.getTitle() + "\" переведена в статус " + status + ".",
                LocalDateTime.now()
        ));
        notifyUsersByRole(
                Role.COMMANDANT,
                "Обновление по заявке",
                "Заявка \"" + request.getTitle() + "\" теперь имеет статус " + status + "."
        );
    }

    public void sendNotification(Long recipientId, String title, String message) {
        UserAccount recipient = userAccountRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Получатель не найден."));
        notificationRepository.save(new Notification(recipient, title, message, LocalDateTime.now()));
    }

    private void notifyUsersByRole(Role role, String title, String message) {
        for (UserAccount user : userAccountRepository.findByRoleOrderByFullNameAsc(role)) {
            notificationRepository.save(new Notification(user, title, message, LocalDateTime.now()));
        }
    }

    public Map<String, Object> buildSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("students", userAccountRepository.countByRole(Role.STUDENT));
        summary.put("staff", userAccountRepository.countByRole(Role.TECH_STAFF));
        summary.put("openRequests", serviceRequestRepository.countByStatus(RequestStatus.OPEN));
        summary.put("resolvedRequests", serviceRequestRepository.countByStatus(RequestStatus.RESOLVED));
        summary.put("paidPayments", paymentRepository.countByStatus(PaymentStatus.PAID));
        summary.put("rooms", roomRepository.count());
        return summary;
    }
}
