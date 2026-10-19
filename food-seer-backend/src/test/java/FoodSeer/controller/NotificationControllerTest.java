package FoodSeer.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import FoodSeer.config.SpringSecurityConfig;
import FoodSeer.dto.NotificationDto;
import FoodSeer.entity.User;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.repositories.UserRepository;
import FoodSeer.security.CustomUserDetailsService;
import FoodSeer.security.DriverStatsAccess;
import FoodSeer.security.JwtAuthenticationFilter;
import FoodSeer.security.JwtTokenProvider;
import FoodSeer.service.NotificationService;
import FoodSeer.service.impl.JwtAccessDeniedHandler;
import FoodSeer.service.impl.JwtAuthenticationEntryPoint;

/**
 * Real JWT login and method security for /api/notifications, with the
 * notification service mocked. No database or server needed.
 */
@WebMvcTest(value = NotificationController.class, properties = {
    "app.jwt-secret=Zm9vZHNlZXItdGVzdC1vbmx5LXNpZ25pbmcta2V5LTMyaXRlbXM=",
    "app.jwt-expiration-milliseconds=60000"
})
@Import({SpringSecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class,
    CustomUserDetailsService.class, DriverStatsAccess.class,
    JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class NotificationControllerTest {

    private static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 12, 12, 0);

    @Autowired private MockMvc mvc;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private UserRepository users;
    @MockBean private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        final String password = passwordEncoder.encode("test-password");
        final String[][] accounts = {
            {"alice", "ROLE_CUSTOMER"}, {"staff", "ROLE_STAFF"}, {"admin", "ROLE_ADMIN"}, {"driver1", "ROLE_DRIVER"}
        };
        for (final String[] account : accounts) {
            final User user = User.builder().username(account[0]).email(account[0] + "@example.test")
                    .password(password).role(account[1]).build();
            when(users.findByUsernameOrEmail(account[0], account[0])).thenReturn(Optional.of(user));
        }
    }

    private String bearer(final String login) {
        final var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, "test-password"));
        return "Bearer " + tokenProvider.generateToken(authentication);
    }

    private NotificationDto notice(final long id, final boolean acknowledged, final String remedy) {
        final LocalDateTime acknowledgedAt;
        if (acknowledged) {
            acknowledgedAt = NOON.plusMinutes(5);
        } else {
            acknowledgedAt = null;
        }
        return new NotificationDto(id, 3L, 8L, "Food safety notice: your order #8 (COOKIES) may have been exposed",
                NOON, acknowledged, acknowledgedAt, remedy);
    }

    @Test
    void anonymousCannotReadNotifications() throws Exception {
        mvc.perform(get("/api/notifications/my")).andExpect(status().isUnauthorized());
        verifyNoInteractions(notificationService);
    }

    @Test
    void customerSeesTheirOwnNotificationsWithRemedy() throws Exception {
        when(notificationService.getNotifications("alice"))
                .thenReturn(List.of(notice(1L, false, "Full refund or a free remake")));

        mvc.perform(get("/api/notifications/my").header("Authorization", bearer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderId").value(8))
                .andExpect(jsonPath("$[0].acknowledged").value(false))
                .andExpect(jsonPath("$[0].remedy").value("Full refund or a free remake"));
        verify(notificationService).getNotifications("alice");
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "admin", "driver1"})
    void nonCustomersHaveNoNotificationInbox(final String login) throws Exception {
        mvc.perform(get("/api/notifications/my").header("Authorization", bearer(login)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notificationService);
    }

    @Test
    void unreadCountIsReturned() throws Exception {
        when(notificationService.countUnacknowledged("alice")).thenReturn(2L);

        mvc.perform(get("/api/notifications/my/unread-count").header("Authorization", bearer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void customerCanAcknowledgeTheirOwnNotification() throws Exception {
        when(notificationService.acknowledge(1L, "alice")).thenReturn(notice(1L, true, null));

        mvc.perform(post("/api/notifications/1/acknowledge").header("Authorization", bearer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true));
        verify(notificationService).acknowledge(1L, "alice");
    }

    @Test
    void acknowledgingSomeoneElsesNotificationIsForbidden() throws Exception {
        when(notificationService.acknowledge(2L, "alice"))
                .thenThrow(new AccessDeniedException("You can only acknowledge your own notifications."));

        mvc.perform(post("/api/notifications/2/acknowledge").header("Authorization", bearer("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void acknowledgingUnknownNotificationIsNotFound() throws Exception {
        when(notificationService.acknowledge(99L, "alice"))
                .thenThrow(new ResourceNotFoundException("Notification does not exist with id 99"));

        mvc.perform(post("/api/notifications/99/acknowledge").header("Authorization", bearer("alice")))
                .andExpect(status().isNotFound());
    }
}
