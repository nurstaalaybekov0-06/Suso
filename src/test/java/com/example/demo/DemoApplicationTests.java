package com.example.demo;

import com.example.demo.model.Notification;
import com.example.demo.model.Role;
import com.example.demo.model.UserAccount;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.UserAccountRepository;
import com.example.demo.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void dashboardRendersForSeededStudent() throws Exception {
		UserAccount student = userAccountRepository.findByEmailIgnoreCase("student@dorm.kg").orElseThrow();
		assertThat(student.getRole()).isEqualTo(Role.STUDENT);

		mockMvc.perform(get("/dashboard")
						.sessionAttr(AuthService.SESSION_USER_ID, student.getId()))
				.andExpect(status().isOk());
	}

	@Test
	void notificationBadgeShowsUnreadCountAndClearsOnNotificationsPage() throws Exception {
		UserAccount commandant = userAccountRepository.findByEmailIgnoreCase("commandant@dorm.kg").orElseThrow();
		Notification notification = notificationRepository.save(new Notification(
				commandant,
				"Тестовое уведомление",
				"Проверьте новое обращение по заселению.",
				LocalDateTime.now()
		));

		long unreadBeforeOpen = notificationRepository.countByRecipientAndReadFalse(commandant);
		assertThat(unreadBeforeOpen).isGreaterThan(0);

		mockMvc.perform(get("/dashboard")
						.param("section", "overview")
						.sessionAttr(AuthService.SESSION_USER_ID, commandant.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(">" + unreadBeforeOpen + "<")));

		mockMvc.perform(get("/dashboard")
						.param("section", "notifications")
						.sessionAttr(AuthService.SESSION_USER_ID, commandant.getId()))
				.andExpect(status().isOk());

		Notification updatedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(updatedNotification.isRead()).isTrue();
		assertThat(notificationRepository.countByRecipientAndReadFalse(commandant)).isZero();
	}

}
