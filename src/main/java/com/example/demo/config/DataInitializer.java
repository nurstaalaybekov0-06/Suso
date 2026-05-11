package com.example.demo.config;

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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserAccountRepository userAccountRepository,
                               RoomRepository roomRepository,
                               PaymentRepository paymentRepository,
                               ServiceRequestRepository serviceRequestRepository,
                               NotificationRepository notificationRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userAccountRepository.count() > 0) {
                return;
            }

            Room room101 = roomRepository.save(new Room("101", "Общежитие А", 1, 2, BigDecimal.valueOf(3500), RoomStatus.AVAILABLE));
            Room room205 = roomRepository.save(new Room("205", "Общежитие А", 2, 3, BigDecimal.valueOf(3900), RoomStatus.AVAILABLE));
            roomRepository.save(new Room("312", "Общежитие B", 3, 2, BigDecimal.valueOf(4200), RoomStatus.MAINTENANCE));

            UserAccount commandant = userAccountRepository.save(new UserAccount(
                    "Айжан Салиева",
                    "commandant@dorm.kg",
                    passwordEncoder.encode("commandant123"),
                    Role.COMMANDANT,
                    "Администрация",
                    "+996700000001",
                    BigDecimal.ZERO
            ));

            UserAccount tech = userAccountRepository.save(new UserAccount(
                    "Руслан Иманов",
                    "tech@dorm.kg",
                    passwordEncoder.encode("tech123"),
                    Role.TECH_STAFF,
                    "Техслужба",
                    "+996700000002",
                    BigDecimal.ZERO
            ));

            UserAccount student = new UserAccount(
                    "Нурсултан Таалайбеков",
                    "student@dorm.kg",
                    passwordEncoder.encode("student123"),
                    Role.STUDENT,
                    "ПИ(б)-3-24",
                    "+996700000003",
                    BigDecimal.valueOf(3500)
            );
            student.setCurrentRoom(room101);
            student = userAccountRepository.save(student);

            UserAccount secondStudent = new UserAccount(
                    "Алина Жумабекова",
                    "alina@dorm.kg",
                    passwordEncoder.encode("student123"),
                    Role.STUDENT,
                    "ПИ(б)-2-24",
                    "+996700000004",
                    BigDecimal.valueOf(3900)
            );
            secondStudent.setCurrentRoom(room205);
            secondStudent = userAccountRepository.save(secondStudent);

            paymentRepository.save(new Payment(student, BigDecimal.valueOf(3500), "Оплата за апрель", LocalDateTime.now().minusDays(7), PaymentStatus.PAID));
            paymentRepository.save(new Payment(secondStudent, BigDecimal.valueOf(3900), "Оплата за апрель", LocalDateTime.now().minusDays(2), PaymentStatus.PENDING));

            ServiceRequest maintenance = new ServiceRequest(
                    student,
                    "Не работает розетка",
                    "Нужен осмотр электрики в комнате 101.",
                    RequestType.MAINTENANCE,
                    RequestStatus.IN_PROGRESS,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().minusHours(5)
            );
            maintenance.setAssignedStaff(tech);
            serviceRequestRepository.save(maintenance);

            serviceRequestRepository.save(new ServiceRequest(
                    secondStudent,
                    "Заявка на заселение подруги",
                    "Нужно рассмотреть возможность подселения в следующем месяце.",
                    RequestType.SETTLEMENT,
                    RequestStatus.OPEN,
                    LocalDateTime.now().minusHours(10),
                    LocalDateTime.now().minusHours(10)
            ));

            notificationRepository.save(new Notification(student, "Проверка оплаты", "Не забудьте подтвердить оплату за следующий месяц до 5 числа.", LocalDateTime.now().minusDays(1)));
            notificationRepository.save(new Notification(tech, "Новая задача", "Поступила заявка по электрике в комнате 101.", LocalDateTime.now().minusHours(8)));
            notificationRepository.save(new Notification(commandant, "Отчет готов", "Система сформировала сводку по платежам и заявкам.", LocalDateTime.now().minusHours(2)));
        };
    }
}
